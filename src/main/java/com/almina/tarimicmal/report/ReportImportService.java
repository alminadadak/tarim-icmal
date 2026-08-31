package com.almina.tarimicmal.report;

import com.almina.tarimicmal.dataimport.ImportResult;
import com.almina.tarimicmal.report.GoogleSheetsService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportImportService {

    private static final String SHEET_ADI = "rapor_girişi";

    private final JdbcTemplate jdbcTemplate;
    private final GoogleSheetsService googleSheetsService;

    public ReportImportService(JdbcTemplate jdbcTemplate, GoogleSheetsService googleSheetsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.googleSheetsService = googleSheetsService;
    }

    public ImportResult importFromCloud(String spreadsheetId) throws Exception {
        List<String> uyarilar = new ArrayList<>();
        List<Object[]> yaziliSatirlar = new ArrayList<>();

        List<List<Object>> rows = googleSheetsService.readSheetData(spreadsheetId, SHEET_ADI + "!A:D");

        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("Google Sheet boş veya bulunamadı.");
        }

        for (int i = 1; i < rows.size(); i++) {
            List<Object> row = rows.get(i);
            
            if (row.isEmpty()) continue;

            String il = getHucre(row, 0);
            String kategori = getHucre(row, 1);
            String tarihMetni = getHucre(row, 2);
            String notlar = getHucre(row, 3);

            if (il.isEmpty()) continue;

            if (kategori.isEmpty()) {
                uyarilar.add(String.format("Satır %d (%s): kategori boş, atlandı", i + 1, il));
                continue;
            }

            LocalDate raporTarihi = parseTarih(tarihMetni);
            if (raporTarihi == null) {
                uyarilar.add(String.format("Satır %d (%s): tarih formatı tanınamadı ('%s'), atlandı", i + 1, il, tarihMetni));
                continue;
            }

            yaziliSatirlar.add(new Object[]{il, kategori, raporTarihi, notlar});
        }

        jdbcTemplate.execute("TRUNCATE TABLE agri_reports RESTART IDENTITY");

        String sql = "INSERT INTO agri_reports (il, kategori, rapor_tarihi, notlar) VALUES (?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, yaziliSatirlar);

        return new ImportResult(yaziliSatirlar.size(), uyarilar);
    }

    private String getHucre(List<Object> row, int index) {
        if (index >= row.size() || row.get(index) == null) return "";
        return row.get(index).toString().trim();
    }

    private LocalDate parseTarih(String metin) {
        if (metin.isEmpty()) return null;
        try {
            if (metin.contains(".")) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                return LocalDate.parse(metin, formatter);
            } else if (metin.contains("-")) {
                return LocalDate.parse(metin);
            }
        } catch (Exception e) {
            // Hata olursa null dön
        }
        return null;
    }
}