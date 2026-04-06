--
-- Remove status, billing, and date range fields from the classes table.
-- The idx_classes_status index must be dropped first as it depends on the status column.
--

drop index if exists idx_classes_status;

alter table classes
    drop column if exists status,
    drop column if exists max_students,
    drop column if exists tuition_amount,
    drop column if exists start_date,
    drop column if exists end_date;
