-- Script pour corriger la longueur de la colonne statut dans la table voyages
-- Exécutez ce script dans votre base de données MySQL

ALTER TABLE voyages MODIFY COLUMN statut VARCHAR(50) NOT NULL;

