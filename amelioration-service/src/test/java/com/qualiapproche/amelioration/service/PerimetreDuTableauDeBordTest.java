package com.qualiapproche.amelioration.service;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qualiapproche.amelioration.client.ReferentielClient;
import com.qualiapproche.amelioration.client.WorkflowClient;
import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.mappers.NonConformiteMapper;
import com.qualiapproche.amelioration.entities.mappers.PlanActionMapper;
import com.qualiapproche.amelioration.repository.ActionRepository;
import com.qualiapproche.amelioration.repository.EfficaciteRepository;
import com.qualiapproche.amelioration.repository.NiveauNonConformiteRepository;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.PieceJointeRepository;
import com.qualiapproche.amelioration.repository.PlanActionRepository;
import com.qualiapproche.amelioration.repository.TypeNonConformiteRepository;
import com.qualiapproche.amelioration.service.impl.NonConformiteFichierService;
import com.qualiapproche.amelioration.service.impl.NonConformiteServiceImpl;
import com.qualiapproche.amelioration.service.impl.PieceJointeStockageService;
import com.qualiapproche.amelioration.service.impl.PlansActionDeLaNonConformiteService;
import com.qualiapproche.common.config.PermissionChecker;
import com.qualiapproche.common.service.SendMailService;

/**
 * Sur qui l'on peut demander un tableau de bord.
 *
 * <p>La permission dit ce que l'appelant peut <b>consulter</b> — ses dossiers, ceux d'une
 * structure, ceux de l'organisme. Elle ne dit pas <b>sur qui</b> : les deux points d'entrée bornés
 * prennent leur périmètre dans l'URL, et rien ne le confrontait à l'appelant. Il suffisait de
 * connaître l'identifiant d'une structure — ou d'un collègue — pour lire ses chiffres.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PerimetreDuTableauDeBordTest {

    private static final String MOI = "agent-1";
    private static final String MA_STRUCTURE = "structure-1";
    private static final String UNE_AUTRE = "structure-2";

    @Mock private NonConformiteRepository nonConformiteRepository;
    @Mock private PieceJointeRepository pieceJointeRepository;
    @Mock private NonConformiteMapper nonConformiteMapper;
    @Mock private PlanActionMapper planActionMapper;
    @Mock private TypeNonConformiteRepository typeNonConformiteRepository;
    @Mock private ReferentielClient referentielClient;
    @Mock private EfficaciteRepository efficaciteRepository;
    @Mock private ActionRepository actionRepository;
    @Mock private NiveauNonConformiteRepository niveauNonConformiteRepository;
    @Mock private PieceJointeStockageService fichierService;
    @Mock private NonConformiteFichierService ncFichierService;
    @Mock private SendMailService sendMailService;
    @Mock private PlanActionRepository planActionRepository;
    @Mock private WorkflowClient workflowClient;
    @Mock private PlansActionDeLaNonConformiteService plansActionService;
    @Mock private PermissionChecker permissionChecker;

    @InjectMocks private NonConformiteServiceImpl service;

    @BeforeEach
    void connecter() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                Jwt.withTokenValue("jeton").header("alg", "none")
                        .subject(MOI).claim("structure_id", MA_STRUCTURE).build()));
        Page<NonConformite> aucun = new PageImpl<>(List.of());
        when(nonConformiteRepository.findAllByStructureSoumissionIdOrOrigineId(
                anyString(), anyString(), any(Pageable.class))).thenReturn(aucun);
        when(nonConformiteRepository.findAllByUserInvolved(anyString(), any(Pageable.class)))
                .thenReturn(aucun);
        when(planActionRepository.findByNonConformeIdIn(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of());
    }

    @AfterEach
    void deconnecter() {
        SecurityContextHolder.clearContext();
    }

    /** L'appelant porte-t-il une portée transverse aux structures ? */
    private void porteeGlobale(boolean transverse) {
        // « detient » prend un varargs : c'est bien un String[] qu'il faut apparier, un matcher
        // d'argument simple ne s'accroche à rien et laisse le mock répondre « false ».
        when(permissionChecker.detient(any(String[].class))).thenReturn(transverse);
    }

    @Test
    @DisplayName("Le tableau d'une autre structure est refusé")
    void autreStructure_refusee() {
        porteeGlobale(false);

        assertThatThrownBy(() -> service.getDashboardPilot(UNE_AUTRE))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
        // Rien n'est lu : le refus précède la requête, il ne la filtre pas après coup.
        verify(nonConformiteRepository, never()).findAllByStructureSoumissionIdOrOrigineId(
                anyString(), anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("Le tableau d'un collègue est refusé")
    void autrePersonne_refusee() {
        porteeGlobale(false);

        // Le tableau d'une personne dit ce qu'elle a déclaré et ce qui lui est imputé : ce n'est
        // pas une donnée publique dans l'organisation.
        assertThatThrownBy(() -> service.getDashboardUser("agent-2"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("Sa propre structure et son propre tableau restent ouverts")
    void sonPerimetre_ouvert() {
        porteeGlobale(false);
        when(workflowClient.avancementDesRessources(anyList())).thenReturn(java.util.Map.of());

        assertThat(service.getDashboardPilot(MA_STRUCTURE)).isNotNull();
        assertThat(service.getDashboardUser(MOI)).isNotNull();
    }

    @Test
    @DisplayName("La portée transverse passe outre : c'est le sens même de sa fonction")
    void porteeGlobale_passeOutre() {
        porteeGlobale(true);
        when(workflowClient.avancementDesRessources(anyList())).thenReturn(java.util.Map.of());

        assertThat(service.getDashboardPilot(UNE_AUTRE)).isNotNull();
        assertThat(service.getDashboardUser("agent-2")).isNotNull();
    }

    @Test
    @DisplayName("Un jeton sans structure ne vaut pas laissez-passer")
    void structureInconnue_refusee() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                Jwt.withTokenValue("jeton").header("alg", "none").subject(MOI).build()));
        porteeGlobale(false);

        // Deux valeurs absentes ne sont pas deux valeurs égales : sans cela, un jeton dépourvu de
        // structure aurait ouvert toute structure dont l'identifiant est également absent.
        assertThatThrownBy(() -> service.getDashboardPilot(MA_STRUCTURE))
                .isInstanceOf(ResponseStatusException.class);
    }
}
