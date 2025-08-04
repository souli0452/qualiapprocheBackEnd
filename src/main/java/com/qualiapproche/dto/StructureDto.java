
package com.qualiapproche.dto;
import com.qualiapproche.enumeration.TypeStructure;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StructureDto extends AuditEntityDto {
  private String libelleCourt;
  private String libelleLong;
  private String description;
  private UUID directionId;
  private String libelleDirection;
  @Enumerated(EnumType.STRING)
  private TypeStructure typeStructure;

  private String region;
  private String email;
  private UUID typeStructureComptableId;
  private String typeStructureComptableLibelle;
  private String ville;
  private String titreAutoriteSignataire;
  private String autoriteSignataire;
  private String titreHonorifiqueSignataire;
  private String titreSignataire;
}
