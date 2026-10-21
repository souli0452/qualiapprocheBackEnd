--
-- evaluation-service — V1 : le module d'audit (M13), d'après le diagramme de classes v7.
--
-- Écarts délibérés au diagramme, et pourquoi :
--   * les identifiants sont des UUID, comme partout dans le dépôt, et non des Long ;
--   * Processus et NonConformite sont « external » : ils vivent dans referentiel-service et
--     amelioration-service, et ne sont donc ici que des identifiants, sans clé étrangère ;
--   * l'Auditeur garde son identifiant Keycloak dans utilisateur_id plutôt que d'en faire sa clé ;
--   * les statuts que le diagramme type en String sont des énumérations : une faute de frappe
--     dans un statut ne doit pas créer un état que rien ne sait traiter.
--
-- Chaque table porte les colonnes de traçabilité communes (AuditEntity) et la direction qui la
-- possède : tout est borné à l'organisation de l'appelant.
--
-- NE JAMAIS MODIFIER un script déjà appliqué : Flyway en vérifie l'empreinte.
--

-- Référentiels paramétrables

CREATE TABLE public.type_audit (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    libelle character varying(255) NOT NULL,
    circuit_externe boolean DEFAULT false NOT NULL,
    -- L'origine des écarts dans le module Non-conformités (type_non_conformite d'amelioration-service).
    type_non_conformite_id uuid,

    CONSTRAINT pk_type_audit PRIMARY KEY (id)
);

CREATE TABLE public.site (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    nom character varying(255) NOT NULL,
    adresse character varying(255),

    CONSTRAINT pk_site PRIMARY KEY (id)
);

