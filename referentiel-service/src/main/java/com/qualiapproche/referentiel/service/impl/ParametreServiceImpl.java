package com.qualiapproche.referentiel.service.impl;

import com.qualiapproche.common.dto.ParametreDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.referentiel.entities.Parametre;
import com.qualiapproche.referentiel.entities.TypeParametre;
import com.qualiapproche.referentiel.repository.ParametreRepository;
import com.qualiapproche.referentiel.service.ParametreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Réglages de l'organisation.
 *
 * <p>Deux règles portent tout le reste : la clé est <b>normalisée</b> à la création, et elle ne
 * change <b>jamais</b> ensuite.</p>
 */
@Service
@RequiredArgsConstructor
public class ParametreServiceImpl implements ParametreService {

    private final ParametreRepository repository;

    @Override
    @Transactional
    public ParametreDto create(ParametreDto dto) {
        String cle = normaliser(dto.getCle());
        if (repository.existsByCle(cle)) {
            throw new BusinessException(
                    "Un réglage porte déjà la clé « " + cle + " ». Modifiez sa valeur plutôt que "
                            + "d'en créer un second : le code ne saurait pas lequel lire.",
                    HttpStatus.CONFLICT);
        }
        exigerUnLibelle(dto.getLibelle());
        TypeParametre type = typeDe(dto.getType());
        verifierLaValeur(type, dto.getValeur());

        Parametre parametre = Parametre.builder()
                .cle(cle)
                .valeur(dto.getValeur())
                .libelle(dto.getLibelle().trim())
                .description(dto.getDescription())
                .type(type)
                .lisibleSansHabilitation(dto.isLisibleSansHabilitation())
                .build();
        return versDto(repository.save(parametre));
    }

    /**
     * Met à jour ce qui peut l'être. La clé n'en fait pas partie.
     *
     * <p>Le refus est explicite plutôt que silencieux : ignorer la clé soumise laisserait croire à
     * l'utilisateur qu'il l'a renommée, et il chercherait longtemps pourquoi le pied de page de ses
     * courriels ne change pas.</p>
     */
    @Override
    @Transactional
    public ParametreDto update(UUID id, ParametreDto dto) {
        Parametre existant = repository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Aucun réglage ne porte cet identifiant : " + id, HttpStatus.NOT_FOUND));

        if (dto.getCle() != null && !dto.getCle().isBlank()
                && !normaliser(dto.getCle()).equals(existant.getCle())) {
            throw new BusinessException(
                    "La clé « " + existant.getCle() + " » ne peut pas être renommée : c'est par elle "
                            + "que l'application désigne ce réglage. Supprimez-le et créez-en un autre "
                            + "si le nom ne convient pas.",
                    HttpStatus.CONFLICT);
        }

        exigerUnLibelle(dto.getLibelle());
        TypeParametre type = dto.getType() != null && !dto.getType().isBlank()
                ? typeDe(dto.getType())
                : existant.getType();
        verifierLaValeur(type, dto.getValeur());

        existant.setValeur(dto.getValeur());
        existant.setLibelle(dto.getLibelle().trim());
        existant.setDescription(dto.getDescription());
        existant.setType(type);
        existant.setLisibleSansHabilitation(dto.isLisibleSansHabilitation());
        return versDto(repository.save(existant));
    }

