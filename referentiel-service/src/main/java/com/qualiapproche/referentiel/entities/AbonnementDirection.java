package com.qualiapproche.referentiel.entities;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "abonnements_directions")
public class AbonnementDirection extends AuditEntity {

    @OneToOne
    @JoinColumn(name = "subscribed_direction_id")
    private Structure direction;

    @Column(name = "license", length = 3000)
    private String license; // Encrypted modules list

    private boolean active = true;

    private java.time.LocalDateTime dateDebut;
    private java.time.LocalDateTime dateFin;
}
