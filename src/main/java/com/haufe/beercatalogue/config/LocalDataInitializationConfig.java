package com.haufe.beercatalogue.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.haufe.beercatalogue.repository.AppUserRepository;
import com.haufe.beercatalogue.repository.ManufacturerRepository;

@Configuration
@Profile("local")
public class LocalDataInitializationConfig {

    @Bean
    @ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
    public ApplicationRunner dataInitializer(
            final ManufacturerRepository manufacturerRepository,
            final AppUserRepository appUserRepository,
            final PasswordEncoder passwordEncoder
    ) {
        return new DataInitializer(
                manufacturerRepository,
                appUserRepository,
                passwordEncoder
        );
    }
}
