package com.almina.tarimicmal.icmal;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


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
            @RequestParam(defaultValue = "Tüm") String donem 
    ) {
        return icmalRepository.findIcmal(il, urun, sezon, donem);
    }



    @GetMapping("/api/iller")
    public List<String> getIller() {
        return icmalRepository.findDistinctIller();
    }

    @GetMapping("/api/sezonlar")
    public List<Integer> getSezonlar() {
        return icmalRepository.findDistinctSezonlar();
    }
}