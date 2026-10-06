package com.qualiapproche.evaluation.service.referentiel;

import com.qualiapproche.evaluation.entities.TypeConstat;
import com.qualiapproche.evaluation.repository.TypeConstatRepository;
import org.springframework.stereotype.Service;

@Service
public class TypeConstatService extends ReferentielSimpleService<TypeConstat> {

    public TypeConstatService(TypeConstatRepository depot) {
        super(depot);
    }

    @Override
    protected String nom() {
        return "nature de constat";
    }

    @Override
    protected TypeConstat nouveau() {
        return new TypeConstat();
    }

    @Override
    protected void recopier(TypeConstat saisie, TypeConstat cible) {
        cible.setLibelle(exigerTexte(saisie.getLibelle(), "libellé"));
        cible.setCouleur(saisie.getCouleur());
        cible.setGenereNonConformite(saisie.isGenereNonConformite());
        cible.setNiveauNonConformiteId(saisie.getNiveauNonConformiteId());
    }
}
