package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.LicenceInstallee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LicenceInstalleeRepository extends JpaRepository<LicenceInstallee, UUID> {

    /** La licence qui s'applique : la dernière installée. */
    Optional<LicenceInstallee> findTopByOrderByInstalleeLeDesc();

    List<LicenceInstallee> findAllByOrderByInstalleeLeDesc();

    boolean existsByReference(String reference);
}
