--
-- evaluation-service — ce qu'un niveau RQAP-BF retient : un écart transmis au module
-- Non-conformités, ou non. Une ligne par direction et par niveau paramétrable (Non conforme,
-- À améliorer, Acceptable). Les lignes se posent à la première lecture de l'écran, avec la
-- règle par défaut (Non conforme et À améliorer ouvrent une non-conformité) : rien à recopier ici.
--
CREATE TABLE public.regle_ecart_rqapbf (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    niveau character varying(32) NOT NULL,
    ouvre_non_conformite boolean NOT NULL,

    CONSTRAINT pk_regle_ecart_rqapbf PRIMARY KEY (id),
    CONSTRAINT uk_regle_ecart_rqapbf UNIQUE (direction_id, niveau)
);
