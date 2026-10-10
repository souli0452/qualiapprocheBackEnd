package com.qualiapproche.referentiel.service.impl;

import com.qualiapproche.common.dto.NiveauEvaluationAuditeurDto;
import com.qualiapproche.referentiel.entities.NiveauEvaluationAuditeur;
import com.qualiapproche.referentiel.entities.mappers.NiveauEvaluationAuditeurMapper;
import com.qualiapproche.referentiel.repository.NiveauEvaluationAuditeurRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class NiveauEvaluationAuditeurService
        extends ParametrageParDirection<NiveauEvaluationAuditeur, NiveauEvaluationAuditeurDto> {

    public NiveauEvaluationAuditeurService(NiveauEvaluationAuditeurRepository depot,
                                           NiveauEvaluationAuditeurMapper mapper) {
        super(depot, mapper::toDto);
    }

    @Override
    protected String nom() {
        return "niveau d'évaluation des auditeurs";
    }

    @Override
    protected NiveauEvaluationAuditeur nouveau() {
        // Posé ici, et non par le seul @Builder.Default, que le constructeur vide ne relit pas.
        NiveauEvaluationAuditeur entree = new NiveauEvaluationAuditeur();
        entree.setActif(true);
        return entree;
    }

    /** L'échelle se lit dans son ordre, du plus bas au plus haut. */
    @Override
    protected Sort ordre() {
        return Sort.by("ordre");
    }

    @Override
    protected void recopier(NiveauEvaluationAuditeurDto saisie, NiveauEvaluationAuditeur cible) {
        cible.setLibelle(exigerTexte(saisie.getLibelle(), "libellé"));
        cible.setOrdre(saisie.getOrdre());
        if (cible.getId() != null) {
            cible.setActif(saisie.isActif());
        }
    }
}
