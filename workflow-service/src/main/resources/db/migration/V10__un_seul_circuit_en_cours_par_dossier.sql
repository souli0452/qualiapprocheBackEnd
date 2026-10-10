--
-- workflow-service — un dossier n'a jamais deux circuits en cours, et le moteur reçoit ses index.
--
-- L'ouverture d'un circuit vérifie qu'aucun n'est déjà en cours sur le dossier, puis en crée un :
-- deux ouvertures simultanées passaient le contrôle toutes les deux, et le dossier se retrouvait
-- avec deux circuits concurrents — deux titulaires, deux décisions, deux clôtures. La base le
-- refuse désormais ; la seconde ouverture reçoit un conflit (409). Vérifié avant d'écrire ce script :
-- aucun dossier n'a aujourd'hui deux circuits en cours.
--
-- Même portée que le contrôle du service (findTopByResourceIdAndStatus) : l'identifiant du
-- dossier seul, les identifiants étant des UUID propres à chaque dossier.
CREATE UNIQUE INDEX uk_instance_en_cours_par_dossier ON public.workflow_validation_instance (resource_id)
    WHERE status = 'EN_COURS';

-- Les recherches courantes du moteur, jusqu'ici en parcours complet.
CREATE INDEX idx_instance_dossier ON public.workflow_validation_instance (resource_id);
CREATE INDEX idx_instance_famille_statut ON public.workflow_validation_instance (resource_type, status);
CREATE INDEX idx_instance_createur_statut ON public.workflow_validation_instance (createur_id, status);
CREATE INDEX idx_instance_etape_statut ON public.workflow_validation_instance (etat_code, status);
CREATE INDEX idx_historique_instance ON public.workflow_validation_history (validation_instance_id);
CREATE INDEX idx_valeur_champ_historique ON public.workflow_field_value (history_id);
CREATE INDEX idx_champ_etape ON public.workflow_step_field (step_id);
CREATE INDEX idx_transition_vers ON public.workflow_transition (to_step_id);
