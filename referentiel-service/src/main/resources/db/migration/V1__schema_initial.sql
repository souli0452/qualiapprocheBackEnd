--
-- referentiel-service — schéma initial, relevé du schéma en service au 7 août 2026.
--
-- Référentiels transverses : structures, processus, réglages de l'organisation.
--
-- Ce script est le POINT DE DÉPART de l'historique de migration : il décrit le schéma tel que
-- « ddl-auto: update » l'avait construit. Une base déjà en service est marquée « v1 » sans que ce
-- script y soit rejoué (baseline-on-migrate) ; une base vide le reçoit intégralement. Les deux
-- suivent ensuite la même suite de migrations, ce qui est tout l'objet de Flyway.
--
-- NE JAMAIS MODIFIER un script déjà appliqué : Flyway en vérifie l'empreinte et refuserait de
-- démarrer. Toute correction du schéma passe par un nouveau V<n+1>__....sql.
--

--
--

SET default_table_access_method = heap;

--
-- Name: abonnements_directions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.abonnements_directions (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    active boolean NOT NULL,
    date_debut timestamp(6) without time zone,
    date_fin timestamp(6) without time zone,
    license character varying(3000),
    subscribed_direction_id uuid
);

--
-- Name: archivage; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.archivage (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    date_archivage timestamp(6) without time zone
);

--
-- Name: categorie_fichier; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.categorie_fichier (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    description_categorie character varying(255),
    libelle_categorie character varying(255),
    necessite_demande_creation_fichier character varying(255)
);

--
-- Name: config_global; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.config_global (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    email_rq character varying(255),
    nom_complet_rq character varying(255),
    rappel_echeance integer NOT NULL
);

--
-- Name: contrat_accord; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.contrat_accord (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    condition_paiement character varying(255),
    date_debut_contrat_accord timestamp(6) without time zone,
    date_fin_contrat_accord timestamp(6) without time zone,
    description_contrat_accord character varying(255),
    fournisseur_id uuid,
    libelle_contrat_accord character varying(255),
    niveau_service character varying(255),
    terme_condition_contrat_accord character varying(255),
    type_contrat_accord character varying(255)
);

--
-- Name: crictere_evaluation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.crictere_evaluation (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    commentaire_evaluation character varying(255),
    delais_livraison character varying(255),
    description_crictere_evaluation character varying(255),
    libelle_crictere_evaluation character varying(255),
    note_atribuer_critere character varying(255),
    service_client character varying(255),
    fournisseur_id uuid
);

--
-- Name: demande; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.demande (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    date_creation_demande timestamp(6) without time zone,
    date_modification_demande timestamp(6) without time zone,
    description_demande character varying(255),
    libelle_demande character varying(255),
    statut_demande character varying(255)
);

--
-- Name: departement; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.departement (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    description_departement character varying(255),
    libelle_departement character varying(255)
);

--
-- Name: domaines_application; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.domaines_application (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    description character varying(255),
    libelle character varying(255) NOT NULL,
    ordre integer
);

--
-- Name: exigence; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.exigence (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    date_echeance_exigence character varying(255),
    description_exigence character varying(255),
    libelle_exigence character varying(255),
    statut character varying(255)
);

--
-- Name: exigence_formations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.exigence_formations (
    exigence_id uuid NOT NULL,
    formations_id uuid NOT NULL
);

--
-- Name: exigence_reglementations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.exigence_reglementations (
    exigence_id uuid NOT NULL,
    reglementations_id uuid NOT NULL
);

--
-- Name: formation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.formation (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    description_formation character varying(255),
    libelle_formation character varying(255),
    statut character varying(255),
    CONSTRAINT formation_statut_check CHECK (((statut)::text = ANY ((ARRAY['ACTIF'::character varying, 'INACTIF'::character varying, 'NON_TRAITER'::character varying, 'EN_VERIFICATION'::character varying, 'EFFICACITE_A_MESURER'::character varying, 'TRAITER'::character varying, 'REJECTED'::character varying])::text[])))
);

