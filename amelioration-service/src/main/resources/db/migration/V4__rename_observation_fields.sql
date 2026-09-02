
-- Renommage pour uniformisation
ALTER TABLE IF EXISTS quali_nc RENAME COLUMN justification_pilote TO observation_pilote;
ALTER TABLE IF EXISTS quali_nc RENAME COLUMN justification_rs TO observation_rs;
-- observations_rq is already plural, let's rename to singular observation_rq for full consistency
ALTER TABLE IF EXISTS quali_nc RENAME COLUMN observations_rq TO observation_rq;

-- S'assurer que le type est bien TEXT
ALTER TABLE IF EXISTS quali_nc ALTER COLUMN observation_pilote TYPE TEXT;
ALTER TABLE IF EXISTS quali_nc ALTER COLUMN observation_rs TYPE TEXT;
ALTER TABLE IF EXISTS quali_nc ALTER COLUMN observation_rq TYPE TEXT;
