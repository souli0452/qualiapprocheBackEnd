package com.qualiapproche.repository;

import com.qualiapproche.entities.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {
}
