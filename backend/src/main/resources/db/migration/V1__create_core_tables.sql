CREATE TABLE shops (
  id UUID PRIMARY KEY,
  shop_code VARCHAR(32) NOT NULL UNIQUE,
  display_name VARCHAR(100) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  bw_rate INTEGER NOT NULL DEFAULT 2,
  color_rate INTEGER NOT NULL DEFAULT 5,
  duplex_rate INTEGER NOT NULL DEFAULT 3,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_shop_code ON shops(shop_code);

CREATE TABLE shop_users (
  id UUID PRIMARY KEY,
  shop_id UUID REFERENCES shops(id),
  email VARCHAR(254) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  display_name VARCHAR(100) NOT NULL,
  role VARCHAR(16) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_user_email ON shop_users(email);

CREATE TABLE plans (
  id UUID PRIMARY KEY,
  code VARCHAR(32) NOT NULL UNIQUE,
  name VARCHAR(80) NOT NULL,
  description VARCHAR(240) NOT NULL,
  monthly_price_minor BIGINT NOT NULL,
  printer_limit INTEGER NOT NULL,
  daily_job_limit INTEGER NOT NULL,
  max_file_size_mb INTEGER NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE uploaded_documents (
  id UUID PRIMARY KEY,
  upload_id VARCHAR(22) NOT NULL UNIQUE,
  shop_id UUID NOT NULL REFERENCES shops(id),
  original_name VARCHAR(180) NOT NULL,
  storage_name VARCHAR(120) NOT NULL,
  job_id VARCHAR(22),
  mime_type VARCHAR(100) NOT NULL,
  file_size BIGINT NOT NULL,
  sha256 VARCHAR(64) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
  consumed BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_upload_shop_expiry ON uploaded_documents(shop_id,expires_at);

CREATE TABLE print_jobs (
  id UUID PRIMARY KEY,
  public_job_id VARCHAR(22) NOT NULL UNIQUE,
  shop_id UUID NOT NULL REFERENCES shops(id),
  file_name VARCHAR(180) NOT NULL,
  file_size BIGINT NOT NULL,
  mime_type VARCHAR(100) NOT NULL,
  sha256 VARCHAR(64),
  total_pages INTEGER NOT NULL,
  page_range VARCHAR(32) NOT NULL,
  copies INTEGER NOT NULL,
  color_mode VARCHAR(20) NOT NULL,
  duplex_mode VARCHAR(20) NOT NULL,
  paper_size VARCHAR(20) NOT NULL,
  payment_method VARCHAR(20) NOT NULL,
  status VARCHAR(32) NOT NULL,
  amount_minor BIGINT NOT NULL,
  currency VARCHAR(3) NOT NULL DEFAULT 'INR',
  idempotency_key VARCHAR(80),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  purge_after TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT uk_job_shop_idempotency UNIQUE(shop_id,idempotency_key)
);
CREATE INDEX idx_job_shop_created ON print_jobs(shop_id,created_at);
CREATE INDEX idx_job_status ON print_jobs(status);

CREATE TABLE job_events (
  id UUID PRIMARY KEY,
  job_id UUID NOT NULL REFERENCES print_jobs(id),
  event VARCHAR(32) NOT NULL,
  actor VARCHAR(40) NOT NULL,
  note VARCHAR(300),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_event_job_time ON job_events(job_id,created_at);
