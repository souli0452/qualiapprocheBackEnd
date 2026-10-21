package com.qualiapproche.evaluation.repository;

import com.qualiapproche.evaluation.entities.PreuveNotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PreuveNotationRepository extends JpaRepository<PreuveNotation, UUID> {

    List<PreuveNotation> findAllByNotationIdOrderByCreatedAtAsc(UUID notationId);
}
