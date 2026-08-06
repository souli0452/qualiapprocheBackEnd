package com.qualiapproche.amelioration.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.PlanActionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Inscrit sur les actions correctives déjà en base les repères du dossier qui les a motivées : son
 * numéro et son processus émetteur.
 *
 * <p>Les deux colonnes existaient depuis toujours et n'étaient écrites par personne — ni le
 * formulaire, ni le serveur. La création les renseigne désormais, mais les actions antérieures
 * resteraient sans : la recherche par numéro de non-conformité n'en trouverait aucune, et les
 * relances d'échéance continueraient d'annoncer une action rattachée à « null » au responsable
 * qu'elles pressent.</p>
 *
 * <p>Les deux valeurs sont <b>recopiées</b> du dossier, qui seul en fait foi ; ce sont des repères
 * d'affichage et de recherche, le rattachement restant porté par {@code nonConformeId}. Ce qu'une
 * action porte déjà est laissé tel quel, et une action dont le dossier a disparu est signalée plutôt
 * que devinée.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RattrapageDesReperesDuDossier implements CommandLineRunner {

    private final PlanActionRepository planActionRepository;
    private final NonConformiteRepository nonConformiteRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<PlanAction> incompletes = planActionRepository.findSansReperesDuDossier();
        if (incompletes.isEmpty()) {
            return;
        }

        List<UUID> dossiers = incompletes.stream()
                .map(PlanAction::getNonConformeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, NonConformite> parIdentifiant = new HashMap<>();
        nonConformiteRepository.findAllById(dossiers)
                .forEach(dossier -> parIdentifiant.put(dossier.getId(), dossier));

        int completees = 0;
        int orphelines = 0;
        for (PlanAction action : incompletes) {
            NonConformite dossier = parIdentifiant.get(action.getNonConformeId());
            if (dossier == null) {
                orphelines++;
                continue;
            }
            boolean complete = false;
            if (estVide(action.getNumeroNc()) && !estVide(dossier.getNumeroReference())) {
                action.setNumeroNc(dossier.getNumeroReference());
                complete = true;
            }
            if (estVide(action.getProcEmetteur()) && !estVide(dossier.getNomProcessus())) {
                action.setProcEmetteur(dossier.getNomProcessus());
                complete = true;
            }
            if (complete) {
                completees++;
            }
        }

        if (completees > 0) {
            planActionRepository.saveAll(incompletes);
            log.info("{} action(s) corrective(s) ont reçu les repères de leur non-conformité.", completees);
        }
        if (orphelines > 0) {
            log.warn("{} action(s) corrective(s) restent sans repères : leur non-conformité est "
                    + "introuvable.", orphelines);
        }
    }

    private boolean estVide(String valeur) {
        return valeur == null || valeur.isBlank();
    }
}
