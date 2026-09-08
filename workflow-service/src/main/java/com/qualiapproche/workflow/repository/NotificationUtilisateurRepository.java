package com.qualiapproche.workflow.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.qualiapproche.workflow.model.NotificationUtilisateur;

@Repository
public interface NotificationUtilisateurRepository extends JpaRepository<NotificationUtilisateur, UUID> {

    /** La boîte d'une personne, les plus récentes d'abord. */
    Page<NotificationUtilisateur> findByDestinataireIdOrderByCreatedAtDesc(String destinataireId,
                                                                          Pageable pageable);

    /** Ses seules non-lues, pour l'écran qui ne montre que ce qui reste à voir. */
    Page<NotificationUtilisateur> findByDestinataireIdAndLueFalseOrderByCreatedAtDesc(String destinataireId,
                                                                                     Pageable pageable);

    long countByDestinataireIdAndLueFalse(String destinataireId);

    /** La ligne déjà déposée sur cette clé, s'il y en a une : c'est elle qu'un second dépôt met à jour. */
    Optional<NotificationUtilisateur> findByDestinataireIdAndCleUnicite(String destinataireId, String cleUnicite);

    List<NotificationUtilisateur> findByResourceIdOrderByCreatedAtDesc(String resourceId);

    /**
     * Retire les lignes qui désignent un dossier supprimé, chez tous ses destinataires.
     *
     * @return le nombre de lignes retirées
     */
    int deleteByResourceId(String resourceId);

    /**
     * Marque lues toutes les non-lues d'une personne, en une requête.
     *
     * <p>Les charger pour les modifier une à une aurait coûté autant de requêtes que de lignes, sur
     * le geste précisément fait quand la boîte en contient beaucoup.</p>
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE NotificationUtilisateur n SET n.lue = true, n.lueLe = CURRENT_TIMESTAMP, "
            + "n.updateAt = CURRENT_TIMESTAMP "
            + "WHERE n.destinataireId = :destinataireId AND n.lue = false")
    int marquerToutesLues(@Param("destinataireId") String destinataireId);
}
