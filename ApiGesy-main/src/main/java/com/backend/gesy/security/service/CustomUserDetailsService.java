package com.backend.gesy.security.service;

import com.backend.gesy.compte.Compte;
import com.backend.gesy.compte.CompteRepository;
import com.backend.gesy.roles.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final CompteRepository compteRepository;

    @Override
    public UserDetails loadUserByUsername(String identifiant) throws UsernameNotFoundException {
        Compte compte = compteRepository.findByIdentifiant(identifiant)
                .orElseThrow(() -> new UsernameNotFoundException("Compte non trouvé avec l'identifiant: " + identifiant));

        if (!compte.getActif()) {
            throw new UsernameNotFoundException("Compte désactivé: " + identifiant);
        }

        return User.builder()
                .username(compte.getIdentifiant())
                .password(compte.getMotDePasse())
                .authorities(getAuthorities(compte))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!compte.getActif())
                .build();
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Compte compte) {
        return compte.getRoles().stream()
                .filter(role -> role.getStatut() == Roles.StatutRole.ACTIF)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getNom().toUpperCase()))
                .collect(Collectors.toList());
    }
}

