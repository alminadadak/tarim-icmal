package com.almina.tarimicmal.dataimport;

import java.util.List;

/**
 * Bir import işleminin sonucunu frontend'e JSON olarak döndürmek için.
 * Python script'lerimizdeki "print" çıktılarının web arayüzü karşılığı gibi
 * düşünebilirsin - orada terminale yazdırıyorduk, burada tarayıcıya JSON
 * olarak gönderiyoruz.
 */
public record ImportResult(
        int yaziliSatirSayisi,
        List<String> uyarilar
) {
}