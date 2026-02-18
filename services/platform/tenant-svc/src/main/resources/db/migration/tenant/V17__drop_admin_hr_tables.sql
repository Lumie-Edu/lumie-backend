-- Drop HR-related tables that are no longer needed (WorkSchedule, Salary, Payroll)
-- Order: child tables first to respect foreign key constraints

DROP TABLE IF EXISTS payroll_items CASCADE;
DROP TABLE IF EXISTS payroll_records CASCADE;
DROP TABLE IF EXISTS salaries CASCADE;
DROP TABLE IF EXISTS work_schedules CASCADE;
