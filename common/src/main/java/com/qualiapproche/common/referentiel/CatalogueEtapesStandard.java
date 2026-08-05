package com.qualiapproche.common.referentiel;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.List;

/**
 * Les étapes des circuits de validation livrés avec l'application (document, non-conformité, plan
 * d'action), décrites une seule fois pour les deux services qui en ont besoin.
 *
 * <p>Ces étapes existaient jusqu'ici uniquement à l'intérieur des circuits par défaut, montés par
 * {@code WorkflowDataInitializer} dans la base de workflow-service. Le catalogue d'étapes
 * réutilisables — {@code qms_workflow_step_templates}, dans la base de support-service, et ce que
 * propose l'éditeur de circuits pour composer une nouvelle étape — démarrait, lui, vide : les
 * étapes standard de l'application n'y figuraient pas, et il fallait ressaisir « Vérification » ou
 * « Imputation » à la main, en espérant retomber sur le même code.</p>
 *
 * <p>Or ce code porte une exigence : c'est lui qui rend une même nature d'étape comparable d'un
 * circuit à l'autre, et donc les statistiques agrégeables. Un catalogue vide le laissait à
 * l'orthographe de chacun.</p>
 *
 * <p>Le fichier ne décrit que ce qui se partage d'un circuit à l'autre — code, libellé, rôle
 * responsable, description. L'ordre, l'état de traitement, le gabarit d'e-mail, les champs de
 * saisie et les transitions n'ont de sens qu'au sein d'un circuit précis : ils restent chez
 * {@code WorkflowDataInitializer}, dont un test vérifie qu'il n'introduit aucune étape absente
 * d'ici.</p>
 */
public final class CatalogueEtapesStandard {

    private static final String RESSOURCE = "workflow-step-catalogue.json";

    private CatalogueEtapesStandard() {
    }

    /**
     * Une entrée du catalogue.
     *
     * @param code             identifiant fonctionnel, unique et immuable une fois publié
     * @param nomEtape         libellé présenté à l'écran
     * @param responsableRole  rôle habilité à décider sur l'étape ; il doit exister parmi les rôles
     *                         créés par {@code RoleInitializer}, faute de quoi l'étape n'aurait ni
     *                         titulaire pour décider ni destinataire à notifier
     * @param description      ce que l'étape attend de son titulaire
     */
    public record EtapeStandard(String code, String nomEtape, String responsableRole, String description) {
    }

    /**
     * Un catalogue illisible interrompt le démarrage plutôt que de laisser filer un catalogue
     * incomplet : la panne serait sinon indiscernable d'une base simplement neuve.
     */
    public static List<EtapeStandard> charger() {
        try (InputStream flux = new ClassPathResource(RESSOURCE).getInputStream()) {
            return new ObjectMapper().readValue(flux, new TypeReference<List<EtapeStandard>>() { });
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Catalogue des étapes standard (" + RESSOURCE + ") illisible.", e);
        }
    }
}
