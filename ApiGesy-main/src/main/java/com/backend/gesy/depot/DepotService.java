package com.backend.gesy.depot;

import com.backend.gesy.depot.dto.DepotDTO;
import java.util.List;
import java.util.Optional;

public interface DepotService {
    List<DepotDTO> findAll();
    Optional<DepotDTO> findById(Long id);
    Optional<DepotDTO> findByNom(String nom);
    DepotDTO save(DepotDTO depotDTO);
    DepotDTO update(Long id, DepotDTO depotDTO);
    void deleteById(Long id);
}

