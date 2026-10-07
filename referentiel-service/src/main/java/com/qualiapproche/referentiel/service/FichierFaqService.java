package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.FichierFaqDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/** Les pièces jointes facultatives d'une entrée de FAQ. */
public interface FichierFaqService {

    List<FichierFaqDto> deposer(UUID faqId, List<MultipartFile> fichiers);

    List<FichierFaqDto> desEntrees(List<UUID> faqIds);

    /** Le contenu, pour téléchargement. L'appartenance de l'entrée est vérifiée d'abord. */
    InputStream contenu(UUID fichierId);

    FichierFaqDto decrire(UUID fichierId);

    void supprimer(UUID fichierId);
}
