--
-- workflow-service — la référence lisible du dossier accompagne l'instance de circuit.
--
-- Le moteur ne connaît la ressource que par son UUID. Les gabarits de courriel, eux, citent une
-- référence (« Non-Conformité n°{numeroNc} ») que le moteur ne pouvait pas fournir : les messages
-- partaient avec un numéro vide. Les modules transmettent désormais leur référence à l'ouverture du
-- circuit, et l'instance la porte.
--

ALTER TABLE public.workflow_validation_instance
    ADD COLUMN IF NOT EXISTS reference_lisible varchar(100);

-- Les objets des courriels citent désormais la référence, par la même syntaxe {variable} que le
-- corps. Seuls les objets encore identiques à ceux livrés sont repris : un objet retouché depuis
-- l'écran appartient à qui l'a rédigé, la mise à jour ne l'écrase pas.
UPDATE public.workflow_email_template SET subject = 'Nouvelle Non-Conformité imputée – {numeroNc}'
 WHERE code = 'emailTemplate' AND subject = 'Nouvelle Non-Conformité imputée';
UPDATE public.workflow_email_template SET subject = 'Non-Conformité transmise – {numeroNc}'
 WHERE code = 'structureToStructure' AND subject = 'Non-Conformité transmise';
UPDATE public.workflow_email_template SET subject = 'Validation requise - Non-Conformité {numeroNc}'
 WHERE code = 'validationNonConformite' AND subject = 'Validation requise - Non-Conformité';
UPDATE public.workflow_email_template SET subject = 'Non-Conformité {numeroNc} rejetée'
 WHERE code = 'rejectNonConformite' AND subject = 'Non-Conformité rejetée';
UPDATE public.workflow_email_template SET subject = 'Nouveau plan d''action correctif – {numeroNc}'
 WHERE code = 'emailPlanAction' AND subject = 'Nouveau plan d''action correctif';
UPDATE public.workflow_email_template SET subject = 'Validation requise - Plans d''actions – {numeroNc}'
 WHERE code = 'validationPlanRequise' AND subject = 'Validation requise - Plans d''actions';
UPDATE public.workflow_email_template SET subject = 'Validation attendue - Non-Conformité {numeroNc}'
 WHERE code = 'validationRq' AND subject = 'Validation attendue - Non-Conformité';
UPDATE public.workflow_email_template SET subject = 'Mise en œuvre du plan d''action – {numeroNc}'
 WHERE code = 'emailRqPlan' AND subject = 'Mise en œuvre du plan d''action';
UPDATE public.workflow_email_template SET subject = 'Validation requise - Clôture NC {numeroNc}'
 WHERE code = 'validationAfterPlan' AND subject = 'Validation requise - Clôture NC';
UPDATE public.workflow_email_template SET subject = 'Traitement NC {numeroNc} réussi'
 WHERE code = 'succesTraitementNonformite' AND subject = 'Traitement NC réussi';
UPDATE public.workflow_email_template SET subject = 'Non-conformité {numeroNc} traitée avec succès'
 WHERE code = 'traitementReussi' AND subject = 'Non-conformité traitée avec succès';
UPDATE public.workflow_email_template SET subject = 'Rejet d''un plan d''action – {numeroNc}'
 WHERE code = 'rejectPlanAction' AND subject = 'Rejet d''un plan d''action';
