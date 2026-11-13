CREATE TABLE ring (
    id               UUID        PRIMARY KEY,
    ringed_at        TIMESTAMPTZ NOT NULL,
    photo_key        TEXT        NOT NULL,
    photo_size_bytes INTEGER     NOT NULL
);

CREATE INDEX idx_ring_ringed_at ON ring (ringed_at DESC);
