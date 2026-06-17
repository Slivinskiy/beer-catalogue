package com.haufe.beercatalogue.controller;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import com.haufe.beercatalogue.controller.dto.BeerRequest;
import com.haufe.beercatalogue.controller.dto.BeerResponse;
import com.haufe.beercatalogue.domain.Beer;
import com.haufe.beercatalogue.domain.Manufacturer;
import com.haufe.beercatalogue.service.BeerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/beers")
public class BeerController {
    private static final List<String> ALLOWED_SORT_FIELDS = List.of("name", "abv", "type");

    private final BeerService beerService;

    public BeerController(final BeerService beerService) {
        this.beerService = beerService;
    }

    @GetMapping
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
    public BeerResponse findById(@PathVariable final Long id) {
        return BeerResponse.from(beerService.findById(id));
    }

    @PostMapping
    public ResponseEntity<BeerResponse> create(@Valid @RequestBody final BeerRequest request) {
        final var beer = toBeer(request);
        final var createdBeer = beerService.create(beer);
        final var response = BeerResponse.from(createdBeer);

        return ResponseEntity
                .created(URI.create("/api/v1/beers/" + response.id()))
                .body(response);
    }

    @PutMapping("/{id}")
    public BeerResponse update(
            @PathVariable final Long id,
            @Valid @RequestBody final BeerRequest request
    ) {
        final var beer = toBeer(request);
        final var updatedBeer = beerService.update(id, beer);
        return BeerResponse.from(updatedBeer);
    }

    @DeleteMapping("/{id}")
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
