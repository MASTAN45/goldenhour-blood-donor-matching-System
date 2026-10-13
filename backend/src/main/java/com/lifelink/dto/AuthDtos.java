package com.lifelink.dto;

import com.lifelink.entity.BloodGroup;
import com.lifelink.entity.Gender;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record RegisterDonorRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Pattern(regexp = "^[0-9+\\- ]{8,20}$", message = "Invalid phone number") String phone,
            @NotBlank String fullName,
            @NotNull @Past LocalDate dateOfBirth,
            @NotNull Gender gender,
            @NotNull BloodGroup bloodGroup,
            @NotNull @DecimalMin("30.0") @DecimalMax("200.0") Double weightKg,
            boolean healthDeclarationOk,
            @PastOrPresent LocalDate lastDonationDate) {
    }

    public record RegisterHospitalRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Pattern(regexp = "^[0-9+\\- ]{8,20}$", message = "Invalid phone number") String phone,
            @NotBlank String name,
            @NotBlank String registrationNo,
            @NotBlank String address,
            @NotBlank String city,
            String state,
            @NotNull Double latitude,
            @NotNull Double longitude,
            String emergencyContact) {
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record AuthResponse(String accessToken, String refreshToken, Long userId, String email, String role) {
    }
}