package com.backend.gesy.paiement;

import com.backend.gesy.alerte.Alerte;
import com.backend.gesy.alerte.AlerteService;
import com.backend.gesy.categoriedepense.CategorieDepenseRepository;
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
import com.backend.gesy.voyage.Voyage;
import com.backend.gesy.voyage.VoyagePaiementMenuRules;
import com.backend.gesy.voyage.VoyageRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PaiementServiceImpl implements PaiementService {

    private static final Logger log = LoggerFactory.getLogger(PaiementServiceImpl.class);

    private static final Pattern VOYAGE_NUM_IN_REF = Pattern.compile("(VOY-\\d{4}-\\d{4})");

    private final PaiementRepository paiementRepository;
    private final FactureRepository factureRepository;
    private final PaiementMapper paiementMapper;
    private final TransactionRepository transactionRepository;
    private final CompteBancaireRepository compteBancaireRepository;
    private final CaisseRepository caisseRepository;
    private final AlerteService alerteService;
    private final CategorieDepenseRepository categorieDepenseRepository;
    private final VoyageRepository voyageRepository;

    /**
     * Au démarrage, répare les paiements orphelins (voyage_id NULL) dont la référence
     * contient un numéro de voyage existant. Ces paiements ont été créés avant que le
     * code ne fasse {@code paiement.setVoyage(...)}.
     */
    @PostConstruct
    @Transactional
    public void repairOrphanedPaiements() {
        List<Paiement> orphans = paiementRepository.findAll().stream()
            .filter(p -> p.getVoyage() == null && p.getReference() != null)
            .collect(Collectors.toList());

        if (orphans.isEmpty()) return;

        int repaired = 0;
        for (Paiement p : orphans) {
            String voyageNum = extractVoyageNumber(p.getReference());
            if (voyageNum == null) continue;

            Optional<Voyage> voyage = voyageRepository.findByNumeroVoyage(voyageNum);
            if (voyage.isPresent()) {
                p.setVoyage(voyage.get());
                paiementRepository.save(p);
                repaired++;
            }
        }
        if (repaired > 0) {
            log.info("Paiements orphelins réparés (voyage_id restauré via référence): {}", repaired);
        }
    }

    @Override
    public List<PaiementDTO> findAll() {
        List<Paiement> all = paiementRepository.findAll();
        return applyVoyageFilter(all).stream()
            .sorted(Comparator.comparing(Paiement::getDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Paiement::getId, Comparator.reverseOrder()))
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
        if (paiementDTO.getCategorieId() != null) {
            paiement.setCategorieDepense(categorieDepenseRepository.findById(paiementDTO.getCategorieId())
                .orElseThrow(() -> new RuntimeException("Catégorie de dépense non trouvée avec l'id: " + paiementDTO.getCategorieId())));
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
        if (paiementDTO.getCategorieId() != null) {
            paiement.setCategorieDepense(categorieDepenseRepository.findById(paiementDTO.getCategorieId())
                .orElseThrow(() -> new RuntimeException("Catégorie de dépense non trouvée avec l'id: " + paiementDTO.getCategorieId())));
        } else {
            paiement.setCategorieDepense(existingPaiement.getCategorieDepense());
        }
        Paiement updatedPaiement = paiementRepository.save(paiement);
        return paiementMapper.toDTO(updatedPaiement);
    }

    @Override
    public List<PaiementDTO> findByStatut(Paiement.StatutPaiement statut) {
        List<Paiement> all = paiementRepository.findAll().stream()
            .filter(p -> p.getStatut() == statut)
            .collect(Collectors.toList());

        return applyVoyageFilter(all).stream()
            .sorted(Comparator.comparing(Paiement::getDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Paiement::getId, Comparator.reverseOrder()))
            .map(paiementMapper::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Applique le filtre voyage : exclut les paiements liés (directement ou via référence)
     * à un voyage {@code EN_ATTENTE_CHARGEMENT}.
     */
    private List<Paiement> applyVoyageFilter(List<Paiement> paiements) {
        List<Paiement> filtered = paiements.stream()
            .filter(VoyagePaiementMenuRules::isPaiementRowVisibleInMenu)
            .collect(Collectors.toList());

        Set<String> blockedVoyageNumbers = voyageRepository
            .findByStatut(Voyage.StatutVoyage.EN_ATTENTE_CHARGEMENT).stream()
            .map(Voyage::getNumeroVoyage)
            .collect(Collectors.toSet());

        if (blockedVoyageNumbers.isEmpty()) return filtered;

        return filtered.stream()
            .filter(p -> {
                String ref = p.getReference();
                if (ref == null || p.getVoyage() != null) return true;
                String voyageNum = extractVoyageNumber(ref);
                return voyageNum == null || !blockedVoyageNumbers.contains(voyageNum);
            })
            .collect(Collectors.toList());
    }

    private static String extractVoyageNumber(String reference) {
        if (reference == null) return null;
        Matcher m = VOYAGE_NUM_IN_REF.matcher(reference);
        return m.find() ? m.group(1) : null;
    }

    @Override
    public List<PaiementDTO> findByCategorieId(Long categorieId) {
        return paiementRepository.findByCategorieDepenseIdOrderByDateDesc(categorieId).stream()
            .map(paiementMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<PaiementDTO> findByCategorieIdAndDateRange(Long categorieId, LocalDate startDate, LocalDate endDate) {
        return paiementRepository.findByCategorieDepenseIdAndDateBetweenOrderByDateDesc(categorieId, startDate, endDate).stream()
            .map(paiementMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<PaiementDTO> findAllWithCategorie(LocalDate startDate, LocalDate endDate) {
        List<Paiement> list;
        if (startDate != null && endDate != null) {
            list = paiementRepository.findByCategorieDepenseIsNotNullAndDateBetweenOrderByDateDesc(startDate, endDate);
        } else {
            list = paiementRepository.findByCategorieDepenseIsNotNullOrderByDateDesc();
        }
        return list.stream().map(paiementMapper::toDTO).collect(Collectors.toList());
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

