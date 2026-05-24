package com.qualiapproche.support.repository;

import com.qualiapproche.support.model.QmsAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QmsAuditLogRepository extends JpaRepository<QmsAuditLog, Long> {
    List<QmsAuditLog> findByDocumentNumberOrderByTimestampDesc(String documentNumber);
}
