package com.almina.tarimicmal.icmal;

import java.math.BigDecimal;
import java.time.OffsetDateTime;


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
