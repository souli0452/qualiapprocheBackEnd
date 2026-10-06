package com.qualiapproche.evaluation.repository;

import com.qualiapproche.common.base.AuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.UUID;

/** Ce que tout référentiel du module sait faire : se lister pour une direction. */
@NoRepositoryBean
public interface ReferentielRepository<E extends AuditEntity> extends JpaRepository<E, UUID> {

    List<E> findAllByDirectionIdOrderByCreatedAtAsc(UUID directionId);
}
