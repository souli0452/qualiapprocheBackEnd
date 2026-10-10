package com.qualiapproche.userservice.repository;

import com.qualiapproche.userservice.entities.JetonDeCompte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface JetonDeCompteRepository extends JpaRepository<JetonDeCompte, UUID> {

    Optional<JetonDeCompte> findByEmpreinte(String empreinte);

    /** Un nouveau lien annule les précédents du même usage : seul le dernier courriel vaut. */
    @Modifying
    @Query("UPDATE JetonDeCompte j SET j.utiliseLe = :maintenant "
            + "WHERE j.userId = :userId AND j.usage = :usage AND j.utiliseLe IS NULL")
    int annulerLesPrecedents(@Param("userId") String userId, @Param("usage") JetonDeCompte.Usage usage,
                             @Param("maintenant") LocalDateTime maintenant);

    /**
     * Marque le jeton servi, <b>s'il ne l'est pas déjà</b> et n'a pas expiré, en une seule
     * instruction.
     *
     * <p>Lire le jeton, constater qu'il est libre, puis l'enregistrer comme servi laissait une
     * fenêtre : deux requêtes simultanées le lisaient libre toutes deux, et toutes deux fixaient un
     * mot de passe. Ici, c'est la base qui tranche — sur une même ligne, la seconde mise à jour
     * attend la première, relit la condition, et ne touche plus rien.</p>
     *
     * @return 1 si le jeton vient d'être consommé par cet appel, 0 si un autre l'a devancé
     */
    @Modifying
    @Query("UPDATE JetonDeCompte j SET j.utiliseLe = :maintenant "
            + "WHERE j.id = :id AND j.utiliseLe IS NULL AND j.expireLe > :maintenant")
    int marquerServi(@Param("id") UUID id, @Param("maintenant") LocalDateTime maintenant);
}
