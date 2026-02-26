package com.backend.gesy.camion;

import com.backend.gesy.camion.dto.CamionDTO;
import com.backend.gesy.camion.dto.CamionMapper;
import com.backend.gesy.camion.dto.CamionWithVoyagesCountDTO;
import com.backend.gesy.compte.Compte;
import com.backend.gesy.compte.CompteRepository;
import com.backend.gesy.roles.Roles;
import com.backend.gesy.fournisseur.Fournisseur;
import com.backend.gesy.fournisseur.FournisseurRepository;
import com.backend.gesy.transaction.Transaction;
import com.backend.gesy.transaction.TransactionRepository;
import com.backend.gesy.utilisateur.Utilisateur;
import com.backend.gesy.utilisateur.UtilisateurService;
import com.backend.gesy.voyage.VoyageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CamionServiceImpl implements CamionService {
    @Autowired
    private CamionRepository camionRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private CamionMapper camionMapper;
    @Autowired
    private FournisseurRepository fournisseurRepository;
    @Autowired
    private CompteRepository compteRepository;
    @Autowired
    private UtilisateurService utilisateurService;
    @Autowired
    private VoyageRepository voyageRepository;

    @Override
    public List<CamionDTO> findAll() {
        return camionRepository.findAll().stream()
            .map(camionMapper::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<CamionDTO> findById(Long id) {
        return camionRepository.findById(id)
            .map(camionMapper::toDTO);
    }

    @Override
    public Optional<CamionDTO> findByImmatriculation(String immatriculation) {
        return camionRepository.findByImmatriculation(immatriculation)
            .map(camionMapper::toDTO);
    }

    @Override
    public CamionDTO save(CamionDTO camionDTO) {
        if (camionDTO.getResponsableId() == null || camionDTO.getResponsableId() <= 0) {
            throw new IllegalArgumentException("Le responsable est obligatoire pour l'ajout d'un camion.");
        }
        Compte responsable = compteRepository.findById(camionDTO.getResponsableId())
            .orElseThrow(() -> new IllegalArgumentException("Compte responsable introuvable avec l'id: " + camionDTO.getResponsableId()));
        Camion camion = camionMapper.toEntity(camionDTO);
        camion.setResponsable(responsable);
        Camion savedCamion = camionRepository.save(camion);
        return camionMapper.toDTO(savedCamion);
    }

    @Override
    public CamionDTO update(Long id, CamionDTO camionDTO) {
        Camion existingCamion = camionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Camion non trouvé avec l'id: " + id));

        Utilisateur currentUser = utilisateurService.getCurrentUser()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non connecté"));
        if (!canModifyCamion(currentUser, existingCamion)) {
            throw new AccessDeniedException("Seul le responsable du camion ou un administrateur peut modifier ce camion.");
        }

        Camion camion = camionMapper.toEntity(camionDTO);
        camion.setId(existingCamion.getId());
        if (camion.getResponsable() == null && existingCamion.getResponsable() != null) {
            camion.setResponsable(existingCamion.getResponsable());
        }
        Camion updatedCamion = camionRepository.save(camion);

        return camionMapper.toDTO(updatedCamion);
    }

    @Override
    public void deleteById(Long id) {
        Camion existingCamion = camionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Camion non trouvé avec l'id: " + id));

        Utilisateur currentUser = utilisateurService.getCurrentUser()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non connecté"));
        if (!canModifyCamion(currentUser, existingCamion)) {
            throw new AccessDeniedException("Seul le responsable du camion ou un administrateur peut supprimer ce camion.");
        }

        camionRepository.deleteById(id);
    }

    private boolean canModifyCamion(Utilisateur user, Camion camion) {
        boolean isAdmin = user.getRoles() != null && user.getRoles().stream()
            .anyMatch(r -> r.getStatut() == Roles.StatutRole.ACTIF && "ADMIN".equalsIgnoreCase(r.getNom()));
        boolean isResponsable = camion.getResponsable() != null
            && camion.getResponsable().getId() != null
            && camion.getResponsable().getId().equals(user.getId());
        return isAdmin || isResponsable;
    }

    @Override
    public List<CamionWithVoyagesCountDTO> findByFournisseurId(Long fournisseurId) {
        Fournisseur fournisseur = fournisseurRepository.findById(fournisseurId)
            .orElseThrow(() -> new RuntimeException("Fournisseur non trouvé avec l'id: " + fournisseurId));
        
        List<Camion> camions = camionRepository.findByFournisseur(fournisseur);
        
        return camions.stream()
            .map(camion -> {
                List<com.backend.gesy.voyage.Voyage> voyages = voyageRepository.findByCamion(camion);
                Long nombreVoyages = (long) voyages.size();
                Long nombreVoyagesNonCession = (long) voyages.stream().filter(v -> v != null && !v.isCession()).count();
                
                CamionWithVoyagesCountDTO dto = new CamionWithVoyagesCountDTO();
                dto.setId(camion.getId());
                dto.setImmatriculation(camion.getImmatriculation());
                dto.setModele(camion.getModele());
                dto.setMarque(camion.getMarque());
                dto.setAnnee(camion.getAnnee());
                dto.setType(camion.getType());
                dto.setCapacite(camion.getCapacite());
                dto.setStatut(camion.getStatut() != null ? camion.getStatut().name() : null);
                dto.setNombreVoyages(nombreVoyages);
                dto.setNombreVoyagesNonCession(nombreVoyagesNonCession);
                
                return dto;
            })
            .collect(Collectors.toList());
    }
}

