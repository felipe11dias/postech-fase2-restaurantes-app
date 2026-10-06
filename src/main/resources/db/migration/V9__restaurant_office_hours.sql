-- ============================================================================
-- V9 — Horário de funcionamento por dia da semana (Modelo de Dados v2).
--
-- Antes: um horário só (restaurants.office_hour_start/end), o mesmo todos os dias.
-- Depois: restaurant_office_hours, um intervalo por linha, com o dia da semana. O
-- horário por dia é um atributo multivalorado, e por isso ganha tabela própria (1FN).
-- ============================================================================

-- 1. O enum do modelo, com os mesmos valores de java.time.DayOfWeek.
CREATE TYPE day_of_week AS ENUM ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY');

-- 2. A tabela. Fechamento antes da abertura quer dizer que o expediente vira a meia-noite;
--    abertura igual ao fechamento não é intervalo. Sobreposição entre linhas (inclusive a que
--    vira a meia-noite e invade o dia seguinte) é regra do agregado, conferida pela aplicação.
CREATE TABLE restaurant_office_hours (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id   UUID         NOT NULL REFERENCES restaurants (id) ON DELETE CASCADE,
    day_of_week     day_of_week  NOT NULL,
    start_time      TIME         NOT NULL,
    end_time        TIME         NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    last_updated_at TIMESTAMP    NOT NULL,
    created_by      VARCHAR(100),
    last_updated_by VARCHAR(100),
    CONSTRAINT uk_restaurant_office_hours_start UNIQUE (restaurant_id, day_of_week, start_time),
    CONSTRAINT ck_restaurant_office_hours_interval CHECK (start_time <> end_time)
);

-- 3. O horário de hoje vale todos os dias: cada restaurante ganha sete linhas, uma por dia,
--    com o mesmo início e fim — o comportamento continua o mesmo depois da migração.
INSERT INTO restaurant_office_hours (restaurant_id, day_of_week, start_time, end_time,
                                     created_at, last_updated_at, created_by, last_updated_by)
SELECT r.id, d.dia, r.office_hour_start, r.office_hour_end,
       r.created_at, r.last_updated_at, r.created_by, r.last_updated_by
FROM restaurants r
CROSS JOIN unnest(enum_range(NULL::day_of_week)) AS d(dia);

-- 4. As colunas do horário único saem.
ALTER TABLE restaurants DROP COLUMN office_hour_start, DROP COLUMN office_hour_end;
