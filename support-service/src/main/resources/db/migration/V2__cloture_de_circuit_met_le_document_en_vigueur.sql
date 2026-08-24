-- Rattrapage des documents laissés au bord de leur circuit par une décision de clôture.
--
-- workflow-service publie « CLOSED » lorsque la décision finale d'un circuit est de nature
-- clôture plutôt qu'approbation, et l'éditeur de circuits propose cette nature pour toutes les
-- familles, documentaire comprise. Support-service n'en tenait aucun compte : le document
-- gardait « CLOTURE » pour étape courante et « es_traiter » à faux, c'est-à-dire ni en vigueur,
-- ni obsolète, ni brouillon. Il n'apparaissait donc pas parmi les documents en vigueur, et
-- aucune demande de modification ne pouvait le viser.
--
-- Le code traite désormais CLOSED comme une fin de circuit aboutie ; ce script fait de même pour
-- les documents déjà dans cet état, qu'aucune transition ne viendra plus corriger.
--
-- Le repère est sûr : « CLOTURE » est le nom de la décision, écrit par le moteur pour un état
-- terminal, tandis qu'une étape réellement nommée ainsi remonte son libellé — « Clôture ».

INSERT INTO public.qms_audit_logs (action, details, document_number, "timestamp", username)
SELECT
    'TRANSITION_STATUT',
    'Circuit clos par une décision de clôture : le document entre en vigueur (rattrapage).',
    d.document_number,
    now(),
    'system'
FROM public.qms_documents d
WHERE d.current_etape = 'CLOTURE'
  AND d.es_traiter IS FALSE
  AND d.obsolete IS FALSE
  AND d.archived IS FALSE;

UPDATE public.qms_documents d
SET es_traiter = TRUE,
    en_retard_revision = FALSE,
    -- Faute de date de décision conservée, l'entrée en vigueur est datée de la reprise. Les
    -- documents qui portaient déjà une date la gardent.
    date_vigueur = COALESCE(d.date_vigueur, now()),
    date_proch_revision = CASE
        WHEN d.periodicite_mois IS NULL THEN d.date_proch_revision
        ELSE COALESCE(d.date_vigueur, now()) + make_interval(months => d.periodicite_mois)
    END,
    last_modified_by = 'system',
    last_modified_reason = 'Clôture du circuit prise en compte (rattrapage V2)'
WHERE d.current_etape = 'CLOTURE'
  AND d.es_traiter IS FALSE
  AND d.obsolete IS FALSE
  AND d.archived IS FALSE;
