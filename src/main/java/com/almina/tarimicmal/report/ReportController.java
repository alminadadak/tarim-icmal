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
            reportRepository.save(report);

            List<String> urls = jdbcTemplate.queryForList("SELECT setting_value FROM app_settings WHERE setting_key = 'google_sync_url'", String.class);
            String url = urls.isEmpty() ? null : urls.get(0);
            
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
            List<String> urls = jdbcTemplate.queryForList("SELECT setting_value FROM app_settings WHERE setting_key = 'google_sync_url'", String.class);
            String url = urls.isEmpty() ? null : urls.get(0);
            
            if (url == null || url.isEmpty()) {
                return ResponseEntity.badRequest().body("Önce bulut senkronizasyon linkini kaydedin.");
            }

            String spreadsheetId = GoogleSheetsService.extractSpreadsheetId(url);
            
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

    @PostMapping("/settings/sync-url")
    public ResponseEntity<?> saveSyncUrl(@RequestBody Map<String, String> body) {
        try {
            String url = body.get("url");
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

    @GetMapping("/classifications/summary")
    public ResponseEntity<?> getClassificationsSummary(@RequestParam String il, @RequestParam(required = false) Integer sezon) {
        try {

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
            return ResponseEntity.ok(Map.of("egitim_sayisi", 0, "tahmin_sayisi", 0));
        }
    }
}