package com.haufe.beercatalogue.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.haufe.beercatalogue.domain.Beer;
import com.haufe.beercatalogue.domain.Manufacturer;
import com.haufe.beercatalogue.exception.NotFoundException;
import com.haufe.beercatalogue.repository.BeerRepository;
import com.haufe.beercatalogue.repository.ManufacturerRepository;

@Service
@Transactional
public class BeerService {
    private final BeerRepository beerRepository;
    private final ManufacturerRepository manufacturerRepository;

    public BeerService(final BeerRepository beerRepository, final ManufacturerRepository manufacturerRepository) {
        this.beerRepository = beerRepository;
        this.manufacturerRepository = manufacturerRepository;
    }

    @Transactional(readOnly = true)
    public List<Beer> findAll() {
        return beerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Beer findById(final Long id) {
        return beerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Beer with id " + id + " not found"));
    }

    public Beer create(final Beer beer) {
        beer.setManufacturer(getManufacturer(beer));
        return beerRepository.save(beer);
    }

    public Beer update(final Long id, final Beer beer) {
        final var existingBeer = findById(id);
        existingBeer.setName(beer.getName());
        existingBeer.setAbv(beer.getAbv());
        existingBeer.setType(beer.getType());
        existingBeer.setDescription(beer.getDescription());
        existingBeer.setManufacturer(getManufacturer(beer));
        return beerRepository.save(existingBeer);
    }

    public void delete(final Long id) {
        final var beer = findById(id);
        beerRepository.delete(beer);
    }

    private Manufacturer getManufacturer(final Beer beer) {
        if (beer.getManufacturer() == null || beer.getManufacturer().getId() == null) {
            throw new IllegalArgumentException("Beer manufacturer id is required");
        }

        final var manufacturerId = beer.getManufacturer().getId();
        return manufacturerRepository.findById(manufacturerId)
                .orElseThrow(() -> new NotFoundException("Manufacturer with id " + manufacturerId + " not found"));
    }
}
