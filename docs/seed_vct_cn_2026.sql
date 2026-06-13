-- ============================================================
-- VCT CN 2026 Seed Data for egg-esports
-- ============================================================
-- Inserts 8 upcoming matches across Stage 1 and Stage 2,
-- featuring all 8 VCT CN 2026 teams.
--
-- Run:
--   mysql -u admin -p egg_esports < seed_vct_cn_2026.sql
-- ============================================================

INSERT INTO egg_match (game_id, title, team_a, team_b, tournament, match_time, status) VALUES (1, 'VCT CN Stage 1 — EDG vs FPX', 'EDG', 'FPX', 'VCT CN 2026 Stage 1', '2026-06-15 17:00:00', 0);
INSERT INTO egg_match (game_id, title, team_a, team_b, tournament, match_time, status) VALUES (1, 'VCT CN Stage 1 — BLG vs TEC', 'BLG', 'TEC', 'VCT CN 2026 Stage 1', '2026-06-15 19:00:00', 0);
INSERT INTO egg_match (game_id, title, team_a, team_b, tournament, match_time, status) VALUES (1, 'VCT CN Stage 1 — JDG vs DRG', 'JDG', 'DRG', 'VCT CN 2026 Stage 1', '2026-06-16 17:00:00', 0);
INSERT INTO egg_match (game_id, title, team_a, team_b, tournament, match_time, status) VALUES (1, 'VCT CN Stage 1 — TYL vs WOL', 'TYL', 'WOL', 'VCT CN 2026 Stage 1', '2026-06-16 19:00:00', 0);
INSERT INTO egg_match (game_id, title, team_a, team_b, tournament, match_time, status) VALUES (1, 'VCT CN Stage 1 — EDG vs BLG', 'EDG', 'BLG', 'VCT CN 2026 Stage 1', '2026-06-17 17:00:00', 0);
INSERT INTO egg_match (game_id, title, team_a, team_b, tournament, match_time, status) VALUES (1, 'VCT CN Stage 2 — FPX vs JDG', 'FPX', 'JDG', 'VCT CN 2026 Stage 2', '2026-06-22 17:00:00', 0);
INSERT INTO egg_match (game_id, title, team_a, team_b, tournament, match_time, status) VALUES (1, 'VCT CN Stage 2 — DRG vs TYL', 'DRG', 'TYL', 'VCT CN 2026 Stage 2', '2026-06-22 19:00:00', 0);
INSERT INTO egg_match (game_id, title, team_a, team_b, tournament, match_time, status) VALUES (1, 'VCT CN Stage 2 — TEC vs WOL', 'TEC', 'WOL', 'VCT CN 2026 Stage 2', '2026-06-23 17:00:00', 0);
