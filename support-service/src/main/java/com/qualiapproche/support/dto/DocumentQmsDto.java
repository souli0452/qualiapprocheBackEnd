package com.qualiapproche.support.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;
import com.qualiapproche.common.dto.WorkflowStateDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Document du système de management de la qualité, tel que le rendent les "
        + "écrans. Le fichier lui-même n'y figure pas : le document porte l'historique de ses "
        + "versions, dont seule la dernière est décrite ici.")
public class DocumentQmsDto {

    @Schema(description = "Identifiant technique du document, stable d'une version à l'autre.")
    private UUID id;

    @Schema(description = "Numéro attribué par le système à l'enregistrement, unique et non "
            + "modifiable. À distinguer de la référence, que l'auteur saisit lui-même.",
            example = "PRO-DSI-2024-001")
    private String documentNumber;

    @Schema(description = "Intitulé du document, librement saisi.",
            example = "Procédure de maîtrise des enregistrements")
    private String titre;

    @Schema(description = "Code du type au référentiel des types documentaires : procédure, "
            + "instruction, formulaire. C'est lui qui désigne le circuit de validation à suivre.",
            example = "PRO")
    private String documentType;

    @Schema(description = "Code du document dans la convention de numérotation propre à "
            + "l'organisation, saisi par l'auteur. Sans garantie d'unicité, à la différence du "
            + "numéro attribué par le système.",
            example = "QSE/PR/012")
    private String reference;

    @Schema(description = "Objet du document, en quelques lignes.")
    private String description;

    @Schema(description = "Structure propriétaire du document. Elle borne qui le voit, qui décide "
            + "de ses étapes et qui reçoit ses courriels.")
    private String serviceId;

    @Schema(description = "Libellé de cette structure, pour l'affichage seul.",
            example = "Direction des systèmes d'information")
    private String serviceLibelle;

    @Schema(description = "Sigle de cette structure, repris dans les entêtes et les numéros.",
            example = "DSI")
    private String serviceSigle;

    @Schema(description = "Qui a rédigé le document, tel que déclaré au dépôt. Renseignement "
            + "déclaratif : il ne confère aucun droit et ne commande aucune étape.")
    private String redacteur;

    @Schema(description = "Le circuit de validation est allé à son terme : le document est en "
            + "vigueur. C'est ce que traduit le statut VALIDE des statistiques.",
            example = "true")
    private boolean esTraiter;

    @Schema(description = "La date de prochaine révision est dépassée. Calculé par la surveillance "
            + "périodique et non à la lecture : un document peut donc être en retard sans que ce "
            + "drapeau l'ait encore constaté.",
            example = "false")
    private boolean enRetardRevision;

    @Schema(description = "Le document a été remplacé ou retiré : il reste consultable pour "
            + "l'archive, mais ne fait plus foi.",
            example = "false")
    private boolean obsolete;

    /** Rang de révision, à partir de zéro : v0 au dépôt, v1 après la première révision acceptée. */
    @Schema(description = "Rang de révision, à partir de zéro : v0 au dépôt, v1 après la première "
            + "révision acceptée. Seule une demande de modification aboutie le fait monter — ni un "
            + "fichier corrigé pendant le circuit initial, ni une reprise après rejet.",
            example = "2")
    private int numeroVersion;

    @Schema(description = "Date à laquelle le document est entré en application. Elle n'est posée "
            + "qu'au terme du circuit, et sert de point de départ au calcul de la révision.")
    private LocalDateTime dateVigueur;

    @Schema(description = "Échéance de la revue périodique, déduite de la mise en vigueur et de la "
            + "périodicité. C'est elle qui déclenche les alertes de révision.")
    private LocalDateTime dateProchRevision;

    @Schema(description = "Intervalle entre deux revues, en mois. Vide, le document n'est soumis à "
            + "aucune révision périodique et ne sera jamais signalé en retard.",
            example = "24")
    private Integer periodiciteMois;

    @Schema(description = "Déduit du niveau de confidentialité et non saisi : vrai dès qu'un niveau "
            + "est retenu. C'est le niveau, et non ce drapeau, qui dit quels rôles ont accès.",
            example = "false")
    private boolean confidentiel;

    @Schema(description = "Le document vient de l'extérieur — norme, texte réglementaire, document "
            + "client. Il n'est alors pas rédigé en interne, seulement maîtrisé, ce qui donne leur "
            + "sens à la référence officielle, à la date de publication et au statut légal.",
            example = "false")
    private boolean documentExterne;

    @Schema(description = "Structure à laquelle le document s'adresse, lorsqu'elle diffère de la "
            + "structure propriétaire.")
    private String processusDestId;

    @Schema(description = "Libellé de cette structure destinataire, pour l'affichage seul.")
    private String processusDestLibelle;

    @Schema(description = "Référence de l'émetteur pour un document externe : le numéro que porte "
            + "la norme ou le texte d'origine.",
            example = "ISO 9001:2015")
    private String referenceOfficielle;

    @Schema(description = "Date de parution du document externe chez son émetteur, à ne pas "
            + "confondre avec sa mise en vigueur dans l'organisation.")
    private LocalDateTime datePublication;

