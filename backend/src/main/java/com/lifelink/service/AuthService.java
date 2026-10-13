package com.lifelink.service;

import com.lifelink.dto.AuthDtos.*;
import com.lifelink.entity.*;
import com.lifelink.repository.*;
import com.lifelink.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final DonorProfileRepository donors;
    private final HospitalRepository hospitals;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final GeoService geo;

    @Value("${lifelink.hospital.auto-verify:true}")
    private boolean autoVerifyHospitals;

    @Transactional
    public AuthResponse registerDonor(RegisterDonorRequest r) {
        User u = createUser(r.email(), r.phone(), r.password(), Role.DONOR);
        donors.save(DonorProfile.builder()
                .user(u).fullName(r.fullName()).dateOfBirth(r.dateOfBirth())
                .gender(r.gender()).bloodGroup(r.bloodGroup()).weightKg(r.weightKg())
                .healthDeclarationOk(r.healthDeclarationOk())
                .lastDonationDate(r.lastDonationDate())
                .build());
        return issue(u);
    }

    @Transactional
    public AuthResponse registerHospital(RegisterHospitalRequest r) {
        if (!geo.isValidIndiaCoordinate(r.latitude(), r.longitude())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coordinates must be within India (lat 5-40, lon 65-100)");
        }
        if (hospitals.existsByRegistrationNo(r.registrationNo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registration number already exists");
        }
        User u = createUser(r.email(), r.phone(), r.password(), Role.REQUESTER);
        hospitals.save(Hospital.builder()
                .user(u).name(r.name()).registrationNo(r.registrationNo()).address(r.address())
                .city(r.city()).state(r.state()).latitude(r.latitude()).longitude(r.longitude())
                .phone(r.phone()).emergencyContact(r.emergencyContact())
                .verified(autoVerifyHospitals).build()); // demo mode auto-verifies; set lifelink.hospital.auto-verify=false in production
        return issue(u);
    }

    @Transactional
    public AuthResponse login(LoginRequest r) {
        User u = users.findByEmail(r.email().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!u.isEnabled() || !encoder.matches(r.password(), u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return issue(u);
    }

    @Transactional
    public AuthResponse refresh(String token) {
        RefreshToken rt = refreshTokens.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokens.delete(rt);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }
        User u = rt.getUser();
        return new AuthResponse(jwt.generateAccessToken(u), rt.getToken(), u.getId(), u.getEmail(), u.getRole().name());
    }

    @Transactional
    public void logout(String token) {
        refreshTokens.deleteByToken(token);
    }

    private User createUser(String email, String phone, String rawPassword, Role role) {
        String e = email.toLowerCase();
        if (users.existsByEmail(e)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        return users.save(User.builder().email(e).phone(phone)
                .passwordHash(encoder.encode(rawPassword)).role(role).build());
    }

    private AuthResponse issue(User u) {
        String token = UUID.randomUUID().toString();
        refreshTokens.save(RefreshToken.builder().user(u).token(token)
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(jwt.refreshExpiryMs()))).build());
        return new AuthResponse(jwt.generateAccessToken(u), token, u.getId(), u.getEmail(), u.getRole().name());
    }
}