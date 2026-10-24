-- V13 — Catálogo de produtos, referências de imagens e opções do cardápio.

CREATE TABLE products (
    id uuid PRIMARY KEY,
    restaurant_id uuid NOT NULL REFERENCES restaurants (id) ON DELETE CASCADE,
    name varchar(150) NOT NULL,
    description varchar(1000),
    price double precision NOT NULL CHECK (price >= 0),
    is_dine_in_only boolean NOT NULL DEFAULT false
);

CREATE INDEX ix_products_restaurant_id ON products (restaurant_id);

CREATE TABLE images (
    id uuid PRIMARY KEY,
    product_id uuid NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    content varchar(2048) NOT NULL
);

CREATE TABLE product_option_groups (
    id uuid PRIMARY KEY,
    product_id uuid NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    name varchar(150) NOT NULL,
    is_required boolean NOT NULL DEFAULT false,
    min_selections integer NOT NULL DEFAULT 0 CHECK (min_selections >= 0),
    max_selections integer NOT NULL DEFAULT 1 CHECK (max_selections >= 1),
    created_at timestamp NOT NULL,
    last_updated_at timestamp NOT NULL,
    created_by varchar,
    last_updated_by varchar,
    CONSTRAINT ck_product_option_group_selection_range
        CHECK (min_selections <= max_selections AND (NOT is_required OR min_selections >= 1))
);

CREATE TABLE product_option_values (
    id uuid PRIMARY KEY,
    option_group_id uuid NOT NULL REFERENCES product_option_groups (id) ON DELETE CASCADE,
    name varchar(150) NOT NULL,
    additional_price numeric(10, 2) NOT NULL DEFAULT 0 CHECK (additional_price >= 0),
    created_at timestamp NOT NULL,
    last_updated_at timestamp NOT NULL,
    created_by varchar,
    last_updated_by varchar
);
