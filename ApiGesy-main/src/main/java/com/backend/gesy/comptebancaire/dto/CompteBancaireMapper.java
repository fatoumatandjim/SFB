package com.backend.gesy.comptebancaire.dto;

import com.backend.gesy.compte.Compte;
import com.backend.gesy.compte.ResponsableRefs;
import com.backend.gesy.comptebancaire.CompteBancaire;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class CompteBancaireMapper {
    public CompteBancaireDTO toDTO(CompteBancaire compte) {
        if (compte == null) {
            return null;
        }
        
        CompteBancaireDTO dto = new CompteBancaireDTO();
        dto.setId(compte.getId());
        dto.setNumero(compte.getNumero());
        dto.setType(compte.getType() != null ? compte.getType().name() : null);
        dto.setSolde(compte.getSolde());
        dto.setBanque(compte.getBanque());
        dto.setNumeroCompteBancaire(compte.getNumeroCompteBancaire());
        dto.setStatut(compte.getStatut() != null ? compte.getStatut().name() : null);
        dto.setDescription(compte.getDescription());
        if (compte.getResponsables() != null && !compte.getResponsables().isEmpty()) {
            dto.setResponsableIds(compte.getResponsables().stream()
                    .map(Compte::getId)
                    .collect(Collectors.toList()));
            dto.setResponsables(ResponsableRefs.toList(compte.getResponsables()));
        }

        return dto;
    }

    public CompteBancaire toEntity(CompteBancaireDTO dto) {
        if (dto == null) {
            return null;
        }
        
        CompteBancaire compte = new CompteBancaire();
        compte.setId(dto.getId());
        compte.setNumero(dto.getNumero());
        if (dto.getType() != null) {
            compte.setType(CompteBancaire.TypeCompte.valueOf(dto.getType()));
        }
        compte.setSolde(dto.getSolde());
        compte.setBanque(dto.getBanque());
        compte.setNumeroCompteBancaire(dto.getNumeroCompteBancaire());
        if (dto.getStatut() != null) {
            compte.setStatut(CompteBancaire.StatutCompte.valueOf(dto.getStatut()));
        }
        compte.setDescription(dto.getDescription());
        
        return compte;
    }
}

