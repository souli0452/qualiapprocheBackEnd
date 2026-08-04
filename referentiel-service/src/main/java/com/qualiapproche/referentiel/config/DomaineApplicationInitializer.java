package com.qualiapproche.referentiel.config;

import com.qualiapproche.referentiel.entities.DomaineApplication;
import com.qualiapproche.referentiel.repository.DomaineApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sème les domaines d'application usuels au premier démarrage.
 *
 * <p>Le domaine était choisi dans une liste écrite en dur dans l'écran de création. Le rendre
 * paramétrable sans rien semer aurait laissé un sélecteur vide au premier lancement, et rendu
 * orphelins les documents déjà déposés, dont le domaine ne correspondrait plus à aucune entrée.
 * Ces huit valeurs sont celles que l'écran proposait ; elles se modifient et se complètent
 * librement ensuite.</p>
 *
 * <p>Rapprochement par libellé, insertion seule : un domaine renommé ou supprimé à dessein ne
 * réapparaît pas sous son ancien nom au démarrage suivant.</p>
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DomaineApplicationInitializer implements CommandLineRunner {

    private final DomaineApplicationRepository repository;

    private static final List<Map.Entry<String, String>> DOMAINES = List.of(
            Map.entry("Qualité", "Système de management de la qualité (SMQ)"),
            Map.entry("Environnement", "Système de management environnemental (SME)"),
            Map.entry("Santé & Sécurité", "Santé et sécurité au travail (SST)"),
            Map.entry("Sécurité de l'information", "Management de la sécurité de l'information (SMSI)"),
            Map.entry("Sécurité alimentaire", "Sécurité des denrées alimentaires (ISO 22000)"),
            Map.entry("Management général", "Pilotage et organisation générale"),
            Map.entry("Ressources humaines", "Gestion et développement des personnes"),
            Map.entry("Production & opérations", "Réalisation des produits et des services"));

    @Override
    @Transactional
    public void run(String... args) {
        Set<String> existants = repository.findAll().stream()
                .map(DomaineApplication::getLibelle)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        int rang = 1;
        int crees = 0;
        for (Map.Entry<String, String> domaine : DOMAINES) {
            if (!existants.contains(domaine.getKey())) {
                repository.save(DomaineApplication.builder()
                        .libelle(domaine.getKey())
                        .description(domaine.getValue())
                        .ordre(rang)
                        .build());
                crees++;
            }
            rang++;
        }

        if (crees > 0) {
            log.info("Domaines d'application : {} domaine(s) usuel(s) ajouté(s) sur {}.",
                    crees, DOMAINES.size());
        }
    }
}
