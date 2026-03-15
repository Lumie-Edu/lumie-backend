-- V7: Add omr_image_key column to exam_results for permanent OMR image storage

ALTER TABLE exam_results ADD COLUMN omr_image_key VARCHAR(500);
