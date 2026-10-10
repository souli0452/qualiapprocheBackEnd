--
-- support-service — les numéros de documents qualité se tirent d'une série.
--
-- Le numéro se calculait par le plus grand numéro existant + 1, sans verrou : deux documents créés
-- au même moment recevaient le même numéro, et le second échouait sur l'unicité (erreur 500,
-- saisie perdue). Le « plus grand » était de surcroît alphabétique : passé 999, …-1000 se classait
-- avant …-999, et la série repartait sur des numéros déjà pris.
--
-- Les séries (patron commun, common/serie) le remplacent : une ligne par préfixe
-- (TYPE-SIGLE-ANNÉE-), verrouillée pendant la transaction de création.
--
CREATE TABLE public.serie_numero (
    cle_serie       varchar(128) NOT NULL,
    valeur_courante bigint       NOT NULL,
    CONSTRAINT serie_numero_pkey PRIMARY KEY (cle_serie),
    CONSTRAINT ck_serie_numero_positive CHECK (valeur_courante >= 0)
);

-- Les séries repartent du plus grand rang déjà émis, préfixe par préfixe, comparé en nombre.
INSERT INTO public.serie_numero (cle_serie, valeur_courante)
SELECT 'DOC|' || regexp_replace(document_number, '[0-9]+$', ''),
       MAX(CAST(substring(document_number FROM '([0-9]+)$') AS bigint))
FROM public.qms_documents
WHERE document_number ~ '-[0-9]+$'
GROUP BY 1;
