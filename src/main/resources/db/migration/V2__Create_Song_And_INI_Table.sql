CREATE TABLE songs (
  id            UUID PRIMARY KEY,
  file_id       UUID NOT NULL REFERENCES files,
  name          TEXT NOT NULL,
  last_indexed  TIMESTAMP WITH TIME ZONE NOT NULL,
  first_indexed TIMESTAMP WITH TIME ZONE NOT NULL
);

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
