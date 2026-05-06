DROP TABLE IF EXISTS candidate_resources;
DROP TABLE IF EXISTS ingestion_tasks;
DROP TABLE IF EXISTS ingestion_sources;
DROP TABLE IF EXISTS resource_manager_grants;
DROP TABLE IF EXISTS collaborator_applications;

ALTER TABLE users
    DROP COLUMN resource_manager;
