package com.backend.gesy.pays;

import com.backend.gesy.pays.dto.PaysDTO;

import java.util.List;
import java.util.Optional;

public interface PaysService {
    List<PaysDTO> findAll();
    Optional<PaysDTO> findById(Long id);
    Optional<PaysDTO> findByNom(String nom);
    PaysDTO save(PaysDTO dto);
    PaysDTO update(Long id, PaysDTO dto);
    void deleteById(Long id);
}
