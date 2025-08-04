package com.qualiapproche.repository;

import com.qualiapproche.entities.CategorieFichier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategorieFichierRepository extends JpaRepository<CategorieFichier, UUID> {
    List<CategorieFichier> findAllByFichiersId(UUID fichierId);

}
