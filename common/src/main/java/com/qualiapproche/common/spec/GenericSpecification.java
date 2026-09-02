package com.qualiapproche.common.spec;

import com.qualiapproche.common.api.CriteriaDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.api.FilterExtra;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Traduit des critères de recherche en clause SQL, pour n'importe quelle entité.
 *
 * <p>Chaque module écrivait sa propre spécification, colonne par colonne : une recherche filtrée y
 * coûtait une signature à rallonge dans le contrôleur, le service et la spécification, et le moindre
 * filtre nouveau une livraison. Ici l'écran nomme la colonne et la comparaison ; rien n'est à
 * prévoir à l'avance.</p>
 *
 * <p><b>Ce que cela n'ouvre pas.</b> Les comparaisons sont un ensemble fermé — {@link
 * com.qualiapproche.common.api.FilterOperator} — et les colonnes sont résolues par le fournisseur de
 * persistance : un nom qui ne désigne aucun attribut de l'entité fait échouer la requête, il ne
 * s'exécute pas. Reste que le client choisit les colonnes qu'il interroge : une entité qui porte
 * des données réservées doit borner la recherche par une clause qui lui est propre — c'est ce que
 * fait la borne de visibilité des non-conformités, combinée <b>en dehors</b> de ces critères pour
 * qu'aucun filtre reçu ne puisse l'élargir.</p>
 *
 * @param <T> entité interrogée
 */
public class GenericSpecification<T> {

    /**
     * Clause correspondant aux critères reçus.
     *
     * @param criteres critères de l'appelant ; {@code null} ou vides, la clause laisse tout passer
     * @param champsRecherchables attributs confrontés au texte libre, ou vide si la ressource n'en
     *                            déclare pas — le texte est alors sans effet
     */
    public Specification<T> query(CriteriaDto criteres, List<String> champsRecherchables) {
        return (root, query, cb) -> {
            List<Predicate> clauses = new ArrayList<>();

            if (criteres != null && criteres.getFilters() != null) {
                for (FilterExtra filtre : criteres.getFilters()) {
                    Predicate clause = clauseDe(root, cb, filtre);
                    if (clause != null) {
                        clauses.add(clause);
                    }
                }
            }

            String texte = criteres == null ? null : criteres.getSearch();
            if (texte != null && !texte.isBlank()
                    && champsRecherchables != null && !champsRecherchables.isEmpty()) {
                // Par préfixe : c'est la seule forme qu'un index sait servir. Un « contient »
                // impose un parcours complet de la table, et la recherche s'effondre à mesure que
                // le référentiel grandit — au moment précis où elle devient utile.
                String prefixe = texte.trim().toLowerCase(Locale.ROOT) + "%";
                clauses.add(cb.or(champsRecherchables.stream()
                        .map(champ -> cb.like(cb.lower(chemin(root, champ).as(String.class)), prefixe))
                        .toArray(Predicate[]::new)));
            }

            return clauses.isEmpty() ? cb.conjunction() : cb.and(clauses.toArray(new Predicate[0]));
        };
    }

    /**
     * Traverse un chemin éventuellement imbriqué : {@code "structure.libelle"} devient
     * {@code root.get("structure").get("libelle")}.
     */
    private static String resoudreAlias(String partie) {
        return switch (partie) {
            case "userImputId" -> "agentImputeId";
            case "userImputFullName" -> "agentImputeNomComplet";
            case "userImputeEmail" -> "agentImputeEmail";
            case "numeroReference" -> "numeroDeReference";
            case "justification" -> "description";
            case "etatTraitement" -> "etatDeTraitement";
            case "sourceNonConformiteId" -> "sourceDeNonConformiteId";
            case "sourceNonConformiteLibelle" -> "sourceDeNonConformiteLibelle";
            case "categorieProcessusId" -> "categorieProcessusId";
            case "categorieProcessusLibelle" -> "categorieProcessusLibelle";
            case "structureSoumissionId" -> "structureDeSoumissionId";
            case "structureSoumissionLibelle" -> "structureDeSoumissionLibelle";
            case "actionDsc" -> "actionImmediate";
            case "pertinancePilote" -> "pertinencePilote";
            case "pertinanceRs" -> "pertinenceRs";
            case "numeroOdre" -> "numeroOrdre";
            case "nonConformeId" -> "nonConformiteId";
            case "causeIdentifiees" -> "causeIdentifiee";
            case "solutionRetenues" -> "solutionRetenue";
            default -> partie;
        };
    }

