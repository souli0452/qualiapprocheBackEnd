package com.qualiapproche.referentiel.service.impl;

import com.qualiapproche.common.dto.CritereEvaluationAuditeurDto;
import com.qualiapproche.referentiel.entities.CritereEvaluationAuditeur;
import com.qualiapproche.referentiel.entities.mappers.CritereEvaluationAuditeurMapper;
import com.qualiapproche.referentiel.repository.CritereEvaluationAuditeurRepository;
import org.springframework.stereotype.Service;

@Service
public class CritereEvaluationAuditeurService
        extends ParametrageParDirection<CritereEvaluationAuditeur, CritereEvaluationAuditeurDto> {

    public CritereEvaluationAuditeurService(CritereEvaluationAuditeurRepository depot,
                                            CritereEvaluationAuditeurMapper mapper) {
        super(depot, mapper::toDto);
    }

    @Override
    protected String nom() {
        return "critère d'évaluation des auditeurs";
    }

    @Override
    protected CritereEvaluationAuditeur nouveau() {
        // Posé ici, et non par le seul @Builder.Default, que le constructeur vide ne relit pas.
        CritereEvaluationAuditeur entree = new CritereEvaluationAuditeur();
        entree.setActif(true);
        return entree;
    }

    @Override
    protected void recopier(CritereEvaluationAuditeurDto saisie, CritereEvaluationAuditeur cible) {
        cible.setLibelle(exigerTexte(saisie.getLibelle(), "libellé"));
        if (cible.getId() != null) {
            cible.setActif(saisie.isActif());
        }
    }
}
