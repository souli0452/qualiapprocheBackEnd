package com.qualiapproche.workflow.model;

import com.qualiapproche.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Familles de ressources auxquelles un circuit de validation peut s'appliquer.
 *
 * <p>Le {@code resourceType} d'un circuit n'était contraint par rien : la colonne accepte une
 * chaîne libre, et le front proposait volontiers un code de type documentaire ('PRO', 'ENR'…).
 * Un circuit ainsi enregistré était pourtant inutilisable. Les services métier ouvrent toujours
 * leurs circuits sur la <b>famille</b> de la ressource — support-service passe {@code DOCUMENT}
 * quel que soit le type du document — si bien que l'ouverture échouait en
 * « Le circuit « X » s'applique aux ressources de type PRO, pas DOCUMENT », au moment le plus
 * coûteux : après le dépôt du fichier, alors que la configuration fautive datait de plusieurs
 * jours. Et l'aurait-elle franchie que la remise des notifications, qui ne sait router que ces
 * trois familles ({@code WorkflowNotificationService}), n'aurait eu aucun destinataire.</p>
 *
 * <p>Le refus est donc rendu à la configuration du circuit, là où l'erreur est commise et où
 * elle se corrige. Pour réserver un circuit aux documents d'un type donné, le circuit reste de
 * famille {@code DOCUMENT} et c'est le type de document qui le désigne, par son
 * {@code workflowId}.</p>
 */
public enum TypeRessource {
    DOCUMENT,
    NON_CONFORMITE,
    PLAN_ACTION,
    /**
     * Demande de modification ou de suppression d'un document.
     *
     * <p>Elle suit un circuit comme le document lui-même : c'est ce qui lui donne des étapes, des
     * responsables, une traçabilité et des notifications sans qu'il faille les réinventer. Son
     * aboutissement n'est pas un simple changement d'état — une modification acceptée ouvre le
     * dépôt du fichier remplaçant, une suppression acceptée retire le document.</p>
     */
    DEMANDE_DOCUMENT;

    /**
     * Rend la famille sous sa forme canonique, ou refuse la valeur en 400.
     *
     * <p>La casse et les espaces sont tolérés — {@code " document "} vaut {@code DOCUMENT} — car
     * ils ne traduisent aucune intention distincte, alors qu'une comparaison sensible à la casse
     * ferait échouer l'ouverture bien plus tard.</p>
     */
    public static String normaliser(String valeur) {
        String candidat = valeur != null ? valeur.trim().toUpperCase() : "";
        if (candidat.isEmpty()) {
            throw new BusinessException(
                    "Le type de ressource du circuit est obligatoire : " + libelleValeursAutorisees() + ".",
                    HttpStatus.BAD_REQUEST);
        }

        return Arrays.stream(values())
                .map(Enum::name)
                .filter(candidat::equals)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "« " + valeur.trim() + " » ne désigne pas une famille de ressource. Un circuit "
                                + "s'applique à " + libelleValeursAutorisees() + ". Pour réserver un circuit "
                                + "aux documents d'un type précis, choisissez DOCUMENT puis désignez ce "
                                + "circuit depuis le type de document concerné.",
                        HttpStatus.BAD_REQUEST));
    }

    /** Valeurs proposables à l'utilisateur qui configure un circuit. */
    public static List<String> valeursAutorisees() {
        return Arrays.stream(values()).map(Enum::name).toList();
    }

    private static String libelleValeursAutorisees() {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
    }
}
