package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.config.PermissionChecker;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.evaluation.dto.CompilationDto;
import com.qualiapproche.evaluation.dto.ConstatDto;
import com.qualiapproche.evaluation.dto.PointControleDto;
import com.qualiapproche.evaluation.dto.PreuveDto;
import com.qualiapproche.evaluation.entities.PreuveConstat;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.Auditeur;
import com.qualiapproche.evaluation.entities.ChecklistAudit;
import com.qualiapproche.evaluation.entities.Constat;
import com.qualiapproche.evaluation.entities.PointControle;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import com.qualiapproche.evaluation.enumeration.StatutConstat;
import com.qualiapproche.evaluation.repository.ChecklistAuditRepository;
import com.qualiapproche.evaluation.repository.ConstatRepository;
import com.qualiapproche.evaluation.repository.PreuveConstatRepository;
import com.qualiapproche.evaluation.repository.TypeConstatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Les constats d'un audit, selon leurs trois paliers.
 *
 * <p>Qui voit quoi, et qui peut quoi :</p>
 * <ul>
 *   <li>un <b>brouillon</b> n'est visible et modifiable que par son rédacteur ;</li>
 *   <li>un constat <b>compilé</b> est visible et modifiable par toute l'équipe de l'audit, qui
 *       peut aussi le renvoyer en brouillon ;</li>
 *   <li>la <b>publication</b> — la validation en réunion d'équipe — revient à l'équipe, ou à
 *       qui détient {@code audit-validate} ;</li>
 *   <li>un constat <b>publié</b> est verrouillé.</li>
 * </ul>
 *
 * <p>On ne saisit de constat que sur un audit en cours : avant, il n'y a rien à constater ;
 * après la clôture, le rapport est signé. Dès le premier visa du rapport, plus aucun constat ne
 * se saisit ni ne se publie : ce serait changer ce que le signataire a lu.</p>
 */
@Service
@RequiredArgsConstructor
public class ConstatService {

    private static final String QUOI = "constat";

    private final ConstatRepository depot;
    private final TypeConstatRepository natures;
    private final ChecklistAuditRepository checklists;
    private final AuditService audits;
    private final AuditeurService auditeurs;
    private final PermissionChecker permissions;
    private final ApplicationEventPublisher evenements;
    private final PreuveConstatRepository preuves;
    private final FichiersAudit fichiers;
    private final RepartitionDesPoints repartition;

    /** Ce que l'équipe voit d'un audit : le compilé et le publié. */
    @Transactional(readOnly = true)
    public List<ConstatDto> compilation(UUID auditId) {
        Audit audit = audits.sienOuRien(auditId);
        return depot.findAllByAuditIdAndStatutInOrderByDateSaisieAsc(audit.getId(),
                EnumSet.of(StatutConstat.COMPILE, StatutConstat.PUBLIE)).stream().map(this::versDto).toList();
    }

    /** « Mes constats du jour » : les brouillons de l'appelant sur cet audit. */
    @Transactional(readOnly = true)
    public List<ConstatDto> mesBrouillons(UUID auditId) {
        Audit audit = audits.sienOuRien(auditId);
        Auditeur moi = auditeurs.auditeurCourant();
        return depot.findAllByAuditIdAndRedacteurIdAndStatutOrderByDateSaisieAsc(audit.getId(), moi.getId(),
                StatutConstat.BROUILLON).stream().map(this::versDto).toList();
    }

    @Transactional(readOnly = true)
    public ConstatDto lire(UUID id) {
        return versDto(visible(id));
    }

    /** Saisit un constat en brouillon ; son rédacteur est l'appelant, membre de l'équipe. */
    @Transactional
    public ConstatDto creer(UUID auditId, ConstatDto saisie) {
        Audit audit = audits.sienOuRien(auditId);
        if (audit.getStatut() != StatutAudit.EN_COURS) {
            throw new BusinessException("On ne saisit de constat que sur un audit en cours ; "
                    + audit.getReference() + " est " + audit.getStatut() + ".", HttpStatus.CONFLICT);
        }
        audits.exigerRapportSansVisa(audit, "un nouveau constat");
        Auditeur moi = auditeurs.auditeurCourant();
        exigerDeLEquipe(audit, moi);
        Constat constat = Constat.builder()
                .audit(audit)
                .redacteur(moi)
                .statut(StatutConstat.BROUILLON)
                .dateSaisie(LocalDateTime.now())
                .build();
        recopier(saisie, constat);
        return versDto(depot.save(constat));
    }

