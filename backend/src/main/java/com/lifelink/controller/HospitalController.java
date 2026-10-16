package com.lifelink.controller;

import com.lifelink.dto.AdminDtos.HospitalSelfView;
import com.lifelink.entity.Hospital;
import com.lifelink.repository.HospitalRepository;
import com.lifelink.security.AuthUser;
import com.lifelink.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/hospitals")
@PreAuthorize("hasRole('REQUESTER')")
@RequiredArgsConstructor
public class HospitalController {
    private final HospitalRepository hospitals;

    /** The signed-in hospital's own status (PENDING, VERIFIED, REJECTED) and registered location. */
    @GetMapping("/me")
    public HospitalSelfView me(@AuthenticationPrincipal AuthUser user) {
        Hospital h = hospitals.findFirstByUser_Id(user.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hospital profile not found"));
        return new HospitalSelfView(h.getName(), h.getCity(), AdminService.statusOf(h), h.getRejectionReason(),
                h.getLatitude(), h.getLongitude());
    }
}