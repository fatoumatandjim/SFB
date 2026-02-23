package com.backend.gesy.manquant;

import com.backend.gesy.manquant.dto.ManquantDTO;
import com.backend.gesy.manquant.dto.ManquantPageDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ManquantService {
    
    // CRUD
    ManquantDTO save(ManquantDTO dto);
    Optional<ManquantDTO> findById(Long id);
    void deleteById(Long id);
    
    // Liste sans pagination
    List<ManquantDTO> findAll();
    List<ManquantDTO> findByVoyageId(Long voyageId);
    
    // Liste avec pagination
    ManquantPageDTO findAllPaginated(int page, int size);
    
    // Filtres par date avec pagination
    ManquantPageDTO findByDate(LocalDate date, int page, int size);
    ManquantPageDTO findByDateRange(LocalDate startDate, LocalDate endDate, int page, int size);
}

