package com.qualiapproche.repository;
import com.qualiapproche.entities.PieceJointe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface PieceJointeRepository extends JpaRepository<PieceJointe, Long> {
    List<PieceJointe> findAllByEntityId(Long entityId);

//    PieceJointe findTop1ByEntityIdOrderByDesc(Long entityId);
}
