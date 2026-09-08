package com.qualiapproche.workflow.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.qualiapproche.common.dto.DepotNotificationDto;
import com.qualiapproche.common.enumeration.GraviteNotification;
import com.qualiapproche.workflow.model.NotificationUtilisateur;
import com.qualiapproche.workflow.repository.NotificationUtilisateurRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La boîte de réception : ce qu'un second dépôt fait de la ligne déjà posée, et à qui la boîte
 * appartient.
 *
 * <p>Ce qui se joue ici est la <b>clé d'unicité</b>. Sans elle, la relance quotidienne d'une
 * échéance déposerait la même ligne tous les matins et la boîte serait inutilisable au bout d'une
 * semaine ; avec elle mais sans la distinction du réveil, marquer lu ne servirait à rien.</p>
 */
class BoiteDeReceptionTest {

    private static final String MOI = "utilisateur-1";
    private static final String AUTRE = "utilisateur-2";

    private NotificationUtilisateurRepository repository;
    private NotificationsUtilisateurService service;
    private final List<NotificationUtilisateur> enregistrees = new ArrayList<>();

    @BeforeEach
    void setUp() {
        repository = mock(NotificationUtilisateurRepository.class);
        service = new NotificationsUtilisateurService(repository);
        enregistrees.clear();
        when(repository.save(any(NotificationUtilisateur.class))).thenAnswer(i -> {
            enregistrees.add(i.getArgument(0));
            return i.getArgument(0);
        });
        authentifier(MOI);
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private void authentifier(String userId) {
        Jwt jwt = Jwt.withTokenValue("jeton").header("alg", "none").subject(userId)
                .issuedAt(java.time.Instant.EPOCH)
                .expiresAt(java.time.Instant.EPOCH.plusSeconds(3600)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, null, List.of()));
    }

    private DepotNotificationDto depot(String cle, String message, boolean reveiller) {
        return DepotNotificationDto.builder()
                .destinataireId(MOI).cleUnicite(cle).code("TEST").source("AMELIORATION")
                .titre("Titre").message(message).gravite(GraviteNotification.INFO)
                .reveiller(reveiller).build();
    }

    private NotificationUtilisateur ligne(String cle, boolean lue) {
        NotificationUtilisateur n = NotificationUtilisateur.builder()
                .destinataireId(MOI).cleUnicite(cle).code("TEST").source("AMELIORATION")
                .titre("Titre").message("ancien message").gravite(GraviteNotification.INFO)
                .lue(lue).build();
        n.setId(UUID.randomUUID());
        return n;
    }

    @Test
    @DisplayName("Un premier dépôt crée la ligne")
    void premierDepot_creeLaLigne() {
        when(repository.findByDestinataireIdAndCleUnicite(MOI, "K")).thenReturn(Optional.empty());

        service.deposer(depot("K", "Trois plans en retard", false));

        assertThat(enregistrees).hasSize(1);
        assertThat(enregistrees.get(0).getMessage()).isEqualTo("Trois plans en retard");
        assertThat(enregistrees.get(0).isLue()).isFalse();
    }

    @Test
    @DisplayName("Le second dépôt sur la même clé met la ligne à jour au lieu d'en créer une")
    void secondDepot_metAJour() {
        // Le cas de la relance quotidienne : sans cela, sept lignes identiques en une semaine.
        NotificationUtilisateur existante = ligne("K", false);
        when(repository.findByDestinataireIdAndCleUnicite(MOI, "K")).thenReturn(Optional.of(existante));

        service.deposer(depot("K", "Cinq plans en retard", false));

        assertThat(enregistrees).containsExactly(existante);
        assertThat(existante.getMessage()).isEqualTo("Cinq plans en retard");
    }

    @Test
    @DisplayName("Sans réveil, une ligne déjà lue le reste : marquer lu fait taire l'alerte")
    void sansReveil_laLigneLueLeReste() {
        // Sans quoi le geste n'aurait aucun sens : l'alerte reviendrait le lendemain matin.
        NotificationUtilisateur existante = ligne("K", true);
        when(repository.findByDestinataireIdAndCleUnicite(MOI, "K")).thenReturn(Optional.of(existante));

        service.deposer(depot("K", "Toujours en retard", false));

        assertThat(existante.isLue()).isTrue();
    }

    @Test
    @DisplayName("Avec réveil, une ligne lue redevient non lue : le dossier revient, il faut y regarder")
    void avecReveil_laLigneRedevientNonLue() {
        NotificationUtilisateur existante = ligne("K", true);
        existante.marquerLue();
        when(repository.findByDestinataireIdAndCleUnicite(MOI, "K")).thenReturn(Optional.of(existante));

        service.deposer(depot("K", "Le dossier vous revient", true));

        assertThat(existante.isLue()).isFalse();
        assertThat(existante.getLueLe()).isNull();
    }

    @Test
    @DisplayName("Un dépôt sans destinataire est ignoré sans bruit")
    void depotSansDestinataire_ignore() {
        // Une étape dont le rôle n'a aucun porteur joignable : déjà signalé là où les destinataires
        // sont résolus, inutile d'échouer ici.
        service.deposer(DepotNotificationDto.builder().destinataireId(null).cleUnicite("K").build());

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Marquer lue une ligne qui n'est pas la sienne ne fait rien")
    void ligneDAutrui_nonMarquee() {
        NotificationUtilisateur dAutrui = ligne("K", false);
        dAutrui.setDestinataireId(AUTRE);
        when(repository.findById(dAutrui.getId())).thenReturn(Optional.of(dAutrui));

        assertThat(service.marquerLue(dAutrui.getId())).isFalse();
        assertThat(dAutrui.isLue()).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("La seconde lecture ne déplace pas l'horodatage")
    void secondeLecture_horodatageInchange() {
        NotificationUtilisateur mienne = ligne("K", false);
        when(repository.findById(mienne.getId())).thenReturn(Optional.of(mienne));

        service.marquerLue(mienne.getId());
        var premiereFois = mienne.getLueLe();
        service.marquerLue(mienne.getId());

        assertThat(mienne.getLueLe()).isEqualTo(premiereFois);
    }

    @Test
    @DisplayName("Sans utilisateur authentifié, la boîte est vide et rien ne se marque")
    void sansUtilisateur_boiteVide() {
        SecurityContextHolder.clearContext();

        assertThat(service.mesNotifications(false, PageRequest.of(0, 20))).isEmpty();
        assertThat(service.nombreDeNonLues()).isZero();
        assertThat(service.toutMarquerLu()).isZero();
        verify(repository, never()).marquerToutesLues(eq(MOI));
    }
}
