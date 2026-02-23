package com.backend.gesy.produit.dto;

import com.backend.gesy.produit.Produit;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ProduitAvecStocksMapper {
    
    public ProduitAvecStocksDTO toDTO(Produit produit) {
        if (produit == null) {
            return null;
        }

        ProduitAvecStocksDTO dto = new ProduitAvecStocksDTO();
        dto.setId(produit.getId());
        dto.setNom(produit.getNom());
        dto.setTypeProduit(produit.getTypeProduit() != null ? produit.getTypeProduit().name() : null);
        dto.setDescription(produit.getDescription());

        // Calculer la quantité totale et mapper les stocks
        // Filtrer pour n'utiliser que les stocks des dépôts ACTIF (ou les citernes qui n'ont pas de dépôt)
        if (produit.getStocks() != null && !produit.getStocks().isEmpty()) {
            java.util.List<com.backend.gesy.stock.Stock> stocksActifs = produit.getStocks().stream()
                .filter(stock -> stock.getDepot() == null || 
                                stock.getDepot().getStatut() == com.backend.gesy.depot.Depot.StatutDepot.ACTIF)
                .collect(Collectors.toList());
            
            double quantiteTotale = stocksActifs.stream()
                .mapToDouble(stock -> stock.getQuantite() != null ? stock.getQuantite() : 0.0)
                .sum();
            dto.setQuantiteTotale(quantiteTotale);

            dto.setStocks(stocksActifs.stream()
                .map(stock -> {
                    ProduitAvecStocksDTO.StockInfoDTO stockInfo = new ProduitAvecStocksDTO.StockInfoDTO();
                    stockInfo.setStockId(stock.getId());
                    stockInfo.setNom(stock.getNom());
                    stockInfo.setCiterne(stock.isCiterne());
                    if (stock.getDepot() != null) {
                        stockInfo.setDepotNom(stock.getDepot().getNom());
                        stockInfo.setDepotVille(stock.getDepot().getVille());
                    }
                    stockInfo.setQuantite(stock.getQuantite());
                    stockInfo.setUnite(stock.getUnite());
                    stockInfo.setSeuilMinimum(stock.getSeuilMinimum());
                    stockInfo.setPrixUnitaire(stock.getPrixUnitaire());
                    return stockInfo;
                })
                .collect(Collectors.toList()));
        } else {
            dto.setQuantiteTotale(0.0);
            dto.setStocks(java.util.Collections.emptyList());
        }

        return dto;
    }
}

