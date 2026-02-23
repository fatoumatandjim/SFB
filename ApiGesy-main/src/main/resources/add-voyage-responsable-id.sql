-- Ajout de la colonne responsable_id à la table voyages (référence vers comptes)
-- Exécutez ce script une fois dans votre base de données

ALTER TABLE voyages ADD COLUMN responsable_id BIGINT NULL;
ALTER TABLE voyages ADD CONSTRAINT fk_voyage_responsable
  FOREIGN KEY (responsable_id) REFERENCES comptes(id);
