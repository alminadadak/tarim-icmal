package com.almina.tarimicmal.datapull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bot")
public class BotController {

    private final BotService botService;

    @Autowired
    public BotController(BotService botService) {
        this.botService = botService;
    }

    // Frontend'den gelen JSON parametrelerini yakalamak için sınıf
    public static class BotLoginRequest {
        private String kullaniciAdi;
        private String sifre;
        private Integer sezon;

        public String getKullaniciAdi() { return kullaniciAdi; }
        public void setKullaniciAdi(String kullaniciAdi) { this.kullaniciAdi = kullaniciAdi; }
        
        public String getSifre() { return sifre; }
        public void setSifre(String sifre) { this.sifre = sifre; }
        
        public Integer getSezon() { return sezon; }
        public void setSezon() { this.sezon = sezon; }

    }

    @PostMapping("/guncelle")
    public ResponseEntity<String> manuelGuncelle(@RequestBody BotLoginRequest request) {
        // Parametrelerin boş gelip gelmediğini kontrol et
        if (request.getKullaniciAdi() == null || request.getSifre() == null || 
            request.getKullaniciAdi().trim().isEmpty() || request.getSifre().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Kullanıcı adı ve şifre eksik.");
        }
        if (request.getSezon() == null) {
            return ResponseEntity.badRequest().body("Sezon eksik.");
        }

        try {
            // Parametreleri servise gönder
            botService.botuCalistir(request.getKullaniciAdi(), request.getSifre(), request.getSezon());
            return ResponseEntity.ok("Veriler başarıyla güncellendi!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Hata oluştu: " + e.getMessage());
        }
    }
}