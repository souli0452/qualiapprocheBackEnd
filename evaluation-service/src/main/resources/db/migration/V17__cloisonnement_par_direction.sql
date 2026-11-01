--
-- evaluation-service — cloisonnement par direction (multitenant).
--
-- Chaque client est une direction racine ; toutes ses lignes portent son identifiant dans
-- direction_id, que Hibernate filtre (@TenantId). Le module Audit filtrait déjà sur cette colonne :
-- ses unicités sont posées par direction depuis V1 (uk_audit_reference, uk_noeud_code,
-- uk_codification_type, uk_auditeur_utilisateur…) et n'ont pas à changer.
--
-- Ce que cette migration ne fait pas : rendre les lignes existantes à leur client. Jusqu'ici
-- direction_id recevait la structure de l'auteur ; la correspondance structure -> direction n'est
-- connue que de referentiel-service, qui la transmet au démarrage (rattrapage des directions).
--

-- Le message déposé emporte la direction de son auteur : le service qui le reçoit le traite pour elle.
ALTER TABLE public.message_sortant ADD COLUMN IF NOT EXISTS direction_id uuid;

-- Chaque lecture filtre désormais sur direction_id : un index par table qui la porte.
DO $$
DECLARE
    t record;
BEGIN
    FOR t IN
        SELECT c.table_name
        FROM information_schema.columns c
        JOIN information_schema.tables tb
          ON tb.table_schema = c.table_schema AND tb.table_name = c.table_name
        WHERE c.table_schema = 'public'
          AND c.column_name = 'direction_id'
          AND tb.table_type = 'BASE TABLE'
          AND c.table_name <> 'message_sortant'
    LOOP
        EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON public.%I (direction_id)',
                       'idx_' || t.table_name || '_direction', t.table_name);
    END LOOP;
END $$;
