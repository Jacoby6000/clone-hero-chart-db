CREATE TABLE files (
  id            UUID PRIMARY KEY,
  parent_id     UUID REFERENCES files,
  name          TEXT NOT NULL,
  api_key       TEXT NOT NULL UNIQUE,
  file_type     TEXT NOT NULL,
  last_indexed  TIMESTAMP WITH TIME ZONE NOT NULL,
  first_indexed TIMESTAMP WITH TIME ZONE NOT NULL
);
