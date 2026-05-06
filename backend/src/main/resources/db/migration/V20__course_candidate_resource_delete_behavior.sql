ALTER TABLE course_resource_candidates
    DROP FOREIGN KEY fk_course_candidates_resource;

ALTER TABLE course_resource_candidates
    ADD CONSTRAINT fk_course_candidates_resource
        FOREIGN KEY (published_resource_id)
        REFERENCES learning_resources(id)
        ON DELETE SET NULL;
