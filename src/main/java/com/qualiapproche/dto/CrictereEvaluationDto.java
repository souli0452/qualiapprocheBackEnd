package com.qualiapproche.dto;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.qualiapproche.entities.Evaluation;
import com.qualiapproche.entities.Fournisseur;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@MappedSuperclass
@SuperBuilder
public class CrictereEvaluationDto extends AuditEntityDto{

    private String libelleCrictereEvaluation;
    private String descriptionCrictereEvaluation;
    private String noteAtribuerCritere;
    private String delaisLivraison;
    private String serviceClient;
    private String commentaireEvaluation;

    @JoinColumn(name = "fournisseur_id")
   // @JsonBackReference
    private UUID fournisseurId;



    public CrictereEvaluationDto(UUID id, String libelleCrictereEvaluation, String descriptionCrictereEvaluation, String noteAtribuerCritere, String serviceClient, String commentaireEvaluation, String delaisLivraison, UUID fournisseurId) {
       // this.id = id;
        this.libelleCrictereEvaluation = libelleCrictereEvaluation;
        this.descriptionCrictereEvaluation = descriptionCrictereEvaluation;
        this.noteAtribuerCritere = noteAtribuerCritere;
        this.serviceClient = serviceClient;
        this.commentaireEvaluation = commentaireEvaluation;
        this.delaisLivraison = delaisLivraison;
        this.fournisseurId = fournisseurId;
    }


}
