-- Proposal B2: speed up the four findByDifficulty queries used to build pv_list.
-- The existing UNIQUE constraint on (pv_id, edition, difficulty) is lead by pv_id,
-- so it cannot serve a WHERE difficulty = ? scan. Add a dedicated index lead by difficulty.
CREATE INDEX IF NOT EXISTS idx_diva_pv_entry_difficulty ON diva_pv_entry (difficulty);
