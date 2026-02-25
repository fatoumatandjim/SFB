-- Table des frais de douane par axe (Dakar: Diboli, Moussala; Abidjan: Kadiana, Zegoua)
CREATE TABLE IF NOT EXISTS frais_douane_axe (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    axe_id BIGINT NOT NULL UNIQUE,
    frais_par_litre DECIMAL(19,4) NOT NULL DEFAULT 0,
    frais_par_litre_gasoil DECIMAL(19,4) NOT NULL DEFAULT 0,
    frais_t1 DECIMAL(19,4) NOT NULL DEFAULT 0,
    CONSTRAINT fk_frais_douane_axe_axe FOREIGN KEY (axe_id) REFERENCES axes(id)
);
