--
-- referentiel-service — V10 : le paramétrage que le module Audit lit sans le posséder.
--
-- Les sites, les critères et les niveaux d'évaluation des auditeurs vivaient dans
-- evaluation-service ; ils se paramètrent désormais ici, avec le reste de la configuration, et
-- le module Audit les lit par client. Deux différences avec les autres référentiels du service :
--   * ils sont propres à chaque direction (direction_id), et non communs à l'installation ;
--   * ils se retirent (actif) au lieu de s'effacer : evaluation-service en garde les
--     identifiants sans clé étrangère, et une ligne effacée laisserait ses dossiers dans le vide.
--
-- NE JAMAIS MODIFIER un script déjà appliqué : Flyway en vérifie l'empreinte.
--

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
    actif boolean DEFAULT true NOT NULL,

    CONSTRAINT pk_site PRIMARY KEY (id)
);

CREATE INDEX idx_site_direction ON public.site (direction_id);

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
    actif boolean DEFAULT true NOT NULL,

    CONSTRAINT pk_critere_evaluation_auditeur PRIMARY KEY (id)
);

CREATE INDEX idx_critere_eval_auditeur_direction ON public.critere_evaluation_auditeur (direction_id);

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
    actif boolean DEFAULT true NOT NULL,

    CONSTRAINT pk_niveau_evaluation_auditeur PRIMARY KEY (id)
);

CREATE INDEX idx_niveau_eval_auditeur_direction ON public.niveau_evaluation_auditeur (direction_id);
