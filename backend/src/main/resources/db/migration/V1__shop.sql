CREATE TABLE users (
 id VARCHAR(36) PRIMARY KEY,
 username VARCHAR(64) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL,
 role VARCHAR(16) NOT NULL
);
CREATE TABLE sessions (
 token_hash VARCHAR(64) PRIMARY KEY,
 user_id VARCHAR(36) NOT NULL REFERENCES users(id),
 expires_at BIGINT NOT NULL
);
CREATE TABLE products (
 id VARCHAR(36) PRIMARY KEY,
 name VARCHAR(100) NOT NULL,
 description VARCHAR(1000) NOT NULL,
 price_cents INT NOT NULL,
 stock INT NOT NULL
);
CREATE TABLE orders (
 id VARCHAR(36) PRIMARY KEY,
 buyer_id VARCHAR(36) NOT NULL REFERENCES users(id),
 product_id VARCHAR(36) NOT NULL REFERENCES products(id),
 quantity INT NOT NULL,
 total_cents INT NOT NULL,
 status VARCHAR(24) NOT NULL,
 address VARCHAR(500) NOT NULL,
 tracking VARCHAR(100),
 created_at BIGINT NOT NULL,
 request_key VARCHAR(64) NOT NULL,
 UNIQUE(buyer_id, request_key)
);
CREATE TABLE refunds (
 id VARCHAR(36) PRIMARY KEY,
 order_id VARCHAR(36) NOT NULL UNIQUE REFERENCES orders(id),
 reason VARCHAR(1000) NOT NULL,
 status VARCHAR(24) NOT NULL,
 created_at BIGINT NOT NULL
);
CREATE TABLE conversations (
 id VARCHAR(36) PRIMARY KEY,
 buyer_id VARCHAR(36) NOT NULL UNIQUE REFERENCES users(id),
 mode VARCHAR(16) NOT NULL DEFAULT 'ASSISTED',
 revision BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE messages (
 id VARCHAR(36) PRIMARY KEY,
 conversation_id VARCHAR(36) NOT NULL REFERENCES conversations(id),
 author VARCHAR(16) NOT NULL,
 content VARCHAR(4000) NOT NULL,
 status VARCHAR(16) NOT NULL,
 created_at BIGINT NOT NULL
);
CREATE TABLE knowledge (
 id VARCHAR(36) PRIMARY KEY,
 title VARCHAR(150) NOT NULL,
 content VARCHAR(3000) NOT NULL
);
CREATE TABLE audit (
 id VARCHAR(36) PRIMARY KEY,
 actor_id VARCHAR(36) NOT NULL,
 action VARCHAR(100) NOT NULL,
 entity_id VARCHAR(100) NOT NULL,
 detail VARCHAR(4000) NOT NULL,
 created_at BIGINT NOT NULL
);
INSERT INTO products VALUES ('p-headphones','轻音无线耳机','蓝牙5.3，USB-C充电，续航约30小时；演示商品。',12900,50);
INSERT INTO products VALUES ('p-keyboard','轻巧机械键盘','87键，有线连接，茶轴；演示商品。',19900,30);
INSERT INTO products VALUES ('p-lamp','护眼学习台灯','三档色温，五档亮度，USB供电；演示商品。',8900,100);
INSERT INTO knowledge VALUES ('k-shipping','配送说明','演示商城：模拟支付完成后由客服模拟发货。物流编号为模拟数据，不代表真实配送。');
INSERT INTO knowledge VALUES ('k-refund','售后说明','演示商城：已模拟支付或已模拟发货的订单可以提交售后申请；商家核实后人工审批。退款仅改变模拟订单状态，不产生资金转移。');
