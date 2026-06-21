package com.haufe.beercatalogue.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import com.haufe.beercatalogue.domain.Beer;
import com.haufe.beercatalogue.domain.BeerType;
import com.haufe.beercatalogue.domain.Manufacturer;
import com.haufe.beercatalogue.domain.Role;
import com.haufe.beercatalogue.repository.AppUserRepository;
import com.haufe.beercatalogue.repository.BeerRepository;
import com.haufe.beercatalogue.repository.ManufacturerRepository;

import java.math.BigDecimal;

@SpringBootTest
@AutoConfigureMockMvc
class ManufacturerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ManufacturerRepository manufacturerRepository;

    @Autowired
    private BeerRepository beerRepository;

    @Autowired
    private AppUserRepository appUserRepository;

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
    void shouldPerformManufacturerCrudFlow() throws Exception {
        final var createRequest = """
                {
                  "name": "BrewDog",
                  "countryOfOrigin": "Scotland"
                }
                """;

        mockMvc.perform(post("/api/v1/manufacturers")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("BrewDog"))
                .andExpect(jsonPath("$.countryOfOrigin").value("Scotland"));

        final var createdManufacturer = manufacturerRepository.findAll().getFirst();

        mockMvc.perform(get("/api/v1/manufacturers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("BrewDog"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        mockMvc.perform(get("/api/v1/manufacturers/{id}", createdManufacturer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdManufacturer.getId()))
                .andExpect(jsonPath("$.name").value("BrewDog"));

        final var updateRequest = """
                {
                  "name": "Guinness",
                  "countryOfOrigin": "Ireland"
                }
                """;

        mockMvc.perform(put("/api/v1/manufacturers/{id}", createdManufacturer.getId())
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdManufacturer.getId()))
                .andExpect(jsonPath("$.name").value("Guinness"))
                .andExpect(jsonPath("$.countryOfOrigin").value("Ireland"));

        mockMvc.perform(delete("/api/v1/manufacturers/{id}", createdManufacturer.getId())
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/manufacturers/{id}", createdManufacturer.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturnBadRequestForInvalidManufacturer() throws Exception {
        final var request = """
                {
                  "name": "",
                  "countryOfOrigin": ""
                }
                """;

        mockMvc.perform(post("/api/v1/manufacturers")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.countryOfOrigin").exists());
    }

    @Test
    void shouldReturnBadRequestWhenManufacturerPageSizeExceedsMaximum() throws Exception {
        mockMvc.perform(get("/api/v1/manufacturers")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Size must be less than or equal to 100"));
    }

    @Test
    void shouldReturnConflictWhenDeletingManufacturerWithBeers() throws Exception {
        final var manufacturer = manufacturerRepository.save(new Manufacturer("BrewDog", "Scotland"));
        beerRepository.save(new Beer(
                "Punk IPA",
                new BigDecimal("5.60"),
                BeerType.IPA,
                "Classic IPA",
                manufacturer
        ));

        mockMvc.perform(delete("/api/v1/manufacturers/{id}", manufacturer.getId())
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldReturnConflictWhenDeletingManufacturerWithUsers() throws Exception {
        final var manufacturer = manufacturerRepository.save(new Manufacturer("BrewDog", "Scotland"));
        appUserRepository.save(new AppUser(
                "brewdog",
                passwordEncoder.encode("password"),
                Role.MANUFACTURER,
                manufacturer
        ));

        mockMvc.perform(delete("/api/v1/manufacturers/{id}", manufacturer.getId())
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
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
}
