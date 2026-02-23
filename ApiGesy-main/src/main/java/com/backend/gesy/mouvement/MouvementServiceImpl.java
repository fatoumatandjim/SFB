package com.backend.gesy.mouvement;

import com.backend.gesy.mouvement.dto.MouvementDTO;
import com.backend.gesy.mouvement.dto.MouvementMapper;
import com.backend.gesy.mouvement.dto.MouvementPageDTO;
import com.backend.gesy.stock.Stock;
import com.backend.gesy.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MouvementServiceImpl implements MouvementService {
    private final MouvementRepository mouvementRepository;
    private final StockRepository stockRepository;
    private final MouvementMapper mouvementMapper;

    @Override
    public List<MouvementDTO> findAll() {
        return mouvementRepository.findAll().stream()
            .map(mouvementMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<MouvementDTO> findRecent(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return mouvementRepository.findRecentMouvements(pageable).stream()
            .map(mouvementMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public MouvementPageDTO findAllPaginated(Pageable pageable) {
        Page<Mouvement> page = mouvementRepository.findAllOrderedByDate(pageable);
        List<MouvementDTO> mouvements = page.getContent().stream()
            .map(mouvementMapper::toDTO)
            .collect(Collectors.toList());
        
        MouvementPageDTO dto = new MouvementPageDTO();
        dto.setMouvements(mouvements);
        dto.setCurrentPage(page.getNumber());
        dto.setTotalPages(page.getTotalPages());
        dto.setTotalElements(page.getTotalElements());
        dto.setPageSize(page.getSize());
        return dto;
    }

    @Override
    public MouvementPageDTO findByDate(LocalDate date, Pageable pageable) {
        // Convertir LocalDate en LocalDateTime (début et fin de jour)
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
        Page<Mouvement> page = mouvementRepository.findByDateRange(startOfDay, endOfDay, pageable);
        List<MouvementDTO> mouvements = page.getContent().stream()
            .map(mouvementMapper::toDTO)
            .collect(Collectors.toList());
        
        MouvementPageDTO dto = new MouvementPageDTO();
        dto.setMouvements(mouvements);
        dto.setCurrentPage(page.getNumber());
        dto.setTotalPages(page.getTotalPages());
        dto.setTotalElements(page.getTotalElements());
        dto.setPageSize(page.getSize());
        return dto;
    }

    @Override
    public MouvementPageDTO findByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        // Convertir LocalDate en LocalDateTime (début et fin de jour)
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        Page<Mouvement> page = mouvementRepository.findByDateRange(startDateTime, endDateTime, pageable);
        List<MouvementDTO> mouvements = page.getContent().stream()
            .map(mouvementMapper::toDTO)
            .collect(Collectors.toList());
        
        MouvementPageDTO dto = new MouvementPageDTO();
        dto.setMouvements(mouvements);
        dto.setCurrentPage(page.getNumber());
        dto.setTotalPages(page.getTotalPages());
        dto.setTotalElements(page.getTotalElements());
        dto.setPageSize(page.getSize());
        return dto;
    }

    @Override
    public Optional<MouvementDTO> findById(Long id) {
        return mouvementRepository.findById(id)
            .map(mouvementMapper::toDTO);
    }

    @Override
    public List<MouvementDTO> findByStockId(Long stockId) {
        return mouvementRepository.findByStockId(stockId).stream()
            .map(mouvementMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public MouvementDTO save(MouvementDTO mouvementDTO) {
        Mouvement mouvement = mouvementMapper.toEntity(mouvementDTO);

        // Récupérer le stock
        if (mouvementDTO.getStockId() != null) {
            Stock stock = stockRepository.findById(mouvementDTO.getStockId())
                .orElseThrow(() -> new RuntimeException("Stock non trouvé avec l'id: " + mouvementDTO.getStockId()));
            mouvement.setStock(stock);
        } else {
            throw new RuntimeException("L'ID du stock est requis");
        }

        Mouvement savedMouvement = mouvementRepository.save(mouvement);
        return mouvementMapper.toDTO(savedMouvement);
    }

    @Override
    public MouvementDTO update(Long id, MouvementDTO mouvementDTO) {
        Mouvement existingMouvement = mouvementRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Mouvement non trouvé avec l'id: " + id));
        
        Mouvement mouvement = mouvementMapper.toEntity(mouvementDTO);
        mouvement.setId(existingMouvement.getId());

        // Récupérer le stock
        if (mouvementDTO.getStockId() != null) {
            Stock stock = stockRepository.findById(mouvementDTO.getStockId())
                .orElseThrow(() -> new RuntimeException("Stock non trouvé avec l'id: " + mouvementDTO.getStockId()));
            mouvement.setStock(stock);
        } else {
            mouvement.setStock(existingMouvement.getStock());
        }

        Mouvement updatedMouvement = mouvementRepository.save(mouvement);
        return mouvementMapper.toDTO(updatedMouvement);
    }

    @Override
    public void deleteById(Long id) {
        mouvementRepository.deleteById(id);
    }
}

