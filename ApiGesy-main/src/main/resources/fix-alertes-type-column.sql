-- Script pour mettre à jour les ENUM de la table alertes
--
-- En MySQL, l'ENUM a une liste fixe. Quand on ajoute des valeurs dans l'enum Java,
-- il faut redéfinir l'ENUM côté MySQL pour inclure les nouvelles valeurs.
-- Ce script redéfinit les ENUM avec toutes les valeurs actuelles de Alerte.TypeAlerte et PrioriteAlerte.
--
-- Exécutez ce script dans votre base de données MySQL.

-- type : redéfinir l'ENUM avec toutes les valeurs (anciennes + nouvelles)
ALTER TABLE alertes MODIFY COLUMN type ENUM(
  'STOCK_FAIBLE',
  'FACTURE_EN_RETARD',
  'PAIEMENT_RECU',
  'VOYAGE_EN_COURS',
  'VOYAGE_CREE',
  'VOYAGE_LIVRE',
  'CLIENT_ATTRIBUE',
  'CLIENT_LIVRE',
  'VOYAGE_LIBERE',
  'VOYAGE_DECLARE',
  'ACHAT_ENREGISTRE',
  'MANQUANT_DECLARE',
  'FACTURE_EMISE',
  'MAINTENANCE_CAMION',
  'AUTRE'
) NOT NULL;

-- priorite : s'assurer que l'ENUM contient bien toutes les valeurs
ALTER TABLE alertes MODIFY COLUMN priorite ENUM(
  'BASSE',
  'MOYENNE',
  'HAUTE',
  'URGENTE'
) NOT NULL;
