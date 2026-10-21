package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.utils.SecurityUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Le périmètre de l'appelant : sa direction, et rien au-delà.
 *
 * <p>Tout le module est borné à l'organisation de l'appelant. Le cloisonnement se vérifie ici,
 * sur chaque lecture par identifiant, plutôt que de se déduire d'un filtre de requête qu'une
 * évolution pourrait retirer.</p>
 */
public final class Perimetre {

    private Perimetre() {
    }

    public static UUID exigerUneDirection() {
        UUID directionId = SecurityUtils.getCurrentDirectionId();
        if (directionId == null) {
            throw new BusinessException("Votre compte n'est rattaché à aucune direction : les audits"
                    + " ne peuvent pas être gérés depuis ce compte.", HttpStatus.FORBIDDEN);
        }
        return directionId;
    }

    /**
     * L'entité, si elle appartient à la direction de l'appelant.
     *
     * <p>Celle d'une autre direction est déclarée introuvable, et non refusée : répondre « elle
     * existe mais n'est pas à vous » dirait déjà quelque chose de cette organisation.</p>
     */
    public static <E extends AuditEntity> E sienneOuRien(JpaRepository<E, UUID> depot, UUID id, String quoi) {
        UUID directionId = exigerUneDirection();
        if (id == null) {
            throw new BusinessException("Identifiant manquant : " + quoi + ".", HttpStatus.BAD_REQUEST);
        }
        return depot.findById(id)
                .filter(e -> directionId.equals(e.getDirectionId()))
                .orElseThrow(() -> new BusinessException(
                        "Aucun(e) " + quoi + " ne porte cet identifiant : " + id, HttpStatus.NOT_FOUND));
    }

    /** Comme {@link #sienneOuRien}, mais un identifiant absent rend {@code null} au lieu d'échouer. */
    public static <E extends AuditEntity> E sienneOuNul(JpaRepository<E, UUID> depot, UUID id, String quoi) {
        return id == null ? null : sienneOuRien(depot, id, quoi);
    }
}
