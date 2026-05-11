CREATE TABLE notifications (
                               id           BIGSERIAL PRIMARY KEY,
                               user_id      BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               tmdb_id      INTEGER     NOT NULL,
                               series_title VARCHAR(255) NOT NULL,
                               episode_code VARCHAR(20),
                               air_date     DATE        NOT NULL,
                               sent_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               read         BOOLEAN     NOT NULL DEFAULT FALSE,
                               CONSTRAINT uq_notification UNIQUE (user_id, tmdb_id, episode_code)
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_read    ON notifications(user_id, read);