package com.backend.gesy.client;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByEmail(String email);
    Optional<Client> findByCodeClient(String codeClient);

    /** Clients ayant au moins un achat (pour voyage de type cession) */
    @Query("SELECT DISTINCT c FROM Client c WHERE c IN (SELECT a.client FROM Achat a WHERE a.client IS NOT NULL)")
    List<Client> findClientsWithAtLeastOneAchat();
}

