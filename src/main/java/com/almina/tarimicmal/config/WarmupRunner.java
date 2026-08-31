package com.almina.tarimicmal.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Uygulama tamamen ayaga kalktiktan hemen sonra, kullanicinin tarayicisi
 * henuz sayfayi acmadan, v_icmal sorgusunu bir kere "bosa" calistirir.
 *
 * Sebep: JVM ve H2, bir sorguyu ILK calistirdiginda (sorgu planini
 * derleme, JIT'in henuz kodu "isitmamis" olmasi gibi nedenlerle) birkac
 * saniye daha yavas calisiyor. Bu isindirma sayesinde, kullanici sayfayi
 * actiginda bu maliyet zaten odenmis oluyor ve ilk deneyimi de aninda
 * hizli oluyor.
 *
 * ILERIDE: Acilista otomatik tarayici acma ozelligini eklerken, tarayiciyi
 * bu isindirma bittikten SONRA acacak sekilde sıralarsak, kullanici hicbir
 * zaman "ilk yukleme yavas" hissini yasamaz.
 */
@Component
public class WarmupRunner {

    private final JdbcTemplate jdbcTemplate;

    public WarmupRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void isindir() {
        long baslangic = System.currentTimeMillis();
        try {
            // v_icmal'i tetikleyen herhangi bir sorgu yeterli - asil amac
            // H2'ye sorgu planini derletmek ve JVM'e JIT firsati vermek.
            jdbcTemplate.queryForList("SELECT * FROM v_icmal LIMIT 1");
            long gecenSure = System.currentTimeMillis() - baslangic;
            System.out.println("[Isindirma] v_icmal sorgusu isindirildi (" + gecenSure + " ms).");
        } catch (Exception e) {
            // Isindirma basarisiz olsa bile uygulama normal calismaya devam
            // eder - sadece ilk gercek sorgu biraz yavas olabilir, o kadar.
            System.out.println("[Isindirma] Basarisiz oldu (onemli degil): " + e.getMessage());
        }
    }
}