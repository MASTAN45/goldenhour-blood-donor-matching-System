package com.lifelink.controller;

import com.lifelink.dto.AuthDtos.*;
import com.lifelink.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;

    @PostMapping("/register/donor")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerDonor(@Valid @RequestBody RegisterDonorRequest body) {
        return auth.registerDonor(body);
    }

    @PostMapping("/register/hospital")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerHospital(@Valid @RequestBody RegisterHospitalRequest body) {
        return auth.registerHospital(body);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest body) {
        return auth.login(body);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest body) {
        return auth.refresh(body.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest body) {
        auth.logout(body.refreshToken());
    }
}