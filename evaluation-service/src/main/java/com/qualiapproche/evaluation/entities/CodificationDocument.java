package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * La règle de numérotation d'un type de document.
 *
 * <p>Format : {@code PRÉFIXE-TYPE-ANNÉE-NNN} — « ABMAQ-PLA-2026-013 » pour un plan,
 * « ABMAQ-RAP-2026-010 » pour un rapport. L'audit lui-même s'écrit sans segment de type,
 * « AUD-2026-012 » : son préfixe le dit déjà. Les documents déjà émis gardent leur référence.</p>
 *
 * <p>Le compteur repart de zéro quand l'année change : la maquette ne le précise pas, mais une
 * référence qui porte son année et continue de compter l'an suivant se lirait mal.</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "codification_document")
public class CodificationDocument extends AuditEntity {

    /** Le type « audit », dont la référence n'a pas de segment de type. */
    public static final String AUDIT = "AUD";

    /** Le document numéroté, qui sert aussi de segment de type : {@code AUD}, {@code PLA}, {@code RAP}. */
    @Column(name = "type_document", nullable = false, length = 50)
    private String typeDocument;

    @Column(name = "prefixe", nullable = false, length = 20)
    private String prefixe;

    @Column(name = "separateur", nullable = false, length = 5)
    private String separateur;

    @Column(name = "annee", length = 4)
    private String annee;

    @Column(name = "compteur", nullable = false)
    private int compteur;

    /** Avance le compteur et rend la référence qu'il désigne. À appeler sous verrou. */
    public String genererProchaineReference(int anneeCourante) {
        String courante = String.valueOf(anneeCourante);
        if (!courante.equals(annee)) {
            annee = courante;
            compteur = 0;
        }
        compteur++;
        String numero = String.format("%03d", compteur);
        if (AUDIT.equals(typeDocument)) {
            return String.join(separateur, prefixe, annee, numero);
        }
        return String.join(separateur, prefixe, typeDocument, annee, numero);
    }
}
