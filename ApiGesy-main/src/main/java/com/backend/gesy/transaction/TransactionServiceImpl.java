package com.backend.gesy.transaction;

import com.backend.gesy.camion.Camion;
import com.backend.gesy.camion.CamionRepository;
import com.backend.gesy.caisse.Caisse;
import com.backend.gesy.caisse.CaisseRepository;
import com.backend.gesy.comptebancaire.CompteBancaire;
import com.backend.gesy.comptebancaire.CompteBancaireRepository;
import com.backend.gesy.facture.Facture;
import com.backend.gesy.facture.FactureRepository;
import com.backend.gesy.voyage.Voyage;
import com.backend.gesy.voyage.VoyageRepository;
import com.backend.gesy.transaction.dto.TransactionDTO;
import com.backend.gesy.transaction.dto.TransactionFilterResultDTO;
import com.backend.gesy.transaction.dto.TransactionMapper;
import com.backend.gesy.transaction.dto.TransactionPageDTO;
import com.backend.gesy.transaction.dto.TransactionStatsDTO;
import com.backend.gesy.transaction.dto.VirementRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final CamionRepository camionRepository;
    private final CompteBancaireRepository compteBancaireRepository;
    private final FactureRepository factureRepository;
    private final VoyageRepository voyageRepository;
    private final CaisseRepository caisseRepository;
    private final TransactionMapper transactionMapper;

    @Override
    public List<TransactionDTO> findAll() {
        return transactionRepository.findAll().stream()
            .map(transactionMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<TransactionDTO> findById(Long id) {
        return transactionRepository.findById(id)
            .map(transactionMapper::toDTO);
    }

    @Override
    public List<TransactionDTO> findByCompteId(Long compteId) {
        CompteBancaire compte = compteBancaireRepository.findById(compteId)
            .orElseThrow(() -> new RuntimeException("Compte bancaire non trouvé avec l'id: " + compteId));
        return transactionRepository.findByCompte(compte).stream()
            .map(transactionMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<TransactionDTO> findByCamionId(Long camionId) {
        Camion camion = camionRepository.findById(camionId)
            .orElseThrow(() -> new RuntimeException("Camion non trouvé avec l'id: " + camionId));
        return transactionRepository.findByCamion(camion).stream()
            .map(transactionMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<TransactionDTO> findByFactureId(Long factureId) {
        Facture facture = factureRepository.findById(factureId)
            .orElseThrow(() -> new RuntimeException("Facture non trouvée avec l'id: " + factureId));
        return transactionRepository.findByFacture(facture).stream()
            .map(transactionMapper::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Indique si le type de transaction correspond à une entrée d'argent (crédit sur compte/caisse).
     */
    private boolean isTransactionEntrante(Transaction.TypeTransaction type) {
        return type == Transaction.TypeTransaction.VIREMENT_ENTRANT || type == Transaction.TypeTransaction.DEPOT;
    }

    @Override
    public TransactionDTO save(TransactionDTO transactionDTO) {
        Transaction transaction = transactionMapper.toEntity(transactionDTO);
        if (transactionDTO.getCamionId() != null) {
            Camion camion = camionRepository.findById(transactionDTO.getCamionId())
                .orElseThrow(() -> new RuntimeException("Camion non trouvé avec l'id: " + transactionDTO.getCamionId()));
            transaction.setCamion(camion);
        }
        if (transactionDTO.getCompteId() != null) {
            CompteBancaire compte = compteBancaireRepository.findById(transactionDTO.getCompteId())
                .orElseThrow(() -> new RuntimeException("Compte bancaire non trouvé avec l'id: " + transactionDTO.getCompteId()));
            transaction.setCompte(compte);
            // Mettre à jour le solde du compte si la transaction est validée
            if (transaction.getStatut() == Transaction.StatutTransaction.VALIDE && transaction.getMontant() != null) {
                if (isTransactionEntrante(transaction.getType())) {
                    compte.setSolde(compte.getSolde().add(transaction.getMontant()));
                } else {
                    if (compte.getStatut() != CompteBancaire.StatutCompte.ACTIF) {
                        throw new RuntimeException("Le compte bancaire n'est pas actif");
                    }
                    if (compte.getSolde().compareTo(transaction.getMontant()) < 0) {
                        throw new RuntimeException("Solde insuffisant sur le compte bancaire");
                    }
                    compte.setSolde(compte.getSolde().subtract(transaction.getMontant()));
                }
                compteBancaireRepository.save(compte);
            }
        }
        if (transactionDTO.getFactureId() != null) {
            Facture facture = factureRepository.findById(transactionDTO.getFactureId())
                .orElseThrow(() -> new RuntimeException("Facture non trouvée avec l'id: " + transactionDTO.getFactureId()));
            transaction.setFacture(facture);
            facture.setMontantPaye(facture.getMontantPaye().add(transaction.getMontant()));
        }
        if (transactionDTO.getVoyageId() != null) {
            Voyage voyage = voyageRepository.findById(transactionDTO.getVoyageId())
                .orElseThrow(() -> new RuntimeException("Voyage non trouvé avec l'id: " + transactionDTO.getVoyageId()));
            transaction.setVoyage(voyage);
            voyage.getTransactions().add(transaction);
        }
        if (transactionDTO.getCaisseId() != null) {
            Caisse caisse = caisseRepository.findById(transactionDTO.getCaisseId())
                .orElseThrow(() -> new RuntimeException("Caisse non trouvée avec l'id: " + transactionDTO.getCaisseId()));
            transaction.setCaisse(caisse);
            // Mettre à jour le solde de la caisse si la transaction est validée
            if (transaction.getStatut() == Transaction.StatutTransaction.VALIDE && transaction.getMontant() != null) {
                if (caisse.getStatut() != Caisse.StatutCaisse.ACTIF) {
                    throw new RuntimeException("La caisse n'est pas active");
                }
                if (isTransactionEntrante(transaction.getType())) {
                    caisse.setSolde(caisse.getSolde().add(transaction.getMontant()));
                } else {
                    if (caisse.getSolde().compareTo(transaction.getMontant()) < 0) {
                        throw new RuntimeException("Solde insuffisant dans la caisse");
                    }
                    caisse.setSolde(caisse.getSolde().subtract(transaction.getMontant()));
                }
                caisseRepository.save(caisse);
            }
        }
        if (transactionDTO.getTransactionLieeId() != null) {
            Transaction transactionLiee = transactionRepository.findById(transactionDTO.getTransactionLieeId())
                .orElseThrow(() -> new RuntimeException("Transaction liée non trouvée avec l'id: " + transactionDTO.getTransactionLieeId()));
            transaction.setTransactionLiee(transactionLiee);
        }
        transaction.setDate(LocalDateTime.now());
        Transaction savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toDTO(savedTransaction);
    }

    @Override
    public TransactionDTO update(Long id, TransactionDTO transactionDTO) {
        Transaction existingTransaction = transactionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Transaction non trouvée avec l'id: " + id));
        Transaction transaction = transactionMapper.toEntity(transactionDTO);
        transaction.setId(existingTransaction.getId());
        if (transactionDTO.getCamionId() != null) {
            Camion camion = camionRepository.findById(transactionDTO.getCamionId())
                .orElseThrow(() -> new RuntimeException("Camion non trouvé avec l'id: " + transactionDTO.getCamionId()));
            transaction.setCamion(camion);
        } else {
            transaction.setCamion(existingTransaction.getCamion());
        }
        if (transactionDTO.getCompteId() != null) {
            CompteBancaire compte = compteBancaireRepository.findById(transactionDTO.getCompteId())
                .orElseThrow(() -> new RuntimeException("Compte bancaire non trouvé avec l'id: " + transactionDTO.getCompteId()));
            transaction.setCompte(compte);
        } else {
            transaction.setCompte(existingTransaction.getCompte());
        }
        if (transactionDTO.getFactureId() != null) {
            Facture facture = factureRepository.findById(transactionDTO.getFactureId())
                .orElseThrow(() -> new RuntimeException("Facture non trouvée avec l'id: " + transactionDTO.getFactureId()));
            transaction.setFacture(facture);
        } else {
            transaction.setFacture(existingTransaction.getFacture());
        }
        if (transactionDTO.getVoyageId() != null) {
            Voyage voyage = voyageRepository.findById(transactionDTO.getVoyageId())
                .orElseThrow(() -> new RuntimeException("Voyage non trouvé avec l'id: " + transactionDTO.getVoyageId()));
            transaction.setVoyage(voyage);
        } else {
            transaction.setVoyage(existingTransaction.getVoyage());
        }
        if (transactionDTO.getCaisseId() != null) {
            Caisse caisse = caisseRepository.findById(transactionDTO.getCaisseId())
                .orElseThrow(() -> new RuntimeException("Caisse non trouvée avec l'id: " + transactionDTO.getCaisseId()));
            transaction.setCaisse(caisse);
        } else {
            transaction.setCaisse(existingTransaction.getCaisse());
        }
        if (transactionDTO.getTransactionLieeId() != null) {
            Transaction transactionLiee = transactionRepository.findById(transactionDTO.getTransactionLieeId())
                .orElseThrow(() -> new RuntimeException("Transaction liée non trouvée avec l'id: " + transactionDTO.getTransactionLieeId()));
            transaction.setTransactionLiee(transactionLiee);
        } else {
            transaction.setTransactionLiee(existingTransaction.getTransactionLiee());
        }
        transaction.setDate(LocalDateTime.now());
        Transaction updatedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toDTO(updatedTransaction);
    }

    @Override
    public void deleteById(Long id) {
        transactionRepository.deleteById(id);
    }

    @Override
    public List<TransactionDTO> createTransactionLiee(VirementRequestDTO request) {
        List<TransactionDTO> transactionsCreees = new ArrayList<>();
        
        // Vérifier que le montant est valide
        if (request.getMontant() == null || request.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Le montant doit être supérieur à zéro");
        }

        // Vérifier que la date est fournie
        LocalDateTime dateTransaction = LocalDateTime.now();
        
        // Déterminer le statut
        Transaction.StatutTransaction statut = request.getStatut() != null 
            ? Transaction.StatutTransaction.valueOf(request.getStatut())
            : Transaction.StatutTransaction.EN_ATTENTE;

        if ("VIREMENT".equals(request.getType())) {
            // VIREMENT ENTRE DEUX COMPTES BANCAIRES
            if (request.getCompteSourceId() == null || request.getCompteDestinationId() == null) {
                throw new RuntimeException("Les comptes source et destination sont requis pour un virement");
            }

            CompteBancaire compteSource = compteBancaireRepository.findById(request.getCompteSourceId())
                .orElseThrow(() -> new RuntimeException("Compte source non trouvé"));
            CompteBancaire compteDestination = compteBancaireRepository.findById(request.getCompteDestinationId())
                .orElseThrow(() -> new RuntimeException("Compte destination non trouvé"));

            // Vérifier que les comptes sont actifs
            if (compteSource.getStatut() != CompteBancaire.StatutCompte.ACTIF) {
                throw new RuntimeException("Le compte source n'est pas actif");
            }
            if (compteDestination.getStatut() != CompteBancaire.StatutCompte.ACTIF) {
                throw new RuntimeException("Le compte destination n'est pas actif");
            }

            // Vérifier que le solde du compte source est suffisant
            if (compteSource.getSolde().compareTo(request.getMontant()) < 0) {
                throw new RuntimeException("Solde insuffisant sur le compte source");
            }

            // Créer la transaction sortante
            Transaction transactionSortante = new Transaction();
            transactionSortante.setType(Transaction.TypeTransaction.VIREMENT_SORTANT);
            transactionSortante.setMontant(request.getMontant());
            transactionSortante.setDate(dateTransaction);
            transactionSortante.setCompte(compteSource);
            transactionSortante.setStatut(statut);
            transactionSortante.setDescription(request.getDescription() != null 
                ? request.getDescription() 
                : "Virement vers " + compteDestination.getBanque() + " - " + compteDestination.getNumero());
            transactionSortante.setReference(request.getReference());

            // Créer la transaction entrante
            Transaction transactionEntrante = new Transaction();
            transactionEntrante.setType(Transaction.TypeTransaction.VIREMENT_ENTRANT);
            transactionEntrante.setMontant(request.getMontant());
            transactionEntrante.setDate(dateTransaction);
            transactionEntrante.setCompte(compteDestination);
            transactionEntrante.setStatut(statut);
            transactionEntrante.setDescription(request.getDescription() != null 
                ? request.getDescription() 
                : "Virement depuis " + compteSource.getBanque() + " - " + compteSource.getNumero());
            transactionEntrante.setReference(request.getReference());

            // Sauvegarder les transactions
            Transaction savedSortante = transactionRepository.save(transactionSortante);
            Transaction savedEntrante = transactionRepository.save(transactionEntrante);

            // Lier les transactions
            savedSortante.setTransactionLiee(savedEntrante);
            savedEntrante.setTransactionLiee(savedSortante);
            transactionRepository.save(savedSortante);
            transactionRepository.save(savedEntrante);

            // Mettre à jour les soldes uniquement si la transaction est validée
            if (statut == Transaction.StatutTransaction.VALIDE) {
                compteSource.setSolde(compteSource.getSolde().subtract(request.getMontant()));
                compteDestination.setSolde(compteDestination.getSolde().add(request.getMontant()));
                compteBancaireRepository.save(compteSource);
                compteBancaireRepository.save(compteDestination);
            }

            transactionsCreees.add(transactionMapper.toDTO(savedSortante));
            transactionsCreees.add(transactionMapper.toDTO(savedEntrante));

        } else if ("DEPOT".equals(request.getType())) {
            // DÉPÔT : CAISSE VERS BANQUE
            if (request.getCaisseId() == null || request.getCompteDestinationId() == null) {
                throw new RuntimeException("La caisse et le compte bancaire sont requis pour un dépôt");
            }

            Caisse caisse = caisseRepository.findById(request.getCaisseId())
                .orElseThrow(() -> new RuntimeException("Caisse non trouvée"));
            CompteBancaire compteDestination = compteBancaireRepository.findById(request.getCompteDestinationId())
                .orElseThrow(() -> new RuntimeException("Compte bancaire non trouvé"));

            // Vérifier que la caisse et le compte sont actifs
            if (caisse.getStatut() != Caisse.StatutCaisse.ACTIF) {
                throw new RuntimeException("La caisse n'est pas active");
            }
            if (compteDestination.getStatut() != CompteBancaire.StatutCompte.ACTIF) {
                throw new RuntimeException("Le compte bancaire n'est pas actif");
            }

            // Vérifier que le solde de la caisse est suffisant
            if (caisse.getSolde().compareTo(request.getMontant()) < 0) {
                throw new RuntimeException("Solde insuffisant dans la caisse");
            }

            // Créer la transaction sortante (caisse)
            Transaction transactionSortante = new Transaction();
            transactionSortante.setType(Transaction.TypeTransaction.RETRAIT);
            transactionSortante.setMontant(request.getMontant());
            transactionSortante.setDate(dateTransaction);
            transactionSortante.setCaisse(caisse);
            transactionSortante.setStatut(statut);
            transactionSortante.setDescription(request.getDescription() != null 
                ? request.getDescription() 
                : "Dépôt vers " + compteDestination.getBanque() + " - " + compteDestination.getNumero());
            transactionSortante.setReference(request.getReference());

            // Créer la transaction entrante (banque)
            Transaction transactionEntrante = new Transaction();
            transactionEntrante.setType(Transaction.TypeTransaction.DEPOT);
            transactionEntrante.setMontant(request.getMontant());
            transactionEntrante.setDate(dateTransaction);
            transactionEntrante.setCompte(compteDestination);
            transactionEntrante.setStatut(statut);
            transactionEntrante.setDescription(request.getDescription() != null 
                ? request.getDescription() 
                : "Dépôt depuis la caisse " + caisse.getNom());
            transactionEntrante.setReference(request.getReference());

            // Sauvegarder les transactions
            Transaction savedSortante = transactionRepository.save(transactionSortante);
            Transaction savedEntrante = transactionRepository.save(transactionEntrante);

            // Lier les transactions
            savedSortante.setTransactionLiee(savedEntrante);
            savedEntrante.setTransactionLiee(savedSortante);
            transactionRepository.save(savedSortante);
            transactionRepository.save(savedEntrante);

            // Mettre à jour les soldes uniquement si la transaction est validée
            if (statut == Transaction.StatutTransaction.VALIDE) {
                caisse.setSolde(caisse.getSolde().subtract(request.getMontant()));
                compteDestination.setSolde(compteDestination.getSolde().add(request.getMontant()));
                caisseRepository.save(caisse);
                compteBancaireRepository.save(compteDestination);
            }

            transactionsCreees.add(transactionMapper.toDTO(savedSortante));
            transactionsCreees.add(transactionMapper.toDTO(savedEntrante));

        } else if ("RETRAIT".equals(request.getType())) {
            // RETRAIT : BANQUE VERS CAISSE
            if (request.getCompteSourceId() == null || request.getCaisseId() == null) {
                throw new RuntimeException("Le compte bancaire et la caisse sont requis pour un retrait");
            }

            CompteBancaire compteSource = compteBancaireRepository.findById(request.getCompteSourceId())
                .orElseThrow(() -> new RuntimeException("Compte bancaire non trouvé"));
            Caisse caisse = caisseRepository.findById(request.getCaisseId())
                .orElseThrow(() -> new RuntimeException("Caisse non trouvée"));

            // Vérifier que le compte et la caisse sont actifs
            if (compteSource.getStatut() != CompteBancaire.StatutCompte.ACTIF) {
                throw new RuntimeException("Le compte bancaire n'est pas actif");
            }
            if (caisse.getStatut() != Caisse.StatutCaisse.ACTIF) {
                throw new RuntimeException("La caisse n'est pas active");
            }

            // Vérifier que le solde du compte bancaire est suffisant
            if (compteSource.getSolde().compareTo(request.getMontant()) < 0) {
                throw new RuntimeException("Solde insuffisant sur le compte bancaire");
            }

            // Créer la transaction sortante (banque)
            Transaction transactionSortante = new Transaction();
            transactionSortante.setType(Transaction.TypeTransaction.RETRAIT);
            transactionSortante.setMontant(request.getMontant());
            transactionSortante.setDate(dateTransaction);
            transactionSortante.setCompte(compteSource);
            transactionSortante.setStatut(statut);
            transactionSortante.setDescription(request.getDescription() != null 
                ? request.getDescription() 
                : "Retrait vers la caisse " + caisse.getNom());
            transactionSortante.setReference(request.getReference());

            // Créer la transaction entrante (caisse)
            Transaction transactionEntrante = new Transaction();
            transactionEntrante.setType(Transaction.TypeTransaction.DEPOT);
            transactionEntrante.setMontant(request.getMontant());
            transactionEntrante.setDate(dateTransaction);
            transactionEntrante.setCaisse(caisse);
            transactionEntrante.setStatut(statut);
            transactionEntrante.setDescription(request.getDescription() != null 
                ? request.getDescription() 
                : "Retrait depuis " + compteSource.getBanque() + " - " + compteSource.getNumero());
            transactionEntrante.setReference(request.getReference());

            // Sauvegarder les transactions
            Transaction savedSortante = transactionRepository.save(transactionSortante);
            Transaction savedEntrante = transactionRepository.save(transactionEntrante);

            // Lier les transactions
            savedSortante.setTransactionLiee(savedEntrante);
            savedEntrante.setTransactionLiee(savedSortante);
            transactionRepository.save(savedSortante);
            transactionRepository.save(savedEntrante);

            // Mettre à jour les soldes uniquement si la transaction est validée
            if (statut == Transaction.StatutTransaction.VALIDE) {
                compteSource.setSolde(compteSource.getSolde().subtract(request.getMontant()));
                caisse.setSolde(caisse.getSolde().add(request.getMontant()));
                compteBancaireRepository.save(compteSource);
                caisseRepository.save(caisse);
            }

            transactionsCreees.add(transactionMapper.toDTO(savedSortante));
            transactionsCreees.add(transactionMapper.toDTO(savedEntrante));

        } else if ("VIREMENT_SIMPLE".equals(request.getType())) {
            // VIREMENT SIMPLE : une seule transaction, pas besoin de compte source
            // On crée juste une transaction simple avec le compte destination (ou caisse)
            Transaction transactionSimple = new Transaction();
            transactionSimple.setType(Transaction.TypeTransaction.VIREMENT_SIMPLE);
            transactionSimple.setMontant(request.getMontant());
            transactionSimple.setDate(dateTransaction);
            transactionSimple.setStatut(statut);
            transactionSimple.setDescription(request.getDescription() != null 
                ? request.getDescription() 
                : "Virement simple");
            transactionSimple.setReference(request.getReference());

            // Si un compte destination est fourni, on l'associe
            if (request.getCompteDestinationId() != null) {
                CompteBancaire compteDestination = compteBancaireRepository.findById(request.getCompteDestinationId())
                    .orElse(null); // Ne pas lever d'exception si le compte n'existe pas
                if (compteDestination != null) {
                    transactionSimple.setCompte(compteDestination);
                    // Mettre à jour le solde uniquement si la transaction est validée et le compte existe
                    if (statut == Transaction.StatutTransaction.VALIDE && 
                        compteDestination.getStatut() == CompteBancaire.StatutCompte.ACTIF) {
                        compteDestination.setSolde(compteDestination.getSolde().add(request.getMontant()));
                        compteBancaireRepository.save(compteDestination);
                    }
                }
            }

            // Si une caisse est fournie, on l'associe
            if (request.getCaisseId() != null) {
                Caisse caisse = caisseRepository.findById(request.getCaisseId())
                    .orElse(null); // Ne pas lever d'exception si la caisse n'existe pas
                if (caisse != null) {
                    transactionSimple.setCaisse(caisse);
                    // Mettre à jour le solde uniquement si la transaction est validée et la caisse existe
                    if (statut == Transaction.StatutTransaction.VALIDE && 
                        caisse.getStatut() == Caisse.StatutCaisse.ACTIF) {
                        caisse.setSolde(caisse.getSolde().add(request.getMontant()));
                        caisseRepository.save(caisse);
                    }
                }
            }

            // Sauvegarder la transaction (même si aucun compte/caisse n'est associé)
            Transaction savedTransaction = transactionRepository.save(transactionSimple);
            transactionsCreees.add(transactionMapper.toDTO(savedTransaction));

        } else {
            throw new RuntimeException("Type de transaction non supporté: " + request.getType());
        }

        return transactionsCreees;
    }

    @Override
    public List<TransactionDTO> findRecentTransactions(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return transactionRepository.findRecentTransactions(pageable).stream()
            .map(transactionMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public TransactionPageDTO findAllPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactionPage = transactionRepository.findAllOrderedByDate(pageable);
        List<TransactionDTO> transactions = transactionPage.getContent().stream()
            .map(transactionMapper::toDTO)
            .collect(Collectors.toList());
        
        TransactionPageDTO dto = new TransactionPageDTO();
        dto.setTransactions(transactions);
        dto.setCurrentPage(transactionPage.getNumber());
        dto.setTotalPages(transactionPage.getTotalPages());
        dto.setTotalElements(transactionPage.getTotalElements());
        dto.setSize(transactionPage.getSize());
        return dto;
    }

    @Override
    public TransactionPageDTO findByDate(LocalDate date, int page, int size) {
        LocalDateTime startOfDay = date.atStartOfDay();
        Pageable pageable = PageRequest.of(page, size);
        
        Page<Transaction> transactionPage = transactionRepository.findByDate(startOfDay, pageable);
        List<TransactionDTO> transactions = transactionPage.getContent().stream()
            .map(transactionMapper::toDTO)
            .collect(Collectors.toList());
        
        TransactionPageDTO dto = new TransactionPageDTO();
        dto.setTransactions(transactions);
        dto.setCurrentPage(transactionPage.getNumber());
        dto.setTotalPages(transactionPage.getTotalPages());
        dto.setTotalElements(transactionPage.getTotalElements());
        dto.setSize(transactionPage.getSize());
        return dto;
    }

    @Override
    public TransactionPageDTO findByDateRange(LocalDate startDate, LocalDate endDate, int page, int size) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        Pageable pageable = PageRequest.of(page, size);
        
        Page<Transaction> transactionPage = transactionRepository.findByDateRange(startDateTime, endDateTime, pageable);
        List<TransactionDTO> transactions = transactionPage.getContent().stream()
            .map(transactionMapper::toDTO)
            .collect(Collectors.toList());
        
        TransactionPageDTO dto = new TransactionPageDTO();
        dto.setTransactions(transactions);
        dto.setCurrentPage(transactionPage.getNumber());
        dto.setTotalPages(transactionPage.getTotalPages());
        dto.setTotalElements(transactionPage.getTotalElements());
        dto.setSize(transactionPage.getSize());
        return dto;
    }

    @Override
    public List<TransactionDTO> findByDateAll(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        List<Transaction> transactions = transactionRepository.findByDateAll(startOfDay);
        return transactions.stream()
            .map(transactionMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<TransactionDTO> findByDateRangeAll(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        List<Transaction> transactions = transactionRepository.findByDateRangeAll(startDateTime, endDateTime);
        return transactions.stream()
            .map(transactionMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public TransactionPageDTO findByCompteIdPaginated(Long compteId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactionPage = transactionRepository.findByCompteId(compteId, pageable);
        List<TransactionDTO> transactions = transactionPage.getContent().stream()
            .map(transactionMapper::toDTO)
            .collect(Collectors.toList());
        
        TransactionPageDTO dto = new TransactionPageDTO();
        dto.setTransactions(transactions);
        dto.setCurrentPage(transactionPage.getNumber());
        dto.setTotalPages(transactionPage.getTotalPages());
        dto.setTotalElements(transactionPage.getTotalElements());
        dto.setSize(transactionPage.getSize());
        return dto;
    }

    @Override
    public TransactionPageDTO findByCaisseIdPaginated(Long caisseId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactionPage = transactionRepository.findByCaisseId(caisseId, pageable);
        List<TransactionDTO> transactions = transactionPage.getContent().stream()
            .map(transactionMapper::toDTO)
            .collect(Collectors.toList());
        
        TransactionPageDTO dto = new TransactionPageDTO();
        dto.setTransactions(transactions);
        dto.setCurrentPage(transactionPage.getNumber());
        dto.setTotalPages(transactionPage.getTotalPages());
        dto.setTotalElements(transactionPage.getTotalElements());
        dto.setSize(transactionPage.getSize());
        return dto;
    }

    @Override
    public TransactionPageDTO findByComptesBancairesOnlyPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactionPage = transactionRepository.findByComptesBancairesOnly(pageable);
        List<TransactionDTO> transactions = transactionPage.getContent().stream()
            .map(transactionMapper::toDTO)
            .collect(Collectors.toList());
        
        TransactionPageDTO dto = new TransactionPageDTO();
        dto.setTransactions(transactions);
        dto.setCurrentPage(transactionPage.getNumber());
        dto.setTotalPages(transactionPage.getTotalPages());
        dto.setTotalElements(transactionPage.getTotalElements());
        dto.setSize(transactionPage.getSize());
        return dto;
    }

    @Override
    public TransactionPageDTO findByCaissesOnlyPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactionPage = transactionRepository.findByCaissesOnly(pageable);
        List<TransactionDTO> transactions = transactionPage.getContent().stream()
            .map(transactionMapper::toDTO)
            .collect(Collectors.toList());
        
        TransactionPageDTO dto = new TransactionPageDTO();
        dto.setTransactions(transactions);
        dto.setCurrentPage(transactionPage.getNumber());
        dto.setTotalPages(transactionPage.getTotalPages());
        dto.setTotalElements(transactionPage.getTotalElements());
        dto.setSize(transactionPage.getSize());
        return dto;
    }

    @Override
    public TransactionPageDTO findByCompteIdAndDate(Long compteId, LocalDate date, int page, int size) {
        LocalDateTime startOfDay = date.atStartOfDay();
        Pageable pageable = PageRequest.of(page, size);
        
        Page<Transaction> transactionPage = transactionRepository.findByCompteIdAndDate(compteId, startOfDay, pageable);
        List<TransactionDTO> transactions = transactionPage.getContent().stream()
            .map(transactionMapper::toDTO)
            .collect(Collectors.toList());
        
        TransactionPageDTO dto = new TransactionPageDTO();
        dto.setTransactions(transactions);
        dto.setCurrentPage(transactionPage.getNumber());
        dto.setTotalPages(transactionPage.getTotalPages());
        dto.setTotalElements(transactionPage.getTotalElements());
        dto.setSize(transactionPage.getSize());
        return dto;
    }

    @Override
    public TransactionPageDTO findByCaisseIdAndDate(Long caisseId, LocalDate date, int page, int size) {
        LocalDateTime startOfDay = date.atStartOfDay();
        Pageable pageable = PageRequest.of(page, size);
        
        Page<Transaction> transactionPage = transactionRepository.findByCaisseIdAndDate(caisseId, startOfDay, pageable);
        List<TransactionDTO> transactions = transactionPage.getContent().stream()
            .map(transactionMapper::toDTO)
            .collect(Collectors.toList());
        
        TransactionPageDTO dto = new TransactionPageDTO();
        dto.setTransactions(transactions);
        dto.setCurrentPage(transactionPage.getNumber());
        dto.setTotalPages(transactionPage.getTotalPages());
        dto.setTotalElements(transactionPage.getTotalElements());
        dto.setSize(transactionPage.getSize());
        return dto;
    }

    @Override
    public TransactionPageDTO findByCompteIdAndDateRange(Long compteId, LocalDate startDate, LocalDate endDate, int page, int size) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        Pageable pageable = PageRequest.of(page, size);
        
        Page<Transaction> transactionPage = transactionRepository.findByCompteIdAndDateRange(compteId, startDateTime, endDateTime, pageable);
        List<TransactionDTO> transactions = transactionPage.getContent().stream()
            .map(transactionMapper::toDTO)
            .collect(Collectors.toList());
        
        TransactionPageDTO dto = new TransactionPageDTO();
        dto.setTransactions(transactions);
        dto.setCurrentPage(transactionPage.getNumber());
        dto.setTotalPages(transactionPage.getTotalPages());
        dto.setTotalElements(transactionPage.getTotalElements());
        dto.setSize(transactionPage.getSize());
        return dto;
    }

    @Override
    public TransactionPageDTO findByCaisseIdAndDateRange(Long caisseId, LocalDate startDate, LocalDate endDate, int page, int size) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        Pageable pageable = PageRequest.of(page, size);
        
        Page<Transaction> transactionPage = transactionRepository.findByCaisseIdAndDateRange(caisseId, startDateTime, endDateTime, pageable);
        List<TransactionDTO> transactions = transactionPage.getContent().stream()
            .map(transactionMapper::toDTO)
            .collect(Collectors.toList());
        
        TransactionPageDTO dto = new TransactionPageDTO();
        dto.setTransactions(transactions);
        dto.setCurrentPage(transactionPage.getNumber());
        dto.setTotalPages(transactionPage.getTotalPages());
        dto.setTotalElements(transactionPage.getTotalElements());
        dto.setSize(transactionPage.getSize());
        return dto;
    }

    @Override
    public TransactionDTO createPaiement(TransactionDTO transactionDTO) {
        // Vérifier que le montant est valide
        if (transactionDTO.getMontant() == null || transactionDTO.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Le montant doit être supérieur à zéro");
        }

        // Vérifier qu'un compte bancaire ou une caisse est sélectionné
        if (transactionDTO.getCompteId() == null && transactionDTO.getCaisseId() == null) {
            throw new RuntimeException("Un compte bancaire ou une caisse doit être sélectionné");
        }

        if (transactionDTO.getCompteId() != null && transactionDTO.getCaisseId() != null) {
            throw new RuntimeException("Veuillez sélectionner soit un compte bancaire, soit une caisse, pas les deux");
        }

        // Créer la transaction
        Transaction transaction = transactionMapper.toEntity(transactionDTO);
        
        // Définir le type de transaction si non fourni
        if (transaction.getType() == null) {
            if (transactionDTO.getCompteId() != null) {
                transaction.setType(Transaction.TypeTransaction.VIREMENT_SORTANT);
            } else {
                transaction.setType(Transaction.TypeTransaction.RETRAIT);
            }
        }

        // Définir le statut par défaut si non fourni
        if (transaction.getStatut() == null) {
            transaction.setStatut(Transaction.StatutTransaction.VALIDE);
        }

        // Définir la date si non fournie
        if (transaction.getDate() == null) {
            transaction.setDate(LocalDateTime.now());
        }

        // Gérer le compte bancaire
        if (transactionDTO.getCompteId() != null) {
            CompteBancaire compte = compteBancaireRepository.findById(transactionDTO.getCompteId())
                .orElseThrow(() -> new RuntimeException("Compte bancaire non trouvé avec l'id: " + transactionDTO.getCompteId()));
            transaction.setCompte(compte);

            // Débiter le compte bancaire si le statut est VALIDE
            if (transaction.getStatut() == Transaction.StatutTransaction.VALIDE) {
                if (transaction.getType() == Transaction.TypeTransaction.VIREMENT_SORTANT) {
                    // Vérifier que le compte est actif
                    if (compte.getStatut() != CompteBancaire.StatutCompte.ACTIF) {
                        throw new RuntimeException("Le compte bancaire n'est pas actif");
                    }
                    // Vérifier que le solde est suffisant
                    if (compte.getSolde().compareTo(transaction.getMontant()) < 0) {
                        throw new RuntimeException("Solde insuffisant dans le compte bancaire");
                    }
                    // Débiter le compte
                    compte.setSolde(compte.getSolde().subtract(transaction.getMontant()));
                    compteBancaireRepository.save(compte);
                }
            }
        }

        // Gérer la caisse
        if (transactionDTO.getCaisseId() != null) {
            Caisse caisse = caisseRepository.findById(transactionDTO.getCaisseId())
                .orElseThrow(() -> new RuntimeException("Caisse non trouvée avec l'id: " + transactionDTO.getCaisseId()));
            transaction.setCaisse(caisse);

            // Débiter la caisse si le statut est VALIDE
            if (transaction.getStatut() == Transaction.StatutTransaction.VALIDE) {
                if (transaction.getType() == Transaction.TypeTransaction.RETRAIT) {
                    // Vérifier que la caisse est active
                    if (caisse.getStatut() != Caisse.StatutCaisse.ACTIF) {
                        throw new RuntimeException("La caisse n'est pas active");
                    }
                    // Vérifier que le solde est suffisant
                    if (caisse.getSolde().compareTo(transaction.getMontant()) < 0) {
                        throw new RuntimeException("Solde insuffisant dans la caisse");
                    }
                    // Débiter la caisse
                    caisse.setSolde(caisse.getSolde().subtract(transaction.getMontant()));
                    caisseRepository.save(caisse);
                }
            }
        }

        // Sauvegarder la transaction
        Transaction savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toDTO(savedTransaction);
    }

    @Override
    public TransactionFilterResultDTO filterByCustom(Transaction.TypeTransaction type, LocalDate date, LocalDate startDate, LocalDate endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactionPage;
        BigDecimal totalMontant;

        boolean useDateRange = startDate != null && endDate != null;
        boolean useDate = !useDateRange && date != null;

        if (type != null) {
            if (useDateRange) {
                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
                transactionPage = transactionRepository.findByTypeAndDateRange(type, startDateTime, endDateTime, pageable);
                totalMontant = transactionRepository.sumMontantByTypeAndDateRange(type, startDateTime, endDateTime);
            } else if (useDate) {
                LocalDateTime startOfDay = date.atStartOfDay();
                transactionPage = transactionRepository.findByTypeAndDate(type, startOfDay, pageable);
                totalMontant = transactionRepository.sumMontantByTypeAndDate(type, startOfDay);
            } else {
                transactionPage = transactionRepository.findByType(type, pageable);
                totalMontant = transactionRepository.sumMontantByType(type);
            }
        } else {
            if (useDateRange) {
                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
                transactionPage = transactionRepository.findByDateRange(startDateTime, endDateTime, pageable);
                totalMontant = transactionRepository.sumMontantByDateRange(startDateTime, endDateTime);
            } else if (useDate) {
                LocalDateTime startOfDay = date.atStartOfDay();
                transactionPage = transactionRepository.findByDate(startOfDay, pageable);
                totalMontant = transactionRepository.sumMontantByDate(startOfDay);
            } else {
                transactionPage = transactionRepository.findAllOrderedByDate(pageable);
                totalMontant = transactionRepository.sumMontantAll();
            }
        }

        if (totalMontant == null) {
            totalMontant = BigDecimal.ZERO;
        }

        List<TransactionDTO> transactions = transactionPage.getContent().stream()
            .map(transactionMapper::toDTO)
            .collect(Collectors.toList());

        TransactionFilterResultDTO dto = new TransactionFilterResultDTO();
        dto.setTransactions(transactions);
        dto.setTotalCount(transactionPage.getTotalElements());
        dto.setTotalMontant(totalMontant);
        dto.setCurrentPage(transactionPage.getNumber());
        dto.setTotalPages(transactionPage.getTotalPages());
        dto.setSize(transactionPage.getSize());
        return dto;
    }

    @Override
    public TransactionStatsDTO getStats() {
        List<Transaction> allTransactions = transactionRepository.findAll();
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate startOfLastMonth = startOfMonth.minusMonths(1);
        LocalDate endOfLastMonth = startOfMonth.minusDays(1);

        // Transactions effectuées ce mois (statut VALIDE)
        List<Transaction> transactionsEffectuees = allTransactions.stream()
            .filter(t -> t.getStatut() == Transaction.StatutTransaction.VALIDE)
            .filter(t -> t.getDate() != null && !t.getDate().toLocalDate().isBefore(startOfMonth) && !t.getDate().toLocalDate().isAfter(now))
            .collect(Collectors.toList());

        // Transactions effectuées le mois dernier
        List<Transaction> transactionsMoisDernier = allTransactions.stream()
            .filter(t -> t.getStatut() == Transaction.StatutTransaction.VALIDE)
            .filter(t -> t.getDate() != null && !t.getDate().toLocalDate().isBefore(startOfLastMonth) && !t.getDate().toLocalDate().isAfter(endOfLastMonth))
            .collect(Collectors.toList());

        long totalEffectuees = transactionsEffectuees.size();
        BigDecimal montantEffectuees = transactionsEffectuees.stream()
            .map(Transaction::getMontant)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalMoisDernier = transactionsMoisDernier.size();
        String evolution = "0%";
        if (totalMoisDernier > 0) {
            double pourcentage = ((double)(totalEffectuees - totalMoisDernier) / totalMoisDernier) * 100;
            evolution = String.format("%.0f%%", pourcentage);
            if (pourcentage > 0) {
                evolution = "+" + evolution;
            }
        } else if (totalEffectuees > 0) {
            evolution = "+100%";
        }

        // Transactions en attente
        List<Transaction> transactionsEnAttente = allTransactions.stream()
            .filter(t -> t.getStatut() == Transaction.StatutTransaction.EN_ATTENTE)
            .collect(Collectors.toList());

        long totalEnAttente = transactionsEnAttente.size();
        BigDecimal montantEnAttente = transactionsEnAttente.stream()
            .map(Transaction::getMontant)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Pourcentage de transactions en attente
        String pourcentageEnAttente = "0%";
        if (allTransactions.size() > 0) {
            double pourcentage = ((double)totalEnAttente / allTransactions.size()) * 100;
            pourcentageEnAttente = String.format("%.0f%%", pourcentage);
        }

        // Transactions en échec (REJETE ou ANNULE)
        List<Transaction> transactionsEchec = allTransactions.stream()
            .filter(t -> t.getStatut() == Transaction.StatutTransaction.REJETE || 
                        t.getStatut() == Transaction.StatutTransaction.ANNULE)
            .collect(Collectors.toList());

        long totalEchec = transactionsEchec.size();
        BigDecimal montantEchec = transactionsEchec.stream()
            .map(Transaction::getMontant)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean urgent = totalEchec > 0;

        // Créer le DTO
        TransactionStatsDTO stats = new TransactionStatsDTO();
        
        TransactionStatsDTO.PaiementsEffectues paiementsEffectues = new TransactionStatsDTO.PaiementsEffectues();
        paiementsEffectues.setTotal(totalEffectuees);
        paiementsEffectues.setMontant(montantEffectuees);
        paiementsEffectues.setPeriode("Ce mois");
        paiementsEffectues.setEvolution(evolution);
        stats.setPaiementsEffectues(paiementsEffectues);

        TransactionStatsDTO.PaiementsEnAttente paiementsEnAttente = new TransactionStatsDTO.PaiementsEnAttente();
        paiementsEnAttente.setTotal(totalEnAttente);
        paiementsEnAttente.setMontant(montantEnAttente);
        paiementsEnAttente.setPourcentage(pourcentageEnAttente);
        stats.setPaiementsEnAttente(paiementsEnAttente);

        TransactionStatsDTO.PaiementsEchec paiementsEchec = new TransactionStatsDTO.PaiementsEchec();
        paiementsEchec.setTotal(totalEchec);
        paiementsEchec.setMontant(montantEchec);
        paiementsEchec.setUrgent(urgent);
        stats.setPaiementsEchec(paiementsEchec);

        return stats;
    }

    @Override
    public void recalculerSoldesDepuisTransactions() {
        // Recalculer le solde de chaque compte bancaire à partir des transactions validées
        for (CompteBancaire compte : compteBancaireRepository.findAll()) {
            List<Transaction> transactions = transactionRepository.findByCompte(compte).stream()
                .filter(t -> t.getStatut() == Transaction.StatutTransaction.VALIDE && t.getMontant() != null)
                .collect(Collectors.toList());
            BigDecimal solde = BigDecimal.ZERO;
            for (Transaction t : transactions) {
                if (isTransactionEntrante(t.getType())) {
                    solde = solde.add(t.getMontant());
                } else {
                    solde = solde.subtract(t.getMontant());
                }
            }
            compte.setSolde(solde);
            compteBancaireRepository.save(compte);
        }
        // Recalculer le solde de chaque caisse à partir des transactions validées
        for (Caisse caisse : caisseRepository.findAll()) {
            List<Transaction> transactions = transactionRepository.findByCaisse(caisse).stream()
                .filter(t -> t.getStatut() == Transaction.StatutTransaction.VALIDE && t.getMontant() != null)
                .collect(Collectors.toList());
            BigDecimal solde = BigDecimal.ZERO;
            for (Transaction t : transactions) {
                if (isTransactionEntrante(t.getType())) {
                    solde = solde.add(t.getMontant());
                } else {
                    solde = solde.subtract(t.getMontant());
                }
            }
            caisse.setSolde(solde);
            caisseRepository.save(caisse);
        }
    }
}

