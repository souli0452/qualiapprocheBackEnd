-- Migration V3 : Refonte de la nomenclature des entités Non-Conformité
-- 1. Table source_de_non_conformite (anciennement type_non_conformite)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'type_non_conformite') THEN
        ALTER TABLE type_non_conformite RENAME TO source_de_non_conformite;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'source_de_non_conformite' AND column_name = 'libelle') THEN
        ALTER TABLE source_de_non_conformite RENAME COLUMN libelle TO libelle_source_non_conformite;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'source_de_non_conformite' AND column_name = 'description') THEN
        ALTER TABLE source_de_non_conformite RENAME COLUMN description TO description_source_non_conformite;
    END IF;
END $$;

-- 2. Table plan_action
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'plan_action' AND column_name = 'numero_odre') THEN
        ALTER TABLE plan_action RENAME COLUMN numero_odre TO numero_ordre;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'plan_action' AND column_name = 'non_conforme_id') THEN
        ALTER TABLE plan_action RENAME COLUMN non_conforme_id TO non_conformite_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'plan_action' AND column_name = 'cause_identifiees') THEN
        ALTER TABLE plan_action RENAME COLUMN cause_identifiees TO cause_identifiee;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'plan_action' AND column_name = 'solution_retenues') THEN
        ALTER TABLE plan_action RENAME COLUMN solution_retenues TO solution_retenue;
    END IF;
END $$;

-- 3. Table quali_nc
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quali_nc' AND column_name = 'justification') THEN
        ALTER TABLE quali_nc RENAME COLUMN justification TO description;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quali_nc' AND column_name = 'type_non_conformite_id') THEN
        ALTER TABLE quali_nc RENAME COLUMN type_non_conformite_id TO source_de_non_conformite_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quali_nc' AND column_name = 'type_non_conformite_libelle') THEN
        ALTER TABLE quali_nc RENAME COLUMN type_non_conformite_libelle TO source_de_non_conformite_libelle;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quali_nc' AND column_name = 'type_processus_id') THEN
        ALTER TABLE quali_nc RENAME COLUMN type_processus_id TO categorie_processus_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quali_nc' AND column_name = 'type_processus_libelle') THEN
        ALTER TABLE quali_nc RENAME COLUMN type_processus_libelle TO categorie_processus_libelle;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quali_nc' AND column_name = 'action_dsc') THEN
        ALTER TABLE quali_nc RENAME COLUMN action_dsc TO action_immediate;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quali_nc' AND column_name = 'structure_soumission_id') THEN
        ALTER TABLE quali_nc RENAME COLUMN structure_soumission_id TO structure_de_soumission_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quali_nc' AND column_name = 'structure_soumission_libelle') THEN
        ALTER TABLE quali_nc RENAME COLUMN structure_soumission_libelle TO structure_de_soumission_libelle;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quali_nc' AND column_name = 'user_imput_id') THEN
        ALTER TABLE quali_nc RENAME COLUMN user_imput_id TO agent_impute_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quali_nc' AND column_name = 'user_imput_full_name') THEN
        ALTER TABLE quali_nc RENAME COLUMN user_imput_full_name TO agent_impute_nom_complet;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quali_nc' AND column_name = 'user_impute_email') THEN
        ALTER TABLE quali_nc RENAME COLUMN user_impute_email TO agent_impute_email;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quali_nc' AND column_name = 'pertinance_pilote') THEN
        ALTER TABLE quali_nc RENAME COLUMN pertinance_pilote TO pertinence_pilote;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quali_nc' AND column_name = 'pertinance_rs') THEN
        ALTER TABLE quali_nc RENAME COLUMN pertinance_rs TO pertinence_rs;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'quali_nc' AND column_name = 'etat_traitement') THEN
        ALTER TABLE quali_nc DROP CONSTRAINT IF EXISTS quali_nc_etat_traitement_check;
        ALTER TABLE quali_nc RENAME COLUMN etat_traitement TO etat_de_traitement;
        ALTER TABLE quali_nc ADD CONSTRAINT quali_nc_etat_de_traitement_check
            CHECK (etat_de_traitement::text = ANY (ARRAY['SOUMISSION'::character varying::text, 'RECEPTION'::character varying::text, 'VALIDATION_RQ'::character varying::text, 'IMPUTATION'::character varying::text, 'TRAITEMENT'::character varying::text, 'VALIDATION'::character varying::text, 'VALIDATION_RS'::character varying::text, 'SUIVI_RQ'::character varying::text, 'CLOTURE'::character varying::text]));
    END IF;
END $$;

-- 4. Table niveau_non_conformite
ALTER TABLE IF EXISTS niveau_non_conformite ADD COLUMN IF NOT EXISTS score_gravite integer;
ALTER TABLE IF EXISTS niveau_non_conformite ADD COLUMN IF NOT EXISTS couleur character varying(20);

-- 5. Table piece_jointe
ALTER TABLE IF EXISTS piece_jointe ADD COLUMN IF NOT EXISTS taille bigint;

ALTER TABLE IF EXISTS niveau_non_conformite ALTER COLUMN description TYPE text;
