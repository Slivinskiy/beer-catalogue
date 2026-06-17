package com.haufe.beercatalogue.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.haufe.beercatalogue.domain.Manufacturer;
import com.haufe.beercatalogue.exception.NotFoundException;
import com.haufe.beercatalogue.repository.ManufacturerRepository;

@ExtendWith(MockitoExtension.class)
class ManufacturerServiceTest {
    @Mock
    private ManufacturerRepository manufacturerRepository;

    @InjectMocks
    private ManufacturerService manufacturerService;

    @Test
    void shouldReturnAllManufacturers() {
        final var manufacturers = List.of(
                new Manufacturer("BrewDog", "Scotland"),
                new Manufacturer("Guinness", "Ireland")
        );
        when(manufacturerRepository.findAll()).thenReturn(manufacturers);

        final var result = manufacturerService.findAll();

        assertEquals(manufacturers, result);
    }

    @Test
    void shouldReturnManufacturerById() {
        final var manufacturer = new Manufacturer("BrewDog", "Scotland");
        manufacturer.setId(1L);
        when(manufacturerRepository.findById(1L)).thenReturn(Optional.of(manufacturer));

        final var result = manufacturerService.findById(1L);

        assertSame(manufacturer, result);
    }

    @Test
    void shouldThrowWhenManufacturerIsMissing() {
        when(manufacturerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> manufacturerService.findById(1L));
    }

    @Test
    void shouldCreateManufacturer() {
        final var manufacturer = new Manufacturer("BrewDog", "Scotland");
        when(manufacturerRepository.save(manufacturer)).thenReturn(manufacturer);

        final var result = manufacturerService.create(manufacturer);

        assertSame(manufacturer, result);
    }

    @Test
    void shouldUpdateManufacturer() {
        final var existingManufacturer = new Manufacturer("Old Name", "Old Country");
        existingManufacturer.setId(1L);

        final var updatedManufacturer = new Manufacturer("New Name", "New Country");

        when(manufacturerRepository.findById(1L)).thenReturn(Optional.of(existingManufacturer));
        when(manufacturerRepository.save(any(Manufacturer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var result = manufacturerService.update(1L, updatedManufacturer);

        assertEquals("New Name", result.getName());
        assertEquals("New Country", result.getCountryOfOrigin());
    }

    @Test
    void shouldDeleteManufacturer() {
        final var manufacturer = new Manufacturer("BrewDog", "Scotland");
        manufacturer.setId(1L);
        when(manufacturerRepository.findById(1L)).thenReturn(Optional.of(manufacturer));

        manufacturerService.delete(1L);

        verify(manufacturerRepository).delete(manufacturer);
    }
}
