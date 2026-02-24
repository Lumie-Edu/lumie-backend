-- Drop spreadsheet tables (feature replaced by Google Sheets embed)
DROP INDEX IF EXISTS idx_spreadsheets_permission;
DROP INDEX IF EXISTS idx_spreadsheets_cells;
DROP INDEX IF EXISTS idx_spreadsheets_owner_id;
DROP TABLE IF EXISTS spreadsheets;
