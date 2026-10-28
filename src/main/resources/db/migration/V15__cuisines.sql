CREATE TABLE cuisines (
                          id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                          name            VARCHAR(100) NOT NULL,
                          created_at      TIMESTAMP    NOT NULL,
                          last_updated_at TIMESTAMP    NOT NULL,
                          created_by      VARCHAR(100),
                          last_updated_by VARCHAR(100)
);

CREATE UNIQUE INDEX uk_cuisines_name_lower ON cuisines (LOWER(name));

CREATE TABLE restaurant_cuisines (
                                     restaurant_id UUID NOT NULL REFERENCES restaurants (id) ON DELETE CASCADE,
                                     cuisine_id    UUID NOT NULL REFERENCES cuisines (id) ON DELETE RESTRICT,
                                     PRIMARY KEY (restaurant_id, cuisine_id)
);

CREATE INDEX idx_restaurant_cuisines_cuisine_id ON restaurant_cuisines (cuisine_id);