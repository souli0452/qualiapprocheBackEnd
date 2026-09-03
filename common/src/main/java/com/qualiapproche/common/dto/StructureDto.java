
package com.qualiapproche.common.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;






import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Unité de l'organisation : la direction, faîtière, et les services qui lui "
        + "sont rattachés. C'est à elle que tout dossier est rapporté — non-conformité, document, "
        + "plan d'action — et c'est par elle que se décide qui le voit et qui en décide.")
public class StructureDto extends AuditEntityDto {
  @Schema(description = "Forme abrégée, employée là où la place manque : en-têtes de tableau, "
          + "listes de choix, cartouches de document.",
          example = "DRH")
  private String libelleCourt;

  @Schema(description = "Dénomination complète, telle qu'elle figure sur les documents édités.",
          example = "Direction des ressources humaines")
  private String libelleLong;

  @Schema(description = "Ce dont la structure a la charge. Utile là où deux intitulés se "
          + "ressemblent et où le seul nom ne dit pas laquelle choisir.",
          example = "Recrutement, gestion des carrières et formation du personnel.")
  private String description;

  @Schema(description = "Direction de rattachement. Renseignée pour un service ; vide pour la "
          + "direction elle-même, qui n'a personne au-dessus d'elle.",
          example = "5e0ca370-03a6-4465-a446-1d22ed758fe2")
  private UUID directionId;

  @Schema(description = "Dénomination de la direction de rattachement, jointe pour éviter un "
          + "second appel au seul titre de l'affichage.",
          accessMode = Schema.AccessMode.READ_ONLY,
          example = "Direction générale")
  private String libelleDirection;

  @Schema(description = "Type de processus dont la structure relève. La typologie appartient à "
          + "l'organisation : elle se paramètre.",
          example = "b7c1a2d4-9f30-4a55-8e21-6c0f4d3b7a19")
  private UUID typeProcessusId;

  @Schema(description = "Intitulé du type de processus, joint pour l'affichage.",
          accessMode = Schema.AccessMode.READ_ONLY,
          example = "Processus de réalisation")
  private String typeProcessusLibelle;

  @Schema(description = "Rang de la structure. L'installation ne compte qu'une direction : c'est "
          + "elle qui porte la licence, et les services lui sont rattachés.",
          example = "SERVICE",
          allowableValues = {"DIRECTION", "SERVICE"})
  private String typeStructure;

  @Schema(description = "Région administrative d'implantation.", example = "Centre")
  private String region;

  @Schema(description = "Adresse de la structure, distincte de celle de ses agents : elle reçoit "
          + "ce qui s'adresse au service et non à une personne.",
          example = "drh@exemple.bf")
  private String email;

  @Schema(description = "Réservé. Le référentiel ne tient aucun type comptable : le champ revient "
          + "vide, et ce qui y est envoyé n'est pas conservé.")
  private UUID typeStructureComptableId;

  @Schema(description = "Réservé, comme l'identifiant qui le précède.")
  private String typeStructureComptableLibelle;

  @Schema(description = "Ville où siège la structure.", example = "Ouagadougou")
  private String ville;

  @Schema(description = "Fonction de l'autorité signataire. Avec les trois mentions qui suivent, "
          + "elle compose le cartouche de signature des documents édités ; aucun traitement du "
          + "serveur ne s'en sert.",
          example = "Directeur général")
  private String titreAutoriteSignataire;

  @Schema(description = "Nom de la personne qui engage la structure par sa signature.",
          example = "Salif Ouédraogo")
  private String autoriteSignataire;

  @Schema(description = "Civilité ou titre d'usage précédant le nom du signataire.",
          example = "Monsieur")
  private String titreHonorifiqueSignataire;

  @Schema(description = "Mention portée sous la signature quand la fonction de l'autorité ne "
          + "suffit pas à la dire. Renseigner l'une, l'autre ou les deux selon ce que le document "
          + "doit porter.",
          example = "Le Directeur général")
  private String titreSignataire;

  @Schema(description = "Premier jour de validité de la licence installée. Renseigné sur la seule "
          + "direction, et lu de la licence elle-même : une structure obtenue par la liste ou par "
          + "son identifiant revient sans ce champ.")
  private java.time.LocalDateTime dateDebutLicence;

  @Schema(description = "Dernier jour de validité de la licence installée. Même réserve : la "
          + "direction seule le porte.")
  private java.time.LocalDateTime dateFinLicence;

  @Schema(description = "Les écritures sont-elles ouvertes. Faux hors licence valide ; la "
          + "consultation, elle, ne se ferme jamais.",
          example = "true")
  private Boolean licenceActive;

  @Schema(description = "Jours restant avant le terme de la licence. Négatif une fois le terme "
          + "passé ; zéro quand aucune licence n'est posée, ce qui n'est pas la même chose qu'une "
          + "licence qui expire aujourd'hui.",
          example = "42")
  private Long licenseDaysRemaining;

  @Schema(description = "Modules que la licence ouvre. Un module qui n'y figure pas reste fermé, "
          + "fût-il installé et l'utilisateur habilité.",
          example = "[\"NON_CONFORMITE\", \"DOCUMENTAIRE\"]")
  private java.util.List<String> modulesSubscribed;
}
