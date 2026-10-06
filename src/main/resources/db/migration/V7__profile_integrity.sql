-- ============================================================================
-- V7 — Integridade dos perfis garantida no banco, e não só conferida pela aplicação.
--
-- A aplicação já confere as duas regras abaixo antes de gravar; mas conferir e gravar
-- são passos separados, e duas requisições simultâneas podem passar pela conferência
-- juntas. Quem decide o empate é o banco (Date: a integridade é declarada no schema).
-- ============================================================================

-- 1. O CPF é da pessoa: o mesmo CPF não pode estar em dois usuários, nem quando um o
--    tem como cliente e o outro como entregador. A unicidade de cada tabela não cobre
--    isso (são duas tabelas), então um gatilho confere a outra. A trava consultiva por
--    CPF serializa as gravações do mesmo CPF: a segunda espera a primeira terminar e,
--    na conferência, já vê o que ela gravou. A violação sai como unique_violation —
--    a mesma recusa (409) da unicidade comum.
CREATE FUNCTION cpf_de_uma_so_pessoa() RETURNS trigger AS $$
BEGIN
    PERFORM pg_advisory_xact_lock(hashtext('cpf:' || NEW.cpf));
    IF EXISTS (SELECT 1 FROM clients WHERE cpf = NEW.cpf AND id <> NEW.id)
       OR EXISTS (SELECT 1 FROM couriers WHERE cpf = NEW.cpf AND id <> NEW.id) THEN
        RAISE EXCEPTION 'CPF já pertence a outro usuário' USING ERRCODE = 'unique_violation';
    END IF;
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_clients_cpf_de_uma_so_pessoa
    BEFORE INSERT OR UPDATE OF cpf ON clients
    FOR EACH ROW EXECUTE FUNCTION cpf_de_uma_so_pessoa();

CREATE TRIGGER tg_couriers_cpf_de_uma_so_pessoa
    BEFORE INSERT OR UPDATE OF cpf ON couriers
    FOR EACH ROW EXECUTE FUNCTION cpf_de_uma_so_pessoa();

-- 2. O dono do restaurante tem perfil de dono. Além da chave para users (V3), o
--    restaurante referencia owners: remover o perfil de dono de quem tem restaurante
--    passa a ser recusado pelo banco, mesmo que a conferência da aplicação e a criação
--    de um restaurante aconteçam ao mesmo tempo. Restaurante antigo cujo dono não tem o
--    perfil (o administrador servia de dono até a Etapa 21) impede a migração, com a
--    contagem — a migration não inventa perfil.
DO $$
DECLARE
    sem_dono INTEGER;
BEGIN
    SELECT count(*) INTO sem_dono
    FROM restaurants r
    WHERE NOT EXISTS (SELECT 1 FROM owners o WHERE o.id = r.user_id);
    IF sem_dono > 0 THEN
        RAISE EXCEPTION 'V7: % restaurante(s) de usuário sem perfil de dono. Inclua o perfil de dono '
            'desses usuários ou transfira os restaurantes antes de migrar.', sem_dono;
    END IF;
END $$;

ALTER TABLE restaurants
    ADD CONSTRAINT fk_restaurants_owner FOREIGN KEY (user_id) REFERENCES owners (id);
