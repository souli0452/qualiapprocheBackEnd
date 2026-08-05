package com.qualiapproche.support.repository;

import com.qualiapproche.support.model.QmsDocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QmsDocumentTypeRepository extends JpaRepository<QmsDocumentType, UUID> {
    Optional<QmsDocumentType> findByCode(String code);

    /** Types dont le code ou le libellé contient le terme, casse indifférente. */
    Page<QmsDocumentType> findByCodeContainingIgnoreCaseOrLibelleContainingIgnoreCase(
            String code, String libelle, Pageable pageable);
}
