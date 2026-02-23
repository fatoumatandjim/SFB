package com.backend.gesy.depense;

import com.backend.gesy.depense.dto.DepenseDTO;
import com.backend.gesy.depense.dto.DepensePageDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DepenseService {
    
    // CRUD
    DepenseDTO save(DepenseDTO dto);
    DepenseDTO update(Long id, DepenseDTO dto);
    Optional<DepenseDTO> findById(Long id);
    void deleteById(Long id);
    
    // Liste sans pagination
    List<DepenseDTO> findAll();
    List<DepenseDTO> findByCategorie(Long categorieId);
    
    // Liste avec pagination
    DepensePageDTO findAllPaginated(int page, int size);
    DepensePageDTO findByCategoriePaginated(Long categorieId, int page, int size);
    
    // Filtres par date avec pagination
    DepensePageDTO findByDate(LocalDate date, int page, int size);
    DepensePageDTO findByDateRange(LocalDate startDate, LocalDate endDate, int page, int size);
    
    // Filtres par catégorie et date avec pagination
    DepensePageDTO findByCategorieAndDate(Long categorieId, LocalDate date, int page, int size);
    DepensePageDTO findByCategorieAndDateRange(Long categorieId, LocalDate startDate, LocalDate endDate, int page, int size);
    
    // Statistiques
    BigDecimal sumByCategorie(Long categorieId);
    BigDecimal sumByDateRange(LocalDate startDate, LocalDate endDate);
}

