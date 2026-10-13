package com.lifelink.repository;

import com.lifelink.entity.BloodRequest;
import com.lifelink.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long> {

    List<BloodRequest> findByRequester_IdOrderByCreatedAtDesc(Long requesterId);

    List<BloodRequest> findByStatusInAndExpiresAtAfter(Collection<RequestStatus> statuses, LocalDateTime now);

    /**
     * Atomically claims one unit. The database is the final arbiter: the WHERE clause
     * guarantees units_accepted never exceeds units_needed, even with many concurrent callers.
     * MySQL evaluates SET assignments left to right, so status is set BEFORE units_accepted is incremented.
     * Returns 1 if a slot was claimed, 0 if the request is full, closed or expired.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE blood_requests "
            + "SET status = CASE WHEN units_accepted + 1 >= units_needed THEN 'FILLED' ELSE 'PARTIALLY_FILLED' END, "
            + "units_accepted = units_accepted + 1, "
            + "version = version + 1 "
            + "WHERE id = :id AND units_accepted < units_needed "
            + "AND status IN ('OPEN','PARTIALLY_FILLED') AND expires_at > :now",
            nativeQuery = true)
    int claimSlot(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE blood_requests SET status = 'EXPIRED', version = version + 1 "
            + "WHERE status IN ('OPEN','PARTIALLY_FILLED') AND expires_at <= :now", nativeQuery = true)
    int expireOverdue(@Param("now") LocalDateTime now);
}