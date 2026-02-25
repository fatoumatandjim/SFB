package com.backend.gesy.pays;

import com.backend.gesy.pays.dto.PaysDTO;
import com.backend.gesy.pays.dto.PaysMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PaysServiceImpl implements PaysService {

    private final PaysRepository paysRepository;
    private final PaysMapper paysMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PaysDTO> findAll() {
        return paysRepository.findAll().stream()
                .map(paysMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaysDTO> findById(Long id) {
        return paysRepository.findById(id).map(paysMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaysDTO> findByNom(String nom) {
        return paysRepository.findByNom(nom).map(paysMapper::toDTO);
    }

    @Override
    public PaysDTO save(PaysDTO dto) {
        if (dto.getNom() == null || dto.getNom().trim().isEmpty()) {
            throw new RuntimeException("Le nom du pays est obligatoire");
        }
        if (paysRepository.existsByNom(dto.getNom().trim())) {
            throw new RuntimeException("Un pays avec ce nom existe déjà");
        }
        Pays entity = new Pays();
        entity.setNom(dto.getNom().trim());
        entity.setFraisParLitre(dto.getFraisParLitre() != null ? dto.getFraisParLitre() : BigDecimal.ZERO);
        entity.setFraisParLitreGasoil(dto.getFraisParLitreGasoil() != null ? dto.getFraisParLitreGasoil() : BigDecimal.ZERO);
        entity.setFraisT1(dto.getFraisT1() != null ? dto.getFraisT1() : BigDecimal.ZERO);
        return paysMapper.toDTO(paysRepository.save(entity));
    }

    @Override
    public PaysDTO update(Long id, PaysDTO dto) {
        Pays existing = paysRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pays non trouvé avec l'id: " + id));

        if (dto.getNom() != null && !dto.getNom().trim().isEmpty()) {
            if (!existing.getNom().equals(dto.getNom().trim()) && paysRepository.existsByNom(dto.getNom().trim())) {
                throw new RuntimeException("Un pays avec ce nom existe déjà");
            }
            existing.setNom(dto.getNom().trim());
        }
        if (dto.getFraisParLitre() != null) existing.setFraisParLitre(dto.getFraisParLitre());
        if (dto.getFraisParLitreGasoil() != null) existing.setFraisParLitreGasoil(dto.getFraisParLitreGasoil());
        if (dto.getFraisT1() != null) existing.setFraisT1(dto.getFraisT1());

        return paysMapper.toDTO(paysRepository.save(existing));
    }

    @Override
    public void deleteById(Long id) {
        if (!paysRepository.existsById(id)) {
            throw new RuntimeException("Pays non trouvé avec l'id: " + id);
        }
        paysRepository.deleteById(id);
    }
}
