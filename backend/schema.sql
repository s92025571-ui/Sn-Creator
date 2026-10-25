-- SN Creator Cloudflare D1 Schema (Zero-Billing Workers + D1 Free Tier)
-- Stores ONLY verified user UIDs, Pakistan calendar day quota counts, and short-lived idempotency reservations.
-- Never stores prompts, generated text history, emails, or personal profile details.

CREATE TABLE IF NOT EXISTS daily_quotas (
  uid TEXT NOT NULL,
  pkt_date TEXT NOT NULL,
  used_count INTEGER NOT NULL DEFAULT 0 CHECK (used_count >= 0 AND used_count <= 10),
  in_flight_count INTEGER NOT NULL DEFAULT 0 CHECK (in_flight_count >= 0 AND in_flight_count <= 10),
  updated_at INTEGER NOT NULL,
  PRIMARY KEY (uid, pkt_date)
);

CREATE TABLE IF NOT EXISTS request_reservations (
  request_id TEXT PRIMARY KEY,
  uid TEXT NOT NULL,
  pkt_date TEXT NOT NULL,
  status TEXT NOT NULL CHECK (status IN ('IN_FLIGHT', 'COMPLETED', 'ROLLED_BACK')),
  created_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_reservations_uid_date
  ON request_reservations (uid, pkt_date, status);
