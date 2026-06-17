package com.haufe.beercatalogue.controller;

import static org.hamcrest.Matchers.hasSize;
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
import org.springframework.test.web.servlet.MockMvc;

import com.haufe.beercatalogue.domain.Manufacturer;
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

    @BeforeEach
    void setUp() {
        beerRepository.deleteAll();
        manufacturerRepository.deleteAll();
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createFirstBeerRequest))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Punk IPA"))
                .andExpect(jsonPath("$.manufacturerId").value(firstManufacturer.getId()));

        final var firstBeer = beerRepository.findAll().getFirst();

        mockMvc.perform(post("/api/v1/beers")
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
                        .param("sortBy", "name")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Foreign Extra Stout"))
                .andExpect(jsonPath("$[1].name").value("Punk IPA"));

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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstBeer.getId()))
                .andExpect(jsonPath("$.name").value("Black Heart"))
                .andExpect(jsonPath("$.manufacturerId").value(secondManufacturer.getId()));

        mockMvc.perform(delete("/api/v1/beers/{id}", firstBeer.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/beers/{id}", firstBeer.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Manufacturer with id 999 not found"));
    }
}
