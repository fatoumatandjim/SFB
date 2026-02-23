package com.backend.gesy.camion;

import com.backend.gesy.camion.dto.CamionDTO;
import com.backend.gesy.camion.dto.CamionWithVoyagesCountDTO;

import java.util.List;
import java.util.Optional;

public interface CamionService {
    List<CamionDTO> findAll();
    Optional<CamionDTO> findById(Long id);
    Optional<CamionDTO> findByImmatriculation(String immatriculation);
    CamionDTO save(CamionDTO camionDTO);
    CamionDTO update(Long id, CamionDTO camionDTO);
    void deleteById(Long id);
    List<CamionWithVoyagesCountDTO> findByFournisseurId(Long fournisseurId);
}

