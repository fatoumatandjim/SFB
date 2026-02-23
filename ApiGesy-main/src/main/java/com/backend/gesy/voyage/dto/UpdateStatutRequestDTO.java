package com.backend.gesy.voyage.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateStatutRequestDTO {
    private String statut;
    private List<ClientQuantiteDTO> clients; // Pour le statut LIVRE : liste de clients avec quantités
    private Map<Long, Double> manquants; // Pour le statut DECHARGER : Map<clientVoyageId, manquant>
}
