package com.qualiapproche.amelioration.repository;

import com.qualiapproche.amelioration.entities.PieceJointe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PieceJointeRepository extends JpaRepository<PieceJointe, UUID> {

    List<PieceJointe> findAllByEntityId(UUID entityId);

    void deleteAllByEntityId(UUID entityId);
}