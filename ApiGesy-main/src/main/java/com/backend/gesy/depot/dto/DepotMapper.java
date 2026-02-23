package com.backend.gesy.depot.dto;

import com.backend.gesy.depot.Depot;
import org.springframework.stereotype.Component;

@Component
public class DepotMapper {
    public DepotDTO toDTO(Depot depot) {
        if (depot == null) {
            return null;
        }
        
        DepotDTO dto = new DepotDTO();
        dto.setId(depot.getId());
        dto.setNom(depot.getNom());
        dto.setAdresse(depot.getAdresse());
        dto.setCapacite(depot.getCapacite());
        dto.setCapaciteUtilisee(depot.getCapaciteUtilisee());
        dto.setStatut(depot.getStatut() != null ? depot.getStatut().name() : null);
        dto.setVille(depot.getVille());
        dto.setPays(depot.getPays());
        dto.setResponsable(depot.getResponsable());
        dto.setTelephone(depot.getTelephone());
        
        return dto;
    }

    public Depot toEntity(DepotDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Depot depot = new Depot();
        depot.setId(dto.getId());
        depot.setNom(dto.getNom());
        depot.setAdresse(dto.getAdresse());
        depot.setCapacite(dto.getCapacite());
        depot.setCapaciteUtilisee(dto.getCapaciteUtilisee());
        if (dto.getStatut() != null) {
            depot.setStatut(Depot.StatutDepot.valueOf(dto.getStatut()));
        }
        depot.setVille(dto.getVille());
        depot.setPays(dto.getPays());
        depot.setResponsable(dto.getResponsable());
        depot.setTelephone(dto.getTelephone());
        
        return depot;
    }
}

