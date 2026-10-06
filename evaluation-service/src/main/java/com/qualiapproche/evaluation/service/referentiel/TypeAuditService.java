package com.qualiapproche.evaluation.service.referentiel;

import com.qualiapproche.evaluation.entities.TypeAudit;
import com.qualiapproche.evaluation.repository.TypeAuditRepository;
import org.springframework.stereotype.Service;

@Service
public class TypeAuditService extends ReferentielSimpleService<TypeAudit> {

    public TypeAuditService(TypeAuditRepository depot) {
        super(depot);
    }

    @Override
    protected String nom() {
        return "type d'audit";
    }

    @Override
    protected TypeAudit nouveau() {
        return new TypeAudit();
    }

    @Override
    protected void recopier(TypeAudit saisie, TypeAudit cible) {
        cible.setLibelle(exigerTexte(saisie.getLibelle(), "libellé"));
        cible.setCircuitExterne(saisie.isCircuitExterne());
        cible.setTypeNonConformiteId(saisie.getTypeNonConformiteId());
    }
}
