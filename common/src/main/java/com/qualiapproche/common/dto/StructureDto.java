
package com.qualiapproche.common.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;






import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StructureDto extends AuditEntityDto {
  private String libelleCourt;
  private String libelleLong;
  private String description;
  private UUID directionId;
  private String libelleDirection;
  private UUID categorieProcessusId;
  private String categorieProcessusLibelle;

  private String typeStructure;

  private String region;
  private String email;
  private UUID typeStructureComptableId;
  private String typeStructureComptableLibelle;
  private String ville;
  private String titreAutoriteSignataire;
  private String autoriteSignataire;
  private String titreHonorifiqueSignataire;
  private String titreSignataire;
  private java.time.LocalDateTime dateDebutLicence;
  private java.time.LocalDateTime dateFinLicence;
  private Boolean licenceActive;
  private Long licenseDaysRemaining;
  private java.util.List<String> modulesSubscribed;
}
