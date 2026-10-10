package com.qualiapproche.amelioration.numerotation;

import com.qualiapproche.amelioration.entities.SerieNumero;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.SerieNumeroRepository;
import com.qualiapproche.common.serie.AttributionDeNumero;
import com.qualiapproche.common.serie.StrategieDeSerie;
import org.springframework.stereotype.Component;

/**
 * Le numéro d'une non-conformité : {@code NFQT-<SIGLE>-<ANNÉE>-<00001>}, une série par structure
 * et par année.
 *
 * <p>Le format n'a pas changé ; seul le tirage a changé — il se calculait par MAX + 1, sans verrou,
 * et deux déclarations simultanées d'une même structure recevaient le même numéro.</p>
 */
@Component
public class NumerotationDesNonConformites implements StrategieDeSerie<NumerotationDesNonConformites.Contexte> {

    private static final String PREFIXE = "NFQT";

    /** La structure de soumission, son sigle (pour l'affichage) et l'année. */
    public record Contexte(String structureId, String sigle, int annee) { }

    private final NonConformiteRepository nonConformites;
    private final AttributionDeNumero<SerieNumero> attribution;

    public NumerotationDesNonConformites(NonConformiteRepository nonConformites, SerieNumeroRepository series) {
        this.nonConformites = nonConformites;
        this.attribution = new AttributionDeNumero<>(series);
    }

    /** Le prochain numéro, pris dans la transaction de création. */
    public String attribuer(String structureId, String sigle, int annee) {
        Contexte c = new Contexte(structureId == null ? "" : structureId, sigle, annee);
        return attribution.attribuer(this, c,
                numero -> nonConformites.existsByStructureSoumissionIdAndNumeroReference(structureId, numero));
    }

    @Override
    public String cle(Contexte c) {
        return "NC|" + c.structureId() + "|" + c.annee();
    }

    /** Une série créée après coup reprend la suite des numéros déjà émis. */
    @Override
    public long valeurInitiale(Contexte c) {
        Integer dernier = nonConformites.findDernierNumero(c.structureId().isEmpty() ? null : c.structureId(), c.annee());
        return dernier == null ? 0 : dernier;
    }

    @Override
    public String formater(Contexte c, long rang) {
        String sigle = c.sigle() == null || c.sigle().isBlank() ? "NC" : c.sigle();
        return String.format("%s-%s-%d-%05d", PREFIXE, sigle.toUpperCase().replaceAll("\\s+", "_"), c.annee(), rang);
    }

    /** Cinq chiffres : au-delà, le numéro changerait de largeur. */
    @Override
    public long rangMaximal() {
        return 99_999;
    }
}
