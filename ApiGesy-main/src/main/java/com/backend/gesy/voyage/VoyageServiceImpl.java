package com.backend.gesy.voyage;

import com.backend.gesy.alerte.Alerte;
import com.backend.gesy.axe.Axe;
import com.backend.gesy.axe.AxeRepository;
import com.backend.gesy.camion.Camion;
import com.backend.gesy.compte.Compte;
import com.backend.gesy.compte.CompteRepository;
import com.backend.gesy.camion.CamionRepository;
import com.backend.gesy.douane.dto.DouaneDTO;
import com.backend.gesy.facture.LigneFacture;
import com.backend.gesy.fournisseur.Fournisseur;
import com.backend.gesy.fournisseur.FournisseurRepository;
import com.backend.gesy.client.Client;
import com.backend.gesy.client.ClientRepository;
import com.backend.gesy.depot.Depot;
import com.backend.gesy.depot.DepotRepository;
import com.backend.gesy.paiement.Paiement;
import com.backend.gesy.paiement.PaiementRepository;
import com.backend.gesy.utilisateur.Utilisateur;
import com.backend.gesy.utilisateur.UtilisateurRepository;
import com.backend.gesy.mouvement.MouvementRepository;
import com.backend.gesy.produit.Produit;
import com.backend.gesy.produit.ProduitRepository;
import com.backend.gesy.transaction.Transaction;
import com.backend.gesy.transaction.TransactionRepository;
import com.backend.gesy.transaction.dto.TransactionMapper;
import com.backend.gesy.transitaire.Transitaire;
import com.backend.gesy.transitaire.TransitaireRepository;
import com.backend.gesy.transaction.TransactionService;
import com.backend.gesy.transaction.dto.TransactionDTO;
import com.backend.gesy.stock.StockRepository;
import com.backend.gesy.stock.Stock;
import com.backend.gesy.mouvement.MouvementService;
import com.backend.gesy.mouvement.dto.MouvementDTO;
import com.backend.gesy.douane.DouaneService;
import com.backend.gesy.facture.Facture;
import com.backend.gesy.facture.FactureRepository;
import com.backend.gesy.facture.FactureService;
import com.backend.gesy.facture.dto.FactureDTO;
import com.backend.gesy.facture.dto.LigneFactureDTO;
import com.backend.gesy.voyage.dto.*;
import com.backend.gesy.alerte.AlerteService;
import com.backend.gesy.manquant.Manquant;
import com.backend.gesy.manquant.ManquantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Service
@Transactional
public class VoyageServiceImpl implements VoyageService {
    @Autowired
    private VoyageRepository voyageRepository;
    @Autowired
    private CamionRepository camionRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private TransitaireRepository transitaireRepository;
    @Autowired
    private ProduitRepository produitRepository;
    @Autowired
    private DepotRepository depotRepository;
    @Autowired
    private VoyageMapper voyageMapper;
    @Autowired
    private TransactionMapper transactionMapper;
    @Autowired
    private EtatVoyageRepository etatVoyageRepository;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private MouvementService mouvementService;
    @Autowired
    private DouaneService douaneService;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private MouvementRepository mouvementRepository;
    @Autowired
    private FactureService factureService;

    @Autowired
    private FactureRepository factureRepository;
    @Autowired
    private UtilisateurRepository utilisateurRepository;
    @Autowired
    private FournisseurRepository fournisseurRepository;
    @Autowired
    private PaiementRepository paiementRepository;
    @Autowired
    private ManquantRepository manquantRepository;
    @Autowired
    private AxeRepository axeRepository;
    @Autowired
    private ClientVoyageRepository clientVoyageRepository;
    @Autowired
    private AlerteService alerteService;
    @Autowired
    private CompteRepository compteRepository;

