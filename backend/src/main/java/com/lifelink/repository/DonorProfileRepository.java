package com.lifelink.repository;

import com.lifelink.entity.BloodGroup;
import com.lifelink.entity.DonorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DonorProfileRepository extends JpaRepository<DonorProfile, Long> {

    Optional<DonorProfile> findByUser_Id(Long userId);

    /** First pass of geofencing: cheap bounding-box filter on indexed latitude/longitude columns. */
    @Query("select d from DonorProfile d join fetch d.user u "
            + "where d.bloodGroup in :groups and d.available = true and u.enabled = true "
            + "and d.latitude between :minLat and :maxLat "
            + "and d.longitude between :minLon and :maxLon")
    List<DonorProfile> findInBox(@Param("groups") Collection<BloodGroup> groups,
                                 @Param("minLat") double minLat, @Param("maxLat") double maxLat,
                                 @Param("minLon") double minLon, @Param("maxLon") double maxLon);
}