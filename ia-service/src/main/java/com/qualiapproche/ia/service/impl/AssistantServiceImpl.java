package com.qualiapproche.ia.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualiapproche.common.config.PermissionChecker;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.utils.PermissionsPortee;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.ia.dto.DemandeAssistanceDto;
import com.qualiapproche.ia.dto.SuggestionHistoriqueDto;
import com.qualiapproche.ia.dto.SuggestionIaDto;
import com.qualiapproche.ia.entities.SuggestionIa;
import com.qualiapproche.ia.enumeration.VerdictSuggestion;
import com.qualiapproche.ia.repository.SuggestionIaRepository;
import com.qualiapproche.ia.service.AssistantService;
import com.qualiapproche.ia.service.BudgetDeJetons;
import com.qualiapproche.ia.service.ClientDuModele;
import com.qualiapproche.ia.service.PromptRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantServiceImpl implements AssistantService {

    /**
     * Rappel rendu avec chaque suggestion. L'assistant propose, l'utilisateur dispose : la
     * responsabilité du texte retenu reste humaine, et la trace le matérialise.
     */
    public static final String AVERTISSEMENT = "Contenu suggéré par IA — à vérifier avant validation";

    private final PromptRegistry promptRegistry;
    private final SuggestionIaRepository repository;
    private final ObjectMapper objectMapper;
    private final ClientDuModele client;
    private final BudgetDeJetons budget;
    private final PermissionChecker permissionChecker;

    @Override
    public SuggestionIaDto assister(DemandeAssistanceDto demande) {
        budget.exigerDuReste();

        PromptRegistry.PromptVersionne prompt = promptRegistry.promptPour(demande.getTypeAssistance());

        long debut = System.currentTimeMillis();
        ChatResponse reponse = client.repondre(prompt.contenu(), construireMessageUtilisateur(demande));
        long dureeMs = System.currentTimeMillis() - debut;

        String texte = client.texteDe(reponse);
        if (texte == null || texte.isBlank()) {
            throw new BusinessException(
                    "Le modèle n'a produit aucune suggestion.", HttpStatus.BAD_GATEWAY);
        }

        // Une trace n'existe que pour une suggestion réellement produite : un appel en échec
        // n'en laisse aucune, et son décompte de jetons reste nul — le budget ne peut pas être
        // consommé par des pannes.
        SuggestionIa trace = SuggestionIa.builder()
                .typeAssistance(demande.getTypeAssistance())
                .promptVersion(prompt.version())
                .modele(client.modeleDe(reponse))
                .texteSource(demande.getTexteSource())
                .contexte(serialiserContexte(demande.getContexte()))
                .suggestion(texte)
                .ressourceType(demande.getRessourceType())
                .ressourceId(demande.getRessourceId())
                .dureeMs(dureeMs)
                .jetonsUtilises(client.jetonsDe(reponse))
                .build();

        SuggestionIa enregistree = repository.save(trace);
        return SuggestionIaDto.builder()
                .suggestion(texte)
                .suggestionId(enregistree.getId())
                .avertissement(AVERTISSEMENT)
                .build();
    }

    @Override
    public SuggestionHistoriqueDto enregistrerVerdict(UUID id, VerdictSuggestion verdict) {
        SuggestionIa trace = repository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Aucune suggestion ne porte cet identifiant : " + id, HttpStatus.NOT_FOUND));
        exigerSaPropreSuggestion(trace);
        trace.setVerdict(verdict);
        return versHistorique(repository.save(trace));
    }

    @Override
    public Page<SuggestionHistoriqueDto> historique(Pageable pageable) {
        Pageable range = pageable.getSort().isSorted() ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                        Sort.by(Sort.Order.desc("createdAt")));
        return pageDuPerimetre(range).map(this::versHistorique);
    }

    /**
     * Les suggestions que l'appelant a le droit de lire, et elles seules.
     *
     * <p>L'historique rendait jusqu'ici {@code findAll} : toute personne détenant
     * {@code assistant-ia-read} lisait les suggestions de toutes les structures, texte produit
     * compris — soit la matière même des dossiers d'autrui, brouillons inclus, alors que les
     * listes de dossiers, elles, sont bornées depuis longtemps.</p>
     *
     * <p>Le périmètre est celui que le budget décompte déjà : la direction du jeton — sa structure
     * de rattachement quand il ne porte pas de {@code direction_id} — et, à défaut de l'une comme
     * de l'autre, ses propres suggestions. Deux règles pour une seule clé : ce qu'on lit est ce
     * qu'on consomme.</p>
     *
     * <p>{@link PermissionsPortee#TOUTES_STRUCTURES} passe outre, comme pour les tableaux de bord
     * et les listes de dossiers : c'est la même habilitation transverse, et lui en opposer une
     * seconde, propre à l'assistant, ferait diverger deux définitions du même privilège.</p>
     *
     * <p>Sans direction ni utilisateur — un jeton qui ne dirait rien de son porteur —, la liste est
     * vide plutôt qu'entière. C'est l'inverse du budget, qui lève alors toute limite : ne pas
     * savoir qui appelle ne coûte, là, qu'une génération de trop ; ici, cela livrerait tout.</p>
     */
    private Page<SuggestionIa> pageDuPerimetre(Pageable range) {
        if (permissionChecker.detient(PermissionsPortee.TOUTES_STRUCTURES)) {
            return repository.findAll(range);
        }
        UUID directionId = SecurityUtils.getCurrentDirectionId();
        if (directionId != null) {
            return repository.findAllByDirectionId(directionId, range);
        }
        String utilisateurId = SecurityUtils.getCurrentUserId();
        return utilisateurId != null
                ? repository.findAllByCreatedById(utilisateurId, range)
                : Page.empty(range);
    }

    /**
     * Le sort d'une suggestion n'appartient qu'à qui l'a sollicitée.
     *
     * <p>Le verdict n'est pas une donnée de plus : c'est le jugement du demandeur sur le texte
     * qu'on lui a proposé, et la seule matière du taux d'acceptation. Le laisser prononcer par un
     * tiers — il suffisait d'un identifiant — fausserait l'indicateur au lieu de l'enrichir.</p>
     *
     * <p>{@link PermissionsPortee#TOUTES_STRUCTURES} ne dispense pas de cette règle, et c'est
     * voulu : cette habilitation ouvre la lecture transverse, pas la décision à la place d'autrui.
     * Voir n'est pas décider.</p>
     *
     * <p>Refus en 403 et non en 404 : l'appelant tient l'identifiant d'une suggestion qui existe,
     * la dissimulation ne protégerait rien qu'il ne sache déjà.</p>
     */
    private void exigerSaPropreSuggestion(SuggestionIa trace) {
        String utilisateurId = SecurityUtils.getCurrentUserId();
        if (utilisateurId == null || !utilisateurId.equals(trace.getCreatedById())) {
            throw new BusinessException(
                    "Seule la personne qui a sollicité cette suggestion peut en prononcer le sort.",
                    HttpStatus.FORBIDDEN);
        }
    }


    private String construireMessageUtilisateur(DemandeAssistanceDto demande) {
        StringBuilder message = new StringBuilder();
        if (demande.getContexte() != null && !demande.getContexte().isEmpty()) {
            message.append("Contexte :\n");
            demande.getContexte().forEach((cle, valeur) ->
                    message.append("- ").append(cle).append(" : ").append(valeur).append('\n'));
            message.append('\n');
        }
        message.append("Éléments fournis par le demandeur :\n").append(demande.getTexteSource());
        return message.toString();
    }

    private String serialiserContexte(Map<String, String> contexte) {
        if (contexte == null || contexte.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(contexte);
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    "Le contexte fourni n'a pas pu être sérialisé.", HttpStatus.BAD_REQUEST);
        }
    }


    private SuggestionHistoriqueDto versHistorique(SuggestionIa trace) {
        return SuggestionHistoriqueDto.builder()
                .id(trace.getId())
                .typeAssistance(trace.getTypeAssistance())
                .promptVersion(trace.getPromptVersion())
                .modele(trace.getModele())
                .suggestion(trace.getSuggestion())
                .verdict(trace.getVerdict())
                .ressourceType(trace.getRessourceType())
                .ressourceId(trace.getRessourceId())
                .dureeMs(trace.getDureeMs())
                .createdAt(trace.getCreatedAt())
                .build();
    }
}
