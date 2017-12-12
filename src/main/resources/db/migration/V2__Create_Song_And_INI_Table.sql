CREATE TABLE songs (
  id            UUID PRIMARY KEY,
  file_id       UUID NOT NULL REFERENCES files,
  name          TEXT NOT NULL,
  genre         TEXT,
  artist        TEXT,
  album         TEXT,
  charter       TEXT,
  last_indexed  TIMESTAMP WITH TIME ZONE NOT NULL,
  first_indexed TIMESTAMP WITH TIME ZONE NOT NULL,

  CONSTRAINT name_genre_artist_charter_unique_index UNIQUE (name, genre, artist, charter)
);

-- Do we really want all of these indexes?
CREATE INDEX genre_index ON songs (genre);
CREATE INDEX artist_index ON songs (artist);
CREATE INDEX charter_index ON songs (charter);
CREATE INDEX name_index ON songs (name);

CREATE TABLE song_ini_entries (
  id            UUID PRIMARY KEY,
  song_id       UUID REFERENCES songs,
  section       TEXT,
  key           TEXT NOT NULL,
  value         TEXT NOT NULL,
  last_indexed  TIMESTAMP WITH TIME ZONE NOT NULL,
  first_indexed TIMESTAMP WITH TIME ZONE NOT NULL,

  CONSTRAINT song_id_section_key_unique_index UNIQUE (song_id, section, key)
);

CREATE INDEX section_key_index ON song_ini_entries (section, key);
