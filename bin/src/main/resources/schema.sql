CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    price DECIMAL(10, 2),
    status VARCHAR(50)
);