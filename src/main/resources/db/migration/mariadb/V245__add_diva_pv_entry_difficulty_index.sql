-- Proposal B2: speed up the four findByDifficulty queries used to build pv_list.
-- The existing UNIQUE constraint on (pv_id, difficulty, edition) is lead by pv_id,
-- so it cannot serve a WHERE difficulty = ? scan. Add a dedicated index lead by difficulty.
CREATE INDEX idx_diva_pv_entry_difficulty ON diva_pv_entry (difficulty);
