package com.qualiapproche.repository;

import com.qualiapproche.entities.ConfigGlobal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConfigGlobalRepository extends JpaRepository<ConfigGlobal, UUID> {
}
