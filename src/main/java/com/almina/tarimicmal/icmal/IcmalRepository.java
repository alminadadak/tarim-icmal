package com.almina.tarimicmal.icmal;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * v_icmal view'ından veri okuyan repository.
 *
 * JPA yerine düz JdbcTemplate kullanıyoruz çünkü v_icmal bir VIEW,
 * gerçek bir tablo değil - doğal bir primary key'i yok ve hiç
 * INSERT/UPDATE yapmıyoruz, sadece okuyoruz. Bu basitlik için ideal.
 */
@Repository
public class IcmalRepository {

    private final JdbcTemplate jdbcTemplate;

    public IcmalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Veritabanındaki tüm benzersiz (distinct) il isimlerini döndürür.
     * Dropdown'ı dinamik doldurmak için kullanılacak - artık HTML'e elle
     * il ismi yazmaya gerek yok.
     */
    public List<String> findDistinctIller() {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT il FROM v_icmal ORDER BY il",
                String.class
        );
    }

    public List<Integer> findDistinctSezonlar() {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT sezon FROM v_icmal ORDER BY sezon DESC",
                Integer.class
        );
    }

    /**
     * il ve/veya urun verilmezse (null ise) o filtre uygulanmaz.
     * Örnek: findIcmal("Şırnak", null) -> Şırnak'taki tüm ürünler
     *        findIcmal(null, "PAMUK") -> tüm illerdeki pamuk verisi
     *        findIcmal(null, null)    -> tüm veri
     */
    public List<IcmalRow> findIcmal(String il, String urun, Integer sezon, String donem) {
        // İŞ KURALI KORUNDU: Sadece planlanan hedefleri olan ürünler listelenir.
        StringBuilder sql = new StringBuilder("""
                SELECT il, urun, sezon,
                       parsel_4da_alti, parsel_4da_ustu,
                       planlanan,
                       referans_parsel, referans_alan,
                       nihai_referans, nihai_alan,
                       tamamlanan_yuzde,
                       son_guncelleme
                FROM v_icmal
                WHERE planlanan > 0
                """);

        List<Object> params = new ArrayList<>();

        if (il != null && !il.isBlank()) {
            sql.append(" AND il = ?");
            params.add(il);
        }
        if (urun != null && !urun.isBlank()) {
            sql.append(" AND urun = ?");
            params.add(urun);
        }
        if (sezon != null) {
            sql.append(" AND sezon = ?");
            params.add(sezon);
        }
        // YENİ: Dönem filtresi
        if (donem != null && !donem.isBlank()) {
            sql.append(" AND donem = ?");
            params.add(donem);
        }

        sql.append(" ORDER BY il, urun");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new IcmalRow(
                rs.getString("il"),
                rs.getString("urun"),
                (Integer) rs.getObject("sezon"),
                (Integer) rs.getObject("parsel_4da_alti"),
                (Integer) rs.getObject("parsel_4da_ustu"),
                rs.getBigDecimal("planlanan"),
                (Integer) rs.getObject("referans_parsel"),
                rs.getBigDecimal("referans_alan"),
                (Integer) rs.getObject("nihai_referans"),
                rs.getBigDecimal("nihai_alan"),
                rs.getBigDecimal("tamamlanan_yuzde"),
                rs.getObject("son_guncelleme", java.time.OffsetDateTime.class)
        ), params.toArray());
    }
}