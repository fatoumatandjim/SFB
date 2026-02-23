package com.backend.gesy.alerte;

import com.backend.gesy.alerte.dto.AlerteDTO;
import com.backend.gesy.alerte.dto.AlerteMapper;
import com.backend.gesy.alerte.dto.AlertePageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AlerteServiceImpl implements AlerteService {
    private final AlerteRepository alerteRepository;
    private final AlerteMapper alerteMapper;

    @Override
    public List<Alerte> findAll() {
        return alerteRepository.findAll();
    }

    @Override
    public Optional<Alerte> findById(Long id) {
        return alerteRepository.findById(id);
    }

    @Override
    public List<Alerte> findByLu(Boolean lu) {
        return alerteRepository.findByLu(lu);
    }

    @Override
    public List<Alerte> findByType(Alerte.TypeAlerte type) {
        return alerteRepository.findByType(type);
    }

    @Override
    public Alerte save(Alerte alerte) {
        return alerteRepository.save(alerte);
    }

    @Override
    public Alerte update(Long id, Alerte alerte) {
        Alerte existingAlerte = alerteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Alerte non trouvée avec l'id: " + id));
        alerte.setId(existingAlerte.getId());
        return alerteRepository.save(alerte);
    }

    @Override
    public void deleteById(Long id) {
        alerteRepository.deleteById(id);
    }

    @Override
    public AlertePageDTO findPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"));
        Page<Alerte> alertePage = alerteRepository.findAllByOrderByDateDesc(pageable);
        List<AlerteDTO> content = alertePage.getContent().stream()
            .map(alerteMapper::toDTO)
            .collect(Collectors.toList());
        return new AlertePageDTO(
            content,
            alertePage.getNumber(),
            alertePage.getSize(),
            alertePage.getTotalElements(),
            alertePage.getTotalPages()
        );
    }

    @Override
    public Alerte creerAlerte(Alerte.TypeAlerte type, String message, Alerte.PrioriteAlerte priorite,
                              String entiteType, Long entiteId, String lien) {
        Alerte alerte = new Alerte();
        alerte.setType(type);
        alerte.setMessage(message != null && message.length() > 500 ? message.substring(0, 500) : message);
        alerte.setPriorite(priorite != null ? priorite : Alerte.PrioriteAlerte.MOYENNE);
        alerte.setDate(LocalDateTime.now());
        alerte.setLu(false);
        alerte.setEntiteType(entiteType);
        alerte.setEntiteId(entiteId);
        alerte.setLien(lien);
        return alerteRepository.save(alerte);
    }
}

