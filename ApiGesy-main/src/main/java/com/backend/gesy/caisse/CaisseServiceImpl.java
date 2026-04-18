package com.backend.gesy.caisse;

import com.backend.gesy.caisse.dto.CaisseDTO;
import com.backend.gesy.caisse.dto.CaisseMapper;
import com.backend.gesy.compte.Compte;
import com.backend.gesy.compte.CompteRepository;
import com.backend.gesy.finance.FinanceEntityAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CaisseServiceImpl implements CaisseService {
    private final CaisseRepository caisseRepository;
    private final CaisseMapper caisseMapper;
    private final CompteRepository compteRepository;
    private final FinanceEntityAccessService financeEntityAccessService;

    private void applyResponsables(Caisse caisse, List<Long> responsableIds) {
        if (caisse.getResponsables() == null) {
            caisse.setResponsables(new HashSet<>());
        }
        caisse.getResponsables().clear();
        if (responsableIds == null) {
            return;
        }
        for (Long cid : responsableIds) {
            if (cid == null) {
                continue;
            }
            Compte compte = compteRepository.findById(cid)
                    .orElseThrow(() -> new RuntimeException("Compte responsable non trouvé avec l'id: " + cid));
            caisse.getResponsables().add(compte);
        }
    }

    @Override
    public List<CaisseDTO> findAll() {
        List<Caisse> list = caisseRepository.findAll();
        if (!financeEntityAccessService.isCurrentUserAdmin()) {
            Optional<Long> uid = financeEntityAccessService.getCurrentCompteId();
            if (uid.isPresent()) {
                Long u = uid.get();
                list = list.stream()
                        .filter(c -> c.getResponsables() == null || c.getResponsables().isEmpty()
                                || c.getResponsables().stream().anyMatch(r -> r.getId() != null && r.getId().equals(u)))
                        .collect(Collectors.toList());
            }
        }
        return list.stream()
            .map(caisseMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<CaisseDTO> findById(Long id) {
        Optional<Caisse> opt = caisseRepository.findById(id);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        financeEntityAccessService.assertCanManageCaisse(opt.get());
        return Optional.of(caisseMapper.toDTO(opt.get()));
    }

    @Override
    public Optional<CaisseDTO> findByNom(String nom) {
        return caisseRepository.findByNom(nom)
            .map(caisseMapper::toDTO);
    }

    @Override
    public CaisseDTO save(CaisseDTO caisseDTO) {
        Caisse caisse = caisseMapper.toEntity(caisseDTO);
        applyResponsables(caisse, caisseDTO.getResponsableIds());
        Caisse savedCaisse = caisseRepository.save(caisse);
        return caisseMapper.toDTO(savedCaisse);
    }

    @Override
    public CaisseDTO update(Long id, CaisseDTO caisseDTO) {
        Caisse existingCaisse = caisseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Caisse non trouvée avec l'id: " + id));
        financeEntityAccessService.assertCanManageCaisse(existingCaisse);
        Caisse caisse = caisseMapper.toEntity(caisseDTO);
        caisse.setId(existingCaisse.getId());
        if (caisseDTO.getResponsableIds() != null) {
            applyResponsables(caisse, caisseDTO.getResponsableIds());
        } else {
            caisse.setResponsables(new HashSet<>(existingCaisse.getResponsables()));
        }
        Caisse updatedCaisse = caisseRepository.save(caisse);
        return caisseMapper.toDTO(updatedCaisse);
    }

    @Override
    public void deleteById(Long id) {
        Caisse existing = caisseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Caisse non trouvée avec l'id: " + id));
        financeEntityAccessService.assertCanManageCaisse(existing);
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

