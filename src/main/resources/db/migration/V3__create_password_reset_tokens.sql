CREATE TABLE password_reset_tokens (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                       token VARCHAR(255) NOT NULL UNIQUE,
                                       expiration TIMESTAMP NOT NULL,
                                       usuario_id UUID NOT NULL,
                                       FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_tokens_expiration ON password_reset_tokens(expiration);
CREATE INDEX idx_password_reset_tokens_usuario ON password_reset_tokens(usuario_id);