package com.haufe.beercatalogue.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.haufe.beercatalogue.domain.Beer;

public interface BeerRepository extends JpaRepository<Beer, Long> {
}
