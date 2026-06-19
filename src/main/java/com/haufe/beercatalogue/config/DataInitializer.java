package com.haufe.beercatalogue.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.haufe.beercatalogue.domain.AppUser;
import com.haufe.beercatalogue.domain.Role;
import com.haufe.beercatalogue.repository.AppUserRepository;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initializeUsers(
            final AppUserRepository appUserRepository,
            final PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (appUserRepository.findByUsername("admin").isEmpty()) {
                final var adminUser = new AppUser(
                        "admin",
                        passwordEncoder.encode("admin123"),
                        Role.ADMIN,
                        null
                );
                appUserRepository.save(adminUser);
            }
        };
    }
}
