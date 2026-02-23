package com.backend.gesy.security.service;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class JwtTokenBlacklistService {
    private final Set<String> blacklistedTokens = new HashSet<>();

    public void blacklistToken(String token) {
        blacklistedTokens.add(token);
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }

    // Optionnel: Nettoyer périodiquement les tokens expirés de la blacklist
    public void removeToken(String token) {
        blacklistedTokens.remove(token);
    }
}

