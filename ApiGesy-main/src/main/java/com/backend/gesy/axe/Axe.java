package com.backend.gesy.axe;

import com.backend.gesy.pays.Pays;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "axes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Axe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pays_id")
    private Pays pays;
}
