
package com.qualiapproche.common.dto;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;


 



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
  private UUID typeProcessusId;
  private String typeProcessusLibelle;
  
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
