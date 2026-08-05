package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.PrioriteDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PrioriteDocumentRepository extends JpaRepository<PrioriteDocument, UUID> {

    /**
     * Entrées dont le libellé ou la description contient le terme, casse indifférente.
     *
     * <p>La recherche porte sur ce que l'utilisateur voit : le libellé qu'affiche la liste
     * déroulante, et la description qui le précise. Chercher sur des champs invisibles
     * rendrait des résultats sans rapport apparent avec la saisie.</p>
     */
    Page<PrioriteDocument> findByLibelleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String libelle, String description, Pageable pageable);
}
