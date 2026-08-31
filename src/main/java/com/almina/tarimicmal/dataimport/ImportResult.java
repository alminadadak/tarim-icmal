package com.almina.tarimicmal.dataimport;

import java.util.List;


public record ImportResult(
        int yaziliSatirSayisi,
        List<String> uyarilar
) {
}