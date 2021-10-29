package org.avni.dao;

import org.joda.time.DateTime;
import org.avni.application.Platform;
import org.avni.domain.Locale;
import org.avni.domain.PlatformTranslation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Repository;

@Repository
@RepositoryRestResource(collectionResourceRel = "platformTranslation", path = "platformTranslation", exported = false)
@PreAuthorize("hasAnyAuthority('user','admin')")
public interface PlatformTranslationRepository extends PagingAndSortingRepository<PlatformTranslation, Long> {

    @PreAuthorize("hasAnyAuthority('admin')")
    <S extends PlatformTranslation> S save(S entity);

    PlatformTranslation findByPlatformAndLanguage(Platform platform, Locale language);

    PlatformTranslation findByLanguage(Locale language);

    Page<PlatformTranslation> findByPlatformAndAuditLastModifiedDateTimeIsBetweenOrderByAuditLastModifiedDateTimeAscIdAsc(
            Platform platform,
            DateTime lastModifiedDateTime,
            DateTime now,
            Pageable pageable);

    boolean existsByPlatformAndAuditLastModifiedDateTimeGreaterThan(Platform platform, DateTime lastModifiedDateTime);

}
