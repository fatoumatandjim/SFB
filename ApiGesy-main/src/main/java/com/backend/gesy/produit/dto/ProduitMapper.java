package com.backend.gesy.produit.dto;

import com.backend.gesy.produit.Produit;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class ProduitMapper {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    public ProduitDTO toDTO(Produit produit) {
        if (produit == null) {
            return null;
        }

        ProduitDTO dto = new ProduitDTO();
        dto.setId(produit.getId());
        dto.setNom(produit.getNom());
        dto.setTypeProduit(produit.getTypeProduit() != null ? produit.getTypeProduit().name() : null);
        dto.setDescription(produit.getDescription());
        dto.setDateCreation(produit.getCreatedAt() != null ? produit.getCreatedAt().format(DATE_FORMATTER) : null);

        return dto;
    }

    public Produit toEntity(ProduitDTO dto) {
        if (dto == null) {
            return null;
        }

        Produit produit = new Produit();
        produit.setId(dto.getId());
        produit.setNom(dto.getNom());
        if (dto.getTypeProduit() != null) {
            produit.setTypeProduit(Produit.TypeProduit.valueOf(dto.getTypeProduit()));
        }
        produit.setDescription(dto.getDescription());

        return produit;
    }
}

