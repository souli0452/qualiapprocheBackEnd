package com.qualiapproche.support.repository;

import com.qualiapproche.support.model.PieceJointeEtape;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PieceJointeEtapeRepository extends JpaRepository<PieceJointeEtape, UUID> {

    /** La référence est l'identité de la pièce : c'est par elle que le circuit la désigne. */
    Optional<PieceJointeEtape> findByReference(String reference);

    /** Pièces d'un dossier, pour les lister sans parcourir le serveur de fichiers. */
    List<PieceJointeEtape> findByFamilleAndDossierIdOrderByCreatedAtAsc(String famille, UUID dossierId);
}
