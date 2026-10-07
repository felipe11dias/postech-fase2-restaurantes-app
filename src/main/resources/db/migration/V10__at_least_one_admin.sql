-- ============================================================================
-- V10 — O sistema não fica sem administrador, garantido no banco.
--
-- A aplicação recusa remover o perfil de administrador do último administrador e
-- excluir o cadastro dele (LastAdminPolicy). Mas ela conta os administradores e só
-- depois grava: dois administradores que se excluem (ou tiram o próprio perfil) ao
-- mesmo tempo passariam os dois pela contagem, e o sistema ficaria sem nenhum — sem
-- ninguém que pudesse conceder o perfil de novo. Quem decide o empate é o banco, como
-- na V7 (Date: a integridade é declarada no schema).
--
-- A trava consultiva serializa as remoções de administrador: a segunda espera a
-- primeira terminar e, na conferência (cada comando do gatilho vê o que já foi
-- confirmado), já não encontra a linha que ela apagou. A recusa sai como violação de
-- integridade — o mesmo 409 das outras disputas decididas pelo banco. A exclusão em
-- cascata a partir de users também passa por aqui.
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM admins) THEN
        RAISE EXCEPTION 'V10: nenhum administrador cadastrado. Inclua o perfil de administrador '
            'de algum usuário antes de migrar.';
    END IF;
END $$;

CREATE FUNCTION ao_menos_um_administrador() RETURNS trigger AS $$
BEGIN
    PERFORM pg_advisory_xact_lock(hashtext('admins'));
    IF NOT EXISTS (SELECT 1 FROM admins) THEN
        RAISE EXCEPTION 'O último administrador não pode deixar de existir'
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;
    RETURN NULL;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_admins_ao_menos_um
    AFTER DELETE ON admins
    FOR EACH ROW EXECUTE FUNCTION ao_menos_um_administrador();
