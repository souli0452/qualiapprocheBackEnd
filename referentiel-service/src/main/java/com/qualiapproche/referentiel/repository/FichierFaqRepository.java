package com.qualiapproche.referentiel.repository;

import com.qualiapproche.referentiel.entities.FichierFaq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FichierFaqRepository extends JpaRepository<FichierFaq, UUID> {

    List<FichierFaq> findAllByFaqIdOrderByCreatedAtAsc(UUID faqId);

    List<FichierFaq> findAllByFaqIdIn(List<UUID> faqIds);
}
