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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/manufacturers")
@Tag(name = "Manufacturers", description = "Browse and manage beer manufacturers.")
public class ManufacturerController {
    private final ManufacturerService manufacturerService;

    public ManufacturerController(final ManufacturerService manufacturerService) {
        this.manufacturerService = manufacturerService;
    }

    @GetMapping
    @Operation(summary = "List manufacturers", description = "Returns all manufacturers.")
    @ApiResponse(responseCode = "200", description = "Manufacturers returned successfully")
    public List<ManufacturerResponse> findAll() {
        return manufacturerService.findAll().stream()
                .map(ManufacturerResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get manufacturer details", description = "Returns a single manufacturer by id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Manufacturer returned successfully"),
            @ApiResponse(responseCode = "404", description = "Manufacturer not found")
    })
    public ManufacturerResponse findById(@PathVariable final Long id) {
        return ManufacturerResponse.from(manufacturerService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Create manufacturer", description = "Creates a new manufacturer.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Manufacturer created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<ManufacturerResponse> create(@Valid @RequestBody final ManufacturerRequest request) {
        final var manufacturer = new Manufacturer(request.name(), request.countryOfOrigin());
        final var createdManufacturer = manufacturerService.create(manufacturer);
        final var response = ManufacturerResponse.from(createdManufacturer);

        return ResponseEntity
                .created(URI.create("/api/v1/manufacturers/" + response.id()))
                .body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update manufacturer", description = "Updates an existing manufacturer.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Manufacturer updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Manufacturer not found")
    })
    public ManufacturerResponse update(@PathVariable final Long id, @Valid @RequestBody final ManufacturerRequest request) {
        final var manufacturer = new Manufacturer(request.name(), request.countryOfOrigin());
        final var updatedManufacturer = manufacturerService.update(id, manufacturer);
        return ManufacturerResponse.from(updatedManufacturer);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete manufacturer", description = "Deletes a manufacturer by id.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Manufacturer deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Manufacturer not found")
    })
    public ResponseEntity<Void> delete(@PathVariable final Long id) {
        manufacturerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
