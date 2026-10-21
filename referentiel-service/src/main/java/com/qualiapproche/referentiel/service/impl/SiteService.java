package com.qualiapproche.referentiel.service.impl;

import com.qualiapproche.common.dto.SiteDto;
import com.qualiapproche.referentiel.entities.Site;
import com.qualiapproche.referentiel.entities.mappers.SiteMapper;
import com.qualiapproche.referentiel.repository.SiteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SiteService extends ParametrageParDirection<Site, SiteDto> {

    public SiteService(SiteRepository depot, SiteMapper mapper) {
        super(depot, mapper::toDto);
    }

    @Override
    protected String nom() {
        return "site";
    }

    /** Un site se cherche par son nom ou son adresse. */
    @Override
    protected List<String> champsRecherche() {
        return List.of("nom", "adresse");
    }

    @Override
    protected Site nouveau() {
        // Posé ici, et non par le seul @Builder.Default, que le constructeur vide ne relit pas.
        Site entree = new Site();
        entree.setActif(true);
        return entree;
    }

    @Override
    protected void recopier(SiteDto saisie, Site cible) {
        cible.setNom(exigerTexte(saisie.getNom(), "nom"));
        cible.setAdresse(saisie.getAdresse());
        if (cible.getId() != null) {
            cible.setActif(saisie.isActif());
        }
    }
}
