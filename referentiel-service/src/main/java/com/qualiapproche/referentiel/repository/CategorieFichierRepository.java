package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.CategorieFichier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CategorieFichierRepository extends JpaRepository<CategorieFichier, UUID> {
/*    List<CategorieFichier> findAllByFichiersId(UUID fichierId);*/

}
