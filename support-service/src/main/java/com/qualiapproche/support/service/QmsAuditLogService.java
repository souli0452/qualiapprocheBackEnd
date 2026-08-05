package com.qualiapproche.support.service;

import com.qualiapproche.support.model.QmsAuditLog;
import com.qualiapproche.support.repository.QmsAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QmsAuditLogService {

    private final QmsAuditLogRepository auditLogRepository;

    public void logAction(String action, String documentNumber, String details) {
        String username = com.qualiapproche.common.utils.SecurityUtils.getCurrentUserFullName();
        if ("Système".equalsIgnoreCase(username)) {
            username = "system";
        }

        log.info("Auditing action '{}' on document '{}' by user '{}'", action, documentNumber, username);

        QmsAuditLog auditLog = QmsAuditLog.builder()
                .username(username)
                .action(action)
                .documentNumber(documentNumber)
                .timestamp(LocalDateTime.now())
                .details(details)
                .build();

        auditLogRepository.save(auditLog);
    }

    public List<QmsAuditLog> getLogsForDocument(String documentNumber) {
        return auditLogRepository.findByDocumentNumberOrderByTimestampDesc(documentNumber);
    }
}
