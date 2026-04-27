ALTER TABLE users
    ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER';

INSERT INTO users (email, password_hash, name, role)
VALUES ('admin@seritracker.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'Admin', 'USER')
    ON CONFLICT (email) DO NOTHING;