package com.almina.tarimicmal.dataimport;

import com.almina.tarimicmal.report.GoogleSheetsService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ParcelImportService {

    private static final Pattern KOLON_DESENI =
            Pattern.compile("^(.*?)\\s*4\\s*DA\\s*(ALTI|ÜSTÜ)$", Pattern.CASE_INSENSITIVE);

    private static final Map<String, String> URUN_NORMALIZASYON = Map.of(
        "KANOLA / KOLZA", "KANOLA"
    );

    private final JdbcTemplate jdbcTemplate;
    private final GoogleSheetsService googleSheetsService;

    public ParcelImportService(JdbcTemplate jdbcTemplate, GoogleSheetsService googleSheetsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.googleSheetsService = googleSheetsService;
    }

    private String normalizeUrun(String urun) {
        return URUN_NORMALIZASYON.getOrDefault(urun, urun);
    }

    public ImportResult importFromCloud(String spreadsheetId, int sezon) throws Exception {
        List<String> uyarilar = new ArrayList<>();
        List<Object[]> yaziliSatirlar = new ArrayList<>();

        List<List<Object>> rows = googleSheetsService.readSheetData(spreadsheetId, "CKS_parsel");

        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("'CKS_parsel' sayfasında veri bulunamadı.");
        }

        List<Object> basliklar = rows.get(0);
        if (basliklar.isEmpty()) {
            throw new IllegalArgumentException("İlk satır (başlıklar) boş görünüyor.");
        }

        Map<Integer, String[]> kolonEslesme = new HashMap<>(); 
        List<String> eslesmeyenKolonlar = new ArrayList<>();

        for (int i = 0; i < basliklar.size(); i++) {
            String baslik = getHucre(basliklar, i);
            if (baslik.isEmpty()) continue;

            Matcher m = KOLON_DESENI.matcher(baslik);
            if (m.matches()) {
                String urun = normalizeUrun(m.group(1).trim());
                String metrik = m.group(2).toUpperCase();
                kolonEslesme.put(i, new String[]{urun, metrik});
            } else if (i != 0) {
                eslesmeyenKolonlar.add(baslik);
            }
        }

        if (!eslesmeyenKolonlar.isEmpty()) {
            uyarilar.add("Şu kolonlar 'ÜRÜN 4 DA ALTI/ÜSTÜ' formatına uymadı, atlandı: " + String.join(", ", eslesmeyenKolonlar));
        }

        OffsetDateTime simdi = OffsetDateTime.now();

        for (int rowIdx = 1; rowIdx < rows.size(); rowIdx++) {
            List<Object> row = rows.get(rowIdx);
            if (row.isEmpty()) continue;

            String il = getHucre(row, 0);
            if (il.isEmpty()) continue;

            Map<String, Map<String, Double>> urunDegerleri = new HashMap<>();

            for (Map.Entry<Integer, String[]> entry : kolonEslesme.entrySet()) {
                int colIdx = entry.getKey();
                String urun = entry.getValue()[0];
                String metrik = entry.getValue()[1];

                Double deger = sayiyaCevirVeyaNull(row, colIdx, il, urun, metrik, uyarilar);

                urunDegerleri.computeIfAbsent(urun, k -> new HashMap<>()).put(metrik, deger);
            }

            for (Map.Entry<String, Map<String, Double>> entry : urunDegerleri.entrySet()) {
                String urun = entry.getKey();
                Double alti = entry.getValue().get("ALTI");
                Double ustu = entry.getValue().get("ÜSTÜ");
                
                if (alti != null || ustu != null) { 
                    yaziliSatirlar.add(new Object[]{sezon, il, urun, alti, ustu, simdi});
                }
            }
        }


        String sql = """
                MERGE INTO agri_parcel_size_stats (season, il, crop, parcels_under_4da, parcels_over_4da, updated_at)
                KEY (season, il, crop)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.batchUpdate(sql, yaziliSatirlar);

        return new ImportResult(yaziliSatirlar.size(), uyarilar);
    }

    private String getHucre(List<Object> row, int index) {
        if (index >= row.size() || row.get(index) == null) return "";
        return row.get(index).toString().trim();
    }

    private Double sayiyaCevirVeyaNull(List<Object> row, int index, String il, String urun, String metrik, List<String> uyarilar) {
        String metin = getHucre(row, index);
        if (metin.isEmpty()) return null;

        try {
            String temizMetin = metin.replace(" ", "").replace(",", ".");
            return Double.parseDouble(temizMetin);
        } catch (Exception e) {
            uyarilar.add(String.format("%s / %s / %s: sayıya çevrilemedi ('%s')", il, urun, metrik, metin));
            return null;
        }
    }
}