package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.Parametre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParametreRepository extends JpaRepository<Parametre, UUID> {

    Optional<Parametre> findByCle(String cle);

    boolean existsByCle(String cle);

    /** Les réglages qu'un service peut lire sans habilitation — voir le pied de page des courriels. */
    List<Parametre> findByLisibleSansHabilitationTrue();

    List<Parametre> findByCleContainingIgnoreCaseOrLibelleContainingIgnoreCase(String cle, String libelle);
}
