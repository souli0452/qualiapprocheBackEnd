--
-- amelioration-service — verrous des tâches planifiées et file durable des courriels.
--
-- Verrous des tâches planifiées (ShedLock) : une ligne par tâche, prise par l'instance qui la déroule.
-- Sans eux, chaque instance du service déroulait sa propre tournée, et ses courriels partaient en double.
CREATE TABLE IF NOT EXISTS public.shedlock (
    name       varchar(64)  NOT NULL,
    lock_until timestamp    NOT NULL,
    locked_at  timestamp    NOT NULL,
    locked_by  varchar(255) NOT NULL,
    CONSTRAINT shedlock_pkey PRIMARY KEY (name)
);

-- Courriels à envoyer (transactional outbox) : déposés dans la transaction métier, remis par la
-- tournée, rejoués après un redémarrage ou une panne du serveur de messagerie.
CREATE TABLE public.courriel_sortant (
    id                     uuid         NOT NULL,
    destinataire           varchar(320) NOT NULL,
    copie                  varchar(320),
    sujet                  varchar(500) NOT NULL,
    corps                  text         NOT NULL,
    origine                varchar(128),
    statut                 varchar(16)  NOT NULL,
    tentatives             integer      NOT NULL DEFAULT 0,
    prochaine_tentative_at timestamp    NOT NULL,
    derniere_erreur        text,
    cree_le                timestamp    NOT NULL,
    envoye_le              timestamp,
    CONSTRAINT courriel_sortant_pkey PRIMARY KEY (id),
    CONSTRAINT ck_courriel_sortant_statut CHECK (statut IN ('A_ENVOYER', 'EN_COURS', 'ENVOYE', 'ABANDONNE'))
);
CREATE INDEX idx_courriel_sortant_echeance ON public.courriel_sortant (statut, prochaine_tentative_at);
