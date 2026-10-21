package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.client.Annuaire;
import com.qualiapproche.evaluation.dto.AuditDto;
import com.qualiapproche.evaluation.dto.CompilationDto;
import com.qualiapproche.evaluation.dto.RapportAuditDto;
import com.qualiapproche.evaluation.dto.SignatureDto;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.Auditeur;
import com.qualiapproche.evaluation.entities.Constat;
import com.qualiapproche.evaluation.entities.EvaluationRQAPBF;
import com.qualiapproche.evaluation.entities.PlanAudit;
import com.qualiapproche.evaluation.entities.Signature;
import com.qualiapproche.evaluation.entities.TypeConstat;
import com.qualiapproche.evaluation.enumeration.StatutConstat;
import com.qualiapproche.evaluation.enumeration.StatutEvaluation;
import com.qualiapproche.evaluation.repository.ConstatRepository;
import com.qualiapproche.evaluation.repository.EvaluationRQAPBFRepository;
import com.qualiapproche.evaluation.repository.PlanAuditRepository;
import com.qualiapproche.evaluation.repository.SignatureRepository;
import com.qualiapproche.evaluation.repository.TypeConstatRepository;
import com.qualiapproche.storage.ModeleDeDocument;
import com.qualiapproche.storage.RenduDocument;
import com.qualiapproche.storage.RenduDocumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Le rapport d'audit, en données (D14) et en documents (PDF, Word) ; le plan d'audit en documents (B8).
 *
 * <p>Le rapport suit les quatorze sections de la maquette. Ce que les données ne disent pas
 * d'elles-mêmes — conclusions, recommandations — vient de la rédaction de l'équipe.</p>
 */
@Service
@RequiredArgsConstructor
public class DocumentsAuditService {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORODATAGE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AuditService audits;
    private final ConstatRepository constats;
    private final TypeConstatRepository natures;
    private final PlanAuditRepository plans;
    private final SignatureRepository signatures;
    private final EvaluationRQAPBFRepository evaluations;
    private final Annuaire annuaire;
    private final RenduDocument rendu;
    private final com.qualiapproche.evaluation.repository.PreuveConstatRepository preuves;
    private final GrilleDesMentionsRQAPBF mentions;

