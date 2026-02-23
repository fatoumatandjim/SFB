package com.backend.gesy.paiement.dto;

import com.backend.gesy.paiement.Paiement;
import org.springframework.stereotype.Component;

@Component
public class PaiementMapper {
    public PaiementDTO toDTO(Paiement paiement) {
        if (paiement == null) {
            return null;
        }
        
        PaiementDTO dto = new PaiementDTO();
        dto.setId(paiement.getId());
        dto.setMontant(paiement.getMontant());
        dto.setDate(paiement.getDate());
        dto.setFactureId(paiement.getFacture() != null ? paiement.getFacture().getId() : null);
        dto.setMethode(paiement.getMethode() != null ? paiement.getMethode().name() : null);
        dto.setStatut(paiement.getStatut() != null ? paiement.getStatut().name() : null);
        dto.setNumeroCheque(paiement.getNumeroCheque());
        dto.setNumeroCompte(paiement.getNumeroCompte());
        dto.setReference(paiement.getReference());
        dto.setNotes(paiement.getNotes());
        
        return dto;
    }

    public Paiement toEntity(PaiementDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Paiement paiement = new Paiement();
        paiement.setId(dto.getId());
        paiement.setMontant(dto.getMontant());
        paiement.setDate(dto.getDate());
        if (dto.getMethode() != null) {
            paiement.setMethode(Paiement.MethodePaiement.valueOf(dto.getMethode()));
        }
        if (dto.getStatut() != null) {
            paiement.setStatut(Paiement.StatutPaiement.valueOf(dto.getStatut()));
        }
        paiement.setNumeroCheque(dto.getNumeroCheque());
        paiement.setNumeroCompte(dto.getNumeroCompte());
        paiement.setReference(dto.getReference());
        paiement.setNotes(dto.getNotes());
        
        return paiement;
    }
}

