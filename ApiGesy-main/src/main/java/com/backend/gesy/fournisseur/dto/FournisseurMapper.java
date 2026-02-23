package com.backend.gesy.fournisseur.dto;

import com.backend.gesy.fournisseur.Fournisseur;
import org.springframework.stereotype.Component;

@Component
public class FournisseurMapper {
    public FournisseurDTO toDTO(Fournisseur fournisseur) {
        if (fournisseur == null) {
            return null;
        }
        
        FournisseurDTO dto = new FournisseurDTO();
        dto.setId(fournisseur.getId());
        dto.setNom(fournisseur.getNom());
        dto.setEmail(fournisseur.getEmail());
        dto.setTelephone(fournisseur.getTelephone());
        dto.setAdresse(fournisseur.getAdresse());
        dto.setCodeFournisseur(fournisseur.getCodeFournisseur());
        dto.setVille(fournisseur.getVille());
        dto.setPays(fournisseur.getPays());
        dto.setContactPersonne(fournisseur.getContactPersonne());
        dto.setTypeFournisseur(fournisseur.getTypeFournisseur() != null ? fournisseur.getTypeFournisseur().name() : null);
        
        return dto;
    }

    public Fournisseur toEntity(FournisseurDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setId(dto.getId());
        fournisseur.setNom(dto.getNom());
        fournisseur.setEmail(dto.getEmail());
        fournisseur.setTelephone(dto.getTelephone());
        fournisseur.setAdresse(dto.getAdresse());
        fournisseur.setCodeFournisseur(dto.getCodeFournisseur());
        fournisseur.setVille(dto.getVille());
        fournisseur.setPays(dto.getPays());
        fournisseur.setContactPersonne(dto.getContactPersonne());
        if (dto.getTypeFournisseur() != null && !dto.getTypeFournisseur().isEmpty()) {
            fournisseur.setTypeFournisseur(Fournisseur.TypeFournisseur.valueOf(dto.getTypeFournisseur()));
        } else {
            fournisseur.setTypeFournisseur(Fournisseur.TypeFournisseur.ACHAT);
        }
        
        return fournisseur;
    }
}

