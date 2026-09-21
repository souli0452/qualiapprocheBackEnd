package com.qualiapproche.ia.dto;

import com.qualiapproche.ia.enumeration.TypeAssistance;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Demande d'assistance rédactionnelle.
 *
 * <p>Le {@code contexte} est une map libre (processus, origine, niveau...) plutôt que des champs
 * nommés : chaque type d'assistance attend un contexte différent, et le figer dans la signature
 * imposerait une évolution de l'API à chaque nouveau type.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeAssistanceDto {

    @NotNull(message = "Le type d'assistance est obligatoire.")
    private TypeAssistance typeAssistance;

    /**
     * Brouillon ou notes de l'utilisateur, matière première de la suggestion.
     *
     * <p>Borné parce qu'un collage de plusieurs centaines de milliers de caractères partait tel
     * quel au fournisseur : dépassement de sa fenêtre de contexte, délai épuisé, et facturation
     * le cas échéant avant tout refus. La borne se pose à l'entrée plutôt que sur la panne qui
     * en résulterait.</p>
     *
     * <p>Vingt mille caractères — une trentaine de pages — arrêtent l'abus franc sans jamais
     * gêner un usage réel : une description de non-conformité, même longue, en fait le dixième.
     * Le garde-fou contre le dépassement de fenêtre n'est pas ici mais dans la traduction des
     * pannes du fournisseur, qui rend 503 ou 504 selon le cas. Une borne plus serrée aurait
     * transformé une description un peu longue en refus, là où elle passait auparavant.</p>
     */
    @NotBlank(message = "Le texte source est obligatoire.")
    @Size(max = 20000, message = "Le texte source ne peut pas dépasser 20000 caractères.")
    private String texteSource;

    /**
     * Éléments de contexte connus de l'écran appelant (processus, origine, niveau...).
     *
     * <p>Bornée en nombre d'entrées : la carte vient du client, et rien n'oblige un appelant à
     * s'en tenir aux quelques clés que les écrans envoient.</p>
     */
    @Size(max = 20, message = "Le contexte ne peut pas porter plus de 20 éléments.")
    private Map<String, String> contexte;

    /** Type de la ressource métier concernée, pour la traçabilité (facultatif). */
    private String ressourceType;

    /** Identifiant de la ressource métier concernée, pour la traçabilité (facultatif). */
    private UUID ressourceId;
}
