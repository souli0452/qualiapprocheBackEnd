package com.qualiapproche.workflow.model;

import java.util.Locale;

/**
 * Qui prévenir quand un dossier atteint une étape, lorsque ce n'est pas celui qui doit y agir.
 *
 * <p>Le courriel d'étape part par défaut vers les porteurs du rôle responsable de l'étape, dans la
 * structure où le dossier se trouve : c'est la règle, et elle vaut pour huit étapes sur neuf. La
 * neuvième la contredit — la clôture d'une non-conformité doit être annoncée au pilote du
 * <b>processus soumissionnaire</b>, celui qui a signalé l'écart et qui attend d'apprendre ce qu'il
 * est devenu. Or à ce stade le dossier a changé de structure depuis longtemps : il a été confié au
 * processus destinataire par la validation qualité, et son rôle de clôture est celui du responsable
 * qualité. Le destinataire n'est donc ni le rôle de l'étape, ni la structure du dossier.</p>
 *
 * <p>D'où une désignation portée par l'étape, sous la forme {@code RÔLE@PORTÉE} — par exemple
 * {@code PILOTE@STRUCTURE_EMETTRICE}. Une étape qui ne la porte pas s'en tient à la règle. La
 * lire d'un cas particulier écrit dans le notificateur (« si l'étape s'appelle CLOTURE… ») aurait
 * figé dans le code une décision qui appartient au circuit, et qu'un administrateur doit pouvoir
 * reprendre depuis l'éditeur.</p>
 *
 * @param role   rôle dont les porteurs sont prévenus
 * @param portee structure où les chercher
 */
public record DestinataireCourriel(String role, DestinataireCourriel.Portee portee) {

    /** Où chercher les porteurs du rôle. */
    public enum Portee {

        /** La structure où le dossier se trouve à cet instant — le comportement ordinaire. */
        STRUCTURE_DOSSIER,

        /**
         * La structure qui a ouvert le dossier, quoi qu'il lui soit arrivé depuis.
         *
         * <p>Un dossier change de structure en cours de route ; celle d'où il vient, elle, ne
         * change pas. C'est à elle qu'on rend compte.</p>
         */
        STRUCTURE_EMETTRICE
    }

    private static final char SEPARATEUR = '@';

    /**
     * Lit la désignation portée par une étape.
     *
     * <p>Une valeur vide, mal formée ou dont la portée est inconnue rend {@code null} : le
     * notificateur s'en tient alors à la règle ordinaire. Refuser l'envoi parce qu'une chaîne de
     * configuration est fautive priverait le destinataire d'un courriel qu'il aurait de toute façon
     * dû recevoir, sous une forme ou une autre.</p>
     */
    public static DestinataireCourriel lire(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        String nettoyee = valeur.trim();
        int separateur = nettoyee.indexOf(SEPARATEUR);
        if (separateur <= 0 || separateur == nettoyee.length() - 1) {
            return null;
        }

        String role = nettoyee.substring(0, separateur).trim();
        String portee = nettoyee.substring(separateur + 1).trim().toUpperCase(Locale.ROOT);
        try {
            return new DestinataireCourriel(role, Portee.valueOf(portee));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** La désignation, telle qu'elle s'écrit sur une étape. */
    public String ecrire() {
        return role + SEPARATEUR + portee.name();
    }
}
