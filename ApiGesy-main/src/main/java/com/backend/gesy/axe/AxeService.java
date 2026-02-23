package com.backend.gesy.axe;

import com.backend.gesy.axe.dto.AxeDTO;

import java.util.List;
import java.util.Optional;

public interface AxeService {
    List<AxeDTO> findAll();
    Optional<AxeDTO> findById(Long id);
    Optional<AxeDTO> findByNom(String nom);
    AxeDTO save(AxeDTO dto);
    AxeDTO update(Long id, AxeDTO dto);
    void deleteById(Long id);
}

