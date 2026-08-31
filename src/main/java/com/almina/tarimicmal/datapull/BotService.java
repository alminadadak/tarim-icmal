package com.almina.tarimicmal.datapull;

import org.springframework.stereotype.Service;

@Service
public class BotService {

    private final DataFetchService dataFetchService;

    public BotService(DataFetchService dataFetchService) {
        this.dataFetchService = dataFetchService;
    }

    public void botuCalistir(String kullaniciAdi, String sifre, int sezon) {
        try {
            dataFetchService.fetchIcmal(kullaniciAdi, sifre, sezon);
        } catch (DataFetchService.NetworkUnavailableException e) {
            throw new AgBaglantiHatasi(e.getMessage());
        }

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

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