    /**
     * Traverse un chemin éventuellement imbriqué : {@code "structure.libelle"} devient
     * {@code root.get("structure").get("libelle")}.
     */
    private Path<?> chemin(Root<T> root, String champ) {
        Path<?> chemin = root;
        for (String partie : champ.split("\\.")) {
            String resolu = resoudreAlias(partie);
            try {
                chemin = chemin.get(resolu);
            } catch (RuntimeException e) {
                try {
                    chemin = chemin.get(partie);
                } catch (RuntimeException ignored) {
                    throw new BusinessException(
                            "Le critère « " + champ + " » ne désigne aucune donnée de cette ressource.",
                            HttpStatus.BAD_REQUEST);
                }
            }
        }
        return chemin;
    }

    /**
     * Clause d'un critère, sur une colonne ou sur plusieurs.
     *
     * <p>Plusieurs attributs nommés : la même comparaison vaut sur l'un <b>ou</b> l'autre. C'est ce
     * qui permet d'exprimer un périmètre — « les dossiers que j'ai déclarés ou qui me sont
     * imputés » — que des critères cumulés, combinés par un ET, ne savent pas dire.</p>
     */
    private Predicate clauseDe(Root<T> root, CriteriaBuilder cb, FilterExtra filtre) {
        if (filtre == null || filtre.getOperator() == null) {
            return null;
        }

        List<String> plusieurs = filtre.getFields();
        if (plusieurs != null && !plusieurs.isEmpty()) {
            List<Predicate> ou = new ArrayList<>();
            for (String champ : plusieurs) {
                Predicate clause = surUnChamp(root, cb, filtre, champ);
                if (clause != null) {
                    ou.add(clause);
                }
            }
            return ou.isEmpty() ? null : cb.or(ou.toArray(new Predicate[0]));
        }

        return surUnChamp(root, cb, filtre, filtre.getField());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate surUnChamp(Root<T> root, CriteriaBuilder cb, FilterExtra filtre, String champ) {
        if (champ == null || champ.isBlank()) {
            // Critère incomplet : ignoré plutôt que refusé. Un écran qui envoie une ligne vide ne
            // doit pas faire échouer la recherche entière — mais il ne doit pas non plus la voir
            // s'élargir, d'où l'omission plutôt qu'une clause toujours vraie.
            return null;
        }

        Path<?> chemin = chemin(root, champ);
        Object brute = filtre.getValue();

        return switch (filtre.getOperator()) {
            case IS_NULL -> cb.isNull(chemin);
            case NOT_NULL -> cb.isNotNull(chemin);
            case EQ -> brute == null ? cb.isNull(chemin)
                    : cb.equal(chemin, convertir(chemin.getJavaType(), brute));
            case NOT_EQ -> brute == null ? cb.isNotNull(chemin)
                    : cb.notEqual(chemin, convertir(chemin.getJavaType(), brute));
            case LIKE -> texte(cb, chemin, brute, "", "%");
            case CONTAINS -> texte(cb, chemin, brute, "%", "%");
            case ENDS_WITH -> texte(cb, chemin, brute, "%", "");
            case NOT_CONTAINS -> brute == null ? null
                    : cb.notLike(cb.lower(chemin.as(String.class)),
                            "%" + brute.toString().toLowerCase(Locale.ROOT) + "%");
            case GTE -> brute == null ? null : cb.greaterThanOrEqualTo(
                    (Path<Comparable>) chemin, (Comparable) convertir(chemin.getJavaType(), brute));
            case LTE -> brute == null ? null : cb.lessThanOrEqualTo(
                    (Path<Comparable>) chemin, (Comparable) convertir(chemin.getJavaType(), brute));
            case GT -> brute == null ? null : cb.greaterThan(
                    (Path<Comparable>) chemin, (Comparable) convertir(chemin.getJavaType(), brute));
            case LT -> brute == null ? null : cb.lessThan(
                    (Path<Comparable>) chemin, (Comparable) convertir(chemin.getJavaType(), brute));
            case BETWEEN -> brute == null || filtre.getValueTo() == null ? null
                    : cb.between((Path<Comparable>) chemin,
                            (Comparable) convertir(chemin.getJavaType(), brute),
                            (Comparable) convertir(chemin.getJavaType(), filtre.getValueTo()));
            case IN -> valeursDe(cb, chemin, brute);
        };
    }

    private Predicate texte(CriteriaBuilder cb, Path<?> chemin, Object valeur,
                            String avant, String apres) {
        if (valeur == null) {
            return null;
        }
        return cb.like(cb.lower(chemin.as(String.class)),
                avant + valeur.toString().toLowerCase(Locale.ROOT) + apres);
    }

    /**
     * Appartenance à un ensemble.
     *
     * <p>Une sélection dont <b>aucune</b> valeur n'est exploitable ne rend rien, et non pas tout :
     * l'utilisateur a filtré, et lui répondre par la liste entière lui ferait croire que son filtre
     * a porté. C'est le défaut le plus difficile à voir — la réponse paraît valide.</p>
     */
    private Predicate valeursDe(CriteriaBuilder cb, Path<?> chemin, Object brute) {
        if (brute == null) {
            return null;
        }
        Collection<?> brutes = brute instanceof Collection<?> collection
                ? collection : List.of(brute);
        List<Object> valeurs = new ArrayList<>();
        for (Object valeur : brutes) {
            if (valeur == null) {
                continue;
            }
            try {
                valeurs.add(convertir(chemin.getJavaType(), valeur));
            } catch (RuntimeException e) {
                // Valeur qui ne se convertit pas au type de la colonne : elle ne désigne rien.
                // L'écarter vaut mieux que de refuser la recherche pour une entrée fautive.
                continue;
            }
        }
        return valeurs.isEmpty() ? cb.disjunction() : chemin.in(valeurs);
    }

    /**
     * Amène une valeur reçue en JSON au type réel de la colonne.
     *
     * <p>Le corps d'une requête ne porte que des chaînes, des nombres et des booléens : un
     * identifiant, une date ou une énumération y arrivent en texte. Comparés tels quels à une
     * colonne typée, ils échouent à l'exécution — après le déploiement, sur la première
     * recherche.</p>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object convertir(Class<?> type, Object valeur) {
        if (valeur == null || type.isInstance(valeur)) {
            return valeur;
        }
        String texte = valeur.toString().trim();
        if (texte.isEmpty()) {
            return null;
        }
        if (type == String.class) {
            return texte;
        }
        if (type == Boolean.class || type == boolean.class) {
            return Boolean.parseBoolean(texte);
        }
        if (type == Integer.class || type == int.class) {
            return Integer.valueOf(texte);
        }
        if (type == Long.class || type == long.class) {
            return Long.valueOf(texte);
        }
        if (type == Double.class || type == double.class) {
            return Double.valueOf(texte);
        }
        if (type == BigDecimal.class) {
            return new BigDecimal(texte);
        }
        if (type == UUID.class) {
            return UUID.fromString(texte);
        }
        if (type == LocalDate.class) {
            return LocalDate.parse(texte);
        }
        if (type == LocalDateTime.class) {
            // Une date seule vaut le début de la journée : l'écran envoie « 2026-08-01 » pour une
            // borne, et exiger l'heure de sa part n'apporterait rien.
            return texte.length() == 10 ? LocalDate.parse(texte).atStartOfDay()
                    : LocalDateTime.parse(texte);
        }
        if (type == LocalTime.class) {
            return LocalTime.parse(texte);
        }
        if (type == Instant.class) {
            return Instant.parse(texte);
        }
        if (Enum.class.isAssignableFrom(type)) {
            return Enum.valueOf((Class<? extends Enum>) type, texte);
        }
        return valeur;
    }
}
