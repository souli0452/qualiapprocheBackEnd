--
-- evaluation-service — V2 : les sites, les critères et les niveaux d'évaluation des auditeurs
-- quittent le module d'audit.
--
-- Ils se paramètrent désormais dans referentiel-service : evaluation-service n'en garde que les
-- identifiants, sans clé étrangère, comme pour Processus et NonConformite. Le niveau reçu par un
-- auditeur est recopié en valeur_niveau, pour que retoucher l'échelle ne réécrive pas les
-- moyennes passées.
--
-- Aucune base ne porte encore d'évaluation d'auditeur : rien à reprendre.
--

ALTER TABLE public.evaluation_auditeur ADD COLUMN valeur_niveau integer NOT NULL;

COMMENT ON COLUMN public.evaluation_auditeur.valeur_niveau IS
    'L''ordre du niveau au moment de l''évaluation : c''est sur lui que se calcule la moyenne.';

ALTER TABLE public.audit_site DROP CONSTRAINT fk_audit_site_cible;
ALTER TABLE public.evaluation_auditeur DROP CONSTRAINT fk_eval_auditeur_critere;
ALTER TABLE public.evaluation_auditeur DROP CONSTRAINT fk_eval_auditeur_niveau;

DROP TABLE public.site;
DROP TABLE public.critere_evaluation_auditeur;
DROP TABLE public.niveau_evaluation_auditeur;
