-- Local vending-machine exercise: reset only these two tables on every MySQL startup.
-- Create the database here too because --init-file also runs during first-time setup.
CREATE DATABASE IF NOT EXISTS mydatabase CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE mydatabase;
SET NAMES utf8mb4;

-- Drop the child table first to preserve foreign-key checks.
DROP TABLE IF EXISTS purchases;
DROP TABLE IF EXISTS products;

CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    price INT NOT NULL,
    stock INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_products_price CHECK (price >= 0),
    CONSTRAINT chk_products_stock CHECK (stock >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE purchases (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price INT NOT NULL,
    purchased_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_purchases_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_purchases_quantity CHECK (quantity > 0),
    CONSTRAINT chk_purchases_unit_price CHECK (unit_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE purchases
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    ADD COLUMN canceled_at TIMESTAMP NULL,
    ADD CONSTRAINT chk_purchases_status CHECK (status IN ('COMPLETED', 'CANCELED'));

INSERT INTO products (id, name, price, stock) VALUES
    (1, '콜라', 1500, 10),
    (2, '캔커피', 1000, 5),
    (3, '생수', 800, 1),
    (4, '오렌지주스', 1800, 0);

-- purchases intentionally starts empty; learners implement the purchase operation.
