package com.backend.gesy.finance;

import com.backend.gesy.caisse.Caisse;
import com.backend.gesy.compte.Compte;
import com.backend.gesy.compte.CompteRepository;
import com.backend.gesy.comptebancaire.CompteBancaire;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Vérifie que l'utilisateur courant peut gérer une caisse ou un compte bancaire :
 * administrateur, ou compte listé comme responsable. Si aucun responsable n'est défini (données historiques), l'accès reste ouvert à tout utilisateur authentifié.
 */
@Service
@RequiredArgsConstructor
public class FinanceEntityAccessService {

    private final CompteRepository compteRepository;

    public boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority a : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(a.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    public Optional<Long> getCurrentCompteId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        return compteRepository.findByIdentifiant(auth.getName()).map(Compte::getId);
    }

    public void assertCanManageCaisse(Caisse caisse) {
        if (caisse == null) {
            return;
        }
        if (isCurrentUserAdmin()) {
            return;
        }
        if (caisse.getResponsables() == null || caisse.getResponsables().isEmpty()) {
            return;
        }
        Long uid = getCurrentCompteId()
                .orElseThrow(() -> new RuntimeException("Session invalide : compte utilisateur introuvable."));
        boolean allowed = caisse.getResponsables().stream().anyMatch(r -> r.getId() != null && r.getId().equals(uid));
        if (!allowed) {
            throw new RuntimeException("Accès refusé : vous n'êtes pas responsable de cette caisse.");
        }
    }

    public void assertCanManageCompteBancaire(CompteBancaire compte) {
        if (compte == null) {
            return;
        }
        if (isCurrentUserAdmin()) {
            return;
        }
        if (compte.getResponsables() == null || compte.getResponsables().isEmpty()) {
            return;
        }
        Long uid = getCurrentCompteId()
                .orElseThrow(() -> new RuntimeException("Session invalide : compte utilisateur introuvable."));
        boolean allowed = compte.getResponsables().stream().anyMatch(r -> r.getId() != null && r.getId().equals(uid));
        if (!allowed) {
            throw new RuntimeException("Accès refusé : vous n'êtes pas responsable de ce compte bancaire.");
        }
    }
}
