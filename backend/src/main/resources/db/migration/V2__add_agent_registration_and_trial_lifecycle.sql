ALTER TABLE shops ADD COLUMN IF NOT EXISTS phone VARCHAR(32);
ALTER TABLE shops ADD COLUMN IF NOT EXISTS printer_brand VARCHAR(100);
ALTER TABLE shops ADD COLUMN IF NOT EXISTS subscription_status VARCHAR(24) NOT NULL DEFAULT 'TRIAL';
ALTER TABLE shops ADD COLUMN IF NOT EXISTS trial_started_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE shops ADD COLUMN IF NOT EXISTS trial_ends_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE shops ADD COLUMN IF NOT EXISTS agent_secret_hash VARCHAR(100);
ALTER TABLE shops ADD COLUMN IF NOT EXISTS agent_secret_created_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE agent_devices (
  id UUID PRIMARY KEY,
  shop_id UUID NOT NULL REFERENCES shops(id),
  fingerprint VARCHAR(128) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  agent_version VARCHAR(40),
  hostname VARCHAR(120),
  platform VARCHAR(40),
  os_platform VARCHAR(180),
  ip_address VARCHAR(64),
  device_meta VARCHAR(4000),
  last_seen_at TIMESTAMP WITH TIME ZONE,
  approved_at TIMESTAMP WITH TIME ZONE,
  revoked_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT uk_device_shop_fingerprint UNIQUE(shop_id,fingerprint)
);
CREATE INDEX idx_device_shop_status ON agent_devices(shop_id,status);
CREATE INDEX idx_device_last_seen ON agent_devices(last_seen_at);

CREATE TABLE device_audit_events (
  id UUID PRIMARY KEY,
  device_id UUID NOT NULL REFERENCES agent_devices(id),
  event VARCHAR(32) NOT NULL,
  actor VARCHAR(80) NOT NULL,
  note VARCHAR(300),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_device_audit_device_time ON device_audit_events(device_id,created_at);