    @Transactional(readOnly = true)
    public RapportAuditDto rapport(java.util.UUID auditId) {
        Audit audit = audits.sienOuRien(auditId);
        List<Constat> publies = constats.findAllByAuditIdAndStatutOrderByDateSaisieAsc(audit.getId(), StatutConstat.PUBLIE);
        List<RapportAuditDto.ConstatRapport> lignes = publies.stream().map(this::ligne).toList();
        EvaluationRQAPBF evaluation = evaluations.findAllByAuditIdOrderByDateEvaluationDesc(audit.getId()).stream()
                .filter(e -> e.getStatut() == StatutEvaluation.VALIDEE).findFirst().orElse(null);
        return RapportAuditDto.builder()
                .audit(audits.lire(audit.getId()))
                .methodesAudit(plans.findByAuditId(audit.getId()).map(p -> List.copyOf(p.getMethodesAudit())).orElse(List.of()))
                .syntheseParNature(synthese(audit, publies))
                .constats(lignes)
                .ecarts(publies.stream().filter(Constat::estNonConformite).map(this::ligne).toList())
                .scoreRqapbf(evaluation == null ? null : evaluation.scoreOfficiel())
                .mentionRqapbf(evaluation == null ? null : mentions.mentionDe(evaluation, true))
                .resultatsRqapbfParDomaine(evaluation == null ? Map.of() : evaluation.genererResultatsParDomaine(true))
                .nombrePreuves(lignes.isEmpty() ? 0 : preuves.countByConstatIdIn(lignes.stream().map(RapportAuditDto.ConstatRapport::getId).toList()))
                .signatures(signatures.findAllByAuditIdOrderByOrdreAsc(audit.getId()).stream()
                        .map(DocumentsAuditService::versDto).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public Fichier rapportEnDocument(java.util.UUID auditId, RenduDocument.Format format) {
        RapportAuditDto r = rapport(auditId);
        Audit audit = audits.sienOuRien(auditId);
        AuditDto a = r.getAudit();
        String processus = annuaire.structure(a.getProcessusId());
        List<ModeleDeDocument.Section> sections = new ArrayList<>();
        sections.add(ModeleDeDocument.Section.texte("Contexte",
                "Processus audité : " + processus,
                "Type d'audit : " + valeur(a.getTypeAuditLibelle())
                        + (a.getOrganismeExterne() == null ? "" : " — mené par " + a.getOrganismeExterne()),
                "Référentiels : " + (a.getReferentiels().isEmpty() ? "—" : String.join(", ", a.getReferentiels())),
                "Période : " + jour(a.getDateDebutPrevue()) + " au " + jour(a.getDateFinPrevue())));
        sections.add(ModeleDeDocument.Section.texte("Objectifs de l'audit", a.getObjectifsAudit()));
        sections.add(ModeleDeDocument.Section.texte("Portée de l'audit", a.getPorteeAudit()));
        sections.add(ModeleDeDocument.Section.texte("Critères d'audit", a.getCriteresAudit()));
        sections.add(ModeleDeDocument.Section.texte("Méthodologie",
                r.getMethodesAudit().isEmpty() ? null : String.join(" · ", r.getMethodesAudit())));
        sections.add(ModeleDeDocument.Section.texte("Équipe d'audit et personnes auditées", equipe(audit)));
        // Même structure et même ordre qu'à l'écran, numérotés de même : la numérotation sautait de 9 à 11
        // à l'écran et différait du PDF, la synthèse se répétait, et l'évaluation RQAP-BF figurait
        // sur un audit ISO (recette du 17 oct. 2026, observation 24).
        sections.add(ModeleDeDocument.Section.tableau("Synthèse des constats publiés",
                List.of("Nature du constat", "Nombre"),
                r.getSyntheseParNature().stream().map(g -> List.of(g.getLibelle(), String.valueOf(g.getNombre()))).toList(),
                "Aucun constat publié."));
        sections.add(ModeleDeDocument.Section.tableau("Détail des constats publiés",
                List.of("Point / critère", "Nature", "Observation", "Auditeur", "Date", "Fiche NC"),
                r.getConstats().stream().map(c -> List.of(valeur(c.getPointControle() != null ? c.getPointControle() : c.getCritere()),
                        valeur(c.getNature()), valeur(c.getObservation()), valeur(c.getAuditeur()),
                        c.getDatePublication() == null ? "—" : c.getDatePublication().format(JOUR),
                        c.getNumeroNonConformite() != null ? c.getNumeroNonConformite()
                                : c.getNonConformiteId() != null ? "ouverte" : "—")).toList(),
                "Aucun constat publié."));
        if (r.getScoreRqapbf() != null) {
            sections.add(ModeleDeDocument.Section.texte("Mention RQAP-BF", lignesDeLaMention(r.getMentionRqapbf())));
            sections.add(ModeleDeDocument.Section.tableau("Évaluation RQAP-BF"
                            + String.format(" (score officiel : %.0f %%)", r.getScoreRqapbf()),
                    List.of("Domaine", "Score"),
                    r.getResultatsRqapbfParDomaine().entrySet().stream()
                            .map(e -> List.of(e.getKey(), e.getValue() == null ? "N/A" : String.format("%.0f %%", e.getValue())))
                            .toList(),
                    "Aucun domaine évalué."));
        }
        sections.add(ModeleDeDocument.Section.texte("Conclusions", a.getConclusionsRapport()));
        sections.add(ModeleDeDocument.Section.texte("Recommandations", a.getRecommandationsRapport()));
        sections.add(ModeleDeDocument.Section.tableau("Écarts transmis au module Non-conformités",
                List.of("Origine", "Type", "Fiche NC"),
                r.getEcarts().stream().map(c -> List.of(valeur(c.getPointControle() != null ? c.getPointControle() : c.getCritere()),
                        valeur(c.getNature()), c.getNumeroNonConformite() != null ? c.getNumeroNonConformite()
                                : c.getNonConformiteId() == null ? "Transmission en attente" : "Non-conformité ouverte"))
                        .toList(),
                "Aucun écart à transmettre."));
        sections.add(ModeleDeDocument.Section.texte("Annexes", annexes(audit, r)));
        List<SignatureDto> visas = r.getSignatures().stream()
                .filter(s -> s.getStatut() != com.qualiapproche.evaluation.enumeration.StatutSignature.REMPLACEE).toList();
        sections.add(ModeleDeDocument.Section.tableau("Signatures",
                List.of("Titre", "Signataire", "Fonction", "Statut", "Date"),
                visas.stream().map(s -> List.of(valeur(s.getRoleLibelle()), annuaire.personne(s.getSignataireId()),
                        valeur(s.getFonctionSignataire()), libelle(s), s.getDateSignature() == null ? "—"
                                : s.getDateSignature().format(HORODATAGE))).toList(),
                "Le rapport n'a pas encore été envoyé en signature."));
        // Tant que tout le circuit n'a pas signé, le document exporté le dit en tête (observation 25).
        boolean signe = !visas.isEmpty() && visas.stream()
                .allMatch(s -> s.getStatut() == com.qualiapproche.evaluation.enumeration.StatutSignature.SIGNEE);
        ModeleDeDocument document = new ModeleDeDocument(signe ? "RAPPORT D'AUDIT" : "RAPPORT D'AUDIT — PROJET NON SIGNÉ",
                a.getReferenceRapport() == null ? "à attribuer à l'envoi en signature" : a.getReferenceRapport(),
                a.getReference() + " — " + processus + " · " + periode(audit), sections,
                mentions(signe ? "Version signée" : "Projet — non signé"));
        return new Fichier(rendre(document, format),
                "Rapport_audit_" + a.getReference() + "." + format.extension(), format.typeMime());
    }

    @Transactional(readOnly = true)
    public Fichier planEnDocument(java.util.UUID auditId, RenduDocument.Format format) {
        Audit audit = audits.sienOuRien(auditId);
        PlanAudit plan = plans.findByAuditId(audit.getId())
                .orElseThrow(() -> new BusinessException("L'audit " + audit.getReference() + " n'a pas encore de plan.",
                        HttpStatus.NOT_FOUND));
        String processus = annuaire.structure(audit.getProcessusId());
        List<ModeleDeDocument.Section> sections = List.of(
                ModeleDeDocument.Section.texte("Objectifs de l'audit", plan.getObjectifs()),
                ModeleDeDocument.Section.texte("Portée de l'audit", plan.getPortee()),
                ModeleDeDocument.Section.texte("Critères d'audit", plan.getCriteres()),
                ModeleDeDocument.Section.texte("Méthodes d'audit",
                        plan.getMethodesAudit().isEmpty() ? null : String.join(" · ", plan.getMethodesAudit())),
                ModeleDeDocument.Section.texte("Équipe d'audit", equipe(audit)),
                ModeleDeDocument.Section.tableau("Programme détaillé et horaires",
                        List.of("Jour et horaire", "Activité", "Processus", "Auditeur(s)", "Personnes auditées"),
                        plan.getActivites().stream().map(act -> List.of(
                                (act.getJour() == null ? "" : jour(act.getJour()) + " · ")
                                        + valeur(act.getHoraireDebut()) + (act.getHoraireFin() == null ? "" : " – " + act.getHoraireFin()),
                                valeur(act.getLibelleActivite()), valeur(act.getProcessusConcerne()),
                                valeur(act.getAuditeur()), valeur(act.getPersonnesAuditees()))).toList(),
                        "Aucune activité planifiée."),
                ModeleDeDocument.Section.texte("Logistique",
                        "Lieu : " + valeur(plan.getLieu()) + " · Langue : " + valeur(plan.getLangue())
                                + " · Moyens : " + valeur(plan.getMoyensNecessaires())));
        ModeleDeDocument document = new ModeleDeDocument("PLAN D'AUDIT", valeur(plan.getReference()),
                audit.getReference() + " — " + processus + " · " + periode(audit), sections,
                mentions("Version " + plan.getVersion()));
        return new Fichier(rendre(document, format),
                "Plan_audit_" + audit.getReference() + "." + format.extension(), format.typeMime());
    }

    private byte[] rendre(ModeleDeDocument document, RenduDocument.Format format) {
        try {
            return rendu.rendre(document, format);
        } catch (RenduDocumentException e) {
            throw new BusinessException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** Un document rendu, prêt à télécharger. */
    public record Fichier(byte[] contenu, String nom, String typeMime) {
    }

    private String equipe(Audit audit) {
        List<String> lignes = new ArrayList<>();
        if (audit.getResponsableEquipe() != null) {
            lignes.add("Responsable de l'équipe d'audit : " + annuaire.personne(audit.getResponsableEquipe().getUtilisateurId()));
        }
        String membres = audit.getMembresEquipe().stream().map(Auditeur::getUtilisateurId).map(annuaire::personne)
                .collect(Collectors.joining(", "));
        if (!membres.isBlank()) {
            lignes.add("Auditeurs associés : " + membres);
        }
        if (audit.getOrganismeExterne() != null) {
            lignes.add("Organisme auditeur : " + audit.getOrganismeExterne());
        }
        return lignes.isEmpty() ? null : String.join("\n", lignes);
    }

    private List<CompilationDto.Groupe> synthese(Audit audit, List<Constat> publies) {
        Map<java.util.UUID, Long> parNature = publies.stream().filter(c -> c.getNature() != null)
                .collect(Collectors.groupingBy(c -> c.getNature().getId(), Collectors.counting()));
        return natures.findAllByDirectionIdOrderByCreatedAtAsc(audit.getDirectionId()).stream()
                .filter(n -> parNature.containsKey(n.getId()))
                .map(n -> CompilationDto.Groupe.builder()
                        .natureId(n.getId()).libelle(n.getLibelle()).couleur(n.getCouleur())
                        .genereNonConformite(n.isGenereNonConformite())
                        .nombre(parNature.get(n.getId())).compiles(0)
                        .build())
                .toList();
    }

    private RapportAuditDto.ConstatRapport ligne(Constat c) {
        TypeConstat n = c.getNature();
        return RapportAuditDto.ConstatRapport.builder()
                .id(c.getId())
                .pointControle(c.getPointControle() == null ? null : c.getPointControle().getLibelle())
                .critere(c.getCritereAudit())
                .nature(n == null ? null : n.getLibelle())
                .natureCouleur(n == null ? null : n.getCouleur())
                .observation(c.getObservation())
                .datePublication(c.getDatePublication())
                .nonConformiteId(c.getNonConformiteId())
                .numeroNonConformite(c.getNumeroNonConformite())
                .auditeur(c.getRedacteur() == null ? null : annuaire.personne(c.getRedacteur().getUtilisateurId()))
                .build();
    }

    private static SignatureDto versDto(Signature s) {
        return SignatureDto.builder()
                .id(s.getId()).auditId(s.getAudit().getId()).signataireId(s.getSignataireId())
                .fonctionSignataire(s.getFonctionSignataire()).ordre(s.getOrdre()).statut(s.getStatut())
                .dateSignature(s.getDateSignature()).motifRefus(s.getMotifRefus())
                .role(s.getRole()).roleLibelle(s.getRole() == null ? null : s.getRole().getLibelle())
                .build();
    }

    private static String libelle(SignatureDto s) {
        return switch (s.getStatut()) {
            case SIGNEE -> "Signé";
            case REFUSEE -> "Refusé : " + valeur(s.getMotifRefus());
            default -> "En attente";
        };
    }

    /** Le pied de page : la version, la date d'édition et la confidentialité (observation 17). */
    private static String mentions(String version) {
        return version + " · édité le " + java.time.LocalDate.now().format(JOUR) + " · Document interne — diffusion restreinte";
    }

    /** Les annexes tirées des données réelles : elles annonçaient des preuves qui n'existaient pas (observation 24). */
    private String annexes(Audit audit, RapportAuditDto r) {
        List<String> lignes = new ArrayList<>();
        plans.findByAuditId(audit.getId()).ifPresent(p -> lignes.add("Plan d'audit " + valeur(p.getReference())
                + " (version " + p.getVersion() + ")"));
        if (!audit.getChecklists().isEmpty()) {
            lignes.add(audit.getChecklists().size() + " checklist(s) : " + audit.getChecklists().stream()
                    .map(c -> c.getNom()).collect(Collectors.joining(", ")));
        }
        long nbPreuves = r.getConstats().isEmpty() ? 0
                : preuves.countByConstatIdIn(r.getConstats().stream().map(RapportAuditDto.ConstatRapport::getId).toList());
        if (nbPreuves > 0) {
            lignes.add(nbPreuves + " preuve(s) jointe(s) aux constats publiés");
        }
        return lignes.isEmpty() ? "Aucune annexe." : String.join("\n", lignes);
    }

    /** « du 22/10/2026 au 23/10/2026 », ou le seul jour connu : le plan ne portait que le début. */
    private static String periode(Audit audit) {
        if (audit.getDateFinPrevue() == null || audit.getDateFinPrevue().equals(audit.getDateDebutPrevue())) {
            return jour(audit.getDateDebutPrevue());
        }
        return "du " + jour(audit.getDateDebutPrevue()) + " au " + jour(audit.getDateFinPrevue());
    }

    private static String jour(java.time.LocalDate date) {
        return date == null ? "—" : date.format(JOUR);
    }

    private static String valeur(String texte) {
        return texte == null || texte.isBlank() ? "—" : texte;
    }

    /**
     * La mention, en clair : niveau, étoiles en toutes lettres — la police du PDF n'a pas le glyphe —
     * et les garde-fous qui l'ont retenue (recette du 17 oct. 2026, observation 28).
     */
    private static String[] lignesDeLaMention(com.qualiapproche.evaluation.dto.MentionRQAPBFDto m) {
        if (m == null) {
            return new String[]{null};
        }
        List<String> lignes = new ArrayList<>();
        lignes.add(m.getNiveau() + " — " + m.getMention()
                + (m.getEtoiles() == 0 ? "" : " (" + m.getEtoiles() + (m.getEtoiles() > 1 ? " étoiles)" : " étoile)")));
        if (m.isPlafonnee()) {
            lignes.add("Mention plafonnée : un domaine au moins reste sous 50 % ("
                    + String.join(", ", m.getDomainesSousLeSeuil()) + ").");
        }
        if (m.isSousReserve()) {
            lignes.add("Délivrée sous réserve de la levée des " + m.getEcarts()
                    + " écart(s) relevé(s), transmis au module Non-conformités.");
        }
        return lignes.toArray(String[]::new);
    }
}
