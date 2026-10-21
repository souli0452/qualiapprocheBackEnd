package com.qualiapproche.evaluation.repository;

import com.qualiapproche.evaluation.entities.PreuveConstat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PreuveConstatRepository extends JpaRepository<PreuveConstat, UUID> {

    List<PreuveConstat> findAllByConstatIdOrderByCreatedAtAsc(UUID constatId);

    long countByConstatIdIn(java.util.Collection<UUID> constatIds);
}
