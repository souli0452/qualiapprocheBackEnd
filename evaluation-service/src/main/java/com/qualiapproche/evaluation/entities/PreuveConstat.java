package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.storage.FichierStocke;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Un élément de preuve joint à un constat : photo, registre scanné, extrait de procédure.
 *
 * <p>Le fichier vit sur le serveur de fichiers ; seule sa désignation est ici, et {@code url} en
 * est la référence de stockage — jamais une adresse publique. Il se lit et se retire avec les
 * droits du constat : un brouillon et ses preuves ne regardent que son rédacteur.</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "preuve_constat")
public class PreuveConstat extends AuditEntity implements FichierStocke {

    @Column(name = "constat_id", nullable = false)
    private UUID constatId;

    @Column(name = "nom")
    private String nom;

    @Column(name = "ext", length = 32)
    private String ext;

    @Column(name = "type", length = 150)
    private String type;

    @Column(name = "url", columnDefinition = "TEXT")
    private String url;
}
