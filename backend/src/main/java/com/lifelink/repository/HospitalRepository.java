package com.lifelink.repository;

import com.lifelink.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    Optional<Hospital> findFirstByUser_Id(Long userId);
    boolean existsByRegistrationNo(String registrationNo);
}