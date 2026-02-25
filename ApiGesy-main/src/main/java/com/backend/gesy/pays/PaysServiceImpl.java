package com.backend.gesy.pays;

import com.backend.gesy.alerte.Alerte;
import com.backend.gesy.alerte.AlerteRepository;
import com.backend.gesy.pays.dto.HistoriquePaysDTO;
import com.backend.gesy.pays.dto.PaysDTO;
import com.backend.gesy.pays.dto.PaysMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PaysServiceImpl implements PaysService {

    private final PaysRepository paysRepository;
    private final PaysMapper paysMapper;
    private final HistoriquePaysRepository historiquePaysRepository;
    private final AlerteRepository alerteRepository;

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

        boolean fraisChanged = hasFraisChanged(existing, dto);

        if (fraisChanged) {
            String modifiePar = getCurrentUsername();

            HistoriquePays historique = new HistoriquePays();
            historique.setPays(existing);
            historique.setAncienFraisParLitre(existing.getFraisParLitre());
            historique.setNouveauFraisParLitre(dto.getFraisParLitre() != null ? dto.getFraisParLitre() : existing.getFraisParLitre());
            historique.setAncienFraisParLitreGasoil(existing.getFraisParLitreGasoil());
            historique.setNouveauFraisParLitreGasoil(dto.getFraisParLitreGasoil() != null ? dto.getFraisParLitreGasoil() : existing.getFraisParLitreGasoil());
            historique.setAncienFraisT1(existing.getFraisT1());
            historique.setNouveauFraisT1(dto.getFraisT1() != null ? dto.getFraisT1() : existing.getFraisT1());
            historique.setDateModification(LocalDateTime.now());
            historique.setModifiePar(modifiePar);
            historique.setCommentaire(buildCommentaire(existing, dto));
            historiquePaysRepository.save(historique);

            Alerte alerte = new Alerte();
            alerte.setType(Alerte.TypeAlerte.AUTRE);
            alerte.setMessage(String.format("Modification des frais du pays %s par %s. Essence: %s → %s, Gasoil: %s → %s, T1: %s → %s",
                    existing.getNom(), modifiePar,
                    existing.getFraisParLitre(), dto.getFraisParLitre(),
                    existing.getFraisParLitreGasoil(), dto.getFraisParLitreGasoil(),
                    existing.getFraisT1(), dto.getFraisT1()));
            alerte.setDate(LocalDateTime.now());
            alerte.setLu(false);
            alerte.setPriorite(Alerte.PrioriteAlerte.MOYENNE);
            alerte.setEntiteType("Pays");
            alerte.setEntiteId(existing.getId());
            alerteRepository.save(alerte);
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

    @Override
    @Transactional(readOnly = true)
    public List<HistoriquePaysDTO> getHistoriqueByPaysId(Long paysId) {
        return historiquePaysRepository.findByPaysIdOrderByDateModificationDesc(paysId).stream()
                .map(h -> {
                    HistoriquePaysDTO dto = new HistoriquePaysDTO();
                    dto.setId(h.getId());
                    dto.setPaysId(h.getPays().getId());
                    dto.setPaysNom(h.getPays().getNom());
                    dto.setAncienFraisParLitre(h.getAncienFraisParLitre());
                    dto.setNouveauFraisParLitre(h.getNouveauFraisParLitre());
                    dto.setAncienFraisParLitreGasoil(h.getAncienFraisParLitreGasoil());
                    dto.setNouveauFraisParLitreGasoil(h.getNouveauFraisParLitreGasoil());
                    dto.setAncienFraisT1(h.getAncienFraisT1());
                    dto.setNouveauFraisT1(h.getNouveauFraisT1());
                    dto.setDateModification(h.getDateModification().toString());
                    dto.setModifiePar(h.getModifiePar());
                    dto.setCommentaire(h.getCommentaire());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private boolean hasFraisChanged(Pays existing, PaysDTO dto) {
        if (dto.getFraisParLitre() != null && existing.getFraisParLitre().compareTo(dto.getFraisParLitre()) != 0) return true;
        if (dto.getFraisParLitreGasoil() != null && existing.getFraisParLitreGasoil().compareTo(dto.getFraisParLitreGasoil()) != 0) return true;
        if (dto.getFraisT1() != null && existing.getFraisT1().compareTo(dto.getFraisT1()) != 0) return true;
        return false;
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "Système";
    }

    private String buildCommentaire(Pays ancien, PaysDTO nouveau) {
        StringBuilder sb = new StringBuilder();
        if (nouveau.getFraisParLitre() != null && ancien.getFraisParLitre().compareTo(nouveau.getFraisParLitre()) != 0) {
            sb.append(String.format("Essence: %s → %s. ", ancien.getFraisParLitre(), nouveau.getFraisParLitre()));
        }
        if (nouveau.getFraisParLitreGasoil() != null && ancien.getFraisParLitreGasoil().compareTo(nouveau.getFraisParLitreGasoil()) != 0) {
            sb.append(String.format("Gasoil: %s → %s. ", ancien.getFraisParLitreGasoil(), nouveau.getFraisParLitreGasoil()));
        }
        if (nouveau.getFraisT1() != null && ancien.getFraisT1().compareTo(nouveau.getFraisT1()) != 0) {
            sb.append(String.format("T1: %s → %s. ", ancien.getFraisT1(), nouveau.getFraisT1()));
        }
        return sb.toString().trim();
    }
}
