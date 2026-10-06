package com.qualiapproche.evaluation.service.document;

import com.qualiapproche.common.dto.StructureDto;
import com.qualiapproche.evaluation.client.AnnuaireClient;
import com.qualiapproche.evaluation.client.StructuresClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Les noms à imprimer dans un document : personnes et structures.
 *
 * <p>Le module ne garde que des identifiants — Keycloak pour les personnes, le référentiel pour
 * les structures. Un nom introuvable n'empêche pas le document : l'identifiant est imprimé à sa
 * place, ce qui se lit mal mais ne trompe personne.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Annuaire {

    private final AnnuaireClient personnes;
    private final StructuresClient structures;

    public String personne(String utilisateurId) {
        if (utilisateurId == null) {
            return "—";
        }
        try {
            Map<String, Object> u = personnes.utilisateur(utilisateurId);
            if (u != null) {
                Object complet = u.get("fullName");
                if (complet != null && !complet.toString().isBlank()) {
                    return complet.toString();
                }
                String compose = (texte(u.get("firstName")) + " " + texte(u.get("lastName"))).trim();
                if (!compose.isBlank()) {
                    return compose;
                }
            }
        } catch (Exception e) {
            log.warn("Nom de l'utilisateur {} illisible pour le document : {}", utilisateurId, e.getMessage());
        }
        return utilisateurId;
    }

    public String structure(UUID structureId) {
        if (structureId == null) {
            return "—";
        }
        try {
            StructureDto s = structures.structure(structureId);
            if (s != null && s.getLibelleLong() != null) {
                return s.getLibelleLong();
            }
        } catch (Exception e) {
            log.warn("Structure {} illisible pour le document : {}", structureId, e.getMessage());
        }
        return structureId.toString();
    }

    private static String texte(Object valeur) {
        return valeur == null ? "" : valeur.toString();
    }
}