    @Schema(description = "Libellé du domaine d'application, tel qu'affiché. Il reprend celui du "
            + "domaine choisi au référentiel ; les valeurs anciennes, saisies en clair, y "
            + "subsistent.",
            example = "Ressources humaines")
    private String domaine;

    @Schema(description = "Portée juridique d'un document externe, pour distinguer ce qui "
            + "s'impose de ce qui oriente.",
            example = "OBLIGATOIRE")
    private String statutLegal;

    @Schema(description = "Non-conformité à l'origine du document, lorsqu'il naît d'une action "
            + "corrective. Simple renvoi, par le numéro de la non-conformité.")
    private String ncReference;

    @Schema(description = "Étape où le circuit de validation est arrêté. Vide, le document est un "
            + "brouillon qui n'a pas encore été soumis.",
            example = "VERIFICATION")
    private String currentEtape;

    @Schema(description = "Circuit suivi par le document. Conservé sur le document lui-même, ce "
            + "qui permet d'afficher les étapes restantes sans interroger le moteur.")
    private UUID workflowId;

    @Schema(description = "Référence, dans le serveur de fichiers, du fichier de la dernière "
            + "version déposée. C'est elle qu'il faut citer pour le télécharger.",
            example = "documents/DSI/PRO-DSI-2024-001-v2.pdf")
    private String currentObjectName;

    @Schema(description = "Empreinte du fichier de la dernière version. Elle permet de vérifier "
            + "qu'un fichier téléchargé est bien celui qui a été validé.")
    private String currentFileHash;

    @Schema(description = "Date du dépôt initial du document. Elle ne bouge plus ensuite, les "
            + "révisions étant datées par leurs versions.")
    private LocalDateTime createdAt;

    @Schema(description = "Identifiant Keycloak de qui a déposé le document.")
    private String createdById;

    @Schema(description = "Nom de qui a déposé le document, relevé au dépôt et figé depuis : il "
            + "reste lisible même si le compte est renommé ou supprimé.")
    private String currentUserfullName;
    // `reference` figure déjà plus haut : c'est le code saisi par l'auteur, selon la convention de
    // numérotation de l'organisation. Il était porté par le DTO sans qu'aucun écran ne l'offre.

    @Schema(description = "Priorité, par son identifiant au référentiel paramétrable servi par "
            + "referentiel-service.")
    private String prioriteId;

    @Schema(description = "Libellé de cette priorité, recopié pour éviter un appel au référentiel "
            + "à chaque affichage.",
            example = "Haute")
    private String prioriteLibelle;

    @Schema(description = "Niveau de confidentialité, par son identifiant au référentiel. Il porte "
            + "la liste des rôles admis à consulter ; cette restriction s'ajoute à celle de la "
            + "structure, elle ne s'y substitue pas.")
    private String niveauConfidentialiteId;

    @Schema(description = "Libellé de ce niveau, recopié pour l'affichage.",
            example = "Diffusion restreinte")
    private String niveauConfidentialiteLibelle;

    @Schema(description = "Domaine d'application, par son identifiant au référentiel. Il a remplacé "
            + "la saisie libre, où « RH » et « Ressources Humaines » comptaient pour deux domaines "
            + "dans les statistiques.")
    private String domaineId;

    @Schema(description = "État du circuit : étape courante, décisions ouvertes à l'appelant, "
            + "champs à renseigner. Renseigné là où l'écran propose d'agir, et nul ailleurs.")
    private WorkflowStateDto workflowState;

    /**
     * Vrai si l'appelant relève de la structure du document (ou l'accompagne au titre de la
     * qualité), faux s'il n'y accède que par un partage.
     *
     * <p>Décidé par le serveur, qui seul connaît la structure de chacun, et transmis pour que
     * l'écran ne propose pas un historique, une piste d'audit ou une traçabilité que le serveur
     * refuserait — un bouton qui mène à un refus est pire que pas de bouton.</p>
     */
    @Schema(description = "L'appelant relève de la structure du document, ou l'accompagne au titre "
            + "de la qualité ; faux s'il n'y accède que par un partage. Décidé par le serveur, et "
            + "transmis pour que l'écran ne propose pas un historique ou une piste d'audit qui "
            + "serait ensuite refusée.",
            example = "true")
    private boolean suiviInterneAutorise;

    /**
     * Avertissement sur le classement, adressé à qui vient de déposer ou de reclasser.
     *
     * <p>Renseigné lorsque le niveau retenu n'admet aucun des rôles qui décident des étapes du
     * circuit : le document est alors enregistré, mais aucun de ses décideurs ne le verra. Le
     * dépôt n'est pas refusé pour autant — le circuit peut être remanié, ou le niveau changé —
     * mais le silence, ici, se paierait d'un document immobile que personne ne saurait
     * expliquer.</p>
     */
    @Schema(description = "Avertissement adressé à qui vient de déposer ou de reclasser, lorsque "
            + "le niveau de confidentialité retenu n'admet aucun des rôles qui décident des étapes "
            + "du circuit. Le dépôt aboutit tout de même, mais le document n'avancerait plus faute "
            + "de décideur pour le voir.")
    private String avertissementConfidentialite;
}
