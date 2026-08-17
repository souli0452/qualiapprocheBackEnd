--
-- workflow-service — la clôture devient une troisième nature d'action.
--
-- Le schéma initial, relevé sur ce que Hibernate avait construit, borne la colonne `decision` aux
-- deux natures d'origine par une contrainte CHECK. L'énumération `StepDecision` s'est enrichie de
-- `CLOTURE` : sans cet élargissement, la validation du schéma passe — Hibernate ne relit pas les
-- contraintes — mais la première action de clôture posée depuis l'éditeur serait refusée par la
-- base au moment de l'écrire, et l'écran recevrait une erreur qu'aucune saisie n'explique.
--
-- Les deux colonnes sont concernées : la nature d'une transition, et la portée d'un champ d'étape
-- (« ne demander cette saisie qu'à qui clôture »).
--

ALTER TABLE public.workflow_transition
    DROP CONSTRAINT IF EXISTS workflow_transition_decision_check;
ALTER TABLE public.workflow_transition
    ADD CONSTRAINT workflow_transition_decision_check
        CHECK (((decision)::text = ANY
            ((ARRAY['APPROUVE'::character varying, 'REJETE'::character varying,
                    'CLOTURE'::character varying])::text[])));

ALTER TABLE public.workflow_step_field
    DROP CONSTRAINT IF EXISTS workflow_step_field_decision_check;
ALTER TABLE public.workflow_step_field
    ADD CONSTRAINT workflow_step_field_decision_check
        CHECK (((decision)::text = ANY
            ((ARRAY['APPROUVE'::character varying, 'REJETE'::character varying,
                    'CLOTURE'::character varying])::text[])));
