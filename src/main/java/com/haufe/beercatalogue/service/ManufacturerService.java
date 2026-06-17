package com.haufe.beercatalogue.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.haufe.beercatalogue.domain.Manufacturer;
import com.haufe.beercatalogue.exception.NotFoundException;
import com.haufe.beercatalogue.repository.ManufacturerRepository;

@Service
@Transactional
public class ManufacturerService {
    private final ManufacturerRepository manufacturerRepository;

    public ManufacturerService(final ManufacturerRepository manufacturerRepository) {
        this.manufacturerRepository = manufacturerRepository;
    }

    @Transactional(readOnly = true)
    public List<Manufacturer> findAll() {
        return manufacturerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Manufacturer findById(final Long id) {
        return manufacturerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Manufacturer with id " + id + " not found"));
    }

    public Manufacturer create(final Manufacturer manufacturer) {
        return manufacturerRepository.save(manufacturer);
    }

    public Manufacturer update(final Long id, final Manufacturer manufacturer) {
        final var existingManufacturer = findById(id);
        existingManufacturer.setName(manufacturer.getName());
        existingManufacturer.setCountryOfOrigin(manufacturer.getCountryOfOrigin());
        return manufacturerRepository.save(existingManufacturer);
    }

    public void delete(final Long id) {
        final var manufacturer = findById(id);
        manufacturerRepository.delete(manufacturer);
    }
}
