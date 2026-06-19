package com.haufe.beercatalogue.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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

@SpringBootTest
@AutoConfigureMockMvc
class BeerControllerIntegrationTest {

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
    void shouldPerformBeerCrudFlowAndSortByName() throws Exception {
        final var firstManufacturer = manufacturerRepository.save(new Manufacturer("BrewDog", "Scotland"));
        final var secondManufacturer = manufacturerRepository.save(new Manufacturer("Guinness", "Ireland"));

        final var createFirstBeerRequest = """
                {
                  "name": "Punk IPA",
                  "abv": 5.6,
                  "type": "IPA",
                  "description": "Classic IPA",
                  "manufacturerId": %d
                }
                """.formatted(firstManufacturer.getId());

        final var createSecondBeerRequest = """
                {
                  "name": "Foreign Extra Stout",
                  "abv": 7.5,
                  "type": "STOUT",
                  "description": "Dark stout",
                  "manufacturerId": %d
                }
                """.formatted(secondManufacturer.getId());

        mockMvc.perform(post("/api/v1/beers")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createFirstBeerRequest))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Punk IPA"))
                .andExpect(jsonPath("$.manufacturerId").value(firstManufacturer.getId()));

        final var firstBeer = beerRepository.findAll().getFirst();

        mockMvc.perform(post("/api/v1/beers")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createSecondBeerRequest))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Foreign Extra Stout"))
                .andExpect(jsonPath("$.manufacturerId").value(secondManufacturer.getId()));

        mockMvc.perform(get("/api/v1/beers/{id}", firstBeer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstBeer.getId()))
                .andExpect(jsonPath("$.name").value("Punk IPA"));

        mockMvc.perform(get("/api/v1/beers")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "name")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name").value("Foreign Extra Stout"))
                .andExpect(jsonPath("$.content[1].name").value("Punk IPA"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        final var updateRequest = """
                {
                  "name": "Black Heart",
                  "abv": 4.1,
                  "type": "STOUT",
                  "description": "Smooth stout",
                  "manufacturerId": %d
                }
                """.formatted(secondManufacturer.getId());

        mockMvc.perform(put("/api/v1/beers/{id}", firstBeer.getId())
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstBeer.getId()))
                .andExpect(jsonPath("$.name").value("Black Heart"))
                .andExpect(jsonPath("$.manufacturerId").value(secondManufacturer.getId()));

        mockMvc.perform(delete("/api/v1/beers/{id}", firstBeer.getId())
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/beers/{id}", firstBeer.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldFilterBeersByNameTypeAbvAndManufacturer() throws Exception {
        final var brewdog = manufacturerRepository.save(new Manufacturer("BrewDog", "Scotland"));
        final var guinness = manufacturerRepository.save(new Manufacturer("Guinness", "Ireland"));

        beerRepository.save(new Beer(
                "Punk IPA",
                new BigDecimal("5.60"),
                BeerType.IPA,
                "Classic IPA",
                brewdog
        ));
        beerRepository.save(new Beer(
                "Hazy Jane",
                new BigDecimal("5.00"),
                BeerType.IPA,
                "Hazy IPA",
                brewdog
        ));
        beerRepository.save(new Beer(
                "Foreign Extra Stout",
                new BigDecimal("7.50"),
                BeerType.STOUT,
                "Dark stout",
                guinness
        ));

        mockMvc.perform(get("/api/v1/beers")
                        .param("name", "punk")
                        .param("type", "IPA")
                        .param("abv", "5.60")
                        .param("manufacturer", "brew")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "name")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Punk IPA"))
                .andExpect(jsonPath("$.content[0].manufacturerId").value(brewdog.getId()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void shouldReturnBadRequestForInvalidBeer() throws Exception {
        final var request = """
                {
                  "name": "",
                  "abv": 101.0,
                  "type": null,
                  "description": "",
                  "manufacturerId": null
                }
                """;

        mockMvc.perform(post("/api/v1/beers")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.abv").exists())
                .andExpect(jsonPath("$.validationErrors.type").exists())
                .andExpect(jsonPath("$.validationErrors.description").exists())
                .andExpect(jsonPath("$.validationErrors.manufacturerId").exists());
    }

    @Test
    void shouldReturnNotFoundWhenBeerManufacturerDoesNotExist() throws Exception {
        final var request = """
                {
                  "name": "Punk IPA",
                  "abv": 5.6,
                  "type": "IPA",
                  "description": "Classic IPA",
                  "manufacturerId": 999
                }
                """;

        mockMvc.perform(post("/api/v1/beers")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Manufacturer with id 999 not found"));
    }

    @Test
    void shouldUploadAndReturnBeerImage() throws Exception {
        final var manufacturer = manufacturerRepository.save(new Manufacturer("BrewDog", "Scotland"));
        final var beer = beerRepository.save(new Beer(
                "Punk IPA",
                new BigDecimal("5.60"),
                BeerType.IPA,
                "Classic IPA",
                manufacturer
        ));
        final var file = new MockMultipartFile("file", "punk.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/beers/{id}/image", beer.getId())
                        .file(file)
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/beers/{id}/image", beer.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void shouldReturnNotFoundWhenBeerImageDoesNotExist() throws Exception {
        final var manufacturer = manufacturerRepository.save(new Manufacturer("BrewDog", "Scotland"));
        final var beer = beerRepository.save(new Beer(
                "Punk IPA",
                new BigDecimal("5.60"),
                BeerType.IPA,
                "Classic IPA",
                manufacturer
        ));

        mockMvc.perform(get("/api/v1/beers/{id}/image", beer.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Image for beer with id " + beer.getId() + " not found"));
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
