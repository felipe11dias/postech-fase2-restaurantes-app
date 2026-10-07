CREATE TYPE "day_of_week" AS ENUM (
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY'
);

CREATE TYPE "courier_vehicle_type" AS ENUM (
  'ON_FOOT',
  'BICYCLE',
  'MOTORCYCLE',
  'CAR'
);

CREATE TYPE "courier_status" AS ENUM (
  'OFFLINE',
  'AVAILABLE',
  'BUSY'
);

CREATE TABLE "users" (
  "id" uuid PRIMARY KEY,
  "name" varchar NOT NULL,
  "email" varchar UNIQUE NOT NULL,
  "login" varchar UNIQUE NOT NULL,
  "password" varchar NOT NULL,
  "created_at" timestamp,
  "last_updated_at" timestamp,
  "created_by" varchar,
  "last_updated_by" varchar
);

CREATE TABLE "user_addresses" (
  "id" uuid PRIMARY KEY,
  "user_id" uuid NOT NULL,
  "address_id" uuid UNIQUE NOT NULL,
  "label" varchar,
  "is_default" boolean NOT NULL DEFAULT false,
  "created_at" timestamp,
  "last_updated_at" timestamp,
  "created_by" varchar,
  "last_updated_by" varchar
);

CREATE TABLE "clients" (
  "id" uuid PRIMARY KEY,
  "cpf" varchar(11) UNIQUE NOT NULL,
  "phone" varchar(20) NOT NULL,
  "birth_date" date,
  "created_at" timestamp,
  "last_updated_at" timestamp,
  "created_by" varchar,
  "last_updated_by" varchar
);

CREATE TABLE "owners" (
  "id" uuid PRIMARY KEY,
  "cnpj" varchar(14) UNIQUE NOT NULL,
  "legal_name" varchar NOT NULL,
  "business_phone" varchar(20) NOT NULL,
  "created_at" timestamp,
  "last_updated_at" timestamp,
  "created_by" varchar,
  "last_updated_by" varchar
);

CREATE TABLE "admins" (
  "id" uuid PRIMARY KEY,
  "employee_code" varchar UNIQUE NOT NULL,
  "department" varchar,
  "is_super_admin" boolean NOT NULL DEFAULT false,
  "created_at" timestamp,
  "last_updated_at" timestamp,
  "created_by" varchar,
  "last_updated_by" varchar
);

CREATE TABLE "couriers" (
  "id" uuid PRIMARY KEY,
  "cpf" varchar(11) UNIQUE NOT NULL,
  "phone" varchar(20) NOT NULL,
  "driver_license_number" varchar UNIQUE,
  "vehicle_type" courier_vehicle_type NOT NULL,
  "vehicle_plate" varchar(8),
  "status" courier_status NOT NULL DEFAULT 'OFFLINE',
  "created_at" timestamp,
  "last_updated_at" timestamp,
  "created_by" varchar,
  "last_updated_by" varchar
);

CREATE TABLE "restaurants" (
  "id" uuid PRIMARY KEY,
  "user_id" uuid NOT NULL,
  "address_id" uuid UNIQUE NOT NULL,
  "name" varchar NOT NULL,
  "created_at" timestamp,
  "last_updated_at" timestamp,
  "created_by" varchar,
  "last_updated_by" varchar
);

CREATE TABLE "restaurant_office_hours" (
  "id" uuid PRIMARY KEY,
  "restaurant_id" uuid NOT NULL,
  "day_of_week" day_of_week NOT NULL,
  "start_time" time NOT NULL,
  "end_time" time NOT NULL,
  "created_at" timestamp,
  "last_updated_at" timestamp,
  "created_by" varchar,
  "last_updated_by" varchar
);

CREATE TABLE "cuisines" (
  "id" uuid PRIMARY KEY,
  "name" varchar UNIQUE NOT NULL,
  "created_at" timestamp,
  "last_updated_at" timestamp,
  "created_by" varchar,
  "last_updated_by" varchar
);

CREATE TABLE "restaurant_cuisines" (
  "restaurant_id" uuid NOT NULL,
  "cuisine_id" uuid NOT NULL,
  PRIMARY KEY ("restaurant_id", "cuisine_id")
);

