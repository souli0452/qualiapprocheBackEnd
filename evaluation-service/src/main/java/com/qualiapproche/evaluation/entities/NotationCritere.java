package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.enumeration.ConstatRQAPBF;
import com.qualiapproche.evaluation.enumeration.NiveauNotationRQAPBF;
import com.qualiapproche.evaluation.enumeration.StatutNotation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/** La note posée sur un critère du référentiel, avec la preuve qui la fonde. */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "notation_critere")
public class NotationCritere extends AuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private EvaluationRQAPBF evaluation;

    /** Le critère noté : une feuille du référentiel. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "noeud_id", nullable = false)
    private NoeudReferentiel noeud;

    @Enumerated(EnumType.STRING)
    @Column(name = "niveau", nullable = false, length = 30)
    private NiveauNotationRQAPBF niveau;

    @Column(name = "score")
    private Double score;

    @Column(name = "preuve", columnDefinition = "TEXT")
    private String preuve;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutNotation statut;

    /** La non-conformité ouverte à partir de cette notation, dans amelioration-service. */
    @Column(name = "non_conformite_id")
    private UUID nonConformiteId;

    public boolean estNonConforme() {
        return niveau == NiveauNotationRQAPBF.NON_CONFORME;
    }

    /** Le score tiré du niveau, aussitôt reporté sur la notation ; nul si non applicable. */
    public Double calculerScore() {
        score = niveau == null ? null : niveau.getScore();
        return score;
    }

    /**
     * Le constat binaire retenu pour la synthèse.
     *
     * <p>Seul « Conforme » vaut conforme : « Acceptable » et « À améliorer » sont des écarts, et
     * partent vers les non-conformités comme « Non conforme ». Un critère non applicable, ou pas
     * encore noté, ne donne aucun constat — il est hors du périmètre.</p>
     */
    public ConstatRQAPBF constatRetenu() {
        if (niveau == null || niveau == NiveauNotationRQAPBF.NON_APPLICABLE) {
            return null;
        }
        return niveau == NiveauNotationRQAPBF.CONFORME ? ConstatRQAPBF.CONFORME : ConstatRQAPBF.NC;
    }

    /** Le critère entre au rapport officiel ; il ne se publie qu'une fois noté. */
    public void publier() {
        if (niveau == null) {
            throw new BusinessException("Le critère " + noeud.getCode() + " n'est pas noté : il ne se publie pas.",
                    HttpStatus.CONFLICT);
        }
        statut = StatutNotation.PUBLIE;
    }

    public boolean estPublie() {
        return statut == StatutNotation.PUBLIE;
    }
}
