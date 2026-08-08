--
-- referentiel-service — retrait de la configuration globale, reversée dans les réglages clé/valeur.
--
-- L'entité `ConfigGlobal` portait trois champs figés dans le code : nom et courriel du responsable
-- qualité, délai de rappel avant échéance. Ils sont devenus trois réglages parmi d'autres dans
-- `parametres`, que l'organisation étend sans qu'on livre une version. La table, elle, subsistait :
-- `ddl-auto` ne supprime jamais rien.
--
-- Les valeurs sont reportées avant la suppression, et seulement là où le réglage est encore vide :
-- une valeur déjà saisie dans l'écran fait foi. Sans ce report, une installation qui avait renseigné
-- son responsable qualité l'aurait perdu au passage à Flyway, et les courriels de non-conformité
-- seraient repartis sans copie — en silence.
--

DO $$
DECLARE
    ancienne RECORD;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = 'public' AND table_name = 'config_global') THEN
        RETURN;
    END IF;

    -- La plus récemment modifiée : la table n'a jamais porté qu'une ligne en pratique, mais rien
    -- ne l'imposait, et un report indéterminé serait pire qu'un report explicite.
    SELECT nom_complet_rq, email_rq, rappel_echeance
      INTO ancienne
      FROM config_global
     ORDER BY update_at DESC NULLS LAST, created_at DESC NULLS LAST
     LIMIT 1;

    IF FOUND THEN
        UPDATE parametres
           SET valeur = btrim(ancienne.nom_complet_rq)
         WHERE cle = 'RESPONSABLE_QUALITE_NOM'
           AND (valeur IS NULL OR btrim(valeur) = '')
           AND ancienne.nom_complet_rq IS NOT NULL
           AND btrim(ancienne.nom_complet_rq) <> '';

        UPDATE parametres
           SET valeur = btrim(ancienne.email_rq)
         WHERE cle = 'RESPONSABLE_QUALITE_EMAIL'
           AND (valeur IS NULL OR btrim(valeur) = '')
           AND ancienne.email_rq IS NOT NULL
           AND btrim(ancienne.email_rq) <> '';

        -- Le délai n'était pas lu par le code de l'époque ; il l'est désormais par la tournée de
        -- rappels. Zéro n'est pas un délai : il vaut « non renseigné », et le défaut s'applique.
        UPDATE parametres
           SET valeur = ancienne.rappel_echeance::text
         WHERE cle = 'RAPPEL_ECHEANCE_JOURS'
           AND (valeur IS NULL OR btrim(valeur) = '')
           AND ancienne.rappel_echeance IS NOT NULL
           AND ancienne.rappel_echeance > 0;
    END IF;
END $$;

DROP TABLE IF EXISTS public.config_global;
