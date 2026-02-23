package com.backend.gesy.achat;

import com.backend.gesy.alerte.Alerte;
import com.backend.gesy.alerte.AlerteService;
import com.backend.gesy.achat.dto.AchatDTO;
import com.backend.gesy.achat.dto.AchatMapper;
import com.backend.gesy.achat.dto.AchatPageDTO;
import com.backend.gesy.achat.dto.AchatMargeDTO;
import com.backend.gesy.achat.dto.CreateAchatCessionDTO;
import com.backend.gesy.achat.dto.CreateAchatWithFactureDTO;
import com.backend.gesy.achat.dto.PayerAchatDTO;
import com.backend.gesy.client.Client;
import com.backend.gesy.client.ClientRepository;
import com.backend.gesy.client.Client.TypeClient;
import com.backend.gesy.comptebancaire.CompteBancaire;
import com.backend.gesy.comptebancaire.CompteBancaireRepository;
import com.backend.gesy.depot.Depot;
import com.backend.gesy.depot.DepotRepository;
import com.backend.gesy.facture.Facture;
import com.backend.gesy.facture.FactureRepository;
import com.backend.gesy.facture.LigneFacture;
import com.backend.gesy.fournisseur.Fournisseur;
import com.backend.gesy.fournisseur.FournisseurRepository;
import com.backend.gesy.produit.Produit;
import com.backend.gesy.produit.ProduitRepository;
import com.backend.gesy.stock.Stock;
import com.backend.gesy.stock.StockRepository;
import com.backend.gesy.transaction.Transaction;
import com.backend.gesy.transaction.TransactionRepository;
import com.backend.gesy.voyage.Voyage;
import com.backend.gesy.voyage.VoyageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Transactional
public class AchatServiceImpl implements AchatService {
    private final AchatRepository achatRepository;
    private final DepotRepository depotRepository;
    private final ProduitRepository produitRepository;
    private final AchatMapper achatMapper;
    private final VoyageRepository voyageRepository;
    private final FactureRepository factureRepository;
    private final ClientRepository clientRepository;
    private final FournisseurRepository fournisseurRepository;
    private final StockRepository stockRepository;
    private final TransactionRepository transactionRepository;
    private final CompteBancaireRepository compteBancaireRepository;
    private final AlerteService alerteService;

    /** Tri par plus récent (dateAchat DESC, id DESC) pour les listes d'achats */
    private static final Sort SORT_MOST_RECENT = Sort.by(Sort.Direction.DESC, "dateAchat")
            .and(Sort.by(Sort.Direction.DESC, "id"));

