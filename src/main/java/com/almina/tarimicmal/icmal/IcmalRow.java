package com.almina.tarimicmal.icmal;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * v_icmal view'ındaki tek bir satırı temsil eder (bir il + bir ürün
 * kombinasyonu). "record" Java'da otomatik getter/constructor/toString
 * üreten basit bir veri taşıyıcı - JPA Entity'sine gerek yok çünkü bu
 * sadece SELECT sonucu, hiç INSERT/UPDATE yapılmıyor.
 */
public record IcmalRow(
        String il,
        String urun,
        Integer sezon,
        Integer parsel4daAlti,
        Integer parsel4daUstu,
        BigDecimal planlanan,
        Integer referansParsel,
        BigDecimal referansAlan,
        Integer nihaiReferans,
        BigDecimal nihaiAlan,
        BigDecimal tamamlananYuzde,
        OffsetDateTime sonGuncelleme
) {
}
