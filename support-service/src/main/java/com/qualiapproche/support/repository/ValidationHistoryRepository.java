package com.qualiapproche.support.repository;

import com.qualiapproche.support.model.ValidationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ValidationHistoryRepository extends JpaRepository<ValidationHistory, Long> {
    List<ValidationHistory> findByValidationInstanceIdOrderByDecisionDateDesc(UUID validationInstanceId);
}
