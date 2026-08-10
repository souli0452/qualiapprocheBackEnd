package com.qualiapproche.common.enumeration;

/**
 * Circuit de traitement retenu pour une non-conformité.
 *
 * <p>C'est le responsable qualité qui le choisit, à l'étape de validation où il désigne aussi le
 * processus destinataire : une non-conformité ne se traite pas de la même façon selon qu'on
 * corrige l'écart constaté ou qu'on s'attaque à ce qui l'a produit.</p>
 *
 * <ul>
 *   <li><b>Correction</b> — on remet en conformité ce qui ne l'était pas. La cause n'a pas à être
 *       recherchée : le plan d'action porte toutes les colonnes <b>sauf</b> celle-là.</li>
 *   <li><b>Action corrective</b> — on supprime la cause pour que l'écart ne revienne pas. Le plan
 *       d'action est alors complet, cause comprise.</li>
 * </ul>
 *
 * <p>Les deux valeurs s'appelaient {@code A} et {@code B}, sans que rien ne dise ce qu'elles
 * désignaient : ni l'écran, ni le circuit, ni le lecteur du code. {@link #depuisValeur(String)}
 * reconnaît encore ces anciens noms, pour les dossiers antérieurs dont un export ou un client
 * porterait toujours la lettre.</p>
 */
public enum Circuit {

    /** Suppression de la cause : plan d'action complet. */
    ACTION_CORRECTIVE("Action corrective"),

    /** Remise en conformité : plan d'action sans la colonne « cause ». */
    CORRECTION("Correction");

    private final String libelle;

    Circuit(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    /**
     * Le circuit désigné par une valeur, quelle que soit sa forme.
     *
     * <p>Le moteur de workflow ne transporte que des chaînes : la valeur retenue à l'étape arrive
     * ici telle que la liste de choix l'a produite. Le nom de la constante, le libellé affiché et
     * les anciennes lettres sont tous acceptés — les refuser aurait fait perdre en silence le choix
     * du responsable qualité, alors que la décision, elle, était déjà prise.</p>
     *
     * @return le circuit reconnu, ou {@code null} si la valeur est vide ou inconnue
     */
    public static Circuit depuisValeur(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        String nettoyee = valeur.trim();
        for (Circuit circuit : values()) {
            if (circuit.name().equalsIgnoreCase(nettoyee) || circuit.libelle.equalsIgnoreCase(nettoyee)) {
                return circuit;
            }
        }
        // Les deux lettres d'origine, dans l'ordre où elles étaient déclarées.
        if ("A".equalsIgnoreCase(nettoyee)) {
            return ACTION_CORRECTIVE;
        }
        if ("B".equalsIgnoreCase(nettoyee)) {
            return CORRECTION;
        }
        return null;
    }
}
