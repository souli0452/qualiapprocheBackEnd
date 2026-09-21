package com.qualiapproche.referentiel.service.impl;

import com.qualiapproche.common.dto.FaqDto;
import com.qualiapproche.common.dto.FichierFaqDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.referentiel.entities.Faq;
import com.qualiapproche.referentiel.repository.FaqRepository;
import com.qualiapproche.referentiel.service.FaqService;
import com.qualiapproche.referentiel.service.FichierFaqService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * La foire aux questions, bornée à la direction de l'appelant.
 *
 * <p>Une organisation n'a pas à lire les réponses d'une autre : ce sont ses procédures, parfois
 * la reformulation d'un document qualité. Le cloisonnement se vérifie ici plutôt que de se
 * déduire d'un filtre de requête qu'une évolution pourrait retirer — et il vaut d'autant plus
 * que l'assistant IA recopie ces textes dans sa consigne.</p>
 */
@Service
@RequiredArgsConstructor
public class FaqServiceImpl implements FaqService {

    private static final Sort ORDRE = Sort.by(Sort.Order.asc("rang"), Sort.Order.asc("createdAt"));

    private final FaqRepository repository;
    private final FichierFaqService fichiers;

    @Override
    @Transactional
    public FaqDto create(FaqDto dto) {
        exigerUneDirection();
        return versDto(repository.save(Faq.builder()
                .question(dto.getQuestion().strip())
                .reponse(dto.getReponse().strip())
                .categorie(nettoyer(dto.getCategorie()))
                .publiee(dto.isPubliee())
                .rang(dto.getRang())
                .build()));
    }

    @Override
    @Transactional
    public FaqDto update(FaqDto dto) {
        Faq entree = sienneOuRien(dto.getId());
        entree.setQuestion(dto.getQuestion().strip());
        entree.setReponse(dto.getReponse().strip());
        entree.setCategorie(nettoyer(dto.getCategorie()));
        entree.setPubliee(dto.isPubliee());
        entree.setRang(dto.getRang());
        return versDto(repository.save(entree));
    }

    @Override
    @Transactional(readOnly = true)
    public FaqDto getById(UUID id) {
        FaqDto dto = versDto(sienneOuRien(id));
        dto.setFichiers(fichiers.desEntrees(List.of(id)));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FaqDto> getAll(String recherche, Pageable pageable) {
        UUID directionId = SecurityUtils.getCurrentDirectionId();
        if (directionId == null) {
            return Page.empty(pageable);
        }
        Pageable range = pageable.getSort().isSorted() ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ORDRE);
        String terme = recherche == null || recherche.isBlank() ? null : recherche.strip();
        return repository.rechercher(directionId, terme, range).map(this::versDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FaqDto> getPubliees() {
        UUID directionId = SecurityUtils.getCurrentDirectionId();
        if (directionId == null) {
            return List.of();
        }
        List<FaqDto> publiees =
                repository.findAllByDirectionIdAndPublieeTrueOrderByRangAscCreatedAtAsc(directionId)
                        .stream().map(this::versDto).toList();
        return avecLeursPieces(publiees);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        repository.delete(sienneOuRien(id));
    }

    /**
     * L'entrée, si elle appartient à la direction de l'appelant.
     *
     * <p>Celle d'une autre direction est déclarée introuvable, et non refusée : répondre
     * « elle existe mais n'est pas à vous » dirait déjà quelque chose de cette organisation.</p>
     */
    private Faq sienneOuRien(UUID id) {
        UUID directionId = exigerUneDirection();
        return repository.findById(id)
                .filter(entree -> directionId.equals(entree.getDirectionId()))
                .orElseThrow(() -> new BusinessException(
                        "Aucune entrée de FAQ ne porte cet identifiant : " + id, HttpStatus.NOT_FOUND));
    }

    private UUID exigerUneDirection() {
        UUID directionId = SecurityUtils.getCurrentDirectionId();
        if (directionId == null) {
            throw new BusinessException(
                    "Votre compte n'est rattaché à aucune direction : la FAQ ne peut pas être"
                            + " administrée depuis ce compte.", HttpStatus.FORBIDDEN);
        }
        return directionId;
    }

    /**
     * Joint à chaque entrée ses pièces, en une seule requête.
     *
     * <p>Une requête par entrée aurait multiplié les allers-retours autant que de lignes — et
     * cette liste est demandée à chaque tour de conversation de l'assistant.</p>
     */
    private List<FaqDto> avecLeursPieces(List<FaqDto> entrees) {
        if (entrees.isEmpty()) {
            return entrees;
        }
        Map<UUID, List<FichierFaqDto>> parEntree =
                fichiers.desEntrees(entrees.stream().map(FaqDto::getId).toList()).stream()
                        .collect(Collectors.groupingBy(FichierFaqDto::getFaqId));
        entrees.forEach(entree -> entree.setFichiers(parEntree.get(entree.getId())));
        return entrees;
    }

    private String nettoyer(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.strip();
    }

    private FaqDto versDto(Faq entree) {
        return FaqDto.builder()
                .id(entree.getId())
                .question(entree.getQuestion())
                .reponse(entree.getReponse())
                .categorie(entree.getCategorie())
                .publiee(entree.isPubliee())
                .rang(entree.getRang())
                .updateAt(entree.getUpdateAt())
                .build();
    }
}
