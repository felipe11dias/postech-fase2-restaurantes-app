-- ============================================================================
-- V5 — Um token de redefinição de senha por usuário (Modelo de Dados v2).
--
-- Antes, cada "esqueci minha senha" inseria um token novo, e um usuário podia
-- acumular vários. Agora o pedido novo reemite o token existente (hash, validade
-- e uso novos), e password_reset_tokens.user_id é único.
-- ============================================================================

-- 1. Fica só o token mais recente de cada usuário: o de validade mais distante
--    (todos nascem com a mesma duração, então é o último emitido); empate pelo
--    id, para o critério ser determinístico. Os demais apenas deixam de valer —
--    token é efêmero, e o usuário pode pedir outro a qualquer momento.
DELETE FROM password_reset_tokens t
USING password_reset_tokens mais_recente
WHERE mais_recente.user_id = t.user_id
  AND (mais_recente.expires_at, mais_recente.id) > (t.expires_at, t.id);

-- 2. A restrição única tem o próprio índice; o índice simples de V1 fica redundante.
DROP INDEX idx_reset_tokens_user_id;
ALTER TABLE password_reset_tokens ADD CONSTRAINT uk_password_reset_tokens_user_id UNIQUE (user_id);
