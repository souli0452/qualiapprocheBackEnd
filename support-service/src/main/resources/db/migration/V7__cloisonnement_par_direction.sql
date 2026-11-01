-- Cloisonnement des données par client (direction racine).
--
-- Hibernate filtre chaque lecture sur direction_id (@TenantId). Ce script :
--  * ajoute la colonne aux tables qui la portent en propre sans hériter d'AuditEntity : le journal
--    des documents, cherché par numéro de document, et les partages, cherchés par utilisateur ou
--    par structure ; les lignes existantes prennent la direction de leur document ;
--  * fait de la direction un élément des unicités : deux clients ont chacun leur type « PRO »,
--    leur catalogue d'étapes et leurs numéros de document ;
--  * note sur chaque message sortant le client pour qui il a été déposé.
--
-- Les lignes encore sans direction (ou portant la structure de leur auteur, ancienne sémantique)
-- sont rendues à leur client par le rattrapage que pilote referentiel-service.

ALTER TABLE public.message_sortant ADD COLUMN IF NOT EXISTS direction_id uuid;

ALTER TABLE public.qms_audit_logs ADD COLUMN IF NOT EXISTS direction_id uuid;
ALTER TABLE public.qms_document_user_access ADD COLUMN IF NOT EXISTS direction_id uuid;
ALTER TABLE public.qms_document_structure_access ADD COLUMN IF NOT EXISTS direction_id uuid;

UPDATE public.qms_document_user_access a SET direction_id = d.direction_id
  FROM public.qms_documents d WHERE a.document_id = d.id AND a.direction_id IS NULL;
UPDATE public.qms_document_structure_access a SET direction_id = d.direction_id
  FROM public.qms_documents d WHERE a.document_id = d.id AND a.direction_id IS NULL;
-- Avant le cloisonnement, un numéro de document était unique sur toute l'installation.
UPDATE public.qms_audit_logs l SET direction_id = d.direction_id
  FROM public.qms_documents d WHERE l.document_number = d.document_number AND l.direction_id IS NULL;

ALTER TABLE public.qms_workflow_step_templates DROP CONSTRAINT IF EXISTS uk_step_template_code;
ALTER TABLE public.qms_workflow_step_templates
    ADD CONSTRAINT uk_step_template_code UNIQUE (direction_id, code);

ALTER TABLE public.qms_document_types DROP CONSTRAINT IF EXISTS uktdf210qog6ow68s5xt4jfyxb9;
ALTER TABLE public.qms_document_types
    ADD CONSTRAINT uk_document_type_code UNIQUE (direction_id, code);

ALTER TABLE public.qms_documents DROP CONSTRAINT IF EXISTS ukpa5sg88srrfutqt2x74ln78qj;
ALTER TABLE public.qms_documents
    ADD CONSTRAINT uk_document_number UNIQUE (direction_id, document_number);

CREATE INDEX IF NOT EXISTS idx_qms_documents_direction ON public.qms_documents (direction_id);
CREATE INDEX IF NOT EXISTS idx_demandes_document_direction ON public.qms_demandes_document (direction_id);
CREATE INDEX IF NOT EXISTS idx_qms_audit_logs_direction ON public.qms_audit_logs (direction_id, document_number);
CREATE INDEX IF NOT EXISTS idx_qms_user_access_direction ON public.qms_document_user_access (direction_id, user_id);
CREATE INDEX IF NOT EXISTS idx_qms_structure_access_direction ON public.qms_document_structure_access (direction_id, structure_id);
