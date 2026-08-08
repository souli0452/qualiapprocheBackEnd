--
-- workflow-service — la structure où le dossier se trouve accompagne l'instance de circuit.
--
-- Les courriels d'étape étaient adressés à tous les porteurs du rôle responsable, toutes
-- structures confondues : chaque franchissement écrivait à la plateforme entière. L'instance
-- porte désormais la structure du dossier — celle du déclarant à l'ouverture, puis celle que
-- désigne une étape de transfert — et la notification s'y borne.
--
-- Les instances déjà ouvertes restent sans structure : leurs notifications conservent l'ancienne
-- portée plutôt que de se taire.
--

ALTER TABLE public.workflow_validation_instance
    ADD COLUMN IF NOT EXISTS structure_id character varying(255);
