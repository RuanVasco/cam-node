-- Expand only: the previous release ignores the column, so it keeps hard-deleting until it is replaced.
ALTER TABLE ring ADD COLUMN removed_at TIMESTAMPTZ;

CREATE INDEX idx_ring_removed_at ON ring (removed_at DESC) WHERE removed_at IS NOT NULL;
