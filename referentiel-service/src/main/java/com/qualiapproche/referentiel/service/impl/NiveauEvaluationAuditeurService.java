package com.qualiapproche.referentiel.service.impl;

import com.qualiapproche.common.dto.NiveauEvaluationAuditeurDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.referentiel.entities.NiveauEvaluationAuditeur;
import com.qualiapproche.referentiel.entities.mappers.NiveauEvaluationAuditeurMapper;
import com.qualiapproche.referentiel.repository.NiveauEvaluationAuditeurRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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

    /**
     * Une couleur {@code #RRGGBB}, ou rien. Elle part telle quelle dans le style des écrans :
     * toute autre forme est refusée plutôt que recopiée.
     */
    private static String couleurOuRien(String couleur) {
        if (couleur == null || couleur.isBlank()) {
            return null;
        }
        String propre = couleur.strip().toLowerCase();
        if (!propre.matches("#[0-9a-f]{6}")) {
            throw new BusinessException("La couleur doit s'écrire #RRGGBB (ex : #16a34a).", HttpStatus.BAD_REQUEST);
        }
        return propre;
    }

    @Override
    protected void recopier(NiveauEvaluationAuditeurDto saisie, NiveauEvaluationAuditeur cible) {
        cible.setLibelle(exigerTexte(saisie.getLibelle(), "libellé"));
        cible.setOrdre(saisie.getOrdre());
        cible.setCouleur(couleurOuRien(saisie.getCouleur()));
        if (cible.getId() != null) {
            cible.setActif(saisie.isActif());
        }
    }
}
