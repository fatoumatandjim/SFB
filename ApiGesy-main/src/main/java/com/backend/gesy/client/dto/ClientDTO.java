package com.backend.gesy.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientDTO {
    private Long id;
    private String nom;
    private String email;
    private String telephone;
    private String adresse;
    private String type;
    private String codeClient;
    private String ville;
    private String pays;
}

