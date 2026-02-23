package com.backend.gesy.camion.dto;

import com.backend.gesy.camion.Camion;
import com.backend.gesy.compte.CompteRepository;
import com.backend.gesy.fournisseur.FournisseurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CamionMapper {
    private final FournisseurRepository fournisseurRepository;
    private final CompteRepository compteRepository;

    public CamionDTO toDTO(Camion camion) {
        if (camion == null) {
            return null;
        }
        
        CamionDTO dto = new CamionDTO();
        dto.setId(camion.getId());
        dto.setImmatriculation(camion.getImmatriculation());
//        dto.setModele(camion.getModele());
//        dto.setMarque(camion.getMarque());
//        dto.setAnnee(camion.getAnnee());
        dto.setType(camion.getType());
        dto.setCapacite(camion.getCapacite());
        dto.setDernierControle(camion.getDernierControle());
        dto.setStatut(camion.getStatut() != null ? camion.getStatut().name() : null);
        dto.setFournisseurId(camion.getFournisseur() != null ? camion.getFournisseur().getId() : null);
        dto.setFournisseurNom(camion.getFournisseur() != null ? camion.getFournisseur().getNom() : null);
        dto.setFournisseurEmail(camion.getFournisseur() != null ? camion.getFournisseur().getEmail() : null);
        dto.setResponsableId(camion.getResponsable() != null ? camion.getResponsable().getId() : null);
        dto.setResponsableIdentifiant(camion.getResponsable() != null ? camion.getResponsable().getIdentifiant() : null);

        return dto;
    }

    public Camion toEntity(CamionDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Camion camion = new Camion();
        camion.setId(dto.getId());
        camion.setImmatriculation(dto.getImmatriculation());
//        camion.setModele(dto.getModele());
//        camion.setMarque(dto.getMarque());
//        camion.setAnnee(dto.getAnnee());
        // Type par défaut: CITERNE
        camion.setType(dto.getType() != null && !dto.getType().isEmpty() ? dto.getType() : "CITERNE");
        camion.setCapacite(dto.getCapacite());
        camion.setDernierControle(dto.getDernierControle());
        // Statut par défaut: DISPONIBLE
        if (dto.getStatut() != null && !dto.getStatut().isEmpty()) {
            camion.setStatut(Camion.StatutCamion.valueOf(dto.getStatut()));
        } else {
            camion.setStatut(Camion.StatutCamion.DISPONIBLE);
        }
        
        // Définir le fournisseur si fourni
        if (dto.getFournisseurId() != null && dto.getFournisseurId() > 0) {
            fournisseurRepository.findById(dto.getFournisseurId())
                .ifPresent(camion::setFournisseur);
        }

        // Définir le responsable si fourni
        if (dto.getResponsableId() != null && dto.getResponsableId() > 0) {
            compteRepository.findById(dto.getResponsableId())
                .ifPresent(camion::setResponsable);
        }

        return camion;
    }
}

