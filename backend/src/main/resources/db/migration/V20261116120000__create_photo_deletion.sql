CREATE TABLE photo_deletion (
    photo_key TEXT        PRIMARY KEY,
    queued_at TIMESTAMPTZ NOT NULL
);
