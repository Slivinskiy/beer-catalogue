package com.haufe.beercatalogue.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.haufe.beercatalogue.domain.Role;
import com.haufe.beercatalogue.repository.AppUserRepository;

@Service
public class AccessService {
    private final AppUserRepository appUserRepository;

    public AccessService(final AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public void requireAdmin() {
        final var currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Admin access is required");
        }
    }

    public void requireAdminOrOwnManufacturer(final Long manufacturerId) {
        final var currentUser = getCurrentUser();
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (currentUser.getRole() == Role.MANUFACTURER && manufacturerId != null && manufacturerId.equals(currentUser.getManufacturerId())) {
            return;
        }
        throw new AccessDeniedException("You can only modify your own manufacturer data");
    }

    private com.haufe.beercatalogue.domain.AppUser getCurrentUser() {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Authentication is required");
        }
        return appUserRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
    }
}
