CREATE TABLE files (
  id            UUID PRIMARY KEY,
  name          TEXT NOT NULL,
  api_key       TEXT NOT NULL UNIQUE,
  parent        UUID REFERENCES files,
  file_type     TEXT NOT NULL,
  last_indexed  TIMESTAMP WITH TIME ZONE NOT NULL,
  first_indexed TIMESTAMP WITH TIME ZONE NOT NULL
);
