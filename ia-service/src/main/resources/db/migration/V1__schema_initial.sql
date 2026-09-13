--
-- ia-service — schéma initial : traçabilité des suggestions de l'assistant IA.
--
-- Ce script est le POINT DE DÉPART de l'historique de migration. NE JAMAIS MODIFIER un script
-- déjà appliqué : Flyway en vérifie l'empreinte et refuserait de démarrer. Toute évolution du
-- schéma passe par un nouveau V<n+1>__....sql.
--

--
-- Name: suggestions_ia; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.suggestions_ia (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,
    type_assistance character varying(255) NOT NULL,
    prompt_version character varying(255) NOT NULL,
    modele character varying(255),
    texte_source text,
    contexte text,
    suggestion text,
    verdict character varying(255),
    ressource_type character varying(255),
    ressource_id uuid,
    duree_ms bigint,
    jetons_utilises bigint,
    CONSTRAINT suggestions_ia_pkey PRIMARY KEY (id)
);

COMMENT ON TABLE public.suggestions_ia IS 'Trace de chaque suggestion de l''assistant IA : prompt et sa version, modèle, matière fournie, texte produit et sort réservé par l''utilisateur.';
COMMENT ON COLUMN public.suggestions_ia.type_assistance IS 'Nature de l''assistance ayant produit la suggestion (DESCRIPTION_NON_CONFORMITE, CAUSES_PLAN_ACTION, ...).';
COMMENT ON COLUMN public.suggestions_ia.prompt_version IS 'Version du prompt système utilisé, portée par PromptRegistry : permet de comparer l''historique entre versions de consignes.';
COMMENT ON COLUMN public.suggestions_ia.modele IS 'Modèle ayant produit la suggestion (ex. mistral-small-latest).';
COMMENT ON COLUMN public.suggestions_ia.texte_source IS 'Brouillon ou notes fournis par le demandeur.';
COMMENT ON COLUMN public.suggestions_ia.contexte IS 'Éléments de contexte fournis par l''écran appelant, sérialisés en JSON.';
COMMENT ON COLUMN public.suggestions_ia.suggestion IS 'Texte produit par l''assistant.';
COMMENT ON COLUMN public.suggestions_ia.verdict IS 'Sort réservé par l''utilisateur (ACCEPTEE, MODIFIEE, REJETEE) ; nul tant qu''il ne s''est pas prononcé.';
COMMENT ON COLUMN public.suggestions_ia.ressource_type IS 'Type de la ressource métier concernée, pour rattacher la trace à son dossier (facultatif).';
COMMENT ON COLUMN public.suggestions_ia.ressource_id IS 'Identifiant de la ressource métier concernée (facultatif).';
COMMENT ON COLUMN public.suggestions_ia.duree_ms IS 'Durée de l''appel au modèle, en millisecondes.';
COMMENT ON COLUMN public.suggestions_ia.jetons_utilises IS 'Jetons consommés par l''appel, si le fournisseur les rapporte.';
