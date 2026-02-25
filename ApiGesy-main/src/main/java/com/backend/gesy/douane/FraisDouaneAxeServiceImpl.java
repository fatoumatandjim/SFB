package com.backend.gesy.douane;

import com.backend.gesy.axe.Axe;
import com.backend.gesy.axe.AxeRepository;
import com.backend.gesy.douane.dto.CreateFraisDouaneAxeWithNewAxeDTO;
import com.backend.gesy.douane.dto.FraisDouaneAxeDTO;
import com.backend.gesy.douane.dto.FraisDouaneAxeMapper;
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
public class FraisDouaneAxeServiceImpl implements FraisDouaneAxeService {
    private final FraisDouaneAxeRepository fraisDouaneAxeRepository;
    private final AxeRepository axeRepository;
    private final FraisDouaneAxeMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<FraisDouaneAxeDTO> findAll() {
        return fraisDouaneAxeRepository.findAll().stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FraisDouaneAxeDTO> findByAxeId(Long axeId) {
        return fraisDouaneAxeRepository.findByAxeId(axeId).map(mapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FraisDouaneAxeDTO> findById(Long id) {
        return fraisDouaneAxeRepository.findById(id).map(mapper::toDTO);
    }

    @Override
    public FraisDouaneAxeDTO save(FraisDouaneAxeDTO dto) {
        if (dto.getAxeId() == null) {
            throw new RuntimeException("L'axe est obligatoire");
        }
        if (fraisDouaneAxeRepository.existsByAxeId(dto.getAxeId())) {
            throw new RuntimeException("Des frais de douane existent déjà pour cet axe");
        }
        Axe axe = axeRepository.findById(dto.getAxeId())
                .orElseThrow(() -> new RuntimeException("Axe non trouvé avec l'id: " + dto.getAxeId()));
        FraisDouaneAxe entity = new FraisDouaneAxe();
        entity.setAxe(axe);
        entity.setFraisParLitre(dto.getFraisParLitre() != null ? dto.getFraisParLitre() : java.math.BigDecimal.ZERO);
        entity.setFraisParLitreGasoil(dto.getFraisParLitreGasoil() != null ? dto.getFraisParLitreGasoil() : java.math.BigDecimal.ZERO);
        entity.setFraisT1(dto.getFraisT1() != null ? dto.getFraisT1() : java.math.BigDecimal.ZERO);
        return mapper.toDTO(fraisDouaneAxeRepository.save(entity));
    }

    @Override
    public FraisDouaneAxeDTO saveWithNewAxe(CreateFraisDouaneAxeWithNewAxeDTO dto) {
        if (dto.getNomAxe() == null || dto.getNomAxe().trim().isEmpty()) {
            throw new RuntimeException("Le nom de l'axe est obligatoire");
        }
        String nomTrimmed = dto.getNomAxe().trim();
        if (axeRepository.existsByNom(nomTrimmed)) {
            throw new RuntimeException("Un axe avec ce nom existe déjà. Veuillez le sélectionner dans la liste.");
        }
        Axe axe = new Axe();
        axe.setNom(nomTrimmed);
        axe = axeRepository.save(axe);
        FraisDouaneAxe frais = new FraisDouaneAxe();
        frais.setAxe(axe);
        frais.setFraisParLitre(dto.getFraisParLitre() != null ? dto.getFraisParLitre() : BigDecimal.ZERO);
        frais.setFraisParLitreGasoil(dto.getFraisParLitreGasoil() != null ? dto.getFraisParLitreGasoil() : BigDecimal.ZERO);
        frais.setFraisT1(dto.getFraisT1() != null ? dto.getFraisT1() : BigDecimal.ZERO);
        return mapper.toDTO(fraisDouaneAxeRepository.save(frais));
    }

    @Override
    public FraisDouaneAxeDTO update(Long id, FraisDouaneAxeDTO dto) {
        FraisDouaneAxe existing = fraisDouaneAxeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Frais douane axe non trouvé avec l'id: " + id));
        existing.setFraisParLitre(dto.getFraisParLitre() != null ? dto.getFraisParLitre() : existing.getFraisParLitre());
        existing.setFraisParLitreGasoil(dto.getFraisParLitreGasoil() != null ? dto.getFraisParLitreGasoil() : existing.getFraisParLitreGasoil());
        existing.setFraisT1(dto.getFraisT1() != null ? dto.getFraisT1() : existing.getFraisT1());
        return mapper.toDTO(fraisDouaneAxeRepository.save(existing));
    }

    @Override
    public void deleteById(Long id) {
        fraisDouaneAxeRepository.deleteById(id);
    }
}
