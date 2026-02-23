package com.backend.gesy.fournisseur;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "fournisseurs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Fournisseur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String telephone;

    @Column(nullable = false)
    private String adresse;

    @Column(name = "code_fournisseur", unique = true)
    private String codeFournisseur;

    private String ville;
    private String pays;
    private String contactPersonne;

    @Column(name = "type_fournisseur", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private TypeFournisseur typeFournisseur;

    public enum TypeFournisseur {
        ACHAT,
        TRANSPORT
    }
}
