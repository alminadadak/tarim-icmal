-- H2 NOTU: WITH (CTE) syntax olarak H2'de destekleniyor ve ilk calistirmada
-- sorunsuz calisiyor, ANCAK H2'nin CTE'li view'leri dahili olarak saklama
-- sekli bir motor hatasina yol aciyor: veritabani dosyasi KAPATILIP TEKRAR
-- ACILDIGINDA "View already exists" hatasi veriyor (H2 2.2.224). Bu yuzden
-- CTE yerine duz subquery kullaniyoruz - islevsel olarak birebir ayni.

DROP VIEW IF EXISTS v_icmal;

CREATE VIEW v_icmal AS
SELECT
    k.il,
    k.urun,
    k.sezon,
    k.donem,
    COALESCE(ps.parcels_under_4da, 0)      AS parsel_4da_alti,
    COALESCE(ps.parcels_over_4da, 0)       AS parsel_4da_ustu,
    COALESCE(pt.planned_count, 0)          AS planlanan,
    COALESCE(fs.field_count, 0)            AS referans_parsel,
    COALESCE(fs.referenced_area, 0)        AS referans_alan,
    COALESCE(fs.referenced_field_count, 0) AS nihai_referans,
    COALESCE(fs.referenced_area, 0)        AS nihai_alan,
    CASE
        WHEN pt.planned_count IS NULL OR pt.planned_count = 0 THEN NULL
        -- PERFORMANS DUZELTMESI: NUMERIC/DECFLOAT bolme islemi H2 2.2.224'te
        -- son derece yavas (35 saniye/2000 satir olculdu!). DOUBLE'a CAST
        -- ederek native kayan noktali aritmetige geciyoruz - yuzde gostermek
        -- icin hassasiyet kaybi onemsiz.
        ELSE ROUND(100.0 * CAST(COALESCE(fs.referenced_field_count, 0) AS DOUBLE) / CAST(pt.planned_count AS DOUBLE), 1)
    END                                    AS tamamlanan_yuzde,
    COALESCE(fs.updated_at, pt.updated_at, ps.updated_at) AS son_guncelleme
FROM (
    SELECT season AS sezon, il, crop AS urun, period AS donem
    FROM agri_planned_targets
    UNION
    SELECT season AS sezon, name AS il,
        CASE WHEN crop_group = 'KANOLA / KOLZA' THEN 'KANOLA' ELSE crop_group END AS urun,
        'Tüm' AS donem
    FROM agri_field_stats
    WHERE level = 'İl'
    UNION
    SELECT season AS sezon, il, crop AS urun, 'Tüm' AS donem
    FROM agri_parcel_size_stats
) k
LEFT JOIN agri_planned_targets pt
    ON pt.season = k.sezon AND pt.il = k.il AND pt.crop = k.urun AND pt.period = k.donem
LEFT JOIN agri_field_stats fs
    ON fs.season = k.sezon AND fs.name = k.il
    AND (CASE WHEN fs.crop_group = 'KANOLA / KOLZA' THEN 'KANOLA' ELSE fs.crop_group END) = k.urun
    AND fs.level = 'İl'
LEFT JOIN agri_parcel_size_stats ps
    ON ps.season = k.sezon AND ps.il = k.il AND ps.crop = k.urun
WHERE k.urun NOT IN ('DİĞER YAZLIK', 'DİĞER KIŞLIK');