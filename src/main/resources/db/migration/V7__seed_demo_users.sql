-- Seed default demo accounts for quick testing and local access
INSERT INTO users (name, email, password_hash, role)
SELECT 'Admin User', 'admin@hotdrop.io', '$2a$10$sO/VysAPgESlhCUklealcuHQ494t9eaqnXD64pHTPo3AfS30x0Loa', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@hotdrop.io');

INSERT INTO users (name, email, password_hash, role)
SELECT 'Demo Buyer', 'buyer@hotdrop.io', '$2a$10$oExhLKIrGjACp9BrQLR9eu7Rv.F1GQQWuYQes3WGR4Get2UlJSmAi', 'USER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'buyer@hotdrop.io');

