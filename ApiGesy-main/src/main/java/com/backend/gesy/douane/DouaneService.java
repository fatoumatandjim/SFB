package com.backend.gesy.douane;

import com.backend.gesy.douane.dto.DouaneDTO;

import java.util.Optional;

public interface DouaneService {
    Optional<DouaneDTO> findById(Long id);
    DouaneDTO getDouane();
    DouaneDTO update(DouaneDTO douaneDTO);
}

