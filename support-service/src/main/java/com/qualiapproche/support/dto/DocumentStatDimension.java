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
                return "ARCHIVE";
            }
            if (d.isObsolete()) {
                return "OBSOLETE";
            }
            if (d.isEsTraiter()) {
                return "VALIDE";
            }
            if (d.getCurrentEtape() != null && !d.getCurrentEtape().isBlank()) {
                return d.getCurrentEtape();
            }
            return "BROUILLON";
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
}
