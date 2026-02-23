package com.backend.gesy.alerte.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlerteDTO {
    private Long id;
    private String type;
    private String message;
    private LocalDateTime date;
    private Boolean lu;
    private String priorite;
    private String lien;
    private String entiteType;
    private Long entiteId;
}

