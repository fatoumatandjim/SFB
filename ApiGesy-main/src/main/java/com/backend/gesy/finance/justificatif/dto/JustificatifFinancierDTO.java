package com.backend.gesy.finance.justificatif.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JustificatifFinancierDTO {
    private Long id;
    private String ownerType;
    private Long ownerId;
    /** Nom du fichier sur le disque (téléchargement via API). */
    private String storedFileName;
    private String originalFilename;
    private LocalDateTime createdAt;
}
