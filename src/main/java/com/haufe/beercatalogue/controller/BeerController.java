package com.haufe.beercatalogue.controller;

import java.net.URI;
import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import com.haufe.beercatalogue.domain.BeerType;
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
    @Operation(summary = "List beers", description = "Returns beers with optional filtering and sorting.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Beers returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter, sort field, sort direction, or pagination")
    })
    public Page<BeerResponse> findAll(
            @RequestParam(required = false) final String name,
            @RequestParam(required = false) final BeerType type,
            @RequestParam(required = false) final BigDecimal abv,
            @RequestParam(required = false) final String manufacturer,
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "100") final int size,
            @RequestParam(defaultValue = "name") final String sortBy,
            @RequestParam(defaultValue = "asc") final String direction
    ) {
        validateSortField(sortBy);
        validatePagination(page, size);

        final var sortDirection = Sort.Direction.fromString(direction);
        final Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        return beerService.findAll(name, type, abv, manufacturer, pageable).map(BeerResponse::from);
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

    private void validatePagination(final int page, final int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be greater than or equal to 0");
        }

        if (size < 1) {
            throw new IllegalArgumentException("Size must be greater than 0");
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
