-- Tabla de usuarios
CREATE TABLE users (
                       id            BIGSERIAL PRIMARY KEY,
                       email         VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       name          VARCHAR(100),
                       created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Tabla de series del usuario
CREATE TABLE user_series (
                             id               BIGSERIAL PRIMARY KEY,
                             user_id          BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             tmdb_id          INTEGER     NOT NULL,
                             title            VARCHAR(255) NOT NULL,
                             poster_url       TEXT,
                             status           VARCHAR(20) NOT NULL DEFAULT 'WANT_TO_WATCH',
                             rating           INTEGER     CHECK (rating BETWEEN 1 AND 10),
                             watched_episodes INTEGER     NOT NULL DEFAULT 0,
                             total_episodes   INTEGER     NOT NULL DEFAULT 0,
                             network          VARCHAR(100),
                             notes            TEXT,
                             created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                             updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                             CONSTRAINT uq_user_series UNIQUE (user_id, tmdb_id)
);

-- Índices para mejorar performance
CREATE INDEX idx_user_series_user_id ON user_series(user_id);
CREATE INDEX idx_user_series_status  ON user_series(user_id, status);