-- V3: agri_parcel_size_stats ve agri_planned_targets tablolarina 'season'
-- kolonu ekliyoruz. Boylece ayni il+urun kombinasyonu farkli sezonlarda
-- ayri ayri saklanabilecek (su ana kadar tek, "guncel" sezon varsayimiyla
-- calisiyorduk).

-- H2'YE GECIS NOTU: Bu dosyada hicbir tip/fonksiyon degisikligi gerekmedi,
-- ALTER TABLE / ADD CONSTRAINT / DROP CONSTRAINT ifadeleri H2'de de ayni
-- calisiyor.

-- 1) Once kolonu NULL izinli ekliyoruz (mevcut satirlarda deger yok cunku)
ALTER TABLE agri_parcel_size_stats ADD COLUMN season INTEGER;
ALTER TABLE agri_planned_targets ADD COLUMN season INTEGER;

-- 2) Su ana kadar yuklenmis, sezon bilgisi olmayan kayitlari 2026 olarak
--    isaretliyoruz (bot'un su anki calisma sezonu) - boylece mevcut
--    verilerin kaybolmasini/anlamsizlasmasini onluyoruz.
UPDATE agri_parcel_size_stats SET season = 2026 WHERE season IS NULL;
UPDATE agri_planned_targets SET season = 2026 WHERE season IS NULL;

-- 3) Artik butun satirlarda deger oldugu icin kolonu zorunlu yapabiliriz
ALTER TABLE agri_parcel_size_stats ALTER COLUMN season SET NOT NULL;
ALTER TABLE agri_planned_targets ALTER COLUMN season SET NOT NULL;

-- 4) Eski (sezonsuz) benzersizlik kurallarini kaldirip, sezonu da iceren
--    yenilerini ekliyoruz.
ALTER TABLE agri_parcel_size_stats DROP CONSTRAINT agri_parcel_size_stats_unique;
ALTER TABLE agri_parcel_size_stats
    ADD CONSTRAINT agri_parcel_size_stats_unique UNIQUE (season, il, crop);

ALTER TABLE agri_planned_targets DROP CONSTRAINT agri_planned_targets_unique;
ALTER TABLE agri_planned_targets
    ADD CONSTRAINT agri_planned_targets_unique UNIQUE (season, il, crop, period);