package com.qualiapproche.amelioration.service.impl;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.qualiapproche.amelioration.client.WorkflowClient;
import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.utils.ReglagesOrganisation;
import com.qualiapproche.common.dto.ValidationHistoryDto;
import com.qualiapproche.common.enumeration.Circuit;
import com.qualiapproche.common.enumeration.Etat;
import com.qualiapproche.common.utils.ClesReglages;
import com.qualiapproche.common.utils.StatutEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Édite la fiche récapitulative d'une non-conformité clôturée.
 *
 * <p>La fiche est le document d'enregistrement de la démarche qualité : identification du dossier,
 * constat, plans d'action menés, et visas — qui a décidé quoi, quand, avec quelle appréciation — à
 * chaque niveau du circuit. Elle ne s'édite qu'en fin de circuit : avant la clôture, le dossier
 * peut encore bouger et le document mentirait dès la décision suivante.</p>
 *
 * <p>Le gabarit est du HTML Thymeleaf rendu en PDF : la maquette se relit et s'amende comme
 * n'importe quel gabarit de courriel, là où le rapport Jasper historique n'existe qu'en binaire
 * compilé, sans source dans le dépôt.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FicheClotureNonConformiteService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH'h'mm");
    private static final String GABARIT = "ficheClotureNc.html";

    /** Délai au-delà duquel le logo est abandonné : la fiche ne doit pas attendre une image. */
    private static final int DELAI_LOGO_MS = 5000;

    private final NonConformiteRepository nonConformiteRepository;
    private final WorkflowClient workflowClient;
    private final ReglagesOrganisation reglages;
    private final TemplateEngine templateEngine;

    /** Fiche prête à servir : le contenu PDF et le nom sous lequel le proposer au téléchargement. */
    public record FicheEditee(String nomDeFichier, byte[] contenu) {
    }

    public FicheEditee editer(UUID nonConformiteId) {
        NonConformite nc = nonConformiteRepository.findById(nonConformiteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Non-conformité introuvable : " + nonConformiteId));

        if (nc.getEtatTraitement() != Etat.CLOTURE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La fiche ne s'édite qu'une fois la non-conformité clôturée. Le dossier est encore "
                            + (nc.getWorkflowStatus() != null ? "à l'étape « " + nc.getWorkflowStatus() + " »."
                            : "en cours de traitement."));
        }

        List<ValidationHistoryDto> visas = lireLesVisas(nonConformiteId);

        String html = templateEngine.process(GABARIT, contexte(nc, visas));
        byte[] pdf = versPdf(html);

        return new FicheEditee(nomDeFichier(nc), pdf);
    }

    /**
     * Les visas sont la raison d'être de la fiche : sans eux, mieux vaut refuser que d'imprimer un
     * document d'enregistrement où aucun niveau n'aurait signé.
     */
    private List<ValidationHistoryDto> lireLesVisas(UUID nonConformiteId) {
        List<ValidationHistoryDto> visas;
        try {
            visas = workflowClient.historiqueDesDecisions(nonConformiteId);
        } catch (Exception e) {
            log.warn("Historique du circuit illisible pour la fiche de clôture de {} : {}",
                    nonConformiteId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "L'historique des visas est momentanément illisible : la fiche ne peut pas être "
                            + "éditée sans ses visas. Réessayez dans quelques instants.");
        }
        if (visas == null || visas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Aucune décision n'est enregistrée sur le circuit de ce dossier : "
                            + "il n'y a pas de visas à porter sur la fiche.");
        }
        return visas;
    }

    private Context contexte(NonConformite nc, List<ValidationHistoryDto> visas) {
        Context contexte = new Context(Locale.FRENCH);

        contexte.setVariable("organisation", valeurOuVide(reglages.valeur(ClesReglages.ORGANISATION_NOM)));
        contexte.setVariable("logo", logoEnDataUri());

        contexte.setVariable("numero", valeurOuTiret(nc.getNumeroReference()));
        contexte.setVariable("version", valeurOuTiret(nc.getVersion()));
        contexte.setVariable("dateEdition", LocalDate.now().format(DATE));

        contexte.setVariable("dateSoumission",
                nc.getCreatedAt() != null ? nc.getCreatedAt().format(DATE) : "—");
        contexte.setVariable("emetteur", valeurOuTiret(nc.getCurrentUserfullName()));
        contexte.setVariable("fonctionEmetteur", valeurOuVide(nc.getFonctionEmetteur()));
        contexte.setVariable("structureEmettrice", valeurOuTiret(nc.getStructureSoumissionLibelle()));
        contexte.setVariable("processus", valeurOuTiret(premierRenseigne(nc.getNomProcessus(), nc.getOrigineService())));
        contexte.setVariable("typeNonConformite", valeurOuTiret(nc.getTypeNonConformiteLibelle()));
        contexte.setVariable("niveau", valeurOuTiret(nc.getNiveauNonConformiteLibelle()));
        contexte.setVariable("origine", valeurOuTiret(nc.getOriginNonConformiteLibelle()));
        contexte.setVariable("circuitTraitement",
                nc.getCircuit() != null ? nc.getCircuit().getLibelle() : "—");
        contexte.setVariable("agentImpute", valeurOuTiret(nc.getUserImputFullName()));
        contexte.setVariable("participants", nc.getParticipants() != null
                && nc.getParticipants().getFullNames() != null
                && !nc.getParticipants().getFullNames().isEmpty()
                ? String.join(", ", nc.getParticipants().getFullNames().stream().sorted().toList())
                : "—");

        contexte.setVariable("description", valeurOuTiret(nc.getJustification()));

        // Un circuit « Correction » remet en conformité sans rechercher la cause : la colonne
        // n'existe pas sur ses plans, la fiche ne l'imprime donc pas non plus.
        contexte.setVariable("avecCause", nc.getCircuit() != Circuit.CORRECTION);
        contexte.setVariable("plans", nc.getPlanActions() == null ? List.of()
                : nc.getPlanActions().stream()
                        .sorted(Comparator.comparing(PlanAction::getNumeroOdre,
                                Comparator.nullsLast(FicheClotureNonConformiteService::comparerRangs)))
                        .map(this::lignePlan)
                        .toList());

        contexte.setVariable("visas", visas.stream().map(this::ligneVisa).toList());
        contexte.setVariable("dateCloture", visas.stream()
                .map(ValidationHistoryDto::getDecisionDate)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(d -> d.format(DATE))
                .orElse("—"));

        return contexte;
    }

    private LignePlan lignePlan(PlanAction plan) {
        return new LignePlan(
                valeurOuVide(plan.getNumeroOdre()),
                valeurOuTiret(plan.getActionCorrective()),
                valeurOuTiret(plan.getCauseIdentifiees()),
                valeurOuTiret(plan.getSolutionRetenues()),
                valeurOuTiret(plan.getResponsableNomComplet()),
                plan.getDateEcheance() != null ? plan.getDateEcheance().format(DATE) : "—",
                libelleStatut(plan.getStatus()));
    }

    private LigneVisa ligneVisa(ValidationHistoryDto decision) {
        return new LigneVisa(
                valeurOuTiret(decision.getStepName() != null ? decision.getStepName() : decision.getStepCode()),
                valeurOuTiret(decision.getDecision()),
                valeurOuTiret(decision.getValidatorFullName()),
                decision.getDecisionDate() != null ? decision.getDecisionDate().format(DATE_HEURE) : "—",
                valeurOuTiret(decision.getComments()));
    }

    /** Ligne du tableau des plans d'action, prête à imprimer. */
    public record LignePlan(String numero, String actionCorrective, String cause, String solution,
                            String responsable, String echeance, String statut) {
    }

    /** Ligne du tableau des visas : une décision d'un niveau du circuit, avec son appréciation. */
    public record LigneVisa(String etape, String decision, String auteur, String date, String appreciation) {
    }

    private static String libelleStatut(StatutEnum statut) {
        if (statut == null) {
            return "—";
        }
        return switch (statut) {
            case NON_TRAITER -> "À réaliser";
            case EN_VERIFICATION -> "Réalisation à vérifier";
            case EFFICACITE_A_MESURER -> "Efficacité à mesurer";
            case TRAITER -> "Soldée";
            case REJECTED -> "Déclinée";
            case ACTIF, INACTIF -> "Proposée";
        };
    }

    /**
     * Le logo n'existe que comme adresse de réglage : il est téléchargé et incorporé au document,
     * qui doit rester lisible hors de toute connexion. Introuvable ou trop lent, la fiche part
     * sans lui — un en-tête sans image vaut mieux qu'aucune fiche.
     */
    private String logoEnDataUri() {
        String adresse = reglages.valeur(ClesReglages.LOGO_URL);
        if (adresse == null) {
            return null;
        }
        try {
            URLConnection connexion = new URL(adresse).openConnection();
            connexion.setConnectTimeout(DELAI_LOGO_MS);
            connexion.setReadTimeout(DELAI_LOGO_MS);
            byte[] octets;
            try (InputStream flux = connexion.getInputStream()) {
                octets = flux.readAllBytes();
            }
            if (octets.length == 0) {
                return null;
            }
            String type = connexion.getContentType();
            if (type == null || !type.startsWith("image/")) {
                type = "image/png";
            }
            return "data:" + type + ";base64," + Base64.getEncoder().encodeToString(octets);
        } catch (Exception e) {
            log.warn("Logo de l'organisation illisible ({}) : la fiche est éditée sans lui.", e.getMessage());
            return null;
        }
    }

    private byte[] versPdf(String html) {
        try {
            ByteArrayOutputStream sortie = new ByteArrayOutputStream();
            PdfRendererBuilder constructeur = new PdfRendererBuilder();
            constructeur.useFastMode();
            constructeur.withHtmlContent(html, null);
            constructeur.toStream(sortie);
            constructeur.run();
            return sortie.toByteArray();
        } catch (Exception e) {
            log.error("Rendu PDF de la fiche de clôture impossible", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "La fiche n'a pas pu être mise en page : " + e.getMessage());
        }
    }

    private static String nomDeFichier(NonConformite nc) {
        String repere = nc.getNumeroReference() != null && !nc.getNumeroReference().isBlank()
                ? nc.getNumeroReference().replaceAll("[^A-Za-z0-9._-]", "_")
                : nc.getId().toString();
        return "Fiche_NC_" + repere + ".pdf";
    }

    /**
     * Le rang d'un plan est saisi comme texte : « 2 » doit pourtant passer avant « 10 ». Deux rangs
     * numériques se comparent en nombres, tout le reste retombe sur l'ordre alphabétique.
     */
    private static int comparerRangs(String premier, String second) {
        try {
            return Integer.compare(Integer.parseInt(premier.trim()), Integer.parseInt(second.trim()));
        } catch (NumberFormatException e) {
            return premier.compareToIgnoreCase(second);
        }
    }

    private static String premierRenseigne(String premier, String second) {
        return premier != null && !premier.isBlank() ? premier : second;
    }

    private static String valeurOuTiret(String valeur) {
        return valeur != null && !valeur.isBlank() ? valeur : "—";
    }

    private static String valeurOuVide(String valeur) {
        return valeur != null && !valeur.isBlank() ? valeur : "";
    }
}
