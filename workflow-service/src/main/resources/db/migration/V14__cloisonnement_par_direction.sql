-- Cloisonnement par direction : chaque client ne voit que ses circuits, ses gabarits, ses dossiers.
--
-- La colonne direction_id existait déjà sur les tables d'AuditEntity (circuits, gabarits,
-- notifications), mais elle recevait la structure de l'auteur, ou rien. referentiel-service la
-- rend à la bonne direction après coup (rattrapage du cloisonnement) ; ce script ne fait que ce que
-- seule la base de ce service sait faire.

-- 1. Le dossier du moteur n'hérite pas d'AuditEntity : il reçoit sa propre colonne.
ALTER TABLE public.workflow_validation_instance ADD COLUMN IF NOT EXISTS direction_id uuid;

-- Un dossier existant prend la direction de son circuit (le code du dossier est l'identifiant du
-- circuit) : le rattrapage la convertira ensuite, avec celle du circuit, en direction racine.
UPDATE public.workflow_validation_instance i
SET direction_id = w.direction_id
FROM public.workflow w
WHERE i.direction_id IS NULL
  AND w.direction_id IS NOT NULL
  AND i.workflow_code = CAST(w.id AS text);

-- 2. La boîte d'envoi note le client pour lequel chaque message part.
ALTER TABLE public.message_sortant ADD COLUMN IF NOT EXISTS direction_id uuid;

-- 3. Un gabarit de courriel est unique par code au sein d'un client, non plus sur l'installation :
--    chaque client reçoit les gabarits livrés, sous les mêmes codes.
ALTER TABLE public.workflow_email_template DROP CONSTRAINT IF EXISTS ukgga8nddxj3cgf1oqeyw5xsm3o;
ALTER TABLE public.workflow_email_template DROP CONSTRAINT IF EXISTS uk_email_template_direction_code;
ALTER TABLE public.workflow_email_template
    ADD CONSTRAINT uk_email_template_direction_code UNIQUE (direction_id, code);

-- 4. Un circuit par défaut par famille et par client : les index d'unicité des circuits sont
--    reposés par ContrainteUniciteDesCircuits, avec la direction en tête, au démarrage qui suit.
DROP INDEX IF EXISTS public.uk_workflow_famille_cible;
DROP INDEX IF EXISTS public.uk_workflow_famille_defaut;

-- 5. Chaque lecture filtre désormais sur la direction.
CREATE INDEX IF NOT EXISTS idx_workflow_direction ON public.workflow (direction_id);
CREATE INDEX IF NOT EXISTS idx_email_template_direction ON public.workflow_email_template (direction_id);
CREATE INDEX IF NOT EXISTS idx_notification_direction ON public.notification_utilisateur (direction_id);
CREATE INDEX IF NOT EXISTS idx_instance_direction ON public.workflow_validation_instance (direction_id, status);
