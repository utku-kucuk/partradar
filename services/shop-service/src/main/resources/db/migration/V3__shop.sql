-- Shops and their location. Distances are computed on the geography type (metres on the spheroid);
-- the expression index makes ST_DWithin(location::geography, ...) use the index.
CREATE TABLE shop
(
  id         UUID                     NOT NULL PRIMARY KEY,
  owner_id   TEXT                     NOT NULL,
  name       TEXT                     NOT NULL,
  phone      TEXT                     NOT NULL,
  address    TEXT                     NOT NULL,
  location   geometry(Point, 4326)    NOT NULL,
  categories TEXT[]                   NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX shop_location_geography_idx ON shop USING gist ((location::geography));
CREATE INDEX shop_categories_idx ON shop USING gin (categories);
CREATE INDEX shop_owner_idx ON shop (owner_id);
