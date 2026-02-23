package com.backend.gesy.mouvement;

import com.backend.gesy.mouvement.dto.MouvementDTO;
import com.backend.gesy.mouvement.dto.MouvementPageDTO;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MouvementService {
    List<MouvementDTO> findAll();
    List<MouvementDTO> findRecent(int limit);
    MouvementPageDTO findAllPaginated(Pageable pageable);
    MouvementPageDTO findByDate(LocalDate date, Pageable pageable);
    MouvementPageDTO findByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable);
    Optional<MouvementDTO> findById(Long id);
    List<MouvementDTO> findByStockId(Long stockId);
    MouvementDTO save(MouvementDTO mouvementDTO);
    MouvementDTO update(Long id, MouvementDTO mouvementDTO);
    void deleteById(Long id);
}

