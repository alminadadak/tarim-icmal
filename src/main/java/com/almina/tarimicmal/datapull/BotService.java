package com.almina.tarimicmal.datapull;

import org.springframework.stereotype.Service;

@Service
public class BotService {

    private final DataFetchService dataFetchService;

    public BotService(DataFetchService dataFetchService) {
        this.dataFetchService = dataFetchService;
    }

    public void botuCalistir(String kullaniciAdi, String sifre, int sezon) {
        // 1. Önce İcmal verisini çek (eskiden: build_icmal.py)
        try {
            dataFetchService.fetchIcmal(kullaniciAdi, sifre, sezon);
        } catch (DataFetchService.NetworkUnavailableException e) {
            throw new AgBaglantiHatasi(e.getMessage());
        }

        try {
            // Sunucuyu yormamak için 5 saniye bekle (eskiden iki ayrı process arasındaydı)
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 2. Ardından Eğitim/Tahmin verisini çek (eskiden: build_classifications.py)
        try {
            dataFetchService.fetchClassifications(kullaniciAdi, sifre);
        } catch (DataFetchService.NetworkUnavailableException e) {
            throw new AgBaglantiHatasi(e.getMessage());
        }
    }

    public static class AgBaglantiHatasi extends RuntimeException {
        public AgBaglantiHatasi(String mesaj) {
            super(mesaj);
        }
    }
}