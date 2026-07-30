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

    @Query("SELECT MAX(d.documentNumber) FROM DocumentQms d WHERE d.documentNumber LIKE :prefix%")
    String findMaxDocumentNumberByPrefix(@Param("prefix") String prefix);
}
