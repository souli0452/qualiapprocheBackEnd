package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.enumeration.StatutSignature;
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

import java.time.LocalDateTime;

/**
 * Un visa du circuit de signature du rapport d'audit.
 *
 * <p>Le circuit se parcourt dans l'ordre : un visa ne se donne qu'une fois les précédents donnés,
 * comme sur un parapheur. Un refus arrête le circuit — l'audit ne se clôt pas tant qu'un nouveau
 * circuit n'a pas été posé.</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "signature")
public class Signature extends AuditEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id", nullable = false)
    private Audit audit;

    /** L'identifiant Keycloak de la personne appelée à signer. */
    @Column(name = "signataire_id", nullable = false)
    private String signataireId;

    @Column(name = "fonction_signataire")
    private String fonctionSignataire;

    @Column(name = "ordre", nullable = false)
    private int ordre;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutSignature statut;

    @Column(name = "date_signature")
    private LocalDateTime dateSignature;

    @Column(name = "motif_refus", columnDefinition = "TEXT")
    private String motifRefus;

    public void signer() {
        exigerEnAttente();
        statut = StatutSignature.SIGNEE;
        dateSignature = LocalDateTime.now();
    }

    public void refuser(String motif) {
        exigerEnAttente();
        if (motif == null || motif.isBlank()) {
            throw new BusinessException("Un refus de signature se motive : le rédacteur du rapport"
                    + " doit savoir quoi reprendre.", HttpStatus.BAD_REQUEST);
        }
        statut = StatutSignature.REFUSEE;
        motifRefus = motif.strip();
        dateSignature = LocalDateTime.now();
    }

    private void exigerEnAttente() {
        if (statut != StatutSignature.EN_ATTENTE) {
            throw new BusinessException("Ce visa est déjà " + statut + ".", HttpStatus.CONFLICT);
        }
    }
}
