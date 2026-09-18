package com.qualiapproche.ia.repository;

import com.qualiapproche.ia.entities.ConversationIa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConversationIaRepository extends JpaRepository<ConversationIa, UUID> {

    /**
     * Les fils d'une personne, du plus vivant au plus ancien.
     *
     * <p>Bornés à leur auteur, sans exception : une conversation n'est pas un dossier, et
     * l'habilitation transverse qui ouvre les dossiers des autres structures n'a pas à ouvrir ce
     * que quelqu'un a écrit à un assistant.</p>
     */
    Page<ConversationIa> findAllByCreatedByIdOrderByDerniereActiviteAtDesc(String createdById,
                                                                          Pageable pageable);
}
