package com.backend.gesy.stock;

import com.backend.gesy.alerte.Alerte;
import com.backend.gesy.alerte.AlerteService;
import com.backend.gesy.achat.AchatService;
import com.backend.gesy.achat.dto.AchatDTO;
import com.backend.gesy.depot.Depot;
import com.backend.gesy.depot.DepotRepository;
import com.backend.gesy.mouvement.MouvementService;
import com.backend.gesy.mouvement.dto.MouvementDTO;
import com.backend.gesy.produit.Produit;
import com.backend.gesy.produit.ProduitRepository;
import com.backend.gesy.stock.dto.StockDTO;
import com.backend.gesy.stock.dto.StockMapper;
import com.backend.gesy.stock.dto.StockStatsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StockServiceImpl implements StockService {
    private final StockRepository stockRepository;
    private final DepotRepository depotRepository;
    private final ProduitRepository produitRepository;
    private final StockMapper stockMapper;
    private final MouvementService mouvementService;
    private final AchatService achatService;
    private final AlerteService alerteService;

    @Override
    public List<StockDTO> findAll() {
        return stockRepository.findAll().stream()
            .map(stockMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<StockDTO> findById(Long id) {
        return stockRepository.findById(id)
            .map(stockMapper::toDTO);
    }

    @Override
    public List<StockDTO> findByDepotId(Long depotId) {
        return stockRepository.findByDepotId(depotId).stream()
            .map(stockMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<StockDTO> findByProduitId(Long produitId) {
        return stockRepository.findByProduitId(produitId).stream()
            .map(stockMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public StockDTO save(StockDTO stockDTO) {
        // Vérifier que les IDs sont fournis
        if (stockDTO.getProduitId() == null) {
            throw new RuntimeException("L'ID du produit est requis");
        }
        if (stockDTO.getDepotId() == null) {
            throw new RuntimeException("L'ID du dépôt est requis");
        }

        // Récupérer le produit
        Produit produit = produitRepository.findById(stockDTO.getProduitId())
            .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + stockDTO.getProduitId()));

        // Récupérer le dépôt
        Depot depot = depotRepository.findById(stockDTO.getDepotId())
            .orElseThrow(() -> new RuntimeException("Dépôt non trouvé avec l'id: " + stockDTO.getDepotId()));

        // Vérifier si un stock existe déjà pour ce produit dans ce dépôt
        Optional<Stock> existingStockOpt = stockRepository.findByDepotIdAndProduitId(
            stockDTO.getDepotId(), 
            stockDTO.getProduitId()
        );

        Stock savedStock;
        Double quantiteAjoutee;
        boolean isUpdate = existingStockOpt.isPresent();

        if (isUpdate) {
            // Mettre à jour le stock existant
            Stock existingStock = existingStockOpt.get();
            Double ancienneQuantite = existingStock.getQuantite();
            quantiteAjoutee = stockDTO.getQuantite();
            Double nouvelleQuantite = ancienneQuantite + quantiteAjoutee;

            // Vérifier la capacité disponible du dépôt
            Double capaciteUtilisee = depot.getCapaciteUtilisee() != null ? depot.getCapaciteUtilisee() : 0.0;
            Double capaciteDisponible = depot.getCapacite() - capaciteUtilisee;
            
            if (quantiteAjoutee > capaciteDisponible) {
                throw new RuntimeException("Capacité insuffisante dans le dépôt. Capacité disponible: " + capaciteDisponible + " " + (stockDTO.getUnite() != null ? stockDTO.getUnite() : "L"));
            }

            // Mettre à jour le stock existant
            existingStock.setQuantite(nouvelleQuantite);
            if (stockDTO.getSeuilMinimum() != null) {
                existingStock.setSeuilMinimum(stockDTO.getSeuilMinimum());
            }
            if (stockDTO.getPrixUnitaire() != null) {
                existingStock.setPrixUnitaire(stockDTO.getPrixUnitaire());
            }
            if (stockDTO.getUnite() != null) {
                existingStock.setUnite(stockDTO.getUnite());
            }

            savedStock = stockRepository.save(existingStock);

            // Mettre à jour la capacité utilisée du dépôt
            Double nouvelleCapaciteUtilisee = capaciteUtilisee + quantiteAjoutee;
            depot.setCapaciteUtilisee(nouvelleCapaciteUtilisee);
        } else {
            // Créer un nouveau stock
            Stock stock = stockMapper.toEntity(stockDTO);
            stock.setProduit(produit);
            stock.setDepot(depot);

            // Vérifier la capacité disponible du dépôt
            Double capaciteUtilisee = depot.getCapaciteUtilisee() != null ? depot.getCapaciteUtilisee() : 0.0;
            Double capaciteDisponible = depot.getCapacite() - capaciteUtilisee;
            
            if (stock.getQuantite() > capaciteDisponible) {
                throw new RuntimeException("Capacité insuffisante dans le dépôt. Capacité disponible: " + capaciteDisponible + " " + (stock.getUnite() != null ? stock.getUnite() : "L"));
            }

            // Sauvegarder le nouveau stock
            savedStock = stockRepository.save(stock);
            quantiteAjoutee = stock.getQuantite();

            // Mettre à jour la capacité utilisée du dépôt
            Double nouvelleCapaciteUtilisee = capaciteUtilisee + stock.getQuantite();
            depot.setCapaciteUtilisee(nouvelleCapaciteUtilisee);
        }

        // Mettre à jour le statut du dépôt si nécessaire
        if (depot.getCapaciteUtilisee() >= depot.getCapacite()) {
            depot.setStatut(Depot.StatutDepot.PLEIN);
        } else if (depot.getStatut() == Depot.StatutDepot.PLEIN && depot.getCapaciteUtilisee() < depot.getCapacite()) {
            depot.setStatut(Depot.StatutDepot.ACTIF);
        }
        
        depotRepository.save(depot);

        // Créer un mouvement d'entrée
        MouvementDTO mouvementDTO = new MouvementDTO();
        mouvementDTO.setStockId(savedStock.getId());
        mouvementDTO.setTypeMouvement("ENTREE");
        mouvementDTO.setQuantite(quantiteAjoutee);
        mouvementDTO.setUnite(savedStock.getUnite());
        String description = isUpdate 
            ? "Ajout de stock - " + produit.getNom() + " dans " + depot.getNom() + " (mise à jour)"
            : "Ajout de stock - " + produit.getNom() + " dans " + depot.getNom() + " (nouveau)";
        mouvementDTO.setDescription(description);
        mouvementService.save(mouvementDTO);

        // Créer un Achat pour cet approvisionnement
        AchatDTO achatDTO = new AchatDTO();
        achatDTO.setDepotId(depot.getId());
        achatDTO.setProduitId(produit.getId());
        achatDTO.setQuantite(quantiteAjoutee);
        achatDTO.setPrixUnitaire(stockDTO.getPrixUnitaire() != null 
            ? BigDecimal.valueOf(stockDTO.getPrixUnitaire()) 
            : null);
        if (achatDTO.getPrixUnitaire() != null && achatDTO.getQuantite() != null) {
            achatDTO.setMontantTotal(achatDTO.getPrixUnitaire()
                .multiply(BigDecimal.valueOf(achatDTO.getQuantite())));
        }
        achatDTO.setUnite(stockDTO.getUnite());
        achatDTO.setDescription("Approvisionnement - " + produit.getNom() + " dans " + depot.getNom());
        achatDTO.setDateAchat(java.time.LocalDateTime.now());
        achatService.save(achatDTO);

        if (savedStock.getSeuilMinimum() != null && savedStock.getQuantite() != null
                && savedStock.getQuantite() <= savedStock.getSeuilMinimum()) {
            alerteService.creerAlerte(Alerte.TypeAlerte.STOCK_FAIBLE,
                    "Stock faible : " + produit.getNom() + " dans " + depot.getNom() + " (quantité: " + savedStock.getQuantite() + ")",
                    Alerte.PrioriteAlerte.HAUTE, "Stock", savedStock.getId(), "/stocks/" + savedStock.getId());
        }
        return stockMapper.toDTO(savedStock);
    }

    @Override
    public StockDTO update(Long id, StockDTO stockDTO) {
        Stock existingStock = stockRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Stock non trouvé avec l'id: " + id));
        
        Depot ancienDepot = existingStock.getDepot();
        Double ancienneQuantite = existingStock.getQuantite();
        
        Stock stock = stockMapper.toEntity(stockDTO);
        stock.setId(existingStock.getId());

        // Récupérer le produit
        if (stockDTO.getProduitId() != null) {
            Produit produit = produitRepository.findById(stockDTO.getProduitId())
                .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + stockDTO.getProduitId()));
            stock.setProduit(produit);
        } else {
            stock.setProduit(existingStock.getProduit());
        }

        // Récupérer le dépôt
        Depot nouveauDepot;
        if (stockDTO.getDepotId() != null && !stockDTO.getDepotId().equals(existingStock.getDepot().getId())) {
            nouveauDepot = depotRepository.findById(stockDTO.getDepotId())
                .orElseThrow(() -> new RuntimeException("Dépôt non trouvé avec l'id: " + stockDTO.getDepotId()));
            stock.setDepot(nouveauDepot);
        } else {
            nouveauDepot = existingStock.getDepot();
            stock.setDepot(nouveauDepot);
        }

        Double nouvelleQuantite = stock.getQuantite();
        Double differenceQuantite = nouvelleQuantite - ancienneQuantite;

        // Si le dépôt a changé, ajuster les deux dépôts
        if (!ancienDepot.getId().equals(nouveauDepot.getId())) {
            // Retirer la quantité de l'ancien dépôt
            Double ancienneCapaciteUtilisee = ancienDepot.getCapaciteUtilisee() != null ? ancienDepot.getCapaciteUtilisee() : 0.0;
            ancienDepot.setCapaciteUtilisee(Math.max(0.0, ancienneCapaciteUtilisee - ancienneQuantite));
            
            // Vérifier la capacité disponible du nouveau dépôt
            Double capaciteUtilisee = nouveauDepot.getCapaciteUtilisee() != null ? nouveauDepot.getCapaciteUtilisee() : 0.0;
            Double capaciteDisponible = nouveauDepot.getCapacite() - capaciteUtilisee;
            
            if (nouvelleQuantite > capaciteDisponible) {
                throw new RuntimeException("Capacité insuffisante dans le nouveau dépôt. Capacité disponible: " + capaciteDisponible + " " + (stock.getUnite() != null ? stock.getUnite() : "L"));
            }
            
            // Ajouter la quantité au nouveau dépôt
            nouveauDepot.setCapaciteUtilisee(capaciteUtilisee + nouvelleQuantite);
            
            depotRepository.save(ancienDepot);
        } else {
            // Même dépôt, ajuster la capacité utilisée
            Double capaciteUtilisee = nouveauDepot.getCapaciteUtilisee() != null ? nouveauDepot.getCapaciteUtilisee() : 0.0;
            Double nouvelleCapaciteUtilisee = capaciteUtilisee + differenceQuantite;
            
            if (nouvelleCapaciteUtilisee < 0) {
                throw new RuntimeException("La quantité ne peut pas être négative");
            }
            
            Double capaciteDisponible = nouveauDepot.getCapacite() - capaciteUtilisee;
            if (differenceQuantite > capaciteDisponible) {
                throw new RuntimeException("Capacité insuffisante dans le dépôt. Capacité disponible: " + capaciteDisponible + " " + (stock.getUnite() != null ? stock.getUnite() : "L"));
            }
            
            nouveauDepot.setCapaciteUtilisee(nouvelleCapaciteUtilisee);
        }

        // Mettre à jour le statut du dépôt si nécessaire
        if (nouveauDepot.getCapaciteUtilisee() >= nouveauDepot.getCapacite()) {
            nouveauDepot.setStatut(Depot.StatutDepot.PLEIN);
        } else if (nouveauDepot.getStatut() == Depot.StatutDepot.PLEIN && nouveauDepot.getCapaciteUtilisee() < nouveauDepot.getCapacite()) {
            nouveauDepot.setStatut(Depot.StatutDepot.ACTIF);
        }
        
        depotRepository.save(nouveauDepot);

        Stock updatedStock = stockRepository.save(stock);

        // Créer un mouvement selon la différence de quantité
        if (differenceQuantite != 0) {
            MouvementDTO mouvementDTO = new MouvementDTO();
            mouvementDTO.setStockId(updatedStock.getId());
            mouvementDTO.setQuantite(Math.abs(differenceQuantite));
            mouvementDTO.setUnite(updatedStock.getUnite());
            
            if (differenceQuantite > 0) {
                // Augmentation de stock = ENTREE
                mouvementDTO.setTypeMouvement("ENTREE");
                mouvementDTO.setDescription("Augmentation de stock de " + Math.abs(differenceQuantite) + " " + updatedStock.getUnite() + " de " + updatedStock.getProduit().getNom() + " au dépôt " + nouveauDepot.getNom());
            } else {
                // Diminution de stock = SORTIE
                mouvementDTO.setTypeMouvement("SORTIE");
                mouvementDTO.setDescription("Sortie de stock de " + Math.abs(differenceQuantite) + " " + updatedStock.getUnite() + " de " + updatedStock.getProduit().getNom() + " du dépôt " + nouveauDepot.getNom());
            }
            mouvementService.save(mouvementDTO);
        }

        if (updatedStock.getSeuilMinimum() != null && updatedStock.getQuantite() != null
                && updatedStock.getQuantite() <= updatedStock.getSeuilMinimum()) {
            alerteService.creerAlerte(Alerte.TypeAlerte.STOCK_FAIBLE,
                    "Stock faible : " + updatedStock.getProduit().getNom() + " dans " + nouveauDepot.getNom() + " (quantité: " + updatedStock.getQuantite() + ")",
                    Alerte.PrioriteAlerte.HAUTE, "Stock", updatedStock.getId(), "/stocks/" + updatedStock.getId());
        }
        return stockMapper.toDTO(updatedStock);
    }

    @Override
    public void deleteById(Long id) {
        Stock stock = stockRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Stock non trouvé avec l'id: " + id));
        
        Depot depot = stock.getDepot();
        Double quantite = stock.getQuantite();
        
        // Retirer la quantité de la capacité utilisée du dépôt
        Double capaciteUtilisee = depot.getCapaciteUtilisee() != null ? depot.getCapaciteUtilisee() : 0.0;
        depot.setCapaciteUtilisee(Math.max(0.0, capaciteUtilisee - quantite));
        
        // Mettre à jour le statut du dépôt si nécessaire
        if (depot.getStatut() == Depot.StatutDepot.PLEIN && depot.getCapaciteUtilisee() < depot.getCapacite()) {
            depot.setStatut(Depot.StatutDepot.ACTIF);
        }
        
        depotRepository.save(depot);
        stockRepository.deleteById(id);
    }

    @Override
    public StockStatsDTO getStats() {
        // Filtrer les stocks pour n'utiliser que ceux des dépôts ACTIF (ou les citernes qui n'ont pas de dépôt)
        List<Stock> allStocks = stockRepository.findByDepotStatut(Depot.StatutDepot.ACTIF);
        
        // Utiliser uniquement les dépôts ACTIF
        List<Depot> allDepots = depotRepository.findByStatut(Depot.StatutDepot.ACTIF);
        
        // Calculer le total des unités en stock (uniquement dépôts ACTIF)
        long totalUnites = allStocks.stream()
            .mapToLong(stock -> stock.getQuantite() != null ? stock.getQuantite().longValue() : 0L)
            .sum();
        
        // Compter les dépôts actifs et les villes uniques
        long totalDepots = allDepots.size();
        
        Set<String> villes = allDepots.stream()
            .filter(depot -> depot.getVille() != null && !depot.getVille().isEmpty())
            .map(Depot::getVille)
            .collect(Collectors.toSet());
        int villesDepots = villes.size();
        
        // Compter les produits critiques (stock < seuil minimum) - uniquement dépôts ACTIF
        int produitsCritiques = (int) allStocks.stream()
            .filter(stock -> {
                if (stock.getSeuilMinimum() == null || stock.getQuantite() == null) {
                    return false;
                }
                return stock.getQuantite() < stock.getSeuilMinimum();
            })
            .count();
        
        // Calculer la valeur totale du stock (uniquement dépôts ACTIF)
        double valeurStock = allStocks.stream()
            .mapToDouble(stock -> {
                if (stock.getPrixUnitaire() != null && stock.getQuantite() != null) {
                    return stock.getPrixUnitaire() * stock.getQuantite();
                }
                return 0.0;
            })
            .sum();
        
        StockStatsDTO stats = new StockStatsDTO();
        stats.setTotalUnites(totalUnites);
        stats.setEvolutionTotalUnites("+5.2%"); // TODO: Calculer l'évolution réelle
        stats.setPeriodeTotalUnites("ce mois");
        stats.setTotalDepots((int) totalDepots);
        stats.setVillesDepots(villesDepots);
        stats.setProduitsCritiques(produitsCritiques);
        stats.setUrgentProduitsCritiques(produitsCritiques > 0);
        stats.setValeurStock(valeurStock);
        stats.setEvolutionValeurStock("+12.5%"); // TODO: Calculer l'évolution réelle
        stats.setPeriodeValeurStock("ce mois");
        
        return stats;
    }
}

