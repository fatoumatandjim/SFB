package com.backend.gesy.paiement;

import com.backend.gesy.alerte.Alerte;
import com.backend.gesy.alerte.AlerteService;
import com.backend.gesy.facture.Facture;
import com.backend.gesy.facture.FactureRepository;
import com.backend.gesy.paiement.dto.PaiementDTO;
import com.backend.gesy.paiement.dto.PaiementMapper;
import com.backend.gesy.transaction.Transaction;
import com.backend.gesy.transaction.TransactionRepository;
import com.backend.gesy.comptebancaire.CompteBancaire;
import com.backend.gesy.comptebancaire.CompteBancaireRepository;
import com.backend.gesy.caisse.Caisse;
import com.backend.gesy.caisse.CaisseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PaiementServiceImpl implements PaiementService {
    private final PaiementRepository paiementRepository;
    private final FactureRepository factureRepository;
    private final PaiementMapper paiementMapper;
    private final TransactionRepository transactionRepository;
    private final CompteBancaireRepository compteBancaireRepository;
    private final CaisseRepository caisseRepository;
    private final AlerteService alerteService;

    @Override
    public List<PaiementDTO> findAll() {
        return paiementRepository.findAll().stream()
            .map(paiementMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<PaiementDTO> findById(Long id) {
        return paiementRepository.findById(id)
            .map(paiementMapper::toDTO);
    }

    @Override
    public List<PaiementDTO> findByFactureId(Long factureId) {
        Facture facture = factureRepository.findById(factureId)
            .orElseThrow(() -> new RuntimeException("Facture non trouvée avec l'id: " + factureId));
        return paiementRepository.findByFacture(facture).stream()
            .map(paiementMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public PaiementDTO save(PaiementDTO paiementDTO) {
        Paiement paiement = paiementMapper.toEntity(paiementDTO);
        if (paiementDTO.getFactureId() != null) {
            Facture facture = factureRepository.findById(paiementDTO.getFactureId())
                .orElseThrow(() -> new RuntimeException("Facture non trouvée avec l'id: " + paiementDTO.getFactureId()));
            paiement.setFacture(facture);
        }
        Paiement savedPaiement = paiementRepository.save(paiement);
        return paiementMapper.toDTO(savedPaiement);
    }

    @Override
    public PaiementDTO update(Long id, PaiementDTO paiementDTO) {
        Paiement existingPaiement = paiementRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Paiement non trouvé avec l'id: " + id));
        Paiement paiement = paiementMapper.toEntity(paiementDTO);
        paiement.setId(existingPaiement.getId());
        if (paiementDTO.getFactureId() != null) {
            Facture facture = factureRepository.findById(paiementDTO.getFactureId())
                .orElseThrow(() -> new RuntimeException("Facture non trouvée avec l'id: " + paiementDTO.getFactureId()));
            paiement.setFacture(facture);
        } else {
            paiement.setFacture(existingPaiement.getFacture());
        }
        Paiement updatedPaiement = paiementRepository.save(paiement);
        return paiementMapper.toDTO(updatedPaiement);
    }

    @Override
    public List<PaiementDTO> findByStatut(Paiement.StatutPaiement statut) {
        return paiementRepository.findAll().stream()
            .filter(p -> p.getStatut() == statut)
            .map(paiementMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public PaiementDTO validerPaiement(Long paiementId, Long compteId, Long caisseId) {
        Paiement paiement = paiementRepository.findById(paiementId)
            .orElseThrow(() -> new RuntimeException("Paiement non trouvé avec l'id: " + paiementId));
        
        if (paiement.getStatut() != Paiement.StatutPaiement.EN_ATTENTE) {
            throw new RuntimeException("Le paiement n'est pas en attente");
        }
        
        // Vérifier qu'un compte ou une caisse est fourni
        if (compteId == null && caisseId == null) {
            throw new RuntimeException("Un compte bancaire ou une caisse doit être sélectionné");
        }
        
        if (compteId != null && caisseId != null) {
            throw new RuntimeException("Veuillez sélectionner soit un compte bancaire, soit une caisse, pas les deux");
        }
        
        // Assigner le compte ou la caisse au paiement
        if (compteId != null) {
            CompteBancaire compte = compteBancaireRepository.findById(compteId)
                .orElseThrow(() -> new RuntimeException("Compte bancaire non trouvé avec l'id: " + compteId));
            paiement.setCompte(compte);
        }
        
        if (caisseId != null) {
            Caisse caisse = caisseRepository.findById(caisseId)
                .orElseThrow(() -> new RuntimeException("Caisse non trouvée avec l'id: " + caisseId));
            paiement.setCaisse(caisse);
        }
        
        // Valider toutes les transactions associées et les débiter
        for (Transaction transaction : paiement.getTransactions()) {
            if (transaction.getStatut() == com.backend.gesy.transaction.Transaction.StatutTransaction.EN_ATTENTE) {
                // Assigner le compte ou la caisse à la transaction
                CompteBancaire compte = null;
                Caisse caisse = null;
                
                if (compteId != null) {
                    compte = compteBancaireRepository.findById(compteId)
                        .orElseThrow(() -> new RuntimeException("Compte bancaire non trouvé avec l'id: " + compteId));
                    transaction.setCompte(compte);
                    if (compte.getStatut() != com.backend.gesy.comptebancaire.CompteBancaire.StatutCompte.ACTIF) {
                        throw new RuntimeException("Le compte bancaire n'est pas actif");
                    }
                }
                
                if (caisseId != null) {
                    caisse = caisseRepository.findById(caisseId)
                        .orElseThrow(() -> new RuntimeException("Caisse non trouvée avec l'id: " + caisseId));
                    transaction.setCaisse(caisse);
                    if (caisse.getStatut() != com.backend.gesy.caisse.Caisse.StatutCaisse.ACTIF) {
                        throw new RuntimeException("La caisse n'est pas active");
                    }
                }
                
                // Valider la transaction
                transaction.setStatut(com.backend.gesy.transaction.Transaction.StatutTransaction.VALIDE);
                transactionRepository.save(transaction);
                
                // Créditer le compte ou la caisse (paiement reçu = entrée d'argent)
                if (compte != null) {
                    compte.setSolde(compte.getSolde().add(transaction.getMontant()));
                    compteBancaireRepository.save(compte);
                }
                
                if (caisse != null) {
                    caisse.setSolde(caisse.getSolde().add(transaction.getMontant()));
                    caisseRepository.save(caisse);
                }
            }
        }
        
        // Valider le paiement
        paiement.setStatut(Paiement.StatutPaiement.VALIDE);
        Paiement savedPaiement = paiementRepository.save(paiement);

        String message = "Paiement reçu : " + savedPaiement.getMontant() + " FCFA";
        if (savedPaiement.getFacture() != null) {
            message += " - Facture " + savedPaiement.getFacture().getNumero();
        }
        alerteService.creerAlerte(Alerte.TypeAlerte.PAIEMENT_RECU, message,
                Alerte.PrioriteAlerte.MOYENNE,
                "Paiement", savedPaiement.getId(),
                savedPaiement.getFacture() != null ? "/factures/" + savedPaiement.getFacture().getId() : null);

        return paiementMapper.toDTO(savedPaiement);
    }

    @Override
    public void deleteById(Long id) {
        paiementRepository.deleteById(id);
    }
}

