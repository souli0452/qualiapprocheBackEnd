-- Le circuit de traitement d'une non-conformité porte enfin son nom.
--
-- Les deux valeurs s'appelaient « A » et « B » : rien, ni dans la base, ni dans le code, ni à
-- l'écran, ne disait laquelle demandait de rechercher la cause. Le responsable qualité choisit
-- désormais entre « Action corrective » et « Correction » à l'étape où il oriente le dossier, et
-- ce choix commande les colonnes que le plan d'action devra porter — d'où l'intérêt qu'il soit
-- lisible partout.
--
-- L'ordre des trois gestes compte : la contrainte de contrôle refuserait les nouvelles valeurs si
-- elle était encore en place au moment de la reprise des données.

ALTER TABLE quali_nc DROP CONSTRAINT IF EXISTS quali_nc_circuit_check;

UPDATE quali_nc SET circuit = 'ACTION_CORRECTIVE' WHERE circuit = 'A';
UPDATE quali_nc SET circuit = 'CORRECTION' WHERE circuit = 'B';

ALTER TABLE quali_nc
    ADD CONSTRAINT quali_nc_circuit_check
    CHECK (circuit IS NULL OR (circuit)::text = ANY ((ARRAY['ACTION_CORRECTIVE'::character varying,
                                                            'CORRECTION'::character varying])::text[]));
