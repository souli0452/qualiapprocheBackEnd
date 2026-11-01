-- Cloisonnement par direction : chaque client ne voit que ses lignes.
--
-- La colonne direction_id existe sur toutes les tables héritées d'AuditEntity ; elle devient le
-- discriminant que Hibernate (@TenantId) pose à l'insertion et ajoute à chaque requête. Les lignes
-- existantes, qui portaient la structure de leur auteur, sont rendues à leur direction par le
-- rattrapage que pilote referentiel-service — pas ici : seul lui connaît l'arbre des structures.

-- Le message emporte la direction pour laquelle il a été déposé : le service qui le reçoit le
-- traite sous elle.
ALTER TABLE public.message_sortant ADD COLUMN IF NOT EXISTS direction_id uuid;

-- La clé d'origine d'une non-conformité issue d'un audit n'est unique qu'au sein d'un client.
DROP INDEX IF EXISTS public.uk_quali_nc_cle_origine;
CREATE UNIQUE INDEX uk_quali_nc_cle_origine ON public.quali_nc (direction_id, cle_origine);

-- Chaque requête porte désormais « direction_id = ? » : les tables consultées en liste l'indexent.
CREATE INDEX IF NOT EXISTS ix_quali_nc_direction ON public.quali_nc (direction_id);
CREATE INDEX IF NOT EXISTS ix_plan_action_direction ON public.plan_action (direction_id);
CREATE INDEX IF NOT EXISTS ix_action_corrective_preventive_direction ON public.action_corrective_preventive (direction_id);
CREATE INDEX IF NOT EXISTS ix_reclamation_direction ON public.reclamation (direction_id);
CREATE INDEX IF NOT EXISTS ix_risque_direction ON public.risque (direction_id);
CREATE INDEX IF NOT EXISTS ix_type_non_conformite_direction ON public.type_non_conformite (direction_id);
CREATE INDEX IF NOT EXISTS ix_niveau_non_conformite_direction ON public.niveau_non_conformite (direction_id);
