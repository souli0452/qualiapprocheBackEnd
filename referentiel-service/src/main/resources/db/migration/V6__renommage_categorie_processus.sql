-- Migration V6 : Renommage de la table type_processus en categorie_processus
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'type_processus') THEN
        ALTER TABLE type_processus RENAME TO categorie_processus;
    END IF;
END $$;
