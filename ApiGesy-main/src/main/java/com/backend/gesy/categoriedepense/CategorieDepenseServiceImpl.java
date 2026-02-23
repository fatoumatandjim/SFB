package com.backend.gesy.categoriedepense;

import com.backend.gesy.categoriedepense.dto.CategorieDepenseDTO;
import com.backend.gesy.categoriedepense.dto.CategorieDepenseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategorieDepenseServiceImpl implements CategorieDepenseService {
    
    private final CategorieDepenseRepository categorieDepenseRepository;
    private final CategorieDepenseMapper categorieDepenseMapper;

    @Override
    public List<CategorieDepenseDTO> findAll() {
        return categorieDepenseRepository.findAll().stream()
                .map(categorieDepenseMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategorieDepenseDTO> findAllActives() {
        return categorieDepenseRepository.findByStatut(CategorieDepense.StatutCategorie.ACTIF).stream()
                .map(categorieDepenseMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<CategorieDepenseDTO> findById(Long id) {
        return categorieDepenseRepository.findById(id)
                .map(categorieDepenseMapper::toDTO);
    }

    @Override
    public CategorieDepenseDTO save(CategorieDepenseDTO dto) {
        if (categorieDepenseRepository.existsByNom(dto.getNom())) {
            throw new RuntimeException("Une catégorie avec ce nom existe déjà");
        }
        CategorieDepense entity = categorieDepenseMapper.toEntity(dto);
        if (entity.getStatut() == null) {
            entity.setStatut(CategorieDepense.StatutCategorie.ACTIF);
        }
        return categorieDepenseMapper.toDTO(categorieDepenseRepository.save(entity));
    }

    @Override
    public CategorieDepenseDTO update(Long id, CategorieDepenseDTO dto) {
        CategorieDepense existing = categorieDepenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec l'id: " + id));
        
        // Vérifier si le nouveau nom n'est pas déjà utilisé par une autre catégorie
        if (!existing.getNom().equals(dto.getNom()) && categorieDepenseRepository.existsByNom(dto.getNom())) {
            throw new RuntimeException("Une catégorie avec ce nom existe déjà");
        }
        
        existing.setNom(dto.getNom());
        existing.setDescription(dto.getDescription());
        if (dto.getStatut() != null) {
            existing.setStatut(CategorieDepense.StatutCategorie.valueOf(dto.getStatut()));
        }
        
        return categorieDepenseMapper.toDTO(categorieDepenseRepository.save(existing));
    }

    @Override
    public void deleteById(Long id) {
        if (!categorieDepenseRepository.existsById(id)) {
            throw new RuntimeException("Catégorie non trouvée avec l'id: " + id);
        }
        categorieDepenseRepository.deleteById(id);
    }
}

