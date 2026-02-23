package com.backend.gesy.produit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {
    Optional<Produit> findByNom(String nom);
    
    @Query("SELECT DISTINCT p FROM Produit p LEFT JOIN FETCH p.stocks s LEFT JOIN FETCH s.depot")
    List<Produit> findAllWithStocks();
}

