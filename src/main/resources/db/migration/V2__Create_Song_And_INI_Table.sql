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

  CONSTRAINT no_duplicate_section_and_key UNIQUE (section, key)
)
