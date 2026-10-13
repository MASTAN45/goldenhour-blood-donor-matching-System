package com.lifelink.controller;

import com.lifelink.dto.RequestDtos.*;
import com.lifelink.security.AuthUser;
import com.lifelink.service.AcceptService;
import com.lifelink.service.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
public class RequestController {
    private final RequestService requests;
    private final AcceptService accepts;

    @PostMapping
    @PreAuthorize("hasRole('REQUESTER')")
    @ResponseStatus(HttpStatus.CREATED)
    public MineView create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody CreateRequest body) {
        return requests.create(user.id(), body);
    }

    @GetMapping("/nearby")
    @PreAuthorize("hasRole('DONOR')")
    public List<NearbyView> nearby(@AuthenticationPrincipal AuthUser user) {
        return requests.nearbyFor(user.id());
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('REQUESTER')")
    public List<MineView> mine(@AuthenticationPrincipal AuthUser user) {
        return requests.mine(user.id());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('REQUESTER')")
    public TrackerView tracker(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return requests.tracker(id, user.id());
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('REQUESTER')")
    public MineView cancel(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return requests.cancel(id, user.id());
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('DONOR')")
    public AcceptService.AcceptResult accept(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return accepts.accept(id, user.id());
    }

    @PostMapping("/{id}/decline")
    @PreAuthorize("hasRole('DONOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decline(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        accepts.decline(id, user.id());
    }
}