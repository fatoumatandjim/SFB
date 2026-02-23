package com.backend.gesy.facture.dto;

import com.backend.gesy.facture.Facture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FactureMapper {
    private final LigneFactureMapper ligneFactureMapper;

    public FactureDTO toDTO(Facture facture) {
        if (facture == null) {
            return null;
        }
        
        FactureDTO dto = new FactureDTO();
        dto.setId(facture.getId());
        dto.setNumero(facture.getNumero());
        dto.setDate(facture.getDate());
        dto.setMontant(facture.getMontant());
        dto.setMontantHT(facture.getMontantHT());
        dto.setMontantTTC(facture.getMontantTTC());
        dto.setTauxTVA(facture.getTauxTVA());
        dto.setClientId(facture.getClient() != null ? facture.getClient().getId() : null);
        dto.setClientNom(facture.getClient() != null ? facture.getClient().getNom() : null);
        dto.setClientEmail(facture.getClient() != null ? facture.getClient().getEmail() : null);
        dto.setStatut(facture.getStatut() != null ? facture.getStatut().name() : null);
        dto.setDateEcheance(facture.getDateEcheance());
        dto.setMontantPaye(facture.getMontantPaye());
        dto.setDescription(facture.getDescription());
        dto.setNotes(facture.getNotes());
        
        if (facture.getLignes() != null) {
            dto.setLignes(facture.getLignes().stream()
                .map(ligneFactureMapper::toDTO)
                .collect(Collectors.toList()));
        }
        
        return dto;
    }

    public Facture toEntity(FactureDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Facture facture = new Facture();
        facture.setId(dto.getId());
        facture.setNumero(dto.getNumero());
        facture.setDate(dto.getDate());
        facture.setMontant(dto.getMontant());
        facture.setMontantHT(dto.getMontantHT());
        facture.setMontantTTC(dto.getMontantTTC());
        facture.setTauxTVA(dto.getTauxTVA());
        if (dto.getStatut() != null) {
            facture.setStatut(Facture.StatutFacture.valueOf(dto.getStatut()));
        }
        facture.setDateEcheance(dto.getDateEcheance());
        facture.setMontantPaye(dto.getMontantPaye());
        facture.setDescription(dto.getDescription());
        facture.setNotes(dto.getNotes());
        
        return facture;
    }
}

