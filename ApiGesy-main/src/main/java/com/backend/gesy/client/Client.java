package com.backend.gesy.client;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clients")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Client {
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

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TypeClient type;

    @Column(name = "code_client", unique = true)
    private String codeClient;

    private String ville;
    private String pays;

    public enum TypeClient {
        PARTICULIER,
        ENTREPRISE,
        GOUVERNEMENT
    }
}
