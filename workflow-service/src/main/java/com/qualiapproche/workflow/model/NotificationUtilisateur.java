package com.qualiapproche.workflow.model;

import java.time.LocalDateTime;

import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.common.enumeration.GraviteNotification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Une ligne déposée dans la boîte d'une personne : quelque chose s'est passé, et elle doit le voir.
 *
 * <p>À ne pas confondre avec la <b>cloche</b> servie par {@code NotificationsDeLUtilisateurService},
 * qui répond à une autre question. La cloche dit « qu'est-ce qui m'attend maintenant » : elle se
 * recalcule à chaque appel, ne conserve rien, et ne peut donc pas se tromper. Cette table-ci dit
 * « qu'est-ce qui s'est passé » : elle conserve, elle s'accuse en lecture, et elle raconte une
 * histoire que la cloche est incapable de raconter — qui a imputé quoi, quand, à qui.</p>
 *
 * <p><b>Les deux ne se remplacent pas, et l'ordre entre elles compte.</b> Un journal persisté ment
 * dès qu'une écriture est manquée : une insertion échouée, et la personne n'apprend jamais qu'elle
 * doit agir. La cloche reste donc la source de vérité de ce qu'il y a à faire ; celle-ci l'accompagne
 * sans jamais la remplacer. C'est aussi pourquoi rien, ici, ne conditionne une habilitation.</p>
 *
 * <p><b>La clé d'unicité est ce qui rend la table tenable.</b> Une relance quotidienne redéposerait
 * sinon la même échéance tous les matins, et la boîte serait inutilisable au bout d'une semaine.
 * Le producteur fournit une clé stable — {@code CODE:ressource} pour une échéance,
 * {@code CODE:ressource:étape} pour un franchissement — et un second dépôt sur la même clé met la
 * ligne à jour au lieu d'en créer une.</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "notification_utilisateur",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_destinataire_cle",
                columnNames = {"destinataire_id", "cle_unicite"}),
        indexes = {
                // La requête de tous les écrans : ma boîte, les non-lues d'abord.
                @Index(name = "idx_notification_destinataire_lue",
                        columnList = "destinataire_id, lue"),
                // La reprise d'un dossier : retrouver ce qui a été dit à son sujet.
                @Index(name = "idx_notification_ressource", columnList = "resource_id")
        })
public class NotificationUtilisateur extends AuditEntity {

    /** À qui la ligne s'adresse : l'identifiant Keycloak, jamais un rôle. */
    @Column(name = "destinataire_id", nullable = false, length = 100)
    private String destinataireId;

    /**
     * Ce qui distingue cette ligne d'une autre chez la même personne.
     *
     * <p>Choisie par le producteur, et c'est lui qui décide de ce qui se confond : une échéance
     * dépassée est une seule ligne quel que soit le nombre de relances, un franchissement en est
     * une par étape atteinte.</p>
     */
    @Column(name = "cle_unicite", nullable = false, length = 200)
    private String cleUnicite;

    /** Repère stable de la nature de la ligne, sur lequel l'écran branche son icône et sa destination. */
    @Column(nullable = false, length = 80)
    private String code;

    /** Le domaine d'où la ligne vient, pour regrouper l'affichage. */
    @Column(nullable = false, length = 40)
    private String source;

    @Column(nullable = false, length = 200)
    private String titre;

    /**
     * La phrase à afficher telle quelle.
     *
     * <p>Composée par le producteur, pas par l'écran : c'est le serveur qui connaît le nombre de
     * dossiers et qui accorde le français.</p>
     */
    @Column(nullable = false, length = 1000)
    private String message;

    /** Le dossier concerné, pour que l'écran sache où emmener. */
    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "resource_type", length = 40)
    private String resourceType;

    /** Où mène la ligne quand on la clique. */
    @Column(length = 500)
    private String lien;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GraviteNotification gravite;

    @Column(nullable = false)
    @lombok.Builder.Default
    private boolean lue = false;

    @Column(name = "lue_le")
    private LocalDateTime lueLe;

    /**
     * Marque la ligne comme lue, une seule fois.
     *
     * <p>Le second appel ne réécrit pas l'horodatage : c'est la première lecture qui compte, et
     * un rafraîchissement d'écran ne doit pas la déplacer.</p>
     */
    public void marquerLue() {
        if (!this.lue) {
            this.lue = true;
            this.lueLe = LocalDateTime.now();
        }
    }
}
