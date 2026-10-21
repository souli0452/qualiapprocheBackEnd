package com.qualiapproche.evaluation.enumeration;

public enum StatutSignature {
    EN_ATTENTE,
    SIGNEE,
    REFUSEE,
    /** Signataire remplacé avant d'avoir signé : le visa reste au circuit, sans plus rien attendre. */
    REMPLACEE
}
