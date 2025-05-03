
package com.qualiapproche.entities;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.qualiapproche.enumeration.Etat;
import com.qualiapproche.enumeration.Status;
import jakarta.persistence.*;
import lombok.*;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@SuperBuilder
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")

public class NonConformite extends AuditEntity {

    // Les propriétés qui interviennent dans la première partie de soumission d'une non conformité
    private String numeroReference;
    // private String intitule;
    private String nomProcessus;
    // Il s'agit là de l'origine de la non conformité (par exemple: Une non conformité au niveau du Contrôle qualité)
    private String origineService;
    // Il s'agit de la fonction de celui qui emet la non conformité (par exemple: Un inspecteur qualité)
    private String fonctionEmetteur;
    // Il s'agit de la date d'émission de la non conformité (Récupérée automatiquement)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime dateVisaEmetteur;
    // Il s'agit d'une description de la non conformité
    private String justification;
    // On stocke uniquement les ID des objets associés
    private UUID efficaciteId;
    // Mineur ou Majeur
    private UUID niveauNonConformiteId;
    // AC - AP - Correction
    private UUID actionId;
    // Reclamation client / fournisseur - Produit / Service - Système
    private UUID typeNonConformiteId;
    // Réalisation - Support - Management
    private UUID typeProcessusId;

    // Il s'agit du total des dates d'échéance pour les différents plans d'action
    private String delaisMiseOeuvre;
    private Etat etatTraitement;
    private String observationsRq;
    private String dateObservationsRq;
    private String observationsCloture;
    private String dateVerification;
    private String dispositionPreventives;
    private String dateClotureRq;
    private Status status;

    @OneToMany
    private List<Fichier> fichiers;
    //@OneToMany(mappedBy = "nonConformite", cascade = CascadeType.ALL, orphanRemoval = true)
    //private List<PlanAction> planActions;
    @OneToMany(mappedBy = "nonConformite", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanAction> planActions = new ArrayList<>();
}
