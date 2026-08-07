package com.qualiapproche.support.model;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "qms_document_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class QmsDocumentType extends AuditEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String libelle;

    @Column(nullable = false)
    private String folderName;

    // Le circuit n'est plus désigné ici : c'est le circuit qui se réserve à un type
    // (Workflow.cibleId), là où il se configure et où l'unicité du couple (famille, cible) peut
    // être tenue par la base. Deux faces d'un même fait finissaient par se contredire.
}
