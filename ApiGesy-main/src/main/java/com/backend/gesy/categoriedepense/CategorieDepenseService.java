package com.backend.gesy.categoriedepense;

import com.backend.gesy.categoriedepense.dto.CategorieDepenseDTO;

import java.util.List;
import java.util.Optional;

public interface CategorieDepenseService {
    List<CategorieDepenseDTO> findAll();
    List<CategorieDepenseDTO> findAllActives();
    Optional<CategorieDepenseDTO> findById(Long id);
    CategorieDepenseDTO save(CategorieDepenseDTO dto);
    CategorieDepenseDTO update(Long id, CategorieDepenseDTO dto);
    void deleteById(Long id);
}

