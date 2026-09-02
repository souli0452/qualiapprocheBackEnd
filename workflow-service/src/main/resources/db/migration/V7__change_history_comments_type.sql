-- Changer le type de la colonne comments en TEXT pour supporter les longues observations
ALTER TABLE workflow_validation_history ALTER COLUMN comments TYPE TEXT;
