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

import com.haufe.beercatalogue.repository.BeerRepository;
import com.haufe.beercatalogue.repository.ManufacturerRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ManufacturerControllerIntegrationTest {

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
    void shouldPerformManufacturerCrudFlow() throws Exception {
        final var createRequest = """
                {
                  "name": "BrewDog",
                  "countryOfOrigin": "Scotland"
                }
                """;

        mockMvc.perform(post("/api/v1/manufacturers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("BrewDog"))
                .andExpect(jsonPath("$.countryOfOrigin").value("Scotland"));

        final var createdManufacturer = manufacturerRepository.findAll().getFirst();

        mockMvc.perform(get("/api/v1/manufacturers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("BrewDog"));

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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdManufacturer.getId()))
                .andExpect(jsonPath("$.name").value("Guinness"))
                .andExpect(jsonPath("$.countryOfOrigin").value("Ireland"));

        mockMvc.perform(delete("/api/v1/manufacturers/{id}", createdManufacturer.getId()))
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.countryOfOrigin").exists());
    }
}
