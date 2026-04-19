CREATE TABLE routes (
    id SERIAL PRIMARY KEY,
    source TEXT,
    destination TEXT,
    distance DOUBLE PRECISION,
    cost DOUBLE PRECISION,
    time DOUBLE PRECISION,
    mode TEXT
);