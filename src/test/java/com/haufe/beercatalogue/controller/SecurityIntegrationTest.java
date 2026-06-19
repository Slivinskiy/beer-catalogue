package com.haufe.beercatalogue.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.haufe.beercatalogue.domain.AppUser;
import com.haufe.beercatalogue.domain.Manufacturer;
import com.haufe.beercatalogue.domain.Role;
import com.haufe.beercatalogue.repository.AppUserRepository;
import com.haufe.beercatalogue.repository.BeerRepository;
import com.haufe.beercatalogue.repository.ManufacturerRepository;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ManufacturerRepository manufacturerRepository;

    @Autowired
    private BeerRepository beerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        appUserRepository.deleteAll();
        beerRepository.deleteAll();
        manufacturerRepository.deleteAll();
        createAdminUser();
    }

    @Test
    void shouldAllowAnonymousReadAccess() throws Exception {
        manufacturerRepository.save(new Manufacturer("BrewDog", "Scotland"));

        mockMvc.perform(get("/api/v1/manufacturers"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectAnonymousWriteAccess() throws Exception {
        final var request = """
                {
                  "name": "BrewDog",
                  "countryOfOrigin": "Scotland"
                }
                """;

        mockMvc.perform(post("/api/v1/manufacturers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowManufacturerUserToEditOwnManufacturer() throws Exception {
        final var manufacturer = manufacturerRepository.save(new Manufacturer("BrewDog", "Scotland"));
        createManufacturerUser("brewdog", "password", manufacturer.getId());

        final var request = """
                {
                  "name": "BrewDog Updated",
                  "countryOfOrigin": "Scotland"
                }
                """;

        mockMvc.perform(put("/api/v1/manufacturers/{id}", manufacturer.getId())
                        .with(httpBasic("brewdog", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("BrewDog Updated"));
    }

    @Test
    void shouldForbidManufacturerUserFromEditingOtherManufacturer() throws Exception {
        final var ownManufacturer = manufacturerRepository.save(new Manufacturer("BrewDog", "Scotland"));
        final var otherManufacturer = manufacturerRepository.save(new Manufacturer("Guinness", "Ireland"));
        createManufacturerUser("brewdog", "password", ownManufacturer.getId());

        final var request = """
                {
                  "name": "Guinness Updated",
                  "countryOfOrigin": "Ireland"
                }
                """;

        mockMvc.perform(put("/api/v1/manufacturers/{id}", otherManufacturer.getId())
                        .with(httpBasic("brewdog", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldAllowManufacturerUserToCreateBeerForOwnManufacturer() throws Exception {
        final var manufacturer = manufacturerRepository.save(new Manufacturer("BrewDog", "Scotland"));
        createManufacturerUser("brewdog", "password", manufacturer.getId());

        final var request = """
                {
                  "name": "Punk IPA",
                  "abv": 5.6,
                  "type": "IPA",
                  "description": "Classic IPA",
                  "manufacturerId": %d
                }
                """.formatted(manufacturer.getId());

        mockMvc.perform(post("/api/v1/beers")
                        .with(httpBasic("brewdog", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.manufacturerId").value(manufacturer.getId()));
    }

    @Test
    void shouldForbidManufacturerUserFromCreatingBeerForOtherManufacturer() throws Exception {
        final var ownManufacturer = manufacturerRepository.save(new Manufacturer("BrewDog", "Scotland"));
        final var otherManufacturer = manufacturerRepository.save(new Manufacturer("Guinness", "Ireland"));
        createManufacturerUser("brewdog", "password", ownManufacturer.getId());

        final var request = """
                {
                  "name": "Foreign Extra Stout",
                  "abv": 7.5,
                  "type": "STOUT",
                  "description": "Dark stout",
                  "manufacturerId": %d
                }
                """.formatted(otherManufacturer.getId());

        mockMvc.perform(post("/api/v1/beers")
                        .with(httpBasic("brewdog", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    private void createAdminUser() {
        final var adminUser = new AppUser(
                "admin",
                passwordEncoder.encode("admin123"),
                Role.ADMIN,
                null
        );
        appUserRepository.save(adminUser);
    }

    private void createManufacturerUser(final String username, final String password, final Long manufacturerId) {
        final var appUser = new AppUser(
                username,
                passwordEncoder.encode(password),
                Role.MANUFACTURER,
                manufacturerId
        );
        appUserRepository.save(appUser);
    }
}
