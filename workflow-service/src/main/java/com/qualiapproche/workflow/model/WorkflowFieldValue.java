package com.qualiapproche.workflow.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workflow_field_value")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowFieldValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "history_id", nullable = false)
    private ValidationHistory history;

    @Column(name = "field_code", nullable = false)
    private String fieldCode;

    @Column(name = "field_name")
    private String fieldName;

    /**
     * Libellé du champ tel qu'il était présenté à celui qui a saisi.
     *
     * <p>Recopié plutôt que relu sur le circuit, pour la même raison que le nom de l'auteur d'une
     * décision : un champ retiré du circuit — ou renommé — laisserait sinon ses valeurs sans
     * intitulé, et la seule chose à afficher serait le nom technique. Une saisie doit rester
     * lisible plus longtemps que le formulaire qui l'a recueillie.</p>
     *
     * <p>Nul sur les valeurs antérieures à ce champ : le nom technique reste alors le seul repère.</p>
     */
    @Column(name = "field_label")
    private String fieldLabel;

    @Column(columnDefinition = "TEXT")
    private String value; // The string representation of the filled value
}
