package com.haufe.beercatalogue.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.haufe.beercatalogue.domain.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);
}