    @Transactional
    public ConstatDto modifier(UUID id, ConstatDto saisie) {
        Constat constat = modifiableParMoi(id);
        recopier(saisie, constat);
        // Le commentaire d'équipe n'a de sens qu'à la compilation : en brouillon, personne d'autre
        // que l'auteur ne le lirait.
        if (constat.getStatut() == StatutConstat.COMPILE) {
            constat.setCommentaireEquipe(saisie.getCommentaireEquipe());
        }
        return versDto(depot.save(constat));
    }

    /** Seul un brouillon s'efface, et seulement par son rédacteur : le reste appartient à l'équipe. */
    @Transactional
    public void supprimer(UUID id) {
        Constat constat = visible(id);
        if (constat.getStatut() != StatutConstat.BROUILLON) {
            throw new BusinessException("Un constat " + constat.getStatut() + " ne s'efface plus :"
                    + " l'équipe peut le renvoyer en brouillon à son auteur.", HttpStatus.CONFLICT);
        }
        depot.delete(constat);
    }

    /** Le rédacteur verse son brouillon à la compilation d'équipe. */
    @Transactional
    public ConstatDto publier(UUID id) {
        Constat constat = modifiableParMoi(id);
        constat.publier();
        return versDto(depot.save(constat));
    }

    /** L'équipe renvoie un constat compilé à son auteur, avec la raison. */
    @Transactional
    public ConstatDto retirerVersBrouillon(UUID id, String motif) {
        Constat constat = modifiableParMoi(id);
        constat.retirerVersBrouillon(motif, com.qualiapproche.common.utils.SecurityUtils.getAuteurNom());
        return versDto(depot.save(constat));
    }

    /** Validé en réunion d'équipe : publié et verrouillé. */
    @Transactional
    public ConstatDto valider(UUID id) {
        Constat constat = visible(id);
        Audit audit = constat.getAudit();
        exigerDeLEquipeOuValideur(audit);
        exigerEnCours(audit);
        audits.exigerRapportSansVisa(audit, "publier un constat");
        constat.valider();
        Constat publie = depot.save(constat);
        annoncerLesEcarts(List.of(publie));
        annoncerAuPilote(audit, List.of(publie));
        return versDto(publie);
    }

    /**
     * Valide d'un geste les constats compilés de l'audit — ceux d'une nature, ou tous.
     *
     * <p>Par nature : « Valider ce groupe ». Sans nature : « L'équipe est d'accord — valider
     * tout ». Seuls les constats compilés passent ; un brouillon reste à son auteur, l'équipe ne
     * le publie jamais à sa place.</p>
     */
    @Transactional
    public List<ConstatDto> validerLaCompilation(UUID auditId, UUID natureId) {
        Audit audit = audits.sienOuRien(auditId);
        exigerDeLEquipeOuValideur(audit);
        exigerEnCours(audit);
        audits.exigerRapportSansVisa(audit, "publier des constats");
        List<Constat> compiles = new ArrayList<>(depot.findAllByAuditIdAndStatutOrderByDateSaisieAsc(
                audit.getId(), StatutConstat.COMPILE).stream()
                .filter(c -> natureId == null || (c.getNature() != null && natureId.equals(c.getNature().getId())))
                .toList());
        compiles.forEach(Constat::valider);
        List<Constat> publies = depot.saveAll(compiles);
        annoncerLesEcarts(publies);
        annoncerAuPilote(audit, publies);
        return publies.stream().map(this::versDto).toList();
    }

