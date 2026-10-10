--
-- workflow-service — verrous des tâches planifiées.
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
