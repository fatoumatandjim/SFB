package com.backend.gesy.transaction.dto;

import com.backend.gesy.transaction.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {
    public TransactionDTO toDTO(Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        
        TransactionDTO dto = new TransactionDTO();
        dto.setId(transaction.getId());
        dto.setType(transaction.getType() != null ? transaction.getType().name() : null);
        dto.setMontant(transaction.getMontant());
        dto.setDate(transaction.getDate());
        dto.setCompteId(transaction.getCompte() != null ? transaction.getCompte().getId() : null);
        dto.setCamionId(transaction.getCamion() != null ? transaction.getCamion().getId() : null);
        dto.setFactureId(transaction.getFacture() != null ? transaction.getFacture().getId() : null);
        dto.setVoyageId(transaction.getVoyage() != null ? transaction.getVoyage().getId() : null);
        dto.setCaisseId(transaction.getCaisse() != null ? transaction.getCaisse().getId() : null);
        dto.setTransactionLieeId(transaction.getTransactionLiee() != null ? transaction.getTransactionLiee().getId() : null);
        dto.setStatut(transaction.getStatut() != null ? transaction.getStatut().name() : null);
        dto.setDescription(transaction.getDescription());
        dto.setReference(transaction.getReference());
        dto.setBeneficiaire(transaction.getBeneficiaire());
        
        return dto;
    }

    public Transaction toEntity(TransactionDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Transaction transaction = new Transaction();
        transaction.setId(dto.getId());
        if (dto.getType() != null) {
            transaction.setType(Transaction.TypeTransaction.valueOf(dto.getType()));
        }
        transaction.setMontant(dto.getMontant());
        transaction.setDate(dto.getDate());
        if (dto.getStatut() != null) {
            transaction.setStatut(Transaction.StatutTransaction.valueOf(dto.getStatut()));
        }
        transaction.setDescription(dto.getDescription());
        transaction.setReference(dto.getReference());
        transaction.setBeneficiaire(dto.getBeneficiaire());
        
        return transaction;
    }
}

