-- The `property` table was created in V2 with `id BIGINT NOT NULL` and a table
-- level PRIMARY KEY. In SQLite only a single column primary key whose declared
-- type is exactly "INTEGER" aliases the rowid, so this id was never
-- auto-assigned. PropertyEntry is mapped with GenerationType.IDENTITY, so
-- Hibernate omits the id on insert and every newly created row failed with:
--   [SQLITE_CONSTRAINT_NOTNULL] A NOT NULL constraint failed: property.id
-- Rows that migrations inserted with an explicit id (diva_news, diva_warning)
-- could still be updated, which is why only newly created settings (e.g. the
-- diva store name) were affected.
-- Rebuild the table with a proper INTEGER PRIMARY KEY AUTOINCREMENT.

CREATE TABLE property_new
(
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    property_key   VARCHAR(255) UNIQUE,
    property_value VARCHAR(255)
);

INSERT INTO property_new (id, property_key, property_value)
SELECT id, property_key, property_value
FROM property;

DROP TABLE property;

ALTER TABLE property_new
    RENAME TO property;
