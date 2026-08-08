--
-- amelioration-service — schéma initial, relevé du schéma en service au 7 août 2026.
--
-- Non-conformités, plans d'action et leurs pièces jointes.
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
-- Name: action; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.action (
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
-- Name: action_corrective_preventive; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.action_corrective_preventive (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    date_debut character varying(255),
    date_fin character varying(255),
    description_action_corrective_preventive text,
    libelle character varying(255),
    responsable character varying(255),
    statut character varying(255),
    type character varying(255),
    CONSTRAINT action_corrective_preventive_statut_check CHECK (((statut)::text = ANY ((ARRAY['ACTIF'::character varying, 'INACTIF'::character varying, 'NON_TRAITER'::character varying, 'EN_VERIFICATION'::character varying, 'EFFICACITE_A_MESURER'::character varying, 'TRAITER'::character varying, 'REJECTED'::character varying])::text[])))
);

--
-- Name: action_exisgence; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.action_exisgence (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    exigence_id uuid,
    action_id uuid
);

--
-- Name: action_risque; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.action_risque (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    action_id uuid,
    risque_id uuid
);

--
-- Name: audite; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audite (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    description_audite character varying(255),
    founisseur_id uuid,
    libelle_audite character varying(255),
    objectif_audite character varying(255),
    resultat_audite character varying(255),
    statut_audite character varying(255),
    type_audite character varying(255)
);

--
-- Name: efficacite; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.efficacite (
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
-- Name: evaluation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.evaluation (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    date_evaluation timestamp(6) without time zone,
    description character varying(255),
    fournisseur_id uuid,
    libelle character varying(255),
    type_evaluation character varying(255)
);

--
-- Name: evaluation_action_corrective_preventive_reconmenders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.evaluation_action_corrective_preventive_reconmenders (
    evaluation_id uuid NOT NULL,
    action_corrective_preventive_reconmenders_id uuid NOT NULL
);

--
-- Name: niveau_non_conformite; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.niveau_non_conformite (
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
-- Name: non_conformite_participants; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.non_conformite_participants (
    non_conformite_id uuid NOT NULL,
    participants character varying(255)
);

--
-- Name: piece_jointe; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.piece_jointe (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    depose_par_circuit boolean DEFAULT false NOT NULL,
    entity_id uuid,
    ext character varying(255),
    nom character varying(255),
    type character varying(255),
    url character varying(255),
    zip_file boolean NOT NULL
);

--
-- Name: plan_action; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.plan_action (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    action_corrective text,
    cause_identifiees text,
    constat_efficacite text,
    critere_efficacite text,
    date_echeance date,
    date_rejet date,
    date_traitement date,
    non_conforme_id uuid,
    numero_nc character varying(255),
    numero_odre character varying(255),
    numero_telephone character varying(255),
    observation text,
    observation_rejet text,
    proc_emetteur character varying(255),
    responsable_email character varying(255),
    responsable_id uuid,
    responsable_nom_complet character varying(255),
    solution_retenues text,
    status character varying(255),
    workflow_id uuid,
    workflow_status character varying(255),
    CONSTRAINT plan_action_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIF'::character varying, 'INACTIF'::character varying, 'NON_TRAITER'::character varying, 'EN_VERIFICATION'::character varying, 'EFFICACITE_A_MESURER'::character varying, 'TRAITER'::character varying, 'REJECTED'::character varying])::text[])))
);

--
-- Name: quali_nc; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quali_nc (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    action_dsc text,
    action_id uuid,
    action_libelle character varying(255),
    action_preventive text,
    archivage_date timestamp(6) without time zone,
    circuit character varying(255),
    date_cloture_rq character varying(255),
    date_observations_rq character varying(255),
    date_suivi timestamp(6) without time zone,
    date_verification character varying(255),
    date_visa_emetteur timestamp(6) without time zone,
    delais_mise_oeuvre character varying(255),
    disposition_preventives text,
    efficacite_id uuid,
    efficacite_libelle character varying(255),
    etat_traitement character varying(255),
    fonction_emetteur character varying(255),
    justification text,
    justification_pilote text,
    justification_rs text,
    niveau_non_conformite_id uuid,
    niveau_non_conformite_libelle character varying(255),
    nom_processus character varying(255),
    numero_fdac character varying(255),
    numero_reference character varying(255),
    observation_rejet text,
    observations_cloture text,
    observations_rq text,
    origin_non_conformite_id character varying(255),
    origin_non_conformite_libelle character varying(255),
    origine_id character varying(255),
    origine_service character varying(255),
    origine_service_libelle_court character varying(255),
    pertinance_pilote character varying(255),
    pertinance_rs character varying(255),
    pertinance_rs_suivi character varying(255),
    publication_date timestamp(6) without time zone,
    status character varying(255),
    structure_responsable_id character varying(255),
    structure_responsable_libelle character varying(255),
    structure_responsable_sigle character varying(255),
    structure_soumission_id character varying(255),
    structure_soumission_libelle character varying(255),
    type_demande character varying(255),
    type_non_conformite_id uuid,
    type_non_conformite_libelle character varying(255),
    type_processus_id uuid,
    type_processus_libelle character varying(255),
    user_imput_full_name character varying(255),
    user_imput_id character varying(255),
    user_impute_email character varying(255),
    version character varying(255),
    workflow_id uuid,
    workflow_status character varying(255),
    doc_rejet_id uuid,
    CONSTRAINT quali_nc_circuit_check CHECK (((circuit)::text = ANY ((ARRAY['A'::character varying, 'B'::character varying])::text[]))),
    CONSTRAINT quali_nc_etat_traitement_check CHECK (((etat_traitement)::text = ANY ((ARRAY['SOUMISSION'::character varying, 'RECEPTION'::character varying, 'VALIDATION_RQ'::character varying, 'IMPUTATION'::character varying, 'TRAITEMENT'::character varying, 'VALIDATION'::character varying, 'VALIDATION_RS'::character varying, 'SUIVI_RQ'::character varying, 'CLOTURE'::character varying])::text[]))),
    CONSTRAINT quali_nc_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'IN_PROGRESS'::character varying, 'ARCHIVED'::character varying, 'PUBLISHED'::character varying, 'DRAFT'::character varying])::text[]))),
    CONSTRAINT quali_nc_type_demande_check CHECK (((type_demande)::text = 'NON_CONFORMITE'::text))
);

