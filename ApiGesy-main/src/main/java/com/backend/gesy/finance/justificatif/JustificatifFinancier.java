package com.backend.gesy.finance.justificatif;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Pièce jointe optionnelle liée à une dépense, un paiement ou une transaction (facture, reçu, etc.).
 */
@Entity
@Table(name = "justificatifs_financiers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JustificatifFinancier {

    public static final String OWNER_DEPENSE = "DEPENSE";
    public static final String OWNER_PAIEMENT = "PAIEMENT";
    public static final String OWNER_TRANSACTION = "TRANSACTION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_type", nullable = false, length = 32)
    private String ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "stored_file_name", nullable = false, length = 512)
    private String storedFileName;

    @Column(name = "original_filename", nullable = false, length = 512)
    private String originalFilename;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