CREATE TABLE "products" (
  "id" uuid PRIMARY KEY,
  "restaurant_id" uuid NOT NULL,
  "name" varchar NOT NULL,
  "description" varchar,
  "price" double NOT NULL,
  "is_dine_in_only" boolean NOT NULL DEFAULT false
);

CREATE TABLE "product_option_groups" (
  "id" uuid PRIMARY KEY,
  "product_id" uuid NOT NULL,
  "name" varchar NOT NULL,
  "is_required" boolean NOT NULL DEFAULT false,
  "min_selections" integer NOT NULL DEFAULT 0,
  "max_selections" integer NOT NULL DEFAULT 1,
  "created_at" timestamp,
  "last_updated_at" timestamp,
  "created_by" varchar,
  "last_updated_by" varchar
);

CREATE TABLE "product_option_values" (
  "id" uuid PRIMARY KEY,
  "option_group_id" uuid NOT NULL,
  "name" varchar NOT NULL,
  "additional_price" numeric(10,2) NOT NULL DEFAULT 0,
  "created_at" timestamp,
  "last_updated_at" timestamp,
  "created_by" varchar,
  "last_updated_by" varchar
);

CREATE TABLE "images" (
  "id" uuid PRIMARY KEY,
  "product_id" uuid NOT NULL,
  "content" blob NOT NULL
);

CREATE TABLE "addresses" (
  "id" uuid PRIMARY KEY,
  "city_id" uuid NOT NULL,
  "street" varchar,
  "number" varchar,
  "complement" varchar,
  "neighborhood" varchar,
  "zip_code" varchar
);

CREATE TABLE "cities" (
  "id" uuid PRIMARY KEY,
  "state_id" uuid NOT NULL,
  "name" varchar
);

CREATE TABLE "states" (
  "id" uuid PRIMARY KEY,
  "name" varchar,
  "acronym" varchar
);

CREATE TABLE "password_reset_tokens" (
  "id" uuid PRIMARY KEY,
  "user_id" uuid UNIQUE NOT NULL,
  "token_hash" varchar UNIQUE NOT NULL,
  "expires_at" timestamp,
  "used" boolean
);

CREATE INDEX ON "user_addresses" ("user_id");

CREATE UNIQUE INDEX ON "restaurant_office_hours" ("restaurant_id", "day_of_week", "start_time");

CREATE INDEX ON "restaurant_cuisines" ("cuisine_id");

COMMENT ON COLUMN "user_addresses"."label" IS 'Ex.: Casa, Trabalho';

COMMENT ON COLUMN "couriers"."driver_license_number" IS 'Obrigatório apenas para MOTORCYCLE e CAR';

COMMENT ON COLUMN "couriers"."vehicle_plate" IS 'Obrigatório apenas para MOTORCYCLE e CAR';

ALTER TABLE "restaurant_cuisines" ADD FOREIGN KEY ("restaurant_id") REFERENCES "restaurants" ("id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "restaurant_cuisines" ADD FOREIGN KEY ("cuisine_id") REFERENCES "cuisines" ("id") ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "restaurant_office_hours" ADD FOREIGN KEY ("restaurant_id") REFERENCES "restaurants" ("id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "user_addresses" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "addresses" ADD FOREIGN KEY ("id") REFERENCES "user_addresses" ("address_id") ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "addresses" ADD FOREIGN KEY ("id") REFERENCES "restaurants" ("address_id") ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "addresses" ADD FOREIGN KEY ("city_id") REFERENCES "cities" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "cities" ADD FOREIGN KEY ("state_id") REFERENCES "states" ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "products" ADD FOREIGN KEY ("restaurant_id") REFERENCES "restaurants" ("id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "product_option_groups" ADD FOREIGN KEY ("product_id") REFERENCES "products" ("id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "product_option_values" ADD FOREIGN KEY ("option_group_id") REFERENCES "product_option_groups" ("id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "images" ADD FOREIGN KEY ("product_id") REFERENCES "products" ("id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "restaurants" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "owners" ADD FOREIGN KEY ("id") REFERENCES "users" ("id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "clients" ADD FOREIGN KEY ("id") REFERENCES "users" ("id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "admins" ADD FOREIGN KEY ("id") REFERENCES "users" ("id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "couriers" ADD FOREIGN KEY ("id") REFERENCES "users" ("id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "password_reset_tokens" ADD FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;
