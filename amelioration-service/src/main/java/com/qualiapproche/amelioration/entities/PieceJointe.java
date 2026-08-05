package com.qualiapproche.amelioration.entities;

import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.storage.FichierStocke;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "piece_jointe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PieceJointe extends AuditEntity implements Serializable, FichierStocke {

    @Serial
    private static final long serialVersionUID = 1L;

    private String nom;
    private String ext;
    private String type;

    @Column(name = "url")
    private String url;

    @Column(name = "entity_id")
    private UUID entityId;

    /**
     * La pièce a été déposée au fil du circuit de validation, et non saisie sur la fiche.
     *
     * <p>Un justificatif de rejet n'appartient pas à la liste des pièces jointes que l'utilisateur
     * compose : il ne figure donc pas dans ce que le client renvoie à l'enregistrement, et
     * l'alignement des pièces sur cette liste l'aurait supprimé à la première modification du
     * dossier — silencieusement, le champ {@code docRejet} pointant alors dans le vide.</p>
     */
    // La valeur par défaut est portée par la colonne, et non par l'annotation : la base est mise à
    // jour par Hibernate (ddl-auto: update) et une colonne « not null » ajoutée sans défaut est
    // refusée par PostgreSQL dès que la table comporte des lignes — c'est-à-dire partout.
    @Column(name = "depose_par_circuit", columnDefinition = "boolean not null default false")
    private boolean deposeParCircuit;

    private boolean zipFile;
}
