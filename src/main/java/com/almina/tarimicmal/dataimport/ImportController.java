package com.almina.tarimicmal.dataimport;

import com.almina.tarimicmal.report.GoogleSheetsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ImportController {

    private final ParcelImportService parcelImportService;
    private final PlannedImportService plannedImportService;
    private final JdbcTemplate jdbcTemplate;

    // YENİ: Veritabanından linki okuyabilmek için JdbcTemplate ekledik
    public ImportController(ParcelImportService parcelImportService,
                            PlannedImportService plannedImportService,
                            JdbcTemplate jdbcTemplate) {
        this.parcelImportService = parcelImportService;
        this.plannedImportService = plannedImportService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Ortak Yardımcı Metot: Veritabanından linki bulup Spreadsheet ID'yi çıkarır
     */
    private String getSpreadsheetId() {
        List<String> urls = jdbcTemplate.queryForList("SELECT setting_value FROM app_settings WHERE setting_key = 'google_sync_url'", String.class);
        if (urls.isEmpty() || urls.get(0) == null || urls.get(0).isEmpty()) {
            throw new IllegalArgumentException("Lütfen önce 'Veri Yönetimi' sekmesindeki kutucuğa Google E-Tablo linkini girin.");
        }
        return GoogleSheetsService.extractSpreadsheetId(urls.get(0));
    }

    // YENİ: Artık MultipartFile (dosya) istemiyoruz, sadece sezon bilgisini alıyoruz
    @PostMapping("/api/import/cks-parsel")
    public ResponseEntity<?> importCksParsel(@RequestParam("sezon") Integer sezon) {
        if (sezon == null) {
            return ResponseEntity.badRequest().body("Sezon gerekli.");
        }
        try {
            String spreadsheetId = getSpreadsheetId();
            ImportResult sonuc = parcelImportService.importFromCloud(spreadsheetId, sezon);
            return ResponseEntity.ok(sonuc);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Beklenmeyen bir hata oluştu: " + e.getMessage());
        }
    }

    // YENİ: Artık MultipartFile (dosya) istemiyoruz, sadece sezon bilgisini alıyoruz
    @PostMapping("/api/import/planlanan")
    public ResponseEntity<?> importPlanlanan(@RequestParam("sezon") Integer sezon) {
        if (sezon == null) {
            return ResponseEntity.badRequest().body("Sezon gerekli.");
        }
        try {
            String spreadsheetId = getSpreadsheetId();
            ImportResult sonuc = plannedImportService.importFromCloud(spreadsheetId, sezon);
            return ResponseEntity.ok(sonuc);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Beklenmeyen bir hata oluştu: " + e.getMessage());
        }
    }
}