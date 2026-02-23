package com.backend.gesy.facture;

import com.backend.gesy.client.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {
    Optional<Facture> findByNumero(String numero);
    
    // Récupérer les factures d'un client triées par date décroissante
    @org.springframework.data.jpa.repository.Query("SELECT f FROM Facture f WHERE f.client = :client ORDER BY f.date DESC, f.id DESC")
    List<Facture> findByClient(@org.springframework.data.repository.query.Param("client") Client client);
    
    // Récupérer toutes les factures triées par date décroissante (pagination)
    @org.springframework.data.jpa.repository.Query("SELECT f FROM Facture f ORDER BY f.date DESC, f.id DESC")
    Page<Facture> findAllOrderByDateDesc(Pageable pageable);
    
    // Récupérer toutes les factures triées par date décroissante (liste complète)
    @org.springframework.data.jpa.repository.Query("SELECT f FROM Facture f ORDER BY f.date DESC, f.id DESC")
    List<Facture> findAllOrderByDateDesc();
    
    // Récupérer les factures non payées (montantPaye < montantTTC) triées par date décroissante
    @org.springframework.data.jpa.repository.Query("SELECT f FROM Facture f WHERE f.montantPaye < f.montantTTC OR f.montantPaye IS NULL ORDER BY f.date DESC, f.id DESC")
    List<Facture> findUnpaidFactures();
    
    // Récupérer les factures en retard (dateEcheance < aujourd'hui et non payées) triées par date décroissante
    @org.springframework.data.jpa.repository.Query("SELECT f FROM Facture f WHERE f.dateEcheance < CURRENT_DATE AND (f.montantPaye < f.montantTTC OR f.montantPaye IS NULL) ORDER BY f.date DESC, f.id DESC")
    List<Facture> findOverdueFactures();

    // Récupérer la facture liée à un voyage et un client (pour mise à jour prix d'achat)
    @org.springframework.data.jpa.repository.Query("SELECT f FROM Facture f WHERE f.voyage.id = :voyageId AND f.client.id = :clientId ORDER BY f.id DESC")
    java.util.List<Facture> findByVoyageIdAndClientId(@org.springframework.data.repository.query.Param("voyageId") Long voyageId, @org.springframework.data.repository.query.Param("clientId") Long clientId);
}

