package com.qualiapproche.referentiel.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Une pièce jointe facultative d'une entrée de FAQ.
 *
 * <p>Facultative parce qu'une réponse se suffit le plus souvent à elle-même ; une procédure, un
 * formulaire ou un modèle viennent l'appuyer quand le texte ne suffit pas. Dans sa propre table
 * parce qu'une entrée peut en porter plusieurs, ou aucune.</p>
 *
 * <p>Le fichier lui-même vit sur le serveur de fichiers, comme les pièces des non-conformités :
 * seule sa désignation est ici, et {@code url} en est la référence de stockage — jamais une
 * adresse publique. Le téléchargement passe par le service, qui vérifie d'abord à qui appartient
 * l'entrée.</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@Table(name = "fichiers_faq")
public class FichierFaq extends AuditEntity implements FichierStocke {

    @Column(name = "faq_id", nullable = false)
    private UUID faqId;

    @Column(name = "nom")
    private String nom;

    @Column(name = "ext", length = 32)
    private String ext;

    @Column(name = "type", length = 150)
    private String type;

    @Column(name = "url", columnDefinition = "TEXT")
    private String url;
}
