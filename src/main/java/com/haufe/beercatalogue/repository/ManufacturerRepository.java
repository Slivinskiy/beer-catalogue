package com.haufe.beercatalogue.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.haufe.beercatalogue.domain.Manufacturer;

public interface ManufacturerRepository extends JpaRepository<Manufacturer, Long> {
}
