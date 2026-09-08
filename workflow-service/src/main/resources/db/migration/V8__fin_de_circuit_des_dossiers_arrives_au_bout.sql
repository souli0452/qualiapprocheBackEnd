--
-- workflow-service — les dossiers arrivés au bout de leur circuit sont terminés.
--
-- Une étape sans transition sortante termine le circuit : c'est la règle depuis « une étape sans
-- action termine le circuit », et le moteur passe l'instance à TERMINE à l'instant où le dossier
-- l'atteint. Les dossiers qui y étaient déjà arrivés avant cette règle n'en ont pas bénéficié :
-- ils sont restés EN_COURS sur une étape d'où plus rien ne part.
--
-- Rien ne les propose plus à personne — aucune transition n'est franchissable —, mais tout ce qui
-- compte les circuits en cours les compte encore. Le résumé de la cloche annonce à leur auteur un
-- dossier « en attente de validation » qui est clôturé depuis des semaines, et les tableaux de
-- bord les rangent parmi les dossiers en cours plutôt que parmi les clos.
--
-- Trois conditions, et non une seule. L'instance est en cours ; son étape existe encore dans son
-- propre circuit — un dossier dont l'étape a été supprimée n'est pas fini, il est bloqué, et il
-- doit être migré, non clos ; et aucune transition ne part de cette étape.
--
-- La date de fin est celle de la dernière décision inscrite : c'est le moment où le dossier a
-- atteint son étape finale. À défaut de décision — un circuit ouvert directement sur une étape
-- sans issue —, la date d'exécution, faute de mieux.
--
-- etat_code porte l'IDENTIFIANT de l'étape, pas son code : le renommage des codes (SOUMISSION,
-- CLOTURE…) ne l'a jamais touché. D'où la comparaison à workflow_step.id.
--

UPDATE public.workflow_validation_instance i
SET status = 'TERMINE',
    completed_at = COALESCE(
        i.completed_at,
        (SELECT max(h.decision_date)
           FROM public.workflow_validation_history h
          WHERE h.validation_instance_id = i.id),
        now())
WHERE i.status = 'EN_COURS'
  AND i.etat_code ~ '^[0-9]+$'
  AND EXISTS (
        SELECT 1
          FROM public.workflow_step s
         WHERE s.id = CAST(i.etat_code AS bigint)
           AND s.workflow_id::text = i.workflow_code)
  AND NOT EXISTS (
        SELECT 1
          FROM public.workflow_transition t
         WHERE t.from_step_id = CAST(i.etat_code AS bigint));
