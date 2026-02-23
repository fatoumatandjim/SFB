package com.backend.gesy.achat.dto;

import com.backend.gesy.achat.Achat;
import org.springframework.stereotype.Component;

@Component
public class AchatMapper {
    
    public AchatDTO toDTO(Achat achat) {
        if (achat == null) {
            return null;
        }

        AchatDTO dto = new AchatDTO();
        dto.setId(achat.getId());
        
        if (achat.getDepot() != null) {
            dto.setDepotId(achat.getDepot().getId());
            dto.setDepotNom(achat.getDepot().getNom());
        }
        
        if (achat.getProduit() != null) {
            dto.setProduitId(achat.getProduit().getId());
            dto.setProduitNom(achat.getProduit().getNom());
            dto.setTypeProduit(achat.getProduit().getTypeProduit() != null ? achat.getProduit().getTypeProduit().name() : null);
        }
        
        dto.setQuantite(achat.getQuantite());
        dto.setPrixUnitaire(achat.getPrixUnitaire());
        dto.setMontantTotal(achat.getMontantTotal());
        dto.setDateAchat(achat.getDateAchat());
        dto.setDescription(achat.getDescription());
        dto.setNotes(achat.getNotes());
        dto.setUnite(achat.getUnite());
        dto.setCession(achat.isCession());
        if (achat.getClient() != null) {
            dto.setClientId(achat.getClient().getId());
            dto.setClientNom(achat.getClient().getNom());
        }

        if (achat.getFacture() != null) {
            dto.setFactureId(achat.getFacture().getId());
            dto.setFactureNumero(achat.getFacture().getNumero());
            dto.setStatutFacture(achat.getFacture().getStatut() != null ? achat.getFacture().getStatut().name() : null);
        }
        
        if (achat.getTransaction() != null) {
            dto.setTransactionId(achat.getTransaction().getId());
            dto.setTransactionReference(achat.getTransaction().getReference());
            dto.setStatutPaiement(achat.getTransaction().getStatut() != null ? achat.getTransaction().getStatut().name() : null);
            // Pour compatibilité, utiliser le statut de transaction comme statut de facture si pas de facture
            if (achat.getFacture() == null) {
                if (achat.getTransaction().getStatut() == com.backend.gesy.transaction.Transaction.StatutTransaction.VALIDE) {
                    dto.setStatutFacture("PAYEE");
                } else if (achat.getTransaction().getStatut() == com.backend.gesy.transaction.Transaction.StatutTransaction.EN_ATTENTE) {
                    dto.setStatutFacture("EMISE");
                }
            }
        }
        
        return dto;
    }
    
    public Achat toEntity(AchatDTO dto) {
        if (dto == null) {
            return null;
        }

        Achat achat = new Achat();
        achat.setId(dto.getId());
        achat.setQuantite(dto.getQuantite());
        achat.setPrixUnitaire(dto.getPrixUnitaire());
        achat.setMontantTotal(dto.getMontantTotal());
        achat.setDateAchat(dto.getDateAchat() != null ? dto.getDateAchat() : java.time.LocalDateTime.now());
        achat.setDescription(dto.getDescription());
        achat.setNotes(dto.getNotes());
        achat.setUnite(dto.getUnite());
        achat.setCession(Boolean.TRUE.equals(dto.getCession()));
        return achat;
    }
}

