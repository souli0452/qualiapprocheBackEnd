package com.qualiapproche.support.numerotation;

import com.qualiapproche.common.cloisonnement.Cloisonnement;
import com.qualiapproche.common.serie.AttributionDeNumero;
import com.qualiapproche.common.serie.StrategieDeSerie;
import com.qualiapproche.support.model.SerieNumero;
import com.qualiapproche.support.repository.DocumentQmsRepository;
import com.qualiapproche.support.repository.SerieNumeroRepository;
import org.springframework.stereotype.Component;

/**
 * Le numéro d'un document qualité : {@code <TYPE>-<SIGLE>-<ANNÉE>-<001>}, une série par préfixe.
 *
 * <p>Le format n'a pas changé. Le tirage, lui, se faisait par le plus grand numéro + 1, sans verrou
 * et en ordre alphabétique : deux créations simultanées se heurtaient, et passé 999 la série
 * repartait sur des numéros déjà pris.</p>
 *
 * <p>Chaque client a ses séries : deux clients peuvent avoir le même type et le même sigle de
 * service, et leurs numéros ne se doivent rien. La clé de série porte donc la direction
 * ({@link SeriesDuClient}), et la reprise sur l'existant ne lit que les documents du client — la
 * requête est native, Hibernate ne la filtre pas.</p>
 */
@Component
public class NumerotationDesDocuments implements StrategieDeSerie<String> {

    private final DocumentQmsRepository documents;
    private final AttributionDeNumero<SerieNumero> attribution;

    public NumerotationDesDocuments(DocumentQmsRepository documents, SerieNumeroRepository series) {
        this.documents = documents;
        this.attribution = new SeriesDuClient(series);
    }

    /** Le prochain numéro pour ce type et ce service, pris dans la transaction de création. */
    public String attribuer(String typeCode, String serviceSigle, int annee) {
        String prefixe = prefixe(typeCode, serviceSigle, annee);
        return attribution.attribuer(this, prefixe, numero -> documents.findByDocumentNumber(numero).isPresent());
    }

    static String prefixe(String typeCode, String serviceSigle, int annee) {
        return typeCode.toUpperCase() + "-" + serviceSigle.toUpperCase() + "-" + annee + "-";
    }

    @Override
    public String cle(String prefixe) {
        return SeriesDuClient.cle("DOC|" + prefixe);
    }

    /** Le plus grand rang déjà émis sous ce préfixe, comparé en nombre et non en texte. */
    @Override
    public long valeurInitiale(String prefixe) {
        Long dernier = documents.dernierRangSousPrefixe(prefixe, Cloisonnement.courante());
        return dernier == null ? 0 : dernier;
    }

    /** Trois chiffres au moins ; au-delà de 999, le rang s'écrit simplement plus long. */
    @Override
    public String formater(String prefixe, long rang) {
        return prefixe + String.format("%03d", rang);
    }
}
