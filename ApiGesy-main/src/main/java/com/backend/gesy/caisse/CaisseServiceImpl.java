package com.backend.gesy.caisse;

import com.backend.gesy.caisse.dto.CaisseDTO;
import com.backend.gesy.caisse.dto.CaisseMapper;
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
public class CaisseServiceImpl implements CaisseService {
    private final CaisseRepository caisseRepository;
    private final CaisseMapper caisseMapper;

    @Override
    public List<CaisseDTO> findAll() {
        return caisseRepository.findAll().stream()
            .map(caisseMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<CaisseDTO> findById(Long id) {
        return caisseRepository.findById(id)
            .map(caisseMapper::toDTO);
    }

    @Override
    public Optional<CaisseDTO> findByNom(String nom) {
        return caisseRepository.findByNom(nom)
            .map(caisseMapper::toDTO);
    }

    @Override
    public CaisseDTO save(CaisseDTO caisseDTO) {
        Caisse caisse = caisseMapper.toEntity(caisseDTO);
        Caisse savedCaisse = caisseRepository.save(caisse);
        return caisseMapper.toDTO(savedCaisse);
    }

    @Override
    public CaisseDTO update(Long id, CaisseDTO caisseDTO) {
        Caisse existingCaisse = caisseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Caisse non trouvée avec l'id: " + id));
        Caisse caisse = caisseMapper.toEntity(caisseDTO);
        caisse.setId(existingCaisse.getId());
        Caisse updatedCaisse = caisseRepository.save(caisse);
        return caisseMapper.toDTO(updatedCaisse);
    }

    @Override
    public void deleteById(Long id) {
        caisseRepository.deleteById(id);
    }

    @Override
    public void initializeDefaultCaisse() {
        // Vérifier si la caisse par défaut existe déjà
        if (!caisseRepository.existsByNom("Caisse Principale")) {
            Caisse caisse = new Caisse();
            caisse.setNom("Caisse Principale");
            caisse.setSolde(BigDecimal.ZERO);
            caisse.setStatut(Caisse.StatutCaisse.ACTIF);
            caisse.setDescription("Caisse principale de l'entreprise");
            caisseRepository.save(caisse);
        }
    }
}