    /** « Publier tous mes constats vers la compilation d'équipe » : les brouillons de l'appelant. */
    @Transactional
    public List<ConstatDto> publierTousMesConstats(UUID auditId) {
        Audit audit = audits.sienOuRien(auditId);
        exigerEnCours(audit);
        Auditeur moi = auditeurs.auditeurCourant();
        List<Constat> brouillons = depot.findAllByAuditIdAndRedacteurIdAndStatutOrderByDateSaisieAsc(
                audit.getId(), moi.getId(), StatutConstat.BROUILLON);
        // Un brouillon sans nature n'est pas fini : il reste à relire, et n'empêche pas les autres
        // de partir — la maquette ne les propose d'ailleurs pas à la publication.
        List<Constat> prets = brouillons.stream().filter(c -> c.getNature() != null).toList();
        prets.forEach(Constat::publier);
        return depot.saveAll(prets).stream().map(this::versDto).toList();
    }

    /** Le bandeau de la compilation : totaux, et décompte par nature dans l'ordre du paramétrage. */
    @Transactional(readOnly = true)
    public CompilationDto synthese(UUID auditId) {
        Audit audit = audits.sienOuRien(auditId);
        List<Constat> visibles = depot.findAllByAuditIdAndStatutInOrderByDateSaisieAsc(audit.getId(),
                EnumSet.of(StatutConstat.COMPILE, StatutConstat.PUBLIE));
        List<CompilationDto.Groupe> groupes = natures.findAllByDirectionIdOrderByCreatedAtAsc(audit.getDirectionId())
                .stream()
                .map(n -> {
                    List<Constat> duGroupe = visibles.stream()
                            .filter(c -> c.getNature() != null && c.getNature().getId().equals(n.getId())).toList();
                    return CompilationDto.Groupe.builder()
                            .natureId(n.getId())
                            .libelle(n.getLibelle())
                            .couleur(n.getCouleur())
                            .genereNonConformite(n.isGenereNonConformite())
                            .nombre(duGroupe.size())
                            .compiles(duGroupe.stream().filter(c -> c.getStatut() == StatutConstat.COMPILE).count())
                            .build();
                })
                .filter(g -> g.getNombre() > 0)
                .toList();
        long compiles = visibles.stream().filter(c -> c.getStatut() == StatutConstat.COMPILE).count();
        return CompilationDto.builder()
                .total(visibles.size())
                .compiles(compiles)
                .publies(visibles.size() - compiles)
                .parNature(groupes)
                .build();
    }

    /**
     * Les points de contrôle que l'appelant doit renseigner sur cet audit : ceux que la répartition
     * de l'audit lui confie, et ceux qui ne sont confiés à personne de l'équipe (RepartitionDesPoints).
     */
    @Transactional(readOnly = true)
    public List<PointControleDto> mesPointsDeControle(UUID auditId) {
        Audit audit = audits.sienOuRien(auditId);
        Auditeur moi = auditeurs.auditeurCourant();
        // Le constat le plus récent de l'appelant sur chaque point : c'est lui qui fait le badge.
        java.util.Map<UUID, Constat> parPoint = new java.util.HashMap<>();
        depot.findAllByAuditIdAndRedacteurId(audit.getId(), moi.getId()).stream()
                .filter(c -> c.getPointControle() != null)
                .forEach(c -> parPoint.merge(c.getPointControle().getId(), c,
                        (a, b) -> a.getDateSaisie().isAfter(b.getDateSaisie()) ? a : b));
        java.util.Map<UUID, UUID> surLAudit = repartition.affectationsDe(audit);
        return audit.getChecklists().stream()
                .flatMap(c -> c.getPoints().stream())
                .filter(p -> {
                    UUID qui = repartition.attribueA(audit, p, surLAudit);
                    return qui == null || qui.equals(moi.getId());
                })
                .map(p -> PointControleDto.builder()
                        .id(p.getId())
                        .checklistId(p.getChecklist().getId())
                        .chapitreISO(p.getChapitreISO())
                        .libelle(p.getLibelle())
                        .question(p.getQuestion())
                        .preuveAttendue(p.getPreuveAttendue())
                        .auditeurAssigneId(moi.getId())
                        .constatId(parPoint.containsKey(p.getId()) ? parPoint.get(p.getId()).getId() : null)
                        .statutConstat(parPoint.containsKey(p.getId()) ? parPoint.get(p.getId()).getStatut() : null)
                        .build())
                .toList();
    }

