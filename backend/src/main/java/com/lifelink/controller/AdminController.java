package com.lifelink.controller;

import com.lifelink.dto.AdminDtos.HospitalAdminView;
import com.lifelink.dto.AdminDtos.RejectRequest;
import com.lifelink.dto.AdminDtos.Stats;
import com.lifelink.realtime.RealtimeEvent;
import com.lifelink.realtime.RealtimePublisher;
import com.lifelink.security.AuthUser;
import com.lifelink.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService admin;
    private final RealtimePublisher publisher;

    /** status = PENDING (default), VERIFIED, REJECTED or ALL. */
    @GetMapping("/hospitals")
    public List<HospitalAdminView> hospitals(@RequestParam(defaultValue = "PENDING") String status) {
        return admin.list(status);
    }

    @PostMapping("/hospitals/{id}/verify")
    public HospitalAdminView verify(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        HospitalAdminView v = admin.verify(id, user.id());
        publisher.toUser(v.userId(), RealtimeEvent.of("HOSPITAL_VERIFIED", null, "Hospital verified",
                "You can now post urgent blood requests.", null));
        return v;
    }

    @PostMapping("/hospitals/{id}/reject")
    public HospitalAdminView reject(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                    @Valid @RequestBody RejectRequest body) {
        HospitalAdminView v = admin.reject(id, user.id(), body.reason());
        publisher.toUser(v.userId(), RealtimeEvent.of("HOSPITAL_REJECTED", null, "Verification declined",
                body.reason().trim(), null));
        return v;
    }

    @GetMapping("/stats")
    public Stats stats() {
        return admin.stats();
    }
}