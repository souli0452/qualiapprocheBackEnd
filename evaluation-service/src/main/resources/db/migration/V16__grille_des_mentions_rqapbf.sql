--
-- evaluation-service — la grille qui traduit le score global RQAP-BF en niveau, mention et étoiles.
--
-- Recette du 17 oct. 2026, observation 28 : le score n'était rendu qu'avec les libellés des niveaux
-- de notation (« À améliorer », « Acceptable »...), sans la mention attendue. Cinq tranches par
-- direction, posées à la première lecture avec la grille par défaut : rien à recopier ici. Seuls
-- leurs bornes et libellés se modifient, en attendant la grille officielle.
--
CREATE TABLE public.tranche_score_rqapbf (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    rang integer NOT NULL,
    score_min double precision NOT NULL,
    niveau character varying(100) NOT NULL,
    mention character varying(255) NOT NULL,
    etoiles integer NOT NULL,

    CONSTRAINT pk_tranche_score_rqapbf PRIMARY KEY (id),
    CONSTRAINT uk_tranche_score_rqapbf UNIQUE (direction_id, rang),
    CONSTRAINT ck_tranche_score_rqapbf_score CHECK (score_min >= 0 AND score_min <= 100),
    CONSTRAINT ck_tranche_score_rqapbf_etoiles CHECK (etoiles >= 0 AND etoiles <= 5)
);
