--
-- amelioration-service — les numéros de non-conformité se tirent d'une série.
--
-- Le numéro se calculait par MAX(numéro de l'année) + 1, sans verrou ni contrainte : deux
-- déclarations simultanées dans une même structure recevaient le même numéro, accepté en silence
-- et repris ensuite dans les courriels et la fiche de clôture.
--
-- Les séries (patron commun, common/serie) le remplacent : une ligne par portée — ici une
-- structure et une année —, verrouillée pendant la transaction de création. Une création annulée
-- rend son numéro ; une série ne recule jamais.
--
CREATE TABLE public.serie_numero (
    cle_serie       varchar(128) NOT NULL,
    valeur_courante bigint       NOT NULL,
    CONSTRAINT serie_numero_pkey PRIMARY KEY (cle_serie),
    CONSTRAINT ck_serie_numero_positive CHECK (valeur_courante >= 0)
);

-- Les séries repartent du dernier numéro déjà attribué, structure par structure et année par année.
INSERT INTO public.serie_numero (cle_serie, valeur_courante)
SELECT 'NC|' || COALESCE(structure_soumission_id, '') || '|' || EXTRACT(YEAR FROM created_at)::integer,
       MAX(CAST(SUBSTRING(numero_reference FROM LENGTH(numero_reference) - 4) AS integer))
FROM public.quali_nc
WHERE numero_reference ~ '-[0-9]{5}$' AND created_at IS NOT NULL
GROUP BY 1;

-- Filet : un chemin de création qui contournerait la série serait refusé par la base au lieu
-- d'enregistrer un doublon. Par structure : deux structures de même sigle restent distinctes.
CREATE UNIQUE INDEX uk_quali_nc_numero ON public.quali_nc (structure_soumission_id, numero_reference)
    WHERE numero_reference IS NOT NULL;
