package com.qualiapproche.support.repository;

import com.qualiapproche.support.model.QmsDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QmsDocumentTypeRepository extends JpaRepository<QmsDocumentType, UUID> {
    Optional<QmsDocumentType> findByCode(String code);
}
