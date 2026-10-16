-- ============================================================================
-- V8 — Excluir o usuário exclui os restaurantes dele (Modelo de Dados v2).
--
-- Antes (V3): restaurants.user_id → users ON DELETE RESTRICT — dono com restaurante
-- não podia ser excluído. O modelo v2 pede CASCADE. Quem remove de fato é a aplicação
-- (DeleteUserUseCase apaga os restaurantes pela JPA, e o endereço de cada um sai junto,
-- o que a cascata do banco não alcança: a chave aponta para addresses). A cascata fica
-- como rede de segurança para a remoção que não passe pela aplicação.
--
-- A chave para owners (V7) continua sem cascata, de propósito: remover o perfil de dono
-- de quem tem restaurante deve ser recusado, e não apagar os restaurantes em silêncio.
-- Ao excluir o usuário direto no banco, as duas cascatas (users → owners e
-- users → restaurants) acontecem no mesmo comando, e a chave para owners, conferida no
-- fim do comando (NO ACTION), já não encontra restaurante órfão.
-- ============================================================================

ALTER TABLE restaurants DROP CONSTRAINT restaurants_user_id_fkey;

ALTER TABLE restaurants
    ADD CONSTRAINT fk_restaurants_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
