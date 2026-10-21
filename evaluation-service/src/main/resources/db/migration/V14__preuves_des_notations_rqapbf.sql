--
-- evaluation-service — un critère RQAP-BF noté porte ses pièces justificatives.
--
-- Les éléments de preuve ne se saisissaient qu'en texte (recette du 17 oct. 2026, observation 31).
-- Comme pour les constats : le fichier vit sur le serveur de fichiers, sa désignation ici.
--
CREATE TABLE public.preuve_notation (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    notation_id uuid NOT NULL,
    nom character varying(255),
    ext character varying(32),
    type character varying(150),
    url text,

    CONSTRAINT pk_preuve_notation PRIMARY KEY (id),
    CONSTRAINT fk_preuve_notation FOREIGN KEY (notation_id) REFERENCES public.notation_critere (id) ON DELETE CASCADE
);
CREATE INDEX idx_preuve_notation ON public.preuve_notation (notation_id);
