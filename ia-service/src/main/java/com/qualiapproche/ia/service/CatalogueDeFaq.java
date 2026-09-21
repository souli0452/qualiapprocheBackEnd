package com.qualiapproche.ia.service;

import com.qualiapproche.common.dto.FaqDto;
import com.qualiapproche.ia.client.ReferentielClient;
import com.qualiapproche.ia.config.IaAssistantProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * La FAQ de l'organisation, mise en forme pour la consigne de l'assistant.
 *
 * <p>C'est le remède de fond au défaut le plus visible du module. Interrogé sur un usage maison,
 * l'assistant produisait une réponse plausible et fausse — il n'a jamais vu l'organisation, et un
 * modèle de langue comble ce qu'il ignore. Lui interdire de parler ne suffit pas : mesuré, un
 * modèle de sept milliards de paramètres ne respecte pas une interdiction négative. Lui donner la
 * réponse, si.</p>
 *
 * <p>La FAQ elle-même vit dans referentiel-service : c'est un référentiel de l'application, que
 * l'aide affiche et que l'assistant se contente de lire. Elle est demandée à chaque tour plutôt
 * que mise en cache — une réponse corrigée doit valoir dès la question suivante, et non au
 * prochain redémarrage.</p>
 *
 * <p>Le texte produit rejoint la consigne système, donc part au fournisseur à chaque tour : d'où
 * le plafond. Sans lui, une FAQ qui grossit au fil des mois finirait par occuper la fenêtre de
 * contexte entière et par évincer la conversation elle-même.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogueDeFaq {

    private final ReferentielClient referentiel;
    private final IaAssistantProperties proprietes;

    /**
     * La FAQ de l'organisation de l'appelant, prête à être jointe à la consigne.
     *
     * <p>Rend une chaîne vide quand il n'y a rien à dire, et <b>aussi quand le référentiel ne
     * répond pas</b> : l'assistant sait encore la méthode et le vocabulaire, qui n'ont que faire
     * de la FAQ. Faire échouer une conversation parce qu'un autre service est momentanément
     * injoignable fermerait l'assistant entier au nom de son complément.</p>
     */
    public String pourLaConsigne() {
        List<FaqDto> publiees;
        try {
            publiees = referentiel.faqPubliee();
        } catch (RuntimeException e) {
            log.warn("FAQ indisponible : l'assistant répondra sans elle. {}", e.getMessage());
            return "";
        }
        if (publiees == null || publiees.isEmpty()) {
            return "";
        }

        StringBuilder texte = new StringBuilder();
        int plafond = Math.max(1, proprietes.getFaqCaracteres());
        int poids = 0;
        int recitees = 0;

        for (FaqDto entree : publiees) {
            if (entree.getQuestion() == null || entree.getReponse() == null) {
                continue;
            }
            // Les pièces jointes sont nommées, jamais lues : l'assistant ne sait pas ouvrir un
            // fichier. Les taire serait pire que de les nommer — la personne repartirait sans
            // savoir qu'un formulaire existe, alors que l'écran d'aide le lui propose.
            String pieces = "";
            if (entree.getFichiers() != null && !entree.getFichiers().isEmpty()) {
                pieces = "\n   (documents joints à cette réponse, à ouvrir depuis l'aide : "
                        + entree.getFichiers().stream()
                                .map(f -> f.getNom() == null ? "document" : f.getNom())
                                .collect(java.util.stream.Collectors.joining(", "))
                        + ")";
            }
            String bloc = "\nQ : " + entree.getQuestion().strip()
                    + "\nR : " + entree.getReponse().strip() + pieces + "\n";
            if (recitees > 0 && poids + bloc.length() > plafond) {
                // Écarté par la fin, jamais par le milieu : l'ordre d'affichage décide de ce qui
                // survit, et l'administrateur le maîtrise depuis l'écran de la FAQ.
                log.warn("FAQ tronquée : {} entrées sur {} tiennent dans le plafond de {}"
                        + " caractères.", recitees, publiees.size(), plafond);
                break;
            }
            texte.append(bloc);
            poids += bloc.length();
            recitees++;
        }

        if (recitees == 0) {
            return "";
        }

        return """


            LA FAQ DE CETTE ORGANISATION — des réponses écrites par ses administrateurs :

            Quand une question s'en approche, réponds AVEC CES MOTS-LÀ, sans les contredire ni
            les compléter de ce que tu crois savoir. Ils font autorité ici, y compris contre
            l'usage courant : une organisation a le droit d'avoir ses propres règles.

            Quand une réponse mentionne des documents joints, dis qu'ils existent et nomme-les :
            la personne les ouvre depuis l'écran d'aide. Tu ne les as pas lus et tu ne prétends
            pas en connaître le contenu.

            Quand aucune n'y répond, ne force pas le rapprochement. Une entrée voisine qui traite
            d'un sujet proche n'est pas une réponse : dis que tu l'ignores et renvoie à
            l'administrateur. Une réponse tirée de travers coûte plus cher qu'un aveu.
            """ + texte + "\n";
    }
}
