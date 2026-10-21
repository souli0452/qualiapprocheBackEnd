package com.qualiapproche.amelioration.service.impl;

import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.PieceJointe;
import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.PlanActionRepository;
import com.qualiapproche.amelioration.service.NonConformiteService;
import com.qualiapproche.common.config.PermissionChecker;
import com.qualiapproche.common.utils.PermissionsPortee;
import com.qualiapproche.common.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Qui peut toucher à un dossier par son identifiant : ses fichiers, et ses plans d'action.
 *
 * <p>La fiche d'une non-conformité est bornée par {@link NonConformiteService#peutConsulter}, mais
 * ses fichiers ne l'étaient pas : le dépôt, le téléchargement et la suppression d'une pièce ne
 * demandaient que la permission générale du module. Un agent qui tenait l'identifiant d'un dossier
 * d'une autre structure — ou celui d'une pièce, qui circule dans les saisies du circuit — lisait
 * ses preuves, en déposait, ou les détruisait. La suppression était la plus grave : elle effaçait
 * la preuve d'un enregistrement qualité, que la traçabilité interdit de perdre.</p>
 *
 * <p>La règle n'est pas réinventée ici : un fichier se suit comme son dossier. La pièce d'un plan
 * d'action relève de la non-conformité qui motive ce plan, comme son rangement sous la structure
 * de cette non-conformité ({@link PlanActionFichierService}).</p>
 *
 * <p>Les plans d'action eux-mêmes suivent la même règle : leur fiche joint la non-conformité
 * entière qui les motive, si bien que lire, corriger ou supprimer un plan par son identifiant
 * revenait à contourner la borne du dossier. Un plan se lit, se corrige et se supprime là où sa
 * non-conformité se consulte.</p>
 *
 * <p>La réponse est 404, comme pour la fiche : « il existe, mais pas pour vous » en dirait trop.</p>
 *
 * <p>Ce contrôle vit à part, et non dans {@link NonConformiteFichierService} ou
 * {@link PieceJointeStockageService} : le service des non-conformités dépend déjà de ces deux-là,
 * et les faire dépendre de lui aurait refermé un cycle.</p>
 */
@Component
@RequiredArgsConstructor
public class AccesAuxDossiers {

    private static final String INTROUVABLE = "Ce dossier n'existe pas.";

    private final NonConformiteRepository nonConformiteRepository;
    private final PlanActionRepository planActionRepository;
    private final NonConformiteService nonConformiteService;
    private final PermissionChecker permissionChecker;

    /** Exige que l'appelant puisse consulter cette non-conformité. */
    @Transactional(readOnly = true)
    public void exigerLaNonConformite(UUID nonConformiteId) {
        NonConformite nc = nonConformiteRepository.findById(nonConformiteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, INTROUVABLE));
        exigerConsultable(nc);
    }

    /**
     * Exige que l'appelant puisse consulter le dossier dont relève ce plan d'action.
     *
     * <p>Garde la fiche, la correction et la suppression d'un plan comme ses fichiers.</p>
     */
    @Transactional(readOnly = true)
    public void exigerLePlan(UUID planActionId) {
        PlanAction plan = planActionRepository.findById(planActionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, INTROUVABLE));
        exigerLeDossierDuPlan(plan);
    }

    /**
     * Exige que l'appelant puisse consulter le dossier auquel la pièce est rattachée.
     *
     * <p>Une pièce porte l'identifiant de son dossier sans dire de quel genre il est : on cherche
     * d'abord une non-conformité, puis un plan d'action. Une pièce rattachée à rien de connu reste
     * introuvable, plutôt qu'ouverte à tous.</p>
     */
    @Transactional(readOnly = true)
    public void exigerLaPiece(PieceJointe piece) {
        UUID dossier = piece.getEntityId();
        if (dossier == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, INTROUVABLE);
        }
        var nc = nonConformiteRepository.findById(dossier);
        if (nc.isPresent()) {
            exigerConsultable(nc.get());
            return;
        }
        PlanAction plan = planActionRepository.findById(dossier)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, INTROUVABLE));
        exigerLeDossierDuPlan(plan);
    }

    /**
     * Un plan sans non-conformité n'a pas de structure à laquelle se rattacher : seule la portée
     * transverse, ou son responsable, y accède. Le cas ne se produit pas aujourd'hui — tout plan
     * naît d'un dossier — mais une donnée ancienne ne doit pas devenir une porte ouverte.
     */
    private void exigerLeDossierDuPlan(PlanAction plan) {
        if (plan.getNonConformeId() == null) {
            String moi = SecurityUtils.getCurrentUserId();
            boolean responsable = moi != null && plan.getResponsableId() != null
                    && moi.equals(plan.getResponsableId().toString());
            if (responsable || permissionChecker.detient(PermissionsPortee.TOUTES_STRUCTURES)) {
                return;
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, INTROUVABLE);
        }
        exigerLaNonConformite(plan.getNonConformeId());
    }

    private void exigerConsultable(NonConformite nc) {
        if (!nonConformiteService.peutConsulter(nc)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, INTROUVABLE);
        }
    }
}
