package com.qualiapproche.referentiel.repository;

import com.qualiapproche.common.base.AuditEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.UUID;

/** Un paramétrage propre à chaque direction, et qui se retire au lieu de s'effacer. */
@NoRepositoryBean
public interface ParametrageParDirectionRepository<E extends AuditEntity> extends JpaRepository<E, UUID>, JpaSpecificationExecutor<E> {

    List<E> findAllByDirectionId(UUID directionId, Sort ordre);

    List<E> findAllByDirectionIdAndActifTrue(UUID directionId, Sort ordre);
}
