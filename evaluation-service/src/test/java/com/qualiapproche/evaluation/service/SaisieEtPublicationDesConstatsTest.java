package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.config.PermissionChecker;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.dto.CompilationDto;
import com.qualiapproche.evaluation.dto.ConstatDto;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.Auditeur;
import com.qualiapproche.evaluation.entities.ChecklistAudit;
import com.qualiapproche.evaluation.entities.Constat;
import com.qualiapproche.evaluation.entities.PointControle;
import com.qualiapproche.evaluation.entities.PreuveConstat;
import com.qualiapproche.evaluation.entities.TypeConstat;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import com.qualiapproche.evaluation.enumeration.StatutAuditeur;
import com.qualiapproche.evaluation.enumeration.StatutConstat;
import com.qualiapproche.evaluation.repository.ChecklistAuditRepository;
import com.qualiapproche.evaluation.repository.ConstatRepository;
import com.qualiapproche.evaluation.repository.PreuveConstatRepository;
import com.qualiapproche.evaluation.repository.TypeConstatRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Les trois paliers d'un constat vus du service : qui le voit, qui le touche, et ce que sa
 * validation déclenche.
 *
 * <p>Un brouillon reste à son auteur, une compilation à l'équipe, un constat publié à personne ;
 * et la validation seule fait partir les écarts vers le module Non-conformités. Ces tests gardent
 * ces frontières, que l'écran de compilation suppose sans les revérifier.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SaisieEtPublicationDesConstatsTest {

    private static final UUID DIRECTION = UUID.randomUUID();

    @Mock private ConstatRepository depot;
    @Mock private TypeConstatRepository natures;
    @Mock private ChecklistAuditRepository checklists;
    @Mock private AuditService audits;
    @Mock private AuditeurService auditeurs;
    @Mock private PermissionChecker permissions;
    @Mock private ApplicationEventPublisher evenements;
    @Mock private PreuveConstatRepository preuves;
    @Mock private FichiersAudit fichiers;

    @InjectMocks private ConstatService service;

    private Auditeur moi;
    private Auditeur collegue;
    private Audit audit;
    private TypeConstat ecart;
    private TypeConstat pointFort;

    private static void connecter(String sujet, UUID direction) {
        Jwt jwt = Jwt.withTokenValue("jeton").header("alg", "none")
                .subject(sujet).claim("direction_id", direction.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private static Auditeur auditeur(String utilisateurId) {
        Auditeur auditeur = Auditeur.builder().utilisateurId(utilisateurId).statut(StatutAuditeur.ACTIF).build();
        auditeur.setId(UUID.randomUUID());
        auditeur.setDirectionId(DIRECTION);
        return auditeur;
    }

    private static TypeConstat nature(String libelle, boolean genereNc) {
        TypeConstat nature = TypeConstat.builder().libelle(libelle).genereNonConformite(genereNc).build();
        nature.setId(UUID.randomUUID());
        nature.setDirectionId(DIRECTION);
        return nature;
    }

    private Constat constat(Auditeur redacteur, StatutConstat statut, TypeConstat nature) {
        Constat constat = Constat.builder().audit(audit).redacteur(redacteur).statut(statut).nature(nature)
                .observation("Procédure non à jour").dateSaisie(LocalDateTime.now()).build();
        constat.setId(UUID.randomUUID());
        constat.setDirectionId(DIRECTION);
        when(depot.findById(constat.getId())).thenReturn(Optional.of(constat));
        return constat;
    }

    @BeforeEach
    void preparer() {
        moi = auditeur("auditeur-moi");
        collegue = auditeur("auditeur-collegue");
        connecter(moi.getUtilisateurId(), DIRECTION);
        audit = Audit.builder().reference("AUD-2026-011").statut(StatutAudit.EN_COURS).build();
        audit.setId(UUID.randomUUID());
        audit.setDirectionId(DIRECTION);
        audit.setResponsableEquipe(moi);
        audit.getMembresEquipe().add(collegue);
        ecart = nature("Non-conformité", true);
        pointFort = nature("Point fort", false);
        when(audits.sienOuRien(audit.getId())).thenReturn(audit);
        when(auditeurs.auditeurCourant()).thenReturn(moi);
        when(depot.save(any(Constat.class))).thenAnswer(i -> i.getArgument(0));
        when(depot.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(natures.findById(ecart.getId())).thenReturn(Optional.of(ecart));
        when(natures.findById(pointFort.getId())).thenReturn(Optional.of(pointFort));
    }

    @AfterEach
    void deconnecter() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------ Visibilité

    @Test
    @DisplayName("Le brouillon d'un collègue est introuvable, et non « refusé »")
    void le_brouillon_d_un_collegue_est_introuvable() {
        Constat sien = constat(collegue, StatutConstat.BROUILLON, ecart);

        assertThatThrownBy(() -> service.lire(sien.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("Un constat compilé se lit par toute l'équipe, pas seulement par son auteur")
    void un_constat_compile_se_lit_par_l_equipe() {
        Constat sien = constat(collegue, StatutConstat.COMPILE, ecart);

        ConstatDto lu = service.lire(sien.getId());

        assertThat(lu.getRedacteurUtilisateurId()).isEqualTo("auditeur-collegue");
        assertThat(lu.isNonConformite()).isTrue();
    }

    @Test
    @DisplayName("Le constat d'une autre direction est introuvable")
    void le_constat_d_une_autre_direction_est_introuvable() {
        Constat ailleurs = constat(moi, StatutConstat.COMPILE, ecart);
        ailleurs.setDirectionId(UUID.randomUUID());

        assertThatThrownBy(() -> service.lire(ailleurs.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Aucun(e) constat");
    }

    @Test
    @DisplayName("Un compte sans direction ne lit aucun constat")
    void un_compte_sans_direction_ne_lit_rien() {
        Constat mien = constat(moi, StatutConstat.BROUILLON, ecart);
        Jwt jwt = Jwt.withTokenValue("jeton").header("alg", "none").subject(moi.getUtilisateurId()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertThatThrownBy(() -> service.lire(mien.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    // ------------------------------------------------------------------ Saisie

    @Test
    @DisplayName("Le rédacteur d'un nouveau constat est l'appelant, et le constat part en brouillon")
    void un_nouveau_constat_part_en_brouillon_au_nom_de_l_appelant() {
        ConstatDto saisi = service.creer(audit.getId(),
                ConstatDto.builder().observation("  Registre non tenu  ").natureId(ecart.getId()).build());

        assertThat(saisi.getStatut()).isEqualTo(StatutConstat.BROUILLON);
        assertThat(saisi.getRedacteurId()).isEqualTo(moi.getId());
        assertThat(saisi.getObservation()).isEqualTo("Registre non tenu");
        assertThat(saisi.getDateSaisie()).isNotNull();
    }

    @Test
    @DisplayName("On ne saisit pas de constat sur un audit qui n'est pas en cours")
    void pas_de_constat_hors_audit_en_cours() {
        audit.setStatut(StatutAudit.EN_PREPARATION);

        assertThatThrownBy(() -> service.creer(audit.getId(), ConstatDto.builder().observation("Écart").build()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("audit en cours");
        verify(depot, never()).save(any());
    }

    @Test
    @DisplayName("Qui n'est pas de l'équipe ne saisit pas de constat sur l'audit")
    void hors_equipe_pas_de_saisie() {
        when(auditeurs.auditeurCourant()).thenReturn(auditeur("intrus"));

        assertThatThrownBy(() -> service.creer(audit.getId(), ConstatDto.builder().observation("Écart").build()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(depot, never()).save(any());
    }

    @Test
    @DisplayName("Un point de contrôle ne se cite pas sans sa checklist")
    void un_point_de_controle_ne_se_cite_pas_sans_checklist() {
        assertThatThrownBy(() -> service.creer(audit.getId(), ConstatDto.builder().observation("Écart")
                .pointControleId(UUID.randomUUID()).build()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("avec sa checklist");
    }

    @Test
    @DisplayName("Un point de contrôle d'une autre checklist est refusé")
    void un_point_d_une_autre_checklist_est_refuse() {
        ChecklistAudit checklist = ChecklistAudit.builder().nom("Achats").build();
        checklist.setId(UUID.randomUUID());
        checklist.setDirectionId(DIRECTION);
        PointControle point = PointControle.builder().checklist(checklist).libelle("Bons de commande").build();
        point.setId(UUID.randomUUID());
        checklist.getPoints().add(point);
        when(checklists.findById(checklist.getId())).thenReturn(Optional.of(checklist));

        assertThatThrownBy(() -> service.creer(audit.getId(), ConstatDto.builder().observation("Écart")
                .checklistId(checklist.getId()).pointControleId(UUID.randomUUID()).build()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Achats");

        ConstatDto saisi = service.creer(audit.getId(), ConstatDto.builder().observation("Écart")
                .checklistId(checklist.getId()).pointControleId(point.getId()).build());
        assertThat(saisi.getPointControleId()).isEqualTo(point.getId());
    }

    @Test
    @DisplayName("Une nature d'une autre direction ne s'attache pas à un constat")
    void une_nature_d_une_autre_direction_ne_s_attache_pas() {
        TypeConstat etrangere = nature("Écart", true);
        etrangere.setDirectionId(UUID.randomUUID());
        when(natures.findById(etrangere.getId())).thenReturn(Optional.of(etrangere));

        assertThatThrownBy(() -> service.creer(audit.getId(), ConstatDto.builder().observation("Écart")
                .natureId(etrangere.getId()).build()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("nature de constat");
    }

    // ------------------------------------------------------------------ Modification

    @Test
    @DisplayName("Un constat publié est verrouillé")
    void un_constat_publie_est_verrouille() {
        Constat publie = constat(moi, StatutConstat.PUBLIE, ecart);

        assertThatThrownBy(() -> service.modifier(publie.getId(),
                ConstatDto.builder().observation("Réécrit").build()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("verrouillé");
        assertThat(publie.getObservation()).isEqualTo("Procédure non à jour");
    }

    @Test
    @DisplayName("Le commentaire d'équipe ne s'écrit qu'à la compilation, jamais sur un brouillon")
    void le_commentaire_d_equipe_ne_s_ecrit_qu_a_la_compilation() {
        Constat brouillon = constat(moi, StatutConstat.BROUILLON, ecart);
        Constat compile = constat(collegue, StatutConstat.COMPILE, ecart);
        ConstatDto saisie = ConstatDto.builder().observation("Revu").natureId(ecart.getId())
                .commentaireEquipe("À reformuler").build();

        service.modifier(brouillon.getId(), saisie);
        service.modifier(compile.getId(), saisie);

        assertThat(brouillon.getCommentaireEquipe()).isNull();
        assertThat(compile.getCommentaireEquipe()).isEqualTo("À reformuler");
    }

    @Test
    @DisplayName("Un constat compilé ne se modifie pas hors de l'équipe")
    void un_compile_ne_se_modifie_pas_hors_equipe() {
        Constat compile = constat(collegue, StatutConstat.COMPILE, ecart);
        when(auditeurs.auditeurCourant()).thenReturn(auditeur("intrus"));

        assertThatThrownBy(() -> service.modifier(compile.getId(),
                ConstatDto.builder().observation("Réécrit").build()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("équipe");
    }

    @Test
    @DisplayName("Un audit clôturé ne laisse plus retoucher ses constats, même en brouillon")
    void un_audit_cloture_fige_ses_constats() {
        Constat brouillon = constat(moi, StatutConstat.BROUILLON, ecart);
        audit.setStatut(StatutAudit.CLOTURE);

        assertThatThrownBy(() -> service.modifier(brouillon.getId(),
                ConstatDto.builder().observation("Réécrit").build()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("n'est pas en cours");
    }

    @Test
    @DisplayName("Seul un brouillon s'efface : un constat compilé appartient à l'équipe")
    void seul_un_brouillon_s_efface() {
        Constat brouillon = constat(moi, StatutConstat.BROUILLON, ecart);
        Constat compile = constat(moi, StatutConstat.COMPILE, ecart);

        service.supprimer(brouillon.getId());
        assertThatThrownBy(() -> service.supprimer(compile.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("renvoyer en brouillon");

        verify(depot).delete(brouillon);
        verify(depot, never()).delete(compile);
    }

    @Test
    @DisplayName("Le renvoi garde sa raison à part, sans effacer le commentaire de la réunion")
    void le_renvoi_garde_sa_raison_sans_effacer_le_commentaire_de_reunion() {
        Constat compile = constat(collegue, StatutConstat.COMPILE, ecart);
        compile.setCommentaireEquipe("Requalifié en mineure après échange avec le pilote");

        ConstatDto renvoye = service.retirerVersBrouillon(compile.getId(), "  Preuve manquante ");

        assertThat(renvoye.getStatut()).isEqualTo(StatutConstat.BROUILLON);
        assertThat(renvoye.isRenvoye()).isTrue();
        assertThat(renvoye.getMotifRenvoi()).isEqualTo("Preuve manquante");
        assertThat(renvoye.getRenvoyeLe()).isNotNull();
        // Le motif s'écrivait dans ce champ et effaçait la réunion (recette du 17 oct. 2026, obs. 23).
        assertThat(renvoye.getCommentaireEquipe()).isEqualTo("Requalifié en mineure après échange avec le pilote");
    }

    // ------------------------------------------------------------------ Publication

    @Test
    @DisplayName("Publier tous mes constats laisse de côté les brouillons sans nature")
    void publier_tous_mes_constats_ecarte_les_brouillons_sans_nature() {
        Constat pret = constat(moi, StatutConstat.BROUILLON, ecart);
        Constat inacheve = constat(moi, StatutConstat.BROUILLON, null);
        when(depot.findAllByAuditIdAndRedacteurIdAndStatutOrderByDateSaisieAsc(audit.getId(), moi.getId(),
                StatutConstat.BROUILLON)).thenReturn(List.of(pret, inacheve));

        List<ConstatDto> publies = service.publierTousMesConstats(audit.getId());

        assertThat(publies).extracting(ConstatDto::getId).containsExactly(pret.getId());
        assertThat(pret.getStatut()).isEqualTo(StatutConstat.COMPILE);
        assertThat(inacheve.getStatut()).isEqualTo(StatutConstat.BROUILLON);
    }

    @Test
    @DisplayName("Valider un constat qui n'est pas un écart prévient le pilote sans rien transmettre à M05")
    void valider_un_point_fort_ne_transmet_rien() {
        Constat compile = constat(collegue, StatutConstat.COMPILE, pointFort);

        service.valider(compile.getId());

        ArgumentCaptor<Object> annonces = ArgumentCaptor.forClass(Object.class);
        verify(evenements).publishEvent(annonces.capture());
        assertThat(annonces.getAllValues()).singleElement().isInstanceOf(ConstatsPublies.class);
        assertThat(compile.getStatut()).isEqualTo(StatutConstat.PUBLIE);
        assertThat(compile.getDatePublication()).isNotNull();
    }

    @Test
    @DisplayName("Valider un écart l'annonce à M05 et au pilote")
    void valider_un_ecart_l_annonce_a_m05() {
        Constat compile = constat(collegue, StatutConstat.COMPILE, ecart);

        ConstatDto publie = service.valider(compile.getId());

        verify(evenements).publishEvent(EcartsPublies.deConstats(List.of(compile.getId())));
        verify(evenements).publishEvent(new ConstatsPublies(audit.getId(), List.of(compile.getId())));
        assertThat(publie.isTransmissionEnAttente()).isTrue();
    }

    @Test
    @DisplayName("Qui détient audit-validate valide sans être de l'équipe")
    void le_valideur_valide_sans_etre_de_l_equipe() {
        Constat compile = constat(collegue, StatutConstat.COMPILE, pointFort);
        when(auditeurs.auditeurCourant()).thenReturn(auditeur("direction-qualite"));
        when(permissions.detient("audit-validate")).thenReturn(true);

        assertThat(service.valider(compile.getId()).getStatut()).isEqualTo(StatutConstat.PUBLIE);
    }

    @Test
    @DisplayName("Ni de l'équipe ni valideur : la validation est refusée")
    void ni_equipe_ni_valideur_ne_valide_pas() {
        Constat compile = constat(collegue, StatutConstat.COMPILE, pointFort);
        when(auditeurs.auditeurCourant()).thenReturn(auditeur("intrus"));

        assertThatThrownBy(() -> service.valider(compile.getId())).isInstanceOf(BusinessException.class);
        assertThat(compile.getStatut()).isEqualTo(StatutConstat.COMPILE);
        verify(evenements, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Valider un groupe ne publie que les constats compilés de cette nature")
    void valider_un_groupe_ne_publie_que_sa_nature() {
        Constat ecartCompile = constat(collegue, StatutConstat.COMPILE, ecart);
        Constat fortCompile = constat(collegue, StatutConstat.COMPILE, pointFort);
        Constat sansNature = constat(collegue, StatutConstat.COMPILE, null);
        when(depot.findAllByAuditIdAndStatutOrderByDateSaisieAsc(audit.getId(), StatutConstat.COMPILE))
                .thenReturn(List.of(ecartCompile, fortCompile, sansNature));

        List<ConstatDto> publies = service.validerLaCompilation(audit.getId(), pointFort.getId());

        assertThat(publies).extracting(ConstatDto::getId).containsExactly(fortCompile.getId());
        assertThat(ecartCompile.getStatut()).isEqualTo(StatutConstat.COMPILE);
        assertThat(sansNature.getStatut()).isEqualTo(StatutConstat.COMPILE);
        verify(evenements, never()).publishEvent(any(EcartsPublies.class));
    }

    @Test
    @DisplayName("Rien à valider : aucun courriel au pilote, aucune transmission")
    void rien_a_valider_rien_n_est_annonce() {
        when(depot.findAllByAuditIdAndStatutOrderByDateSaisieAsc(audit.getId(), StatutConstat.COMPILE))
                .thenReturn(List.of());

        assertThat(service.validerLaCompilation(audit.getId(), null)).isEmpty();
        verify(evenements, never()).publishEvent(any());
    }

    // ------------------------------------------------------------------ Synthèse et preuves

    @Test
    @DisplayName("La synthèse ne compte que les natures présentes, compilés et publiés distingués")
    void la_synthese_compte_par_nature() {
        Constat c1 = constat(moi, StatutConstat.COMPILE, ecart);
        Constat c2 = constat(collegue, StatutConstat.PUBLIE, ecart);
        Constat c3 = constat(collegue, StatutConstat.PUBLIE, null);
        when(depot.findAllByAuditIdAndStatutInOrderByDateSaisieAsc(any(), any())).thenReturn(List.of(c1, c2, c3));
        when(natures.findAllByDirectionIdOrderByCreatedAtAsc(DIRECTION)).thenReturn(List.of(pointFort, ecart));

        CompilationDto synthese = service.synthese(audit.getId());

        assertThat(synthese.getTotal()).isEqualTo(3);
        assertThat(synthese.getCompiles()).isEqualTo(1);
        assertThat(synthese.getPublies()).isEqualTo(2);
        assertThat(synthese.getParNature()).singleElement().satisfies(g -> {
            assertThat(g.getNatureId()).isEqualTo(ecart.getId());
            assertThat(g.getNombre()).isEqualTo(2);
            assertThat(g.getCompiles()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("La preuve d'un brouillon d'autrui ne se télécharge pas")
    void la_preuve_d_un_brouillon_d_autrui_ne_se_telecharge_pas() {
        Constat sien = constat(collegue, StatutConstat.BROUILLON, ecart);
        PreuveConstat preuve = PreuveConstat.builder().constatId(sien.getId()).nom("photo.jpg").url("cle").build();
        preuve.setId(UUID.randomUUID());
        preuve.setDirectionId(DIRECTION);
        when(preuves.findById(preuve.getId())).thenReturn(Optional.of(preuve));

        assertThatThrownBy(() -> service.preuve(preuve.getId())).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Une preuve ne se retire pas d'un constat publié : le fichier reste au dossier")
    void une_preuve_ne_se_retire_pas_d_un_constat_publie() {
        Constat publie = constat(moi, StatutConstat.PUBLIE, ecart);
        PreuveConstat preuve = PreuveConstat.builder().constatId(publie.getId()).nom("photo.jpg").url("cle").build();
        preuve.setId(UUID.randomUUID());
        preuve.setDirectionId(DIRECTION);
        when(preuves.findById(preuve.getId())).thenReturn(Optional.of(preuve));

        assertThatThrownBy(() -> service.retirerPreuve(preuve.getId())).isInstanceOf(BusinessException.class);
        verify(preuves, never()).delete(any());
        verify(fichiers, never()).retirer(any());
    }
}
