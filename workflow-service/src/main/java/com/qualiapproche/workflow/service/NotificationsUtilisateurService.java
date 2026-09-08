package com.qualiapproche.workflow.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qualiapproche.common.dto.DepotNotificationDto;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.workflow.model.NotificationUtilisateur;
import com.qualiapproche.workflow.repository.NotificationUtilisateurRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * La boîte de réception : y déposer, la lire, en accuser réception.
 *
 * <p>Un seul dépôt pour toutes les origines — un franchissement de circuit, une échéance dépassée,
 * l'ouverture d'un dossier. Chacune fournit sa clé d'unicité et décide si un nouveau dépôt doit
 * <b>réveiller</b> la ligne ; le reste est identique, et c'est ce qui permet à un module d'annoncer
 * quelque chose sans rien connaître du circuit.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationsUtilisateurService {

    private final NotificationUtilisateurRepository repository;

    /**
     * Dépose une ligne, ou met à jour celle qui porte déjà la même clé.
     *
     * <p>Le message est réécrit à chaque dépôt : « 3 plans en retard » devient « 5 plans en retard »
     * sans qu'une seconde ligne apparaisse. L'état de lecture, lui, ne se perd que si le producteur
     * le demande.</p>
     *
     * <p>Un dépôt sans destinataire est ignoré sans bruit : c'est le cas d'une étape dont le rôle
     * n'a aucun porteur joignable, déjà signalé là où les destinataires sont résolus.</p>
     */
    @Transactional
    public void deposer(DepotNotificationDto depot) {
        if (depot.destinataireId() == null || depot.destinataireId().isBlank()) {
            return;
        }

        repository.findByDestinataireIdAndCleUnicite(depot.destinataireId(), depot.cleUnicite())
                .ifPresentOrElse(existante -> {
                    existante.setTitre(depot.titre());
                    existante.setMessage(depot.message());
                    existante.setGravite(depot.gravite());
                    existante.setLien(depot.lien());
                    if (depot.reveiller() && existante.isLue()) {
                        existante.setLue(false);
                        existante.setLueLe(null);
                    }
                    repository.save(existante);
                }, () -> repository.save(NotificationUtilisateur.builder()
                        .destinataireId(depot.destinataireId())
                        .cleUnicite(depot.cleUnicite())
                        .code(depot.code())
                        .source(depot.source())
                        .titre(depot.titre())
                        .message(depot.message())
                        .resourceId(depot.resourceId())
                        .resourceType(depot.resourceType())
                        .lien(depot.lien())
                        .gravite(depot.gravite())
                        .build()));
    }

    /**
     * La boîte de l'appelant.
     *
     * <p>Bornée à lui, et non au périmètre demandé : une notification nomme des personnes et
     * rapporte ce qu'on attend d'elles. Aucun paramètre ne permet de désigner quelqu'un d'autre —
     * c'est le seul moyen d'être sûr qu'aucun appel ne le fera.</p>
     */
    public Page<NotificationUtilisateur> mesNotifications(boolean seulementNonLues, Pageable pageable) {
        String moi = SecurityUtils.getCurrentUserId();
        if (moi == null) {
            return Page.empty(pageable);
        }
        return seulementNonLues
                ? repository.findByDestinataireIdAndLueFalseOrderByCreatedAtDesc(moi, pageable)
                : repository.findByDestinataireIdOrderByCreatedAtDesc(moi, pageable);
    }

    public long nombreDeNonLues() {
        String moi = SecurityUtils.getCurrentUserId();
        return moi == null ? 0L : repository.countByDestinataireIdAndLueFalse(moi);
    }

    /**
     * Accuse réception d'une ligne.
     *
     * <p>Silencieux sur une ligne qui n'est pas la sienne, plutôt qu'un refus : répondre « accès
     * interdit » confirmerait à qui essaie qu'une notification porte bien cet identifiant, et il
     * n'y a rien à protéger de plus qu'en n'en disant rien.</p>
     *
     * @return vrai si la ligne appartenait bien à l'appelant et a été marquée
     */
    @Transactional
    public boolean marquerLue(UUID id) {
        String moi = SecurityUtils.getCurrentUserId();
        if (moi == null) {
            return false;
        }
        return repository.findById(id)
                .filter(n -> moi.equals(n.getDestinataireId()))
                .map(n -> {
                    n.marquerLue();
                    repository.save(n);
                    return true;
                })
                .orElse(false);
    }

    /** @return le nombre de lignes que ce geste a marquées */
    @Transactional
    public int toutMarquerLu() {
        String moi = SecurityUtils.getCurrentUserId();
        return moi == null ? 0 : repository.marquerToutesLues(moi);
    }
}
