package com.backend.gesy.stock;

import com.backend.gesy.depot.Depot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    List<Stock> findByDepotId(Long depotId);
    List<Stock> findByProduitId(Long produitId);
    Optional<Stock> findByDepotIdAndProduitId(Long depotId, Long produitId);
   Optional<Stock> findByProduitIdAndCiterne(Long produitId, boolean citerne);
   
   @Query("SELECT s FROM Stock s WHERE s.depot IS NULL OR s.depot.statut = :statut")
   List<Stock> findByDepotStatut(@Param("statut") Depot.StatutDepot statut);
}

