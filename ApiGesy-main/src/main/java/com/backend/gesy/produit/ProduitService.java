package com.backend.gesy.produit;

import com.backend.gesy.produit.dto.ProduitDTO;
import com.backend.gesy.produit.dto.ProduitAvecStocksDTO;

import java.util.List;
import java.util.Optional;

public interface ProduitService {
    List<ProduitDTO> findAll();
    List<ProduitAvecStocksDTO> findAllAvecStocks();
    Optional<ProduitDTO> findById(Long id);
    Optional<ProduitAvecStocksDTO> findByIdAvecStocks(Long id);
    Optional<ProduitDTO> findByNom(String nom);
    ProduitDTO save(ProduitDTO produitDTO);
    ProduitDTO update(Long id, ProduitDTO produitDTO);
    void deleteById(Long id);
}

