--
-- referentiel-service — V8 : la foire aux questions de l'application.
--
-- Un référentiel comme les autres : l'organisation y écrit ses réponses, et ses utilisateurs les
-- consultent depuis l'aide. Elle vit donc ici, et non dans ia-service — l'assistant n'en est
-- qu'un lecteur parmi d'autres, et la FAQ doit survivre à une installation qui ne souscrirait
-- jamais au module ASSISTANT_IA.
--
-- Ce que l'assistant en fait : il la reçoit dans sa consigne et y puise quand une question s'en
-- approche, au lieu d'inventer une réponse plausible. C'est un usage de la FAQ, pas sa raison
-- d'être.
--
-- NE JAMAIS MODIFIER un script déjà appliqué : Flyway en vérifie l'empreinte.
--

CREATE TABLE public.faq (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    question text NOT NULL,
    reponse text NOT NULL,

    -- Le regroupement affiché dans l'aide : « Documents », « Non-conformités », « Compte »...
    -- Libre plutôt qu'énuméré : les rubriques d'aide d'une organisation ne se devinent pas.
    categorie character varying(120),

    -- Une entrée se retire sans se perdre : la dépublier n'efface ni son texte ni son auteur.
    -- Un DELETE emporterait une réponse qu'on voulait seulement suspendre le temps d'une revue.
    publiee boolean DEFAULT true NOT NULL,

    -- Ordre d'affichage, et ordre de récitation pour l'assistant : ce qui compte le plus vient
    -- en tête, et le plafond de jetons de l'assistant tranche par la fin.
    rang integer DEFAULT 0 NOT NULL,

    CONSTRAINT pk_faq PRIMARY KEY (id)
);

CREATE INDEX idx_faq_publication ON public.faq (direction_id, publiee, rang);

--
-- Les pièces jointes d'une entrée : facultatives, et dans leur propre table.
--
-- Facultatives parce qu'une réponse se suffit le plus souvent à elle-même ; une procédure, un
-- formulaire ou un modèle viennent l'appuyer quand le texte ne suffit pas. Table séparée parce
-- qu'une entrée peut en porter plusieurs, ou aucune, et qu'y réserver des colonnes dans faq
-- aurait laissé des vides sur la grande majorité des lignes.
--
-- Le fichier lui-même vit sur le serveur de fichiers, comme les pièces des non-conformités et
-- des documents ; seule sa désignation est ici. Le service de stockage ne s'active que si
-- l'installation le configure — sans lui, la FAQ fonctionne, le dépôt est simplement refusé
-- avec un message qui le dit.
--
CREATE TABLE public.fichiers_faq (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    faq_id uuid NOT NULL,

    nom character varying(255),
    ext character varying(32),
    type character varying(150),
    url text,

    CONSTRAINT pk_fichiers_faq PRIMARY KEY (id),
    CONSTRAINT fk_fichiers_faq_entree FOREIGN KEY (faq_id)
        REFERENCES public.faq (id) ON DELETE CASCADE
);

CREATE INDEX idx_fichiers_faq_entree ON public.fichiers_faq (faq_id);

COMMENT ON TABLE public.fichiers_faq IS
    'Pieces jointes facultatives d''une entree de FAQ ; le fichier vit sur le serveur de fichiers.';
COMMENT ON COLUMN public.fichiers_faq.url IS
    'Reference de l''objet stocke, jamais une adresse publique : le telechargement passe par le service.';

COMMENT ON TABLE public.faq IS
    'Foire aux questions de l''application : consultee dans l''aide, et recitee par l''assistant IA.';
COMMENT ON COLUMN public.faq.publiee IS
    'Entree visible dans l''aide et recitee par l''assistant. Fausse, elle reste en base.';
COMMENT ON COLUMN public.faq.rang IS
    'Ordre d''affichage ; le plafond de jetons de l''assistant ecarte par la fin.';
