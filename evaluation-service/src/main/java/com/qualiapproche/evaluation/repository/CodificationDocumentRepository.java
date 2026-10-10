package com.qualiapproche.evaluation.repository;

import com.qualiapproche.evaluation.entities.CodificationDocument;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CodificationDocumentRepository extends ParametrageRepository<CodificationDocument> {

    /**
     * La règle, verrouillée jusqu'à la fin de la transaction.
     *
     * <p>Deux audits créés au même instant liraient sinon le même compteur, et porteraient la même
     * référence — que la contrainte d'unicité refuserait alors, en 500, au second.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CodificationDocument> findByDirectionIdAndTypeDocument(UUID directionId, String typeDocument);
}
