package com.backend.gesy.douane;

import com.backend.gesy.douane.dto.FraisDouaneAxeDTO;

import java.util.List;
import java.util.Optional;

public interface FraisDouaneAxeService {
    List<FraisDouaneAxeDTO> findAll();
    Optional<FraisDouaneAxeDTO> findByAxeId(Long axeId);
    Optional<FraisDouaneAxeDTO> findById(Long id);
    FraisDouaneAxeDTO save(FraisDouaneAxeDTO dto);
    FraisDouaneAxeDTO update(Long id, FraisDouaneAxeDTO dto);
    void deleteById(Long id);
}
