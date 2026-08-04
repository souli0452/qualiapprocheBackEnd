package com.qualiapproche.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Niveau de confidentialité d'un document, et rôles admis à le consulter.
 *
 * <p>La restriction s'ajoute à celle de la structure, elle ne s'y substitue pas : pour voir un
 * document, il faut relever de sa structure (ou en avoir reçu le partage) <b>et</b> détenir l'un
 * des rôles listés ici. Un niveau sans rôle ne restreint rien — c'est le niveau ordinaire.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class NiveauConfidentialiteDto extends AuditEntityDto {
    private String libelle;
    private String description;

    /** Rang, du moins sensible au plus sensible. */
    private Integer ordre;

    /**
     * Rôles admis à consulter un document de ce niveau, désignés par leur nom
     * (« RESPONSABLE_QUALITE », « PILOTE »…). Liste vide : aucune restriction de rôle.
     */
    @lombok.Builder.Default
    private List<String> rolesAutorises = new ArrayList<>();
}
