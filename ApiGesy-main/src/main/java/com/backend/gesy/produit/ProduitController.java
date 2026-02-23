package com.backend.gesy.produit;

import com.backend.gesy.produit.dto.ProduitDTO;
import com.backend.gesy.produit.dto.ProduitAvecStocksDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produits")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProduitController {
    private final ProduitService produitService;

    @GetMapping
    public ResponseEntity<List<ProduitDTO>> getAllProduits() {
        return ResponseEntity.ok(produitService.findAll());
    }

    @GetMapping("/avec-stocks")
    public ResponseEntity<List<ProduitAvecStocksDTO>> getAllProduitsAvecStocks() {
        return ResponseEntity.ok(produitService.findAllAvecStocks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProduitDTO> getProduitById(@PathVariable Long id) {
        return produitService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/avec-stocks")
    public ResponseEntity<ProduitAvecStocksDTO> getProduitByIdAvecStocks(@PathVariable Long id) {
        return produitService.findByIdAvecStocks(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nom/{nom}")
    public ResponseEntity<ProduitDTO> getProduitByNom(@PathVariable String nom) {
        return produitService.findByNom(nom)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ProduitDTO> createProduit(@RequestBody ProduitDTO produitDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produitService.save(produitDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProduitDTO> updateProduit(@PathVariable Long id, @RequestBody ProduitDTO produitDTO) {
        return ResponseEntity.ok(produitService.update(id, produitDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduit(@PathVariable Long id) {
        produitService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

