package com.qualiapproche.amelioration.repository;

import com.qualiapproche.amelioration.entities.Efficacite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EfficaciteRepository extends JpaRepository<Efficacite, UUID> {

}
