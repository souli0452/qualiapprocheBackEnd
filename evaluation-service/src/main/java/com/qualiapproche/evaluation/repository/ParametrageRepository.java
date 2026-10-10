package com.qualiapproche.evaluation.repository;

import com.qualiapproche.common.base.AuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.UUID;

/**
 * Ce que tout paramétrage du module sait faire : se lister pour une direction, entier pour un
 * choix, page par page et filtré pour un écran (voir {@code common.spec.Filtres}).
 */
@NoRepositoryBean
public interface ParametrageRepository<E extends AuditEntity> extends JpaRepository<E, UUID>, JpaSpecificationExecutor<E> {

    List<E> findAllByDirectionIdOrderByCreatedAtAsc(UUID directionId);
}