--
-- Name: quali_nc_fichiers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quali_nc_fichiers (
    non_conformite_id uuid NOT NULL,
    fichiers_id uuid NOT NULL
);

--
-- Name: quali_nc_plan_actions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quali_nc_plan_actions (
    non_conformite_id uuid NOT NULL,
    plan_actions_id uuid NOT NULL
);

--
-- Name: reclamation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reclamation (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    date_reclamation character varying(255),
    nom_demendeur character varying(255),
    numero_reference character varying(255)
);

--
-- Name: reclamation_action_corrective_preventives; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reclamation_action_corrective_preventives (
    reclamation_id uuid NOT NULL,
    action_corrective_preventives_id uuid NOT NULL
);

--
-- Name: risque; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.risque (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    commentaire_risque character varying(255),
    description character varying(255),
    evidence_risque character varying(255),
    libelle character varying(255),
    niveau character varying(255),
    plant_attenuation character varying(255),
    statut smallint,
    CONSTRAINT risque_statut_check CHECK (((statut >= 0) AND (statut <= 2)))
);

--
-- Name: type_non_conformite; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.type_non_conformite (
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
-- Name: action_corrective_preventive action_corrective_preventive_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.action_corrective_preventive
    ADD CONSTRAINT action_corrective_preventive_pkey PRIMARY KEY (id);

--
-- Name: action_exisgence action_exisgence_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.action_exisgence
    ADD CONSTRAINT action_exisgence_pkey PRIMARY KEY (id);

--
-- Name: action action_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.action
    ADD CONSTRAINT action_pkey PRIMARY KEY (id);

--
-- Name: action_risque action_risque_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.action_risque
    ADD CONSTRAINT action_risque_pkey PRIMARY KEY (id);

--
-- Name: audite audite_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audite
    ADD CONSTRAINT audite_pkey PRIMARY KEY (id);

--
-- Name: efficacite efficacite_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.efficacite
    ADD CONSTRAINT efficacite_pkey PRIMARY KEY (id);

--
-- Name: evaluation evaluation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluation
    ADD CONSTRAINT evaluation_pkey PRIMARY KEY (id);

--
-- Name: niveau_non_conformite niveau_non_conformite_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.niveau_non_conformite
    ADD CONSTRAINT niveau_non_conformite_pkey PRIMARY KEY (id);

--
-- Name: piece_jointe piece_jointe_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.piece_jointe
    ADD CONSTRAINT piece_jointe_pkey PRIMARY KEY (id);

--
-- Name: plan_action plan_action_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.plan_action
    ADD CONSTRAINT plan_action_pkey PRIMARY KEY (id);

--
-- Name: quali_nc quali_nc_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quali_nc
    ADD CONSTRAINT quali_nc_pkey PRIMARY KEY (id);

--
-- Name: reclamation reclamation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reclamation
    ADD CONSTRAINT reclamation_pkey PRIMARY KEY (id);

--
-- Name: risque risque_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.risque
    ADD CONSTRAINT risque_pkey PRIMARY KEY (id);

--
-- Name: type_non_conformite type_non_conformite_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.type_non_conformite
    ADD CONSTRAINT type_non_conformite_pkey PRIMARY KEY (id);

--
-- Name: action_risque uk1gpd87p58tf8pemty4pnr9pmi; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.action_risque
    ADD CONSTRAINT uk1gpd87p58tf8pemty4pnr9pmi UNIQUE (action_id);

--
-- Name: reclamation_action_corrective_preventives ukbda4th1tns14cyewmp4y0785g; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reclamation_action_corrective_preventives
    ADD CONSTRAINT ukbda4th1tns14cyewmp4y0785g UNIQUE (action_corrective_preventives_id);

--
-- Name: action_risque ukfnutdsrf5j9rlu1s1mg9mflf7; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.action_risque
    ADD CONSTRAINT ukfnutdsrf5j9rlu1s1mg9mflf7 UNIQUE (risque_id);

--
-- Name: quali_nc_fichiers ukiqmftsmbyablayral4ll9ili2; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quali_nc_fichiers
    ADD CONSTRAINT ukiqmftsmbyablayral4ll9ili2 UNIQUE (fichiers_id);

--
-- Name: quali_nc_plan_actions ukl734oyb1gl317eiyp35s4rrpp; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quali_nc_plan_actions
    ADD CONSTRAINT ukl734oyb1gl317eiyp35s4rrpp UNIQUE (plan_actions_id);

--
-- Name: action_exisgence ukomcl9uwb3gbdsx0ge0r7mhkty; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.action_exisgence
    ADD CONSTRAINT ukomcl9uwb3gbdsx0ge0r7mhkty UNIQUE (action_id);

--
-- Name: reclamation_action_corrective_preventives fk30kgu6hjucbjnkmdaaw9e9om2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reclamation_action_corrective_preventives
    ADD CONSTRAINT fk30kgu6hjucbjnkmdaaw9e9om2 FOREIGN KEY (action_corrective_preventives_id) REFERENCES public.action_corrective_preventive(id);

--
-- Name: action_risque fk39okfdqia6figi5c3igav6na1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.action_risque
    ADD CONSTRAINT fk39okfdqia6figi5c3igav6na1 FOREIGN KEY (action_id) REFERENCES public.action_corrective_preventive(id);

--
-- Name: quali_nc_plan_actions fk66trs84d730kofu59dktdhlmm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quali_nc_plan_actions
    ADD CONSTRAINT fk66trs84d730kofu59dktdhlmm FOREIGN KEY (non_conformite_id) REFERENCES public.quali_nc(id);

--
-- Name: quali_nc_fichiers fk7weg1t0xwmxf6jhyjdklbtwxw; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quali_nc_fichiers
    ADD CONSTRAINT fk7weg1t0xwmxf6jhyjdklbtwxw FOREIGN KEY (non_conformite_id) REFERENCES public.quali_nc(id);

--
-- Name: quali_nc_fichiers fkbnxpk16uye9797vs3j66j4x8e; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quali_nc_fichiers
    ADD CONSTRAINT fkbnxpk16uye9797vs3j66j4x8e FOREIGN KEY (fichiers_id) REFERENCES public.piece_jointe(id);

--
-- Name: evaluation_action_corrective_preventive_reconmenders fkdq2dim3q16vftvpx9sfkum7km; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluation_action_corrective_preventive_reconmenders
    ADD CONSTRAINT fkdq2dim3q16vftvpx9sfkum7km FOREIGN KEY (action_corrective_preventive_reconmenders_id) REFERENCES public.action_corrective_preventive(id);

--
-- Name: evaluation_action_corrective_preventive_reconmenders fke5xun3u5mokj9g1rm8qd99njm; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.evaluation_action_corrective_preventive_reconmenders
    ADD CONSTRAINT fke5xun3u5mokj9g1rm8qd99njm FOREIGN KEY (evaluation_id) REFERENCES public.evaluation(id);

--
-- Name: quali_nc fkje0tfw51c68d4td856h4771ep; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quali_nc
    ADD CONSTRAINT fkje0tfw51c68d4td856h4771ep FOREIGN KEY (doc_rejet_id) REFERENCES public.piece_jointe(id);

--
-- Name: quali_nc_plan_actions fkl4xadna7csmcmokdywdth2j05; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quali_nc_plan_actions
    ADD CONSTRAINT fkl4xadna7csmcmokdywdth2j05 FOREIGN KEY (plan_actions_id) REFERENCES public.plan_action(id);

--
-- Name: reclamation_action_corrective_preventives fkmh48yrv0wunoxsxxnwb6vwx9l; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reclamation_action_corrective_preventives
    ADD CONSTRAINT fkmh48yrv0wunoxsxxnwb6vwx9l FOREIGN KEY (reclamation_id) REFERENCES public.reclamation(id);

--
-- Name: action_risque fkn245vm1mefpkmwwsqyewh10c; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.action_risque
    ADD CONSTRAINT fkn245vm1mefpkmwwsqyewh10c FOREIGN KEY (risque_id) REFERENCES public.risque(id);

--
-- Name: action_exisgence fkoh5xpnq1ewnm4p2yisvalp2ch; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.action_exisgence
    ADD CONSTRAINT fkoh5xpnq1ewnm4p2yisvalp2ch FOREIGN KEY (action_id) REFERENCES public.action_corrective_preventive(id);

--
-- Name: non_conformite_participants fkqsqdb31y9s2i2dp6n0021c5al; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.non_conformite_participants
    ADD CONSTRAINT fkqsqdb31y9s2i2dp6n0021c5al FOREIGN KEY (non_conformite_id) REFERENCES public.quali_nc(id);

--
--