--
-- Name: fournisseur; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fournisseur (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    adresse character varying(255),
    contact_principal character varying(255),
    email character varying(255),
    nom character varying(255),
    site_web character varying(255),
    statut character varying(255),
    telephone character varying(255)
);

--
-- Name: niveau_confidentialite_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.niveau_confidentialite_roles (
    niveau_id uuid NOT NULL,
    role_nom character varying(255)
);

--
-- Name: niveaux_confidentialite; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.niveaux_confidentialite (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    description character varying(255),
    libelle character varying(255) NOT NULL,
    ordre integer
);

--
-- Name: parametres; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.parametres (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    cle character varying(80) NOT NULL,
    description character varying(255),
    libelle character varying(255) NOT NULL,
    lisible_sans_habilitation boolean NOT NULL,
    type character varying(20) NOT NULL,
    valeur character varying(2000),
    CONSTRAINT parametres_type_check CHECK (((type)::text = ANY ((ARRAY['TEXTE'::character varying, 'COURRIEL'::character varying, 'TELEPHONE'::character varying, 'URL'::character varying, 'IMAGE'::character varying, 'NOMBRE'::character varying, 'ADRESSE'::character varying])::text[])))
);

--
-- Name: prestataire; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.prestataire (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    adresse_prestataire character varying(255),
    contact_principal_prestataire character varying(255),
    email_prestataire character varying(255),
    nom_prestataire character varying(255),
    site_web_prestataire character varying(255),
    statut_prestataire character varying(255),
    telephone_prestataire character varying(255)
);

--
-- Name: priorites_document; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.priorites_document (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    couleur character varying(255),
    description character varying(255),
    libelle character varying(255) NOT NULL,
    ordre integer
);

--
-- Name: produit; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.produit (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    description_produit character varying(255),
    libelle_produit character varying(255)
);

--
-- Name: reglementation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reglementation (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    description_reglementation character varying(255),
    nom_reglementation character varying(255),
    organisme_reglementation character varying(255)
);

--
-- Name: reglementation_exigences; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reglementation_exigences (
    reglementation_id uuid NOT NULL,
    exigences_id uuid NOT NULL
);

--
-- Name: reglementation_suivi_audit_inspections; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reglementation_suivi_audit_inspections (
    reglementation_id uuid NOT NULL,
    suivi_audit_inspections_id uuid NOT NULL
);

--
-- Name: structures; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.structures (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    adresse character varying(255),
    autorite_signataire character varying(255),
    email character varying(255),
    email_struct character varying(255),
    libelle_court character varying(255),
    libelle_long character varying(255),
    licence_active boolean,
    region character varying(255),
    responsable character varying(255),
    telephone character varying(255),
    titre_autorite_signataire character varying(255),
    titre_honorifique_signataire character varying(255),
    titre_signataire character varying(255),
    type_structure character varying(255),
    ville character varying(255),
    parent_direction_id uuid,
    type_processus_id uuid,
    CONSTRAINT structures_type_structure_check CHECK (((type_structure)::text = ANY ((ARRAY['DIRECTION'::character varying, 'SERVICE'::character varying])::text[])))
);

--
-- Name: suivi_audit_inspection; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.suivi_audit_inspection (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    action_recommender character varying(255),
    resultat_suivi_audit_inspection character varying(255),
    statut_suivi_audit_inspection character varying(255)
);

--
-- Name: suivi_audit_inspection_reglementations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.suivi_audit_inspection_reglementations (
    suivi_audit_inspection_id uuid NOT NULL,
    reglementations_id uuid NOT NULL
);

--
-- Name: type_processus; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.type_processus (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    description character varying(255),
    libelle character varying(255)
);

--
-- Name: abonnements_directions abonnements_directions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.abonnements_directions
    ADD CONSTRAINT abonnements_directions_pkey PRIMARY KEY (id);

--
-- Name: archivage archivage_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.archivage
    ADD CONSTRAINT archivage_pkey PRIMARY KEY (id);

--
-- Name: categorie_fichier categorie_fichier_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categorie_fichier
    ADD CONSTRAINT categorie_fichier_pkey PRIMARY KEY (id);

