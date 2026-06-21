package com.haufe.beercatalogue.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.haufe.beercatalogue.domain.AppUser;
import com.haufe.beercatalogue.domain.Manufacturer;
import com.haufe.beercatalogue.domain.Role;
import com.haufe.beercatalogue.repository.AppUserRepository;
import com.haufe.beercatalogue.repository.ManufacturerRepository;

public class DataInitializer implements ApplicationRunner {
    private static final String GUINNESS = "Guinness";
    private static final String STONE_BREWING = "Stone Brewing";

    private final ManufacturerRepository manufacturerRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            final ManufacturerRepository manufacturerRepository,
            final AppUserRepository appUserRepository,
            final PasswordEncoder passwordEncoder
    ) {
        this.manufacturerRepository = manufacturerRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(final ApplicationArguments args) {
        seedManufacturers();
        seedUsers();
    }

    private void seedManufacturers() {
        if (manufacturerRepository.count() > 0) {
            return;
        }

        final var manufacturers = List.of(
                new Manufacturer("BrewDog", "Scotland"),
                new Manufacturer("Guinness", "Ireland"),
                new Manufacturer("Paulaner", "Germany"),
                new Manufacturer("Pilsner Urquell", "Czech Republic"),
                new Manufacturer("Sierra Nevada", "United States"),
                new Manufacturer("Leffe", "Belgium"),
                new Manufacturer("Stone Brewing", "United States"),
                new Manufacturer("Asahi", "Japan"),
                new Manufacturer("Corona", "Mexico"),
                new Manufacturer("Hoegaarden", "Belgium")
        );

        manufacturerRepository.saveAll(manufacturers);
    }

    private void seedUsers() {
        createUserIfMissing("admin", "admin123", Role.ADMIN, null);
        manufacturerRepository.findByName(GUINNESS)
                .ifPresent(manufacturer -> createUserIfMissing("manufacturer2", "manufacturer2123", Role.MANUFACTURER, manufacturer));
        manufacturerRepository.findByName(STONE_BREWING)
                .ifPresent(manufacturer -> createUserIfMissing("manufacturer7", "manufacturer7123", Role.MANUFACTURER, manufacturer));
    }

    private void createUserIfMissing(
            final String username,
            final String password,
            final Role role,
            final Manufacturer manufacturer
    ) {
        if (appUserRepository.findByUsername(username).isPresent()) {
            return;
        }

        final var user = new AppUser(
                username,
                passwordEncoder.encode(password),
                role,
                manufacturer
        );
        appUserRepository.save(user);
    }
}
