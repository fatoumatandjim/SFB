-- Script pour ajouter la colonne montant_manquant dans la table manquants
-- montant_manquant = quantite * prixAchat du ClientVoyage
-- À exécuter une fois dans votre base MySQL

ALTER TABLE manquants
    ADD COLUMN montant_manquant DECIMAL(19,2) NULL;

