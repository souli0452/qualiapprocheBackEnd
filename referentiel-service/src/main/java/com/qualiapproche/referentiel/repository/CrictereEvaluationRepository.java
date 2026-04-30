package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.CrictereEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CrictereEvaluationRepository extends JpaRepository<CrictereEvaluation, UUID> {
}
