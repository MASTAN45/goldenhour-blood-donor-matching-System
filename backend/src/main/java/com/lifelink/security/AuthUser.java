package com.lifelink.security;

/** The authenticated principal placed in the SecurityContext for each request. */
public record AuthUser(Long id, String email, String role) {
}