    // ------------------------------------------------------------------ Éléments de preuve

    @Transactional(readOnly = true)
    public List<PreuveDto> preuves(UUID constatId) {
        Constat constat = visible(constatId);
        return preuves.findAllByConstatIdOrderByCreatedAtAsc(constat.getId()).stream().map(this::versDto).toList();
    }

    /** Joint des preuves à un constat que l'appelant peut encore modifier : pas à un publié. */
    @Transactional
    public List<PreuveDto> joindre(UUID constatId, List<org.springframework.web.multipart.MultipartFile> aDeposer) {
        Constat constat = modifiableParMoi(constatId);
        List<PreuveDto> posees = new ArrayList<>();
        for (org.springframework.web.multipart.MultipartFile fichier : aDeposer) {
            if (fichier == null || fichier.isEmpty()) {
                continue;
            }
            String reference = fichiers.deposer(fichier, constat.getAudit().getId().toString(), "preuves",
                    constat.getId().toString());
            posees.add(versDto(preuves.save(PreuveConstat.builder()
                    .constatId(constat.getId())
                    .nom(fichier.getOriginalFilename())
                    .ext(FichiersAudit.extensionDe(fichier.getOriginalFilename()))
                    .type(fichier.getContentType())
                    .url(reference)
                    .build())));
        }
        return posees;
    }

    /** La preuve et son contenu, si son constat est visible de l'appelant. */
    @Transactional(readOnly = true)
    public PreuveConstat preuve(UUID preuveId) {
        PreuveConstat preuve = Perimetre.sienneOuRien(preuves, preuveId, "élément de preuve");
        visible(preuve.getConstatId());
        return preuve;
    }

    @Transactional(readOnly = true)
    public java.io.InputStream contenu(PreuveConstat preuve) {
        return fichiers.contenu(preuve.getUrl());
    }

    @Transactional
    public void retirerPreuve(UUID preuveId) {
        PreuveConstat preuve = Perimetre.sienneOuRien(preuves, preuveId, "élément de preuve");
        modifiableParMoi(preuve.getConstatId());
        preuves.delete(preuve);
        fichiers.retirer(preuve.getUrl());
    }

    private PreuveDto versDto(PreuveConstat p) {
        return PreuveDto.builder().id(p.getId()).constatId(p.getConstatId()).nom(p.getNom())
                .ext(p.getExt()).type(p.getType()).build();
    }

    /**
     * Le constat, s'il est visible de l'appelant.
     *
     * <p>Un brouillon d'autrui est déclaré introuvable, comme l'entité d'une autre direction :
     * « il existe mais il est privé » en dirait déjà trop.</p>
     */
    private Constat visible(UUID id) {
        Constat constat = Perimetre.sienneOuRien(depot, id, QUOI);
        if (constat.getStatut() == StatutConstat.BROUILLON && !estDeMoi(constat)) {
            throw new BusinessException("Aucun(e) constat ne porte cet identifiant : " + id, HttpStatus.NOT_FOUND);
        }
        return constat;
    }

    private Constat modifiableParMoi(UUID id) {
        Constat constat = visible(id);
        if (constat.getStatut() == StatutConstat.PUBLIE) {
            throw new BusinessException("Un constat publié est verrouillé.", HttpStatus.CONFLICT);
        }
        if (constat.getStatut() == StatutConstat.COMPILE) {
            exigerDeLEquipe(constat.getAudit(), auditeurs.auditeurCourant());
        }
        exigerEnCours(constat.getAudit());
        return constat;
    }

    private static void exigerEnCours(Audit audit) {
        if (audit.getStatut() != StatutAudit.EN_COURS) {
            throw new BusinessException("L'audit " + audit.getReference() + " n'est pas en cours :"
                    + " ses constats ne se modifient pas.", HttpStatus.CONFLICT);
        }
    }

    private boolean estDeMoi(Constat constat) {
        String moi = SecurityUtils.getCurrentUserId();
        return moi != null && moi.equals(constat.getRedacteur().getUtilisateurId());
    }

