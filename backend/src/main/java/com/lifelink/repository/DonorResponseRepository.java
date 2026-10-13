package com.lifelink.repository;

import com.lifelink.entity.DonorResponse;
import com.lifelink.entity.DonorResponseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DonorResponseRepository extends JpaRepository<DonorResponse, Long> {

    Optional<DonorResponse> findByRequest_IdAndDonor_Id(Long requestId, Long donorId);

    long countByRequest_IdAndStatus(Long requestId, DonorResponseStatus status);

    List<DonorResponse> findByRequest_IdOrderByIdAsc(Long requestId);

    List<DonorResponse> findByDonor_IdAndStatusInOrderByIdDesc(Long donorId, Collection<DonorResponseStatus> statuses);

    @Query("select dr.donor.id from DonorResponse dr where dr.request.id = :rid and dr.status = :status")
    List<Long> findDonorIdsByStatus(@Param("rid") Long requestId, @Param("status") DonorResponseStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE donor_responses SET status = 'ACCEPTED', responded_at = :now "
            + "WHERE request_id = :rid AND donor_id = :did AND status = 'NOTIFIED'", nativeQuery = true)
    int markAccepted(@Param("rid") Long requestId, @Param("did") Long donorId, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE donor_responses SET status = 'DECLINED', responded_at = :now "
            + "WHERE request_id = :rid AND donor_id = :did AND status = 'NOTIFIED'", nativeQuery = true)
    int markDeclined(@Param("rid") Long requestId, @Param("did") Long donorId, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE donor_responses SET status = 'AUTO_CLOSED', responded_at = :now "
            + "WHERE request_id = :rid AND status = 'NOTIFIED'", nativeQuery = true)
    int closeOthers(@Param("rid") Long requestId, @Param("now") LocalDateTime now);
}