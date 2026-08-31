package com.almina.tarimicmal.dataimport;

import com.almina.tarimicmal.report.GoogleSheetsService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlannedImportService {

    private final JdbcTemplate jdbcTemplate;
    private final GoogleSheetsService googleSheetsService;

    private static final Map<String, String> URUN_NORMALIZASYON = Map.of(
        "KANOLA / KOLZA", "KANOLA"
    );

    // YENİ: Google servisini constructor'a ekledik
    public PlannedImportService(JdbcTemplate jdbcTemplate, GoogleSheetsService googleSheetsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.googleSheetsService = googleSheetsService;
    }

    private String normalizeUrun(String urun) {
        return URUN_NORMALIZASYON.getOrDefault(urun, urun);
    }

    private record KolonBilgisi(String urun, String donem) {}

    public ImportResult importFromCloud(String spreadsheetId, int sezon) throws Exception {
        List<String> uyarilar = new ArrayList<>();
        List<Object[]> yaziliSatirlar = new ArrayList<>();

        // 1. Dosya yüklemek yerine Google API'den "Planlanan" sekmesini çekiyoruz
        List<List<Object>> rows = googleSheetsService.readSheetData(spreadsheetId, "Planlanan");

        if (rows == null || rows.size() < 2) {
            throw new IllegalArgumentException("'Planlanan' sayfasında yeterli veri veya başlık bulunamadı.");
        }

        List<Object> ustBaslik = rows.get(0);   // ürün adları (birleşik hücreler)
        List<Object> altBaslik = rows.get(1);   // Tüm / Kışlık / Yazlık

        int sonKolon = Math.max(ustBaslik.size(), altBaslik.size());

        Map<Integer, KolonBilgisi> kolonEslesme = new HashMap<>();
        String sonGorulenUrun = null;

        // 2. Başlıkları analiz et (Yatay forward-fill)
        for (int col = 2; col < sonKolon; col++) {
            String ustHucre = getHucre(ustBaslik, col);
            if (!ustHucre.isEmpty()) {
                sonGorulenUrun = normalizeUrun(ustHucre);
            }
            
            String donem = getHucre(altBaslik, col);

            if (sonGorulenUrun != null && !donem.isEmpty()) {
                kolonEslesme.put(col, new KolonBilgisi(sonGorulenUrun, donem));
            }
        }

        if (kolonEslesme.isEmpty()) {
            uyarilar.add("Hiçbir ürün/dönem kolonu tanınamadı - başlık satırlarının yapısını kontrol etmek gerekebilir.");
        }

        OffsetDateTime simdi = OffsetDateTime.now();
        String sonGorulenGrup = null;

        // 3. Veri satırlarını oku (Dikey forward-fill) - 2. satırdan (index 2) başlıyor
        for (int rowIdx = 2; rowIdx < rows.size(); rowIdx++) {
            List<Object> row = rows.get(rowIdx);
            if (row.isEmpty()) continue;

            String grupHucre = getHucre(row, 0);
            if (!grupHucre.isEmpty()) {
                sonGorulenGrup = grupHucre; // dikey forward-fill (birleşik hücre)
            }

            String il = getHucre(row, 1);
            if (il.isEmpty()) continue; // boş satırları atla

            for (Map.Entry<Integer, KolonBilgisi> entry : kolonEslesme.entrySet()) {
                int colIdx = entry.getKey();
                KolonBilgisi bilgi = entry.getValue();

                Double deger = sayiyaCevirVeyaNull(row, colIdx, il, bilgi.urun(), bilgi.donem(), uyarilar);

                if (deger == null) continue;

                yaziliSatirlar.add(new Object[]{
                        sezon, sonGorulenGrup, il, bilgi.urun(), bilgi.donem(), deger, simdi
                });
            }
        }

        // 4. Veritabanına upsert
        // H2'YE GECIS NOTU: MERGE INTO ... KEY(...) - ayrinti icin
        // ParcelImportService'teki ayni degisiklige bakabilirsin.
        String sql = """
                MERGE INTO agri_planned_targets (season, grup, il, crop, period, planned_count, updated_at)
                KEY (season, il, crop, period)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.batchUpdate(sql, yaziliSatirlar);

        return new ImportResult(yaziliSatirlar.size(), uyarilar);
    }

    // YENİ: List<Object> içinden güvenle string okuma metodu
    private String getHucre(List<Object> row, int index) {
        if (index >= row.size() || row.get(index) == null) return "";
        return row.get(index).toString().trim();
    }

    // YENİ: String veriyi Double'a çeviren akıllı metot (Virgülleri noktaya çevirir)
    private Double sayiyaCevirVeyaNull(List<Object> row, int index, String il, String urun, String donem, List<String> uyarilar) {
        String metin = getHucre(row, index);
        if (metin.isEmpty()) return null;

        try {
            // "1000,5" gibi Türkçe formatlı ondalıkları Java'nın anlayacağı "1000.5" formatına çevir
            String temizMetin = metin.replace(" ", "").replace(",", ".");
            return Double.parseDouble(temizMetin);
        } catch (Exception e) {
            uyarilar.add(String.format("%s / %s / %s: sayıya çevrilemedi ('%s'), atlandı", il, urun, donem, metin));
            return null;
        }
    }
}