--
-- ia-service — V3 : le fil de discussion avec l'assistant qualité.
--
-- Le fil vit ici, et non dans le navigateur : c'est lui qu'on renvoie au modèle à chaque tour, et
-- un historique fourni par le client se forgerait — il suffirait d'y glisser un tour « assistant »
-- inventé pour faire reprendre n'importe quoi au suivant.
--
-- Tables distinctes de suggestions_ia, à dessein : le taux d'acceptation se calcule sur les
-- suggestions rédactionnelles, et y verser des messages de conversation — qui n'ont pas de verdict
-- — noierait l'indicateur sous des lignes qui ne mesurent rien.
--
-- NE JAMAIS MODIFIER un script déjà appliqué : Flyway en vérifie l'empreinte.
--

CREATE TABLE public.conversations_ia (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,
    titre character varying(200),
    derniere_activite_at timestamp(6) without time zone,
    nombre_messages integer DEFAULT 0 NOT NULL,
    CONSTRAINT conversations_ia_pkey PRIMARY KEY (id)
);

COMMENT ON TABLE public.conversations_ia IS 'Fils de discussion avec l''assistant qualite : un fil par personne et par sujet.';
COMMENT ON COLUMN public.conversations_ia.titre IS 'Premiere question, tronquee : de quoi reconnaitre le fil sans payer une generation pour le titrer.';
COMMENT ON COLUMN public.conversations_ia.derniere_activite_at IS 'Dernier echange, pour trier du plus vivant au plus ancien.';
COMMENT ON COLUMN public.conversations_ia.nombre_messages IS 'Messages du fil, les deux roles confondus ; borne la longueur d''une conversation.';

CREATE TABLE public.messages_ia (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,
    conversation_id uuid NOT NULL,
    role character varying(255) NOT NULL,
    contenu text NOT NULL,
    rang integer NOT NULL,
    modele character varying(255),
    prompt_version character varying(255),
    duree_ms bigint,
    jetons_utilises bigint,
    CONSTRAINT messages_ia_pkey PRIMARY KEY (id),
    CONSTRAINT messages_ia_conversation_fkey FOREIGN KEY (conversation_id)
        REFERENCES public.conversations_ia (id) ON DELETE CASCADE
);

COMMENT ON TABLE public.messages_ia IS 'Messages d''un fil : la question comme la reponse — sans la question, une reponse relue plus tard ne veut rien dire.';
COMMENT ON COLUMN public.messages_ia.role IS 'UTILISATEUR ou ASSISTANT ; inscrit par le serveur, jamais recu de l''appelant.';
COMMENT ON COLUMN public.messages_ia.rang IS 'Rang dans le fil, a partir de 1 : l''ordre d''un echange ne se deduit pas d''un horodatage.';
COMMENT ON COLUMN public.messages_ia.jetons_utilises IS 'Jetons de l''appel ayant produit ce message ; decomptes du meme budget quotidien que les suggestions.';

--
-- Le fil se relit dans l'ordre, et se reprend par sa fenetre la plus recente : les deux passent
-- par (conversation, rang). Sans cet index, chaque tour balayerait la table des messages.
--
CREATE INDEX idx_messages_ia_fil ON public.messages_ia (conversation_id, rang);

--
-- Liste des fils d'une personne, triee par activite ; et decompte quotidien des jetons, par
-- direction puis par personne — les memes que pour les suggestions, sur l'autre table.
--
CREATE INDEX idx_conversations_ia_auteur ON public.conversations_ia (created_by_id, derniere_activite_at);
CREATE INDEX idx_messages_ia_direction_jour ON public.messages_ia (direction_id, created_at);
CREATE INDEX idx_messages_ia_createur_jour ON public.messages_ia (created_by_id, created_at);
