package com.haufe.beercatalogue.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.haufe.beercatalogue.domain.Manufacturer;
import com.haufe.beercatalogue.exception.ConflictException;
import com.haufe.beercatalogue.exception.NotFoundException;
import com.haufe.beercatalogue.repository.AppUserRepository;
import com.haufe.beercatalogue.repository.BeerRepository;
import com.haufe.beercatalogue.repository.ManufacturerRepository;

@Service
@Transactional
public class ManufacturerService {
    private final ManufacturerRepository manufacturerRepository;
    private final BeerRepository beerRepository;
    private final AppUserRepository appUserRepository;
    private final AccessService accessService;

    public ManufacturerService(
            final ManufacturerRepository manufacturerRepository,
            final BeerRepository beerRepository,
            final AppUserRepository appUserRepository,
            final AccessService accessService
    ) {
        this.manufacturerRepository = manufacturerRepository;
        this.beerRepository = beerRepository;
        this.appUserRepository = appUserRepository;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public Page<Manufacturer> findAll(final Pageable pageable) {
        return manufacturerRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Manufacturer findById(final Long id) {
        return manufacturerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Manufacturer with id " + id + " not found"));
    }

    public Manufacturer create(final Manufacturer manufacturer) {
        accessService.requireAdmin();
        return manufacturerRepository.save(manufacturer);
    }

    public Manufacturer update(final Long id, final Manufacturer manufacturer) {
        accessService.requireAdminOrOwnManufacturer(id);
        final var existingManufacturer = findById(id);
        existingManufacturer.setName(manufacturer.getName());
        existingManufacturer.setCountryOfOrigin(manufacturer.getCountryOfOrigin());
        return manufacturerRepository.save(existingManufacturer);
    }

    public void delete(final Long id) {
        accessService.requireAdminOrOwnManufacturer(id);
        final var manufacturer = findById(id);
        if (beerRepository.existsByManufacturer_Id(id)) {
            throw new ConflictException("Manufacturer with id " + id + " cannot be deleted because beers are linked to it");
        }
        if (appUserRepository.existsByManufacturer_Id(id)) {
            throw new ConflictException("Manufacturer with id " + id + " cannot be deleted because users are linked to it");
        }
        manufacturerRepository.delete(manufacturer);
    }
}
