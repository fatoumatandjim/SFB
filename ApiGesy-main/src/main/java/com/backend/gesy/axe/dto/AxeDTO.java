package com.backend.gesy.axe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AxeDTO {
    private Long id;
    private String nom;
    private Long paysId;
    private String paysNom;
}
