ALTER TABLE reviews
ADD COLUMN service_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL AFTER doctor_id;

ALTER TABLE reviews
ADD CONSTRAINT fk_reviews_service
FOREIGN KEY (service_id) REFERENCES services(service_id);