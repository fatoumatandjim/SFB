package com.backend.gesy.caisse;

import com.backend.gesy.caisse.dto.CaisseDTO;

import java.util.List;
import java.util.Optional;

public interface CaisseService {
    List<CaisseDTO> findAll();
    Optional<CaisseDTO> findById(Long id);
    Optional<CaisseDTO> findByNom(String nom);
    CaisseDTO save(CaisseDTO caisseDTO);
    CaisseDTO update(Long id, CaisseDTO caisseDTO);
    void deleteById(Long id);
    void initializeDefaultCaisse();
}

