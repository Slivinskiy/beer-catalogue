package com.haufe.beercatalogue.service;

import java.math.BigDecimal;
import java.io.IOException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.haufe.beercatalogue.domain.Beer;
import com.haufe.beercatalogue.domain.BeerType;
import com.haufe.beercatalogue.domain.Manufacturer;
import com.haufe.beercatalogue.exception.NotFoundException;
import com.haufe.beercatalogue.repository.BeerRepository;
import com.haufe.beercatalogue.repository.ManufacturerRepository;

@Service
@Transactional
public class BeerService {
    private static final List<String> ALLOWED_IMAGE_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final BeerRepository beerRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final AccessService accessService;

    public BeerService(
            final BeerRepository beerRepository,
            final ManufacturerRepository manufacturerRepository,
            final AccessService accessService
    ) {
        this.beerRepository = beerRepository;
        this.manufacturerRepository = manufacturerRepository;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public Page<Beer> findAll(
            final String name,
            final BeerType type,
            final BigDecimal abv,
            final String manufacturer,
            final Pageable pageable
    ) {
        final var specification = buildSpecification(name, type, abv, manufacturer);
        return beerRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public Beer findById(final Long id) {
        return beerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Beer with id " + id + " not found"));
    }

    public Beer create(final Beer beer) {
        beer.setManufacturer(getManufacturer(beer));
        accessService.requireAdminOrOwnManufacturer(beer.getManufacturer().getId());
        return beerRepository.save(beer);
    }

    public Beer update(final Long id, final Beer beer) {
        final var existingBeer = findById(id);
        accessService.requireAdminOrOwnManufacturer(existingBeer.getManufacturer().getId());
        existingBeer.setName(beer.getName());
        existingBeer.setAbv(beer.getAbv());
        existingBeer.setType(beer.getType());
        existingBeer.setDescription(beer.getDescription());
        existingBeer.setManufacturer(getManufacturer(beer));
        accessService.requireAdminOrOwnManufacturer(existingBeer.getManufacturer().getId());
        return beerRepository.save(existingBeer);
    }

    public void delete(final Long id) {
        final var beer = findById(id);
        accessService.requireAdminOrOwnManufacturer(beer.getManufacturer().getId());
        beerRepository.delete(beer);
    }

    public void uploadImage(final Long id, final MultipartFile file) {
        validateImage(file);

        final var beer = findById(id);
        accessService.requireAdminOrOwnManufacturer(beer.getManufacturer().getId());
        beer.setImage(getImageBytes(file));
        beer.setImageContentType(file.getContentType());
        beerRepository.save(beer);
    }

    @Transactional(readOnly = true)
    public BeerImageData getImage(final Long id) {
        final var beer = findById(id);

        if (beer.getImage() == null || beer.getImageContentType() == null) {
            throw new NotFoundException("Image for beer with id " + id + " not found");
        }

        return new BeerImageData(beer.getImage(), beer.getImageContentType());
    }

    private Manufacturer getManufacturer(final Beer beer) {
        if (beer.getManufacturer() == null || beer.getManufacturer().getId() == null) {
            throw new IllegalArgumentException("Beer manufacturer id is required");
        }

        final var manufacturerId = beer.getManufacturer().getId();
        return manufacturerRepository.findById(manufacturerId)
                .orElseThrow(() -> new NotFoundException("Manufacturer with id " + manufacturerId + " not found"));
    }

    private Specification<Beer> buildSpecification(
            final String name,
            final BeerType type,
            final BigDecimal abv,
            final String manufacturer
    ) {
        Specification<Beer> specification = Specification.unrestricted();

        if (name != null && !name.isBlank()) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("name")),
                            "%" + name.toLowerCase() + "%"
                    )
            );
        }

        if (type != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("type"), type)
            );
        }

        if (abv != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("abv"), abv)
            );
        }

        if (manufacturer != null && !manufacturer.isBlank()) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.join("manufacturer").get("name")),
                            "%" + manufacturer.toLowerCase() + "%"
                    )
            );
        }

        return specification;
    }

    private void validateImage(final MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        if (file.getContentType() == null || !ALLOWED_IMAGE_CONTENT_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Unsupported image content type");
        }
    }

    private byte[] getImageBytes(final MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not read image file");
        }
    }
}
