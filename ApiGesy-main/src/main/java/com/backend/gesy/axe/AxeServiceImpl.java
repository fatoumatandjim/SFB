package com.backend.gesy.axe;

import com.backend.gesy.axe.dto.AxeDTO;
import com.backend.gesy.axe.dto.AxeMapper;
import com.backend.gesy.pays.Pays;
import com.backend.gesy.pays.PaysRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AxeServiceImpl implements AxeService {

    private final AxeRepository axeRepository;
    private final PaysRepository paysRepository;
    private final AxeMapper axeMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AxeDTO> findAll() {
        return axeRepository.findAll().stream()
                .map(axeMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AxeDTO> findById(Long id) {
        return axeRepository.findById(id).map(axeMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AxeDTO> findByNom(String nom) {
        return axeRepository.findByNom(nom).map(axeMapper::toDTO);
    }

    @Override
    public AxeDTO save(AxeDTO dto) {
        if (dto.getNom() == null || dto.getNom().trim().isEmpty()) {
            throw new RuntimeException("Le nom de l'axe est obligatoire");
        }
        if (axeRepository.existsByNom(dto.getNom().trim())) {
            throw new RuntimeException("Un axe avec ce nom existe déjà");
        }

        Axe entity = new Axe();
        entity.setNom(dto.getNom().trim());
        if (dto.getPaysId() != null) {
            Pays pays = paysRepository.findById(dto.getPaysId())
                    .orElseThrow(() -> new RuntimeException("Pays non trouvé avec l'id: " + dto.getPaysId()));
            entity.setPays(pays);
        }
        return axeMapper.toDTO(axeRepository.save(entity));
    }

    @Override
    public AxeDTO update(Long id, AxeDTO dto) {
        Axe existing = axeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Axe non trouvé avec l'id: " + id));

        if (dto.getNom() == null || dto.getNom().trim().isEmpty()) {
            throw new RuntimeException("Le nom de l'axe est obligatoire");
        }
        if (!existing.getNom().equals(dto.getNom().trim()) && axeRepository.existsByNom(dto.getNom().trim())) {
            throw new RuntimeException("Un axe avec ce nom existe déjà");
        }

        existing.setNom(dto.getNom().trim());
        if (dto.getPaysId() != null) {
            Pays pays = paysRepository.findById(dto.getPaysId())
                    .orElseThrow(() -> new RuntimeException("Pays non trouvé avec l'id: " + dto.getPaysId()));
            existing.setPays(pays);
        } else {
            existing.setPays(null);
        }

        return axeMapper.toDTO(axeRepository.save(existing));
    }

    @Override
    public void deleteById(Long id) {
        if (!axeRepository.existsById(id)) {
            throw new RuntimeException("Axe non trouvé avec l'id: " + id);
        }
        axeRepository.deleteById(id);
    }
}