    /**
     * La validation est un geste d'équipe : tout membre peut la porter, comme le fait l'écran de
     * compilation, qui ne distingue pas le responsable des auditeurs associés. Qui détient
     * {@code audit-validate} le peut aussi, sans être de l'équipe.
     */
    private void exigerDeLEquipeOuValideur(Audit audit) {
        if (permissions.detient("audit-validate")) {
            return;
        }
        exigerDeLEquipe(audit, auditeurs.auditeurCourant());
    }

    /**
     * Les constats publiés dont la nature génère une non-conformité partent vers M05 — une fois
     * cette publication committée, voir TransmissionVersM05.
     */
    private void annoncerLesEcarts(List<Constat> publies) {
        List<UUID> ecarts = publies.stream().filter(Constat::estNonConformite).map(Constat::getId).toList();
        if (!ecarts.isEmpty()) {
            evenements.publishEvent(EcartsPublies.deConstats(ecarts));
        }
    }

    /** Le pilote du processus audité est prévenu après commit, d'un seul courriel par geste. */
    private void annoncerAuPilote(Audit audit, List<Constat> publies) {
        if (!publies.isEmpty()) {
            evenements.publishEvent(new ConstatsPublies(audit.getId(), publies.stream().map(Constat::getId).toList()));
        }
    }

    private static void exigerDeLEquipe(Audit audit, Auditeur auditeur) {
        if (!audit.aDansLEquipe(auditeur)) {
            throw new BusinessException("Vous ne faites pas partie de l'équipe de l'audit "
                    + audit.getReference() + ".", HttpStatus.FORBIDDEN);
        }
    }

    private void recopier(ConstatDto saisie, Constat cible) {
        cible.setObservation(saisie.getObservation().strip());
        cible.setCritereAudit(saisie.getCritereAudit());
        cible.setNature(Perimetre.sienneOuNul(natures, saisie.getNatureId(), "nature de constat"));
        ChecklistAudit checklist = Perimetre.sienneOuNul(checklists, saisie.getChecklistId(), "checklist");
        PointControle point = null;
        if (saisie.getPointControleId() != null) {
            if (checklist == null) {
                throw new BusinessException("Un point de contrôle se cite avec sa checklist.", HttpStatus.BAD_REQUEST);
            }
            point = checklist.getPoints().stream().filter(p -> p.getId().equals(saisie.getPointControleId()))
                    .findFirst().orElseThrow(() -> new BusinessException("Ce point de contrôle n'appartient"
                            + " pas à la checklist « " + checklist.getNom() + " ».", HttpStatus.BAD_REQUEST));
        }
        cible.setChecklist(checklist);
        cible.setPointControle(point);
    }

    private ConstatDto versDto(Constat c) {
        return ConstatDto.builder()
                .id(c.getId())
                .auditId(c.getAudit().getId())
                .critereAudit(c.getCritereAudit())
                .observation(c.getObservation())
                .statut(c.getStatut())
                .datePublication(c.getDatePublication())
                .dateSaisie(c.getDateSaisie())
                .commentaireEquipe(c.getCommentaireEquipe())
                .renvoye(c.estRenvoye())
                .motifRenvoi(c.getMotifRenvoi())
                .renvoyeLe(c.getRenvoyeLe())
                .renvoyePar(c.getRenvoyePar())
                .natureId(c.getNature() == null ? null : c.getNature().getId())
                .natureLibelle(c.getNature() == null ? null : c.getNature().getLibelle())
                .natureCouleur(c.getNature() == null ? null : c.getNature().getCouleur())
                .nonConformite(c.estNonConformite())
                .checklistId(c.getChecklist() == null ? null : c.getChecklist().getId())
                .pointControleId(c.getPointControle() == null ? null : c.getPointControle().getId())
                .redacteurId(c.getRedacteur().getId())
                .redacteurUtilisateurId(c.getRedacteur().getUtilisateurId())
                .nonConformiteId(c.getNonConformiteId())
                .transmissionEnAttente(c.getStatut() == StatutConstat.PUBLIE && c.estNonConformite()
                        && c.getNonConformiteId() == null)
                .build();
    }
}
