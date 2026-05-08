-- ============================================================
--  V2 — Datos iniciales: roles + tags
--  El usuario admin se crea desde DataInitializer.java
--  (necesario para hashear la contraseña con BCrypt)
-- ============================================================

INSERT INTO roles (name) VALUES ('ROLE_USER');
INSERT INTO roles (name) VALUES ('ROLE_MODERATOR');
INSERT INTO roles (name) VALUES ('ROLE_ADMIN');

INSERT INTO tags (name) VALUES ('spring-boot');
INSERT INTO tags (name) VALUES ('java');
INSERT INTO tags (name) VALUES ('thymeleaf');
INSERT INTO tags (name) VALUES ('websockets');
