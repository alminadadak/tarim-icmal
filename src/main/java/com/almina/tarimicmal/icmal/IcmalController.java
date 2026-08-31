package com.almina.tarimicmal.icmal;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Örnek kullanım:
 *   GET /api/icmal                         -> tüm veri
 *   GET /api/icmal?il=Şırnak                -> sadece Şırnak
 *   GET /api/icmal?urun=PAMUK               -> tüm illerde pamuk
 *   GET /api/icmal?il=Şırnak&urun=PAMUK     -> Şırnak'ta pamuk
 */
@RestController
public class IcmalController {

    private final IcmalRepository icmalRepository;

    public IcmalController(IcmalRepository icmalRepository) {
        this.icmalRepository = icmalRepository;
    }

    @GetMapping("/api/icmal")
    public List<IcmalRow> getIcmal(
            @RequestParam(required = false) String il,
            @RequestParam(required = false) String urun,
            @RequestParam(required = false) Integer sezon,
            @RequestParam(defaultValue = "Tüm") String donem // YENİ: Dönem parametresi eklendi
    ) {
        // Repository'ye 'donem' bilgisini de paslıyoruz
        return icmalRepository.findIcmal(il, urun, sezon, donem);
    }


    /**
     * Dropdown'ı doldurmak için: veritabanındaki tüm illerin listesi.
     * Örnek: GET /api/iller -> ["Adana", "Adıyaman", ..., "Şırnak"]
     */
    @GetMapping("/api/iller")
    public List<String> getIller() {
        return icmalRepository.findDistinctIller();
    }

    @GetMapping("/api/sezonlar")
    public List<Integer> getSezonlar() {
        return icmalRepository.findDistinctSezonlar();
    }
}