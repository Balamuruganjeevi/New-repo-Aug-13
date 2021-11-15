package org.avni.dao;

import org.avni.domain.AddressLevel;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.avni.domain.ProgramEnrolment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RepositoryRestResource(collectionResourceRel = "programEnrolment", path = "programEnrolment", exported = false)
@PreAuthorize("hasAnyAuthority('user','admin')")
public interface ProgramEnrolmentRepository extends TransactionalDataRepository<ProgramEnrolment>, FindByLastModifiedDateTime<ProgramEnrolment>, OperatingIndividualScopeAwareRepository<ProgramEnrolment> {

    Page<ProgramEnrolment> findByIndividualAddressInAndProgramIdAndAuditLastModifiedDateTimeIsBetweenOrderByAuditLastModifiedDateTimeAscIdAsc(
            List<AddressLevel> addressLevels,
            Long programId,
            DateTime lastModifiedDateTime,
            DateTime now,
            Pageable pageable);

    Page<ProgramEnrolment> findByIndividualFacilityIdAndProgramIdAndAuditLastModifiedDateTimeIsBetweenOrderByAuditLastModifiedDateTimeAscIdAsc(
            long facilityId,
            Long programId,
            DateTime lastModifiedDateTime,
            DateTime now,
            Pageable pageable);

    boolean existsByProgramIdAndAuditLastModifiedDateTimeGreaterThanAndIndividualAddressLevelIdIn(
            Long programId,
            DateTime lastModifiedDateTime,
            List<Long> addressIds);

    boolean existsByIndividualFacilityIdAndProgramIdAndAuditLastModifiedDateTimeGreaterThan(
            long facilityId,
            Long programId,
            DateTime lastModifiedDateTime);

    @Override
    default boolean isEntityChangedForCatchment(List<Long> addressIds, DateTime lastModifiedDateTime, Long typeId){
        return existsByProgramIdAndAuditLastModifiedDateTimeGreaterThanAndIndividualAddressLevelIdIn(typeId, lastModifiedDateTime, addressIds);
    }

    @Override
    default boolean isEntityChangedForFacility(long facilityId, DateTime lastModifiedDateTime, Long typeId){
        return existsByIndividualFacilityIdAndProgramIdAndAuditLastModifiedDateTimeGreaterThan(facilityId, typeId, lastModifiedDateTime);
    }

    @Query("select enl from ProgramEnrolment enl " +
            "join enl.individual i " +
            "where enl.program.uuid = :programUUID " +
            "and enl.isVoided = false " +
            "and i.isVoided = false " +
            "and coalesce(enl.enrolmentDateTime, enl.programExitDateTime) between :startDateTime and :endDateTime " +
            "and (coalesce(:locationIds, null) is null OR i.addressLevel.id in :locationIds)")
    Page<ProgramEnrolment> findEnrolments(String programUUID, List<Long> locationIds, DateTime startDateTime, DateTime endDateTime, Pageable pageable);

    //group by is added for distinct enl records
    @Query("select enl from ProgramEnrolment enl " +
            "join enl.programEncounters enc " +
            "join enl.individual i " +
            "where enc.encounterType.uuid = :encounterTypeUUID " +
            "and enl.program.uuid = :programUUID " +
            "and enc.isVoided = false " +
            "and enl.isVoided = false " +
            "and i.isVoided = false " +
            "and coalesce(enc.encounterDateTime, enc.cancelDateTime) between :startDateTime and :endDateTime " +
            "and (coalesce(:locationIds, null) is null OR i.addressLevel.id in :locationIds) " +
            "group by enl.id")
    Page<ProgramEnrolment> findProgramEncounters(List<Long> locationIds, DateTime startDateTime, DateTime endDateTime, String encounterTypeUUID, String programUUID, Pageable pageable);


    Page<ProgramEnrolment> findByAuditLastModifiedDateTimeGreaterThanAndAuditLastModifiedDateTimeLessThanAndProgramNameOrderByAuditLastModifiedDateTimeAscIdAsc(
            DateTime lastModifiedDateTime,
            DateTime now,
            String program,
            Pageable pageable);

    Page<ProgramEnrolment> findByProgramNameAndIndividualUuidOrderByAuditLastModifiedDateTimeAscIdAsc(
            String program,
            String individualUuid,
            Pageable pageable);

    ProgramEnrolment findByLegacyId(String legacyId);

    Page<ProgramEnrolment> findByAuditLastModifiedDateTimeGreaterThanAndAuditLastModifiedDateTimeLessThanOrderByAuditLastModifiedDateTimeAscIdAsc(
            @Param("lastModifiedDateTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime lastModifiedDateTime,
            @Param("now") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime now,
            Pageable pageable);

    @Override
    default Page<ProgramEnrolment> syncByCatchment(SyncParameters syncParameters) {
        return findByIndividualAddressInAndProgramIdAndAuditLastModifiedDateTimeIsBetweenOrderByAuditLastModifiedDateTimeAscIdAsc(syncParameters.getAddressLevels(), syncParameters.getFilter(), syncParameters.getLastModifiedDateTime(), syncParameters.getNow(), syncParameters.getPageable());
    }

    @Override
    default Page<ProgramEnrolment> syncByFacility(SyncParameters syncParameters) {
        return findByIndividualFacilityIdAndProgramIdAndAuditLastModifiedDateTimeIsBetweenOrderByAuditLastModifiedDateTimeAscIdAsc(syncParameters.getCatchmentId(), syncParameters.getFilter(), syncParameters.getLastModifiedDateTime(), syncParameters.getNow(), syncParameters.getPageable());
    }
}
