-- Stockage réel du document d'ordonnance (jusqu'ici seul le nom du fichier était mémorisé).
ALTER TABLE ordonnance ADD COLUMN document_contenu BYTEA;
