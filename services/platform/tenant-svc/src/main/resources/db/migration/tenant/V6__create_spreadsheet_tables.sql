-- Create spreadsheets table for collaborative spreadsheet feature
CREATE TABLE IF NOT EXISTS spreadsheets (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    row_count INTEGER NOT NULL DEFAULT 100,
    column_count INTEGER NOT NULL DEFAULT 26,
    column_widths JSONB DEFAULT '{}',
    row_heights JSONB DEFAULT '{}',
    cells JSONB DEFAULT '{}',
    owner_id BIGINT NOT NULL,
    permission VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for fast owner-based queries
CREATE INDEX idx_spreadsheets_owner_id ON spreadsheets(owner_id);

-- GIN index for JSONB cells column to support efficient cell queries
CREATE INDEX idx_spreadsheets_cells ON spreadsheets USING GIN(cells);

-- Index for permission-based filtering
CREATE INDEX idx_spreadsheets_permission ON spreadsheets(permission);

-- Comment on table
COMMENT ON TABLE spreadsheets IS 'Stores collaborative spreadsheet data with JSONB cells';
COMMENT ON COLUMN spreadsheets.cells IS 'JSONB map of cell address (e.g., A1, B2) to cell data including value, formula, and style';
COMMENT ON COLUMN spreadsheets.permission IS 'Access permission: PRIVATE, VIEW_ONLY, or EDITABLE';
