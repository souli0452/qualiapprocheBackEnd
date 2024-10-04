package com.qualiapproche.utils;

public class ApiUrls {
    public static final String QUALI_APPROCHE_ROOT_URL="api/v1/quali-approche";
    public static final String FOURNISSEUR_ROOT_URL =QUALI_APPROCHE_ROOT_URL+ "/fournisseurs";
    public static final String GET_ALL_FOURNISSEUR = "/all";
    public static final String UPDATE_FOURNISSEUR = "/update";


    public static final String CATEGORIE_FICHIER_ROOT_URL = QUALI_APPROCHE_ROOT_URL + "/categorie/fichier";
    public static final String CREATE_CATEGORIE_FICHIER = "/create";
    public static final String UPDATE_CATEGORIE_FICHIER = "/update";
    public static final String GET_ALL_CATEGORIE_FICHIER = "/all";
    //public static final String GET_BAY_ID_CATEGORIE_FICHIER = "";


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






}