    @Override
    public List<VoyageDTO> findAll() {
        return voyageRepository.findAll().stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<VoyageDTO> findById(Long id) {
        return voyageRepository.findById(id)
                .map(voyageMapper::toDTO);
    }

    @Override
    public Optional<VoyageDTO> findByNumeroVoyage(String numeroVoyage) {
        return voyageRepository.findByNumeroVoyage(numeroVoyage)
                .map(voyageMapper::toDTO);
    }

    @Override
    public List<VoyageDTO> findByCamionId(Long camionId) {
        Camion camion = camionRepository.findById(camionId)
                .orElseThrow(() -> new RuntimeException("Camion non trouvé avec l'id: " + camionId));
        return voyageRepository.findByCamion(camion).stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VoyageDTO> findByClientId(Long clientId) {
        // Vérifier que le client existe
        if (!clientRepository.existsById(clientId)) {
            throw new RuntimeException("Client non trouvé avec l'id: " + clientId);
        }
        
        // Récupérer les ClientVoyage pour ce client
        List<ClientVoyage> clientVoyages = clientVoyageRepository.findByClientId(clientId);
        
        // Extraire les voyages uniques et les mapper en DTO
        return clientVoyages.stream()
                .map(ClientVoyage::getVoyage)
                .distinct() // Éviter les doublons si un voyage a plusieurs ClientVoyage pour le même client
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VoyageDTO> findByTransitaireId(Long transitaireId) {
        Transitaire transitaire = transitaireRepository.findById(transitaireId)
                .orElseThrow(() -> new RuntimeException("Transitaire non trouvé avec l'id: " + transitaireId));
        return voyageRepository.findByTransitaire(transitaire).stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VoyageDTO> findVoyagesNonDeclaresByTransitaireId(Long transitaireId) {
        Transitaire transitaire = transitaireRepository.findById(transitaireId)
                .orElseThrow(() -> new RuntimeException("Transitaire non trouvé avec l'id: " + transitaireId));
        // Voyages actifs = non libérés (reste en « en cours » après déclarer ou passer non déclarer)
        return voyageRepository.findVoyagesActifsByTransitaire(transitaire)
                .stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VoyageDTO> findVoyagesNonDeclaresByTransitaireIdentifiant(String identifiant) {
        Transitaire transitaire = transitaireRepository.findByIdentifiant(identifiant)
                .orElseThrow(() -> new RuntimeException("Transitaire non trouvé avec l'identifiant: " + identifiant));
        // Voyages actifs = non libérés (reste en « en cours » après déclarer ou passer non déclarer)
        return voyageRepository.findVoyagesActifsByTransitaire(transitaire)
                .stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VoyageDTO> findByDepotId(Long depotId) {
        Depot depot = depotRepository.findById(depotId)
                .orElseThrow(() -> new RuntimeException("Dépôt non trouvé avec l'id: " + depotId));
        return voyageRepository.findByDepot(depot).stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VoyageDTO> findByDepotIdAndStatutsChargement(Long depotId) {
        Depot depot = depotRepository.findById(depotId)
                .orElseThrow(() -> new RuntimeException("Dépôt non trouvé avec l'id: " + depotId));
        return voyageRepository.findByDepotAndStatutsChargement(depot).stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VoyageDTO> findByAxeId(Long axeId) {
        Axe axe = axeRepository.findById(axeId)
                .orElseThrow(() -> new RuntimeException("Axe non trouvé avec l'id: " + axeId));
        return voyageRepository.findByAxe(axe).stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public VoyagePageDto findByAxeIdPaginated(Long axeId, int page, int size) {
        Axe axe = axeRepository.findById(axeId)
                .orElseThrow(() -> new RuntimeException("Axe non trouvé avec l'id: " + axeId));

        Pageable pageable = PageRequest.of(page, size);
        Page<Voyage> voyagePage = voyageRepository.findByAxePaginated(axe, pageable);

        List<VoyageDTO> voyages = voyagePage.getContent().stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());

        return new VoyagePageDto(
                voyages,
                voyagePage.getNumber(),
                voyagePage.getTotalPages(),
                voyagePage.getTotalElements(),
                voyagePage.getSize());
    }

    @Override
    public List<VoyageDTO> findByUtilisateurIdentifiant(String identifiant) {
        Utilisateur utilisateur = utilisateurRepository.findByIdentifiant(identifiant)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'identifiant: " + identifiant));

        if (utilisateur.getDepot() == null) {
            throw new RuntimeException("L'utilisateur n'a pas de dépôt associé");
        }

        return voyageRepository.findByDepotAndStatutsChargement(utilisateur.getDepot()).stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VoyageDTO> updateStatutMultiple(List<Long> voyageIds, String statut) {
        List<VoyageDTO> voyagesUpdated = new ArrayList<>();

        for (Long voyageId : voyageIds) {
            try {
                VoyageDTO voyageUpdated = updateStatut(voyageId, statut, null, null, null);
                voyagesUpdated.add(voyageUpdated);
            } catch (RuntimeException e) {
                // Log l'erreur mais continue avec les autres voyages
                System.err.println("Erreur lors de la mise à jour du voyage " + voyageId + ": " + e.getMessage());
            }
        }

        return voyagesUpdated;
    }

    @Override
    public Long countCamionsChargesByDepotId(Long depotId) {
        Depot depot = depotRepository.findById(depotId)
                .orElseThrow(() -> new RuntimeException("Dépôt non trouvé avec l'id: " + depotId));
        System.out.println("Vérification du voyage: " + depot.getNom());
        Long count = 0L;
        for (Voyage voyage : voyageRepository.findByDepot(depot)) {
            System.out.println("Vérification du voyage: " + voyage.getNumeroVoyage());
            for (EtatVoyage etat : etatVoyageRepository.findByVoyageId(voyage.getId())) {
                if (etat.getEtat().equals("Chargé") || etat.getEtat().equals("Départ") && etat.getValider()) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    @Override
    public VoyageDTO save(VoyageDTO voyageDTO) {
        Voyage voyage = voyageMapper.toEntity(voyageDTO);

        // Responsable obligatoire
        if (voyageDTO.getResponsableId() == null || voyageDTO.getResponsableId() <= 0) {
            throw new RuntimeException("Un responsable doit être sélectionné pour le voyage.");
        }
        Compte responsable = compteRepository.findById(voyageDTO.getResponsableId())
                .orElseThrow(() -> new RuntimeException("Compte responsable non trouvé avec l'id: " + voyageDTO.getResponsableId()));
        voyage.setResponsable(responsable);

        // Cession : pas de cout du voyage, client obligatoire
        if (Boolean.TRUE.equals(voyageDTO.getCession())) {
            voyage.setCession(true);
            voyage.setPrixUnitaire(null);
            if (voyageDTO.getClientId() == null || voyageDTO.getClientId() <= 0) {
                throw new RuntimeException("En cas de vente de type cession, un client doit être sélectionné (client ayant déjà un achat).");
            }
        }

        // Générer un numéro de voyage unique
        String numeroVoyage = generateUniqueNumeroVoyage();
        voyage.setNumeroVoyage(numeroVoyage);

        // Statut par défaut: CHARGEMENT
        if (voyage.getStatut() == null) {
            voyage.setStatut(Voyage.StatutVoyage.CHARGEMENT);
        }

        // Définir le camion et mettre à jour son statut
        Camion camion = null;
        if (voyageDTO.getCamionId() != null) {
            camion = camionRepository.findById(voyageDTO.getCamionId())
                    .orElseThrow(() -> new RuntimeException("Camion non trouvé avec l'id: " + voyageDTO.getCamionId()));
            voyage.setCamion(camion);
            voyage.setQuantite(camion.getCapacite());

            // Mettre à jour le statut du camion en fonction du statut du voyage
            updateCamionStatusFromVoyage(camion, voyage.getStatut(), voyage);
        } else {
            throw new RuntimeException("Un camion doit être sélectionné");
        }

        // Définir le client si fourni - créer un ClientVoyage
        if (voyageDTO.getClientId() != null && voyageDTO.getClientId() > 0) {
            Client client = clientRepository.findById(voyageDTO.getClientId())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé avec l'id: " + voyageDTO.getClientId()));
            
            // Créer un ClientVoyage pour ce voyage
            ClientVoyage clientVoyage = new ClientVoyage();
            clientVoyage.setVoyage(voyage);
            clientVoyage.setClient(client);
            clientVoyage.setQuantite(voyage.getQuantite() != null ? voyage.getQuantite() : 0.0);
            clientVoyage.setStatut(ClientVoyage.StatutLivraison.NON_LIVRE);
            clientVoyage.setDateCreation(LocalDateTime.now());
            voyage.getClientVoyages().add(clientVoyage);
        }

        // Définir le transitaire si fourni
        if (voyageDTO.getTransitaireId() != null && voyageDTO.getTransitaireId() > 0) {
            Transitaire transitaire = transitaireRepository.findById(voyageDTO.getTransitaireId())
                    .orElseThrow(() -> new RuntimeException(
                            "Transitaire non trouvé avec l'id: " + voyageDTO.getTransitaireId()));
            voyage.setTransitaire(transitaire);
        }

        // Définir l'axe (obligatoire)
        if (voyageDTO.getAxeId() == null || voyageDTO.getAxeId() <= 0) {
            throw new RuntimeException("Un axe doit être sélectionné");
        }

        Axe axe = axeRepository.findById(voyageDTO.getAxeId())
                .orElseThrow(() -> new RuntimeException("Axe non trouvé avec l'id: " + voyageDTO.getAxeId()));
        voyage.setAxe(axe);

        // Définir le produit (obligatoire)
        Produit produit = null;
        if (voyageDTO.getProduitId() != null) {
            produit = produitRepository.findById(voyageDTO.getProduitId())
                    .orElseThrow(
                            () -> new RuntimeException("Produit non trouvé avec l'id: " + voyageDTO.getProduitId()));
            voyage.setProduit(produit);
        } else {
            throw new RuntimeException("Un produit doit être sélectionné");
        }

        // Définir le dépôt (obligatoire)
        if (voyageDTO.getDepotId() == null || voyageDTO.getDepotId() <= 0) {
            throw new RuntimeException("Un dépôt doit être sélectionné");
        }

        Depot depot = depotRepository.findById(voyageDTO.getDepotId())
                .orElseThrow(() -> new RuntimeException("Dépôt non trouvé avec l'id: " + voyageDTO.getDepotId()));
        voyage.setDepot(depot);

        // Vérifier que le produit existe dans le dépôt
        Optional<Stock> stockOpt = stockRepository
                .findByDepotIdAndProduitId(depot.getId(), produit.getId());
        if (stockOpt.isEmpty()) {
            throw new RuntimeException(
                    "Le produit " + produit.getNom() + " n'existe pas dans le dépôt " + depot.getNom());
        }

        Stock stock = stockOpt.get();

        // Vérifier que le stock est suffisant
        Double quantiteVoyage = voyage.getQuantite() != null ? voyage.getQuantite() : camion.getCapacite();
        if (stock.getQuantite() == null || stock.getQuantite() < quantiteVoyage) {
            throw new RuntimeException("Stock insuffisant dans le dépôt " + depot.getNom() +
                    ". Stock disponible: " + (stock.getQuantite() != null ? stock.getQuantite() : 0) +
                    " L, Quantité requise: " + quantiteVoyage + " L");
        }

        Voyage savedVoyage = voyageRepository.save(voyage);

        // Créer les 5 états par défaut pour le voyage
        createDefaultEtats(savedVoyage);

        // En cession : pas de cout du voyage, donc pas de transaction ni paiement
        if (!savedVoyage.isCession()) {
            Transaction transactionFraisT1 = new Transaction();
            transactionFraisT1.setMontant(savedVoyage.getPrixUnitaire().multiply(BigDecimal.valueOf(savedVoyage.getQuantite())));
            transactionFraisT1.setDate(LocalDateTime.now());
            transactionFraisT1.setStatut(Transaction.StatutTransaction.EN_ATTENTE); // En attente
            transactionFraisT1.setType(Transaction.TypeTransaction.FRAIS);
            transactionFraisT1.setDescription("Cout de transport pour le voyage " + savedVoyage.getNumeroVoyage());
            transactionFraisT1.setVoyage(savedVoyage);
            transactionFraisT1.setReference("COUT-VOY-" + savedVoyage.getNumeroVoyage());

            transactionFraisT1 = transactionRepository.save(transactionFraisT1);

            // Créer un paiement en attente avec les transactions
            Paiement paiement = new Paiement();
            paiement.setMontant(transactionFraisT1.getMontant());
            paiement.setDate(LocalDate.now());
            paiement.setMethode(Paiement.MethodePaiement.VIREMENT);
            paiement.setStatut(Paiement.StatutPaiement.EN_ATTENTE);
            paiement.setReference("PAY-COU-VOY-" + voyage.getNumeroVoyage());
            paiement.setNotes("Paiement du cout de transport " + voyage.getNumeroVoyage());
            paiement.getTransactions().add(transactionFraisT1);
            paiement.setVoyage(savedVoyage);

            paiementRepository.save(paiement);
        }

        alerteService.creerAlerte(
                Alerte.TypeAlerte.VOYAGE_CREE,
                "Nouveau voyage créé : " + savedVoyage.getNumeroVoyage(),
                Alerte.PrioriteAlerte.MOYENNE,
                "Voyage", savedVoyage.getId(), "/voyages/" + savedVoyage.getId());

        if (voyageDTO.getClientId() != null && voyageDTO.getClientId() > 0 && !savedVoyage.getClientVoyages().isEmpty()) {
            ClientVoyage cv = savedVoyage.getClientVoyages().iterator().next();
            alerteService.creerAlerte(
                    Alerte.TypeAlerte.CLIENT_ATTRIBUE,
                    "Client attribué au voyage : " + cv.getClient().getNom() + " - Voyage " + savedVoyage.getNumeroVoyage(),
                    Alerte.PrioriteAlerte.MOYENNE,
                    "ClientVoyage", cv.getId(), "/voyages/" + savedVoyage.getId());
        }

        return voyageMapper.toDTO(savedVoyage);
    }

    @Override
    public VoyageDTO update(Long id, VoyageDTO voyageDTO) {
        Voyage existingVoyage = voyageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voyage non trouvé avec l'id: " + id));

        Voyage voyage = voyageMapper.toEntity(voyageDTO);
        voyage.setId(existingVoyage.getId());
        voyage.setNumeroVoyage(existingVoyage.getNumeroVoyage()); // Conserver le numéro existant

        // Responsable : mettre à jour si fourni, sinon conserver l'existant
        if (voyageDTO.getResponsableId() != null && voyageDTO.getResponsableId() > 0) {
            Compte responsable = compteRepository.findById(voyageDTO.getResponsableId())
                    .orElseThrow(() -> new RuntimeException("Compte responsable non trouvé avec l'id: " + voyageDTO.getResponsableId()));
            voyage.setResponsable(responsable);
        } else if (voyage.getResponsable() == null && existingVoyage.getResponsable() != null) {
            voyage.setResponsable(existingVoyage.getResponsable());
        }

        // Mettre à jour le camion et son statut
        Camion camion;
        if (voyageDTO.getCamionId() != null) {
            camion = camionRepository.findById(voyageDTO.getCamionId())
                    .orElseThrow(() -> new RuntimeException("Camion non trouvé avec l'id: " + voyageDTO.getCamionId()));
            voyage.setCamion(camion);
        } else {
            camion = existingVoyage.getCamion();
            voyage.setCamion(camion);
        }

        // Mettre à jour le statut du camion en fonction du statut du voyage
        if (camion != null && voyage.getStatut() != null) {
            updateCamionStatusFromVoyage(camion, voyage.getStatut(), voyage);
        }

        // Mettre à jour le client - créer ou mettre à jour un ClientVoyage
        boolean clientAttribueDansCetteRequete = false;
        if (voyageDTO.getClientId() != null && voyageDTO.getClientId() > 0) {
            Client client = clientRepository.findById(voyageDTO.getClientId())
                    .orElseThrow(() -> new RuntimeException("Client non trouvé avec l'id: " + voyageDTO.getClientId()));
            
            // Vérifier si un ClientVoyage existe déjà pour ce voyage et ce client
            ClientVoyage clientVoyage = clientVoyageRepository
                    .findByVoyageIdAndClientId(voyage.getId(), voyageDTO.getClientId())
                    .orElse(null);
            
            if (clientVoyage == null) {
                // Créer un nouveau ClientVoyage
                clientVoyage = new ClientVoyage();
                clientVoyage.setVoyage(voyage);
                clientVoyage.setClient(client);
                clientVoyage.setQuantite(voyage.getQuantite() != null ? voyage.getQuantite() : 0.0);
                clientVoyage.setStatut(ClientVoyage.StatutLivraison.NON_LIVRE);
                clientVoyage.setDateCreation(LocalDateTime.now());
                voyage.getClientVoyages().add(clientVoyage);
                clientAttribueDansCetteRequete = true;
            }
        }
        // Si pas de clientId dans le DTO, on garde les ClientVoyage existants

        // Mettre à jour le transitaire
        if (voyageDTO.getTransitaireId() != null && voyageDTO.getTransitaireId() > 0) {
            Transitaire transitaire = transitaireRepository.findById(voyageDTO.getTransitaireId())
                    .orElseThrow(() -> new RuntimeException(
                            "Transitaire non trouvé avec l'id: " + voyageDTO.getTransitaireId()));
            voyage.setTransitaire(transitaire);
        } else {
            voyage.setTransitaire(existingVoyage.getTransitaire());
        }

        // Mettre à jour l'axe (obligatoire)
        if (voyageDTO.getAxeId() != null && voyageDTO.getAxeId() > 0) {
            Axe axe = axeRepository.findById(voyageDTO.getAxeId())
                    .orElseThrow(() -> new RuntimeException("Axe non trouvé avec l'id: " + voyageDTO.getAxeId()));
            voyage.setAxe(axe);
        } else if (existingVoyage.getAxe() == null) {
            throw new RuntimeException("Un axe doit être sélectionné");
        } else {
            voyage.setAxe(existingVoyage.getAxe());
        }

        // Mettre à jour le produit
        if (voyageDTO.getProduitId() != null) {
            Produit produit = produitRepository.findById(voyageDTO.getProduitId())
                    .orElseThrow(
                            () -> new RuntimeException("Produit non trouvé avec l'id: " + voyageDTO.getProduitId()));
            voyage.setProduit(produit);
        } else {
            voyage.setProduit(existingVoyage.getProduit());
        }

        // Mettre à jour le dépôt
        if (voyageDTO.getDepotId() != null && voyageDTO.getDepotId() > 0) {
            Depot depot = depotRepository.findById(voyageDTO.getDepotId())
                    .orElseThrow(() -> new RuntimeException("Dépôt non trouvé avec l'id: " + voyageDTO.getDepotId()));
            voyage.setDepot(depot);
        } else {
            voyage.setDepot(existingVoyage.getDepot());
        }

        Voyage updatedVoyage = voyageRepository.save(voyage);

        if (clientAttribueDansCetteRequete && voyageDTO.getClientId() != null) {
            ClientVoyage cv = clientVoyageRepository
                    .findByVoyageIdAndClientId(updatedVoyage.getId(), voyageDTO.getClientId())
                    .orElse(null);
            if (cv != null) {
                alerteService.creerAlerte(
                        Alerte.TypeAlerte.CLIENT_ATTRIBUE,
                        "Client attribué au voyage : " + cv.getClient().getNom() + " - Voyage " + updatedVoyage.getNumeroVoyage(),
                        Alerte.PrioriteAlerte.MOYENNE,
                        "ClientVoyage", cv.getId(), "/voyages/" + updatedVoyage.getId());
            }
        }

        // Créer les états par défaut si le voyage n'en a pas encore
        List<EtatVoyage> etatsExistants = etatVoyageRepository.findByVoyageId(updatedVoyage.getId());
        if (etatsExistants.isEmpty()) {
            createDefaultEtats(updatedVoyage);
        }

        return voyageMapper.toDTO(updatedVoyage);
    }


    /**
     * Détermine automatiquement si un voyage est complètement déchargé (DECHARGER)
     * ou partiellement déchargé (PARTIELLEMENT_DECHARGER) en fonction de l'état réel
     * des livraisons aux clients.
     * 
     * @param voyage Le voyage à vérifier
     * @return Le statut approprié : DECHARGER si complètement déchargé, PARTIELLEMENT_DECHARGER sinon, ou null si pas encore déchargeable
     */
    private Voyage.StatutVoyage determinerStatutDechargement(Voyage voyage) {
        List<ClientVoyage> clientVoyages = clientVoyageRepository.findByVoyageId(voyage.getId());
        
        // Si aucun client n'est attribué, on ne peut pas déterminer le statut
        if (clientVoyages.isEmpty()) {
            return null;
        }
        
        // Vérifier que tous les ClientVoyage sont LIVRER
        boolean tousClientsLivres = clientVoyages.stream()
                .allMatch(cv -> cv.getStatut() == ClientVoyage.StatutLivraison.LIVRER);
        
        if (!tousClientsLivres) {
            // Si tous les clients ne sont pas encore livrés, le voyage n'est pas encore déchargeable
            return null;
        }
        
        // Calculer la somme des quantités réellement livrées (quantité - manquant pour chaque client)
        Double quantiteTotaleLivree = clientVoyages.stream()
                .filter(cv -> cv.getStatut() == ClientVoyage.StatutLivraison.LIVRER)
                .mapToDouble(cv -> {
                    Double quantite = cv.getQuantite() != null ? cv.getQuantite() : 0.0;
                    Double manquant = cv.getManquant() != null ? cv.getManquant() : 0.0;
                    return quantite - manquant; // Quantité réellement livrée
                })
                .sum();
        
        Double quantiteVoyage = voyage.getQuantite() != null ? voyage.getQuantite() : 0.0;
        
        // Si tous les clients sont livrés ET la quantité totale livrée = quantité du voyage
        // Alors c'est complètement déchargé (DECHARGER)
        // Sinon, c'est partiellement déchargé (PARTIELLEMENT_DECHARGER)
        boolean completementDecharge = Math.abs(quantiteTotaleLivree - quantiteVoyage) <= 0.01; // Tolérance de 0.01
        
        if (completementDecharge) {
            return Voyage.StatutVoyage.DECHARGER;
        } else {
            return Voyage.StatutVoyage.PARTIELLEMENT_DECHARGER;
        }
    }

    public VoyageDTO updateStatut(Long id, UpdateStatutRequestDTO request) {
        if (request == null || request.getStatut() == null) {
            throw new RuntimeException("Le statut est requis");
        }
        
        Voyage voyage = voyageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voyage non trouvé avec l'id: " + id));

        ensureCurrentUserIsResponsable(voyage);

        try {
            Voyage.StatutVoyage nouveauStatut = convertStatut(request.getStatut());
            
            // Pour DECHARGER, on va d'abord traiter les manquants, puis vérifier les conditions
            // pour déterminer si c'est complètement déchargé ou partiellement
            boolean isDechargerRequest = (nouveauStatut == Voyage.StatutVoyage.DECHARGER ||
                                          nouveauStatut == Voyage.StatutVoyage.PARTIELLEMENT_DECHARGER);

            voyage.setStatut(nouveauStatut);

            System.out.println("--------------------------------------------------------------");
            System.out.println("Mise à jour du statut du voyage ID: " + voyage.getId() +
                    ", Numéro: " + voyage.getNumeroVoyage() +
                    ", Statut demandé: " + request.getStatut() +
                    ", Statut interne après conversion: " + nouveauStatut);
            System.out.println("--------------------------------------------------------------");

            // Gérer les dates selon le statut
            LocalDateTime now = LocalDateTime.now();
            if (nouveauStatut == Voyage.StatutVoyage.CHARGE) {
                // Retirer du dépôt et ajouter au stock citerne lors du chargement
                retirerDuDepotEtAjouterAuStockCiterne(voyage, now);
                voyage.setDateDepart(now);
                validerEtat(voyage, "DEPART");
                voyage.setStatut(Voyage.StatutVoyage.DEPART);
            } else if (nouveauStatut == Voyage.StatutVoyage.DEPART) {
                voyage.setDateDepart(now);
            } else if (nouveauStatut == Voyage.StatutVoyage.ARRIVER) {
                voyage.setDateArrivee(now);
                voyage.setStatut(Voyage.StatutVoyage.DOUANE);
                validerEtat(voyage, "DOUANE");
            }

            // Ajouter ou mettre à jour les clients attribués au voyage (possible quel que soit le statut)
            if (request.getClients() != null && !request.getClients().isEmpty()) {
                for (ClientQuantiteDTO clientQuantite : request.getClients()) {
                    if (clientQuantite.getClientId() == null || clientQuantite.getClientId() <= 0) {
                        continue; // Ignorer les clients invalides
                    }
                    Client client = clientRepository.findById(clientQuantite.getClientId())
                            .orElseThrow(() -> new RuntimeException("Client non trouvé avec l'id: " + clientQuantite.getClientId()));
                    ClientVoyage clientVoyage = clientVoyageRepository
                            .findByVoyageIdAndClientId(voyage.getId(), clientQuantite.getClientId())
                            .orElse(null);
                    boolean clientVientDEtreAttribue = (clientVoyage == null);
                    if (clientVoyage == null) {
                        clientVoyage = new ClientVoyage();
                        clientVoyage.setVoyage(voyage);
                        clientVoyage.setClient(client);
                        clientVoyage.setStatut(ClientVoyage.StatutLivraison.NON_LIVRE);
                        clientVoyage.setDateCreation(now);
                    }
                    if (clientQuantite.getQuantite() != null && clientQuantite.getQuantite() > 0) {
                        clientVoyage.setQuantite(clientQuantite.getQuantite());
                    } else if (clientVoyage.getQuantite() == null) {
                        clientVoyage.setQuantite(voyage.getQuantite());
                    }
                    clientVoyage.setStatut(ClientVoyage.StatutLivraison.NON_LIVRE);
                    clientVoyage.setDateModification(now);
                    clientVoyageRepository.save(clientVoyage);
                    if (clientVientDEtreAttribue) {
                        alerteService.creerAlerte(
                                Alerte.TypeAlerte.CLIENT_ATTRIBUE,
                                "Client attribué au voyage : " + client.getNom() + " - Voyage " + voyage.getNumeroVoyage(),
                                Alerte.PrioriteAlerte.MOYENNE,
                                "ClientVoyage", clientVoyage.getId(), "/voyages/" + voyage.getId());
                    }
                }
            }

            if (nouveauStatut == Voyage.StatutVoyage.LIVRE) {
                // Vérifier si l'état Livré du voyage est déjà validé ou pas
                boolean etatLivrerValide = voyage.getEtats().stream()
                        .anyMatch(etatVoyage -> etatVoyage.getEtat().equals("Livré") && etatVoyage.getValider());
                if (!etatLivrerValide) {
                    voyage.setStatut(Voyage.StatutVoyage.LIVRE);
                    validerEtat(voyage, "LIVRE");
                } else {
                    voyage.setStatut(Voyage.StatutVoyage.PARTIELLEMENT_DECHARGER);
                }
            } else if (nouveauStatut == Voyage.StatutVoyage.DECHARGER || nouveauStatut == Voyage.StatutVoyage.PARTIELLEMENT_DECHARGER) {
                // Gérer plusieurs manquants par ClientVoyage
                if (request.getManquants() != null && !request.getManquants().isEmpty()) {
                    for (Map.Entry<Long, Double> entry : request.getManquants().entrySet()) {
                        Long clientVoyageId = entry.getKey();
                        Double manquant = entry.getValue();
                        
                        // CORRECTION : Permettre les manquants de 0 (livraison complète)
                        // Seuls les manquants négatifs sont invalides
                        if (clientVoyageId == null || manquant == null || manquant < 0) {
                            continue; // Ignorer les manquants invalides
                        }
                        System.out.println("Traitement du manquant pour ClientVoyage ID: " + clientVoyageId + ", Manquant: " + manquant);
                        ClientVoyage clientVoyage = clientVoyageRepository.findById(clientVoyageId)
                                .orElseThrow(() -> new RuntimeException(
                                        "ClientVoyage non trouvé avec l'id: " + clientVoyageId));
                        System.out.println("-------------------------------------fin cherche-------------------------------------");
                        
                        // Vérifier que le ClientVoyage appartient au voyage
                        if (!clientVoyage.getVoyage().getId().equals(voyage.getId())) {
                            throw new RuntimeException(
                                    "Le ClientVoyage " + clientVoyageId + " n'appartient pas au voyage " + voyage.getId());
                        }
                        
                        clientVoyage.setManquant(manquant);
                        clientVoyage.setDateModification(now);
                        clientVoyage.setStatut(ClientVoyage.StatutLivraison.LIVRER);
                        clientVoyageRepository.save(clientVoyage);
                        alerteService.creerAlerte(
                                Alerte.TypeAlerte.CLIENT_LIVRE,
                                "Client livré : " + clientVoyage.getClient().getNom() + " - Voyage " + voyage.getNumeroVoyage(),
                                Alerte.PrioriteAlerte.MOYENNE,
                                "ClientVoyage", clientVoyage.getId(), "/voyages/" + voyage.getId());

                        // Vérifier si un enregistrement Manquant existe déjà pour ce voyage ET ce client
                        Optional<Manquant> manquantExistantOpt = manquantRepository.findByVoyageIdAndClientId(voyage.getId(), clientVoyage.getClient().getId());
                        Manquant manquantEntity;
                        boolean isUpdate = false;
                        
                        if (manquantExistantOpt.isPresent()) {
                            // Mettre à jour le manquant existant
                            manquantEntity = manquantExistantOpt.get();
                            manquantEntity.setQuantite(BigDecimal.valueOf(manquant));
                            manquantEntity.setDateModification(now);
                            manquantEntity.setDescription("Manquant lors du déchargement du voyage " + voyage.getNumeroVoyage() 
                                    + " pour le client " + clientVoyage.getClient().getNom() + " (mis à jour)");
                            isUpdate = true;
                        } else {
                            // Créer un nouvel enregistrement Manquant
                            manquantEntity = new Manquant();
                            manquantEntity.setVoyage(voyage);
                            manquantEntity.setClient(clientVoyage.getClient());
                            manquantEntity.setQuantite(BigDecimal.valueOf(manquant));
                            manquantEntity.setDateCreation(now);
                            manquantEntity.setDescription("Manquant lors du déchargement du voyage " + voyage.getNumeroVoyage() 
                                    + " pour le client " + clientVoyage.getClient().getNom());
                        }

                        // Calculer le montant du manquant = quantite * prixAchat du client
                        BigDecimal prixAchatClient = clientVoyage.getPrixAchat() != null
                                ? clientVoyage.getPrixAchat()
                                : BigDecimal.ZERO;
                        BigDecimal montantManquant = BigDecimal
                                .valueOf(manquant)
                                .multiply(prixAchatClient);
                        manquantEntity.setMontantManquant(montantManquant);

                        // Récupérer l'utilisateur actuel
                        Authentication authentication = SecurityContextHolder
                                .getContext().getAuthentication();
                        if (authentication != null && authentication.isAuthenticated()) {
                            manquantEntity.setCreePar(authentication.getName());
                        } else {
                            manquantEntity.setCreePar("Système");
                        }

                        manquantRepository.save(manquantEntity);
                        mettreAJourCoutTransportApresManquant(voyage);

                        // Retirer du stock citerne UNIQUEMENT lors du premier déchargement (pas lors des mises à jour)
                        // ET seulement si le stock n'a pas déjà été retiré pour ce ClientVoyage
                        if (!isUpdate) {
                            retirerDuStockCiterne(voyage, clientVoyage.getClient().getId(), now);
                        }
                    }
                }
                
                // APRÈS avoir traité les manquants, vérifier si le voyage est complètement déchargé
                // Cette vérification doit se faire APRÈS la mise à jour des ClientVoyage
                if (isDechargerRequest) {
                    List<ClientVoyage> clientVoyages = clientVoyageRepository.findByVoyageId(voyage.getId());
                    
                    if (clientVoyages.isEmpty()) {
                        throw new RuntimeException("Impossible de décharger un voyage sans clients attribués");
                    }
                    
                    // Vérifier que tous les ClientVoyage sont LIVRER
                    boolean tousClientsLivres = clientVoyages.stream()
                            .allMatch(cv -> cv.getStatut() == ClientVoyage.StatutLivraison.LIVRER);
                    
                    // Calculer la somme des quantités livrées (quantité - manquant pour chaque client)
                    Double quantiteTotaleLivree = clientVoyages.stream()
                            .filter(cv -> cv.getStatut() == ClientVoyage.StatutLivraison.LIVRER)
                            .mapToDouble(cv -> {
                                return cv.getQuantite() != null ? cv.getQuantite() : 0.0;
                            })
                            .sum();
                    
                     Double quantiteVoyage = voyage.getQuantite() != null ? voyage.getQuantite() : 0.0;
                    
                    // Si tous les clients sont livrés ET la quantité totale livrée = quantité du voyage
                    // Alors c'est complètement déchargé (DECHARGER)
                    // Sinon, c'est partiellement déchargé (PARTIELLEMENT_DECHARGER)
                    boolean completementDecharge = tousClientsLivres && 
                            Math.abs(quantiteTotaleLivree - quantiteVoyage) <= 0.01; // Tolérance de 0.01
                    System.out.println("-----------------------------------------------------------------");
                    System.out.println("Vérification du déchargement: Tous clients livrés = " + tousClientsLivres +
                            ", Quantité totale livrée = " + quantiteTotaleLivree +
                            ", Quantité du voyage = " + quantiteVoyage +
                            ", Complètement déchargé = " + completementDecharge);
                    System.out.println("-----------------------------------------------------------------");
                    if (completementDecharge) {
                        // Le voyage est complètement déchargé
                        voyage.setStatut(Voyage.StatutVoyage.DECHARGER);
                        validerEtat(voyage, "DECHARGER");
                    } else {
                        // Si les conditions ne sont pas remplies, forcer PARTIELLEMENT_DECHARGER
                        voyage.setStatut(Voyage.StatutVoyage.PARTIELLEMENT_DECHARGER);
                    }

                    System.out.println("---------------------------------------------------%%%%%%%%");
                    System.out.println("Statut final du voyage après vérification: " + voyage.getStatut());
                    System.out.println("---------------------------------------------------%%%%%%%%");
                }
            }

            // Mettre à jour le statut du camion en fonction du nouveau statut du voyage
            if (voyage.getCamion() != null) {
                updateCamionStatusFromVoyage(voyage.getCamion(), nouveauStatut, voyage);
            }

            Voyage updatedVoyage = voyageRepository.save(voyage);
            return voyageMapper.toDTO(updatedVoyage);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut invalide: " + request.getStatut());
        }
    }

    @Override
    public VoyageDTO updateStatut(Long id, String statut, Long clientId, Double manquant,
            BigDecimal prixAchat) {
        Voyage voyage = voyageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voyage non trouvé avec l'id: " + id));

        ensureCurrentUserIsResponsable(voyage);

        try {
            Voyage.StatutVoyage nouveauStatut = convertStatut(statut);
            
            // Pour DECHARGER, déterminer automatiquement si c'est complètement déchargé ou partiellement
            // Cette vérification sera refaite après le traitement des manquants si nécessaire
            boolean isDechargerRequest = (nouveauStatut == Voyage.StatutVoyage.DECHARGER);
            
            voyage.setStatut(nouveauStatut);

            // Gérer les dates selon le statut
            LocalDateTime now = LocalDateTime.now();
            if (nouveauStatut == Voyage.StatutVoyage.CHARGE) {
                // Retirer du dépôt et ajouter au stock citerne lors du chargement
                retirerDuDepotEtAjouterAuStockCiterne(voyage, now);
                voyage.setDateDepart(now);
                validerEtat(voyage, "DEPART");
                voyage.setStatut(Voyage.StatutVoyage.DEPART);
            } else if (nouveauStatut == Voyage.StatutVoyage.DEPART) {
                voyage.setDateDepart(now);
            } else if (nouveauStatut == Voyage.StatutVoyage.ARRIVER) {
                voyage.setDateArrivee(now);
                voyage.setStatut(Voyage.StatutVoyage.DOUANE);
                validerEtat(voyage, "DOUANE");
            } else if (nouveauStatut == Voyage.StatutVoyage.LIVRE) {
                // Assigner le client si fourni - créer un ClientVoyage et le marquer comme "Livrer"
                if (clientId != null && clientId > 0) {
                    Client client = clientRepository.findById(clientId)
                            .orElseThrow(() -> new RuntimeException("Client non trouvé avec l'id: " + clientId));
                    
                    // Vérifier si un ClientVoyage existe déjà pour ce voyage et ce client
                    ClientVoyage clientVoyage = clientVoyageRepository
                            .findByVoyageIdAndClientId(voyage.getId(), clientId)
                            .orElse(null);
                    boolean clientVientDEtreAttribue = (clientVoyage == null);
                    if (clientVoyage == null) {
                        // Créer un nouveau ClientVoyage
                        clientVoyage = new ClientVoyage();
                        clientVoyage.setVoyage(voyage);
                        clientVoyage.setClient(client);
                        clientVoyage.setQuantite(voyage.getQuantite()); // Par défaut, toute la quantité
                        clientVoyage.setDateCreation(now);
                    }
                    
                    // CORRECTION : Marquer comme "Livrer" et initialiser le manquant à 0 si non défini
                    if (clientVoyage.getManquant() == null) {
                        clientVoyage.setManquant(0.0); // Pas de manquant par défaut
                    }
                    clientVoyage.setStatut(ClientVoyage.StatutLivraison.LIVRER);
                    clientVoyage.setDateModification(now);
                    clientVoyageRepository.save(clientVoyage);
                    if (clientVientDEtreAttribue) {
                        alerteService.creerAlerte(
                                Alerte.TypeAlerte.CLIENT_ATTRIBUE,
                                "Client attribué au voyage : " + client.getNom() + " - Voyage " + voyage.getNumeroVoyage(),
                                Alerte.PrioriteAlerte.MOYENNE,
                                "ClientVoyage", clientVoyage.getId(), "/voyages/" + voyage.getId());
                    }
                    alerteService.creerAlerte(
                            Alerte.TypeAlerte.CLIENT_LIVRE,
                            "Client livré : " + client.getNom() + " - Voyage " + voyage.getNumeroVoyage(),
                            Alerte.PrioriteAlerte.MOYENNE,
                            "ClientVoyage", clientVoyage.getId(), "/voyages/" + voyage.getId());

                    // APRÈS chaque livraison, vérifier automatiquement si le voyage peut être déchargé
                    Voyage.StatutVoyage statutAuto = determinerStatutDechargement(voyage);
                    if (statutAuto != null) {
                        nouveauStatut = statutAuto;
                        voyage.setStatut(nouveauStatut);
                    }
                }
            } else if (nouveauStatut == Voyage.StatutVoyage.DECHARGER || nouveauStatut == Voyage.StatutVoyage.PARTIELLEMENT_DECHARGER) {
                // Les validations ont déjà été faites avant de définir le statut
                // Enregistrer le manquant au niveau du ClientVoyage si fourni
                // CORRECTION : Permettre les manquants de 0 (livraison complète)
                if (clientId != null && clientId > 0 && manquant != null && manquant >= 0) {
                    ClientVoyage clientVoyage = clientVoyageRepository
                            .findByVoyageIdAndClientId(voyage.getId(), clientId)
                            .orElseThrow(() -> new RuntimeException(
                                    "ClientVoyage non trouvé pour le voyage " + voyage.getId() + " et le client " + clientId));
                    
                    clientVoyage.setManquant(manquant);
                    // S'assurer que le client est marqué comme LIVRER
                    clientVoyage.setStatut(ClientVoyage.StatutLivraison.LIVRER);
                    clientVoyage.setDateModification(now);
                    clientVoyageRepository.save(clientVoyage);
                    alerteService.creerAlerte(
                            Alerte.TypeAlerte.CLIENT_LIVRE,
                            "Client livré : " + clientVoyage.getClient().getNom() + " - Voyage " + voyage.getNumeroVoyage(),
                            Alerte.PrioriteAlerte.MOYENNE,
                            "ClientVoyage", clientVoyage.getId(), "/voyages/" + voyage.getId());

                    // Vérifier si un enregistrement Manquant existe déjà pour ce voyage ET ce client
                    Optional<Manquant> manquantExistantOpt = manquantRepository.findByVoyageIdAndClientId(voyage.getId(), clientId);
                    Manquant manquantEntity;
                    boolean isUpdate = false;
                    
                    if (manquantExistantOpt.isPresent()) {
                        // Mettre à jour le manquant existant
                        manquantEntity = manquantExistantOpt.get();
                        manquantEntity.setQuantite(BigDecimal.valueOf(manquant));
                        manquantEntity.setDateModification(now);
                        manquantEntity.setDescription("Manquant lors du déchargement du voyage " + voyage.getNumeroVoyage() 
                                + " pour le client " + clientVoyage.getClient().getNom() + " (mis à jour)");
                        isUpdate = true;
                    } else {
                        // Créer un nouvel enregistrement Manquant
                        manquantEntity = new Manquant();
                        manquantEntity.setVoyage(voyage);
                        manquantEntity.setClient(clientVoyage.getClient());
                        manquantEntity.setQuantite(BigDecimal.valueOf(manquant));
                        manquantEntity.setDateCreation(now);
                        manquantEntity.setDescription("Manquant lors du déchargement du voyage " + voyage.getNumeroVoyage() 
                                + " pour le client " + clientVoyage.getClient().getNom());

                        // Récupérer l'utilisateur actuel
                        Authentication authentication = SecurityContextHolder
                                .getContext().getAuthentication();
                        if (authentication != null && authentication.isAuthenticated()) {
                            manquantEntity.setCreePar(authentication.getName());
                        } else {
                            manquantEntity.setCreePar("Système");
                        }
                    }

                    // Calculer le montant du manquant = quantite * prixAchat du client
                    BigDecimal prixAchatClient = clientVoyage.getPrixAchat() != null
                            ? clientVoyage.getPrixAchat()
                            : BigDecimal.ZERO;
                    BigDecimal montantManquant = BigDecimal
                            .valueOf(manquant)
                            .multiply(prixAchatClient);
                    manquantEntity.setMontantManquant(montantManquant);

                    manquantRepository.save(manquantEntity);
                    mettreAJourCoutTransportApresManquant(voyage);

                    // Retirer du stock citerne UNIQUEMENT lors du premier déchargement (pas lors des mises à jour)
                    if (!isUpdate) {
                        retirerDuStockCiterne(voyage, clientId, now);
                    }
                    
                    // APRÈS avoir traité le manquant, vérifier automatiquement si le voyage est complètement déchargé
                    Voyage.StatutVoyage statutAuto = determinerStatutDechargement(voyage);
                    if (statutAuto != null) {
                        nouveauStatut = statutAuto;
                        voyage.setStatut(nouveauStatut);
                    }
                } else {
                    // Si pas de clientId fourni, retirer du stock pour le voyage entier (compatibilité ancien système)
                    retirerDuStockCiterne(voyage, clientId, now);
                    
                    // Vérifier quand même l'état actuel
                    Voyage.StatutVoyage statutAuto = determinerStatutDechargement(voyage);
                    if (statutAuto != null) {
                        nouveauStatut = statutAuto;
                        voyage.setStatut(nouveauStatut);
                    }
                }
            }

            // Valider l'état correspondant au nouveau statut
            // Pour DECHARGER, valider l'état "Décharger" seulement si complètement déchargé
            // Pour PARTIELLEMENT_DECHARGER, ne pas valider l'état "Décharger"
            if (nouveauStatut == Voyage.StatutVoyage.DECHARGER) {
                validerEtat(voyage, "DECHARGER");
            } else if (nouveauStatut != Voyage.StatutVoyage.PARTIELLEMENT_DECHARGER) {
                // Pour les autres statuts, utiliser le statut de la requête
                validerEtat(voyage, statut);
            }

            // Mettre à jour le statut du camion en fonction du nouveau statut du voyage
            if (voyage.getCamion() != null) {
                updateCamionStatusFromVoyage(voyage.getCamion(), nouveauStatut, voyage);
            }

            Voyage updatedVoyage = voyageRepository.save(voyage);
            if (nouveauStatut == Voyage.StatutVoyage.LIVRE) {
                alerteService.creerAlerte(
                        Alerte.TypeAlerte.VOYAGE_LIVRE,
                        "Voyage livré : " + updatedVoyage.getNumeroVoyage(),
                        Alerte.PrioriteAlerte.MOYENNE,
                        "Voyage", updatedVoyage.getId(), "/voyages/" + updatedVoyage.getId());
            }
            return voyageMapper.toDTO(updatedVoyage);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut invalide: " + statut);
        }
    }

    @Override
    public VoyageDTO donnerPrixAchat(Long voyageId, Long clientVoyageId, BigDecimal prixAchat) {
        Voyage voyage = voyageRepository.findById(voyageId)
                .orElseThrow(() -> new RuntimeException("Voyage non trouvé avec l'id: " + voyageId));

        // Récupérer le ClientVoyage
        ClientVoyage clientVoyage = clientVoyageRepository.findById(clientVoyageId)
                .orElseThrow(() -> new RuntimeException("ClientVoyage non trouvé avec l'id: " + clientVoyageId));

        // Vérifier que le ClientVoyage appartient au voyage
        if (!clientVoyage.getVoyage().getId().equals(voyageId)) {
            throw new RuntimeException("Le ClientVoyage n'appartient pas au voyage spécifié");
        }

        // Vérifier que le prix d'achat est valide
        if (prixAchat == null || prixAchat.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Le prix d'achat doit être supérieur à zéro");
        }

        // Calculer la quantité livrée (quantité du ClientVoyage - manquant)
        Double quantiteLivree = clientVoyage.getQuantite();
        if (clientVoyage.getManquant() != null && clientVoyage.getManquant() > 0) {
            quantiteLivree = clientVoyage.getQuantite() - clientVoyage.getManquant();
        }

        boolean isModification = clientVoyage.getPrixAchat() != null;

        if (isModification) {
            // Modifier le prix d'achat existant : mettre à jour ClientVoyage et la facture existante
            Long clientId = clientVoyage.getClient().getId();
            java.util.List<Facture> factures = factureRepository.findByVoyageIdAndClientId(voyageId, clientId);
            if (factures.isEmpty()) {
                throw new RuntimeException("Aucune facture trouvée pour ce ClientVoyage (impossible de modifier le prix)");
            }
            Facture facture = factures.get(0);

            // Mettre à jour le prix d'achat du ClientVoyage
            clientVoyage.setPrixAchat(prixAchat);
            clientVoyage.setDateModification(LocalDateTime.now());
            clientVoyageRepository.save(clientVoyage);

            // Mettre à jour les lignes de la facture (prix unitaire et total)
            BigDecimal nouveauTotal = prixAchat.multiply(BigDecimal.valueOf(quantiteLivree));
            if (facture.getLignes() != null && !facture.getLignes().isEmpty()) {
                for (LigneFacture ligne : facture.getLignes()) {
                    ligne.setPrixUnitaire(prixAchat);
                    ligne.setTotal(nouveauTotal);
                }
            }
            // Recalculer les montants de la facture
            facture.setMontantHT(nouveauTotal);
            facture.setMontantTTC(nouveauTotal);
            facture.setMontant(nouveauTotal);
            factureRepository.save(facture);
        } else {
            // Premier enregistrement du prix d'achat : créer la facture
            clientVoyage.setPrixAchat(prixAchat);
            clientVoyage.setDateModification(LocalDateTime.now());
            clientVoyageRepository.save(clientVoyage);

            FactureDTO factureDTO = new FactureDTO();
            factureDTO.setClientId(clientVoyage.getClient().getId());
            factureDTO.setDate(LocalDate.now());
            factureDTO.setStatut("EMISE");
            factureDTO.setDescription("Facture pour le voyage " + voyage.getNumeroVoyage() + " - Client: " + clientVoyage.getClient().getNom());
            factureDTO.setTauxTVA(BigDecimal.ZERO);

            LigneFactureDTO ligneDTO = new LigneFactureDTO();
            ligneDTO.setProduitId(voyage.getProduit().getId());
            ligneDTO.setQuantite(quantiteLivree);
            ligneDTO.setPrixUnitaire(prixAchat);
            ligneDTO.setTotal(prixAchat.multiply(BigDecimal.valueOf(quantiteLivree)));
            factureDTO.setLignes(List.of(ligneDTO));

            FactureDTO savedFactureDTO = factureService.save(factureDTO);

            // Lier la facture au voyage sans toucher à voyage.getFactures() (évite le chargement lazy)
            Facture facture = factureRepository.findById(savedFactureDTO.getId())
                    .orElseThrow(() -> new RuntimeException("Facture non trouvée après création"));
            facture.setVoyage(voyage);
            factureRepository.save(facture);
        }

        // Recharger le voyage pour le DTO (évite d'avoir modifié les collections lazy en mémoire)
        Voyage updatedVoyage = voyageRepository.findById(voyageId)
                .orElseThrow(() -> new RuntimeException("Voyage non trouvé avec l'id: " + voyageId));
        return voyageMapper.toDTO(updatedVoyage);
    }

    @Override
    public VoyageDTO updateClientVoyageQuantite(Long voyageId, Long clientVoyageId, Long newClientId, Double quantite) {
        if (quantite == null || quantite <= 0) {
            throw new RuntimeException("La quantité doit être supérieure à zéro");
        }
        if (newClientId == null || newClientId <= 0) {
            throw new RuntimeException("Le client est invalide");
        }

        Voyage voyage = voyageRepository.findById(voyageId)
                .orElseThrow(() -> new RuntimeException("Voyage non trouvé avec l'id: " + voyageId));

        ClientVoyage clientVoyage = clientVoyageRepository.findById(clientVoyageId)
                .orElseThrow(() -> new RuntimeException("ClientVoyage non trouvé avec l'id: " + clientVoyageId));

        // Vérifier que le ClientVoyage appartient bien à ce voyage
        if (!clientVoyage.getVoyage().getId().equals(voyageId)) {
            throw new RuntimeException("Le ClientVoyage " + clientVoyageId + " n'appartient pas au voyage " + voyageId);
        }

        // Charger le nouveau client
        Client nouveauClient = clientRepository.findById(newClientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé avec l'id: " + newClientId));

        // Vérifier que la somme des quantités des autres clients + la nouvelle quantité
        // ne dépasse pas la quantité du voyage
        List<ClientVoyage> clientVoyages = clientVoyageRepository.findByVoyageId(voyageId);
        double totalAutres = clientVoyages.stream()
                .filter(cv -> !cv.getId().equals(clientVoyageId))
                .mapToDouble(cv -> cv.getQuantite() != null ? cv.getQuantite() : 0.0)
                .sum();

        double quantiteVoyage = voyage.getQuantite() != null ? voyage.getQuantite() : 0.0;
        if (totalAutres + quantite > quantiteVoyage) {
            throw new RuntimeException("La somme des quantités des clients (" + (totalAutres + quantite)
                    + " L) dépasse la quantité du voyage (" + quantiteVoyage + " L)");
        }

        // Mettre à jour le client et la quantité
        clientVoyage.setClient(nouveauClient);
        clientVoyage.setQuantite(quantite);
        clientVoyage.setDateModification(LocalDateTime.now());
        clientVoyageRepository.save(clientVoyage);

        // Recharger le voyage à jour et le renvoyer
        Voyage updatedVoyage = voyageRepository.findById(voyageId)
                .orElseThrow(() -> new RuntimeException("Voyage non trouvé après mise à jour avec l'id: " + voyageId));
        return voyageMapper.toDTO(updatedVoyage);
    }

    @Override
    public VoyageDTO assignTransitaire(Long voyageId, Long transitaireId) {
        Voyage voyage = voyageRepository.findById(voyageId)
                .orElseThrow(() -> new RuntimeException("Voyage non trouvé avec l'id: " + voyageId));

        if (transitaireId == null || transitaireId <= 0) {
            // Retirer le transitaire si transitaireId est null ou 0
            voyage.setTransitaire(null);
        } else {
            Transitaire transitaire = transitaireRepository.findById(transitaireId)
                    .orElseThrow(() -> new RuntimeException("Transitaire non trouvé avec l'id: " + transitaireId));
            voyage.setTransitaire(transitaire);
        }

        Voyage updatedVoyage = voyageRepository.save(voyage);
        return voyageMapper.toDTO(updatedVoyage);
    }

    @Override
    @Transactional(readOnly = true)
    public VoyageMargeDTO calculateMarge(Long voyageId) {
        // Récupérer le voyage
        Voyage voyage = voyageRepository.findById(voyageId)
                .orElseThrow(() -> new RuntimeException("Voyage non trouvé avec l'id: " + voyageId));

        // Créer le DTO de marge
        VoyageMargeDTO margeDTO = new VoyageMargeDTO();
        margeDTO.setVoyageId(voyage.getId());
        margeDTO.setNumeroVoyage(voyage.getNumeroVoyage());
        margeDTO.setQuantite(voyage.getQuantite());

        // Voyage de type cession : pas de cout ni marge comptés
        if (voyage.isCession()) {
            margeDTO.setPrixUnitaireAchat(BigDecimal.ZERO);
            margeDTO.setCoutVoyage(BigDecimal.ZERO);
            margeDTO.setFraisTotaux(BigDecimal.ZERO);
            margeDTO.setCoutReelTotal(BigDecimal.ZERO);
            margeDTO.setCoutReelParLitre(BigDecimal.ZERO);
            margeDTO.setHasFacture(false);
            margeDTO.setPrixVenteUnitaire(BigDecimal.ZERO);
            margeDTO.setMontantVenteTotal(BigDecimal.ZERO);
            margeDTO.setMargeBrute(BigDecimal.ZERO);
            margeDTO.setMargeNet(BigDecimal.ZERO);
            margeDTO.setMargePourcentage(BigDecimal.ZERO);
            return margeDTO;
        }

        // Coûts
        margeDTO.setPrixUnitaireAchat(
                voyage.getPrixUnitaire() != null ? voyage.getPrixUnitaire() : BigDecimal.ZERO);
        margeDTO.setCoutVoyage(voyage.getPrixUnitaire() != null
                ? voyage.getPrixUnitaire().multiply(BigDecimal.valueOf(voyage.getQuantite() != null ? voyage.getQuantite() : 0))
                : BigDecimal.ZERO);

        // Calculer les frais totaux (somme de toutes les transactions du voyage)
        BigDecimal fraisTotaux = BigDecimal.ZERO;
        if (voyage.getTransactions() != null) {
            for (Transaction transaction : voyage.getTransactions()) {
                if (transaction.getMontant() != null) {
                    fraisTotaux = fraisTotaux.add(transaction.getMontant());
                }
            }
        }
        margeDTO.setFraisTotaux(fraisTotaux);

        // Calculer le coût réel total
        BigDecimal coutReelTotal = margeDTO.getCoutVoyage().add(fraisTotaux);
        margeDTO.setCoutReelTotal(coutReelTotal);

        // Calculer le coût réel par litre
        BigDecimal coutReelParLitre = BigDecimal.ZERO;
        if (voyage.getQuantite() != null && voyage.getQuantite() > 0) {
            coutReelParLitre = coutReelTotal.divide(BigDecimal.valueOf(voyage.getQuantite()), 2,
                    RoundingMode.HALF_UP);
        }
        margeDTO.setCoutReelParLitre(coutReelParLitre);

        // Récupérer la facture pour obtenir le prix de vente
        // Utiliser la première facture si disponible
        Facture facture = (voyage.getFactures() != null && !voyage.getFactures().isEmpty()) 
                ? voyage.getFactures().get(0) : null;
        BigDecimal prixVenteUnitaire = BigDecimal.ZERO;
        BigDecimal montantVenteTotal = BigDecimal.ZERO;
        boolean hasFacture = false;

        if (facture != null && facture.getLignes() != null) {
            hasFacture = true;
            // Trouver la ligne de facture correspondant au produit du voyage
            for (LigneFacture ligne : facture.getLignes()) {
                if (ligne.getProduit() != null && voyage.getProduit() != null &&
                        ligne.getProduit().getId().equals(voyage.getProduit().getId())) {
                    prixVenteUnitaire = ligne.getPrixUnitaire() != null ? ligne.getPrixUnitaire()
                            : BigDecimal.ZERO;
                    if (ligne.getQuantite() != null && prixVenteUnitaire.compareTo(BigDecimal.ZERO) > 0) {
                        montantVenteTotal = prixVenteUnitaire
                                .multiply(BigDecimal.valueOf(ligne.getQuantite()));
                    }
                    break;
                }
            }
        }

        margeDTO.setHasFacture(hasFacture);
        margeDTO.setPrixVenteUnitaire(prixVenteUnitaire);
        margeDTO.setMontantVenteTotal(montantVenteTotal);

        // Calculer la marge
        BigDecimal margeBrute = BigDecimal.ZERO;
        BigDecimal margeNet = BigDecimal.ZERO;
        BigDecimal margePourcentage = BigDecimal.ZERO;

        if (hasFacture && voyage.getQuantite() != null && voyage.getQuantite() > 0) {
            // Marge brute = (prix de vente - prix d'achat) * quantité
            if (prixVenteUnitaire.compareTo(BigDecimal.ZERO) > 0 &&
                    margeDTO.getPrixUnitaireAchat().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal differencePrix = prixVenteUnitaire.subtract(margeDTO.getPrixUnitaireAchat());
                margeBrute = differencePrix.multiply(BigDecimal.valueOf(voyage.getQuantite()));
            }

            // Marge nette = montant de vente - coût réel total
            margeNet = montantVenteTotal.subtract(coutReelTotal);

            // Pourcentage de marge = (marge nette / montant de vente) * 100
            if (montantVenteTotal.compareTo(BigDecimal.ZERO) > 0) {
                margePourcentage = margeNet.divide(montantVenteTotal, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
        }

        margeDTO.setMargeBrute(margeBrute);
        margeDTO.setMargeNet(margeNet);
        margeDTO.setMargePourcentage(margePourcentage);

        return margeDTO;
    }

    @Override
    public VoyageDTO assignerNumeroBonEnlevement(Long voyageId, String numeroBonEnlevement) {
        Voyage voyage = voyageRepository.findById(voyageId)
                .orElseThrow(() -> new RuntimeException("Voyage non trouvé avec l'id: " + voyageId));

        // Vérifier si le numéro existe déjà
        if (numeroBonEnlevement != null && !numeroBonEnlevement.trim().isEmpty()) {
            if (voyageRepository.existsByNumeroBonEnlevement(numeroBonEnlevement)) {
                throw new RuntimeException("Le numéro de bon d'enlèvement " + numeroBonEnlevement + " existe déjà");
            }
            voyage.setNumeroBonEnlevement(numeroBonEnlevement);
        }

        Voyage savedVoyage = voyageRepository.save(voyage);
        return voyageMapper.toDTO(savedVoyage);
    }

    @Override
    public VoyageDTO genererNumeroBonEnlevement(Long voyageId) {
        Voyage voyage = voyageRepository.findById(voyageId)
                .orElseThrow(() -> new RuntimeException("Voyage non trouvé avec l'id: " + voyageId));

        // Si le voyage a déjà un numéro, ne rien faire
        if (voyage.getNumeroBonEnlevement() != null && !voyage.getNumeroBonEnlevement().trim().isEmpty()) {
            return voyageMapper.toDTO(voyage);
        }

        // Trouver le dernier numéro assigné
        String dernierNumero = trouverDernierNumeroBonEnlevement();

        // Générer le prochain numéro
        String nouveauNumero = genererProchainNumero(dernierNumero);

        // Vérifier l'unicité avant d'assigner
        while (voyageRepository.existsByNumeroBonEnlevement(nouveauNumero)) {
            nouveauNumero = genererProchainNumero(nouveauNumero);
        }

        voyage.setNumeroBonEnlevement(nouveauNumero);
        Voyage savedVoyage = voyageRepository.save(voyage);
        return voyageMapper.toDTO(savedVoyage);
    }

    private String trouverDernierNumeroBonEnlevement() {
        Pageable pageable = PageRequest.of(0, 1);
        List<String> derniersNumeros = voyageRepository.findLastNumeroBonEnlevement(pageable);

        if (derniersNumeros.isEmpty() || derniersNumeros.get(0) == null) {
            return null;
        }

        return derniersNumeros.get(0);
    }

    private String genererProchainNumero(String dernierNumero) {
        if (dernierNumero == null || dernierNumero.trim().isEmpty()) {
            // Si aucun numéro n'existe, commencer à 1
            return String.format("%05d", 1);
        }

        try {
            // Extraire le numéro (les 5 premiers caractères si le format est
            // "00297-SFB/2025" ou juste "00297")
            String numeroStr = dernierNumero;
            if (dernierNumero.contains("-")) {
                // Format: "00297-SFB/2025"
                numeroStr = dernierNumero.substring(0, dernierNumero.indexOf("-"));
            } else if (dernierNumero.length() > 5) {
                // Prendre les 5 premiers caractères
                numeroStr = dernierNumero.substring(0, 5);
            }

            // Nettoyer le numéro (enlever les zéros non significatifs pour le parsing)
            int numero = Integer.parseInt(numeroStr);
            int prochainNumero = numero + 1;
            return String.format("%05d", prochainNumero);
        } catch (NumberFormatException e) {
            // Si le format n'est pas valide, commencer à 1
            return String.format("%05d", 1);
        }
    }

    @Override
    public CoutTransportResponseDTO getCoutsTransport(
            Long fournisseurId,
            String filterOption,
            LocalDate startDate,
            LocalDate endDate) {

        // Récupérer le fournisseur
        Fournisseur fournisseur = fournisseurRepository.findById(fournisseurId)
                .orElseThrow(() -> new RuntimeException("Fournisseur non trouvé avec l'id: " + fournisseurId));

        // Récupérer les camions du fournisseur
        List<Camion> camions = camionRepository.findByFournisseur(fournisseur);

        if (camions.isEmpty()) {
            return new CoutTransportResponseDTO(
                    new ArrayList<>(),
                    new CoutTransportStatsDTO(
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            0L));
        }

        // Récupérer tous les voyages de ces camions (hors cession : pas de cout)
        List<Voyage> allVoyages = new ArrayList<>();
        for (Camion camion : camions) {
            voyageRepository.findByCamion(camion).stream()
                    .filter(v -> !v.isCession())
                    .forEach(allVoyages::add);
        }

        // Filtrer selon l'option
        List<Voyage> filteredVoyages;

        if ("intervalle".equals(filterOption) && startDate != null && endDate != null) {
            // Filtrer par intervalle de dates
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            filteredVoyages = allVoyages.stream()
                    .filter(v -> v.getDateDepart() != null
                            && v.getDateDepart().isAfter(startDateTime.minusSeconds(1))
                            && v.getDateDepart().isBefore(endDateTime.plusSeconds(1)))
                    .collect(Collectors.toList());
        } else {
            // Filtrer selon le statut de paiement
            filteredVoyages = allVoyages.stream()
                    .filter(v -> {
                        String statutPaiement = determineStatutPaiement(v);
                        if ("nonPaye".equals(filterOption)) {
                            return "NON_PAYE".equals(statutPaiement);
                        } else if ("paye".equals(filterOption)) {
                            return "PAYE".equals(statutPaiement);
                        }
                        return true; // Tous si aucune option spécifique
                    })
                    .collect(Collectors.toList());
        }

        // Transformer en DTO
        List<CoutTransportDTO> coutsDTO = filteredVoyages.stream()
                .map(v -> {
                    // Coût brut du voyage (avant prise en compte des manquants)
                    BigDecimal prixUnitaire = v.getPrixUnitaire() != null ? v.getPrixUnitaire() : BigDecimal.ZERO;
                    double quantite = v.getQuantite() != null ? v.getQuantite() : 0.0;
                    BigDecimal coutVoyage = prixUnitaire.multiply(BigDecimal.valueOf(quantite));

                    // Somme des montants de manquants pour ce voyage
                    BigDecimal manquantMontant = BigDecimal.ZERO;
                    for (ClientVoyage cleint: v.getClientVoyages()){
                        if (cleint.getPrixAchat() != null && cleint.getManquant()!=null) manquantMontant = manquantMontant.add(BigDecimal.valueOf(cleint.getManquant() * cleint.getPrixAchat().doubleValue()));
                    }

                    System.out.println("-------------------------------------------manquant******************");
                    System.out.println("totalMontantManquant : "+ manquantMontant);
                    System.out.println("-------------------------------------------manquant******************");

                    // Coût total réel = coût brut - somme des manquants (jamais négatif)
                    BigDecimal coutTotal = coutVoyage.subtract(manquantMontant);
                    if (coutTotal.compareTo(BigDecimal.ZERO) < 0) {
                        coutTotal = BigDecimal.ZERO;
                    }

                    String statutPaiement = determineStatutPaiement(v);
                    LocalDateTime datePaiement = getDatePaiement(v);

                    return new CoutTransportDTO(
                            v.getId(),
                            v.getNumeroVoyage(),
                            v.getCamion() != null ? v.getCamion().getImmatriculation() : null,
                            v.getDateDepart(),
                            v.getDestination(),
                            v.getQuantite(),
                            coutVoyage,
                            BigDecimal.ZERO, // fraisTotaux non utilisés pour l'instant
                            coutTotal,
                            statutPaiement,
                            datePaiement);
                })
                .collect(Collectors.toList());

        // Calculer les statistiques
        BigDecimal totalCout = coutsDTO.stream()
                .map(c -> c.getCoutTotal() != null ? c.getCoutTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNonPaye = coutsDTO.stream()
                .filter(c -> "NON_PAYE".equals(c.getStatutPaiement()))
                .map(c -> c.getCoutTotal() != null ? c.getCoutTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaye = coutsDTO.stream()
                .filter(c -> "PAYE".equals(c.getStatutPaiement()))
                .map(c -> c.getCoutTotal() != null ? c.getCoutTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CoutTransportStatsDTO stats = new CoutTransportStatsDTO(
                totalCout,
                totalNonPaye,
                totalPaye,
                (long) coutsDTO.size());

        return new CoutTransportResponseDTO(coutsDTO, stats);
    }

    /**
     * Détermine le statut de paiement d'un voyage en fonction des paiements liés.
     *
     * Règle métier demandée :
     * - Si aucun paiement ou paiement non validé -> "EN_ATTENTE" (non payé)
     * - Si au moins un paiement VALIDE          -> "PAYE"
     */
    private String determineStatutPaiement(Voyage voyage) {
        if (voyage == null || voyage.getId() == null) {
            return "NON_PAYE";
        }

        // Récupérer le paiement associé au voyage (si présent)
        Paiement paiement = paiementRepository.findByVoyage(voyage);
        if (paiement == null || paiement.getStatut() == null) {
            // Aucun paiement enregistré ou statut inconnu => considéré comme non payé
            return "NON_PAYE";
        }

        // Si le paiement est validé, le voyage est considéré comme payé
        if (paiement.getStatut() == Paiement.StatutPaiement.VALIDE) {
            return "PAYE";
        }

        // Tous les autres statuts (EN_ATTENTE, REJETE, ANNULE) sont considérés comme non payés
        return "NON_PAYE";
    }

    private BigDecimal calculateFraisTotaux(Voyage voyage) {
        if (voyage.getTransactions() == null || voyage.getTransactions().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return voyage.getTransactions().stream()
                .filter(t -> t.getType() == Transaction.TypeTransaction.FRAIS_DOUANE
                        || t.getType() == Transaction.TypeTransaction.FRAIS_T1
                        || t.getType() == Transaction.TypeTransaction.FRAIS_FRONTIERE)
                .map(t -> t.getMontant() != null ? t.getMontant() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LocalDateTime getDatePaiement(Voyage voyage) {
        if (voyage.getFactures() == null || voyage.getFactures().isEmpty()) {
            return null;
        }

        // Utiliser la première facture pour la compatibilité
        Facture facture = voyage.getFactures().get(0);

        // Si la facture est payée, retourner la date de la facture ou la date de
        // dernière transaction
        if (facture.getMontantPaye() != null && facture.getMontant() != null) {
            if (facture.getMontantPaye().compareTo(facture.getMontant()) >= 0) {
                // Chercher la date de la dernière transaction payée
                if (voyage.getTransactions() != null && !voyage.getTransactions().isEmpty()) {
                    return voyage.getTransactions().stream()
                            .filter(t -> t
                                    .getStatut() == Transaction.StatutTransaction.VALIDE)
                            .map(Transaction::getDate)
                            .filter(Objects::nonNull)
                            .max(LocalDateTime::compareTo)
                            .orElse(facture.getDate() != null ? facture.getDate().atStartOfDay() : null);
                }
                return facture.getDate() != null ? facture.getDate().atStartOfDay() : null;
            }
        }

        return null;
    }

    @Override
    public VoyagePageDto findVoyagesChargesByIdentifiant(
            String identifiant,
            int page,
            int size,
            LocalDate date,
            LocalDate startDate,
            LocalDate endDate) {

        // Récupérer l'utilisateur par identifiant
        Utilisateur utilisateur = utilisateurRepository.findByIdentifiant(identifiant)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'identifiant: " + identifiant));

        if (utilisateur.getDepot() == null) {
            throw new RuntimeException("L'utilisateur n'a pas de dépôt associé");
        }

        Depot depot = utilisateur.getDepot();
        Pageable pageable = PageRequest.of(page, size);
        Page<Voyage> voyagePage;

        // Appliquer les filtres selon les paramètres
        if (date != null) {
            // Filtre par date unique
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59).plusSeconds(1); // Début du jour suivant
            voyagePage = voyageRepository.findVoyagesChargesByDepotAndDate(depot, startOfDay, endOfDay, pageable);
        } else if (startDate != null && endDate != null) {
            // Filtre par intervalle de dates
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            voyagePage = voyageRepository.findVoyagesChargesByDepotAndDateRange(depot, startDateTime, endDateTime,
                    pageable);
        } else {
            // Pas de filtre de date
            voyagePage = voyageRepository.findVoyagesChargesByDepot(depot, pageable);
        }

        // Convertir en DTO
        List<VoyageDTO> voyageDTOs = voyagePage.getContent().stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());

        return new VoyagePageDto(
                voyageDTOs,
                voyagePage.getNumber(),
                voyagePage.getTotalPages(),
                voyagePage.getTotalElements(),
                voyagePage.getSize());
    }

    @Override
    public void deleteById(Long id) {
        voyageRepository.deleteById(id);
    }

    /**
     * Génère un numéro de voyage unique au format VOY-YYYY-NNNN
     */
    private String generateUniqueNumeroVoyage() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        String prefix = "VOY-" + year + "-";

        // Trouver le dernier numéro de voyage de l'année
        List<Voyage> voyagesAnnee = voyageRepository.findAll().stream()
                .filter(v -> v.getNumeroVoyage() != null && v.getNumeroVoyage().startsWith(prefix))
                .toList();

        int nextNumber = 1;
        if (!voyagesAnnee.isEmpty()) {
            // Extraire le numéro le plus élevé
            int maxNumber = voyagesAnnee.stream()
                    .mapToInt(v -> {
                        try {
                            String numStr = v.getNumeroVoyage().substring(prefix.length());
                            return Integer.parseInt(numStr);
                        } catch (NumberFormatException e) {
                            return 0;
                        }
                    })
                    .max()
                    .orElse(0);
            nextNumber = maxNumber + 1;
        }

        String numeroVoyage = prefix + String.format("%04d", nextNumber);

        // Vérifier l'unicité (au cas où)
        while (voyageRepository.findByNumeroVoyage(numeroVoyage).isPresent()) {
            nextNumber++;
            numeroVoyage = prefix + String.format("%04d", nextNumber);
        }

        return numeroVoyage;
    }

    /**
     * Met à jour le statut du camion en fonction du statut du voyage
     */
    private void updateCamionStatusFromVoyage(Camion camion, Voyage.StatutVoyage statutVoyage) {
        updateCamionStatusFromVoyage(camion, statutVoyage, null);
    }

    private void updateCamionStatusFromVoyage(Camion camion, Voyage.StatutVoyage statutVoyage, Voyage voyage) {
        if (camion == null || statutVoyage == null) {
            return;
        }

        switch (statutVoyage) {
            case CHARGEMENT:
            case CHARGE:
            case DEPART:
            case ARRIVER:
            case DOUANE:
                // Si le voyage est en cours, le camion est en route
                // Sauf s'il est en maintenance ou hors service, on ne change pas le statut
                if (camion.getStatut() != Camion.StatutCamion.EN_MAINTENANCE &&
                        camion.getStatut() != Camion.StatutCamion.HORS_SERVICE) {
                    camion.setStatut(Camion.StatutCamion.EN_ROUTE);
                }
                break;
            case RECEPTIONNER:
            case LIVRE:
                // Si le voyage est arrivé au dépôt ou livré
                // Le camion redevient disponible seulement si tous les ClientVoyage sont "Livrer"
                boolean tousClientsLivres = true;
                if (voyage != null && voyage.getId() != null) {
                    List<ClientVoyage> clientVoyages = clientVoyageRepository.findByVoyageId(voyage.getId());
                    if (!clientVoyages.isEmpty()) {
                        // Vérifier que tous les ClientVoyage sont "Livrer"
                        tousClientsLivres = clientVoyages.stream()
                                .allMatch(cv -> cv.getStatut() == ClientVoyage.StatutLivraison.LIVRER);
                    } else {
                        // Si aucun ClientVoyage, considérer comme disponible (compatibilité ancien système)
                        tousClientsLivres = true;
                    }
                }
                
                if (tousClientsLivres &&
                        camion.getStatut() != Camion.StatutCamion.EN_MAINTENANCE &&
                        camion.getStatut() != Camion.StatutCamion.HORS_SERVICE) {
                    camion.setStatut(Camion.StatutCamion.DISPONIBLE);
                }
                break;
            case DECHARGER:
                // Pour DECHARGER, vérifier que l'état "Décharger" est validé pour ce voyage
                // ET que tous les ClientVoyage sont "Livrer"
                boolean dechargerValide = false;
                boolean tousClientsLivresDecharger = false;
                
                if (voyage != null && voyage.getId() != null) {
                    List<EtatVoyage> etats = etatVoyageRepository.findByVoyageId(voyage.getId());
                    dechargerValide = etats.stream()
                            .anyMatch(e -> "Décharger".equals(e.getEtat()) && Boolean.TRUE.equals(e.getValider()));
                    
                    // Vérifier que tous les ClientVoyage sont "Livrer"
                    List<ClientVoyage> clientVoyages = clientVoyageRepository.findByVoyageId(voyage.getId());
                    if (!clientVoyages.isEmpty()) {
                        tousClientsLivresDecharger = clientVoyages.stream()
                                .allMatch(cv -> cv.getStatut() == ClientVoyage.StatutLivraison.LIVRER);
                    }
                }

                // Le camion devient disponible seulement si l'état "Décharger" est validé
                // ET que tous les ClientVoyage sont "Livrer"
                if (dechargerValide && tousClientsLivresDecharger &&
                        camion.getStatut() != Camion.StatutCamion.EN_MAINTENANCE &&
                        camion.getStatut() != Camion.StatutCamion.HORS_SERVICE) {
                    camion.setStatut(Camion.StatutCamion.DISPONIBLE);
                }
                break;
            case PARTIELLEMENT_DECHARGER:
                // Pour PARTIELLEMENT_DECHARGER, le camion reste en route (pas disponible)
                // car le voyage n'est pas complètement déchargé
                if (camion.getStatut() != Camion.StatutCamion.EN_MAINTENANCE &&
                        camion.getStatut() != Camion.StatutCamion.HORS_SERVICE) {
                    camion.setStatut(Camion.StatutCamion.EN_ROUTE);
                }
                break;
        }

        // Sauvegarder le camion avec le nouveau statut
        camionRepository.save(camion);
    }

    /**
     * Crée les 10 états par défaut pour un voyage
     */
    private void createDefaultEtats(Voyage voyage) {
        LocalDateTime now = LocalDateTime.now();

        // Créer les 8 états par défaut
        String[] etats = {
                "Chargement",
                "Chargé",
                "Départ",
                "Arrivé",
                "Douane",
                "Réceptionné",
                "Livré",
                "Décharger"
        };

        for (String etat : etats) {
            EtatVoyage etatVoyage = new EtatVoyage();
            etatVoyage.setEtat(etat);
            etatVoyage.setDateHeure(now);
            etatVoyage.setValider(false);
            etatVoyage.setVoyage(voyage);
            if (etat.equals("Chargement")) {
                etatVoyage.setValider(true); // Le premier état est validé par défaut
            }
            etatVoyageRepository.save(etatVoyage);
        }
    }

    /**
     * Vérifie que l'utilisateur connecté peut mettre à jour le statut du voyage :
     * - Admin ou Contrôleur : autorisés pour tous les voyages (attribution, manquants).
     * - Sinon : seul le responsable (logisticien) du voyage est autorisé.
     */
    private void ensureCurrentUserIsResponsable(Voyage voyage) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof String)) {
            throw new RuntimeException("Non authentifié. Seul le logisticien responsable du voyage ou le Contrôleur peut mettre à jour le statut.");
        }
        String identifiant = (String) authentication.getPrincipal();
        Compte compte = compteRepository.findByIdentifiant(identifiant)
                .orElseThrow(() -> new RuntimeException("Compte non trouvé pour l'identifiant: " + identifiant));
        // Admin ou Contrôleur : autorisés pour tous les voyages (attribution clients, manquants)
        boolean isAdminOrControleur = compte.getRoles() != null && compte.getRoles().stream()
                .anyMatch(r -> r != null && ("Admin".equalsIgnoreCase(r.getNom()) || "Contrôleur".equals(r.getNom())));
        if (isAdminOrControleur) {
            return;
        }
        if (voyage.getResponsable() == null) {
            throw new RuntimeException("Ce voyage n'a pas de responsable assigné. Impossible de modifier le statut.");
        }
        if (!voyage.getResponsable().getId().equals(compte.getId())) {
            throw new RuntimeException("Seul le logisticien responsable du voyage peut mettre à jour le statut.");
        }
    }

    /**
     * Valide l'état correspondant au statut du voyage
     * Ne peut pas valider un état déjà validé (sauf si c'est le même état)
     */
    private void validerEtat(Voyage voyage, String statut) {
        // Mapper le statut vers le texte de l'état
        String etatTexte = getEtatTexteFromStatut(statut);

        // Trouver tous les états du voyage
        List<EtatVoyage> etats = etatVoyageRepository.findByVoyageId(voyage.getId());

        // Trouver l'état correspondant au texte
        EtatVoyage etatVoyage = etats.stream()
                .filter(e -> e.getEtat().equals(etatTexte))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("État non trouvé pour le statut: " + statut));

        // Si l'état est déjà validé, on met juste à jour la date/heure sans lever
        // d'exception
        // Cela permet de gérer les cas où plusieurs statuts mappent vers le même état
        if (etatVoyage.getValider()) {
            // Mettre à jour la date/heure même si déjà validé
            etatVoyage.setDateHeure(LocalDateTime.now());
            etatVoyageRepository.save(etatVoyage);
            return; // Ne pas re-valider un état déjà validé
        }

        // Valider l'état et mettre à jour la date/heure
        etatVoyage.setValider(true);
        etatVoyage.setDateHeure(LocalDateTime.now());
        etatVoyageRepository.save(etatVoyage);
    }

    /**
     * Convertit un statut en texte d'état
     */
    private String getEtatTexteFromStatut(String statut) {
        return switch (statut) {
            case "CHARGEMENT" -> "Chargement";
            case "CHARGE" -> "Chargé";
            case "DEPART" -> "Départ";
            case "ARRIVER" -> "Arrivé";
            case "DOUANE" -> "Douane";
            case "RECEPTIONNER" -> "Réceptionné";
            case "LIVRE" -> "Livré";
            case "DECHARGER" -> "Décharger";
            case "PARTIELLEMENT_DECHARGER" -> "Décharger"; // Même état mais non validé
            default -> throw new RuntimeException("Statut invalide: " + statut);
        };
    }

    /**
     * Crée une transaction pour le coût du voyage
     */
    private void createTransactionFromVoyage(Voyage voyage, VoyageDTO voyageDTO) {
        // Vérifier qu'un compte bancaire ou une caisse est sélectionné
        if (voyageDTO.getCompteId() == null && voyageDTO.getCaisseId() == null) {
            throw new RuntimeException(
                    "Un compte bancaire ou une caisse doit être sélectionné pour créer la transaction du coût du voyage");
        }

        if (voyageDTO.getCompteId() != null && voyageDTO.getCaisseId() != null) {
            throw new RuntimeException("Veuillez sélectionner soit un compte bancaire, soit une caisse, pas les deux");
        }

        // Créer la transaction DTO
        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setMontant(voyageDTO.getCoutVoyage());
        transactionDTO.setDate(LocalDateTime.now());
        transactionDTO.setStatut("VALIDE");
        transactionDTO.setType("FRAIS");
        transactionDTO.setDescription("Coût du voyage " + voyage.getNumeroVoyage());
        transactionDTO.setVoyageId(voyage.getId());
        transactionDTO.setCompteId(voyageDTO.getCompteId());
        transactionDTO.setCaisseId(voyageDTO.getCaisseId());

        // Créer la transaction en utilisant le service de paiement pour débiter
        // automatiquement
        transactionService.createPaiement(transactionDTO);
    }

    /**
     * Retire la quantité du voyage du dépôt correspondant
     */
    private void retirerQuantiteDuDepot(Voyage voyage) {
        // Vérifier que le voyage a un dépôt et un produit
        if (voyage.getDepot() == null || voyage.getProduit() == null) {
            return; // Pas de dépôt ou produit, rien à retirer
        }

        // Vérifier que la quantité est valide
        if (voyage.getQuantite() == null || voyage.getQuantite() <= 0) {
            return; // Quantité invalide
        }

        // Trouver le stock correspondant au produit et au dépôt
        Optional<Stock> stockOpt = stockRepository.findByDepotIdAndProduitId(
                voyage.getDepot().getId(),
                voyage.getProduit().getId());

        if (stockOpt.isEmpty()) {
            throw new RuntimeException("Stock non trouvé pour le produit " + voyage.getProduit().getNom() +
                    " dans le dépôt " + voyage.getDepot().getNom());
        }

        Stock stock = stockOpt.get();
        Double quantiteActuelle = stock.getQuantite();
        Double quantiteARetirer = voyage.getQuantite();

        // Vérifier que la quantité disponible est suffisante
        if (quantiteActuelle < quantiteARetirer) {
            throw new RuntimeException("Quantité insuffisante dans le dépôt. Quantité disponible: " + quantiteActuelle +
                    ", quantité demandée: " + quantiteARetirer);
        }

        // Retirer la quantité du stock
        Double nouvelleQuantite = quantiteActuelle - quantiteARetirer;
        stock.setQuantite(nouvelleQuantite);
        stock.setDateDerniereMiseAJour(LocalDateTime.now());
        stockRepository.save(stock);

        // Mettre à jour la capacité utilisée du dépôt
        Depot depot = voyage.getDepot();
        Double capaciteUtilisee = depot.getCapaciteUtilisee() != null ? depot.getCapaciteUtilisee() : 0.0;
        depot.setCapaciteUtilisee(Math.max(0.0, capaciteUtilisee - quantiteARetirer));

        // Mettre à jour le statut du dépôt si nécessaire
        if (depot.getCapaciteUtilisee() < depot.getCapacite()) {
            if (depot.getStatut() == Depot.StatutDepot.PLEIN) {
                depot.setStatut(Depot.StatutDepot.ACTIF);
            }
        }

        depotRepository.save(depot);

        // Créer un mouvement de sortie
        MouvementDTO mouvementDTO = new MouvementDTO();
        mouvementDTO.setStockId(stock.getId());
        mouvementDTO.setTypeMouvement("SORTIE");
        mouvementDTO.setQuantite(quantiteARetirer);
        mouvementDTO.setUnite(stock.getUnite());
        mouvementDTO.setDescription("Sortie pour voyage " + voyage.getNumeroVoyage() +
                " - " + voyage.getProduit().getNom() + " du dépôt " + depot.getNom());
        mouvementService.save(mouvementDTO);
    }

    /**
     * Retire la quantité du dépôt et l'ajoute au stock citerne lors du départ
     */
    private void retirerDuDepotEtAjouterAuStockCiterne(Voyage voyage, LocalDateTime now) {
        if (voyage.getDepot() == null || voyage.getProduit() == null) {
            return;
        }

        if (voyage.getQuantite() == null || voyage.getQuantite() <= 0) {
            return;
        }

        // Retirer du stock du dépôt
        Optional<Stock> stockDepotOpt = stockRepository.findByDepotIdAndProduitId(
                voyage.getDepot().getId(),
                voyage.getProduit().getId());

        if (stockDepotOpt.isEmpty()) {
            throw new RuntimeException("Stock non trouvé pour le produit " + voyage.getProduit().getNom() +
                    " dans le dépôt " + voyage.getDepot().getNom());
        }

        Stock stockDepot = stockDepotOpt.get();
        Double quantiteActuelle = stockDepot.getQuantite();
        Double quantiteARetirer = voyage.getQuantite();

        if (quantiteActuelle < quantiteARetirer) {
            throw new RuntimeException("Quantité insuffisante dans le dépôt. Quantité disponible: " + quantiteActuelle +
                    ", quantité demandée: " + quantiteARetirer);
        }

        // Retirer la quantité du stock du dépôt
        stockDepot.setQuantite(quantiteActuelle - quantiteARetirer);
        stockDepot.setDateDerniereMiseAJour(now);
        stockRepository.save(stockDepot);

        // Mettre à jour la capacité utilisée du dépôt
        Depot depot = voyage.getDepot();
        Double capaciteUtilisee = depot.getCapaciteUtilisee() != null ? depot.getCapaciteUtilisee() : 0.0;
        depot.setCapaciteUtilisee(Math.max(0.0, capaciteUtilisee - quantiteARetirer));

        if (depot.getCapaciteUtilisee() < depot.getCapacite()) {
            if (depot.getStatut() == Depot.StatutDepot.PLEIN) {
                depot.setStatut(Depot.StatutDepot.ACTIF);
            }
        }
        depotRepository.save(depot);

        // Ajouter au stock citerne
        Stock stockCiterne = stockRepository.findByProduitIdAndCiterne(
                voyage.getProduit().getId(),
                true)
                .orElseThrow(() -> new RuntimeException(
                        "Stock Citerne non trouvé pour le produit avec l'id: " + voyage.getProduit().getId()));

        stockCiterne.setQuantite(stockCiterne.getQuantite() + quantiteARetirer);
        stockCiterne.setDateDerniereMiseAJour(now);
        stockRepository.save(stockCiterne);

        // Créer un mouvement d'entrée pour le stock citerne
        MouvementDTO mouvementEntreeDTO = new MouvementDTO();
        mouvementEntreeDTO.setStockId(stockCiterne.getId());
        mouvementEntreeDTO.setTypeMouvement("ENTREE");
        mouvementEntreeDTO.setQuantite(quantiteARetirer);
        mouvementEntreeDTO.setUnite(stockCiterne.getUnite());
        mouvementEntreeDTO.setDescription("Entrée dans le stock citerne pour le voyage " + voyage.getNumeroVoyage());
        mouvementService.save(mouvementEntreeDTO);

        // Créer un mouvement de sortie pour le dépôt
        MouvementDTO mouvementSortieDTO = new MouvementDTO();
        mouvementSortieDTO.setStockId(stockDepot.getId());
        mouvementSortieDTO.setTypeMouvement("SORTIE");
        mouvementSortieDTO.setQuantite(quantiteARetirer);
        mouvementSortieDTO.setUnite(stockDepot.getUnite());
        mouvementSortieDTO
                .setDescription("Sortie du dépôt " + depot.getNom() + " pour le voyage " + voyage.getNumeroVoyage());
        mouvementService.save(mouvementSortieDTO);
    }

    /**
     * Met à jour le montant de la transaction et du paiement "coût de transport" du voyage
     * après chaque ajout ou modification de manquant : nouveau montant = cout brut - somme des montantManquant.
     */
    private void mettreAJourCoutTransportApresManquant(Voyage voyage) {
        if (voyage == null || voyage.getId() == null || Boolean.TRUE.equals(voyage.isCession())) {
            return;
        }
        if (voyage.getPrixUnitaire() == null || voyage.getQuantite() == null) {
            return;
        }
        BigDecimal coutBrut = voyage.getPrixUnitaire().multiply(BigDecimal.valueOf(voyage.getQuantite()));
        BigDecimal totalMontantManquant = manquantRepository.findByVoyageIdOrderByDateCreationDesc(voyage.getId()).stream()
                .map(m -> m.getMontantManquant() != null ? m.getMontantManquant() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal newMontant = coutBrut.subtract(totalMontantManquant);
        if (newMontant.compareTo(BigDecimal.ZERO) < 0) {
            newMontant = BigDecimal.ZERO;
        }
        List<Transaction> fraisList = transactionRepository.findByVoyageIdAndTypeFrais(voyage.getId());
        for (Transaction t : fraisList) {
            t.setMontant(newMontant);
            transactionRepository.save(t);
        }
        List<Paiement> paiements = paiementRepository.findCoutTransportByVoyageId(voyage.getId());
        for (Paiement p : paiements) {
            p.setMontant(newMontant);
            paiementRepository.save(p);
        }
    }

    /**
     * Retire la quantité du stock citerne lors de la livraison
     * Si clientId est fourni, utilise la quantité du ClientVoyage correspondant
     */
    private void retirerDuStockCiterne(Voyage voyage, Long clientId, LocalDateTime now) {
        if (voyage.getProduit() == null) {
            return;
        }

        Double quantiteARetirer;
        
        // Si un clientId est fourni, utiliser la quantité du ClientVoyage
        if (clientId != null && clientId > 0) {
            ClientVoyage clientVoyage = clientVoyageRepository
                    .findByVoyageIdAndClientId(voyage.getId(), clientId)
                    .orElseThrow(() -> new RuntimeException(
                            "ClientVoyage non trouvé pour le voyage " + voyage.getId() + " et le client " + clientId));
            
            if (clientVoyage.getQuantite() == null || clientVoyage.getQuantite() <= 0) {
                return;
            }
            
            quantiteARetirer = clientVoyage.getQuantite();
        } else {
            // Sinon, utiliser la quantité du voyage (compatibilité avec l'ancien système)
            if (voyage.getQuantite() == null || voyage.getQuantite() <= 0) {
                return;
            }
            quantiteARetirer = voyage.getQuantite();
        }

        // Trouver le stock citerne
        Optional<Stock> stockCiterneOpt = stockRepository.findByProduitIdAndCiterne(
                voyage.getProduit().getId(),
                true);

        if (stockCiterneOpt.isEmpty()) {
            throw new RuntimeException("Stock Citerne non trouvé pour le produit " + voyage.getProduit().getNom());
        }

        Stock stockCiterne = stockCiterneOpt.get();
        Double quantiteActuelle = stockCiterne.getQuantite();

        if (quantiteActuelle < quantiteARetirer) {
            throw new RuntimeException(
                    "Quantité insuffisante dans le stock citerne. Quantité disponible: " + quantiteActuelle +
                            ", quantité demandée: " + quantiteARetirer);
        }

        // Retirer la quantité du stock citerne
        stockCiterne.setQuantite(quantiteActuelle - quantiteARetirer);
        stockCiterne.setDateDerniereMiseAJour(now);
        stockRepository.save(stockCiterne);

        // Créer un mouvement de sortie
        String description = "Sortie du stock citerne pour la livraison du voyage " + voyage.getNumeroVoyage();
        if (clientId != null && clientId > 0) {
            ClientVoyage clientVoyage = clientVoyageRepository
                    .findByVoyageIdAndClientId(voyage.getId(), clientId)
                    .orElse(null);
            if (clientVoyage != null) {
                description += " - Client: " + clientVoyage.getClient().getNom();
            }
        }
        
        MouvementDTO mouvementDTO = new MouvementDTO();
        mouvementDTO.setStockId(stockCiterne.getId());
        mouvementDTO.setTypeMouvement("SORTIE");
        mouvementDTO.setQuantite(quantiteARetirer);
        mouvementDTO.setUnite(stockCiterne.getUnite());
        mouvementDTO.setDescription(description);
        mouvementService.save(mouvementDTO);
    }

    @Override
    public VoyageDTO declarerVoyage(Long voyageId, Long compteId, Long caisseId) {
        Voyage voyage = voyageRepository.findById(voyageId)
                .orElseThrow(() -> new RuntimeException("Voyage non trouvé avec l'id: " + voyageId));

        // Vérifier que le voyage est à la douane
        if (voyage.getDeclarer()) {
            throw new RuntimeException("Le voyage est dejà déclaré à la douane");
        }

        // Récupérer les frais douane
        DouaneDTO douane = douaneService.getDouane();

        // Récupérer le camion pour obtenir sa capacité
        Camion camion = voyage.getCamion();
        if (camion == null) {
            throw new RuntimeException("Le voyage n'a pas de camion associé");
        }

        // Déterminer le frais par litre selon le type de produit
        BigDecimal fraisParLitre;
        if (voyage.getProduit() != null && voyage.getProduit().getTypeProduit() != null) {
            // Si le type de produit contient "GAZOLE" (ou "gasoil" en minuscule), utiliser
            // le frais gasoil
            if (voyage.getProduit().getTypeProduit() == Produit.TypeProduit.GAZOLE) {
                fraisParLitre = douane.getFraisParLitreGasoil();
            } else {
                // Pour l'essence et les autres types, utiliser le frais essence
                fraisParLitre = douane.getFraisParLitre();
            }
        } else {
            // Par défaut, utiliser le frais essence
            fraisParLitre = douane.getFraisParLitre();
        }

        // Calculer les frais douane = fraisParLitre * capacité du camion
        BigDecimal fraisDouane = fraisParLitre
                .multiply(BigDecimal.valueOf(camion.getCapacite()));

        // Récupérer les frais T1
        BigDecimal fraisT1 = douane.getFraisT1();

        // Vérifier qu'on ne sélectionne pas les deux en même temps
        // if (compteId != null && caisseId != null) {
        // throw new RuntimeException("Veuillez sélectionner soit un compte bancaire,
        // soit une caisse, pas les deux");
        // }

        // Changer le statut du voyage à RÉCEPTIONNÉ
        voyage.setStatut(Voyage.StatutVoyage.RECEPTIONNER);
        voyage.setDeclarer(true); // Marquer le voyage comme déclaré
        voyage.setPassager("passer_declarer"); // Marquer comme passé déclaré
        updateCamionStatusFromVoyage(camion, voyage.getStatut(), voyage);
        Voyage savedVoyage = voyageRepository.save(voyage);
        validerEtat(voyage, "RECEPTIONNER");
        alerteService.creerAlerte(
                Alerte.TypeAlerte.VOYAGE_DECLARE,
                "Voyage déclaré à la douane : " + savedVoyage.getNumeroVoyage(),
                Alerte.PrioriteAlerte.MOYENNE,
                "Voyage", savedVoyage.getId(), "/voyages/" + savedVoyage.getId());

        // Créer les transactions en attente (non validées)
        Transaction transactionFraisDouane = new Transaction();
        transactionFraisDouane.setMontant(fraisDouane);
        transactionFraisDouane.setDate(LocalDateTime.now());
        transactionFraisDouane.setStatut(Transaction.StatutTransaction.EN_ATTENTE); // En attente
        transactionFraisDouane.setType(Transaction.TypeTransaction.FRAIS_DOUANE);
        transactionFraisDouane.setDescription("Frais de douane pour le voyage " + voyage.getNumeroVoyage() +
                " - Camion " + camion.getImmatriculation());
        transactionFraisDouane.setVoyage(savedVoyage);

        Transaction transactionFraisT1 = new Transaction();
        transactionFraisT1.setMontant(fraisT1);
        transactionFraisT1.setDate(LocalDateTime.now());
        transactionFraisT1.setStatut(Transaction.StatutTransaction.EN_ATTENTE); // En attente
        transactionFraisT1.setType(Transaction.TypeTransaction.FRAIS_T1);
        transactionFraisT1.setDescription("Frais T1 pour le voyage " + voyage.getNumeroVoyage() +
                " - Camion " + camion.getImmatriculation());
        transactionFraisT1.setVoyage(savedVoyage);

        // Sauvegarder les transactions
        transactionFraisDouane = transactionRepository.save(transactionFraisDouane);
        transactionFraisT1 = transactionRepository.save(transactionFraisT1);

        // Créer deux paiements en attente : un pour les frais de douane, un pour le T1
        Paiement paiementDouane = new Paiement();
        paiementDouane.setMontant(fraisDouane);
        paiementDouane.setDate(LocalDate.now());
        paiementDouane.setMethode(Paiement.MethodePaiement.VIREMENT);
        paiementDouane.setStatut(Paiement.StatutPaiement.EN_ATTENTE);
        paiementDouane.setReference("PAY-DOUANE-" + voyage.getNumeroVoyage());
        paiementDouane.setNotes("Frais de douane pour le voyage " + voyage.getNumeroVoyage());
        paiementDouane.getTransactions().add(transactionFraisDouane);
        paiementDouane.setVoyage(savedVoyage);

        Paiement paiementT1 = new Paiement();
        paiementT1.setMontant(fraisT1);
        paiementT1.setDate(LocalDate.now());
        paiementT1.setMethode(Paiement.MethodePaiement.VIREMENT);
        paiementT1.setStatut(Paiement.StatutPaiement.EN_ATTENTE);
        paiementT1.setReference("PAY-T1-" + voyage.getNumeroVoyage());
        paiementT1.setNotes("Frais T1 pour le voyage " + voyage.getNumeroVoyage());
        paiementT1.getTransactions().add(transactionFraisT1);
        paiementT1.setVoyage(savedVoyage);

        paiementRepository.save(paiementDouane);
        paiementRepository.save(paiementT1);

        return voyageMapper.toDTO(savedVoyage);
    }

    @Override
    public List<VoyageDTO> declarerVoyagesMultiple(List<Long> voyageIds, Long compteId, Long caisseId) {
        List<VoyageDTO> voyagesDeclares = new ArrayList<>();

        for (Long voyageId : voyageIds) {
            try {
                VoyageDTO voyageDeclare = declarerVoyage(voyageId, compteId, caisseId);
                voyagesDeclares.add(voyageDeclare);
            } catch (RuntimeException e) {
                // Log l'erreur mais continue avec les autres voyages
                System.err.println("Erreur lors de la déclaration du voyage " + voyageId + ": " + e.getMessage());
            }
        }

        return voyagesDeclares;
    }

    @Override
    public VoyageDTO passerNonDeclarer(Long voyageId) {
        Voyage voyage = voyageRepository.findById(voyageId)
                .orElseThrow(() -> new RuntimeException("Voyage non trouvé avec l'id: " + voyageId));

        voyage.setPassager("passer_non_declarer");
        voyage.setStatut(Voyage.StatutVoyage.RECEPTIONNER);
        Voyage savedVoyage = voyageRepository.save(voyage);
//        validerEtat(voyage, "RECEPTIONNER");
        return voyageMapper.toDTO(savedVoyage);
    }

    @Override
    public VoyageDTO libererVoyage(Long voyageId) {
        Voyage voyage = voyageRepository.findById(voyageId)
                .orElseThrow(() -> new RuntimeException("Voyage non trouvé avec l'id: " + voyageId));
        voyage.setLiberer(true);
        Voyage savedVoyage = voyageRepository.save(voyage);
        validerEtat(voyage, "RECEPTIONNER");
        alerteService.creerAlerte(
                Alerte.TypeAlerte.VOYAGE_LIBERE,
                "Camion sortie de la douane : " + savedVoyage.getNumeroVoyage(),
                Alerte.PrioriteAlerte.BASSE,
                "Voyage", savedVoyage.getId(), "/voyages/" + savedVoyage.getId());
        return voyageMapper.toDTO(savedVoyage);
    }

    @Override
    public List<VoyageDTO> libererVoyages(List<Long> voyageIds) {
        if (voyageIds == null || voyageIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<VoyageDTO> result = new ArrayList<>();
        for (Long voyageId : voyageIds) {
            Voyage voyage = voyageRepository.findById(voyageId).orElse(null);
            if (voyage != null) {
                voyage.setLiberer(true);
                Voyage savedVoyage = voyageRepository.save(voyage);
                alerteService.creerAlerte(
                        Alerte.TypeAlerte.VOYAGE_LIBERE,
                        "Voyage libéré (archivé) : " + savedVoyage.getNumeroVoyage(),
                        Alerte.PrioriteAlerte.BASSE,
                        "Voyage", savedVoyage.getId(), "/voyages/" + savedVoyage.getId());
                result.add(voyageMapper.toDTO(savedVoyage));
            }
        }
        return result;
    }

    @Override
    public TransitaireStatsDTO getTransitaireStats(Long transitaireId) {
        Transitaire transitaire = transitaireRepository.findById(transitaireId)
                .orElseThrow(() -> new RuntimeException("Transitaire non trouvé avec l'id: " + transitaireId));

        // Obtenir le début et la fin du mois actuel
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime debutMois = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime finMois = now.withDayOfMonth(now.toLocalDate().lengthOfMonth())
                .withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        // Compter les voyages déclarés (statut RECEPTIONNER) ce mois pour ce
        // transitaire
        List<Voyage> voyagesDeclares = voyageRepository.findByTransitaire(transitaire).stream()
                .filter(v -> v.getStatut() == Voyage.StatutVoyage.RECEPTIONNER)
                .filter(v -> {
                    // Trouver l'état "Réceptionné" pour obtenir la date de validation
                    List<EtatVoyage> etats = etatVoyageRepository.findByVoyageId(v.getId());
                    return etats.stream()
                            .filter(e -> e.getEtat().equals("Réceptionné") && e.getValider())
                            .anyMatch(e -> {
                                LocalDateTime dateValidation = e.getDateHeure();
                                return dateValidation != null &&
                                        dateValidation.isAfter(debutMois) &&
                                        dateValidation.isBefore(finMois);
                            });
                })
                .toList();

        long nombreCamionsDeclares = voyagesDeclares.size();

        BigDecimal totalFraisDouane = BigDecimal.ZERO;
        BigDecimal totalMontantT1 = BigDecimal.ZERO;

        for (Voyage voyage : voyagesDeclares) {
            List<Transaction> transactions = transactionRepository.findAll().stream()
                    .filter(t -> t.getVoyage() != null && t.getVoyage().getId().equals(voyage.getId()))
                    .filter(t -> t.getType() == Transaction.TypeTransaction.FRAIS_DOUANE ||
                            t.getType() == Transaction.TypeTransaction.FRAIS_T1)
                    .filter(t -> {
                        LocalDateTime dateTransaction = t.getDate();
                        return dateTransaction != null &&
                                dateTransaction.isAfter(debutMois) &&
                                dateTransaction.isBefore(finMois);
                    })
                    .toList();

            for (Transaction transaction : transactions) {
                if (transaction.getType() == Transaction.TypeTransaction.FRAIS_DOUANE) {
                    totalFraisDouane = totalFraisDouane.add(transaction.getMontant());
                } else if (transaction.getType() == Transaction.TypeTransaction.FRAIS_T1) {
                    totalMontantT1 = totalMontantT1.add(transaction.getMontant());
                }
            }
        }

        BigDecimal totalFrais = totalFraisDouane.add(totalMontantT1);
        return new TransitaireStatsDTO(nombreCamionsDeclares, totalFraisDouane, totalMontantT1, totalFrais);
    }

    @Override
    public TransitaireStatsDTO getTransitaireStatsByIdentifiant(String identifiant) {
        Transitaire transitaire = transitaireRepository.findByIdentifiant(identifiant)
                .orElseThrow(() -> new RuntimeException("Transitaire non trouvé avec l'identifiant: " + identifiant));

        // Obtenir le début et la fin du mois actuel
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime debutMois = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime finMois = now.withDayOfMonth(now.toLocalDate().lengthOfMonth())
                .withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        // Compter les voyages déclarés ce mois pour ce transitaire
        // Un voyage est considéré comme déclaré ce mois s'il a declarer = true
        // et qu'il a une transaction FRAIS_DOUANE datée de ce mois
        List<Voyage> voyagesDeclares = voyageRepository.findByTransitaire(transitaire).stream()
                .filter(Voyage::getDeclarer)
                .filter(v -> {
                    // Vérifier qu'il y a une transaction FRAIS_DOUANE ce mois pour ce voyage
                    List<Transaction> transactions = transactionRepository
                            .findFraisDouaniersByVoyageIdAndDateRange(v.getId(), debutMois, finMois);
                    return transactions.stream()
                            .anyMatch(t -> t.getType() == Transaction.TypeTransaction.FRAIS_DOUANE);
                })
                .toList();

        long nombreCamionsDeclares = voyagesDeclares.size();

        BigDecimal totalFraisDouane = BigDecimal.ZERO;
        BigDecimal totalMontantT1 = BigDecimal.ZERO;

        for (Voyage voyage : voyagesDeclares) {
            List<Transaction> transactions = transactionRepository
                    .findFraisDouaniersByVoyageIdAndDateRange(voyage.getId(), debutMois, finMois);

            for (Transaction transaction : transactions) {
                if (transaction.getType() == Transaction.TypeTransaction.FRAIS_DOUANE) {
                    totalFraisDouane = totalFraisDouane.add(transaction.getMontant());
                } else if (transaction.getType() == Transaction.TypeTransaction.FRAIS_T1) {
                    totalMontantT1 = totalMontantT1.add(transaction.getMontant());
                }
            }
        }

        BigDecimal totalFrais = totalFraisDouane.add(totalMontantT1);
        return new TransitaireStatsDTO(nombreCamionsDeclares, totalFraisDouane, totalMontantT1, totalFrais);
    }

    @Override
    public VoyagePageDto findArchivedVoyagesByTransitaire(Long transitaireId, int page, int size) {
        Transitaire transitaire = transitaireRepository.findById(transitaireId)
                .orElseThrow(() -> new RuntimeException("Transitaire non trouvé avec l'id: " + transitaireId));

        Pageable pageable = PageRequest.of(page, size);
        Page<Voyage> voyagePage = voyageRepository.findArchivedVoyagesByTransitaire(transitaire, pageable);

        List<VoyageDTO> voyages = voyagePage.getContent().stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());

        return new VoyagePageDto(
                voyages,
                voyagePage.getNumber(),
                voyagePage.getTotalPages(),
                voyagePage.getTotalElements(),
                voyagePage.getSize());
    }

    @Override
    public VoyagePageDto findArchivedVoyagesByTransitaireAndDate(Long transitaireId, LocalDate date, int page,
            int size) {
        Transitaire transitaire = transitaireRepository.findById(transitaireId)
                .orElseThrow(() -> new RuntimeException("Transitaire non trouvé avec l'id: " + transitaireId));

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59).plusSeconds(1); // Début du jour suivant
        Pageable pageable = PageRequest.of(page, size);
        Page<Voyage> voyagePage = voyageRepository.findArchivedVoyagesByTransitaireAndDate(transitaire, startOfDay,
                endOfDay, pageable);

        List<VoyageDTO> voyages = voyagePage.getContent().stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());

        return new VoyagePageDto(
                voyages,
                voyagePage.getNumber(),
                voyagePage.getTotalPages(),
                voyagePage.getTotalElements(),
                voyagePage.getSize());
    }

    @Override
    public VoyagePageDto findArchivedVoyagesByTransitaireAndDateRange(Long transitaireId, LocalDate startDate,
            LocalDate endDate, int page, int size) {
        Transitaire transitaire = transitaireRepository.findById(transitaireId)
                .orElseThrow(() -> new RuntimeException("Transitaire non trouvé avec l'id: " + transitaireId));

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        Pageable pageable = PageRequest.of(page, size);
        Page<Voyage> voyagePage = voyageRepository.findArchivedVoyagesByTransitaireAndDateRange(transitaire,
                startDateTime, endDateTime, pageable);

        List<VoyageDTO> voyages = voyagePage.getContent().stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());

        return new VoyagePageDto(
                voyages,
                voyagePage.getNumber(),
                voyagePage.getTotalPages(),
                voyagePage.getTotalElements(),
                voyagePage.getSize());
    }

    @Override
    public VoyagePageDto findArchivedVoyagesByTransitaireIdentifiant(String identifiant, int page, int size) {
        Transitaire transitaire = transitaireRepository.findByIdentifiant(identifiant)
                .orElseThrow(() -> new RuntimeException("Transitaire non trouvé avec l'identifiant: " + identifiant));

        Pageable pageable = PageRequest.of(page, size);
        // Archives = voyages libérés (passent aux archives quand on les libère)
        Page<Voyage> voyagePage = voyageRepository.findVoyagesArchivesByTransitaire(transitaire, pageable);

        List<VoyageDTO> voyages = voyagePage.getContent().stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());

        return new VoyagePageDto(
                voyages,
                voyagePage.getNumber(),
                voyagePage.getTotalPages(),
                voyagePage.getTotalElements(),
                voyagePage.getSize());
    }

    @Override
    public VoyagePageDto findVoyagesEnCoursByTransitaireIdentifiant(String identifiant, int page, int size) {
        Transitaire transitaire = transitaireRepository.findByIdentifiant(identifiant)
                .orElseThrow(() -> new RuntimeException("Transitaire non trouvé avec l'identifiant: " + identifiant));

        Pageable pageable = PageRequest.of(page, size);
        Page<Voyage> voyagePage = voyageRepository.findVoyagesEnCoursByTransitaire(transitaire, pageable);

        List<VoyageDTO> voyages = voyagePage.getContent().stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());

        return new VoyagePageDto(
                voyages,
                voyagePage.getNumber(),
                voyagePage.getTotalPages(),
                voyagePage.getTotalElements(),
                voyagePage.getSize());
    }

    @Override
    public VoyagePageDto findArchivedVoyagesByTransitaireIdentifiantAndDate(String identifiant, LocalDate date,
            int page, int size) {
        Transitaire transitaire = transitaireRepository.findByIdentifiant(identifiant)
                .orElseThrow(() -> new RuntimeException("Transitaire non trouvé avec l'identifiant: " + identifiant));

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59).plusSeconds(1);
        Pageable pageable = PageRequest.of(page, size);
        Page<Voyage> voyagePage = voyageRepository.findVoyagesArchivesByTransitaireAndDate(transitaire, startOfDay,
                endOfDay, pageable);

        List<VoyageDTO> voyages = voyagePage.getContent().stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());

        return new VoyagePageDto(
                voyages,
                voyagePage.getNumber(),
                voyagePage.getTotalPages(),
                voyagePage.getTotalElements(),
                voyagePage.getSize());
    }

    @Override
    public VoyagePageDto findArchivedVoyagesByTransitaireIdentifiantAndDateRange(String identifiant,
            LocalDate startDate, LocalDate endDate, int page, int size) {
        Transitaire transitaire = transitaireRepository.findByIdentifiant(identifiant)
                .orElseThrow(() -> new RuntimeException("Transitaire non trouvé avec l'identifiant: " + identifiant));

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        Pageable pageable = PageRequest.of(page, size);
        Page<Voyage> voyagePage = voyageRepository.findVoyagesArchivesByTransitaireAndDateRange(transitaire,
                startDateTime, endDateTime, pageable);

        List<VoyageDTO> voyages = voyagePage.getContent().stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());

        return new VoyagePageDto(
                voyages,
                voyagePage.getNumber(),
                voyagePage.getTotalPages(),
                voyagePage.getTotalElements(),
                voyagePage.getSize());
    }

    @Override
    public VoyagePageDto findArchivedVoyages(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Voyage> voyagePage = voyageRepository.findArchivedVoyages(pageable);

        List<VoyageDTO> voyages = voyagePage.getContent().stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());

        return new VoyagePageDto(
                voyages,
                voyagePage.getNumber(),
                voyagePage.getTotalPages(),
                voyagePage.getTotalElements(),
                voyagePage.getSize());
    }

    @Override
    public VoyagePageDto findArchivedVoyagesByDate(LocalDate date, int page, int size) {
        LocalDateTime dateTime = date.atStartOfDay();
        Pageable pageable = PageRequest.of(page, size);
        Page<Voyage> voyagePage = voyageRepository.findArchivedVoyagesByDate(dateTime, pageable);

        List<VoyageDTO> voyages = voyagePage.getContent().stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());

        return new VoyagePageDto(
                voyages,
                voyagePage.getNumber(),
                voyagePage.getTotalPages(),
                voyagePage.getTotalElements(),
                voyagePage.getSize());
    }

    @Override
    public VoyagePageDto findArchivedVoyagesByDateRange(LocalDate startDate, LocalDate endDate, int page, int size) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        Pageable pageable = PageRequest.of(page, size);
        Page<Voyage> voyagePage = voyageRepository.findArchivedVoyagesByDateRange(startDateTime, endDateTime, pageable);

        List<VoyageDTO> voyages = voyagePage.getContent().stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());

        return new VoyagePageDto(
                voyages,
                voyagePage.getNumber(),
                voyagePage.getTotalPages(),
                voyagePage.getTotalElements(),
                voyagePage.getSize());
    }

    @Override
    public List<VoyageDTO> findVoyagesPassesNonDeclares() {
        return voyageRepository.findVoyagesPassesNonDeclares().stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public VoyagePageDto findVoyagesPassesNonDeclaresPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Voyage> voyagePage = voyageRepository.findVoyagesPassesNonDeclaresPaginated(pageable);

        List<VoyageDTO> voyages = voyagePage.getContent().stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());

        return new VoyagePageDto(
                voyages,
                voyagePage.getNumber(),
                voyagePage.getTotalPages(),
                voyagePage.getTotalElements(),
                voyagePage.getSize());
    }

    @Override
    public VoyagePageDto findVoyagesAvecClientSansFacture(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Voyage> voyagePage = voyageRepository.findVoyagesAvecClientSansFacture(pageable);

        List<VoyageDTO> voyages = voyagePage.getContent().stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());

        return new VoyagePageDto(
                voyages,
                voyagePage.getNumber(),
                voyagePage.getTotalPages(),
                voyagePage.getTotalElements(),
                voyagePage.getSize());
    }

    @Override
    public VoyagesParClientPageDto findVoyagesAvecClientSansFactureGroupesParClient(
            int page, int size) {
        // Récupérer tous les ClientVoyage avec client mais sans prix d'achat (sans facture)
        List<ClientVoyage> allClientVoyages = clientVoyageRepository.findByPrixAchatIsNull();

        // Grouper par client
        Map<Long, List<ClientVoyage>> clientVoyagesParClient = allClientVoyages.stream()
                .collect(Collectors.groupingBy(cv -> cv.getClient().getId()));

        // Convertir en liste de ClientVoyagesDTO
        List<ClientVoyagesDTO> clientsVoyages = clientVoyagesParClient.entrySet().stream()
                .map(entry -> {
                    Long clientId = entry.getKey();
                    List<ClientVoyage> clientVoyages = entry.getValue();
                    ClientVoyage premierClientVoyage = clientVoyages.get(0);

                    ClientVoyagesDTO dto = new ClientVoyagesDTO();
                    dto.setClientId(clientId);
                    dto.setClientNom(premierClientVoyage.getClient().getNom());
                    dto.setClientEmail(premierClientVoyage.getClient().getEmail());
                    // Convertir les ClientVoyage en VoyageDTO (en récupérant les voyages associés)
                    dto.setVoyages(clientVoyages.stream()
                            .map(cv -> voyageMapper.toDTO(cv.getVoyage()))
                            .distinct() // Éviter les doublons si plusieurs ClientVoyage pour le même voyage
                            .collect(Collectors.toList()));
                    dto.setNombreVoyages(clientVoyages.size());

                    return dto;
                })
                .sorted((a, b) -> Integer.compare(b.getNombreVoyages(), a.getNombreVoyages())) // Trier par nombre de
                                                                                               // voyages décroissant
                .collect(Collectors.toList());

        // Appliquer la pagination manuellement
        int totalElements = clientsVoyages.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int start = page * size;
        int end = Math.min(start + size, totalElements);

        List<ClientVoyagesDTO> pageContent = start < totalElements
                ? clientsVoyages.subList(start, end)
                : new ArrayList<>();

        return new VoyagesParClientPageDto(
                pageContent,
                page,
                totalPages,
                totalElements,
                size);
    }

    @Override
    public VoyagePageDto findVoyagesPartiellementDecharges(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Voyage> voyagePage = voyageRepository.findVoyagesPartiellementDecharges(pageable);

        List<VoyageDTO> voyages = voyagePage.getContent().stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());

        return new VoyagePageDto(
                voyages,
                voyagePage.getNumber(),
                voyagePage.getTotalPages(),
                voyagePage.getTotalElements(),
                voyagePage.getSize());
    }

