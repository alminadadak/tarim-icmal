package com.almina.tarimicmal.report;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ReportRepository {

    private final JdbcTemplate jdbcTemplate;

    public ReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Seçilen ile ait raporları tarihe göre (en yeni en üstte) getirir
    public List<AgriReport> findByIl(String il) {
        String sql = """
                SELECT id, il, kategori, rapor_tarihi, notlar, created_at
                FROM agri_reports
                WHERE il = ?
                ORDER BY rapor_tarihi DESC, created_at DESC
                """;
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AgriReport(
                rs.getLong("id"),
                rs.getString("il"),
                rs.getString("kategori"),
                rs.getObject("rapor_tarihi", java.time.LocalDate.class),
                rs.getString("notlar"),
                rs.getObject("created_at", java.time.OffsetDateTime.class)
        ), il);
    }

    // Arayüzden gelen yeni raporu veritabanına kaydeder
    public void save(AgriReport report) {
        String sql = """
                INSERT INTO agri_reports (il, kategori, rapor_tarihi, notlar)
                VALUES (?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql, report.il(), report.kategori(), report.raporTarihi(), report.notlar());
    }

    // Google'dan veya Excel'den gelen raporları topluca ve çok hızlı kaydeder
    // Google'dan veya Excel'den gelen raporları akıllıca (mükerrer olmadan) kaydeder
    public void saveAll(List<AgriReport> reports) {
        // SQL Diyor ki: "Eğer bu il, kategori, tarih ve nota sahip bir kayıt YOKSA (NOT EXISTS) ekle!"
        String sql = """
                INSERT INTO agri_reports (il, kategori, rapor_tarihi, notlar)
                SELECT ?, ?, ?, ?
                WHERE NOT EXISTS (
                    SELECT 1 FROM agri_reports 
                    WHERE il = ? AND kategori = ? AND rapor_tarihi = ? AND notlar = ?
                )
                """;

        jdbcTemplate.batchUpdate(sql, reports, reports.size(),
            (ps, report) -> {
                // 1. Kısım: Eklenecek veriler (SELECT kısmı için)
                ps.setString(1, report.il());
                ps.setString(2, report.kategori());
                ps.setObject(3, report.raporTarihi());
                ps.setString(4, report.notlar());
                
                // 2. Kısım: Kontrol edilecek veriler (NOT EXISTS kısmı için)
                ps.setString(5, report.il());
                ps.setString(6, report.kategori());
                ps.setObject(7, report.raporTarihi());
                ps.setString(8, report.notlar());
            });
    }
    
}