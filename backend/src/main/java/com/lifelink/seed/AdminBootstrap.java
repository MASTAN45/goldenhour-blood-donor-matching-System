package com.lifelink.seed;

import com.lifelink.entity.Role;
import com.lifelink.entity.User;
import com.lifelink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first admin from ADMIN_EMAIL and ADMIN_PASSWORD (at least 12 characters), if they are set. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Value("${lifelink.admin.email:}")
    private String email;
    @Value("${lifelink.admin.password:}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        String e = email.trim().toLowerCase();
        if (users.existsByEmail(e)) {
            return;
        }
        if (password.length() < 12) {
            log.error("ADMIN_PASSWORD must be at least 12 characters, admin account not created");
            return;
        }
        users.save(User.builder().email(e).phone("0000000000")
                .passwordHash(encoder.encode(password)).role(Role.ADMIN).build());
        log.info("Bootstrap admin created: {}", e);
    }
}