    @Override
    public VoyagePageDto findVoyagesEnCours(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Voyage> voyagePage = voyageRepository.findVoyagesEnCours(pageable);

        List<VoyageDTO> voyages = voyagePage.getContent().stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());

        return new VoyagePageDto(
                voyages,
                voyagePage.getNumber(),
                voyagePage.getTotalPages(),
                voyagePage.getTotalElements(),
                voyagePage.getSize());
    }

    @Override
    public List<VoyageDTO> findVoyagesEnCoursAvecClients() {
        List<Voyage> voyages = voyageRepository.findVoyagesEnCoursAvecClients();
        return voyages.stream()
                .map(voyageMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convertit un statut (peut être ancien ou nouveau) vers le nouveau format
     */
    private Voyage.StatutVoyage convertStatut(String statut) {
        if (statut == null) {
            return null;
        }

        // Mapping des anciens statuts vers les nouveaux
        switch (statut) {
            case "ASSIGNE_AU_CHARGEMENT":
            case "EN_CHARGEMENT":
                return Voyage.StatutVoyage.CHARGEMENT;
            case "DEPART":
                return Voyage.StatutVoyage.DEPART;
            case "EN_ROUTE_VERS_BAMAKO":
            case "EN_DEPOT_SORTIE_DEPOT":
            case "EN_TRANSIT_VERS_STATION":
                return Voyage.StatutVoyage.ARRIVER;
            case "A_LA_DOUANE":
            case "A_LA_FRONTIERE":
            case "SORTIE_A_LA_DOUANE":
                return Voyage.StatutVoyage.DOUANE;
            case "DECLARE":
                return Voyage.StatutVoyage.RECEPTIONNER;
            case "LIVRE":
            case "DEPOTE_EN_STATION":
                return Voyage.StatutVoyage.LIVRE;
            case "DECHARGER":
                return Voyage.StatutVoyage.DECHARGER;
            default:
                // Essayer de convertir directement si c'est déjà un nouveau statut
                try {
                    return Voyage.StatutVoyage.valueOf(statut);
                } catch (IllegalArgumentException e) {
                    // Si le statut n'existe pas, retourner CHARGEMENT par défaut
                    return Voyage.StatutVoyage.CHARGEMENT;
                }
        }
    }
}
