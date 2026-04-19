package com.backend.gesy.compte;

import com.backend.gesy.utilisateur.Utilisateur;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ResponsableRefs {

    private ResponsableRefs() {
    }

    public static List<ResponsableRefDTO> toList(Set<Compte> responsables) {
        if (responsables == null || responsables.isEmpty()) {
            return List.of();
        }
        return responsables.stream()
                .filter(Objects::nonNull)
                .map(ResponsableRefs::toRef)
                .sorted(Comparator.comparing(ResponsableRefDTO::getNom, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static ResponsableRefDTO toRef(Compte c) {
        String nom;
        if (c instanceof Utilisateur u) {
            nom = u.getNom() != null ? u.getNom() : c.getIdentifiant();
        } else {
            nom = c.getIdentifiant() != null ? c.getIdentifiant() : ("Compte " + c.getId());
        }
        return new ResponsableRefDTO(c.getId(), nom);
    }
}
