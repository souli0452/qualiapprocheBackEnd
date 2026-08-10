package com.qualiapproche.amelioration.service.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.qualiapproche.common.dto.NcCountsDto;
import com.qualiapproche.common.dto.NcDashboardDto;
import com.qualiapproche.common.dto.NcEvolutionDto;
import com.qualiapproche.common.dto.NcStats;
import com.qualiapproche.common.dto.NonConformiteByStructDto;
import com.qualiapproche.common.dto.NonConformiteDto;
import com.qualiapproche.common.dto.PlanActionDto;
import com.qualiapproche.common.dto.StructureDto;
import com.qualiapproche.common.dto.WorkflowInstanceDto;
import com.qualiapproche.common.dto.WorkflowSummaryDto;
import com.qualiapproche.common.dto.WorkflowValidationRequestDto;
import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.mappers.NonConformiteMapper;
import com.qualiapproche.amelioration.entities.mappers.PlanActionMapper;
import com.qualiapproche.amelioration.repository.ActionRepository;
import com.qualiapproche.amelioration.repository.EfficaciteRepository;
import com.qualiapproche.amelioration.repository.NiveauNonConformiteRepository;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.PlanActionRepository;
import com.qualiapproche.amelioration.repository.TypeNonConformiteRepository;
import com.qualiapproche.common.enumeration.Etat;
import com.qualiapproche.common.enumeration.Status;
import com.qualiapproche.common.enumeration.TypeDemande;
import com.qualiapproche.common.enumeration.Circuit;
import com.qualiapproche.amelioration.client.ReferentielClient;
import com.qualiapproche.amelioration.client.WorkflowClient;
import com.qualiapproche.common.service.SendMailService;
import com.qualiapproche.common.utils.RolesPlateforme;
import com.qualiapproche.common.utils.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import com.qualiapproche.amelioration.service.NonConformiteService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import com.qualiapproche.amelioration.specification.NonConformiteSpecification;
import lombok.RequiredArgsConstructor;
import com.qualiapproche.amelioration.repository.PieceJointeRepository;
import com.qualiapproche.common.config.PermissionChecker;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NonConformiteServiceImpl implements NonConformiteService {

    /**
     * Nom du champ d'étape portant le document justificatif d'un rejet.
     *
     * <p>Il est déclaré sous ce nom sur les étapes de rejet du circuit de non-conformité
     * ({@code WorkflowDataInitializer}). Le nom est le seul repère commun entre le circuit et ce
     * service : les identifiants d'étapes diffèrent d'une installation à l'autre.</p>
     */
    private static final String CHAMP_DOC_REJET = "docRejet";

    /**
     * Champs d'étape désignant la structure à qui la non-conformité est confiée.
     *
     * <p>Le passage d'une structure à une autre s'écrivait par une mise à jour ordinaire du
     * dossier : rien ne disait qui l'avait décidé ni quand, et n'importe quel enregistrement
     * pouvait le défaire. Il arrive maintenant par le circuit, avec le reste de la décision.</p>
     */
    private static final String CHAMP_STRUCTURE_DESTINATAIRE_ID = "structureDestinataireId";

    /**
     * Champ d'étape désignant l'agent à qui la non-conformité est imputée.
     *
     * <p>L'imputation passait par une mise à jour du dossier ; elle est maintenant une décision du
     * circuit. Sans cette reprise, l'agent désigné aurait bien été habilité à traiter — le moteur
     * inscrit le titulaire sur l'instance — mais la non-conformité, elle, serait restée sans
     * imputé : elle n'aurait figuré dans aucune de ses listes, et aucun courriel ne l'aurait
     * nommé.</p>
     */
    private static final String CHAMP_AGENT_IMPUTE = "userImputId";

    /**
     * Champ d'étape par lequel le responsable qualité qualifie l'écart : action corrective, ou
     * correction.
     *
     * <p>Ce n'est pas une mention d'affichage : le circuit retenu commande les colonnes que le plan
     * d'action devra porter — une correction remet en conformité sans qu'on ait à rechercher la
     * cause, une action corrective ne vaut que par elle. Il arrive avec la décision qui oriente le
     * dossier, au moment même où le responsable qualité désigne le processus destinataire.</p>
     */
    private static final String CHAMP_CIRCUIT_TRAITEMENT = "circuitTraitement";

    private final NonConformiteRepository nonConformiteRepository;
    private final PieceJointeRepository pieceJointeRepository;
    private final NonConformiteMapper nonConformiteMapper;
    private final PlanActionMapper planActionMapper;
    private final TypeNonConformiteRepository typeNonConformiteRepository;
    private final ReferentielClient referentielClient;
    private final EfficaciteRepository efficaciteRepository;
    private final ActionRepository actionRepository;
    private final NiveauNonConformiteRepository niveauNonConformiteRepository;
    private final PieceJointeStockageService fichierService;
    private final NonConformiteFichierService ncFichierService;
    private final SendMailService sendMailService;
    private final PlanActionRepository planActionRepository;
    private final WorkflowClient workflowClient;
    private final PlansActionDeLaNonConformiteService plansActionService;
    private final PermissionChecker permissionChecker;

    @Value("${frontend.url}")
    private String frontendUrl;

    /**
     * Recherche les entités en base et renvoie une exception si l'ID est invalide.
     */
    private UUID findEfficaciteById(UUID id) {
        return id;
    }

    private UUID findNiveauNonConformiteById(UUID id) {
        return id;
    }

    private UUID findActionById(UUID id) {
        return id;
    }

    private UUID findTypeNonConformiteById(UUID id) {
        return id;
    }

    private UUID findTypeProcessusById(UUID id) {
        return id;
    }

    private NonConformiteDto populateAttachments(NonConformiteDto dto) {
        if (dto == null) {
            return null;
        }
        dto.setFichiers(fichierService.getPjByEntityId(dto.getId()));
        completerLesPlansDAction(dto);
        if (dto.getPlanActions() != null) {
            dto.getPlanActions().forEach(plan -> plan.setFichiers(fichierService.getPjByEntityId(plan.getId())));
        }
        return dto;
    }

    /**
     * Ajoute à la fiche les plans d'action que sa collection ne porte pas.
     *
     * <p>Un plan est rattaché à son dossier de <b>deux</b> façons : par la colonne
     * {@code non_conforme_id}, sur laquelle travaillent tous les services, et par la collection
     * {@code NonConformite.planActions}, qui alimente seule la fiche. Un plan créé par
     * {@code PlanActionService} n'écrivait que la première : il existait, les circuits le
     * pilotaient, la clôture du dossier attendait qu'il soit soldé — et la fiche ne le montrait
     * nulle part. On demandait donc au pilote et au responsable qualité de valider des actions
     * qu'aucun écran ne leur présentait.</p>
     *
     * <p>La colonne fait foi, la collection est complétée à la lecture : cela rétablit aussi les
     * dossiers déjà saisis, sans reprise de données.</p>
     */
    private void completerLesPlansDAction(NonConformiteDto dto) {
        if (dto.getId() == null) {
            return;
        }
        List<PlanActionDto> portes = dto.getPlanActions() == null ? new ArrayList<>() : dto.getPlanActions();
        Set<UUID> connus = portes.stream()
                .map(PlanActionDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<PlanActionDto> absents = planActionRepository.findPlanActionsByNonConformeId(dto.getId()).stream()
                .filter(plan -> !connus.contains(plan.getId()))
                .map(planActionMapper::toDto)
                .toList();
        if (absents.isEmpty()) {
            return;
        }

        List<PlanActionDto> tous = new ArrayList<>(portes);
        tous.addAll(absents);
        dto.setPlanActions(tous);
    }

    /**
     * Crée une nouvelle NonConformité après validation.
     *
     * @param dto Les données de la NonConformité.
     * @return Le NonConformiteDto correspondant.
     */
    @Override
    public NonConformiteDto createNonConformite(NonConformiteDto dto) throws IOException {
        NonConformite nonConformite = nonConformiteMapper.toEntity(dto);
        nonConformite.setTypeDemande(TypeDemande.NON_CONFORMITE);
        nonConformite.setVersion("1.0");
        nonConformite.setOriginNonConformiteLibelle(dto.getOriginNonConformiteLibelle());
        nonConformite.setNumeroReference(genererNumeroReference(dto.getStructureSoumissionId(), dto.getStructureSoumissionLibelle()));
        nonConformite.setStructureSoumissionId(dto.getStructureSoumissionId());
        nonConformite.setStructureSoumissionLibelle(dto.getStructureSoumissionLibelle());
        nonConformite.setOrigineId(null);
        nonConformite.setOrigineService(null);
        nonConformite.setOrigineServiceLibelleCourt(null);
        nonConformite.setNiveauNonConformiteId(findNiveauNonConformiteById(dto.getNiveauNonConformiteId()));
        nonConformite.setTypeNonConformiteId(findTypeNonConformiteById(dto.getTypeNonConformiteId()));
        nonConformite.setTypeProcessusId(findTypeProcessusById(dto.getTypeProcessusId()));
        // État de départ, avant toute transition : le circuit prendra la suite. Les deux branches
        // se réaffectaient la valeur qu'elles venaient de lire, ce qui masquait la règle.
        if (nonConformite.getEtatTraitement() == null) {
            nonConformite.setEtatTraitement(Etat.SOUMISSION);
        }
        if (nonConformite.getStatus() == null) {
            nonConformite.setStatus(Status.DRAFT);
        }
            nonConformite.setDateVisaEmetteur(LocalDateTime.now());


        // Sauvegarder la NonConformité avec ses PlanActions automatiquement persistées
        NonConformite savedNonConformite = nonConformiteRepository.save(nonConformite);

        // Initialiser le workflow : une indisponibilité de workflow-service ne doit pas bloquer
        // la création de la non-conformité (aligné sur la politique déjà utilisée pour PlanAction).
        // La non-conformité reste alors sans workflowId ; un correctif ultérieur pourra prévoir
        // une reprise automatique (ex. tâche planifiée) sur les dossiers sans workflow associé.
        try {
            WorkflowSummaryDto activeWorkflow = workflowClient.getActiveWorkflowByType("NON_CONFORMITE");
            UUID workflowId = activeWorkflow.getId();
            savedNonConformite.setWorkflowId(workflowId);

            WorkflowInstanceDto workflowInstance = workflowClient.initiateWorkflow(
                    savedNonConformite.getId(),
                    "NON_CONFORMITE",
                    workflowId,
                    // La référence lisible : c'est elle que citeront les courriels d'étape.
                    savedNonConformite.getNumeroReference()
            );

            if (workflowInstance != null && workflowInstance.getCurrentStateName() != null) {
                savedNonConformite.setWorkflowStatus(workflowInstance.getCurrentStateName());
            }
            savedNonConformite = nonConformiteRepository.save(savedNonConformite);
        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation du workflow NON_CONFORMITE pour {} : {}",
                    savedNonConformite.getId(), e.getMessage());
        }

        ncFichierService.synchroniser(dto.getFichiers(), savedNonConformite.getId());

        return populateAttachments(nonConformiteMapper.toDto(savedNonConformite));
    }

    /**
     * Soumet au pilote du processus une non-conformité déjà enregistrée.
     *
     * <p>L'agent décrivait son constat, quittait l'écran, puis devait retrouver son dossier dans une
     * liste pour le soumettre — alors qu'il n'avait le plus souvent rien à y ajouter. Le brouillon
     * reste possible, et utile à qui veut relire ou compléter sa description plus tard : il cesse
     * seulement d'être un passage obligé.</p>
     *
     * <p>La décision passe par le moteur comme toutes les autres, sans quoi il n'y aurait ni
     * historique, ni courriel au pilote, ni habilitation vérifiée. Elle est jouée sous le jeton de
     * l'agent, et l'étape de soumission est réservée à l'auteur du dossier.</p>
     *
     * <p><b>Hors transaction, délibérément.</b> Le moteur, une fois sa propre décision committée,
     * revient vers ce service par un appel HTTP pour lui dire quelle étape le dossier a atteinte.
     * Cet appel ouvre sa propre transaction : appelée depuis celle qui vient d'écrire le dossier,
     * elle n'y aurait pas trouvé de dossier du tout, et l'étape n'aurait été inscrite qu'à la
     * prochaine reprise de l'ordonnanceur.</p>
     *
     * <p>Un échec n'annule pas l'enregistrement : le constat est écrit, il ne doit pas se perdre
     * parce que le circuit n'a pas répondu. Le dossier reste alors en brouillon, là où l'agent le
     * retrouvera, et le journal dit pourquoi.</p>
     */
    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public NonConformiteDto soumettre(UUID id) {
        NonConformite nc = nonConformiteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Non conformité introuvable"));

        if (nc.getWorkflowId() == null) {
            log.warn("Non-conformité {} enregistrée sans circuit : elle ne peut pas être soumise "
                    + "immédiatement et reste en brouillon.", id);
            return populateAttachments(nonConformiteMapper.toDto(nc));
        }

        try {
            WorkflowValidationRequestDto soumission = new WorkflowValidationRequestDto();
            // Toute décision porte un commentaire, et l'historique le montre. Celle-ci n'en a pas
            // de saisi : dire d'où elle vient vaut mieux qu'une ligne muette dans le fil du dossier.
            soumission.setComments("Soumise par son auteur au moment de l'enregistrement.");
            workflowClient.validateStep(id, null, soumission);
            log.info("Non-conformité {} soumise dès son enregistrement.", id);
        } catch (Exception e) {
            log.error("Non-conformité {} enregistrée, mais non soumise : {}. Elle reste en brouillon.",
                    id, e.getMessage());
        }

        // Relue plutôt que rendue de mémoire : le moteur vient d'inscrire sur le dossier l'étape
        // atteinte, et l'objet que nous tenions date d'avant — l'écran aurait annoncé un brouillon
        // là où le dossier est parti chez le pilote.
        return populateAttachments(nonConformiteMapper.toDto(
                nonConformiteRepository.findById(id).orElse(nc)));
    }

    @Override
    public NonConformiteDto updateNonConformite(UUID id, NonConformiteDto dto) throws IOException {
        // Vérifier si la non-conformité existe
        NonConformite existingNonConformite = nonConformiteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Non-conformité non trouvée avec l'ID : " + id));
        // Mise à jour des champs modifiables
        existingNonConformite.setEfficaciteId(findEfficaciteById(dto.getEfficaciteId()));
        existingNonConformite.setNiveauNonConformiteId(findNiveauNonConformiteById(dto.getNiveauNonConformiteId()));
        existingNonConformite.setActionId(findActionById(dto.getActionId()));
        existingNonConformite.setTypeNonConformiteId(findTypeNonConformiteById(dto.getTypeNonConformiteId()));
        existingNonConformite.setTypeProcessusId(findTypeProcessusById(dto.getTypeProcessusId()));
        existingNonConformite.setCircuit(dto.getCircuit());
        existingNonConformite.setOrigineId(dto.getOrigineId());
        existingNonConformite.setOrigineService(dto.getOrigineService());
        existingNonConformite.setOrigineServiceLibelleCourt(dto.getOrigineServiceLibelleCourt());
        existingNonConformite.setActionLibelle(dto.getActionLibelle());

        existingNonConformite.setUserImputId(dto.getUserImputId());
        existingNonConformite.setUserImputFullName(dto.getUserImputFullName());
        appliquerLesParticipants(existingNonConformite, dto);
        // Mettre à jour les fichiers s'ils sont fournis
        ncFichierService.synchroniser(dto.getFichiers(), id);
        // Les plans d'action ne se sauvegardent plus avec la fiche. Ils étaient recomposés en bloc
        // depuis ce qu'envoyait l'écran, sur une collection en `orphanRemoval` : un client qui en
        // omettait un le supprimait, et l'enregistrement d'une fiche pouvait changer le responsable
        // d'un plan engagé ou effacer une action déjà confiée — contournant les contrôles que
        // PlanActionService pose sur ces deux gestes précisément. Un plan se crée, se corrige et se
        // retire par ses propres points d'entrée, qui savent ce qu'un engagement interdit.
        // Sauvegarde de la mise à jour
        NonConformite updatedNonConformite = nonConformiteRepository.save(existingNonConformite);
        // Retour DTO
        return populateAttachments(nonConformiteMapper.toDto(updatedNonConformite));
    }

    /**
     * Inscrit sur le dossier les personnes ayant pris part à l'analyse.
     *
     * <p>Le champ existait en base et s'affichait sur la fiche, mais aucun point d'entrée ne
     * l'écrivait : il était donc vide partout, et la rubrique « Participants » restait invisible sur
     * tous les dossiers. C'est pourtant ce qui dit qui a cherché les causes — une non-conformité
     * s'analyse rarement seul, et la traçabilité de l'analyse en fait partie.</p>
     *
     * <p><b>Absent n'est pas vide.</b> Tous les écrans qui enregistrent la fiche ne montrent pas les
     * participants : {@code null} laisse en place ceux qui y sont, une liste vide les retire — c'est
     * une saisie, pas une omission. Même règle que pour les pièces jointes, et pour la même raison.</p>
     */
    private void appliquerLesParticipants(NonConformite nc, NonConformiteDto dto) {
        if (dto.getParticipants() == null) {
            return;
        }
        if (nc.getParticipants() == null) {
            nc.setParticipants(new com.qualiapproche.common.base.Participants());
        }
        nc.getParticipants().setFullNames(new java.util.HashSet<>(dto.getParticipants()));
    }

    @Override
    public List<NonConformiteDto> updateNonConformites(List<NonConformiteDto> dtos) throws IOException {
        dtos.forEach(dto -> {
            NonConformite existingNonConformite = nonConformiteRepository.findById(dto.getId())
                    .orElseThrow(
                            () -> new EntityNotFoundException("Non-conformité non trouvée avec l'ID : " + dto.getId()));
            existingNonConformite.setPertinanceRs(dto.getPertinanceRs());
            existingNonConformite.setJustificationPilote(dto.getJustificationPilote());
            existingNonConformite.setPertinancePilote(dto.getPertinancePilote());
            existingNonConformite.setJustificationRs(dto.getJustificationRs());
            existingNonConformite.setEfficaciteId(findEfficaciteById(dto.getEfficaciteId()));
            existingNonConformite.setNiveauNonConformiteId(findNiveauNonConformiteById(dto.getNiveauNonConformiteId()));
            existingNonConformite.setTypeNonConformiteId(findTypeNonConformiteById(dto.getTypeNonConformiteId()));
            existingNonConformite.setTypeProcessusId(findTypeProcessusById(dto.getTypeProcessusId()));
            if (dto.getCircuit() != null) {
                existingNonConformite.setCircuit(dto.getCircuit());
            }
            if (dto.getOrigineId() != null) {
                existingNonConformite.setOrigineId(dto.getOrigineId());
            }
            if (dto.getOrigineService() != null) {
                existingNonConformite.setOrigineService(dto.getOrigineService());
            }
            if (dto.getOrigineServiceLibelleCourt() != null) {
                existingNonConformite.setOrigineServiceLibelleCourt(dto.getOrigineServiceLibelleCourt());
            }

            existingNonConformite.setUserImputId(dto.getUserImputId());
            existingNonConformite.setUserImputeEmail(dto.getUserImputeEmail());
            existingNonConformite.setUserImputFullName(dto.getUserImputFullName());
            existingNonConformite.setPertinanceRs(dto.getPertinanceRs());
            existingNonConformite.setActionPreventive(dto.getActionPreventive());
            existingNonConformite.setPertinanceRsSuivi(dto.getPertinanceRsSuivi());
            existingNonConformite.setNumeroFdac(dto.getNumeroFdac());

            ncFichierService.synchroniser(dto.getFichiers(), dto.getId());

            existingNonConformite.setDelaisMiseOeuvre(dto.getDelaisMiseOeuvre());

            if (dto.getParticipants() != null && !dto.getParticipants().isEmpty()) {
                dto.getParticipants().forEach(participant -> {
                    existingNonConformite.getParticipants().getFullNames().add(participant);
                });
            }
            existingNonConformite.setUserImputFullName(dto.getUserImputFullName());
            // Les actions correctives ne se réécrivent plus depuis la fiche. Ce bloc vidait la
            // collection — en `orphanRemoval`, cela les **supprimait** — puis en recréait à partir
            // de ce que l'écran renvoyait : chaque enregistrement effaçait le responsable, le
            // critère et le constat d'efficacité, l'observation, et jusqu'au rattachement au
            // circuit, remplaçant les actions par des jumelles amnésiques portant de nouveaux
            // identifiants. Une action se crée, se corrige et se retire par PlanActionService, qui
            // sait ce qu'un engagement interdit.
            nonConformiteRepository.save(existingNonConformite);
        });
        return dtos.stream().map(this::populateAttachments).toList();
    }

    @Override
    public NonConformiteDto update(NonConformiteDto nonConformiteDto) {
        return nonConformiteRepository.findById(nonConformiteDto.getId()).map(nonConformiteExisted -> {
            nonConformiteMapper.updateEntityFromDto(nonConformiteDto, nonConformiteExisted);
            return populateAttachments(nonConformiteMapper.toDto((nonConformiteExisted)));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.OK, "Aucune NonConformité trouvée."));
    }

    @Override
    public Page<NonConformiteDto> allNonConformites(Pageable pageable) {
        return visiblesParLAppelant(pageable)
                .map(nc -> populateAttachments(nonConformiteMapper.toDto(nc)));
    }

    /**
     * Non-conformités que l'appelant a le droit de voir.
     *
     * <p>Ce point d'entrée rendait la table entière : la restriction n'existait que dans le choix
     * des écrans, si bien qu'un agent obtenait les dossiers de toutes les structures en demandant
     * simplement la liste générale. Elle est désormais appliquée par le service, seul endroit qui
     * la garantisse.</p>
     *
     * <p>L'administration et la responsabilité qualité continuent de tout voir : leur fonction est
     * transverse. Voir, et non décider — l'habilitation des étapes, elle, reste entière.</p>
     */
    private Page<NonConformite> visiblesParLAppelant(Pageable pageable) {
        if (permissionChecker.detient(RolesPlateforme.PORTEE_GLOBALE.toArray(String[]::new))) {
            return nonConformiteRepository.findAll(pageable);
        }
        return nonConformiteRepository.findVisiblesPar(
                SecurityUtils.getCurrentUserStructureId(), SecurityUtils.getCurrentUserId(), pageable);
    }

    @Override
    public Page<NonConformiteDto> findImupted(String userId, Etat etat, Pageable pageable) {
        return nonConformiteRepository.findByUserImputIdAndEtatTraitement(userId, etat, pageable)
                .map(nc -> populateAttachments(nonConformiteMapper.toDto(nc)));
    }

    @Override
    public Page<NonConformiteDto> getNonConformitesByEtatNonConformite(Etat etat, Pageable pageable) {
        return nonConformiteRepository.findByEtatTraitement(etat, pageable)
                .map(nc -> populateAttachments(nonConformiteMapper.toDto(nc)));
    }

    /**
     * Non-conformités sur lesquelles l'appelant a une décision ouverte.
     *
     * <p>Les identifiants viennent du moteur, qui applique l'habilitation de l'étape courante.
     * Aucune règle de visibilité n'est rejouée ici : elle serait la seconde, et les deux
     * divergeraient.</p>
     *
     * <p>Le moteur hors d'atteinte rend une liste vide plutôt qu'une erreur : mieux vaut une liste
     * de travail momentanément vide, qui se remplira au rétablissement, qu'un écran en échec.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public Page<NonConformiteDto> aTraiterParLAppelant(Pageable pageable) {
        List<UUID> aDecider;
        try {
            aDecider = workflowClient.ressourcesADecider("NON_CONFORMITE");
        } catch (Exception e) {
            log.warn("Liste « à traiter » indisponible, le service de workflow est injoignable : {}", e.getMessage());
            return Page.empty(pageable);
        }

        if (aDecider == null || aDecider.isEmpty()) {
            return Page.empty(pageable);
        }
        return nonConformiteRepository.findByIdIn(aDecider, pageable)
                .map(nc -> populateAttachments(nonConformiteMapper.toDto(nc)));
    }

    @Override
    public Page<NonConformiteDto> getNonConformitesByEtatAnStructure(Etat etat, String uuid, Pageable pageable) {
        return nonConformiteRepository.findAllByEtatTraitementAndStructureSoumissionId(etat, uuid, pageable)
                .map(nc -> populateAttachments(nonConformiteMapper.toDto(nc)));
    }

    @Override
    public Page<NonConformiteDto> getNonConformitesByStructure(String uuid, Pageable pageable) {
        return nonConformiteRepository.findAllByOrigineId(uuid, pageable)
                .map(nc -> populateAttachments(nonConformiteMapper.toDto(nc)));
    }

    @Override
    public Page<NonConformiteDto> getNonConformitesByEtatAndStructureOrigine(Etat etat, String uuid, Pageable pageable) {
        return nonConformiteRepository.findAllByEtatTraitementAndOrigineId(etat, uuid, pageable)
                .map(nc -> populateAttachments(nonConformiteMapper.toDto(nc)));
    }

    @Override
    public NonConformiteDto getNonConformiteById(UUID id) {
        if (nonConformiteRepository.existsById(id)) {
            return populateAttachments(nonConformiteMapper.toDto(nonConformiteRepository.getReferenceById(id)));
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cette NonConformité n'existe pas.");
        }
    }

    @Override

    public void delete(UUID id) {
        if (nonConformiteRepository.existsById(id)) {
            nonConformiteRepository.deleteById(id);
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Cette NonConformité n'existe pas.");
        }
    }

    public void deleteMultiple(List<NonConformiteDto> nonConformiteDtos) {
        nonConformiteDtos.forEach(actualityDto -> {
            if (!nonConformiteRepository.existsById(actualityDto.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Actualité invalide, impossible de supprimer.");
            }
            nonConformiteRepository.deleteById(actualityDto.getId());
        });
    }

    @Transactional(readOnly = true)
    public List<NcStats> getNcStats(String structureSoumissionId) {
        return nonConformiteRepository.countByStatusForStructure(structureSoumissionId);
    }

    @Transactional(readOnly = true)
    public Page<NonConformiteDto> findAll(final Status status, final String structureSoumissionId, Pageable pageable) {
        log.debug("Request to get all Actualities");

        Page<NonConformite> nonConformites;

        if (Objects.nonNull(status)) {
            // Note: complex logic combining multiple status queries should ideally be one repository query.
            // For now, we will just use the exact status or fallback to findAll if not possible cleanly with Pageable.
            nonConformites = nonConformiteRepository.findAllByStatusAndStructureSoumissionId(status,
                    structureSoumissionId, pageable);
        } else {
            nonConformites = visiblesParLAppelant(pageable);
        }

        return nonConformites.map(actuality -> {
            NonConformiteDto nonConformiteDto = nonConformiteMapper.toDto(actuality);
            return populateAttachments(nonConformiteDto);
        });
    }

    @Override
    public Page<NonConformiteDto> findAllByStructure(String structureSoumissionId, Pageable pageable) {
        // Ideally this should be a single query like: findAllByStructureSoumissionIdOrOrigineIdAndStatusIsNot(..., pageable)
        // We will just use findAllByStructureSoumissionIdOrOrigineId for now
        Page<NonConformite> nonConformites = nonConformiteRepository
                .findAllByStructureSoumissionIdOrOrigineId(structureSoumissionId, structureSoumissionId, pageable);

        return nonConformites.map(nc -> populateAttachments(nonConformiteMapper.toDto(nc)));
    }

    public String genererNumeroReference(String structureSoumissionId, String structureSoumissionLibelle) {
        final String prefix = "NFQT";
        final int annee = LocalDate.now().getYear();

        Integer dernierNumero = nonConformiteRepository.findDernierNumero(structureSoumissionId, annee);
        int nouveauNumero = (dernierNumero != null) ? dernierNumero + 1 : 1;

        String sigle = structureSoumissionLibelle;
        if (structureSoumissionId != null) {
            try {
                StructureDto structure = referentielClient.getStructureById(UUID.fromString(structureSoumissionId));
                if (structure != null && structure.getLibelleCourt() != null) {
                    sigle = structure.getLibelleCourt();
                }
            } catch (Exception e) {
                log.error("Error fetching structure sigle for reference generation: {}", e.getMessage());
            }
        }

        String numeroFormate = String.format("%05d", nouveauNumero);
        return String.format("%s-%s-%d-%s", prefix, sigle.toUpperCase().replaceAll("\\s+", "_"), annee, numeroFormate);
    }

    @Override
    public Map<String, Long> getNonConformiteStatsByStructure(int annee) {
        LocalDateTime debutAnnee = LocalDateTime.of(annee, 1, 1, 0, 0, 0);
        LocalDateTime finAnnee = LocalDateTime.of(annee, 12, 31, 23, 59, 59, 999999999);

        // 2. Appel du repository avec les dates précises
        return nonConformiteRepository
                .getNonConformiteStatsByStructureAndPeriod(debutAnnee, finAnnee)
                .stream()
                .collect(Collectors.toMap(
                        NonConformiteByStructDto::getOrigineServiceLibelleCourt,
                        NonConformiteByStructDto::getCount));
    }

    @Override
    public Map<String, Map<String, Long>> getStatsParAnnee(int annee) {
        Map<String, Long> stats = new LinkedHashMap<>();

        // Initialiser tous les mois à 0
        List<String> mois = Arrays.asList(
                "janvier", "février", "mars", "avril", "mai", "juin",
                "juillet", "août", "septembre", "octobre", "novembre", "décembre");
        mois.forEach(m -> stats.put(m, 0L));

        // Remplir avec les données de la base
        nonConformiteRepository.countByMonth(annee).forEach(row -> {
            int moisIndex = ((Number) row[0]).intValue() - 1;
            long count = ((Number) row[1]).longValue();
            stats.put(mois.get(moisIndex), count);
        });

        return Map.of(String.valueOf(annee), stats);
    }

    public Map<String, Map<String, Map<String, Long>>> getStatsDetailleesParAnnee(int annee) {
        // 1. Plage temporelle
        LocalDateTime debutAnnee = LocalDateTime.of(annee, 1, 1, 0, 0, 0);
        LocalDateTime finAnnee = LocalDateTime.of(annee, 12, 31, 23, 59, 59, 999_999_999);

        // 2. Debug: Vérifier les données existantes
        log.debug("Vérification des statuts en base:");
        nonConformiteRepository.countStatusForDebug(debutAnnee, finAnnee)
                .forEach(row -> log.debug("Statut: {}, Count: {}", row[0], row[1]));

        // 3. Statuts à inclure (convertir les enums en String)
        List<String> statutsFiltres = Arrays.stream(Status.values())
                .filter(s -> Set.of(
                        Status.APPROVED,
                        Status.REJECTED,
                        Status.IN_PROGRESS).contains(s))
                .map(Enum::name)
                .collect(Collectors.toList());

        // 4. Initialisation de la structure de réponse
        Map<String, Map<String, Long>> statsMensuelles = new LinkedHashMap<>();
        List<String> nomsMois = List.of("janvier", "février", "mars", "avril", "mai", "juin",
                "juillet", "août", "septembre", "octobre", "novembre", "décembre");

        nomsMois.forEach(mois -> {
            Map<String, Long> stats = new LinkedHashMap<>();
            statutsFiltres.forEach(statut -> stats.put(statut, 0L));
            statsMensuelles.put(mois, stats);
        });

        // 5. Récupération des données
        List<Object[]> resultats = nonConformiteRepository.countByMonthAndStatus(
                debutAnnee,
                finAnnee,
                statutsFiltres);

        log.debug("Résultats de la requête:");
        resultats.forEach(row -> log.debug("Mois: {}, Statut: {}, Count: {}", row[0], row[1], row[2]));

        // 6. Traitement des résultats
        resultats.forEach(row -> {
            try {
                int moisIndex = ((Number) row[0]).intValue() - 1;
                String statut = ((String) row[1]).toUpperCase();
                long count = ((Number) row[2]).longValue();

                if (moisIndex >= 0 && moisIndex < nomsMois.size()) {
                    String mois = nomsMois.get(moisIndex);
                    statsMensuelles.get(mois).put(statut, count);
                }
            } catch (Exception e) {
                log.error("Erreur traitement ligne: {}", Arrays.toString(row), e);
            }
        });

        return Map.of(String.valueOf(annee), statsMensuelles);
    }

    @Override
    public NonConformiteDto getByNumeroRef(String numeroRef) {
        return populateAttachments(
                nonConformiteMapper.toDto(nonConformiteRepository.getNonConformiteByNumeroReference(numeroRef)));
    }

    private static final List<String> STATUTS = Arrays.asList(
            "APPROVED", "REJECTED", "IN_PROGRESS");

    private static final List<String> MOIS = Arrays.asList(
            "janvier", "février", "mars", "avril", "mai", "juin",
            "juillet", "août", "septembre", "octobre", "novembre", "décembre");

    @Override
    public Map<String, Map<String, Long>> getStatsMensuellesParService(int annee, String origineServiceId) {
        // 1. Définir la plage temporelle exacte pour l'année
        LocalDateTime debutAnnee = LocalDateTime.of(annee, 1, 1, 0, 0, 0);
        LocalDateTime finAnnee = LocalDateTime.of(annee, 12, 31, 23, 59, 59, 999_999_999);

        // 2. Récupérer les données
        List<Object[]> resultats = nonConformiteRepository.countByMonthAndService(
                debutAnnee,
                finAnnee,
                origineServiceId);

        // 3. Initialiser la structure de réponse
        Map<String, Long> statsParMois = new LinkedHashMap<>();
        List<String> nomsMois = List.of(
                "janvier", "février", "mars", "avril", "mai", "juin",
                "juillet", "août", "septembre", "octobre", "novembre", "décembre");

        // Initialiser tous les mois à 0
        nomsMois.forEach(mois -> statsParMois.put(mois, 0L));

        // 4. Peupler les résultats
        resultats.forEach(row -> {
            try {
                int moisIndex = ((Number) row[0]).intValue() - 1; // Conversion 1-12 → 0-11
                long count = ((Number) row[1]).longValue();

                if (moisIndex >= 0 && moisIndex < nomsMois.size()) {
                    statsParMois.put(nomsMois.get(moisIndex), count);
                }
            } catch (Exception e) {
                log.error("Erreur traitement ligne statistiques: {}", Arrays.toString(row), e);
            }
        });

        // 5. Structurer la réponse finale
        return Collections.singletonMap(String.valueOf(annee), statsParMois);
    }

    @Override
    public Map<String, Map<String, Map<String, Long>>> getStatsDetailleesServiceParAnnee(int annee,
            String origineServiceId) {
        // 1. Plage temporelle
        LocalDateTime debutAnnee = LocalDateTime.of(annee, 1, 1, 0, 0, 0);
        LocalDateTime finAnnee = LocalDateTime.of(annee, 12, 31, 23, 59, 59, 999_999_999);

        // 2. Debug: Vérifier les données existantes
        log.debug("Vérification des statuts en base:");
        nonConformiteRepository.countStatusForDebug(debutAnnee, finAnnee)
                .forEach(row -> log.debug("Statut: {}, Count: {}", row[0], row[1]));

        // 3. Statuts à inclure (convertir les enums en String)
        List<String> statutsFiltres = Arrays.stream(Status.values())
                .filter(s -> Set.of(
                        Status.APPROVED,
                        Status.REJECTED,
                        Status.IN_PROGRESS).contains(s))
                .map(Enum::name)
                .collect(Collectors.toList());

        // 4. Initialisation de la structure de réponse
        Map<String, Map<String, Long>> statsMensuelles = new LinkedHashMap<>();
        List<String> nomsMois = List.of("janvier", "février", "mars", "avril", "mai", "juin",
                "juillet", "août", "septembre", "octobre", "novembre", "décembre");

        nomsMois.forEach(mois -> {
            Map<String, Long> stats = new LinkedHashMap<>();
            statutsFiltres.forEach(statut -> stats.put(statut, 0L));
            statsMensuelles.put(mois, stats);
        });

        // 5. Récupération des données
        List<Object[]> resultats = nonConformiteRepository.countByMonthAndStatus(
                debutAnnee,
                finAnnee,
                statutsFiltres);

        log.debug("Résultats de la requête:");
        resultats.forEach(row -> log.debug("Mois: {}, Statut: {}, Count: {}", row[0], row[1], row[2]));

        // 6. Traitement des résultats
        resultats.forEach(row -> {
            try {
                int moisIndex = ((Number) row[0]).intValue() - 1;
                String statut = ((String) row[1]).toUpperCase();
                long count = ((Number) row[2]).longValue();

                if (moisIndex >= 0 && moisIndex < nomsMois.size()) {
                    String mois = nomsMois.get(moisIndex);
                    statsMensuelles.get(mois).put(statut, count);
                }
            } catch (Exception e) {
                log.error("Erreur traitement ligne: {}", Arrays.toString(row), e);
            }
        });

        return Map.of(String.valueOf(annee), statsMensuelles);
    }

    @Override
    public Map<String, Map<String, Map<String, Long>>> getStatsNiveauParAnnee(int annee, String origineServiceId) {
        LocalDateTime debutAnnee = LocalDateTime.of(annee, 1, 1, 0, 0, 0);
        LocalDateTime finAnnee = LocalDateTime.of(annee, 12, 31, 23, 59, 59, 999_999_999);

        // 2. Récupération de tous les niveaux existants
        List<String> tousNiveaux = niveauNonConformiteRepository.findAllLibelles();
        log.debug("Niveaux disponibles: {}", tousNiveaux);

        // 3. Initialisation de la structure de réponse
        Map<String, Map<String, Long>> statsMensuelles = new LinkedHashMap<>();
        List<String> nomsMois = List.of("janvier", "février", "mars", "avril", "mai", "juin",
                "juillet", "août", "septembre", "octobre", "novembre", "décembre");

        nomsMois.forEach(mois -> {
            Map<String, Long> stats = new LinkedHashMap<>();
            tousNiveaux.forEach(niveau -> stats.put(niveau, 0L));
            statsMensuelles.put(mois, stats);
        });

        // 4. Récupération des données
        List<Object[]> resultats = nonConformiteRepository.countByMonthAndNiveau(
                debutAnnee,
                finAnnee,
                origineServiceId);

        // 5. Traitement des résultats
        resultats.forEach(row -> {
            try {
                int moisIndex = ((Number) row[0]).intValue() - 1;
                String niveau = (String) row[1];
                long count = ((Number) row[2]).longValue();

                if (moisIndex >= 0 && moisIndex < nomsMois.size() && tousNiveaux.contains(niveau)) {
                    String mois = nomsMois.get(moisIndex);
                    statsMensuelles.get(mois).put(niveau, count);
                }
            } catch (Exception e) {
                log.error("Erreur traitement ligne: {}", Arrays.toString(row), e);
            }
        });

        return Map.of(String.valueOf(annee), statsMensuelles);

    }

    @Override
    public Page<NonConformiteDto> findAllByInitiator(String userId, Pageable pageable) {
        return nonConformiteRepository.findAllByCreatedById(userId, pageable)
                .map(nonConformiteMapper::toDto)
                .map(this::populateAttachments);
    }

    @Override
    public Page<NonConformiteDto> findByUser(String userId, Pageable pageable) {
        return nonConformiteRepository.findAllByCreatedById(userId, pageable)
                .map(nc -> populateAttachments(nonConformiteMapper.toDto(nc)));
    }

    @Override
    public Page<NonConformiteDto> findImputedByUser(String userId, Pageable pageable) {
        return nonConformiteRepository.findAllByUserImputId(userId, pageable)
                .map(nc -> populateAttachments(nonConformiteMapper.toDto(nc)));
    }

    @Override
    public Page<NonConformiteDto> findArchivedByUser(String userId, Pageable pageable) {
        return nonConformiteRepository.findAllByCreatedByIdAndStatus(userId, Status.ARCHIVED, pageable)
                .map(nc -> populateAttachments(nonConformiteMapper.toDto(nc)));
    }

    @Override
    public NcCountsDto getCountsByUser(String userId) {
        return NcCountsDto.builder()
                .brouillons(nonConformiteRepository.countByCreatedByIdAndStatus(userId, Status.DRAFT))
                .imputees(nonConformiteRepository.countByUserImputId(userId))
                .archives(nonConformiteRepository.countByCreatedByIdAndStatus(userId, Status.ARCHIVED))
                .build();
    }

    @Override
    public Page<NonConformiteDto> findByStructure(String structureId, Pageable pageable) {
        return nonConformiteRepository.findAllByStructureSoumissionIdOrOrigineId(structureId, structureId, pageable)
                .map(nc -> populateAttachments(nonConformiteMapper.toDto(nc)));
    }

    @Override
    public Page<NonConformiteDto> findByStructureAllUsers(String structureId, Pageable pageable) {
        return nonConformiteRepository.findAllByCurrentUserStructure(structureId, pageable)
                .map(nc -> populateAttachments(nonConformiteMapper.toDto(nc)));
    }

    @Override
    public NcDashboardDto getDashboardRQ() {
        List<NonConformite> all = nonConformiteRepository.findAll();
        return buildDashboardDto(all);
    }

    @Override
    public NcDashboardDto getDashboardPilot(String structureId) {
        List<NonConformite> all = nonConformiteRepository.findAllByStructureSoumissionIdOrOrigineId(structureId, structureId, Pageable.unpaged())
                .getContent();
        return buildDashboardDto(all);
    }

    @Override
    public NcDashboardDto getDashboardUser(String userId) {
        List<NonConformite> all = nonConformiteRepository.findAllByUserInvolved(userId, Pageable.unpaged()).getContent();
        return buildDashboardDto(all);
    }

    private NcDashboardDto buildDashboardDto(List<NonConformite> ncs) {
        Map<Status, Long> statsByStatus = ncs.stream()
                .filter(nc -> nc.getStatus() != null)
                .collect(Collectors.groupingBy(NonConformite::getStatus, Collectors.counting()));

        Map<Status, Map<String, Long>> statsByStatusAndGravity = ncs.stream()
                .filter(nc -> nc.getStatus() != null && nc.getNiveauNonConformiteLibelle() != null)
                .collect(Collectors.groupingBy(
                        NonConformite::getStatus,
                        Collectors.groupingBy(NonConformite::getNiveauNonConformiteLibelle, Collectors.counting())
                ));

        return NcDashboardDto.builder()
                .totalNC(ncs.size())
                .statsByStatus(statsByStatus)
                .statsByStatusAndGravity(statsByStatusAndGravity)
                .build();
    }

    @Override
    public NcEvolutionDto getNcEvolutionStats(int annee, Integer mois, String structureId) {
        if (structureId != null && (structureId.trim().isEmpty() || "null".equalsIgnoreCase(structureId.trim()))) {
            structureId = null;
        }

        List<String> tousNiveaux = niveauNonConformiteRepository.findAllLibelles();
        if (tousNiveaux == null) {
            tousNiveaux = new ArrayList<>();
        }

        long totalEvolution;
        double pct = 0.0;
        List<String> labels;
        List<NcEvolutionDto.DatasetDto> datasets = new ArrayList<>();
        Map<String, Long> gravityCounts = new LinkedHashMap<>();

        for (String libelle : tousNiveaux) {
            gravityCounts.put(libelle, 0L);
        }

        if (mois == null) {
            totalEvolution = nonConformiteRepository.countTotalByYear(annee, structureId);
            long countPrev = nonConformiteRepository.countTotalByYear(annee - 1, structureId);

            if (countPrev > 0) {
                pct = ((double) (totalEvolution - countPrev) / countPrev) * 100.0;
            } else if (totalEvolution > 0) {
                pct = 100.0;
            }

            labels = List.of("Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Jul", "Août", "Sep", "Oct", "Nov", "Déc");

            for (String libelle : tousNiveaux) {
                datasets.add(NcEvolutionDto.DatasetDto.builder()
                        .label(libelle)
                        .data(new ArrayList<>(Collections.nCopies(12, 0L)))
                        .build());
            }

            List<Object[]> results = nonConformiteRepository.getEvolutionStatsByYear(annee, structureId);
            for (Object[] row : results) {
                if (row[0] == null || row[1] == null || row[2] == null) {
                    continue;
                }
                int moisVal = ((Number) row[0]).intValue();
                String gravite = (String) row[1];
                long count = ((Number) row[2]).longValue();

                int moisIdx = moisVal - 1;
                if (moisIdx >= 0 && moisIdx < 12) {
                    for (NcEvolutionDto.DatasetDto dataset : datasets) {
                        if (dataset.getLabel().equals(gravite)) {
                            dataset.getData().set(moisIdx, count);
                            break;
                        }
                    }
                    if (gravityCounts.containsKey(gravite)) {
                        gravityCounts.put(gravite, gravityCounts.get(gravite) + count);
                    }
                }
            }
        } else {
            totalEvolution = nonConformiteRepository.countTotalByMonth(annee, mois, structureId);
            long countPrev;
            if (mois > 1) {
                countPrev = nonConformiteRepository.countTotalByMonth(annee, mois - 1, structureId);
            } else {
                countPrev = nonConformiteRepository.countTotalByMonth(annee - 1, 12, structureId);
            }

            if (countPrev > 0) {
                pct = ((double) (totalEvolution - countPrev) / countPrev) * 100.0;
            } else if (totalEvolution > 0) {
                pct = 100.0;
            }

            labels = List.of("Semaine 1", "Semaine 2", "Semaine 3", "Semaine 4");

            for (String libelle : tousNiveaux) {
                datasets.add(NcEvolutionDto.DatasetDto.builder()
                        .label(libelle)
                        .data(new ArrayList<>(Collections.nCopies(4, 0L)))
                        .build());
            }

            List<Object[]> results = nonConformiteRepository.getEvolutionStatsByMonth(annee, mois, structureId);
            for (Object[] row : results) {
                if (row[0] == null || row[1] == null || row[2] == null) {
                    continue;
                }
                int semaineVal = ((Number) row[0]).intValue();
                String gravite = (String) row[1];
                long count = ((Number) row[2]).longValue();

                int semaineIdx = Math.min(4, semaineVal) - 1;
                if (semaineIdx >= 0 && semaineIdx < 4) {
                    for (NcEvolutionDto.DatasetDto dataset : datasets) {
                        if (dataset.getLabel().equals(gravite)) {
                            long existing = dataset.getData().get(semaineIdx);
                            dataset.getData().set(semaineIdx, existing + count);
                            break;
                        }
                    }
                    if (gravityCounts.containsKey(gravite)) {
                        gravityCounts.put(gravite, gravityCounts.get(gravite) + count);
                    }
                }
            }
        }

        String pourcentageEvolution;
        if (pct > 0.0) {
            pourcentageEvolution = String.format(Locale.US, "+%.1f", pct);
        } else if (pct < 0.0) {
            pourcentageEvolution = String.format(Locale.US, "%.1f", pct);
        } else {
            pourcentageEvolution = "0.0";
        }

        List<NcEvolutionDto.GravityCountDto> gravitesBreakdown = new ArrayList<>();
        for (Map.Entry<String, Long> entry : gravityCounts.entrySet()) {
            gravitesBreakdown.add(NcEvolutionDto.GravityCountDto.builder()
                    .nom(entry.getKey())
                    .count(entry.getValue())
                    .build());
        }

        NcEvolutionDto.ChartDataDto chartData = NcEvolutionDto.ChartDataDto.builder()
                .labels(labels)
                .datasets(datasets)
                .build();

        return NcEvolutionDto.builder()
                .totalEvolution(totalEvolution)
                .pourcentageEvolution(pourcentageEvolution)
                .gravites(gravitesBreakdown)
                .chartData(chartData)
                .build();
    }

    @Override
    public Page<NonConformiteDto> getNonConformitesByNiveau(UUID niveauId, Pageable pageable) {
        return nonConformiteRepository.findAllByNiveauNonConformiteId(niveauId, pageable)
                .map(nc -> populateAttachments(nonConformiteMapper.toDto(nc)));
    }

    @Override
    public Page<NonConformiteDto> search(
            String numeroReference, String nomProcessus, String origineId, String origineService,
            String structureSoumissionId, String structureResponsableId,
            Etat etatTraitement,
            Status status,
            TypeDemande typeDemande,
            Circuit circuit,
            String userImputeEmail, String typeNonConformiteLibelle, String niveauNonConformiteLibelle,
            UUID typeNonConformiteId, UUID niveauNonConformiteId,
            LocalDateTime publicationDateFrom, LocalDateTime publicationDateTo,
            Pageable pageable) {
        Specification<NonConformite> spec = NonConformiteSpecification.filter(
                numeroReference, nomProcessus, origineId, origineService,
                structureSoumissionId, structureResponsableId,
                etatTraitement, status, typeDemande, circuit,
                userImputeEmail, typeNonConformiteLibelle, niveauNonConformiteLibelle,
                typeNonConformiteId, niveauNonConformiteId,
                publicationDateFrom, publicationDateTo
        );
        return nonConformiteRepository.findAll(spec, pageable)
                .map(nc -> populateAttachments(nonConformiteMapper.toDto(nc)));
    }

    /**
     * Met à jour l'état de la non-conformité suite à un webhook du service de workflow.
     * Cette méthode est appelée par le contrôleur interne (AmeliorationInternalCallbackController).
     *
     * @param nonConformiteId Identifiant de la non conformité
     * @param newStateName    Le nom du nouvel état (ex: "Validation RS", "Clôture")
     * @param newEtatTraitement L'énumération Etat correspondante si définie (ex: "VALIDATION_RS", "CLOTURE")
     */
    @Override
    @Transactional
    public void updateWorkflowState(UUID nonConformiteId, String issue, String nomEtape, String etatCode,
                                    Map<String, String> champs) {
        log.info("Non-conformité {} : issue={}, étape={}, état={}", nonConformiteId, issue, nomEtape, etatCode);
        NonConformite nc = nonConformiteRepository.findById(nonConformiteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Non conformité introuvable"));

        appliquerChampsSaisis(nc, champs);

        nc.setWorkflowStatus(nomEtape);
        Status ancienStatut = nc.getStatus();

        // L'état de traitement vient de l'étape, pas d'un rapprochement sur son nom : c'est le
        // champ que le circuit porte pour ça, et il suit l'étape quel que soit son libellé.
        Etat etat = etatDeTraitement(etatCode);
        if (etat != null) {
            nc.setEtatTraitement(etat);
        }

        nc.setStatus(statutDepuisLIssue(issue, etat, ancienStatut));
        confierLesPlansSiLEtapeLeVeut(nc, etat);

        if (nc.getStatus() == Status.PUBLISHED
                && (ancienStatut == Status.DRAFT || ancienStatut == Status.REJECTED)) {
            nc.setPublicationDate(LocalDateTime.now());
        }

        nonConformiteRepository.save(nc);
    }

    /**
     * Recopie sur la non-conformité les saisies faites à l'étape.
     *
     * <p>Aujourd'hui, le document justificatif du rejet. Le champ ne porte que la <b>référence</b>
     * de l'objet déposé — le moteur ne transporte que des chaînes — et la pièce jointe
     * correspondante a été créée lors du dépôt, avec son nom d'origine et son type. Le
     * rapprochement se fait donc sur cette référence, restreinte à la non-conformité concernée :
     * une référence est une chaîne qui circule côté client, elle ne vaut pas droit d'accrocher à ce
     * dossier le fichier d'un autre.</p>
     *
     * <p>Une référence inconnue n'interrompt pas la transition : la décision de l'utilisateur a
     * déjà été prise et enregistrée par le moteur, la refuser ici la ferait rejouer sans fin.</p>
     */
    private void appliquerChampsSaisis(NonConformite nc, Map<String, String> champs) {
        if (champs == null || champs.isEmpty()) {
            return;
        }

        appliquerDocumentDeRejet(nc, champs.get(CHAMP_DOC_REJET));
        appliquerStructureDestinataire(nc, champs);
        appliquerCircuitDeTraitement(nc, champs.get(CHAMP_CIRCUIT_TRAITEMENT));
        appliquerAgentImpute(nc, champs.get(CHAMP_AGENT_IMPUTE));
    }

    /**
     * Inscrit sur la non-conformité le circuit de traitement que le responsable qualité a retenu.
     *
     * <p>De ce choix dépend ce que le plan d'action devra porter : en <b>correction</b>, la cause
     * n'a pas à être recherchée et la colonne correspondante disparaît ; en <b>action
     * corrective</b>, le plan est complet. Sans cette reprise, la décision aurait été prise et
     * enregistrée par le moteur, et le module qui contrôle les plans d'action n'en aurait rien
     * su.</p>
     *
     * <p>Une valeur que l'énumération ne reconnaît pas laisse le dossier sur le circuit qu'il
     * portait : la décision est déjà jouée, la refuser ici la ferait rejouer sans fin.</p>
     */
    private void appliquerCircuitDeTraitement(NonConformite nc, String valeur) {
        String choix = valeurRenseignee(valeur);
        if (choix == null) {
            return;
        }

        Circuit circuit = Circuit.depuisValeur(choix);
        if (circuit == null) {
            log.warn("Non-conformité {} : circuit de traitement « {} » inconnu ; le dossier garde « {} ».",
                    nc.getId(), choix, nc.getCircuit());
            return;
        }
        if (circuit == nc.getCircuit()) {
            return;
        }

        nc.setCircuit(circuit);
        log.info("Non-conformité {} traitée au titre du circuit « {} »", nc.getId(), circuit.getLibelle());
    }

    /**
     * Inscrit sur la non-conformité l'agent que l'étape d'imputation a désigné.
     *
     * <p>C'est cette valeur — et non le titulaire porté par l'instance de circuit — que lisent les
     * listes du module : « mes non-conformités imputées », les tableaux de bord, les relances.</p>
     */
    private void appliquerAgentImpute(NonConformite nc, String agentId) {
        String agent = valeurRenseignee(agentId);
        if (agent == null || agent.equals(nc.getUserImputId())) {
            return;
        }
        nc.setUserImputId(agent);
        log.info("Non-conformité {} imputée à l'agent {}", nc.getId(), agent);
    }

    private void appliquerDocumentDeRejet(NonConformite nc, String reference) {
        if (reference == null || reference.isBlank()) {
            return;
        }

        pieceJointeRepository.findByUrlAndEntityId(reference.trim(), nc.getId())
                .ifPresentOrElse(
                        nc::setDocRejet,
                        () -> log.warn("Non-conformité {} : le document de rejet « {} » est introuvable "
                                + "parmi ses pièces jointes ; il n'est pas rattaché.", nc.getId(), reference));
    }

    /**
     * Confie la non-conformité à la structure désignée à l'étape.
     *
     * <p>Le libellé et le sigle accompagnent l'identifiant, et ne sont écrits que s'ils sont
     * fournis : une étape qui ne désigne que l'identifiant ne doit pas effacer le nom déjà affiché
     * dans les listes, qui deviendraient illisibles.</p>
     *
     * <p>Un champ vide n'est pas un transfert vers nulle part : c'est une décision qui ne réoriente
     * rien. Le dossier reste alors où il est.</p>
     */
    private void appliquerStructureDestinataire(NonConformite nc, Map<String, String> champs) {
        String structureId = valeurRenseignee(champs.get(CHAMP_STRUCTURE_DESTINATAIRE_ID));
        if (structureId == null) {
            return;
        }

        String precedente = nc.getOrigineId();
        if (structureId.equals(precedente)) {
            return;
        }

        nc.setOrigineId(structureId);
        nommerLaStructure(nc, structureId);

        log.info("Non-conformité {} confiée à la structure {} (précédemment {})",
                nc.getId(), structureId, precedente == null ? "aucune" : precedente);
    }

    /**
     * Inscrit sur la non-conformité le nom de la structure désignée, lu au référentiel.
     *
     * <p>Le circuit ne transporte que l'identifiant : le libellé et le sigle étaient auparavant
     * deux champs de saisie supplémentaires, c'est-à-dire qu'on demandait à l'utilisateur de
     * ressaisir ce que le référentiel sait — en lui laissant la possibilité de le contredire.</p>
     *
     * <p>Ils sont conservés sur le dossier, et non relus à chaque affichage : les listes de
     * non-conformités montrent le nom de la structure sans interroger le référentiel à chaque
     * ligne. Un référentiel injoignable laisse le dossier changer de structure sans son nom —
     * l'affectation est la décision, la nommer n'en est que la présentation.</p>
     */
    private void nommerLaStructure(NonConformite nc, String structureId) {
        try {
            StructureDto structure = referentielClient.getStructureById(UUID.fromString(structureId));
            if (structure == null) {
                return;
            }
            if (structure.getLibelleLong() != null) {
                nc.setOrigineService(structure.getLibelleLong());
            }
            if (structure.getLibelleCourt() != null) {
                nc.setOrigineServiceLibelleCourt(structure.getLibelleCourt());
            }
        } catch (IllegalArgumentException e) {
            log.warn("La structure désignée « {} » n'est pas un identifiant exploitable.", structureId);
        } catch (Exception e) {
            log.warn("Nom de la structure {} indisponible auprès du référentiel : {}",
                    structureId, e.getMessage());
        }
    }

    private String valeurRenseignee(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }

    /**
     * Confie les plans d'action à leurs responsables quand la validation qualité est atteinte.
     *
     * <p>C'est là que le dossier passe de la proposition à l'exécution : les actions proposées par
     * l'agent, validées par le pilote, deviennent des engagements nominatifs suivis par leur propre
     * circuit. Le faire plus tôt les aurait engagés avant validation ; plus tard, la clôture les
     * aurait attendus sans qu'ils aient jamais commencé.</p>
     */
    private void confierLesPlansSiLEtapeLeVeut(NonConformite nc, Etat etat) {
        if (etat != Etat.VALIDATION_RS) {
            return;
        }
        plansActionService.confierLesPlans(nc.getId());
    }

    /** État de traitement porté par l'étape atteinte, ou {@code null} sur une fin de circuit. */
    private Etat etatDeTraitement(String etatCode) {
        if (etatCode == null || etatCode.isBlank()) {
            return null;
        }
        try {
            return Etat.valueOf(etatCode.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Une étape peut porter un état que l'énumération ne connaît pas : le circuit est
            // paramétrable, l'énumération ne l'est pas. La NC garde alors son état précédent
            // plutôt que d'en prendre un faux.
            log.warn("L'étape atteinte porte un état de traitement inconnu : {}", etatCode);
            return null;
        }
    }

    /**
     * Statut de la non-conformité, déduit de l'issue que le moteur a établie.
     *
     * <p>Une seule règle métier s'y ajoute : atteindre l'étape de clôture publie la
     * non-conformité. Elle porte sur l'état de traitement — une valeur du circuit — et non sur le
     * libellé de l'étape, qui n'engage personne.</p>
     */
    private Status statutDepuisLIssue(String issue, Etat etat, Status ancienStatut) {
        if (etat == Etat.CLOTURE) {
            return Status.PUBLISHED;
        }
        if (issue == null || issue.isBlank()) {
            return ancienStatut;
        }
        return switch (issue.trim().toUpperCase()) {
            case "APPROVED" -> Status.APPROVED;
            case "REJECTED" -> Status.REJECTED;
            case "EN_COURS" -> Status.IN_PROGRESS;
            default -> ancienStatut;
        };
    }
}
