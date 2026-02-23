package com.backend.gesy.alerte;

import com.backend.gesy.alerte.dto.AlertePageDTO;

import java.util.List;
import java.util.Optional;

public interface AlerteService {
    List<Alerte> findAll();
    Optional<Alerte> findById(Long id);
    List<Alerte> findByLu(Boolean lu);
    List<Alerte> findByType(Alerte.TypeAlerte type);
    Alerte save(Alerte alerte);
    Alerte update(Long id, Alerte alerte);
    void deleteById(Long id);

    /**
     * Pagination : alertes triées par date décroissante.
     */
    AlertePageDTO findPaginated(int page, int size);

    /**
     * Crée et enregistre une alerte (non lue, date = now).
     */
    Alerte creerAlerte(Alerte.TypeAlerte type, String message, Alerte.PrioriteAlerte priorite,
                       String entiteType, Long entiteId, String lien);
}

