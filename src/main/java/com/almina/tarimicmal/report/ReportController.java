package com.almina.tarimicmal.report;

import com.almina.tarimicmal.dataimport.ImportResult;
import com.almina.tarimicmal.report.GoogleSheetsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final GoogleSheetsService googleSheetsService;
    private final ReportRepository reportRepository;
    private final ReportImportService reportImportService;
    private final JdbcTemplate jdbcTemplate; 

    // DÜZELTİLDİ: googleSheetsService buraya eklendi!
    public ReportController(GoogleSheetsService googleSheetsService,
                            ReportRepository reportRepository, 
                            ReportImportService reportImportService,
                            JdbcTemplate jdbcTemplate) { 
        this.googleSheetsService = googleSheetsService;
        this.reportRepository = reportRepository;
        this.reportImportService = reportImportService;
        this.jdbcTemplate = jdbcTemplate; 
    }

    // GET /api/reports?il=Adana
    @GetMapping
    public List<AgriReport> getReports(@RequestParam String il) {
        return reportRepository.findByIl(il);
    }

    // POST /api/reports
    // POST /api/reports
    @PostMapping
    public ResponseEntity<?> addReport(@RequestBody AgriReport report) {
        try {
            // 1. Önce kendi veritabanımıza kaydedelim
            reportRepository.save(report);

            // 2. Google Linkini veritabanından GÜVENLİ bir şekilde bul
            List<String> urls = jdbcTemplate.queryForList("SELECT setting_value FROM app_settings WHERE setting_key = 'google_sync_url'", String.class);
            String url = urls.isEmpty() ? null : urls.get(0);
            
            // 3. Google Excel'in en alt satırına veriyi ekle!
            if (url != null && !url.isEmpty()) {
                String spreadsheetId = GoogleSheetsService.extractSpreadsheetId(url);
                
                List<Object> rowData = List.of(
                        report.il(),
                        report.kategori(),
                        report.raporTarihi().toString(), 
                        report.notlar()
                );
                
                googleSheetsService.appendRowToSheet(spreadsheetId, "rapor_girişi", rowData);
            }

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Rapor eklendi ama Google'a yazılamadı: " + e.getMessage());
        }
    }

    // POST /api/reports/sync
    @PostMapping("/sync")
    public ResponseEntity<?> syncReportsFromCloud() {
        try {
            // Veritabanından linki GÜVENLİ bir şekilde al
            List<String> urls = jdbcTemplate.queryForList("SELECT setting_value FROM app_settings WHERE setting_key = 'google_sync_url'", String.class);
            String url = urls.isEmpty() ? null : urls.get(0);
            
            if (url == null || url.isEmpty()) {
                return ResponseEntity.badRequest().body("Önce bulut senkronizasyon linkini kaydedin.");
            }

            String spreadsheetId = GoogleSheetsService.extractSpreadsheetId(url);
            
            // Yeni servisi çağır
            ImportResult sonuc = reportImportService.importFromCloud(spreadsheetId);
            return ResponseEntity.ok(sonuc);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Beklenmeyen bir hata oluştu: " + e.getMessage());
        }
    }

    // Kayıtlı Google CSV linkini getirir
    @GetMapping("/settings/sync-url")
    public ResponseEntity<String> getSyncUrl() {
        try {
            String url = jdbcTemplate.queryForObject(
                "SELECT setting_value FROM app_settings WHERE setting_key = 'google_sync_url'",
                String.class
            );
            return ResponseEntity.ok(url);
        } catch (Exception e) {
            // Veritabanında link henüz yoksa sistemi çökertmez, sessizce boş döner
            return ResponseEntity.ok("");
        }
    }

    // Google CSV linkini veritabanına kaydeder
    @PostMapping("/settings/sync-url")
    public ResponseEntity<?> saveSyncUrl(@RequestBody Map<String, String> body) {
        try {
            String url = body.get("url");
            // H2'YE GECIS NOTU: MERGE INTO ... KEY(...) - Postgres'in ON CONFLICT'i yerine.
            jdbcTemplate.update(
                "MERGE INTO app_settings (setting_key, setting_value) " +
                "KEY (setting_key) VALUES ('google_sync_url', ?)",
                url
            );
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Ayarlar kaydedilemedi: " + e.getMessage());
        }
    }

    // Seçilen il ve sezona göre Eğitim ve Tahmin sayılarını getirir
    @GetMapping("/classifications/summary")
    public ResponseEntity<?> getClassificationsSummary(@RequestParam String il, @RequestParam(required = false) Integer sezon) {
        try {
            // H2'YE GECIS NOTU: COUNT(*) FILTER (WHERE ...) yerine, her veritabaninda
            // sorunsuz calisan SUM(CASE WHEN ... THEN 1 ELSE 0 END) kullandim - FILTER
            // H2'de calisiyor olabilirdi ama garantiye almak istedim.
            String sql = "SELECT " +
                        "SUM(CASE WHEN is_train = true THEN 1 ELSE 0 END) as egitim_sayisi, " +
                        "SUM(CASE WHEN is_test = true THEN 1 ELSE 0 END) as tahmin_sayisi " +
                        "FROM agri_field_classifications WHERE LOWER(il) = LOWER(?) "; 
            
            Map<String, Object> result;
            if (sezon != null) {
                sql += "AND season = ?";
                result = jdbcTemplate.queryForMap(sql, il, sezon);
            } else {
                result = jdbcTemplate.queryForMap(sql, il);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Hata olursa veya tablo boşsa 0 döndür, arayüzü bozma
            return ResponseEntity.ok(Map.of("egitim_sayisi", 0, "tahmin_sayisi", 0));
        }
    }
}