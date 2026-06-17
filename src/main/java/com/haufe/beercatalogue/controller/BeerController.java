package com.haufe.beercatalogue.controller;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.haufe.beercatalogue.controller.dto.BeerRequest;
import com.haufe.beercatalogue.controller.dto.BeerResponse;
import com.haufe.beercatalogue.domain.Beer;
import com.haufe.beercatalogue.domain.Manufacturer;
import com.haufe.beercatalogue.service.BeerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/beers")
@Tag(name = "Beers", description = "Browse and manage beers.")
public class BeerController {
    private static final List<String> ALLOWED_SORT_FIELDS = List.of("name", "abv", "type");

    private final BeerService beerService;

    public BeerController(final BeerService beerService) {
        this.beerService = beerService;
    }

    @GetMapping
    @Operation(summary = "List beers", description = "Returns all beers with optional sorting.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Beers returned successfully"),
            @ApiResponse(responseCode = "400", description = "Unsupported sort field or direction")
    })
    public List<BeerResponse> findAll(
            @RequestParam(defaultValue = "name") final String sortBy,
            @RequestParam(defaultValue = "asc") final String direction
    ) {
        validateSortField(sortBy);

        final var sortDirection = Sort.Direction.fromString(direction);
        final var sort = Sort.by(sortDirection, sortBy);

        return beerService.findAll(sort).stream()
                .map(BeerResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get beer details", description = "Returns a single beer by id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Beer returned successfully"),
            @ApiResponse(responseCode = "404", description = "Beer not found")
    })
    public BeerResponse findById(@PathVariable final Long id) {
        return BeerResponse.from(beerService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Create beer", description = "Creates a new beer linked to a manufacturer.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Beer created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Referenced manufacturer not found")
    })
    public ResponseEntity<BeerResponse> create(@Valid @RequestBody final BeerRequest request) {
        final var beer = toBeer(request);
        final var createdBeer = beerService.create(beer);
        final var response = BeerResponse.from(createdBeer);

        return ResponseEntity
                .created(URI.create("/api/v1/beers/" + response.id()))
                .body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update beer", description = "Updates an existing beer.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Beer updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Beer or manufacturer not found")
    })
    public BeerResponse update(@PathVariable final Long id, @Valid @RequestBody final BeerRequest request) {
        final var beer = toBeer(request);
        final var updatedBeer = beerService.update(id, beer);
        return BeerResponse.from(updatedBeer);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete beer", description = "Deletes a beer by id.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Beer deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Beer not found")
    })
    public ResponseEntity<Void> delete(@PathVariable final Long id) {
        beerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void validateSortField(final String sortBy) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Unsupported sort field: " + sortBy);
        }
    }

    private Beer toBeer(final BeerRequest request) {
        final var manufacturer = new Manufacturer(null, null);
        manufacturer.setId(request.manufacturerId());

        return new Beer(
                request.name(),
                request.abv(),
                request.type(),
                request.description(),
                manufacturer
        );
    }
}
