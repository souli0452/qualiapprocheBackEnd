package com.qualiapproche.evaluation.service.referentiel;

import com.qualiapproche.evaluation.entities.Site;
import com.qualiapproche.evaluation.repository.SiteRepository;
import org.springframework.stereotype.Service;

@Service
public class SiteService extends ReferentielSimpleService<Site> {

    public SiteService(SiteRepository depot) {
        super(depot);
    }

    @Override
    protected String nom() {
        return "site";
    }

    @Override
    protected Site nouveau() {
        return new Site();
    }

    @Override
    protected void recopier(Site saisie, Site cible) {
        cible.setNom(exigerTexte(saisie.getNom(), "nom"));
        cible.setAdresse(saisie.getAdresse());
    }
}
