package com.qualiapproche.evaluation.service;

import com.qualiapproche.evaluation.entities.CodificationDocument;
import com.qualiapproche.evaluation.repository.CodificationDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.UUID;

/**
 * Les règles de numérotation, et la distribution des références.
 *
 * <p>Sans règle posée pour un type de document, la référence suit un format par défaut —
 * {@code AUD-2026-001} pour un audit, {@code QS-PLA-2026-001} pour un plan — plutôt que de
 * bloquer la création : une organisation qui n'a pas encore paramétré sa codification doit
 * pouvoir planifier son premier audit. Le préfixe d'un plan ou d'un rapport reprend celui d'une
 * règle déjà posée pour l'autre, la structure n'ayant qu'un préfixe.</p>
 */
@Service
public class CodificationDocumentService extends ParametrageSimpleService<CodificationDocument> {

    public static final String AUDIT = CodificationDocument.AUDIT;
    public static final String PLAN_AUDIT = "PLA";
    public static final String RAPPORT_AUDIT = "RAP";

    private static final String PREFIXE_PAR_DEFAUT = "QS";

    private final CodificationDocumentRepository depot;

    public CodificationDocumentService(CodificationDocumentRepository depot) {
        super(depot);
        this.depot = depot;
    }

    @Override
    protected String nom() {
        return "règle de codification";
    }

    /** Une règle n'a pas de libellé : on la cherche par son type de document ou son préfixe. */
    @Override
    protected List<String> champsRecherche() {
        return List.of("typeDocument", "prefixe");
    }

    @Override
    protected CodificationDocument nouveau() {
        return new CodificationDocument();
    }

    @Override
    protected void recopier(CodificationDocument saisie, CodificationDocument cible) {
        cible.setTypeDocument(exigerTexte(saisie.getTypeDocument(), "type de document").toUpperCase());
        cible.setPrefixe(exigerTexte(saisie.getPrefixe(), "préfixe"));
        cible.setSeparateur(saisie.getSeparateur() == null ? "-" : saisie.getSeparateur());
        // Le compteur et l'année ne se saisissent qu'à la création : les corriger ensuite ferait
        // resservir des références déjà distribuées.
        if (cible.getId() == null) {
            cible.setCompteur(Math.max(0, saisie.getCompteur()));
            cible.setAnnee(saisie.getAnnee());
        }
    }

    /**
     * La prochaine référence pour ce type de document.
     *
     * <p>Dans la transaction de l'appelant ({@code MANDATORY}) : si la création de l'audit échoue,
     * le compteur revient en arrière avec elle, et aucun numéro ne se perd.</p>
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public String genererReference(String typeDocument) {
        UUID directionId = Perimetre.exigerUneDirection();
        int annee = Year.now().getValue();
        CodificationDocument regle = depot.findByDirectionIdAndTypeDocument(directionId, typeDocument)
                .orElseGet(() -> depot.save(CodificationDocument.builder()
                        .typeDocument(typeDocument)
                        .prefixe(AUDIT.equals(typeDocument) ? AUDIT : prefixeDeLaStructure(directionId))
                        .separateur("-")
                        .compteur(0)
                        .build()));
        String reference = regle.genererProchaineReference(annee);
        depot.save(regle);
        return reference;
    }

    private String prefixeDeLaStructure(UUID directionId) {
        return depot.findAllByDirectionIdOrderByCreatedAtAsc(directionId).stream()
                .filter(r -> !AUDIT.equals(r.getTypeDocument()))
                .map(CodificationDocument::getPrefixe)
                .findFirst().orElse(PREFIXE_PAR_DEFAUT);
    }
}
