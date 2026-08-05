package com.qualiapproche.amelioration.repository;

import com.qualiapproche.amelioration.entities.PieceJointe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PieceJointeRepository extends JpaRepository<PieceJointe, UUID> {

    /**
     * Pièces jointes composées par l'utilisateur sur la fiche du dossier.
     *
     * <p>Celles déposées au fil du circuit en sont exclues : elles ne figurent pas dans ce que le
     * client renvoie à l'enregistrement, et les aligner sur cette liste les supprimerait. Voir
     * {@code PieceJointe#deposeParCircuit}.</p>
     */
    List<PieceJointe> findAllByEntityIdAndDeposeParCircuitFalse(UUID entityId);

    /**
     * Pièce jointe désignée par sa référence d'objet.
     *
     * <p>C'est par elle que le moteur de workflow renvoie un fichier déposé : il ne transporte que
     * des chaînes, la référence est donc la seule chose que la valeur d'un champ {@code FILE}
     * puisse contenir. La pièce jointe, elle, garde le nom d'origine et le type.</p>
     */
    Optional<PieceJointe> findByUrlAndEntityId(String url, UUID entityId);
}
