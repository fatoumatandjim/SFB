package com.backend.gesy.douane;

import com.backend.gesy.alerte.Alerte;
import com.backend.gesy.alerte.AlerteRepository;
import com.backend.gesy.douane.dto.DouaneDTO;
import com.backend.gesy.douane.dto.DouaneMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class DouaneServiceImpl implements DouaneService {
    private final DouaneRepository douaneRepository;
    private final DouaneMapper douaneMapper;
    private final HistoriqueDouaneRepository historiqueDouaneRepository;
    private final AlerteRepository alerteRepository;

    @Override
    public Optional<DouaneDTO> findById(Long id) {
        return douaneRepository.findById(id)
            .map(douaneMapper::toDTO);
    }

    @Override
    public DouaneDTO getDouane() {
        Douane douane = douaneRepository.findFirstByOrderByIdAsc();
        if (douane == null) {
            throw new RuntimeException("Aucune configuration douane trouvée");
        }
        return douaneMapper.toDTO(douane);
    }

    @Override
    public DouaneDTO update(DouaneDTO douaneDTO) {
        Douane existingDouane = douaneRepository.findFirstByOrderByIdAsc();
        if (existingDouane == null) {
            throw new RuntimeException("Aucune configuration douane trouvée");
        }

        // Récupérer l'utilisateur connecté
        String modifiePar = getCurrentUsername();

        // Créer l'historique avant modification
        HistoriqueDouane historique = new HistoriqueDouane();
        historique.setAncienFraisParLitre(existingDouane.getFraisParLitre());
        historique.setNouveauFraisParLitre(douaneDTO.getFraisParLitre());
        historique.setAncienFraisParLitreGasoil(existingDouane.getFraisParLitreGasoil());
        historique.setNouveauFraisParLitreGasoil(douaneDTO.getFraisParLitreGasoil());
        historique.setAncienFraisT1(existingDouane.getFraisT1());
        historique.setNouveauFraisT1(douaneDTO.getFraisT1());
        historique.setDateModification(LocalDateTime.now());
        historique.setModifiePar(modifiePar);
        historique.setCommentaire(buildCommentaire(existingDouane, douaneDTO));
        historiqueDouaneRepository.save(historique);

        // Créer une alerte pour tracer la modification
        Alerte alerte = new Alerte();
        alerte.setType(Alerte.TypeAlerte.AUTRE);
        alerte.setMessage(String.format("Modification des frais de douane par %s. Essence: %s → %s FCFA, Gasoil: %s → %s FCFA, T1: %s → %s FCFA",
            modifiePar,
            existingDouane.getFraisParLitre(), douaneDTO.getFraisParLitre(),
            existingDouane.getFraisParLitreGasoil(), douaneDTO.getFraisParLitreGasoil(),
            existingDouane.getFraisT1(), douaneDTO.getFraisT1()));
        alerte.setDate(LocalDateTime.now());
        alerte.setLu(false);
        alerte.setPriorite(Alerte.PrioriteAlerte.MOYENNE);
        alerte.setEntiteType("Douane");
        alerte.setEntiteId(existingDouane.getId());
        alerteRepository.save(alerte);

        // Mettre à jour les frais
        existingDouane.setFraisParLitre(douaneDTO.getFraisParLitre());
        existingDouane.setFraisParLitreGasoil(douaneDTO.getFraisParLitreGasoil());
        existingDouane.setFraisT1(douaneDTO.getFraisT1());
        
        Douane updatedDouane = douaneRepository.save(existingDouane);
        return douaneMapper.toDTO(updatedDouane);
    }

    public List<HistoriqueDouane> getHistorique() {
        return historiqueDouaneRepository.findAllByOrderByDateModificationDesc();
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "Système";
    }

    private String buildCommentaire(Douane ancien, DouaneDTO nouveau) {
        StringBuilder sb = new StringBuilder();
        if (!ancien.getFraisParLitre().equals(nouveau.getFraisParLitre())) {
            sb.append(String.format("Frais Essence: %s → %s. ", ancien.getFraisParLitre(), nouveau.getFraisParLitre()));
        }
        if (!ancien.getFraisParLitreGasoil().equals(nouveau.getFraisParLitreGasoil())) {
            sb.append(String.format("Frais Gasoil: %s → %s. ", ancien.getFraisParLitreGasoil(), nouveau.getFraisParLitreGasoil()));
        }
        if (!ancien.getFraisT1().equals(nouveau.getFraisT1())) {
            sb.append(String.format("Frais T1: %s → %s. ", ancien.getFraisT1(), nouveau.getFraisT1()));
        }
        return sb.toString().trim();
    }
}

