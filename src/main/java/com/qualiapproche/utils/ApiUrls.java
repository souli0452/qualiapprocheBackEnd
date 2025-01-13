package com.qualiapproche.utils;

public class ApiUrls {

    /* Base urls */
    public static final String QUALI_APPROCHE_ROOT_URL="api/v1/quali-approche";

    public static final String URL_ROLES =QUALI_APPROCHE_ROOT_URL+"/roles";


    /* Fournnisseur urls */

    public static final String FOURNISSEUR_ROOT_URL = QUALI_APPROCHE_ROOT_URL + "/fournisseur";
    public static final String CREATE_FOURNISSEUR = "/create";
    public static final String DELETE_FOURNISSEUR = "/delete/{id}";
    public static final String GET_ALL_FOURNISSEUR = "/all";
    public static final String UPDATE_FOURNISSEUR = "/update";
    public static final String ASSIGN_CRICTERE_FOURNISSEUR = "/{fournisseurId}/criteres";
    public static final String FOURNISSEUR_GET_CRICTERE_EVALUATION = "/{fournisseurId}";

    /* Catégories de fichiers urls */

    public static final String CATEGORIE_FICHIER_ROOT_URL = QUALI_APPROCHE_ROOT_URL + "/categorie/fichier";
    public static final String CREATE_CATEGORIE_FICHIER = "/create";
    public static final String UPDATE_CATEGORIE_FICHIER = "/update";
    public static final String GET_ALL_CATEGORIE_FICHIER = "/all";

    public static final String FICHIER_ROOT_URL = QUALI_APPROCHE_ROOT_URL + "/fichier";
    public static final String CREATE_FICHIER_ROOT_URL = "/create";
    public static final String DELETE_FICHIER_ROOT_URL = "/delete" + "/{fichierId}";

    /* Action corrective et preventive urls */

    public static final String ACTION_ROOT_URL =QUALI_APPROCHE_ROOT_URL + "/action-corrective";
    public static final String CREATE_ACTION = "/create";
    public static final String GET_ALL_ACTION = "/all";
    public static final String GET_ACTION_BY_ID = "/get/{id}";
    public static final String UPDATE_ACTION = "/update";
    public static final String DELETE_ACTION = "/delete/{id}";

    /* Formation urls */

    public static final String FORMATION_ROOT_URL =QUALI_APPROCHE_ROOT_URL+ "/formation";
    public static final String CREATE_FORMATION = "/create";
    public static final String GET_ALL_FORMATION = "/all";
    public static final String GET_FORMATION_BY_ID = "/get/{id}";
    public static final String UPDATE_FORMATION = "/update";
    public static final String DELETE_FORMATION = "/delete/{id}";

    /* Crictère d'évaluation urls */

    public static final String CRICTERE_EVALUATION_ROOT_URL = QUALI_APPROCHE_ROOT_URL + "/critere-evaluation";
    public static final String CREATE_CRICTERE_EVALUATION = "/create";
    public static final String GET_CRICTERE_EVALUATION_BY_ID = "/get";
    public static final String GET_ALL_CRICTERE_EVALUATION = "/all";
    public static final String UPDATE_CRICTERE_EVALUATION = "/update";
    public static final String DELETE_CRICTERE_EVALUATION = "/delete/{id}";

    /* Département urls */

    public static final String DEPARTEMENT_ROOT_URL = QUALI_APPROCHE_ROOT_URL + "/departement";
    public static final String CREATE_DEPARTEMENT = "/create";
    public static final String GET_DEPARTEMENT_BY_ID = "/get";
    public static final String GET_ALL_DEPARTEMENT = "/all";
    public static final String UPDATE_DEPARTEMENT = "/update";
    public static final String DELETE_DEPARTEMENT = "/delete/{id}";

    /* Risque urls */

