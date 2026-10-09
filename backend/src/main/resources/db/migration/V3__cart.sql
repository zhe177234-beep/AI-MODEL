CREATE TABLE cart_items (
 buyer_id VARCHAR(36) NOT NULL REFERENCES users(id),
 product_id VARCHAR(36) NOT NULL REFERENCES products(id),
 quantity INT NOT NULL,
 PRIMARY KEY(buyer_id, product_id)
);
ALTER TABLE orders ADD COLUMN checkout_key VARCHAR(64);
CREATE INDEX idx_checkout ON orders(buyer_id,checkout_key);
