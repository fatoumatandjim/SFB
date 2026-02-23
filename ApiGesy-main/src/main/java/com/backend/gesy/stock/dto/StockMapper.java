package com.backend.gesy.stock.dto;

import com.backend.gesy.stock.Stock;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class StockMapper {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm");

    public StockDTO toDTO(Stock stock) {
        if (stock == null) {
            return null;
        }
        
        StockDTO dto = new StockDTO();
        dto.setId(stock.getId());
        dto.setProduitId(stock.getProduit() != null ? stock.getProduit().getId() : null);
        dto.setProduitNom(stock.getProduit() != null ? stock.getProduit().getNom() : null);
        dto.setTypeProduit(stock.getProduit() != null && stock.getProduit().getTypeProduit() != null 
            ? stock.getProduit().getTypeProduit().name() : null);
        dto.setQuantite(stock.getQuantite());
        dto.setQuantityCession(stock.getQuantityCession() != null ? stock.getQuantityCession() : 0.0);
        dto.setDepotId(stock.getDepot() != null ? stock.getDepot().getId() : null);
        dto.setDepotNom(stock.getDepot() != null ? stock.getDepot().getNom() : null);
        dto.setSeuilMinimum(stock.getSeuilMinimum());
        dto.setPrixUnitaire(stock.getPrixUnitaire());
        dto.setUnite(stock.getUnite());
        dto.setDateDerniereMiseAJour(stock.getDateDerniereMiseAJour() != null 
            ? stock.getDateDerniereMiseAJour().format(DATE_FORMATTER) : null);
        
        return dto;
    }

    public Stock toEntity(StockDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Stock stock = new Stock();
        stock.setId(dto.getId());
        stock.setQuantite(dto.getQuantite());
        stock.setQuantityCession(dto.getQuantityCession() != null ? dto.getQuantityCession() : 0.0);
        stock.setSeuilMinimum(dto.getSeuilMinimum());
        stock.setPrixUnitaire(dto.getPrixUnitaire());
        stock.setUnite(dto.getUnite());
        // produit et depot seront définis par le service
        
        return stock;
    }
}

