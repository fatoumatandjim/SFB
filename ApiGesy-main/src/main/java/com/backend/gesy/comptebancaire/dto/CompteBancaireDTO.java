package com.backend.gesy.comptebancaire.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.backend.gesy.compte.ResponsableRefDTO;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompteBancaireDTO {
    private Long id;
    private String numero;
    private String type;
    private BigDecimal solde;
    private String banque;
    private String numeroCompteBancaire;
    private String statut;
    private String description;
    /** Comptes utilisateurs responsables de ce compte bancaire / mobile money. */
    private List<Long> responsableIds;
    /** Détail affichable (id + nom) des responsables. */
    private List<ResponsableRefDTO> responsables;
}

