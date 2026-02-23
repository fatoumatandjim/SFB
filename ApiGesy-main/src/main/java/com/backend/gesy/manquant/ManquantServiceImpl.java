package com.backend.gesy.manquant;

import com.backend.gesy.alerte.Alerte;
import com.backend.gesy.alerte.AlerteService;
import com.backend.gesy.manquant.dto.ManquantDTO;
import com.backend.gesy.manquant.dto.ManquantMapper;
import com.backend.gesy.manquant.dto.ManquantPageDTO;
import com.backend.gesy.voyage.Voyage;
import com.backend.gesy.voyage.VoyageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
public class ManquantServiceImpl implements ManquantService {
    
    private final ManquantRepository manquantRepository;
    private final VoyageRepository voyageRepository;
    private final ManquantMapper manquantMapper;
    private final AlerteService alerteService;

    @Override
    public ManquantDTO save(ManquantDTO dto) {
        Manquant entity = manquantMapper.toEntity(dto);
        entity.setDateCreation(LocalDateTime.now());
        entity.setCreePar(getCurrentUsername());
        
        if (dto.getVoyageId() != null) {
            Voyage voyage = voyageRepository.findById(dto.getVoyageId())
                    .orElseThrow(() -> new RuntimeException("Voyage non trouvé avec l'id: " + dto.getVoyageId()));
            entity.setVoyage(voyage);
        } else {
            throw new RuntimeException("Un voyage doit être associé au manquant");
        }

        Manquant saved = manquantRepository.save(entity);
        Voyage voyage = entity.getVoyage();
        alerteService.creerAlerte(Alerte.TypeAlerte.MANQUANT_DECLARE,
                "Manquant déclaré : " + (saved.getQuantite() != null ? saved.getQuantite() : "") + " - Voyage #" + (voyage != null && voyage.getNumeroVoyage() != null ? voyage.getNumeroVoyage() : dto.getVoyageId()),
                Alerte.PrioriteAlerte.HAUTE, "Manquant", saved.getId(),
                dto.getVoyageId() != null ? "/voyages/" + dto.getVoyageId() : null);
        return manquantMapper.toDTO(saved);
    }

    @Override
    public Optional<ManquantDTO> findById(Long id) {
        return manquantRepository.findById(id).map(manquantMapper::toDTO);
    }

    @Override
    public void deleteById(Long id) {
        if (!manquantRepository.existsById(id)) {
            throw new RuntimeException("Manquant non trouvé avec l'id: " + id);
        }
        manquantRepository.deleteById(id);
    }

    @Override
    public List<ManquantDTO> findAll() {
        return manquantRepository.findAllByOrderByDateCreationDesc().stream()
                .map(manquantMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ManquantDTO> findByVoyageId(Long voyageId) {
        return manquantRepository.findByVoyageIdOrderByDateCreationDesc(voyageId).stream()
                .map(manquantMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ManquantPageDTO findAllPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Manquant> manquantPage = manquantRepository.findAllByOrderByDateCreationDesc(pageable);
        return toPageDTO(manquantPage);
    }

    @Override
    public ManquantPageDTO findByDate(LocalDate date, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime dateTime = date.atStartOfDay();
        Page<Manquant> manquantPage = manquantRepository.findByDate(dateTime, pageable);
        return toPageDTO(manquantPage);
    }

    @Override
    public ManquantPageDTO findByDateRange(LocalDate startDate, LocalDate endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        Page<Manquant> manquantPage = manquantRepository.findByDateRange(start, end, pageable);
        return toPageDTO(manquantPage);
    }

    private ManquantPageDTO toPageDTO(Page<Manquant> manquantPage) {
        List<ManquantDTO> manquants = manquantPage.getContent().stream()
                .map(manquantMapper::toDTO)
                .collect(Collectors.toList());
        
        return new ManquantPageDTO(
                manquants,
                manquantPage.getNumber(),
                manquantPage.getSize(),
                manquantPage.getTotalElements(),
                manquantPage.getTotalPages()
        );
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "Système";
    }
}

