package com.backend.gesy.produit;

import com.backend.gesy.produit.dto.ProduitDTO;
import com.backend.gesy.produit.dto.ProduitMapper;
import com.backend.gesy.produit.dto.ProduitAvecStocksDTO;
import com.backend.gesy.produit.dto.ProduitAvecStocksMapper;
import com.backend.gesy.stock.Stock;
import com.backend.gesy.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProduitServiceImpl implements ProduitService {
    private final ProduitRepository produitRepository;
    private final ProduitMapper produitMapper;
    private final ProduitAvecStocksMapper produitAvecStocksMapper;
    private final StockRepository stockRepository;

    @Override
    public List<ProduitDTO> findAll() {
        return produitRepository.findAll().stream()
            .map(produitMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<ProduitAvecStocksDTO> findAllAvecStocks() {
        return produitRepository.findAllWithStocks().stream()
            .map(produitAvecStocksMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<ProduitDTO> findById(Long id) {
        return produitRepository.findById(id)
            .map(produitMapper::toDTO);
    }

    @Override
    public Optional<ProduitAvecStocksDTO> findByIdAvecStocks(Long id) {
        return produitRepository.findById(id)
            .map(produitAvecStocksMapper::toDTO);
    }

    @Override
    public Optional<ProduitDTO> findByNom(String nom) {
        return produitRepository.findByNom(nom)
            .map(produitMapper::toDTO);
    }

    @Override
    public ProduitDTO save(ProduitDTO produitDTO) {
        Produit produit = produitMapper.toEntity(produitDTO);
        Produit savedProduit = produitRepository.save(produit);
        if (stockRepository.findByProduitIdAndCiterne(savedProduit.getId(), true).isEmpty()) {
            Stock stockCiterne = new Stock();
            stockCiterne.setProduit(savedProduit);
            stockCiterne.setQuantite((double) 0);
            stockCiterne.setCiterne(true);
            stockCiterne.setDateDerniereMiseAJour(LocalDateTime.now());
            stockCiterne.setNom("Stock Citerne " + savedProduit.getNom());
            stockRepository.save(stockCiterne);
        }
        return produitMapper.toDTO(savedProduit);
    }

    @Override
    public ProduitDTO update(Long id, ProduitDTO produitDTO) {
        Produit existingProduit = produitRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + id));
        Produit produit = produitMapper.toEntity(produitDTO);
        produit.setId(existingProduit.getId());
        Produit updatedProduit = produitRepository.save(produit);
        return produitMapper.toDTO(updatedProduit);
    }

    @Override
    public void deleteById(Long id) {
        produitRepository.deleteById(id);
    }
}

