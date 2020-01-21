package org.openchs.dao;

import org.openchs.dao.FindByLastModifiedDateTime;
import org.openchs.dao.ImplReferenceDataRepository;
import org.openchs.domain.Video;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Repository;

@Repository
@RepositoryRestResource(collectionResourceRel = "video", path = "video")
@PreAuthorize(value = "hasAnyAuthority('user', 'organisation_admin')")
public interface VideoRepository extends ImplReferenceDataRepository<Video>, FindByLastModifiedDateTime<Video> {

    Video findByTitle(String title);

    Video findByTitleIgnoreCase(String title);

    @Override
    default Video findByName(String name) {
        return findByTitle(name);
    }

    @Override
    default Video findByNameIgnoreCase(String name) {
        return findByTitleIgnoreCase(name);
    }
}
