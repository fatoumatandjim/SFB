package com.backend.gesy.voyage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EtatVoyageDTO {
    private Long id;
    private String etat;
    private LocalDateTime dateHeure;
    private Boolean valider;
}