    public static final String RISQUE_ROOT_URL = QUALI_APPROCHE_ROOT_URL + "/risque";
    public static final String CREATE_RISQUE = "/create";
    public static final String GET_RISQUE_BY_ID = "/get" + "/{risqueId}";
    public static final String GET_ALL_RISQUE = "/all";
    public static final String UPDATE_RISQUE = "/update";
    public static final String DELETE_RISQUE = "/delete" + "/{risqueId}";

    /* Non conformité urls */
    
    public static final String NON_CONFORMITE_ROOT_URL = QUALI_APPROCHE_ROOT_URL + "/non-conformite";
    public static final String CREATE_NON_CONFORMITE = "/create";
    public static final String GET_NON_CONFORMITE_BY_ID = "/get" + "/{id}";
    public static final String GET_ALL_NON_CONFORMITE = "/all";
    public static final String UPDATE_NON_CONFORMITE = "/update";
    public static final String DELETE_NON_CONFORMITE = "/delete" + "/{id}";

    /* Reclamation urls */

    public static final String RECLAMATION_ROOT_URL = QUALI_APPROCHE_ROOT_URL + "/reclamation";
    public static final String CREATE_RECLAMATION = "/create";
    public static final String GET_RECLAMATION_BY_ID = "/get" + "/{id}";
    public static final String GET_ALL_RECLAMATION = "/all";
    public static final String UPDATE_RECLAMATION = "/update";
    public static final String DELETE_RECLAMATION = "/delete" + "/{id}";

    /* Reglementation urls */

    public static final String REGLEMENTATION_ROOT_URL = QUALI_APPROCHE_ROOT_URL + "/reglementation";
    public static final String CREATE_REGLEMENTATION = "/create";
    public static final String GET_REGLEMENTATION_BY_ID = "/get" + "/{id}";
    public static final String GET_ALL_REGLEMENTATION = "/all";
    public static final String UPDATE_REGLEMENTATION = "/update";
    public static final String DELETE_REGLEMENTATION = "/delete" + "/{id}";

    /* Prestataire urls */

    public static final String PRESTATAIRE_ROOT_URL = QUALI_APPROCHE_ROOT_URL + "/prestataire";
    public static final String CREATE_PRESTATAIRE = "/create";
    public static final String GET_PRESTATAIRE_BY_ID = "/get" + "/{id}";
    public static final String GET_ALL_PRESTATAIRE = "/all";
    public static final String UPDATE_PRESTATAIRE = "/update";
    public static final String DELETE_PRESTATAIRE = "/delete" + "/{id}";

    /* Audites urls */

    public static final String AUDITE_ROOT_URL = QUALI_APPROCHE_ROOT_URL + "/audite";
    public static final String CREATE_AUDITE = "/create";
    public static final String GET_AUDITE_BY_ID = "/get" + "/{id}";
    public static final String GET_ALL_AUDITE = "/all";
    public static final String UPDATE_AUDITE = "/update";
    public static final String DELETE_AUDITE = "/delete" + "/{id}";

    /* Exigence urls */

    public static final String EXIGENCE_ROOT_URL = QUALI_APPROCHE_ROOT_URL + "/exigence";
    public static final String CREATE_EXIGENCE = "/create";
    public static final String GET_EXIGENCE_BY_ID = "/get" + "/{id}";
    public static final String GET_ALL_EXIGENCE = "/all";
    public static final String UPDATE_EXIGENCE = "/update";
    public static final String DELETE_EXIGENCE = "/delete" + "/{id}";

    /* Exigence urls */

    public static final String PRODUIT_ROOT_URL = QUALI_APPROCHE_ROOT_URL + "/produit";
    public static final String CREATE_PRODUIT = "/create";
    public static final String GET_PRODUIT_BY_ID = "/get" + "/{id}";
    public static final String GET_ALL_PRODUIT = "/all";
    public static final String UPDATE_PRODUIT = "/update";
    public static final String DELETE_PRODUIT = "/delete" + "/{id}";

}
