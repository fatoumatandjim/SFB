package com.backend.gesy.depense.dto;

import com.backend.gesy.caisse.CaisseRepository;
import com.backend.gesy.categoriedepense.CategorieDepense;
import com.backend.gesy.categoriedepense.CategorieDepenseRepository;
import com.backend.gesy.comptebancaire.CompteBancaireRepository;
import com.backend.gesy.depense.Depense;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DepenseMapper {

    private final CategorieDepenseRepository categorieDepenseRepository;
    private final CompteBancaireRepository compteBancaireRepository;
    private final CaisseRepository caisseRepository;

    public DepenseDTO toDTO(Depense entity) {
        if (entity == null) return null;

        DepenseDTO dto = new DepenseDTO();
        dto.setId(entity.getId());
        dto.setLibelle(entity.getLibelle());
        dto.setMontant(entity.getMontant());
        dto.setDateDepense(entity.getDateDepense());
        dto.setDescription(entity.getDescription());
        dto.setReference(entity.getReference());
        dto.setDateCreation(entity.getDateCreation());
        dto.setCreePar(entity.getCreePar());

        if (entity.getCategorie() != null) {
            dto.setCategorieId(entity.getCategorie().getId());
            dto.setCategorieNom(entity.getCategorie().getNom());
        }
        if (entity.getCompteBancaire() != null) {
            dto.setCompteId(entity.getCompteBancaire().getId());
        }
        if (entity.getCaisse() != null) {
            dto.setCaisseId(entity.getCaisse().getId());
        }

        return dto;
    }

    public Depense toEntity(DepenseDTO dto) {
        if (dto == null) return null;

        Depense entity = new Depense();
        entity.setId(dto.getId());
        entity.setLibelle(dto.getLibelle());
        entity.setMontant(dto.getMontant());
        entity.setDateDepense(dto.getDateDepense());
        entity.setDescription(dto.getDescription());
        entity.setReference(dto.getReference());
        entity.setDateCreation(dto.getDateCreation());
        entity.setCreePar(dto.getCreePar());

        if (dto.getCategorieId() != null) {
            CategorieDepense categorie = categorieDepenseRepository.findById(dto.getCategorieId())
                    .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec l'id: " + dto.getCategorieId()));
            entity.setCategorie(categorie);
        }
        if (dto.getCompteId() != null) {
            entity.setCompteBancaire(compteBancaireRepository.findById(dto.getCompteId())
                    .orElseThrow(() -> new RuntimeException("Compte bancaire non trouvé avec l'id: " + dto.getCompteId())));
        }
        if (dto.getCaisseId() != null) {
            entity.setCaisse(caisseRepository.findById(dto.getCaisseId())
                    .orElseThrow(() -> new RuntimeException("Caisse non trouvée avec l'id: " + dto.getCaisseId())));
        }

        return entity;
    }
}

