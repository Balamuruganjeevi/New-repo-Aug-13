package org.avni.dao;

import java.util.*;

import org.avni.domain.*;
import org.avni.projection.IndividualWebProjection;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.avni.application.projections.WebSearchResultProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Repository;

import javax.persistence.criteria.*;

@Repository
@RepositoryRestResource(collectionResourceRel = "individual", path = "individual", exported = false)
@PreAuthorize("hasAnyAuthority('user','admin')")
public interface IndividualRepository extends TransactionalDataRepository<Individual>, OperatingIndividualScopeAwareRepository<Individual> {

    default Specification<Individual> syncTypeIdSpecification(Long typeId) {
        return (Root<Individual> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.equal(root.get("subjectType").get("id"), typeId);
    }

    @Override
    default Page<Individual> getSyncResults(SyncParameters syncParameters) {
        return findAll(syncAuditSpecification(syncParameters)
                        .and(syncTypeIdSpecification(syncParameters.getTypeId()))
                        .and(syncStrategySpecification(syncParameters, true, false)),
                syncParameters.getPageable());
    }

    @Override
    default boolean isEntityChangedForCatchment(SyncParameters syncParameters) {
        return count(syncEntityChangedAuditSpecification(syncParameters)
                .and(syncTypeIdSpecification(syncParameters.getTypeId()))
                .and(syncStrategySpecification(syncParameters, true, false))
        ) > 0;
    }

    default Specification<Individual> getFilterSpecForVoid(Boolean includeVoided) {
        return (Root<Individual> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                includeVoided == null || includeVoided ? cb.and() : cb.isFalse(root.get("isVoided"));
    }

    default Specification<Individual> getFilterSpecForName(String value) {
        return (Root<Individual> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (value != null && !value.isEmpty()) {
                Predicate[] predicates = new Predicate[2];
                String[] values = value.trim().split(" ");
                if (values.length > 0) {
                    predicates[0] = cb.like(cb.upper(root.get("firstName")), values[0].toUpperCase() + "%");
                    predicates[1] = cb.like(cb.upper(root.get("lastName")), values[0].toUpperCase() + "%");
                }
                if (values.length > 1) {
                    predicates[1] = cb.like(cb.upper(root.get("lastName")), values[1].toUpperCase() + "%");
                    return cb.and(predicates[0], predicates[1]);
                }
                return cb.or(predicates[0], predicates[1]);
            }
            return cb.and();
        };
    }

    default Specification<Individual> getFilterSpecForSubjectTypeId(String subjectTypeUUID) {
        return (Root<Individual> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                subjectTypeUUID == null ? cb.and() : root.get("subjectType").get("uuid").in(subjectTypeUUID);
    }

    default Specification<Individual> getFilterSpecForObs(String value) {
        return (Root<Individual> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                value == null ? cb.and() : cb.or(
                        jsonContains(root.get("observations"), "%" + value + "%", cb),
                        jsonContains(root.join("programEnrolments", JoinType.LEFT).get("observations"), "%" + value + "%", cb));
    }

    default Specification<Individual> getFilterSpecForLocationIds(List<Long> locationIds) {
        return (Root<Individual> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                locationIds == null ? cb.and() : root.get("addressLevel").get("id").in(locationIds);
    }

    default Specification<Individual> getFilterSpecForAddress(String locationName) {
        return (Root<Individual> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                locationName == null ? cb.and() :
                        cb.like(cb.upper(root.get("addressLevel").get("titleLineage")), "%" + locationName.toUpperCase() + "%");
    }

    @Query("select ind from Individual ind " +
            "where ind.isVoided = false " +
            "and ind.subjectType.uuid = :subjectTypeUUID " +
            "and ind.registrationDate between :startDateTime and :endDateTime " +
            "and (coalesce(:locationIds,NULL) is null OR ind.addressLevel.id in :locationIds)")
    Page<Individual> findIndividuals(String subjectTypeUUID, List<Long> locationIds, LocalDate startDateTime, LocalDate endDateTime, Pageable pageable);

    //group by is added for distinct ind records
    @Query("select i from Individual i " +
            "join i.encounters enc " +
            "where enc.encounterType.uuid = :encounterTypeUUID " +
            "and enc.isVoided = false " +
            "and i.isVoided = false " +
            "and coalesce(enc.encounterDateTime, enc.cancelDateTime) between :startDateTime and :endDateTime " +
            "and (coalesce(:locationIds, null) is null OR i.addressLevel.id in :locationIds)" +
            "group by i.id")
    Page<Individual> findEncounters(List<Long> locationIds, DateTime startDateTime, DateTime endDateTime, String encounterTypeUUID, Pageable pageable);


    @Query("select i from Individual i where i.uuid =:id or i.legacyId = :id")
    Individual findByLegacyIdOrUuid(String id);

    @Query("select i from Individual i where (i.uuid =:id or i.legacyId = :id) and i.subjectType = :subjectType")
    Individual findByLegacyIdOrUuidAndSubjectType(String id, SubjectType subjectType);

    @Query(value = "select firstname,lastname,fullname,id,uuid,title_lineage,subject_type_name,gender_name,date_of_birth,enrolments,total_elements from web_search_function(:jsonSearch, :dbUser)", nativeQuery = true)
    List<WebSearchResultProjection> getWebSearchResults(String jsonSearch, String dbUser);

    default Specification<Individual> findBySubjectTypeSpec(String subjectType) {
        Specification<Individual> spec = (Root<Individual> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            Join<Individual, SubjectType> subjectTypeJoin = root.join("subjectType", JoinType.LEFT);
            return cb.and(cb.equal(subjectTypeJoin.get("name"), subjectType));
        };
        return spec;
    }

    default Specification<Individual> findInLocationSpec(List<Long> addressIds) {
        return (Root<Individual> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                addressIds.isEmpty() ? null : root.get("addressLevel").get("id").in(addressIds);
    }

    default Page<Individual> findByConcepts(Date lastModifiedDateTime, Date now, Map<Concept, String> concepts, List<Long> addressIds, Pageable pageable) {
        return findAll(lastModifiedBetween(lastModifiedDateTime, now)
                .and(withConceptValues(concepts))
                .and(findInLocationSpec(addressIds)), pageable);
    }

    default Page<Individual> findByConceptsAndSubjectType(Date lastModifiedDateTime, Date now, Map<Concept, String> concepts, String subjectType, List<Long> addressIds, Pageable pageable) {
        return findAll(lastModifiedBetween(lastModifiedDateTime, now)
                .and(withConceptValues(concepts))
                .and(findBySubjectTypeSpec(subjectType))
                .and(findInLocationSpec(addressIds)), pageable);
    }

    List<Individual> findAllByAddressLevelAndSubjectTypeAndIsVoidedFalse(AddressLevel addressLevel, SubjectType subjectType);

    List<IndividualWebProjection> findAllByUuidIn(List<String> uuids);
}
