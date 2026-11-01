-- Cloisonnement par direction (un client = une direction racine).
--
-- Les rôles applicatifs et leurs affectations appartiennent désormais chacun à un client : leur
-- colonne direction_id, présente depuis l'origine mais remplie avec la structure de l'auteur, est
-- filtrée par Hibernate (@TenantId) et rendue à la bonne direction par le rattrapage que
-- referentiel-service commande (CloisonnementInterneController).
--
-- Aucune contrainte d'unicité n'est posée sur (direction_id, name) : app_roles n'en portait pas,
-- et une contrainte ferait échouer le rattrapage si deux lignes d'un même rôle se retrouvaient
-- dans une même direction. L'amorce (RoleInitializer) ne crée un rôle que s'il manque.

ALTER TABLE public.message_sortant ADD COLUMN IF NOT EXISTS direction_id uuid;

CREATE INDEX IF NOT EXISTS idx_app_roles_direction_nom ON public.app_roles (direction_id, name);
CREATE INDEX IF NOT EXISTS idx_user_role_assignments_direction ON public.user_role_assignments (direction_id, user_id);
