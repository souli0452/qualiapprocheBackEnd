package com.qualiapproche.ia.client;

import com.qualiapproche.common.dto.NcDashboardDto;
import com.qualiapproche.common.dto.NonConformiteDto;
import com.qualiapproche.ia.client.vue.PlanActionVue;
import com.qualiapproche.common.response.PaginatedResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import static com.qualiapproche.common.utils.ApiUrls.NON_CONFORMITE_ROOT_URL;
import static com.qualiapproche.common.utils.ApiUrls.PLAN_ACTION_ROOT_URL;

/**
 * Ce que l'assistant sait demander au module des non-conformités.
 *
 * <p>L'appel part avec le jeton et les permissions de la personne qui a posé la question — voir
 * {@code FeignConfig}. C'est le service métier qui décide alors de ce qu'elle voit, exactement
 * comme si elle avait ouvert l'écran : l'assistant n'ouvre aucune porte que l'écran garderait
 * fermée.</p>
 */
@FeignClient(name = "amelioration-service")
public interface AmeliorationClient {

    /**
     * Les non-conformités sur lesquelles l'appelant peut décider maintenant.
     *
     * <p>C'est le circuit qui les désigne, puisque c'est lui qui porte l'habilitation de chaque
     * étape — et non un filtre sur l'état, qui rendrait les dossiers des autres structures.</p>
     */
    @GetMapping(NON_CONFORMITE_ROOT_URL + "/a-traiter")
    PaginatedResponse<NonConformiteDto> aTraiter(@RequestParam("page") int page,
                                                 @RequestParam("size") int size);

    /**
     * Les plans d'action sur lesquels l'appelant doit agir — même désignation par le circuit que
     * pour les non-conformités.
     */
    @GetMapping(PLAN_ACTION_ROOT_URL + "/a-traiter")
    PaginatedResponse<PlanActionVue> plansATraiter(@RequestParam("page") int page,
                                                   @RequestParam("size") int size);

    /** Les chiffres de l'organisation entière. */
    @GetMapping(NON_CONFORMITE_ROOT_URL + "/dashboard/rq")
    NcDashboardDto tableauDeBordOrganisation();

    /** Les chiffres d'une structure — le module refuse celle d'un autre. */
    @GetMapping(NON_CONFORMITE_ROOT_URL + "/dashboard/pilot/{structureId}")
    NcDashboardDto tableauDeBordStructure(@PathVariable("structureId") String structureId);

    /** Les chiffres d'une personne — le module refuse ceux d'un autre. */
    @GetMapping(NON_CONFORMITE_ROOT_URL + "/dashboard/user/{userId}")
    NcDashboardDto tableauDeBordPersonne(@PathVariable("userId") String userId);
}
