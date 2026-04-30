package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.ConfigGlobal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConfigGlobalRepository extends JpaRepository<ConfigGlobal, UUID> {
}
