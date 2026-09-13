package com.qualiapproche.ia.service.impl;

import com.qualiapproche.common.config.PermissionChecker;
import com.qualiapproche.common.dto.NcDashboardDto;
import com.qualiapproche.common.dto.NonConformiteDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.response.PaginatedResponse;
import com.qualiapproche.ia.client.AmeliorationClient;
import com.qualiapproche.ia.client.SupportClient;
import com.qualiapproche.ia.client.vue.DemandeDocumentVue;
import com.qualiapproche.ia.client.vue.DocumentVue;
import com.qualiapproche.ia.client.vue.PlanActionVue;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.ia.dto.ChiffreDto;
import com.qualiapproche.ia.dto.ElementReponseDto;
import com.qualiapproche.ia.dto.QuestionDto;
import com.qualiapproche.ia.dto.ReponseConversationDto;
import com.qualiapproche.ia.dto.ReponseQuestionDto;
import com.qualiapproche.ia.enumeration.QuestionPredefinie;
import com.qualiapproche.ia.service.ConversationService;
import com.qualiapproche.ia.service.QuestionService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Les questions prédéfinies : l'application interroge l'API, le modèle commente.
 *
 * <p>Le partage des rôles est la raison d'être de cette classe. <b>L'application</b> choisit le
 * point d'entrée — jamais le modèle : un 7B choisit mal, invente ses paramètres, et un texte piégé
 * lu en chemin pourrait lui dicter l'appel. <b>Le service métier</b> décide de ce que l'appelant
 * voit, puisque l'appel part avec son jeton et ses permissions. <b>Le modèle</b> ne fait qu'écrire
 * une phrase par-dessus, et les données s'affichent sous elle, intactes.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    /**
     * Nombre d'éléments demandés. Assez pour que la réponse ait du corps, assez peu pour que le
     * relevé transmis au modèle reste court — il se paie en jetons à chaque tour de la fenêtre.
     * Le total réel, lui, vient du service métier et peut le dépasser.
     */
    private static final int TAILLE_ECHANTILLON = 10;

    /** Longueur du rappel tiré d'un texte long (description, objectif). */
    private static final int LONGUEUR_LIBELLE = 140;

    private final AmeliorationClient ameliorationClient;
    private final SupportClient supportClient;
    private final ConversationService conversationService;
    private final PermissionChecker permissionChecker;

    /**
     * Ce qu'un module a rendu : des lignes, ou des chiffres — jamais les deux.
     *
     * <p>Une question « à traiter » rend une liste ; une question de statistiques rend un relevé
     * de compteurs. La réponse porte les deux champs, et l'écran affiche celui qui est rempli.</p>
     */
    private record Releve(List<ElementReponseDto> elements, List<ChiffreDto> chiffres, long total) {

        static Releve deLignes(List<ElementReponseDto> elements, long total) {
            return new Releve(elements, List.of(), total);
        }

        static Releve deChiffres(List<ChiffreDto> chiffres) {
            return new Releve(List.of(), chiffres, chiffres.size());
        }

        boolean estVide() {
            return elements.isEmpty() && chiffres.isEmpty();
        }
    }

    @Override
    public List<QuestionDto> catalogue() {
        return Arrays.stream(QuestionPredefinie.values())
                .filter(this::estOuverte)
                .map(question -> QuestionDto.builder()
                        .code(question)
                        .libelle(question.getLibelle())
                        .description(question.getDescription())
                        .build())
                .toList();
    }

    @Override
    public ReponseQuestionDto repondre(QuestionPredefinie code, UUID conversationId) {
        exigerLaPortee(code);
        Releve releve = switch (code) {
            case CE_QUI_M_ATTEND -> nonConformitesATraiter();
            case MES_PLANS_ACTION -> plansActionATraiter();
            case DOCUMENTS_A_VISER -> documentsATraiter();
            case DEMANDES_A_INSTRUIRE -> demandesATraiter();
            case CHIFFRES_DE_L_ORGANISATION -> chiffresDeLOrganisation();
            case CHIFFRES_DE_MA_STRUCTURE -> chiffresDeMaStructure();
            case MES_CHIFFRES -> mesChiffres();
        };
        return composer(code, releve, conversationId);
    }

    /**
     * La question est-elle ouverte à l'appelant ?
     *
     * <p>Les questions de statistiques ne portent pas toutes la même chose : celles de
     * l'organisation exigent l'habilitation transverse, celles d'une structure ou d'une personne
     * se rabattent sur les siennes. Une question dont le périmètre n'est pas connu du jeton —
     * pas de structure, pas d'utilisateur — n'est pas proposée : elle n'aurait rien à interroger.</p>
     */
    private boolean estOuverte(QuestionPredefinie question) {
        if (question.getPorteeRequise() != null && !permissionChecker.detient(question.getPorteeRequise())) {
            return false;
        }
        return switch (question) {
            case CHIFFRES_DE_MA_STRUCTURE -> SecurityUtils.getCurrentUserStructureId() != null;
            case MES_CHIFFRES -> SecurityUtils.getCurrentUserId() != null;
            default -> true;
        };
    }

    /**
     * Refuse une question fermée à l'appelant.
     *
     * <p>Le catalogue filtré ne protège rien : l'adresse se devine, et rien n'empêche de la poster
     * directement. Le contrôle a donc lieu ici aussi — non pour suppléer le module, qui garde ses
     * tableaux de bord par les mêmes habilitations, mais pour refuser <b>avant</b> l'appel et
     * rendre une phrase qui parle de périmètre plutôt qu'un refus venu d'ailleurs.</p>
     */
    private void exigerLaPortee(QuestionPredefinie question) {
        if (!estOuverte(question)) {
            throw new BusinessException(
                    "Cette question n'est pas ouverte à votre périmètre.", HttpStatus.FORBIDDEN);
        }
    }

    // ---------------------------------------------------------------- ce que chaque module rend

    private Releve nonConformitesATraiter() {
        PaginatedResponse<NonConformiteDto> page = demander(
                () -> ameliorationClient.aTraiter(0, TAILLE_ECHANTILLON));
        List<NonConformiteDto> dossiers = contenu(page);
        return Releve.deLignes(
                dossiers.stream().map(dossier -> ElementReponseDto.builder()
                        .reference(dossier.getNumeroReference())
                        .libelle(resumer(dossier.getJustification()))
                        .niveau(dossier.getNiveauNonConformiteLibelle())
                        .etat(dossier.getWorkflowStatus() != null ? dossier.getWorkflowStatus()
                                : (dossier.getEtatTraitement() != null ? dossier.getEtatTraitement().name() : null))
                        .ressourceType("NON_CONFORMITE")
                        .ressourceId(dossier.getId() != null ? dossier.getId().toString() : null)
                        .build()).toList(),
                total(page));
    }

    private Releve plansActionATraiter() {
        PaginatedResponse<PlanActionVue> page = demander(
                () -> ameliorationClient.plansATraiter(0, TAILLE_ECHANTILLON));
        List<PlanActionVue> plans = contenu(page);
        return Releve.deLignes(
                plans.stream().map(plan -> ElementReponseDto.builder()
                        .reference(plan.getNumeroOdre())
                        .libelle(resumer(plan.getCauseIdentifiees()))
                        // L'échéance tient lieu de niveau : sur un plan d'action, c'est elle qui
                        // dit l'urgence, là où une non-conformité la dit par sa gravité.
                        .niveau(plan.getDateEcheance() != null ? "échéance " + plan.getDateEcheance() : null)
                        .etat(plan.getWorkflowStatus())
                        .ressourceType("PLAN_ACTION")
                        .ressourceId(plan.getId())
                        .build()).toList(),
                total(page));
    }

    private Releve documentsATraiter() {
        List<DocumentVue> documents = demander(supportClient::documentsATraiter);
        if (documents == null) {
            documents = List.of();
        }
        return Releve.deLignes(
                documents.stream().limit(TAILLE_ECHANTILLON).map(document -> ElementReponseDto.builder()
                        .reference(document.getDocumentNumber())
                        .libelle(document.getTitre())
                        .niveau(document.isEnRetardRevision() ? "révision en retard" : document.getDocumentType())
                        .etat(document.getServiceLibelle())
                        .ressourceType("DOCUMENT")
                        .ressourceId(document.getId())
                        .build()).toList(),
                documents.size());
    }

    private Releve demandesATraiter() {
        List<DemandeDocumentVue> demandes = demander(supportClient::demandesATraiter);
        if (demandes == null) {
            demandes = List.of();
        }
        return Releve.deLignes(
                demandes.stream().limit(TAILLE_ECHANTILLON).map(demande -> ElementReponseDto.builder()
                        .reference(demande.getDocumentNumber() != null
                                ? demande.getDocumentNumber() : demande.getDocumentTitre())
                        .libelle(resumer(demande.getObjectif()))
                        .niveau(demande.getType())
                        .etat(demande.getEtat())
                        .ressourceType("DEMANDE_DOCUMENT")
                        .ressourceId(demande.getId())
                        .build()).toList(),
                demandes.size());
    }

    // ---------------------------------------------------------------- les chiffres, selon la portée

    /** Les chiffres de toutes les structures — réservés à l'habilitation transverse. */
    private Releve chiffresDeLOrganisation() {
        return Releve.deChiffres(versChiffres(
                demander(ameliorationClient::tableauDeBordOrganisation)));
    }

    /** Les chiffres de sa propre structure : le module refuse celle d'un autre. */
    private Releve chiffresDeMaStructure() {
        String structureId = SecurityUtils.getCurrentUserStructureId();
        return Releve.deChiffres(versChiffres(
                demander(() -> ameliorationClient.tableauDeBordStructure(structureId))));
    }

    /** Ses propres chiffres : le module refuse ceux d'un autre. */
    private Releve mesChiffres() {
        String utilisateurId = SecurityUtils.getCurrentUserId();
        return Releve.deChiffres(versChiffres(
                demander(() -> ameliorationClient.tableauDeBordPersonne(utilisateurId))));
    }

    /**
     * Met le tableau de bord en chiffres affichables.
     *
     * <p>Mis en forme <b>ici</b>, une fois pour toutes : ni l'écran ni le modèle n'ont à arrondir
     * un taux ou à recompter un total. Les deux taux sont marqués en alerte sous les seuils
     * usuels d'un système qualité — c'est ce qui fait la différence entre un tableau qu'on lit et
     * un tableau qu'on regarde.</p>
     */
    private List<ChiffreDto> versChiffres(NcDashboardDto tableau) {
        if (tableau == null) {
            return List.of();
        }
        List<ChiffreDto> chiffres = new ArrayList<>();
        chiffres.add(chiffre("Total", String.valueOf(tableau.getTotal()), null));
        chiffres.add(chiffre("En cours", String.valueOf(tableau.getEnCours()), null));
        chiffres.add(chiffre("Clôturées", String.valueOf(tableau.getCloturees()), null));
        chiffres.add(chiffre("En retard", String.valueOf(tableau.getEnRetard()),
                tableau.getEnRetard() > 0 ? "retard" : null));
        if (tableau.getTauxResolution() != null) {
            chiffres.add(chiffre("Taux de résolution", pourcentage(tableau.getTauxResolution()),
                    tableau.getTauxResolution() < 80 ? "bas" : null));
        }
        if (tableau.getTauxSla() != null) {
            chiffres.add(chiffre("Respect des délais", pourcentage(tableau.getTauxSla()),
                    tableau.getTauxSla() < 80 ? "bas" : null));
        }
        return chiffres;
    }

    private ChiffreDto chiffre(String libelle, String valeur, String alerte) {
        return ChiffreDto.builder().libelle(libelle).valeur(valeur).alerte(alerte).build();
    }

    private String pourcentage(Double taux) {
        return Math.round(taux) + " %";
    }

    // ---------------------------------------------------------------- la réponse rendue

    /**
     * Assemble la réponse : les données d'abord, la phrase ensuite.
     *
     * <p>Rien à traiter, pas d'appel au modèle : il n'aurait rien à commenter, la phrase juste est
     * connue d'avance, et la lui faire écrire reviendrait à payer pour qu'il la brode.</p>
     */
    private ReponseQuestionDto composer(QuestionPredefinie code, Releve releve, UUID conversationId) {
        if (releve.estVide()) {
            return ReponseQuestionDto.builder()
                    .code(code)
                    .libelle(code.getLibelle())
                    .elements(releve.elements())
                    .chiffres(releve.chiffres())
                    .total(0)
                    .commentaire("Rien ne vous attend de ce côté pour le moment.")
                    .conversationId(conversationId)
                    .avertissement(ConversationServiceImpl.AVERTISSEMENT)
                    .build();
        }

        ReponseConversationDto commentaire = conversationService.repondreSurMatiere(
                conversationId, code.getLibelle(), releve(code, releve));

        return ReponseQuestionDto.builder()
                .code(code)
                .libelle(code.getLibelle())
                .elements(releve.elements())
                .chiffres(releve.chiffres())
                .total(releve.total())
                .commentaire(commentaire.getReponse())
                .conversationId(commentaire.getConversationId())
                .avertissement(commentaire.getAvertissement())
                .build();
    }

    /**
     * Le relevé transmis au modèle — et à lui seul.
     *
     * <p>Volontairement sec, et sans rien qui ne figure déjà à l'écran : ce n'est pas un texte à
     * rendre, c'est la matière d'une phrase. Le total y est nommé pour que le modèle n'ait aucune
     * raison d'en compter un autre.</p>
     */
    private String releve(QuestionPredefinie code, Releve releve) {
        StringBuilder matiere = new StringBuilder();
        matiere.append(code.getDescription()).append('\n');

        if (!releve.chiffres().isEmpty()) {
            for (ChiffreDto chiffre : releve.chiffres()) {
                matiere.append("- ").append(chiffre.getLibelle()).append(" : ")
                        .append(chiffre.getValeur()).append('\n');
            }
            return matiere.toString();
        }

        matiere.append("Total : ").append(releve.total())
                .append(", dont voici les ").append(releve.elements().size()).append(" premiers.\n");
        for (ElementReponseDto element : releve.elements()) {
            matiere.append("- ").append(element.getReference());
            if (element.getNiveau() != null) {
                matiere.append(" [").append(element.getNiveau()).append(']');
            }
            if (element.getEtat() != null) {
                matiere.append(" [").append(element.getEtat()).append(']');
            }
            if (element.getLibelle() != null) {
                matiere.append(" : ").append(element.getLibelle());
            }
            matiere.append('\n');
        }
        return matiere.toString();
    }

    // ---------------------------------------------------------------- utilitaires

    private <T> List<T> contenu(PaginatedResponse<T> page) {
        return page != null && page.getContent() != null ? page.getContent() : List.of();
    }

    private long total(PaginatedResponse<?> page) {
        return page != null ? page.getTotalElements() : 0;
    }

    /** Les textes saisis portent du HTML d'éditeur, et sont longs : on n'en garde qu'un rappel. */
    private String resumer(String texte) {
        if (texte == null || texte.isBlank()) {
            return null;
        }
        String propre = texte.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        return propre.length() <= LONGUEUR_LIBELLE ? propre
                : propre.substring(0, LONGUEUR_LIBELLE).trim() + "…";
    }

    /**
     * Interroge le service métier et traduit ses échecs.
     *
     * <p>Le refus de droit se distingue de la panne, et ce n'est pas un détail : le premier ne
     * cédera jamais à un nouvel essai. Les confondre tous deux en « réessayez dans un instant »
     * ferait recliquer indéfiniment quelqu'un à qui il manque simplement une permission —
     * l'assistant ne voit que ce que la personne voit, et doit le dire quand elle ne voit rien.</p>
     *
     * <p>Le détail part au journal, jamais à l'écran : il porte l'adresse interne du service et
     * des fragments de la requête.</p>
     */
    private <T> T demander(Supplier<T> appel) {
        try {
            return appel.get();
        } catch (FeignException e) {
            log.warn("Le module interrogé a répondu {} : {}", e.status(), e.getMessage());
            if (e.status() == HttpStatus.FORBIDDEN.value() || e.status() == HttpStatus.UNAUTHORIZED.value()) {
                throw new BusinessException(
                        "Vous n'avez pas le droit de consulter ces données. "
                                + "L'assistant ne voit que ce que vous voyez.",
                        HttpStatus.FORBIDDEN);
            }
            throw new BusinessException(
                    "Les données n'ont pas pu être relevées. Réessayez dans un instant.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        } catch (RuntimeException e) {
            log.warn("Le module interrogé n'a pas répondu : {}", e.getMessage());
            throw new BusinessException(
                    "Les données n'ont pas pu être relevées. Réessayez dans un instant.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
