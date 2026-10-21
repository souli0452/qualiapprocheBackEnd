--
-- workflow-service — les rappels qui étaient en cours de remise au moment de la bascule.
--
-- V12 recopiait vers la boîte d'envoi les rappels du circuit encore dus, en retenant les états
-- 'A_REMETTRE' et 'EN_COURS'. Mais l'ancien registre nommait le second 'EN_COURS_DE_REMISE' : un
-- rappel revendiqué par un ouvrier — exactement l'état de celui qu'on remettait quand le service a
-- été arrêté pour être redéployé — restait dans l'archive. Le dossier concerné (non-conformité,
-- plan d'action, document) ne recevait jamais l'avancement de son circuit et demeurait à l'étape
-- précédente.
--
-- Ils partent à leur tour par la boîte d'envoi. Un rappel qui aurait tout de même atteint son
-- service avant l'arrêt est relu sans dommage : l'avancement qu'il porte est déjà en place, et
-- son application ne change rien. Les rappels abandonnés après épuisement des tentatives
-- ('ABANDONNEE') restent dans l'archive, comme avant : ils demandaient déjà une reprise manuelle.
--
INSERT INTO public.message_sortant (id, destination, type, contenu, statut, tentatives, prochaine_tentative_at, cree_le)
SELECT id,
       CASE WHEN upper(resource_type) IN ('DOCUMENT', 'DEMANDE_DOCUMENT') THEN 'qualisira.circuit.rappels.support-service'
            ELSE 'qualisira.circuit.rappels.amelioration-service' END,
       'rappel-circuit',
       json_build_object('typeRessource', resource_type, 'ressourceId', resource_id, 'contenu', payload::json)::text,
       'A_PUBLIER', 0, now(), coalesce(created_at, now())
FROM public.workflow_notification_archive
WHERE canal = 'WEBHOOK' AND statut = 'EN_COURS_DE_REMISE'
  AND upper(resource_type) IN ('DOCUMENT', 'DEMANDE_DOCUMENT', 'NON_CONFORMITE', 'PLAN_ACTION')
ON CONFLICT (id) DO NOTHING;
