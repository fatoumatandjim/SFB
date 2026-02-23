package com.backend.gesy.transitaire;

import com.backend.gesy.compte.Compte;
import com.backend.gesy.voyage.Voyage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.proxy.HibernateProxy;

import java.util.List;
import java.util.Objects;

@Entity
@DiscriminatorValue("TRANSITAIRE")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transitaire extends Compte {
    @Column(nullable = false)
    private String nom;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String telephone;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatutTransitaire statut;

    @OneToMany(mappedBy = "transitaire", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Voyage> voyages;

    public enum StatutTransitaire {
        ACTIF,
        INACTIF,
        SUSPENDU
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Transitaire that = (Transitaire) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
