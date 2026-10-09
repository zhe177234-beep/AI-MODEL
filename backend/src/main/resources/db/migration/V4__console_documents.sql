CREATE TABLE console_documents (
 id VARCHAR(40) PRIMARY KEY,
 payload TEXT NOT NULL,
 revision BIGINT NOT NULL DEFAULT 0
);
