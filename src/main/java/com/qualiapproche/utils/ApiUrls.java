package com.qualiapproche.utils;

public class ApiUrls {
    public static final String QUALI_APPROCHE_ROOT_URL="api/v1/quali-approche";

    public static final String FOURNISSEUR_ROOT_URL = QUALI_APPROCHE_ROOT_URL + "/fournisseurs";
    public static final String CREATE_FOURNISSEUR = "/create";
    public static final String GET_ALL_FOURNISSEUR = "/all";
    public static final String UPDATE_FOURNISSEUR = "/update";
    public static final String ASSIGN_CRICTERE_FOURNISSEUR = "/{fournisseurId}/criteres";
    public static final String FOURNISSEUR_GET_CRICTERE_EVALUATION = "/{fournisseurId}";


    public static final String CATEGORIE_FICHIER_ROOT_URL = QUALI_APPROCHE_ROOT_URL + "/categorie/fichier";
    public static final String CREATE_CATEGORIE_FICHIER = "/create";
    public static final String UPDATE_CATEGORIE_FICHIER = "/update";
    public static final String GET_ALL_CATEGORIE_FICHIER = "/all";

    public static final String FICHIER_ROOT_URL = QUALI_APPROCHE_ROOT_URL + "/fichier";
    public static final String CREATE_FICHIER_ROOT_URL = "/create";
    public static final String DELETE_FICHIER_ROOT_URL = "/delete" + "/{fichierId}";

    /* Action corrective et preventive urls */
    public static final String ACTION_ROOT_URL =QUALI_APPROCHE_ROOT_URL+ "/action-corrective";
    public static final String GET_ALL_ACTION = "/all";
    public static final String GET_ACTION_BY_ID = "/get";
    public static final String UPDATE_ACTION = "/update";
    public static final String DELETE_ACTION = "/delete";


    /* Formation urls */
    public static final String FORMATION_ROOT_URL =QUALI_APPROCHE_ROOT_URL+ "/formation";
    public static final String GET_ALL_FORMATION = "/all";
    public static final String GET_FORMATION_BY_ID = "/get";
    public static final String UPDATE_FORMATION = "/update";
    public static final String DELETE_FORMATION = "/delete";


    public static final String CRICTERE_EVALUATION_ROOT_URL = QUALI_APPROCHE_ROOT_URL + "/crictere-evaluation";
    public static final String CREATE_CRICTERE_EVALUATION = "/create";
    public static final String GET_CRICTERE_EVALUATION_BY_ID = "/get";
    public static final String GET_ALL_CRICTERE_EVALUATION = "/all";
    public static final String UPDATE_CRICTERE_EVALUATION = "/update";
    public static final String DELETE_CRICTERE_EVALUATION = "/delete";

    public static final String DEPARTEMENT_ROOT_URL = QUALI_APPROCHE_ROOT_URL + "/departement";
    public static final String CREATE_DEPARTEMENT = "/create";
    public static final String GET_DEPARTEMENT_BY_ID = "/get";
    public static final String GET_ALL_DEPARTEMENT = "/all";
    public static final String UPDATE_DEPARTEMENT = "/update";
    public static final String DELETE_DEPARTEMENT = "/delete";

    public static final String RISQUE_ROOT_URL = QUALI_APPROCHE_ROOT_URL + "/risque";
    public static final String CREATE_RISQUE = "/create";
    public static final String GET_RISQUE_BY_ID = "/get" + "/{risqueId}";
    public static final String GET_ALL_RISQUE = "/all";
    public static final String UPDATE_RISQUE = "/update";
    public static final String DELETE_RISQUE = "/delete" + "/{risqueId}";


    public static final String NON_CONFORMITE_ROOT_URL = QUALI_APPROCHE_ROOT_URL + "/non-conformite";
    public static final String CREATE_NON_CONFORMITE = "/create";
    public static final String GET_NON_CONFORMITE_BY_ID = "/get" + "/{id}";
    public static final String GET_ALL_NON_CONFORMITE = "/all";
    public static final String UPDATE_NON_CONFORMITE = "/update";
    public static final String DELETE_NON_CONFORMITE = "/delete" + "/{id}";

}
