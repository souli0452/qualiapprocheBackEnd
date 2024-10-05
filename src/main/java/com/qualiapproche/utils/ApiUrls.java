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


    public static final String CRICTERE_EVALUATION_ROOT_URL = QUALI_APPROCHE_ROOT_URL + "/crictere-evaluation";
    public static final String CREATE_CRICTERE_EVALUATION = "/create";







}
