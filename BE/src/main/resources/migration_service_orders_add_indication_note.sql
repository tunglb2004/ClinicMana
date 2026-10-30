ALTER TABLE service_orders
ADD COLUMN indication_note TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL
AFTER result_file_url;