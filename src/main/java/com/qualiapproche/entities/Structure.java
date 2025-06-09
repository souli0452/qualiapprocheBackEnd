
package com.qualiapproche.entities;
import com.qualiapproche.enumeration.TypeStructure;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ref_structures")
public class Structure extends AuditEntity {

  @Column(unique = true)
  private String libelleCourt;

  @Column(unique = true)
  private String libelleLong;

  private String description;
  private String region;
  private String email;
  private String ville;

  private String titreAutoriteSignataire;
  private String autoriteSignataire;
  private String titreHonorifiqueSignataire;
  private String titreSignataire;

  @OneToMany(mappedBy = "direction", fetch = FetchType.LAZY)
  private List<Structure> services;

  @ManyToOne
  private Structure direction;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TypeStructure typeStructure;

}
