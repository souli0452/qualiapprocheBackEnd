package com.qualiapproche.repository;

import com.qualiapproche.entities.Efficacite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EfficaciteRepository extends JpaRepository<Efficacite, UUID> {

}
