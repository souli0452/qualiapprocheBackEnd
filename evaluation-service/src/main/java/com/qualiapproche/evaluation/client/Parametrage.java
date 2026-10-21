package com.qualiapproche.evaluation.client;

import com.qualiapproche.common.dto.AuditEntityDto;
import com.qualiapproche.common.dto.CritereEvaluationAuditeurDto;
import com.qualiapproche.common.dto.NiveauEvaluationAuditeurDto;
import com.qualiapproche.common.dto.SiteDto;
import com.qualiapproche.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Les sites, critères et niveaux d'évaluation des auditeurs, lus dans referentiel-service.
 *
 * <p>Deux usages, deux conduites quand le référentiel ne répond pas :</p>
 * <ul>
 *   <li><b>choisir</b> — rattacher un site à un audit, noter un auditeur — exige les entrées
 *       actives, et échoue en 503 en disant pourquoi : on ne valide pas un choix qu'on ne peut
 *       pas vérifier ;</li>
 *   <li><b>afficher</b> un dossier lit aussi les entrées retirées, et se contente d'une table
 *       vide si le référentiel est muet : le dossier s'affiche, sans ses libellés, plutôt que pas
 *       du tout.</li>
 * </ul>
 *
 * <p>Une liste par appel, et non une requête par ligne : une page de dix évaluations ne doit pas
 * coûter dix allers-retours.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Parametrage {

    private final ParametrageClient client;

    /** Les sites, retirés compris, pour afficher ; vide si le référentiel ne répond pas. */
    public Map<UUID, SiteDto> sitesPourAffichage() {
        return pourAffichage(() -> client.sites(true), "sites");
    }

    public Map<UUID, CritereEvaluationAuditeurDto> criteresPourAffichage() {
        return pourAffichage(() -> client.criteres(true), "critères d'évaluation des auditeurs");
    }

    public Map<UUID, NiveauEvaluationAuditeurDto> niveauxPourAffichage() {
        return pourAffichage(() -> client.niveaux(true), "niveaux d'évaluation des auditeurs");
    }

    /** Vérifie que chaque site est un site actif de la direction ; échoue sur le premier qui ne l'est pas. */
    public void exigerDesSitesActifs(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return;
        }
        Map<UUID, SiteDto> actifs = pourChoisir(() -> client.sites(false));
        for (UUID id : ids) {
            exigerParmi(actifs, id, "site");
        }
    }

    public CritereEvaluationAuditeurDto critereActif(UUID id) {
        return exigerParmi(pourChoisir(() -> client.criteres(false)), id, "critère d'évaluation");
    }

    public NiveauEvaluationAuditeurDto niveauActif(UUID id) {
        return exigerParmi(pourChoisir(() -> client.niveaux(false)), id, "niveau d'évaluation");
    }

    private static <D> D exigerParmi(Map<UUID, D> actifs, UUID id, String quoi) {
        if (id == null) {
            throw new BusinessException("Identifiant manquant : " + quoi + ".", HttpStatus.BAD_REQUEST);
        }
        D trouve = actifs.get(id);
        if (trouve == null) {
            throw new BusinessException("Aucun(e) " + quoi + " actif(ve) ne porte cet identifiant : " + id,
                    HttpStatus.NOT_FOUND);
        }
        return trouve;
    }

    private static <D extends AuditEntityDto> Map<UUID, D> pourChoisir(Supplier<List<D>> lecture) {
        try {
            return parId(lecture.get());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Paramétrage illisible dans referentiel-service : {}", e.getMessage());
            throw new BusinessException("Le paramétrage (sites, échelle d'évaluation) est momentanément"
                    + " illisible : le choix ne peut pas être vérifié. Réessayez dans un instant.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private static <D extends AuditEntityDto> Map<UUID, D> pourAffichage(Supplier<List<D>> lecture, String quoi) {
        try {
            return parId(lecture.get());
        } catch (Exception e) {
            log.warn("{} illisibles dans referentiel-service, affichés sans libellé : {}", quoi, e.getMessage());
            return Map.of();
        }
    }

    private static <D extends AuditEntityDto> Map<UUID, D> parId(List<D> liste) {
        return liste == null ? Map.of()
                : liste.stream().collect(Collectors.toMap(AuditEntityDto::getId, Function.identity(), (a, b) -> a));
    }
}
