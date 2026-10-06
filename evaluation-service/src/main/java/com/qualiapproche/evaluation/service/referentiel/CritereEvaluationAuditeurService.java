package com.qualiapproche.evaluation.service.referentiel;

import com.qualiapproche.evaluation.entities.CritereEvaluationAuditeur;
import com.qualiapproche.evaluation.repository.CritereEvaluationAuditeurRepository;
import org.springframework.stereotype.Service;

@Service
public class CritereEvaluationAuditeurService extends ReferentielSimpleService<CritereEvaluationAuditeur> {

    public CritereEvaluationAuditeurService(CritereEvaluationAuditeurRepository depot) {
        super(depot);
    }

    @Override
    protected String nom() {
        return "critère d'évaluation des auditeurs";
    }

    @Override
    protected CritereEvaluationAuditeur nouveau() {
        return new CritereEvaluationAuditeur();
    }

    @Override
    protected void recopier(CritereEvaluationAuditeur saisie, CritereEvaluationAuditeur cible) {
        cible.setLibelle(exigerTexte(saisie.getLibelle(), "libellé"));
    }
}
