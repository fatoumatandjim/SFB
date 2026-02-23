package com.backend.gesy.abonnement;

import java.util.List;
import java.util.Optional;

public interface AbonnementService {
    List<Abonnement> findAll();
    Optional<Abonnement> findById(Long id);
    List<Abonnement> findByActif(Boolean actif);
    Abonnement save(Abonnement abonnement);
    Abonnement update(Long id, Abonnement abonnement);
    void deleteById(Long id);
}

