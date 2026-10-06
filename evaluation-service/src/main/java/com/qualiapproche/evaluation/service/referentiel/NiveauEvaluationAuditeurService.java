package com.qualiapproche.evaluation.service.referentiel;

import com.qualiapproche.evaluation.entities.NiveauEvaluationAuditeur;
import com.qualiapproche.evaluation.repository.NiveauEvaluationAuditeurRepository;
import org.springframework.stereotype.Service;

@Service
public class NiveauEvaluationAuditeurService extends ReferentielSimpleService<NiveauEvaluationAuditeur> {

    public NiveauEvaluationAuditeurService(NiveauEvaluationAuditeurRepository depot) {
        super(depot);
    }

    @Override
    protected String nom() {
        return "niveau d'évaluation des auditeurs";
    }

    @Override
    protected NiveauEvaluationAuditeur nouveau() {
        return new NiveauEvaluationAuditeur();
    }

    @Override
    protected void recopier(NiveauEvaluationAuditeur saisie, NiveauEvaluationAuditeur cible) {
        cible.setLibelle(exigerTexte(saisie.getLibelle(), "libellé"));
        cible.setOrdre(saisie.getOrdre());
    }
}
