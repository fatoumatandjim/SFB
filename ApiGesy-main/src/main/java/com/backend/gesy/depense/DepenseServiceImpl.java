package com.backend.gesy.depense;

import com.backend.gesy.caisse.Caisse;
import com.backend.gesy.caisse.CaisseRepository;
import com.backend.gesy.categoriedepense.CategorieDepense;
import com.backend.gesy.categoriedepense.CategorieDepenseRepository;
import com.backend.gesy.comptebancaire.CompteBancaire;
import com.backend.gesy.comptebancaire.CompteBancaireRepository;
import com.backend.gesy.transaction.Transaction;
import com.backend.gesy.transaction.TransactionRepository;
import com.backend.gesy.depense.dto.DepenseDTO;
import com.backend.gesy.depense.dto.DepenseMapper;
import com.backend.gesy.depense.dto.DepensePageDTO;
import com.backend.gesy.depense.dto.DepenseUnifiedPageDTO;
import com.backend.gesy.depense.dto.UnifiedLigneDepenseDTO;
import com.backend.gesy.finance.FinanceEntityAccessService;
import com.backend.gesy.paiement.PaiementService;
import com.backend.gesy.paiement.dto.PaiementDTO;
import com.backend.gesy.transaction.TransactionService;
import com.backend.gesy.transaction.dto.TransactionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DepenseServiceImpl implements DepenseService {
    
    private final DepenseRepository depenseRepository;
    private final CategorieDepenseRepository categorieDepenseRepository;
    private final CompteBancaireRepository compteBancaireRepository;
    private final CaisseRepository caisseRepository;
    private final TransactionRepository transactionRepository;
    private final DepenseMapper depenseMapper;
    private final TransactionService transactionService;
    private final PaiementService paiementService;
    private final FinanceEntityAccessService financeEntityAccessService;

    @Override
    public DepenseDTO save(DepenseDTO dto) {
        if (dto.getMontant() == null || dto.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Le montant de la dépense doit être supérieur à zéro");
        }
        boolean fromCompte = dto.getCompteId() != null;
        boolean fromCaisse = dto.getCaisseId() != null;
        if (fromCompte == fromCaisse) {
            throw new RuntimeException("Veuillez sélectionner soit un compte bancaire, soit une caisse pour déduire le montant (pas les deux, pas aucun)");
        }

        Depense entity = depenseMapper.toEntity(dto);
        entity.setDateCreation(LocalDateTime.now());
        entity.setCreePar(getCurrentUsername());
        if (dto.getDateDepense() == null) {
            entity.setDateDepense(LocalDateTime.now());
        }

        if (fromCompte) {
            financeEntityAccessService.assertCanManageCompteBancaire(
                    compteBancaireRepository.findById(dto.getCompteId())
                            .orElseThrow(() -> new RuntimeException("Compte bancaire non trouvé")));
        } else {
            financeEntityAccessService.assertCanManageCaisse(
                    caisseRepository.findById(dto.getCaisseId())
                            .orElseThrow(() -> new RuntimeException("Caisse non trouvée")));
        }

        Depense saved = depenseRepository.save(entity);

        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setMontant(saved.getMontant());
        transactionDTO.setDate(saved.getDateDepense());
        transactionDTO.setType(fromCompte ? "VIREMENT_SORTANT" : "RETRAIT");
        transactionDTO.setStatut("VALIDE");
        transactionDTO.setDescription("Dépense: " + (saved.getLibelle() != null ? saved.getLibelle() : ""));
        transactionDTO.setReference(saved.getReference());
        if (fromCompte) {
            transactionDTO.setCompteId(dto.getCompteId());
        } else {
            transactionDTO.setCaisseId(dto.getCaisseId());
        }
        TransactionDTO createdTx = transactionService.createPaiement(transactionDTO);
        if (createdTx != null && createdTx.getId() != null) {
            saved.setTransactionId(createdTx.getId());
            saved = depenseRepository.save(saved);
        }

        return depenseMapper.toDTO(saved);
    }

    @Override
    public DepenseDTO update(Long id, DepenseDTO dto) {
        Depense existing = depenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dépense non trouvée avec l'id: " + id));

        if (dto.getMontant() == null || dto.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Le montant de la dépense doit être supérieur à zéro");
        }

        CompteBancaire oldCompte = existing.getCompteBancaire();
        Caisse oldCaisse = existing.getCaisse();
        BigDecimal oldMontantDepense = existing.getMontant() != null ? existing.getMontant() : BigDecimal.ZERO;
        Long txId = existing.getTransactionId();

        if (oldCompte != null) {
            financeEntityAccessService.assertCanManageCompteBancaire(
                    compteBancaireRepository.findById(oldCompte.getId())
                            .orElseThrow(() -> new RuntimeException("Compte bancaire non trouvé")));
        } else if (oldCaisse != null) {
            financeEntityAccessService.assertCanManageCaisse(
                    caisseRepository.findById(oldCaisse.getId())
                            .orElseThrow(() -> new RuntimeException("Caisse non trouvée")));
        }

        Long dtoCompteId = dto.getCompteId();
        Long dtoCaisseId = dto.getCaisseId();
        if (dtoCompteId != null && dtoCaisseId != null) {
            throw new RuntimeException(
                    "Veuillez sélectionner soit un compte bancaire, soit une caisse pour déduire le montant (pas les deux, pas aucun)");
        }
        final Long resolvedCompteId;
        final Long resolvedCaisseId;
        if (dtoCompteId != null) {
            resolvedCompteId = dtoCompteId;
            resolvedCaisseId = null;
        } else if (dtoCaisseId != null) {
            resolvedCompteId = null;
            resolvedCaisseId = dtoCaisseId;
        } else if (oldCompte != null) {
            resolvedCompteId = oldCompte.getId();
            resolvedCaisseId = null;
        } else if (oldCaisse != null) {
            resolvedCompteId = null;
            resolvedCaisseId = oldCaisse.getId();
        } else {
            resolvedCompteId = null;
            resolvedCaisseId = null;
        }
        boolean newFromCompte = resolvedCompteId != null;
        boolean newFromCaisse = resolvedCaisseId != null;
        if (newFromCompte == newFromCaisse) {
            throw new RuntimeException(
                    "Veuillez sélectionner soit un compte bancaire, soit une caisse pour déduire le montant (pas les deux, pas aucun)");
        }

        if (newFromCompte) {
            financeEntityAccessService.assertCanManageCompteBancaire(
                    compteBancaireRepository.findById(resolvedCompteId)
                            .orElseThrow(() -> new RuntimeException("Compte bancaire non trouvé avec l'id: " + resolvedCompteId)));
        } else {
            financeEntityAccessService.assertCanManageCaisse(
                    caisseRepository.findById(resolvedCaisseId)
                            .orElseThrow(() -> new RuntimeException("Caisse non trouvée avec l'id: " + resolvedCaisseId)));
        }

        existing.setLibelle(dto.getLibelle());
        existing.setMontant(dto.getMontant());
        existing.setDateDepense(dto.getDateDepense() != null ? dto.getDateDepense() : existing.getDateDepense());
        existing.setDescription(dto.getDescription());
        existing.setReference(dto.getReference());

        if (dto.getCategorieId() != null) {
            CategorieDepense categorie = categorieDepenseRepository.findById(dto.getCategorieId())
                    .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec l'id: " + dto.getCategorieId()));
            existing.setCategorie(categorie);
        }

        if (newFromCompte) {
            existing.setCompteBancaire(compteBancaireRepository.findById(resolvedCompteId)
                    .orElseThrow(() -> new RuntimeException("Compte bancaire non trouvé avec l'id: " + resolvedCompteId)));
            existing.setCaisse(null);
        } else {
            existing.setCaisse(caisseRepository.findById(resolvedCaisseId)
                    .orElseThrow(() -> new RuntimeException("Caisse non trouvée avec l'id: " + resolvedCaisseId)));
            existing.setCompteBancaire(null);
        }

        Depense saved = depenseRepository.save(existing);

        if (txId != null) {
            Optional<Transaction> txOpt = transactionRepository.findById(txId);
            if (txOpt.isPresent()) {
                Transaction tx = txOpt.get();
                if (tx.getStatut() != Transaction.StatutTransaction.VALIDE) {
                    syncDepenseTransactionMetadata(tx, saved);
                    transactionRepository.save(tx);
                } else {
                    BigDecimal montantDejaDebite = tx.getMontant() != null ? tx.getMontant() : oldMontantDepense;

                    boolean memeCompte = oldCompte != null && resolvedCompteId != null
                            && oldCompte.getId().equals(resolvedCompteId);
                    boolean memeCaisse = oldCaisse != null && resolvedCaisseId != null
                            && oldCaisse.getId().equals(resolvedCaisseId);

                    if (memeCompte) {
                        CompteBancaire compte = compteBancaireRepository.findById(resolvedCompteId)
                                .orElseThrow(() -> new RuntimeException("Compte bancaire non trouvé"));
                        applyDeltaDepenseCompte(compte, dto.getMontant().subtract(montantDejaDebite));
                        syncDepenseTransactionMetadata(tx, saved);
                        tx.setCompte(compte);
                        tx.setCaisse(null);
                        tx.setType(Transaction.TypeTransaction.VIREMENT_SORTANT);
                        tx.setMontant(saved.getMontant());
                        transactionRepository.save(tx);
                    } else if (memeCaisse) {
                        Caisse caisse = caisseRepository.findById(resolvedCaisseId)
                                .orElseThrow(() -> new RuntimeException("Caisse non trouvée"));
                        applyDeltaDepenseCaisse(caisse, dto.getMontant().subtract(montantDejaDebite));
                        syncDepenseTransactionMetadata(tx, saved);
                        tx.setCaisse(caisse);
                        tx.setCompte(null);
                        tx.setType(Transaction.TypeTransaction.RETRAIT);
                        tx.setMontant(saved.getMontant());
                        transactionRepository.save(tx);
                    } else {
                        if (montantDejaDebite.compareTo(BigDecimal.ZERO) > 0) {
                            if (oldCompte != null) {
                                CompteBancaire ancien = compteBancaireRepository.findById(oldCompte.getId())
                                        .orElseThrow(() -> new RuntimeException("Compte bancaire introuvable pour remboursement."));
                                ancien.setSolde(ancien.getSolde().add(montantDejaDebite));
                                compteBancaireRepository.save(ancien);
                            } else if (oldCaisse != null) {
                                Caisse ancienne = caisseRepository.findById(oldCaisse.getId())
                                        .orElseThrow(() -> new RuntimeException("Caisse introuvable pour remboursement."));
                                ancienne.setSolde(ancienne.getSolde().add(montantDejaDebite));
                                caisseRepository.save(ancienne);
                            }
                        }
                        if (newFromCompte) {
                            CompteBancaire compte = compteBancaireRepository.findById(resolvedCompteId)
                                    .orElseThrow(() -> new RuntimeException("Compte bancaire non trouvé"));
                            debitComptePourDepense(compte, saved.getMontant());
                            tx.setCompte(compte);
                            tx.setCaisse(null);
                            tx.setType(Transaction.TypeTransaction.VIREMENT_SORTANT);
                        } else {
                            Caisse caisse = caisseRepository.findById(resolvedCaisseId)
                                    .orElseThrow(() -> new RuntimeException("Caisse non trouvée"));
                            debitCaissePourDepense(caisse, saved.getMontant());
                            tx.setCaisse(caisse);
                            tx.setCompte(null);
                            tx.setType(Transaction.TypeTransaction.RETRAIT);
                        }
                        syncDepenseTransactionMetadata(tx, saved);
                        tx.setMontant(saved.getMontant());
                        transactionRepository.save(tx);
                    }
                }
            }
        }

        return depenseMapper.toDTO(saved);
    }

    private void syncDepenseTransactionMetadata(Transaction tx, Depense saved) {
        tx.setDescription("Dépense: " + (saved.getLibelle() != null ? saved.getLibelle() : ""));
        tx.setReference(saved.getReference());
        if (saved.getDateDepense() != null) {
            tx.setDate(saved.getDateDepense());
        }
    }

    /** Delta &gt; 0 : dépense augmentée (débit supplémentaire). Delta &lt; 0 : dépense réduite (recrédit). */
    private void applyDeltaDepenseCompte(CompteBancaire compte, BigDecimal delta) {
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        if (compte.getStatut() != CompteBancaire.StatutCompte.ACTIF) {
            throw new RuntimeException("Le compte bancaire n'est pas actif");
        }
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            if (compte.getSolde().compareTo(delta) < 0) {
                throw new RuntimeException("Solde insuffisant dans le compte bancaire pour augmenter la dépense");
            }
            compte.setSolde(compte.getSolde().subtract(delta));
        } else {
            compte.setSolde(compte.getSolde().add(delta.negate()));
        }
        compteBancaireRepository.save(compte);
    }

    private void applyDeltaDepenseCaisse(Caisse caisse, BigDecimal delta) {
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        if (caisse.getStatut() != Caisse.StatutCaisse.ACTIF) {
            throw new RuntimeException("La caisse n'est pas active");
        }
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            if (caisse.getSolde().compareTo(delta) < 0) {
                throw new RuntimeException("Solde insuffisant dans la caisse pour augmenter la dépense");
            }
            caisse.setSolde(caisse.getSolde().subtract(delta));
        } else {
            caisse.setSolde(caisse.getSolde().add(delta.negate()));
        }
        caisseRepository.save(caisse);
    }

    private void debitComptePourDepense(CompteBancaire compte, BigDecimal montant) {
        if (compte.getStatut() != CompteBancaire.StatutCompte.ACTIF) {
            throw new RuntimeException("Le compte bancaire n'est pas actif");
        }
        if (compte.getSolde().compareTo(montant) < 0) {
            throw new RuntimeException("Solde insuffisant dans le compte bancaire");
        }
        compte.setSolde(compte.getSolde().subtract(montant));
        compteBancaireRepository.save(compte);
    }

    private void debitCaissePourDepense(Caisse caisse, BigDecimal montant) {
        if (caisse.getStatut() != Caisse.StatutCaisse.ACTIF) {
            throw new RuntimeException("La caisse n'est pas active");
        }
        if (caisse.getSolde().compareTo(montant) < 0) {
            throw new RuntimeException("Solde insuffisant dans la caisse");
        }
        caisse.setSolde(caisse.getSolde().subtract(montant));
        caisseRepository.save(caisse);
    }

    @Override
    public Optional<DepenseDTO> findById(Long id) {
        return depenseRepository.findById(id).map(depenseMapper::toDTO);
    }

    @Override
    public void deleteById(Long id) {
        Depense depense = depenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dépense non trouvée avec l'id: " + id));
        if (depense.getTransactionId() != null) {
            transactionRepository.findById(depense.getTransactionId()).ifPresent(t -> {
                if (t.getStatut() == Transaction.StatutTransaction.VALIDE && t.getMontant() != null) {
                    if (t.getCompte() != null) {
                        CompteBancaire compte = compteBancaireRepository.findById(t.getCompte().getId())
                                .orElseThrow(() -> new RuntimeException("Compte bancaire introuvable pour remboursement."));
                        compte.setSolde(compte.getSolde().add(t.getMontant()));
                        compteBancaireRepository.save(compte);
                    } else if (t.getCaisse() != null) {
                        Caisse caisse = caisseRepository.findById(t.getCaisse().getId())
                                .orElseThrow(() -> new RuntimeException("Caisse introuvable pour remboursement."));
                        caisse.setSolde(caisse.getSolde().add(t.getMontant()));
                        caisseRepository.save(caisse);
                    }
                }
                transactionRepository.delete(t);
            });
        }
        depenseRepository.deleteById(id);
    }

    @Override
    public List<DepenseDTO> findAll() {
        return depenseRepository.findByOrderByDateDepenseDesc().stream()
                .map(depenseMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DepenseDTO> findByCategorie(Long categorieId) {
        CategorieDepense categorie = getCategorieById(categorieId);
        return depenseRepository.findByCategorieOrderByDateDepenseDesc(categorie).stream()
                .map(depenseMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DepensePageDTO findAllPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Depense> depensePage = depenseRepository.findAllByOrderByDateDepenseDesc(pageable);
        return toPageDTO(depensePage);
    }

    @Override
    public DepensePageDTO findByCategoriePaginated(Long categorieId, int page, int size) {
        CategorieDepense categorie = getCategorieById(categorieId);
        Pageable pageable = PageRequest.of(page, size);
        Page<Depense> depensePage = depenseRepository.findByCategorieOrderByDateDepenseDesc(categorie, pageable);
        return toPageDTO(depensePage);
    }

    @Override
    public DepensePageDTO findByDate(LocalDate date, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime dateTime = date.atStartOfDay();
        Page<Depense> depensePage = depenseRepository.findByDate(dateTime, pageable);
        return toPageDTO(depensePage);
    }

    @Override
    public DepensePageDTO findByDateRange(LocalDate startDate, LocalDate endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        Page<Depense> depensePage = depenseRepository.findByDateRange(start, end, pageable);
        return toPageDTO(depensePage);
    }

    @Override
    public DepensePageDTO findByCategorieAndDate(Long categorieId, LocalDate date, int page, int size) {
        CategorieDepense categorie = getCategorieById(categorieId);
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime dateTime = date.atStartOfDay();
        Page<Depense> depensePage = depenseRepository.findByCategorieAndDate(categorie, dateTime, pageable);
        return toPageDTO(depensePage);
    }

    @Override
    public DepensePageDTO findByCategorieAndDateRange(Long categorieId, LocalDate startDate, LocalDate endDate, int page, int size) {
        CategorieDepense categorie = getCategorieById(categorieId);
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        Page<Depense> depensePage = depenseRepository.findByCategorieAndDateRange(categorie, start, end, pageable);
        return toPageDTO(depensePage);
    }

    @Override
    public BigDecimal sumByCategorie(Long categorieId) {
        CategorieDepense categorie = getCategorieById(categorieId);
        BigDecimal sum = depenseRepository.sumByCategorie(categorie);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal sumByDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        BigDecimal sum = depenseRepository.sumByDateRange(start, end);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public DepenseUnifiedPageDTO findUnified(Long categorieId, LocalDate startDate, LocalDate endDate, int page, int size) {
        List<DepenseDTO> depenses;
        List<PaiementDTO> paiements;
        if (categorieId != null) {
            CategorieDepense cat = getCategorieById(categorieId);
            if (startDate != null && endDate != null) {
                depenses = depenseRepository.findByCategorieAndDateRangeList(cat, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX)).stream()
                        .map(depenseMapper::toDTO).collect(Collectors.toList());
                paiements = paiementService.findByCategorieIdAndDateRange(categorieId, startDate, endDate);
            } else {
                depenses = depenseRepository.findByCategorieOrderByDateDepenseDesc(cat).stream()
                        .map(depenseMapper::toDTO).collect(Collectors.toList());
                paiements = paiementService.findByCategorieId(categorieId);
            }
        } else {
            if (startDate != null && endDate != null) {
                depenses = depenseRepository.findByDateRange(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX), PageRequest.of(0, Integer.MAX_VALUE))
                        .getContent().stream().map(depenseMapper::toDTO).collect(Collectors.toList());
                paiements = paiementService.findAllWithCategorie(startDate, endDate);
            } else {
                depenses = depenseRepository.findByOrderByDateDepenseDesc().stream().map(depenseMapper::toDTO).collect(Collectors.toList());
                paiements = paiementService.findAllWithCategorie(null, null);
            }
        }
        List<UnifiedLigneDepenseDTO> lignes = new ArrayList<>();
        depenses.stream().map(this::toUnifiedLigneFromDepense).forEach(lignes::add);
        paiements.stream().map(this::toUnifiedLigneFromPaiement).forEach(lignes::add);
        lignes.sort(Comparator.comparing(UnifiedLigneDepenseDTO::getDate, Comparator.nullsLast(Comparator.reverseOrder())));
        long total = lignes.size();
        int from = Math.min(page * size, lignes.size());
        int to = Math.min(from + size, lignes.size());
        List<UnifiedLigneDepenseDTO> pageContent = from < lignes.size() ? lignes.subList(from, to) : new ArrayList<>();
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new DepenseUnifiedPageDTO(pageContent, page, size, total, totalPages);
    }

    private UnifiedLigneDepenseDTO toUnifiedLigneFromDepense(DepenseDTO d) {
        UnifiedLigneDepenseDTO u = new UnifiedLigneDepenseDTO();
        u.setType(UnifiedLigneDepenseDTO.TypeLigne.DEPENSE);
        u.setId(d.getId());
        u.setLibelle(d.getLibelle());
        u.setMontant(d.getMontant());
        u.setDate(d.getDateDepense() != null ? d.getDateDepense().toLocalDate() : null);
        u.setCategorieId(d.getCategorieId());
        u.setCategorieNom(d.getCategorieNom());
        u.setReference(d.getReference());
        u.setVoyageId(null);
        u.setNumeroVoyage(null);
        return u;
    }

    private UnifiedLigneDepenseDTO toUnifiedLigneFromPaiement(PaiementDTO p) {
        UnifiedLigneDepenseDTO u = new UnifiedLigneDepenseDTO();
        u.setType(UnifiedLigneDepenseDTO.TypeLigne.PAIEMENT);
        u.setId(p.getId());
        u.setLibelle(p.getNotes() != null && !p.getNotes().isBlank() ? p.getNotes() : (p.getReference() != null ? "Paiement " + p.getReference() : "Paiement"));
        u.setMontant(p.getMontant());
        u.setDate(p.getDate());
        u.setCategorieId(p.getCategorieId());
        u.setCategorieNom(p.getCategorieNom());
        u.setReference(p.getReference());
        u.setVoyageId(p.getVoyageId());
        u.setNumeroVoyage(p.getNumeroVoyage());
        return u;
    }

    private CategorieDepense getCategorieById(Long categorieId) {
        return categorieDepenseRepository.findById(categorieId)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec l'id: " + categorieId));
    }

    private DepensePageDTO toPageDTO(Page<Depense> depensePage) {
        List<DepenseDTO> depenses = depensePage.getContent().stream()
                .map(depenseMapper::toDTO)
                .collect(Collectors.toList());
        
        return new DepensePageDTO(
                depenses,
                depensePage.getNumber(),
                depensePage.getSize(),
                depensePage.getTotalElements(),
                depensePage.getTotalPages()
        );
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "Système";
    }
}

