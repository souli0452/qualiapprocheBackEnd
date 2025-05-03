package com.qualiapproche.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.qualiapproche.entities.*;
import com.qualiapproche.enumeration.Etat;
import com.qualiapproche.enumeration.Status;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import lombok.experimental.SuperBuilder;
import org.springframework.transaction.annotation.Transactional;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@MappedSuperclass
@SuperBuilder
@Transactional
public class NonConformiteDto extends AuditEntityDto {

    private String numeroReference;
    private String nomProcessus;
    private String origineService;
    private String fonctionEmetteur;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime dateVisaEmetteur;
    private String justification;
    private String delaisMiseOeuvre;
    private Etat etatTraitement;
    private Status status;
    private String observationsRq;
    private String dateObservationsRq;
    private String observationsCloture;
    private String dateVerification;
    private String dispositionPreventives;
    private String dateClotureRq;
    // On stocke uniquement les ID des objets associés
    private UUID efficaciteId;
    private UUID niveauNonConformiteId;
    private UUID actionId;
    private UUID typeNonConformiteId;
    private UUID typeProcessusId;
    private List<Fichier> fichiers;
    private List<PlanActionDto> planActions;
}
