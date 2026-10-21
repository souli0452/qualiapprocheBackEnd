package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.entities.RegleNotification;
import com.qualiapproche.evaluation.repository.RegleNotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RegleNotificationService extends ParametrageSimpleService<RegleNotification> {

    public RegleNotificationService(RegleNotificationRepository depot) {
        super(depot);
    }

    @Override
    protected String nom() {
        return "règle de notification";
    }

    @Override
    protected RegleNotification nouveau() {
        return new RegleNotification();
    }

    @Override
    protected void recopier(RegleNotification saisie, RegleNotification cible) {
        cible.setLibelle(exigerTexte(saisie.getLibelle(), "libellé"));
        cible.setDelaiJours(saisie.getDelaiJours());
        cible.setDestinataires(saisie.getDestinataires());
        cible.setActif(saisie.isActif());
    }

    @Override
    protected void verifier(RegleNotification regle) {
        if (regle.getDelaiJours() < 0) {
            throw new BusinessException("Le délai d'une règle se compte en jours avant le début de"
                    + " l'audit : il ne peut pas être négatif.", HttpStatus.BAD_REQUEST);
        }
    }
}
