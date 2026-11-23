ALTER TABLE reports
    ADD COLUMN public_approved BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN last_public_approved_by BIGINT,
    ADD COLUMN last_public_approved_at TIMESTAMPTZ,
    ADD CONSTRAINT fk_reports_last_public_approved_by
        FOREIGN KEY (last_public_approved_by) REFERENCES users(id) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_reports_public_approval_actor_time
        CHECK ((last_public_approved_by IS NULL) = (last_public_approved_at IS NULL)),
    ADD CONSTRAINT ck_reports_public_approval_requires_actor
        CHECK (
            NOT public_approved
            OR (last_public_approved_by IS NOT NULL AND last_public_approved_at IS NOT NULL)
        );

ALTER TABLE report_progress_updates
    ADD COLUMN public_approved BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN last_public_approved_by BIGINT,
    ADD COLUMN last_public_approved_at TIMESTAMPTZ,
    ADD COLUMN note_public_approved BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN last_note_public_approved_by BIGINT,
    ADD COLUMN last_note_public_approved_at TIMESTAMPTZ,
    ADD CONSTRAINT fk_progress_last_public_approved_by
        FOREIGN KEY (last_public_approved_by) REFERENCES users(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_progress_last_note_public_approved_by
        FOREIGN KEY (last_note_public_approved_by) REFERENCES users(id) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_progress_public_approval_actor_time
        CHECK ((last_public_approved_by IS NULL) = (last_public_approved_at IS NULL)),
    ADD CONSTRAINT ck_progress_public_approval_requires_actor
        CHECK (
            NOT public_approved
            OR (last_public_approved_by IS NOT NULL AND last_public_approved_at IS NOT NULL)
        ),
    ADD CONSTRAINT ck_progress_note_approval_actor_time
        CHECK ((last_note_public_approved_by IS NULL) = (last_note_public_approved_at IS NULL)),
    ADD CONSTRAINT ck_progress_note_approval_requires_actor
        CHECK (
            NOT note_public_approved
            OR (last_note_public_approved_by IS NOT NULL AND last_note_public_approved_at IS NOT NULL)
        ),
    ADD CONSTRAINT ck_progress_note_approval_requires_note
        CHECK (
            NOT note_public_approved
            OR (note IS NOT NULL AND btrim(note) <> '')
        );

CREATE INDEX ix_reports_public_feed
    ON reports (created_at DESC, id DESC)
    WHERE public_approved = TRUE;

CREATE INDEX ix_progress_public_feed
    ON report_progress_updates (report_id, created_at ASC, id ASC)
    WHERE public_approved = TRUE;
