package com.backend.gesy.mouvement;

import com.backend.gesy.stock.Stock;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "mouvements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Mouvement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TypeMouvement typeMouvement;

    @Column(nullable = false)
    private Double quantite;

    private String unite;

    private String description;

    @Column(name = "date_mouvement", nullable = false)
    private LocalDateTime dateMouvement = LocalDateTime.now();

    public enum TypeMouvement {
        ENTREE,
        SORTIE,
        TRANSFERT,
        INVENTAIRE
    }
}

