package com.qualiapproche.support.repository;

import com.qualiapproche.support.model.DocumentQms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentQmsRepository extends JpaRepository<DocumentQms, UUID>, JpaSpecificationExecutor<DocumentQms> {

    Optional<DocumentQms> findByDocumentNumber(String documentNumber);

    List<DocumentQms> findByEsTraiterTrueAndObsoleteFalseAndArchivedFalse();

    /** Le plus grand rang émis sous ce préfixe, comparé en nombre (…-1000 après …-999). */
    @Query(value = "SELECT MAX(CAST(substring(document_number FROM '([0-9]+)$') AS bigint)) FROM qms_documents "
            + "WHERE document_number LIKE :prefix || '%' "
            + "AND substring(document_number FROM char_length(:prefix) + 1) ~ '^[0-9]+$'", nativeQuery = true)
    Long dernierRangSousPrefixe(@Param("prefix") String prefix);
}
