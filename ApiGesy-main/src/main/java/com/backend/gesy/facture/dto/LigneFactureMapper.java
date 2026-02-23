package com.backend.gesy.facture.dto;

import com.backend.gesy.facture.LigneFacture;
import org.springframework.stereotype.Component;

@Component
public class LigneFactureMapper {
    public LigneFactureDTO toDTO(LigneFacture ligne) {
        if (ligne == null) {
            return null;
        }

        LigneFactureDTO dto = new LigneFactureDTO();
        dto.setId(ligne.getId());
        dto.setProduitId(ligne.getProduit() != null ? ligne.getProduit().getId() : null);
        dto.setProduitNom(ligne.getProduit() != null ? ligne.getProduit().getNom() : null);
        dto.setProduitType(ligne.getProduit() != null && ligne.getProduit().getTypeProduit() != null 
            ? ligne.getProduit().getTypeProduit().name() : null);
        dto.setQuantite(ligne.getQuantite());
        dto.setPrixUnitaire(ligne.getPrixUnitaire());
        dto.setTotal(ligne.getTotal());

        return dto;
    }

    public LigneFacture toEntity(LigneFactureDTO dto) {
        if (dto == null) {
            return null;
        }

        LigneFacture ligne = new LigneFacture();
        ligne.setId(dto.getId());
        ligne.setQuantite(dto.getQuantite());
        ligne.setPrixUnitaire(dto.getPrixUnitaire());
        ligne.setTotal(dto.getTotal());
        // produit et facture seront définis par le service

        return ligne;
    }
}
