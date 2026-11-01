package com.qualiapproche.amelioration.controller;

import com.qualiapproche.amelioration.client.ReferentielClient;
import java.util.UUID;
import com.qualiapproche.amelioration.entities.SerieNumero;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.SerieNumeroRepository;
import com.qualiapproche.common.cloisonnement.Cloisonnement;
import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.serie.AttributionDeNumero;
import com.qualiapproche.common.serie.SerieController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.Set;

/** Les séries de numérotation des non-conformités : lecture et calage, réservés à l'administration. */
@RestController
@RequestMapping("/api/v1/series-numerotation")
@RequirePermissions(
        read = {"CONFIG_READ", "CONFIG_GLOBAL_MANAGE", "config-global-read", "config-global-write"},
        update = {"CONFIG_GLOBAL_MANAGE", "config-global-write"}
)
public class SerieNumeroController extends SerieController<SerieNumero> {

    private final AttributionDeNumero<SerieNumero> attribution;
    private final NonConformiteRepository nonConformites;
    private final ReferentielClient referentiel;

    public SerieNumeroController(SerieNumeroRepository series, NonConformiteRepository nonConformites,
                                 ReferentielClient referentiel) {
        this.attribution = new AttributionDeNumero<>(series);
        this.nonConformites = nonConformites;
        this.referentiel = referentiel;
    }

    /**
     * La série est-elle portée par ce client : sa direction, une structure où il a déjà des
     * dossiers, ou une structure que referentiel lui reconnaît (une structure neuve, pas encore de
     * dossier) — referentiel ne rend pas la structure d'un autre client.
     */
    @Override
    protected boolean appartientAuClient(String cle) {
        return estAuClient(porteur(cle));
    }

    private boolean estAuClient(String porteur) {
        if (porteurs().contains(porteur)) {
            return true;
        }
        UUID structure = Cloisonnement.uuid(porteur);
        if (structure == null) {
            return false;
        }
        try {
            return referentiel.getStructureById(structure) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /** Ce qui porte les séries du client : ses structures de soumission, et sa direction. */
    private Set<String> porteurs() {
        Set<String> porteurs = new HashSet<>(nonConformites.structuresDeSoumission());
        porteurs.add("D:" + Cloisonnement.courante());
        return porteurs;
    }

    private static String porteur(String cle) {
        String[] parties = cle == null ? new String[0] : cle.split("\\|");
        return parties.length >= 2 ? parties[1] : "";
    }

    @Override
    protected AttributionDeNumero<SerieNumero> attribution() {
        return attribution;
    }

    @Override
    protected SerieNumero nouvelle() {
        return new SerieNumero();
    }
}