    @Override
    @Transactional(readOnly = true)
    public ParametreDto getById(UUID id) {
        return repository.findById(id).map(this::versDto)
                .orElseThrow(() -> new BusinessException(
                        "Aucun réglage ne porte cet identifiant : " + id, HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public ParametreDto getByCle(String cle) {
        return repository.findByCle(normaliser(cle)).map(this::versDto)
                .orElseThrow(() -> new BusinessException(
                        "Aucun réglage ne porte la clé « " + cle + " ».", HttpStatus.NOT_FOUND));
    }

    /** Rangés par clé : l'ordre d'insertion n'a aucun sens pour qui cherche un réglage dans une liste. */
    @Override
    @Transactional(readOnly = true)
    public List<ParametreDto> getAll(String recherche) {
        List<Parametre> parametres = recherche == null || recherche.isBlank()
                ? repository.findAll()
                : repository.findByCleContainingIgnoreCaseOrLibelleContainingIgnoreCase(
                        recherche.trim(), recherche.trim());
        return parametres.stream()
                .sorted(Comparator.comparing(Parametre::getCle))
                .map(this::versDto)
                .toList();
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new BusinessException(
                    "Aucun réglage ne porte cet identifiant : " + id, HttpStatus.NOT_FOUND);
        }
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> valeursPubliques() {
        Map<String, String> valeurs = new LinkedHashMap<>();
        repository.findByLisibleSansHabilitationTrue().stream()
                .sorted(Comparator.comparing(Parametre::getCle))
                // Un réglage sans valeur n'a rien à dire : le transmettre vide obligerait chaque
                // consommateur à refaire ce tri, et un pied de page afficherait « Téléphone : ».
                .filter(parametre -> parametre.getValeur() != null && !parametre.getValeur().isBlank())
                .forEach(parametre -> valeurs.put(parametre.getCle(), parametre.getValeur().trim()));
        return valeurs;
    }

    /**
     * Clé sous sa forme canonique : majuscules, accents retirés, séparateurs réduits au souligné.
     *
     * <p>Sans cela, « contact email », « Contact_Email » et « CONTACT_EMAIL » désigneraient trois
     * réglages, dont un seul serait lu par le code.</p>
     */
    private String normaliser(String cle) {
        if (cle == null || cle.isBlank()) {
            throw new BusinessException(
                    "La clé du réglage est obligatoire : c'est par elle que l'application le désigne.",
                    HttpStatus.BAD_REQUEST);
        }
        String normalisee = java.text.Normalizer.normalize(cle.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_|_$", "");
        if (normalisee.isEmpty()) {
            throw new BusinessException(
                    "« " + cle + " » ne donne aucune clé exploitable.", HttpStatus.BAD_REQUEST);
        }
        return normalisee.length() > 80 ? normalisee.substring(0, 80) : normalisee;
    }

    private TypeParametre typeDe(String type) {
        if (type == null || type.isBlank()) {
            return TypeParametre.TEXTE;
        }
        try {
            return TypeParametre.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    "« " + type + " » n'est pas une nature de réglage. Valeurs admises : "
                            + java.util.Arrays.toString(TypeParametre.values()) + ".",
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Refuse une valeur que sa nature ne permet pas.
     *
     * <p>Seuls les nombres sont vérifiés : un délai de rappel saisi « deux jours » ne se verrait
     * qu'au moment de l'échéance, dans un fil de fond, sous la forme d'une conversion qui échoue.
     * Autant le dire à qui le saisit. Une valeur vide reste admise — un réglage non renseigné est
     * simplement ignoré par ce qui le lit.</p>
     */
    private void verifierLaValeur(TypeParametre type, String valeur) {
        if (type != TypeParametre.NOMBRE || valeur == null || valeur.isBlank()) {
            return;
        }
        try {
            Long.parseLong(valeur.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(
                    "« " + valeur + " » n'est pas un nombre entier. Ce réglage est lu comme un nombre "
                            + "par l'application.", HttpStatus.BAD_REQUEST);
        }
    }

    private void exigerUnLibelle(String libelle) {
        if (libelle == null || libelle.isBlank()) {
            throw new BusinessException(
                    "L'intitulé du réglage est obligatoire : la clé est technique, c'est lui que lit "
                            + "l'administrateur.", HttpStatus.BAD_REQUEST);
        }
    }

    private ParametreDto versDto(Parametre parametre) {
        return ParametreDto.builder()
                .id(parametre.getId())
                .cle(parametre.getCle())
                .valeur(parametre.getValeur())
                .libelle(parametre.getLibelle())
                .description(parametre.getDescription())
                .type(parametre.getType() != null ? parametre.getType().name() : null)
                .lisibleSansHabilitation(parametre.isLisibleSansHabilitation())
                .build();
    }
}
