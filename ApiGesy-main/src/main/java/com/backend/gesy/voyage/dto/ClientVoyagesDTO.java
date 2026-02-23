package com.backend.gesy.voyage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientVoyagesDTO {
    private Long clientId;
    private String clientNom;
    private String clientEmail;
    private List<VoyageDTO> voyages;
    private int nombreVoyages;
}
