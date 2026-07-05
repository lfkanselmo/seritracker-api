CREATE TABLE episode_watch (
                               id             BIGSERIAL PRIMARY KEY,
                               user_series_id BIGINT      NOT NULL REFERENCES user_series(id) ON DELETE CASCADE,
                               season_number  INTEGER     NOT NULL,
                               episode_number INTEGER     NOT NULL,
                               watched_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               CONSTRAINT uq_episode_watch UNIQUE (user_series_id, season_number, episode_number)
);

CREATE INDEX idx_episode_watch_user_series_id ON episode_watch(user_series_id);
