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
}
