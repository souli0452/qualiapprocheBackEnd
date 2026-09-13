-- Bases de données de la stack locale QualiSira.
--
-- Ce script n'est joué par l'entrypoint postgres qu'au PREMIER démarrage du volume
-- (répertoire de données vide) — pas besoin d'y être idempotent. Pour créer une base
-- sur un volume déjà initialisé : docker compose exec postgres createdb -U postgres <base>.
--
-- Un service par base ; les noms reprennent les valeurs par défaut des application.yml
-- (variables DB_URL_*).

CREATE DATABASE qualisira_user;              -- user-service
CREATE DATABASE qualisira_referentiel;       -- referentiel-service
CREATE DATABASE qualisira_amelioration;      -- amelioration-service
CREATE DATABASE qualisira_supportdb;         -- support-service
CREATE DATABASE bd_quali_sira_evaluation;    -- evaluation-service
CREATE DATABASE bd_quali_sira_realisation;   -- realisation-service
CREATE DATABASE bd_quali_sira_planification; -- planification-service
CREATE DATABASE bd_quali_sira_contexte;      -- contexte-service
CREATE DATABASE qualisira_workflowdb;        -- workflow-service
CREATE DATABASE qualisira_ia;                -- ia-service
