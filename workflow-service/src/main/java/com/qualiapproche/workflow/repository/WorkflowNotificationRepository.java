package com.qualiapproche.workflow.repository;

import com.qualiapproche.workflow.model.WorkflowNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowNotificationRepository extends JpaRepository<WorkflowNotification, UUID> {

    List<WorkflowNotification> findByStatutAndProchaineTentativeAtLessThanEqualOrderByCreatedAtAsc(
            WorkflowNotification.NotificationStatut statut, LocalDateTime echeance, Pageable pageable);

    long countByStatut(WorkflowNotification.NotificationStatut statut);
}
