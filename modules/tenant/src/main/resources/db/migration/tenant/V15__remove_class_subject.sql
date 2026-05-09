-- Remove subject column from classes table
alter table classes drop column if exists subject;
