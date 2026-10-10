--
-- workflow-service — messagerie entre services : boîte d'envoi publiée sur Artemis (patron outbox).
--
-- Tout ce que le service émet — courriels, rappels du circuit — est inscrit ici dans la
-- transaction métier, puis publié sur le courtier ; le relais (sous ShedLock) reprend ce qui
-- n'a pas pu partir. Remplace la file des courriels envoyés par SMTP depuis chaque service.
--
CREATE TABLE public.message_sortant (
    id                     uuid         NOT NULL,
    destination            varchar(200) NOT NULL,
    type                   varchar(64)  NOT NULL,
    contenu                text         NOT NULL,
    statut                 varchar(16)  NOT NULL,
    tentatives             integer      NOT NULL DEFAULT 0,
    prochaine_tentative_at timestamp    NOT NULL,
    derniere_erreur        text,
    cree_le                timestamp    NOT NULL,
    publie_le              timestamp,
    CONSTRAINT message_sortant_pkey PRIMARY KEY (id),
    CONSTRAINT ck_message_sortant_statut CHECK (statut IN ('A_PUBLIER', 'EN_COURS', 'PUBLIE'))
);
CREATE INDEX idx_message_sortant_echeance ON public.message_sortant (statut, prochaine_tentative_at);

-- Les messages déjà traités par ce service : un courtier livre « au moins une fois », une
-- livraison en double est écartée par cette clé.
CREATE TABLE public.message_traite (
    id      uuid      NOT NULL,
    recu_le timestamp NOT NULL,
    CONSTRAINT message_traite_pkey PRIMARY KEY (id)
);

-- Les rappels encore en attente dans l'ancien registre partent par la boîte d'envoi, vers la file
-- du service propriétaire du dossier.
INSERT INTO public.message_sortant (id, destination, type, contenu, statut, tentatives, prochaine_tentative_at, cree_le)
SELECT id,
       CASE WHEN upper(resource_type) IN ('DOCUMENT', 'DEMANDE_DOCUMENT') THEN 'qualisira.circuit.rappels.support-service'
            ELSE 'qualisira.circuit.rappels.amelioration-service' END,
       'rappel-circuit',
       json_build_object('typeRessource', resource_type, 'ressourceId', resource_id, 'contenu', payload::json)::text,
       'A_PUBLIER', 0, now(), coalesce(created_at, now())
FROM public.workflow_notification
WHERE canal = 'WEBHOOK' AND statut IN ('A_REMETTRE', 'EN_COURS')
  AND upper(resource_type) IN ('DOCUMENT', 'DEMANDE_DOCUMENT', 'NON_CONFORMITE', 'PLAN_ACTION');

-- L'ancien registre n'est plus écrit ni lu. Il est renommé plutôt qu'effacé : son historique reste
-- consultable, et un courriel qui y attendait encore (canal EMAIL, gabarit non rendu) peut y être
-- relu pour être renvoyé à la main.
ALTER TABLE public.workflow_notification RENAME TO workflow_notification_archive;
