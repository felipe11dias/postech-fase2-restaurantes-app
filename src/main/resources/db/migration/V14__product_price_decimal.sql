-- Mantém a precisão monetária em centavos e converte os preços já cadastrados.
ALTER TABLE products
    ALTER COLUMN price TYPE numeric(10, 2) USING price::numeric(10, 2);
