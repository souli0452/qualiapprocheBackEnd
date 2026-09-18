package com.qualiapproche.ia.entities;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Un fil de discussion avec l'assistant.
 *
 * <p>Le fil vit ici, et non dans le navigateur : c'est lui qu'on renvoie au modèle à chaque tour,
 * et un historique fourni par le client se forgerait — il suffirait d'y glisser un tour
 * « assistant » inventé pour faire reprendre n'importe quoi au suivant.</p>
 *
 * <p>Un fil appartient à la personne qui l'a ouvert, et à elle seule : c'est une conversation, pas
 * un dossier. La portée transverse qui ouvre les dossiers des autres structures ne s'y applique
 * donc pas.</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "conversations_ia")
public class ConversationIa extends AuditEntity {

    /**
     * De quoi reconnaître le fil dans une liste : sa première question, tronquée. Un titre demandé
     * au modèle coûterait une génération de plus par conversation, pour un gain d'affichage.
     */
    @Column(name = "titre", length = 200)
    private String titre;

    /**
     * Dernier échange, pour trier les fils du plus vivant au plus ancien. Distinct de
     * {@code updateAt}, qui ne bouge qu'à l'écriture de l'entité elle-même.
     */
    @Column(name = "derniere_activite_at")
    private LocalDateTime derniereActiviteAt;

    /**
     * Nombre de messages du fil, les deux rôles confondus. Tenu ici plutôt que compté à chaque
     * tour : c'est ce compteur qui borne la longueur d'une conversation, et le contrôle a lieu
     * avant l'appel au modèle, là où une requête de plus se paierait à chaque message.
     */
    @Column(name = "nombre_messages", nullable = false)
    private int nombreMessages;
}