--
-- Name: config_global config_global_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.config_global
    ADD CONSTRAINT config_global_pkey PRIMARY KEY (id);

--
-- Name: contrat_accord contrat_accord_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contrat_accord
    ADD CONSTRAINT contrat_accord_pkey PRIMARY KEY (id);

--
-- Name: crictere_evaluation crictere_evaluation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crictere_evaluation
    ADD CONSTRAINT crictere_evaluation_pkey PRIMARY KEY (id);

--
-- Name: demande demande_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.demande
    ADD CONSTRAINT demande_pkey PRIMARY KEY (id);

--
-- Name: departement departement_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.departement
    ADD CONSTRAINT departement_pkey PRIMARY KEY (id);

--
-- Name: domaines_application domaines_application_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.domaines_application
    ADD CONSTRAINT domaines_application_pkey PRIMARY KEY (id);

--
-- Name: exigence exigence_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exigence
    ADD CONSTRAINT exigence_pkey PRIMARY KEY (id);

--
-- Name: formation formation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.formation
    ADD CONSTRAINT formation_pkey PRIMARY KEY (id);

--
-- Name: fournisseur fournisseur_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fournisseur
    ADD CONSTRAINT fournisseur_pkey PRIMARY KEY (id);

--
-- Name: niveaux_confidentialite niveaux_confidentialite_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.niveaux_confidentialite
    ADD CONSTRAINT niveaux_confidentialite_pkey PRIMARY KEY (id);

--
-- Name: parametres parametres_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.parametres
    ADD CONSTRAINT parametres_pkey PRIMARY KEY (id);

--
-- Name: prestataire prestataire_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.prestataire
    ADD CONSTRAINT prestataire_pkey PRIMARY KEY (id);

--
-- Name: priorites_document priorites_document_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.priorites_document
    ADD CONSTRAINT priorites_document_pkey PRIMARY KEY (id);

--
-- Name: produit produit_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.produit
    ADD CONSTRAINT produit_pkey PRIMARY KEY (id);

--
-- Name: reglementation reglementation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reglementation
    ADD CONSTRAINT reglementation_pkey PRIMARY KEY (id);

--
-- Name: structures structures_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.structures
    ADD CONSTRAINT structures_pkey PRIMARY KEY (id);

--
-- Name: suivi_audit_inspection suivi_audit_inspection_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.suivi_audit_inspection
    ADD CONSTRAINT suivi_audit_inspection_pkey PRIMARY KEY (id);

--
-- Name: type_processus type_processus_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.type_processus
    ADD CONSTRAINT type_processus_pkey PRIMARY KEY (id);

--
-- Name: domaines_application uk_domaine_libelle; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.domaines_application
    ADD CONSTRAINT uk_domaine_libelle UNIQUE (libelle);

--
-- Name: niveaux_confidentialite uk_niveau_confidentialite_libelle; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.niveaux_confidentialite
    ADD CONSTRAINT uk_niveau_confidentialite_libelle UNIQUE (libelle);

--
-- Name: parametres uk_parametre_cle; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.parametres
    ADD CONSTRAINT uk_parametre_cle UNIQUE (cle);

--
-- Name: priorites_document uk_priorite_libelle; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.priorites_document
    ADD CONSTRAINT uk_priorite_libelle UNIQUE (libelle);

--
-- Name: abonnements_directions ukbgqrsftxqq64dfkw612ypxrxu; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.abonnements_directions
    ADD CONSTRAINT ukbgqrsftxqq64dfkw612ypxrxu UNIQUE (subscribed_direction_id);

--
-- Name: suivi_audit_inspection_reglementations fk2snn1erea30tfp0riha9s6uvo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.suivi_audit_inspection_reglementations
    ADD CONSTRAINT fk2snn1erea30tfp0riha9s6uvo FOREIGN KEY (reglementations_id) REFERENCES public.reglementation(id);

