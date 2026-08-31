package com.almina.tarimicmal.report;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AgriReport(
        Long id,
        String il,
        String kategori,
        LocalDate raporTarihi,
        String notlar,
        OffsetDateTime createdAt
) {}