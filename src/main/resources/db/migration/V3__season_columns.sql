
ALTER TABLE agri_parcel_size_stats ADD COLUMN season INTEGER;
ALTER TABLE agri_planned_targets ADD COLUMN season INTEGER;


UPDATE agri_parcel_size_stats SET season = 2026 WHERE season IS NULL;
UPDATE agri_planned_targets SET season = 2026 WHERE season IS NULL;

ALTER TABLE agri_parcel_size_stats ALTER COLUMN season SET NOT NULL;
ALTER TABLE agri_planned_targets ALTER COLUMN season SET NOT NULL;


ALTER TABLE agri_parcel_size_stats DROP CONSTRAINT agri_parcel_size_stats_unique;
ALTER TABLE agri_parcel_size_stats
    ADD CONSTRAINT agri_parcel_size_stats_unique UNIQUE (season, il, crop);

ALTER TABLE agri_planned_targets DROP CONSTRAINT agri_planned_targets_unique;
ALTER TABLE agri_planned_targets
    ADD CONSTRAINT agri_planned_targets_unique UNIQUE (season, il, crop, period);