CREATE TABLE public.type_constat (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    libelle character varying(255) NOT NULL,
    couleur character varying(32),
    genere_non_conformite boolean DEFAULT false NOT NULL,
    -- La gravité dans le module Non-conformités (niveau_non_conformite d'amelioration-service).
    niveau_non_conformite_id uuid,

    CONSTRAINT pk_type_constat PRIMARY KEY (id)
);

CREATE TABLE public.critere_evaluation_auditeur (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    libelle character varying(255) NOT NULL,

    CONSTRAINT pk_critere_evaluation_auditeur PRIMARY KEY (id)
);

CREATE TABLE public.niveau_evaluation_auditeur (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    libelle character varying(255) NOT NULL,
    ordre integer NOT NULL,

    CONSTRAINT pk_niveau_evaluation_auditeur PRIMARY KEY (id)
);

CREATE TABLE public.regle_notification (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    libelle character varying(255) NOT NULL,
    delai_jours integer NOT NULL,
    destinataires text,
    actif boolean DEFAULT true NOT NULL,

    CONSTRAINT pk_regle_notification PRIMARY KEY (id)
);

CREATE TABLE public.codification_document (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    type_document character varying(50) NOT NULL,
    prefixe character varying(20) NOT NULL,
    separateur character varying(5) NOT NULL,
    annee character varying(4),
    compteur integer DEFAULT 0 NOT NULL,

    CONSTRAINT pk_codification_document PRIMARY KEY (id),
    CONSTRAINT uk_codification_type UNIQUE (direction_id, type_document)
);


-- Auditeurs

CREATE TABLE public.auditeur (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    utilisateur_id character varying(255) NOT NULL,
    disponibilite character varying(255),
    structure_appartenance uuid,
    statut character varying(20) NOT NULL,
    niveau_habilitation character varying(255),

    CONSTRAINT pk_auditeur PRIMARY KEY (id),
    CONSTRAINT uk_auditeur_utilisateur UNIQUE (direction_id, utilisateur_id)
);

CREATE TABLE public.auditeur_domaine_habilite (
    auditeur_id uuid NOT NULL,
    valeur character varying(255) NOT NULL,
    CONSTRAINT fk_auditeur_domaine_habilite FOREIGN KEY (auditeur_id) REFERENCES public.auditeur (id) ON DELETE CASCADE
);
CREATE INDEX idx_auditeur_domaine_habilite ON public.auditeur_domaine_habilite (auditeur_id);

CREATE TABLE public.auditeur_certification (
    auditeur_id uuid NOT NULL,
    valeur character varying(255) NOT NULL,
    CONSTRAINT fk_auditeur_certification FOREIGN KEY (auditeur_id) REFERENCES public.auditeur (id) ON DELETE CASCADE
);
CREATE INDEX idx_auditeur_certification ON public.auditeur_certification (auditeur_id);

CREATE TABLE public.auditeur_processus_gere (
    auditeur_id uuid NOT NULL,
    valeur character varying(255) NOT NULL,
    CONSTRAINT fk_auditeur_processus_gere FOREIGN KEY (auditeur_id) REFERENCES public.auditeur (id) ON DELETE CASCADE
);
CREATE INDEX idx_auditeur_processus_gere ON public.auditeur_processus_gere (auditeur_id);


-- Checklists

CREATE TABLE public.checklist_audit (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    nom character varying(255) NOT NULL,
    chapitre_iso character varying(50),
    statut character varying(20) NOT NULL,

    CONSTRAINT pk_checklist_audit PRIMARY KEY (id)
);

CREATE TABLE public.point_controle (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    checklist_id uuid NOT NULL,
    chapitre_iso character varying(50),
    libelle character varying(255) NOT NULL,
    question text,
    preuve_attendue text,
    auditeur_assigne_id uuid,

    CONSTRAINT pk_point_controle PRIMARY KEY (id),
    CONSTRAINT fk_point_controle_checklist FOREIGN KEY (checklist_id)
        REFERENCES public.checklist_audit (id) ON DELETE CASCADE,
    CONSTRAINT fk_point_controle_auditeur FOREIGN KEY (auditeur_assigne_id)
        REFERENCES public.auditeur (id)
);


-- Audits

CREATE TABLE public.audit (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    reference character varying(60) NOT NULL,
    objectifs_audit text,
    portee_audit text,
    criteres_audit text,
    date_debut_prevue date,
    date_fin_prevue date,
    duree_estimee_jours integer,
    niveau_risque character varying(50),
    description_risque text,
    statut character varying(20) NOT NULL,
    origine_rapport character varying(20),
    fichier_rapport text,
    type_audit_id uuid,
    processus_id uuid,
    responsable_equipe_id uuid,
    reference_rapport character varying(60),
    conclusions_rapport text,
    recommandations_rapport text,
    organisme_externe character varying(255),

    CONSTRAINT pk_audit PRIMARY KEY (id),
    CONSTRAINT uk_audit_reference UNIQUE (direction_id, reference),
    CONSTRAINT fk_audit_type FOREIGN KEY (type_audit_id) REFERENCES public.type_audit (id),
    CONSTRAINT fk_audit_responsable FOREIGN KEY (responsable_equipe_id) REFERENCES public.auditeur (id)
);

CREATE INDEX idx_audit_direction_statut ON public.audit (direction_id, statut, date_debut_prevue);

CREATE TABLE public.audit_site (
    audit_id uuid NOT NULL,
    site_id uuid NOT NULL,
    CONSTRAINT pk_audit_site PRIMARY KEY (audit_id, site_id),
    CONSTRAINT fk_audit_site_audit FOREIGN KEY (audit_id) REFERENCES public.audit (id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_site_cible FOREIGN KEY (site_id) REFERENCES public.site (id)
);

CREATE TABLE public.audit_membre_equipe (
    audit_id uuid NOT NULL,
    auditeur_id uuid NOT NULL,
    CONSTRAINT pk_audit_membre_equipe PRIMARY KEY (audit_id, auditeur_id),
    CONSTRAINT fk_audit_membre_equipe_audit FOREIGN KEY (audit_id) REFERENCES public.audit (id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_membre_equipe_cible FOREIGN KEY (auditeur_id) REFERENCES public.auditeur (id)
);

CREATE TABLE public.audit_checklist (
    audit_id uuid NOT NULL,
    checklist_id uuid NOT NULL,
    CONSTRAINT pk_audit_checklist PRIMARY KEY (audit_id, checklist_id),
    CONSTRAINT fk_audit_checklist_audit FOREIGN KEY (audit_id) REFERENCES public.audit (id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_checklist_cible FOREIGN KEY (checklist_id) REFERENCES public.checklist_audit (id)
);

CREATE TABLE public.audit_referentiel (
    audit_id uuid NOT NULL,
    valeur character varying(255) NOT NULL,
    CONSTRAINT fk_audit_referentiel FOREIGN KEY (audit_id) REFERENCES public.audit (id) ON DELETE CASCADE
);

CREATE TABLE public.action_maitrise_risque (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    audit_id uuid NOT NULL,
    libelle text NOT NULL,
    responsable character varying(255),
    delai date,
    efficacite character varying(30) NOT NULL,

    CONSTRAINT pk_action_maitrise_risque PRIMARY KEY (id),
    CONSTRAINT fk_action_risque_audit FOREIGN KEY (audit_id) REFERENCES public.audit (id) ON DELETE CASCADE
);


-- Plan d'audit

CREATE TABLE public.plan_audit (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    audit_id uuid NOT NULL,
    reference character varying(60),
    objectifs text,
    portee text,
    criteres text,
    lieu character varying(255),
    langue character varying(50),
    moyens_necessaires text,
    origine character varying(20) NOT NULL,
    fichier_recu text,
    statut character varying(20) NOT NULL,
    date_partage timestamp(6) without time zone,

    CONSTRAINT pk_plan_audit PRIMARY KEY (id),
    CONSTRAINT uk_plan_audit_audit UNIQUE (audit_id),
    CONSTRAINT fk_plan_audit_audit FOREIGN KEY (audit_id) REFERENCES public.audit (id) ON DELETE CASCADE
);

CREATE TABLE public.plan_audit_methode (
    plan_audit_id uuid NOT NULL,
    valeur character varying(255) NOT NULL,
    CONSTRAINT fk_plan_audit_methode FOREIGN KEY (plan_audit_id) REFERENCES public.plan_audit (id) ON DELETE CASCADE
);

CREATE TABLE public.activite_plan (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    plan_audit_id uuid NOT NULL,
    horaire_debut character varying(30),
    horaire_fin character varying(30),
    libelle_activite text NOT NULL,
    processus_concerne character varying(255),
    auditeur character varying(255),
    personnes_auditees text,

    CONSTRAINT pk_activite_plan PRIMARY KEY (id),
    CONSTRAINT fk_activite_plan FOREIGN KEY (plan_audit_id) REFERENCES public.plan_audit (id) ON DELETE CASCADE
);


-- Constats

CREATE TABLE public.constat (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    audit_id uuid NOT NULL,
    critere_audit text,
    observation text NOT NULL,
    statut character varying(20) NOT NULL,
    date_publication timestamp(6) without time zone,
    date_saisie timestamp(6) without time zone NOT NULL,
    commentaire_equipe text,
    type_constat_id uuid,
    checklist_id uuid,
    point_controle_id uuid,
    redacteur_id uuid NOT NULL,
    non_conformite_id uuid,

    CONSTRAINT pk_constat PRIMARY KEY (id),
    CONSTRAINT fk_constat_audit FOREIGN KEY (audit_id) REFERENCES public.audit (id) ON DELETE CASCADE,
    CONSTRAINT fk_constat_nature FOREIGN KEY (type_constat_id) REFERENCES public.type_constat (id),
    CONSTRAINT fk_constat_checklist FOREIGN KEY (checklist_id) REFERENCES public.checklist_audit (id),
    CONSTRAINT fk_constat_point FOREIGN KEY (point_controle_id) REFERENCES public.point_controle (id),
    CONSTRAINT fk_constat_redacteur FOREIGN KEY (redacteur_id) REFERENCES public.auditeur (id)
);

CREATE INDEX idx_constat_audit_statut ON public.constat (audit_id, statut);


-- Évaluation des auditeurs

CREATE TABLE public.evaluation_auditeur (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    audit_id uuid NOT NULL,
    auditeur_id uuid NOT NULL,
    critere_id uuid NOT NULL,
    niveau_id uuid NOT NULL,
    date_evaluation date NOT NULL,
    commentaire text,
    statut character varying(20) NOT NULL,

    CONSTRAINT pk_evaluation_auditeur PRIMARY KEY (id),
    CONSTRAINT uk_evaluation_auditeur UNIQUE (audit_id, auditeur_id, critere_id),
    CONSTRAINT fk_eval_auditeur_audit FOREIGN KEY (audit_id) REFERENCES public.audit (id) ON DELETE CASCADE,
    CONSTRAINT fk_eval_auditeur_auditeur FOREIGN KEY (auditeur_id) REFERENCES public.auditeur (id),
    CONSTRAINT fk_eval_auditeur_critere FOREIGN KEY (critere_id) REFERENCES public.critere_evaluation_auditeur (id),
    CONSTRAINT fk_eval_auditeur_niveau FOREIGN KEY (niveau_id) REFERENCES public.niveau_evaluation_auditeur (id)
);


-- Référentiel et évaluation RQAPBF

CREATE TABLE public.noeud_referentiel (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    code character varying(50) NOT NULL,
    libelle text NOT NULL,
    type_noeud character varying(50),
    preuve_attendue text,
    ordre integer DEFAULT 0 NOT NULL,
    parent_id uuid,

    CONSTRAINT pk_noeud_referentiel PRIMARY KEY (id),
    CONSTRAINT uk_noeud_code UNIQUE (direction_id, code),
    CONSTRAINT fk_noeud_parent FOREIGN KEY (parent_id) REFERENCES public.noeud_referentiel (id)
);

CREATE TABLE public.audit_domaine_rqapbf (
    audit_id uuid NOT NULL,
    noeud_id uuid NOT NULL,
    CONSTRAINT pk_audit_domaine_rqapbf PRIMARY KEY (audit_id, noeud_id),
    CONSTRAINT fk_audit_domaine_rqapbf_audit FOREIGN KEY (audit_id) REFERENCES public.audit (id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_domaine_rqapbf_noeud FOREIGN KEY (noeud_id) REFERENCES public.noeud_referentiel (id)
);

CREATE TABLE public.evaluation_rqapbf (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    audit_id uuid NOT NULL,
    date_evaluation date NOT NULL,
    score_global double precision,
    niveau_conformite character varying(50),
    statut character varying(20) NOT NULL,

    CONSTRAINT pk_evaluation_rqapbf PRIMARY KEY (id),
    CONSTRAINT fk_evaluation_rqapbf_audit FOREIGN KEY (audit_id) REFERENCES public.audit (id) ON DELETE CASCADE
);

CREATE TABLE public.notation_critere (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    evaluation_id uuid NOT NULL,
    noeud_id uuid NOT NULL,
    niveau character varying(30) NOT NULL,
    score double precision,
    preuve text,
    commentaire text,
    statut character varying(20) DEFAULT 'BROUILLON' NOT NULL,
    non_conformite_id uuid,

    CONSTRAINT pk_notation_critere PRIMARY KEY (id),
    CONSTRAINT uk_notation_critere UNIQUE (evaluation_id, noeud_id),
    CONSTRAINT fk_notation_evaluation FOREIGN KEY (evaluation_id) REFERENCES public.evaluation_rqapbf (id) ON DELETE CASCADE,
    CONSTRAINT fk_notation_noeud FOREIGN KEY (noeud_id) REFERENCES public.noeud_referentiel (id)
);


-- Circuit de signature du rapport

CREATE TABLE public.signature (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    audit_id uuid NOT NULL,
    signataire_id character varying(255) NOT NULL,
    fonction_signataire character varying(255),
    ordre integer NOT NULL,
    statut character varying(20) NOT NULL,
    date_signature timestamp(6) without time zone,
    motif_refus text,

    CONSTRAINT pk_signature PRIMARY KEY (id),
    CONSTRAINT fk_signature_audit FOREIGN KEY (audit_id) REFERENCES public.audit (id) ON DELETE CASCADE
);

CREATE INDEX idx_signature_audit ON public.signature (audit_id, ordre);

-- Rappels d'échéance relevés par la ronde de nuit : les « notifications récentes » du tableau
-- de bord. Hors des colonnes de traçabilité communes : la ronde n'a pas de jeton, la direction
-- est recopiée de l'audit.
CREATE TABLE public.rappel_echeance (
    id uuid NOT NULL,
    direction_id uuid NOT NULL,
    audit_id uuid NOT NULL,
    audit_reference character varying(60) NOT NULL,
    regle_id uuid NOT NULL,
    regle_libelle character varying(255) NOT NULL,
    delai_jours integer NOT NULL,
    destinataires text,
    date_rappel date NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,

    CONSTRAINT pk_rappel_echeance PRIMARY KEY (id),
    CONSTRAINT uk_rappel_echeance UNIQUE (audit_id, regle_id, date_rappel),
    CONSTRAINT fk_rappel_echeance_audit FOREIGN KEY (audit_id) REFERENCES public.audit (id) ON DELETE CASCADE
);
CREATE INDEX idx_rappel_echeance_direction ON public.rappel_echeance (direction_id, created_at DESC);

-- Éléments de preuve joints aux constats. Le fichier vit sur le serveur de fichiers ; seule sa
-- désignation est ici.
CREATE TABLE public.preuve_constat (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    constat_id uuid NOT NULL,
    nom character varying(255),
    ext character varying(32),
    type character varying(150),
    url text,

    CONSTRAINT pk_preuve_constat PRIMARY KEY (id),
    CONSTRAINT fk_preuve_constat FOREIGN KEY (constat_id) REFERENCES public.constat (id) ON DELETE CASCADE
);
CREATE INDEX idx_preuve_constat ON public.preuve_constat (constat_id);
