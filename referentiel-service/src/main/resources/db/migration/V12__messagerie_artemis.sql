--
-- referentiel-service — messagerie entre services : boîte d'envoi publiée sur Artemis (patron outbox).
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
