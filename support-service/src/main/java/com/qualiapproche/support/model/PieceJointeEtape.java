package com.qualiapproche.support.model;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Pièce déposée pour satisfaire un champ d'étape, sur un document ou une demande.
 *
 * <p>Le serveur de fichiers ne conserve pas le nom d'origine : sa clé d'objet ne reprend que
 * l'extension, deux fichiers homonymes s'y seraient écrasés. Or c'est le nom qui identifie une pièce
 * pour qui la relit — « attestation-formation-2026.pdf » ne se remplace pas par
 * « 9f1c2d3e-….pdf » dans un dossier d'audit. Cette table fait la correspondance.</p>
 *
 * <p>Elle porte aussi ce que la clé ne dit pas : le type déclaré, la taille, et le dossier auquel la
 * pièce appartient — ce dernier faisant autorité pour le téléchargement. Le rangement dans le
 * serveur de fichiers reste le second rempart, pour les pièces déposées avant cette table.</p>
 */
@Entity
@Table(name = "qms_pieces_etape",
        indexes = @Index(name = "idx_piece_etape_dossier", columnList = "famille,dossier_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PieceJointeEtape extends AuditEntity {

    /**
     * Référence de l'objet dans le serveur de fichiers : l'identité de la pièce.
     *
     * <p>C'est cette valeur que le moteur de workflow transporte comme valeur du champ d'étape, et
     * c'est par elle que la pièce est relue. Unique, donc : deux lignes pour une même référence
     * rendraient le nom d'origine indéterminé.</p>
     */
    @Column(nullable = false, unique = true, length = 512)
    private String reference;

    /** Famille du dossier : {@code documents} ou {@code demandes}. */
    @Column(nullable = false, length = 30)
    private String famille;

    /** Dossier auquel la pièce appartient — il fait autorité pour autoriser la relecture. */
    @Column(name = "dossier_id", nullable = false)
    private UUID dossierId;

    /** Nom du fichier tel que l'utilisateur l'a déposé. */
    @Column(nullable = false, length = 255)
    private String nom;

    /** Type déclaré par le navigateur, pour proposer le bon type au téléchargement. */
    @Column(length = 150)
    private String type;

    /** Taille en octets, telle que reçue. */
    private long taille;
}