    @Override
    @Transactional(readOnly = true)
    public List<AchatDTO> findAll() {
        return StreamSupport.stream(achatRepository.findAll(SORT_MOST_RECENT).spliterator(), false)
                .map(achatMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AchatDTO findById(Long id) {
        return achatRepository.findById(id)
                .map(achatMapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Achat non trouvé avec l'id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AchatDTO> findByDepotId(Long depotId) {
        return achatRepository.findByDepotId(depotId).stream()
                .map(achatMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AchatDTO> findByProduitId(Long produitId) {
        return achatRepository.findByProduitId(produitId).stream()
                .map(achatMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AchatDTO save(AchatDTO achatDTO) {
        // Vérifier que les IDs sont fournis
        if (achatDTO.getDepotId() == null) {
            throw new RuntimeException("L'ID du dépôt est requis");
        }
        if (achatDTO.getProduitId() == null) {
            throw new RuntimeException("L'ID du produit est requis");
        }

        // Récupérer le dépôt
        Depot depot = depotRepository.findById(achatDTO.getDepotId())
                .orElseThrow(() -> new RuntimeException("Dépôt non trouvé avec l'id: " + achatDTO.getDepotId()));

        // Récupérer le produit
        Produit produit = produitRepository.findById(achatDTO.getProduitId())
                .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + achatDTO.getProduitId()));

        // Créer l'entité Achat
        Achat achat = achatMapper.toEntity(achatDTO);
        achat.setDepot(depot);
        achat.setProduit(produit);

        // Calculer le montant total si prixUnitaire et quantite sont fournis
        if (achat.getPrixUnitaire() != null && achat.getQuantite() != null) {
            BigDecimal montantTotal = achat.getPrixUnitaire()
                    .multiply(BigDecimal.valueOf(achat.getQuantite()));
            achat.setMontantTotal(montantTotal);
        }

        // Si dateAchat n'est pas fournie, utiliser la date actuelle
        if (achat.getDateAchat() == null) {
            achat.setDateAchat(java.time.LocalDateTime.now());
        }

        Achat savedAchat = achatRepository.save(achat);
        alerteService.creerAlerte(Alerte.TypeAlerte.ACHAT_ENREGISTRE,
                "Achat enregistré : " + produit.getNom() + " - " + savedAchat.getQuantite() + " "
                        + (savedAchat.getUnite() != null ? savedAchat.getUnite() : ""),
                Alerte.PrioriteAlerte.BASSE, "Achat", savedAchat.getId(), "/achats/" + savedAchat.getId());
        return achatMapper.toDTO(savedAchat);
    }

    @Override
    public void deleteById(Long id) {
        if (!achatRepository.existsById(id)) {
            throw new RuntimeException("Achat non trouvé avec l'id: " + id);
        }
        achatRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AchatPageDTO findAllPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, SORT_MOST_RECENT);
        // Liste « Tous » : uniquement les achats hors cession, triés du plus récent au
        // plus ancien
        Page<Achat> achatPage = achatRepository.findByCessionFalse(pageable);

        List<AchatDTO> achats = achatPage.getContent().stream()
                .map(achatMapper::toDTO)
                .collect(Collectors.toList());

        return new AchatPageDTO(
                achats,
                achatPage.getNumber(),
                achatPage.getTotalPages(),
                achatPage.getTotalElements(),
                achatPage.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public AchatPageDTO findByDate(LocalDate date, int page, int size) {
        LocalDateTime dateTime = date.atStartOfDay();
        Pageable pageable = PageRequest.of(page, size, SORT_MOST_RECENT);
        Page<Achat> achatPage = achatRepository.findByCessionFalseAndDateAchat(dateTime, pageable);

        List<AchatDTO> achats = achatPage.getContent().stream()
                .map(achatMapper::toDTO)
                .collect(Collectors.toList());

        return new AchatPageDTO(
                achats,
                achatPage.getNumber(),
                achatPage.getTotalPages(),
                achatPage.getTotalElements(),
                achatPage.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public AchatPageDTO findByDateRange(LocalDate startDate, LocalDate endDate, int page, int size) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        Pageable pageable = PageRequest.of(page, size, SORT_MOST_RECENT);
        Page<Achat> achatPage = achatRepository.findByCessionFalseAndDateAchatBetween(startDateTime, endDateTime,
                pageable);

        List<AchatDTO> achats = achatPage.getContent().stream()
                .map(achatMapper::toDTO)
                .collect(Collectors.toList());

        return new AchatPageDTO(
                achats,
                achatPage.getNumber(),
                achatPage.getTotalPages(),
                achatPage.getTotalElements(),
                achatPage.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public AchatPageDTO findByDepotIdPaginated(Long depotId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, SORT_MOST_RECENT);
        Page<Achat> achatPage = achatRepository.findByDepotId(depotId, pageable);

        List<AchatDTO> achats = achatPage.getContent().stream()
                .map(achatMapper::toDTO)
                .collect(Collectors.toList());

        return new AchatPageDTO(
                achats,
                achatPage.getNumber(),
                achatPage.getTotalPages(),
                achatPage.getTotalElements(),
                achatPage.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public AchatPageDTO findByDepotIdAndDate(Long depotId, LocalDate date, int page, int size) {
        LocalDateTime dateTime = date.atStartOfDay();
        Pageable pageable = PageRequest.of(page, size, SORT_MOST_RECENT);
        Page<Achat> achatPage = achatRepository.findByDepotIdAndDateAchat(depotId, dateTime, pageable);

        List<AchatDTO> achats = achatPage.getContent().stream()
                .map(achatMapper::toDTO)
                .collect(Collectors.toList());

        return new AchatPageDTO(
                achats,
                achatPage.getNumber(),
                achatPage.getTotalPages(),
                achatPage.getTotalElements(),
                achatPage.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public AchatPageDTO findByDepotIdAndDateRange(Long depotId, LocalDate startDate, LocalDate endDate, int page,
            int size) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        Pageable pageable = PageRequest.of(page, size, SORT_MOST_RECENT);
        Page<Achat> achatPage = achatRepository.findByDepotIdAndDateAchatBetween(depotId, startDateTime, endDateTime,
                pageable);

        List<AchatDTO> achats = achatPage.getContent().stream()
                .map(achatMapper::toDTO)
                .collect(Collectors.toList());

        return new AchatPageDTO(
                achats,
                achatPage.getNumber(),
                achatPage.getTotalPages(),
                achatPage.getTotalElements(),
                achatPage.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public AchatMargeDTO calculateMarge(Long achatId) {
        // Récupérer l'achat
        Achat achat = achatRepository.findById(achatId)
                .orElseThrow(() -> new RuntimeException("Achat non trouvé avec l'id: " + achatId));

        Produit produit = achat.getProduit();
        if (produit == null) {
            throw new RuntimeException("Le produit de l'achat est introuvable");
        }

        // 1. Récupérer tous les achats du même produit (pour calculer la répartition
        // des frais)
        List<Achat> tousAchatsProduit = achatRepository.findByProduitId(produit.getId())
                .stream()
                .sorted(Comparator.comparing(Achat::getDateAchat)) // Trier par date (FIFO)
                .collect(Collectors.toList());

        // 2. Récupérer tous les voyages qui utilisent ce produit
        List<Voyage> voyages = voyageRepository.findByProduit(produit);

        // 3. Calculer les frais totaux (somme de toutes les transactions des voyages)
        BigDecimal fraisTotaux = BigDecimal.ZERO;
        for (Voyage voyage : voyages) {
            if (voyage.getTransactions() != null) {
                for (Transaction transaction : voyage.getTransactions()) {
                    if (transaction.getMontant() != null) {
                        fraisTotaux = fraisTotaux.add(transaction.getMontant());
                    }
                }
            }
        }

        // 4. Calculer la quantité totale achetée pour ce produit
        Double quantiteTotaleAchetee = tousAchatsProduit.stream()
                .mapToDouble(a -> a.getQuantite() != null ? a.getQuantite() : 0.0)
                .sum();

        // 5. Répartir les frais proportionnellement selon les quantités achetées
        BigDecimal fraisProportionnels = BigDecimal.ZERO;
        if (quantiteTotaleAchetee > 0 && achat.getQuantite() != null) {
            BigDecimal proportion = BigDecimal.valueOf(achat.getQuantite())
                    .divide(BigDecimal.valueOf(quantiteTotaleAchetee), 4, RoundingMode.HALF_UP);
            fraisProportionnels = fraisTotaux.multiply(proportion);
        }

        // 6. Calculer le coût réel par litre = prix d'achat + (frais proportionnels /
        // quantité achetée)
        BigDecimal coutReelParLitre = achat.getPrixUnitaire();
        if (achat.getQuantite() != null && achat.getQuantite() > 0) {
            BigDecimal fraisParLitre = fraisProportionnels
                    .divide(BigDecimal.valueOf(achat.getQuantite()), 2, RoundingMode.HALF_UP);
            coutReelParLitre = achat.getPrixUnitaire().add(fraisParLitre);
        }

        // 7. Calculer les quantités vendues et le prix de vente (méthode FIFO)
        // Créer une map pour suivre les quantités restantes de chaque achat
        java.util.Map<Long, Double> quantitesRestantes = new java.util.HashMap<>();
        for (Achat a : tousAchatsProduit) {
            quantitesRestantes.put(a.getId(), a.getQuantite() != null ? a.getQuantite() : 0.0);
        }

        // Trier les voyages par date de départ (FIFO)
        List<Voyage> voyagesTries = voyages.stream()
                .filter(v -> v.getDateDepart() != null)
                .sorted(Comparator.comparing(Voyage::getDateDepart))
                .collect(Collectors.toList());

        // Variables pour cet achat spécifique
        Double quantiteVendue = 0.0;
        BigDecimal sommeMontantsVente = BigDecimal.ZERO;

        // Parcourir les voyages et attribuer les ventes aux achats dans l'ordre FIFO
        for (Voyage voyage : voyagesTries) {
            // Récupérer les factures du voyage (maintenant une liste)
            if (voyage.getFactures() != null && !voyage.getFactures().isEmpty()) {
                // Parcourir toutes les factures du voyage
                for (Facture facture : voyage.getFactures()) {
                    if (facture != null && facture.getLignes() != null) {
                        // Trouver la ligne de facture correspondant au produit
                        for (LigneFacture ligne : facture.getLignes()) {
                            if (ligne.getProduit() != null && ligne.getProduit().getId().equals(produit.getId())) {
                                Double quantiteLigne = ligne.getQuantite() != null ? ligne.getQuantite() : 0.0;
                                BigDecimal prixUnitaireLigne = ligne.getPrixUnitaire() != null ? ligne.getPrixUnitaire()
                                        : BigDecimal.ZERO;
                                Double quantiteRestanteLigne = quantiteLigne;

                                // Attribuer les quantités aux achats dans l'ordre FIFO
                                for (Achat achatFIFO : tousAchatsProduit) {
                                    if (quantiteRestanteLigne <= 0) {
                                        break;
                                    }

                                    Double quantiteRestanteAchatFIFO = quantitesRestantes.get(achatFIFO.getId());
                                    if (quantiteRestanteAchatFIFO > 0) {
                                        // Calculer combien de cette quantité provient de cet achat (FIFO)
                                        Double quantiteVendueDeCetAchatFIFO = Math.min(quantiteRestanteLigne,
                                                quantiteRestanteAchatFIFO);

                                        // Si c'est l'achat qu'on analyse, enregistrer les données
                                        if (achatFIFO.getId().equals(achat.getId())) {
                                            quantiteVendue += quantiteVendueDeCetAchatFIFO;
                                            BigDecimal montantLigne = prixUnitaireLigne
                                                    .multiply(BigDecimal.valueOf(quantiteVendueDeCetAchatFIFO));
                                            sommeMontantsVente = sommeMontantsVente.add(montantLigne);
                                        }

                                        // Mettre à jour les quantités restantes
                                        quantitesRestantes.put(achatFIFO.getId(),
                                                quantiteRestanteAchatFIFO - quantiteVendueDeCetAchatFIFO);
                                        quantiteRestanteLigne -= quantiteVendueDeCetAchatFIFO;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        BigDecimal montantVenteTotal = sommeMontantsVente;
        BigDecimal prixVenteMoyen = BigDecimal.ZERO;

        // Calculer le prix de vente moyen
        if (quantiteVendue > 0) {
            prixVenteMoyen = sommeMontantsVente.divide(BigDecimal.valueOf(quantiteVendue), 2, RoundingMode.HALF_UP);
            montantVenteTotal = sommeMontantsVente;
        }

        // 8. Calculer la marge
        BigDecimal margeBrute = BigDecimal.ZERO;
        BigDecimal margeNet = BigDecimal.ZERO;
        BigDecimal margePourcentage = BigDecimal.ZERO;

        if (quantiteVendue > 0) {
            // Marge brute = (quantité vendue * prix de vente) - (quantité vendue * prix
            // d'achat)
            BigDecimal revenus = prixVenteMoyen.multiply(BigDecimal.valueOf(quantiteVendue));
            BigDecimal coutAchat = achat.getPrixUnitaire().multiply(BigDecimal.valueOf(quantiteVendue));
            margeBrute = revenus.subtract(coutAchat);

            // Marge nette = marge brute - frais proportionnels
            margeNet = margeBrute.subtract(fraisProportionnels);

            // Pourcentage de marge = (marge nette / revenus) * 100
            if (revenus.compareTo(BigDecimal.ZERO) > 0) {
                margePourcentage = margeNet.divide(revenus, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
        }

        // 9. Calculer le stock restant
        Double stockRestant = (achat.getQuantite() != null ? achat.getQuantite() : 0.0) - quantiteVendue;
        BigDecimal valeurStockRestant = BigDecimal.ZERO;
        if (stockRestant > 0) {
            valeurStockRestant = coutReelParLitre.multiply(BigDecimal.valueOf(stockRestant));
        }

        // Créer et retourner le DTO
        AchatMargeDTO margeDTO = new AchatMargeDTO();
        margeDTO.setAchatId(achat.getId());
        margeDTO.setQuantiteAchetee(achat.getQuantite());
        margeDTO.setPrixUnitaireAchat(achat.getPrixUnitaire());
        margeDTO.setMontantTotalAchat(achat.getMontantTotal());
        margeDTO.setFraisTotaux(fraisTotaux);
        margeDTO.setFraisProportionnels(fraisProportionnels);
        margeDTO.setCoutReelParLitre(coutReelParLitre);
        margeDTO.setQuantiteVendue(quantiteVendue);
        margeDTO.setPrixVenteMoyen(prixVenteMoyen);
        margeDTO.setMontantVenteTotal(montantVenteTotal);
        margeDTO.setMargeBrute(margeBrute);
        margeDTO.setMargeNet(margeNet);
        margeDTO.setMargePourcentage(margePourcentage);
        margeDTO.setStockRestant(stockRestant);
        margeDTO.setValeurStockRestant(valeurStockRestant);

        return margeDTO;
    }

    @Override
    public AchatDTO createAchatWithFacture(CreateAchatWithFactureDTO dto) {
        // Récupérer le dépôt
        Depot depot = depotRepository.findById(dto.getDepotId())
                .orElseThrow(() -> new RuntimeException("Dépôt non trouvé avec l'id: " + dto.getDepotId()));

        // Récupérer le produit
        Produit produit = produitRepository.findById(dto.getProduitId())
                .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + dto.getProduitId()));

        // Récupérer le fournisseur
        Fournisseur fournisseur = fournisseurRepository.findById(dto.getFournisseurId())
                .orElseThrow(() -> new RuntimeException("Fournisseur non trouvé avec l'id: " + dto.getFournisseurId()));

        // Trouver ou créer un client à partir du fournisseur
        Client client = clientRepository.findByEmail(fournisseur.getEmail())
                .orElseGet(() -> {
                    // Créer un nouveau client à partir du fournisseur
                    Client newClient = new Client();
                    newClient.setNom(fournisseur.getNom());
                    newClient.setEmail(fournisseur.getEmail());
                    newClient.setTelephone(fournisseur.getTelephone());
                    newClient.setAdresse(fournisseur.getAdresse());
                    newClient.setType(TypeClient.ENTREPRISE);
                    newClient.setVille(fournisseur.getVille());
                    newClient.setPays(fournisseur.getPays());
                    return clientRepository.save(newClient);
                });

        // Calculer le montant total
        BigDecimal montantTotal = dto.getPrixUnitaire()
                .multiply(BigDecimal.valueOf(dto.getQuantite()));

        // Créer une transaction sortante (paiement) avec statut EN_ATTENTE
        Transaction transaction = new Transaction();
        transaction.setType(Transaction.TypeTransaction.VIREMENT_SORTANT);
        transaction.setMontant(montantTotal);
        transaction.setDate(LocalDateTime.now());
        transaction.setStatut(Transaction.StatutTransaction.EN_ATTENTE);
        transaction.setDescription("Paiement achat - " + produit.getNom() + " - " + fournisseur.getNom());
        transaction.setReference("ACHAT-" + System.currentTimeMillis());
        transaction.setBeneficiaire(fournisseur.getNom());

        Transaction savedTransaction = transactionRepository.save(transaction);

        // Créer l'achat (sans approvisionner le dépôt)
        Achat achat = new Achat();
        achat.setDepot(depot);
        achat.setProduit(produit);
        achat.setQuantite(dto.getQuantite());
        achat.setPrixUnitaire(dto.getPrixUnitaire());
        achat.setMontantTotal(montantTotal);
        achat.setDateAchat(LocalDateTime.now());
        achat.setDescription(dto.getDescription());
        achat.setNotes(dto.getNotes());
        achat.setUnite(dto.getUnite() != null ? dto.getUnite() : "L"); // Unité par défaut
        achat.setTransaction(savedTransaction);

        Achat savedAchat = achatRepository.save(achat);
        alerteService.creerAlerte(Alerte.TypeAlerte.ACHAT_ENREGISTRE,
                "Achat avec facture : " + produit.getNom() + " - " + savedAchat.getQuantite() + " "
                        + (savedAchat.getUnite() != null ? savedAchat.getUnite() : ""),
                Alerte.PrioriteAlerte.MOYENNE, "Achat", savedAchat.getId(), "/achats/" + savedAchat.getId());
        return achatMapper.toDTO(savedAchat);
    }

    @Override
    public AchatDTO payerAchat(PayerAchatDTO dto) {
        // Récupérer l'achat
        Achat achat = achatRepository.findById(dto.getAchatId())
                .orElseThrow(() -> new RuntimeException("Achat non trouvé avec l'id: " + dto.getAchatId()));

        if (achat.getTransaction() == null) {
            throw new RuntimeException("Cet achat n'a pas de transaction associée");
        }

        Transaction transaction = achat.getTransaction();

        // Vérifier que la transaction n'est pas déjà validée
        if (transaction.getStatut() == Transaction.StatutTransaction.VALIDE) {
            throw new RuntimeException("Cette transaction est déjà validée");
        }

        // Récupérer le compte bancaire
        CompteBancaire compte = compteBancaireRepository.findById(dto.getCompteBancaireId())
                .orElseThrow(() -> new RuntimeException(
                        "Compte bancaire non trouvé avec l'id: " + dto.getCompteBancaireId()));

        // Vérifier que le compte a suffisamment de fonds
        if (compte.getSolde().compareTo(transaction.getMontant()) < 0) {
            throw new RuntimeException("Solde insuffisant sur le compte bancaire");
        }

        // Débiter le compte bancaire
        compte.setSolde(compte.getSolde().subtract(transaction.getMontant()));
        compteBancaireRepository.save(compte);

        // Mettre à jour la transaction (lier au compte et valider)
        transaction.setCompte(compte);
        transaction.setStatut(Transaction.StatutTransaction.VALIDE);
        transactionRepository.save(transaction);

        // Approvisionner le dépôt (créer ou mettre à jour le stock)
        Optional<Stock> stockOpt = stockRepository.findByDepotIdAndProduitId(
                achat.getDepot().getId(),
                achat.getProduit().getId());

        Stock stock;
        if (stockOpt.isPresent()) {
            // Mettre à jour le stock existant
            stock = stockOpt.get();
            stock.setQuantite(stock.getQuantite() + achat.getQuantite());
            // Mettre à jour le prix unitaire si nécessaire
            if (achat.getPrixUnitaire() != null) {
                stock.setPrixUnitaire(achat.getPrixUnitaire().doubleValue());
            }
        } else {
            // Créer un nouveau stock
            stock = new Stock();
            stock.setDepot(achat.getDepot());
            stock.setProduit(achat.getProduit());
            stock.setQuantite(achat.getQuantite());
            stock.setPrixUnitaire(achat.getPrixUnitaire() != null ? achat.getPrixUnitaire().doubleValue() : null);
            stock.setUnite(achat.getUnite() != null ? achat.getUnite() : "L"); // Unité par défaut
        }
        stock.setDateDerniereMiseAJour(LocalDateTime.now());
        stockRepository.save(stock);

        // Mettre à jour la capacité utilisée du dépôt
        Depot depot = achat.getDepot();
        Double capaciteUtilisee = depot.getCapaciteUtilisee() != null ? depot.getCapaciteUtilisee() : 0.0;
        depot.setCapaciteUtilisee(capaciteUtilisee + achat.getQuantite());
        depotRepository.save(depot);

        return achatMapper.toDTO(achat);
    }

    @Override
    @Transactional(readOnly = true)
    public AchatPageDTO findByStatutFacture(String statut, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, SORT_MOST_RECENT);
        // Convertir le statut de facture vers le statut de transaction
        Transaction.StatutTransaction statutTransaction;
        if ("PAYEE".equals(statut)) {
            statutTransaction = Transaction.StatutTransaction.VALIDE;
        } else if ("EMISE".equals(statut)) {
            statutTransaction = Transaction.StatutTransaction.EN_ATTENTE;
        } else {
            // Essayer de mapper directement
            try {
                statutTransaction = Transaction.StatutTransaction.valueOf(statut);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Statut invalide: " + statut);
            }
        }

        Page<Achat> achatPage = achatRepository.findByTransactionStatut(statutTransaction, pageable);

        List<AchatDTO> achats = achatPage.getContent().stream()
                .map(achatMapper::toDTO)
                .collect(Collectors.toList());

        return new AchatPageDTO(
                achats,
                achatPage.getNumber(),
                achatPage.getTotalPages(),
                achatPage.getTotalElements(),
                achatPage.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public AchatPageDTO findByStatutFactureAndDate(String statut, LocalDate date, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, SORT_MOST_RECENT);
        // Convertir le statut de facture vers le statut de transaction
        Transaction.StatutTransaction statutTransaction;
        if ("PAYEE".equals(statut)) {
            statutTransaction = Transaction.StatutTransaction.VALIDE;
        } else if ("EMISE".equals(statut)) {
            statutTransaction = Transaction.StatutTransaction.EN_ATTENTE;
        } else {
            try {
                statutTransaction = Transaction.StatutTransaction.valueOf(statut);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Statut invalide: " + statut);
            }
        }
        LocalDateTime dateTime = date.atStartOfDay();

        Page<Achat> achatPage = achatRepository.findByTransactionStatutAndDateAchat(statutTransaction, dateTime,
                pageable);

        List<AchatDTO> achats = achatPage.getContent().stream()
                .map(achatMapper::toDTO)
                .collect(Collectors.toList());

        return new AchatPageDTO(
                achats,
                achatPage.getNumber(),
                achatPage.getTotalPages(),
                achatPage.getTotalElements(),
                achatPage.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public AchatPageDTO findByStatutFactureAndDateRange(String statut, LocalDate startDate, LocalDate endDate, int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size, SORT_MOST_RECENT);
        // Convertir le statut de facture vers le statut de transaction
        Transaction.StatutTransaction statutTransaction;
        if ("PAYEE".equals(statut)) {
            statutTransaction = Transaction.StatutTransaction.VALIDE;
        } else if ("EMISE".equals(statut)) {
            statutTransaction = Transaction.StatutTransaction.EN_ATTENTE;
        } else {
            try {
                statutTransaction = Transaction.StatutTransaction.valueOf(statut);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Statut invalide: " + statut);
            }
        }
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        Page<Achat> achatPage = achatRepository.findByTransactionStatutAndDateAchatBetween(statutTransaction,
                startDateTime, endDateTime, pageable);

        List<AchatDTO> achats = achatPage.getContent().stream()
                .map(achatMapper::toDTO)
                .collect(Collectors.toList());

        return new AchatPageDTO(
                achats,
                achatPage.getNumber(),
                achatPage.getTotalPages(),
                achatPage.getTotalElements(),
                achatPage.getSize());
    }

    @Override
    public AchatDTO createAchatCession(CreateAchatCessionDTO dto) {
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new RuntimeException("Client non trouvé avec l'id: " + dto.getClientId()));
        Depot depot = depotRepository.findById(dto.getDepotId())
                .orElseThrow(() -> new RuntimeException("Dépôt non trouvé avec l'id: " + dto.getDepotId()));
        Produit produit = produitRepository.findById(dto.getProduitId())
                .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + dto.getProduitId()));
        if (dto.getQuantite() == null || dto.getQuantite() <= 0) {
            throw new RuntimeException("La quantité doit être strictement positive");
        }

        Achat achat = new Achat();
        achat.setCession(true);
        achat.setClient(client);
        achat.setDepot(depot);
        achat.setProduit(produit);
        achat.setQuantite(dto.getQuantite());
        achat.setPrixUnitaire(null);
        achat.setMontantTotal(null);
        achat.setDateAchat(LocalDateTime.now());
        achat.setDescription(dto.getDescription());
        achat.setNotes(dto.getNotes());
        achat.setUnite(dto.getUnite() != null ? dto.getUnite() : "L");
        achat.setFacture(null);
        achat.setTransaction(null);

        Achat savedAchat = achatRepository.save(achat);

        Optional<Stock> stockOpt = stockRepository.findByDepotIdAndProduitId(depot.getId(), produit.getId());
        Stock stock;
        if (stockOpt.isPresent()) {
            stock = stockOpt.get();
            stock.setQuantityCession(
                    (stock.getQuantityCession() != null ? stock.getQuantityCession() : 0.0) + dto.getQuantite());
        } else {
            stock = new Stock();
            stock.setDepot(depot);
            stock.setProduit(produit);
            stock.setQuantite(0.0);
            stock.setQuantityCession(dto.getQuantite());
            stock.setUnite(achat.getUnite() != null ? achat.getUnite() : "L");
        }
        stock.setDateDerniereMiseAJour(LocalDateTime.now());
        stockRepository.save(stock);

        // Mettre à jour la capacité utilisée du dépôt (stock en cession occupe la
        // capacité)
        Double capaciteUtilisee = depot.getCapaciteUtilisee() != null ? depot.getCapaciteUtilisee() : 0.0;
        depot.setCapaciteUtilisee(capaciteUtilisee + dto.getQuantite());
        depotRepository.save(depot);

        alerteService.creerAlerte(Alerte.TypeAlerte.ACHAT_ENREGISTRE,
                "Achat de cession : " + produit.getNom() + " - " + savedAchat.getQuantite() + " "
                        + (savedAchat.getUnite() != null ? savedAchat.getUnite() : "") + " (client: " + client.getNom()
                        + ")",
                Alerte.PrioriteAlerte.MOYENNE, "Achat", savedAchat.getId(), "/achats/" + savedAchat.getId());
        return achatMapper.toDTO(savedAchat);
    }

    @Override
    @Transactional(readOnly = true)
    public AchatPageDTO findCessionPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, SORT_MOST_RECENT);
        Page<Achat> achatPage = achatRepository.findByCessionTrue(pageable);
        List<AchatDTO> achats = achatPage.getContent().stream()
                .map(achatMapper::toDTO)
                .collect(Collectors.toList());
        return new AchatPageDTO(
                achats,
                achatPage.getNumber(),
                achatPage.getTotalPages(),
                achatPage.getTotalElements(),
                achatPage.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public AchatPageDTO findCessionByDate(LocalDate date, int page, int size) {
        LocalDateTime dateTime = date.atStartOfDay();
        Pageable pageable = PageRequest.of(page, size, SORT_MOST_RECENT);
        Page<Achat> achatPage = achatRepository.findByCessionTrueAndDateAchat(dateTime, pageable);
        List<AchatDTO> achats = achatPage.getContent().stream()
                .map(achatMapper::toDTO)
                .collect(Collectors.toList());
        return new AchatPageDTO(
                achats,
                achatPage.getNumber(),
                achatPage.getTotalPages(),
                achatPage.getTotalElements(),
                achatPage.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public AchatPageDTO findCessionByDateRange(LocalDate startDate, LocalDate endDate, int page, int size) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        Pageable pageable = PageRequest.of(page, size, SORT_MOST_RECENT);
        Page<Achat> achatPage = achatRepository.findByCessionTrueAndDateAchatBetween(startDateTime, endDateTime,
                pageable);
        List<AchatDTO> achats = achatPage.getContent().stream()
                .map(achatMapper::toDTO)
                .collect(Collectors.toList());
        return new AchatPageDTO(
                achats,
                achatPage.getNumber(),
                achatPage.getTotalPages(),
                achatPage.getTotalElements(),
                achatPage.getSize());
    }

    private String generateUniqueNumeroFacture() {
        int currentYear = LocalDate.now().getYear();
        String prefix = "FAC-ACHAT-" + currentYear + "-";

        List<Facture> factures = factureRepository.findAll();
        int nextNumber = 1;

        for (Facture f : factures) {
            if (f.getNumero() != null && f.getNumero().startsWith(prefix)) {
                try {
                    String numberPart = f.getNumero().substring(prefix.length());
                    int num = Integer.parseInt(numberPart);
                    if (num >= nextNumber) {
                        nextNumber = num + 1;
                    }
                } catch (NumberFormatException e) {
                    // Ignorer les numéros qui ne correspondent pas au format
                }
            }
        }

        return prefix + String.format("%04d", nextNumber);
    }
}
