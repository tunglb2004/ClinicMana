ALTER TABLE appointments
    ADD COLUMN visit_type VARCHAR(20) NOT NULL DEFAULT 'INITIAL' AFTER status,
    ADD COLUMN follow_up_source_record_id VARCHAR(20) NULL AFTER visit_type;

ALTER TABLE appointments
    ADD KEY fk_appointments_follow_up_source_record (follow_up_source_record_id),
    ADD CONSTRAINT fk_appointments_follow_up_source_record
        FOREIGN KEY (follow_up_source_record_id) REFERENCES medical_records (record_id);

ALTER TABLE medical_records
    ADD COLUMN visit_type VARCHAR(20) NOT NULL DEFAULT 'INITIAL' AFTER status,
    ADD COLUMN parent_record_id VARCHAR(20) NULL AFTER visit_type,
    ADD COLUMN root_record_id VARCHAR(20) NULL AFTER parent_record_id;

ALTER TABLE medical_records
    ADD KEY fk_medical_records_parent_record (parent_record_id),
    ADD KEY fk_medical_records_root_record (root_record_id),
    ADD CONSTRAINT fk_medical_records_parent_record
        FOREIGN KEY (parent_record_id) REFERENCES medical_records (record_id),
    ADD CONSTRAINT fk_medical_records_root_record
        FOREIGN KEY (root_record_id) REFERENCES medical_records (record_id);

UPDATE appointments
SET visit_type = 'INITIAL'
WHERE visit_type IS NULL OR TRIM(visit_type) = '';

UPDATE medical_records
SET visit_type = 'INITIAL'
WHERE visit_type IS NULL OR TRIM(visit_type) = '';
