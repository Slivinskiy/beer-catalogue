package com.haufe.beercatalogue.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.haufe.beercatalogue.controller.dto.ManufacturerRequest;
import com.haufe.beercatalogue.controller.dto.ManufacturerResponse;
import com.haufe.beercatalogue.domain.Manufacturer;
import com.haufe.beercatalogue.service.ManufacturerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/manufacturers")
public class ManufacturerController {
    private final ManufacturerService manufacturerService;

    public ManufacturerController(final ManufacturerService manufacturerService) {
        this.manufacturerService = manufacturerService;
    }

    @GetMapping
    public List<ManufacturerResponse> findAll() {
        return manufacturerService.findAll().stream()
                .map(ManufacturerResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ManufacturerResponse findById(@PathVariable final Long id) {
        return ManufacturerResponse.from(manufacturerService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ManufacturerResponse> create(@Valid @RequestBody final ManufacturerRequest request) {
        final var manufacturer = new Manufacturer(request.name(), request.countryOfOrigin());
        final var createdManufacturer = manufacturerService.create(manufacturer);
        final var response = ManufacturerResponse.from(createdManufacturer);

        return ResponseEntity
                .created(URI.create("/api/v1/manufacturers/" + response.id()))
                .body(response);
    }

    @PutMapping("/{id}")
    public ManufacturerResponse update(
            @PathVariable final Long id,
            @Valid @RequestBody final ManufacturerRequest request
    ) {
        final var manufacturer = new Manufacturer(request.name(), request.countryOfOrigin());
        final var updatedManufacturer = manufacturerService.update(id, manufacturer);
        return ManufacturerResponse.from(updatedManufacturer);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable final Long id) {
        manufacturerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
