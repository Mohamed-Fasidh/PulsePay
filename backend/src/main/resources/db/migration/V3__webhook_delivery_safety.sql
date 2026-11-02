ALTER TABLE webhook_endpoints ADD COLUMN secret_encrypted TEXT;
ALTER TABLE webhook_deliveries ADD CONSTRAINT webhook_deliveries_endpoint_event_unique UNIQUE (webhook_endpoint_id, event_id);

