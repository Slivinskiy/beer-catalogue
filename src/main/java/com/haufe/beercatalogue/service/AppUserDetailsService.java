package com.haufe.beercatalogue.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.haufe.beercatalogue.repository.AppUserRepository;

@Service
public class AppUserDetailsService implements UserDetailsService {
    private final AppUserRepository appUserRepository;

    public AppUserDetailsService(final AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        final var appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return User.withUsername(appUser.getUsername())
                .password(appUser.getPassword())
                .roles(appUser.getRole().name())
                .build();
    }
}
