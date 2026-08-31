package com.almina.tarimicmal.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;


@Component
public class WarmupRunner {

    private final JdbcTemplate jdbcTemplate;

    public WarmupRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void isindir() {
        long baslangic = System.currentTimeMillis();
        try {
            jdbcTemplate.queryForList("SELECT * FROM v_icmal LIMIT 1");
            long gecenSure = System.currentTimeMillis() - baslangic;
            System.out.println("[Isindirma] v_icmal sorgusu isindirildi (" + gecenSure + " ms).");
        } catch (Exception e) {
            System.out.println("[Isindirma] Basarisiz oldu (onemli degil): " + e.getMessage());
        }
    }
}