--
-- Name: exigence_formations fk40m6l49g06v9yii4tld35r3bo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exigence_formations
    ADD CONSTRAINT fk40m6l49g06v9yii4tld35r3bo FOREIGN KEY (formations_id) REFERENCES public.formation(id);

--
-- Name: niveau_confidentialite_roles fk8og4a01r7rvsi51ufdfy4s95e; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.niveau_confidentialite_roles
    ADD CONSTRAINT fk8og4a01r7rvsi51ufdfy4s95e FOREIGN KEY (niveau_id) REFERENCES public.niveaux_confidentialite(id);

--
-- Name: structures fk9ndrgr88nh58fffhpedtu34ne; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.structures
    ADD CONSTRAINT fk9ndrgr88nh58fffhpedtu34ne FOREIGN KEY (type_processus_id) REFERENCES public.type_processus(id);

--
-- Name: reglementation_suivi_audit_inspections fkfogk87n4fkr0en3dgreqfooy2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reglementation_suivi_audit_inspections
    ADD CONSTRAINT fkfogk87n4fkr0en3dgreqfooy2 FOREIGN KEY (reglementation_id) REFERENCES public.reglementation(id);

--
-- Name: reglementation_suivi_audit_inspections fkh29v012lsjuy4pj5rp2f0tkmq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reglementation_suivi_audit_inspections
    ADD CONSTRAINT fkh29v012lsjuy4pj5rp2f0tkmq FOREIGN KEY (suivi_audit_inspections_id) REFERENCES public.suivi_audit_inspection(id);

--
-- Name: suivi_audit_inspection_reglementations fki36vu8w89uq6oeohjud8tc285; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.suivi_audit_inspection_reglementations
    ADD CONSTRAINT fki36vu8w89uq6oeohjud8tc285 FOREIGN KEY (suivi_audit_inspection_id) REFERENCES public.suivi_audit_inspection(id);

--
-- Name: structures fkiuwd5pte20703ofup0e43nd4e; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.structures
    ADD CONSTRAINT fkiuwd5pte20703ofup0e43nd4e FOREIGN KEY (parent_direction_id) REFERENCES public.structures(id);

--
-- Name: exigence_reglementations fkkgg57x90nbtwnx0mx5b1240kc; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exigence_reglementations
    ADD CONSTRAINT fkkgg57x90nbtwnx0mx5b1240kc FOREIGN KEY (reglementations_id) REFERENCES public.reglementation(id);

--
-- Name: reglementation_exigences fkkib5ijfdp69nea1it5i2p4d1b; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reglementation_exigences
    ADD CONSTRAINT fkkib5ijfdp69nea1it5i2p4d1b FOREIGN KEY (exigences_id) REFERENCES public.exigence(id);

--
-- Name: crictere_evaluation fklkj6q76vpye8tr2u93biwvack; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.crictere_evaluation
    ADD CONSTRAINT fklkj6q76vpye8tr2u93biwvack FOREIGN KEY (fournisseur_id) REFERENCES public.fournisseur(id);

--
-- Name: reglementation_exigences fkn82k8si5y05tr7u85vgb2rc6a; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reglementation_exigences
    ADD CONSTRAINT fkn82k8si5y05tr7u85vgb2rc6a FOREIGN KEY (reglementation_id) REFERENCES public.reglementation(id);

--
-- Name: exigence_formations fkqe1qiag15n875maq3bjmwroyp; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exigence_formations
    ADD CONSTRAINT fkqe1qiag15n875maq3bjmwroyp FOREIGN KEY (exigence_id) REFERENCES public.exigence(id);

--
-- Name: abonnements_directions fksh8qysk84djvnyi714h7mcm8l; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.abonnements_directions
    ADD CONSTRAINT fksh8qysk84djvnyi714h7mcm8l FOREIGN KEY (subscribed_direction_id) REFERENCES public.structures(id);

--
-- Name: exigence_reglementations fktmstyua3pfq2glc389xsb225c; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exigence_reglementations
    ADD CONSTRAINT fktmstyua3pfq2glc389xsb225c FOREIGN KEY (exigence_id) REFERENCES public.exigence(id);

--
--
