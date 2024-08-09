package com.qualiapproche.repository;

import com.qualiapproche.entities.CrictereEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CrictereEvaluationRepository extends JpaRepository<CrictereEvaluation, UUID> {
}
