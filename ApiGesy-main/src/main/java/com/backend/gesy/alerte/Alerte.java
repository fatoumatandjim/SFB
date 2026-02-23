package com.backend.gesy.alerte;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "alertes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alerte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TypeAlerte type;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(nullable = false)
    private Boolean lu;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PrioriteAlerte priorite;

    private String lien;
    private String entiteType; // Type d'entité concernée (Facture, Stock, etc.)
    private Long entiteId; // ID de l'entité concernée

    public enum TypeAlerte {
        STOCK_FAIBLE,
        FACTURE_EN_RETARD,
        PAIEMENT_RECU,
        VOYAGE_EN_COURS,
        VOYAGE_CREE,
        VOYAGE_LIVRE,
        CLIENT_ATTRIBUE,
        CLIENT_LIVRE,
        VOYAGE_LIBERE,
        VOYAGE_DECLARE,
        ACHAT_ENREGISTRE,
        MANQUANT_DECLARE,
        FACTURE_EMISE,
        MAINTENANCE_CAMION,
        AUTRE
    }

    public enum PrioriteAlerte {
        BASSE,
        MOYENNE,
        HAUTE,
        URGENTE
    }
}
