package com.qualiapproche.support.dto;

import com.qualiapproche.support.model.DocumentQms;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Dimension selon laquelle regrouper les documents pour la statistique générique
 * ({@code GET /documents/stats/by/{dimension}}). Ajouter un type de statistique = ajouter
 * une constante ici, sans toucher au service ni au contrôleur.
 */
@Schema(description = "Clé de regroupement de la statistique générique. Chaque constante dit "
        + "quelle valeur du document sert de clé au comptage ; les documents dont cette valeur est "
        + "absente ne sont comptés nulle part. STATUT et CURRENT_ETAPE se recouvrent en partie : "
        + "le premier ne nomme l'étape que si le document est encore en circuit.")
public enum DocumentStatDimension {

    DOCUMENT_TYPE {
        public String extract(DocumentQms d) {
            return d.getDocumentType();
        }
    },
    /** Statut affiché du document (brouillon/en cours/validé/obsolète/archivé) — même règle que {@code getDocumentDisplayState}. */
    STATUT {
        public String extract(DocumentQms d) {
            if (d.isArchived()) {
                return STATUT_ARCHIVE;
            }
            if (d.isObsolete()) {
                return STATUT_OBSOLETE;
            }
            if (d.isEsTraiter()) {
                return STATUT_EN_VIGUEUR;
            }
            if (d.getCurrentEtape() != null && !d.getCurrentEtape().isBlank()) {
                return d.getCurrentEtape();
            }
            return STATUT_BROUILLON;
        }
    },
    DOMAINE {
        public String extract(DocumentQms d) {
            return d.getDomaine();
        }
    },
    SERVICE {
        public String extract(DocumentQms d) {
            return d.getServiceLibelle();
        }
    },
    STATUT_LEGAL {
        public String extract(DocumentQms d) {
            return d.getStatutLegal();
        }
    },
    REDACTEUR {
        public String extract(DocumentQms d) {
            return d.getRedacteur();
        }
    },
    PROCESSUS_DEST {
        public String extract(DocumentQms d) {
            return d.getProcessusDestLibelle();
        }
    },
    CURRENT_ETAPE {
        public String extract(DocumentQms d) {
            return d.getCurrentEtape();
        }
    },
    ANNEE_CREATION {
        public String extract(DocumentQms d) {
            return d.getCreatedAt() != null ? String.valueOf(d.getCreatedAt().getYear()) : null;
        }
    },
    MOIS_CREATION {
        public String extract(DocumentQms d) {
            return d.getCreatedAt() != null
                    ? String.format("%04d-%02d", d.getCreatedAt().getYear(), d.getCreatedAt().getMonthValue())
                    : null;
        }
    },
    CONFIDENTIALITE {
        public String extract(DocumentQms d) {
            return d.isConfidentiel() ? "CONFIDENTIEL" : "STANDARD";
        }
    },
    DOCUMENT_EXTERNE {
        public String extract(DocumentQms d) {
            return d.isDocumentExterne() ? "EXTERNE" : "INTERNE";
        }
    };

    public abstract String extract(DocumentQms document);

    /**
     * Les quatre statuts que {@link #STATUT} nomme lui-même. Toute autre valeur qu'il rend est le
     * code d'une étape, et signifie donc que le document est en circuit.
     *
     * <p>Ils étaient écrits en clair dans la dimension et recopiés partout où l'on voulait compter
     * les documents en vigueur : deux orthographes suffisaient à faire diverger deux chiffres tirés
     * de la même règle.</p>
     */
    public static final String STATUT_ARCHIVE = "ARCHIVE";
    public static final String STATUT_OBSOLETE = "OBSOLETE";
    /** Validé, donc officiellement applicable. */
    public static final String STATUT_EN_VIGUEUR = "VALIDE";
    /** Déposé, jamais remis au circuit. */
    public static final String STATUT_BROUILLON = "BROUILLON";

    /**
     * Le document est-il en cours de rédaction, de vérification ou d'approbation ?
     *
     * <p>Se lit à l'envers : est en circuit tout document dont le statut affiché n'est aucun des
     * quatre que la dimension nomme — c'est alors le code de son étape courante. Énumérer les
     * étapes aurait supposé de les connaître, or elles sont paramétrables et un circuit peut en
     * gagner une sans que rien ici ne l'apprenne.</p>
     */
    public static boolean estEnCircuit(DocumentQms document) {
        String statut = STATUT.extract(document);
        return !STATUT_ARCHIVE.equals(statut)
                && !STATUT_OBSOLETE.equals(statut)
                && !STATUT_EN_VIGUEUR.equals(statut)
                && !STATUT_BROUILLON.equals(statut);
    }

    /** Le document est-il officiellement applicable ? */
    public static boolean estEnVigueur(DocumentQms document) {
        return STATUT_EN_VIGUEUR.equals(STATUT.extract(document));
    }
}
