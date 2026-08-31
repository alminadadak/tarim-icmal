package com.almina.tarimicmal.datapull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DataFetchService {

    private static final String BASE_URL = "https://apiuydu.tarimorman.gov.tr";
    private static final String USER_AGENT =
            "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:144.0) Gecko/20100101 Firefox/144.0";

    private static final List<String> CROPS = List.of(
            "ASPİR", "AYÇİÇEĞİ", "BOŞ", "FASULYE", "FİĞ", "HUBUBAT", "KANOLA",
            "KORUNGA", "MERCİMEK", "MISIR", "NADAS", "NOHUT", "PAMUK", "PATATES",
            "ŞEKERPANCARI", "SİLAJLIK MISIR", "SOĞAN", "SOYA", "YO", "YONCA", "DİĞER"
    );

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public DataFetchService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public static class NetworkUnavailableException extends RuntimeException {
        public NetworkUnavailableException(String message) {
            super(message);
        }
    }

    // ─────────────────────────── LOGIN ───────────────────────────

    private String login(String kullaniciAdi, String sifre) {
        String body;
        try {
            ObjectNode json = objectMapper.createObjectNode();
            json.put("login", kullaniciAdi);
            json.put("password", sifre);
            body = objectMapper.writeValueAsString(json);
        } catch (Exception e) {
            throw new RuntimeException("Login govdesi olusturulamadi: " + e.getMessage(), e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/user/login"))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
                .header("Content-Type", "application/json")
                .header("Origin", "https://uydutakip.tarimorman.gov.tr")
                .header("Referer", "https://uydutakip.tarimorman.gov.tr/")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (java.net.ConnectException | HttpTimeoutException e) {
            throw new NetworkUnavailableException(
                    "Tarım Bakanlığı sistemine ulaşılamadı. Lütfen kurum ağına (VPN/intranet) bağlı olduğunuzdan emin olun.");
        } catch (IOException | InterruptedException e) {
            throw new NetworkUnavailableException(
                    "Tarım Bakanlığı sistemine ulaşılamadı: " + e.getMessage());
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException("Login başarısız - " + response.statusCode() + ": "
                    + kirp(response.body(), 300));
        }

        JsonNode data;
        try {
            data = objectMapper.readTree(response.body());
        } catch (Exception e) {
            throw new RuntimeException("Login yanıtı JSON olarak okunamadı: " + e.getMessage());
        }

        JsonNode tokenNode = data.get("authToken");
        if (tokenNode == null || tokenNode.isNull()) {
            throw new RuntimeException("Login yanıtında 'authToken' bulunamadı.");
        }
        return tokenNode.asText();
    }

    private HttpRequest.Builder headersIle(String token, String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
                .header("Authorization", "Bearer " + token)
                .header("Origin", "https://uydutakip.tarimorman.gov.tr")
                .header("Referer", "https://uydutakip.tarimorman.gov.tr/");
    }

    // ─────────────────────── build_icmal.py ───────────────────────

    public void fetchIcmal(String kullaniciAdi, String sifre, int sezon) {
        String token = login(kullaniciAdi, sifre);
        List<Object[]> tumSatirlar = new ArrayList<>();
        List<String> basarisizUrunler = new ArrayList<>();

        for (String crop : CROPS) {
            JsonNode data;
            try {
                data = urunVerisiCek(crop, sezon, token);
            } catch (TokenSuresiDolduException e) {
                token = login(kullaniciAdi, sifre);
                try {
                    data = urunVerisiCek(crop, sezon, token);
                } catch (Exception e2) {
                    basarisizUrunler.add(crop);
                    continue;
                }
            } catch (Exception e) {
                basarisizUrunler.add(crop);
                continue;
            }

            if (data == null || !data.isArray() || data.isEmpty()) {
                continue;
            }

            JsonNode turkiye = data.get(0);
            OffsetDateTime simdi = OffsetDateTime.now();

            tumSatirlar.add(satiraCevir(sezon, crop, turkiye, "Türkiye", null, simdi));

            for (JsonNode il : turkiye.path("level1List")) {
                tumSatirlar.add(satiraCevir(sezon, crop, il, "İl", "Türkiye", simdi));
                for (JsonNode ilce : il.path("level2List")) {
                    tumSatirlar.add(satiraCevir(sezon, crop, ilce, "İlçe", metin(il, "name"), simdi));
                }
            }
        }

        if (tumSatirlar.isEmpty()) {
            throw new RuntimeException("Hiç icmal verisi çekilemedi.");
        }

        String sql = """
                MERGE INTO agri_field_stats (
                    season, crop, crop_group, level, name, parent,
                    field_count, total_area,
                    referenced_field_count, referenced_area,
                    test_field_count, test_area,
                    requires_on_site_observation_field_count, requires_on_site_observation_area,
                    waiting_control_reference_field_count, waiting_control_reference_area,
                    rejected_reference_field_count, rejected_reference_area,
                    suspicious_reference_field_count, suspicious_reference_area,
                    updated_at
                )
                KEY (season, crop, level, name, parent)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.batchUpdate(sql, tumSatirlar);

        if (!basarisizUrunler.isEmpty()) {
            System.out.println("UYARI: Şu ürünler çekilemedi: " + basarisizUrunler);
        }
        System.out.println("İcmal: " + tumSatirlar.size() + " satır yazıldı.");
    }

    private static class TokenSuresiDolduException extends RuntimeException {
    }

    private JsonNode urunVerisiCek(String crop, int sezon, String token) throws IOException, InterruptedException {
        String url = BASE_URL + "/field_Plantings/seasonalStatistics?season=" + sezon
                + "&crops=" + URLEncoder.encode(crop, StandardCharsets.UTF_8);

        int maxDeneme = 3;
        for (int deneme = 0; deneme < maxDeneme; deneme++) {
            HttpRequest request = headersIle(token, url)
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException e) {
                if (deneme < maxDeneme - 1) {
                    sleepMs(3000);
                    continue;
                }
                throw e;
            }

            if (response.statusCode() == 401) {
                throw new TokenSuresiDolduException();
            }
            if (response.statusCode() == 200) {
                return objectMapper.readTree(response.body());
            }
            if (deneme < maxDeneme - 1) {
                sleepMs(3000);
            }
        }
        throw new IOException(crop + " için " + maxDeneme + " deneme de başarısız oldu.");
    }

    private Object[] satiraCevir(int sezon, String crop, JsonNode node, String level, String parent, OffsetDateTime simdi) {
        return new Object[]{
                sezon, crop, crop, level, metin(node, "name"), parent,
                tamsayi(node, "fieldCount"), ondalik(node, "totalArea"),
                tamsayi(node, "referencedFieldCount"), ondalik(node, "referencedArea"),
                tamsayi(node, "testFieldCount"), ondalik(node, "testArea"),
                tamsayi(node, "requiresOnSiteObservationFieldCount"), ondalik(node, "requiresOnSiteObservationArea"),
                tamsayi(node, "waitingControlReferenceFieldCount"), ondalik(node, "waitingControlReferenceArea"),
                tamsayi(node, "rejectedReferenceFieldCount"), ondalik(node, "rejectedReferenceArea"),
                tamsayi(node, "suspiciousReferenceFieldCount"), ondalik(node, "suspiciousReferenceArea"),
                simdi
        };
    }

    // ─────────────────── build_classifications.py ───────────────────

    public void fetchClassifications(String kullaniciAdi, String sifre) {
        String token = login(kullaniciAdi, sifre);

        List<String> bilinenIller = jdbcTemplate.queryForList(
                "SELECT DISTINCT name FROM agri_field_stats WHERE level = 'İl'", String.class);
        Map<String, String> normalizeHarita = new HashMap<>();
        for (String il : bilinenIller) {
            normalizeHarita.put(turkceNormalize(il), il);
        }

        HttpRequest request = headersIle(token, BASE_URL + "/fieldClassifications")
                .timeout(Duration.ofSeconds(120))
                .GET()
                .build();

        JsonNode kayitlar;
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("fieldClassifications hatası: " + response.statusCode()
                        + " - " + kirp(response.body(), 500));
            }
            kayitlar = objectMapper.readTree(response.body());
        } catch (java.net.ConnectException | HttpTimeoutException e) {
            throw new NetworkUnavailableException(
                    "Tarım Bakanlığı sistemine ulaşılamadı. Lütfen kurum ağına (VPN/intranet) bağlı olduğunuzdan emin olun.");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("fieldClassifications çekilemedi: " + e.getMessage());
        }

        OffsetDateTime simdi = OffsetDateTime.now();
        List<Object[]> yazilacakSatirlar = new ArrayList<>();

        for (JsonNode kayit : kayitlar) {
            String name = metin(kayit, "name");
            if (name == null) name = "";
            List<String> illerHam = illeriAyikla(name);
            if (illerHam.isEmpty()) continue;

            List<String> cropsListesi = new ArrayList<>();
            for (JsonNode c : kayit.path("crops")) {
                cropsListesi.add(c.asText());
            }
            String cropsStr = String.join(",", cropsListesi);

            for (String ilHam : illerHam) {
                String normalizeKey = turkceNormalize(ilHam);
                String il = normalizeHarita.get(normalizeKey);
                if (il == null) continue;

                yazilacakSatirlar.add(new Object[]{
                        metin(kayit, "id"),
                        il,
                        tamsayi(kayit, "season"),
                        tamsayi(kayit, "quarter"),
                        cropsStr,
                        kayit.path("isTrain").asBoolean(false),
                        kayit.path("isTest").asBoolean(false),
                        metin(kayit, "status"),
                        parseTarih(metin(kayit, "createdAt")),
                        simdi
                });
            }
        }

        if (yazilacakSatirlar.isEmpty()) {
            System.out.println("Sınıflandırma: yazılacak satır yok.");
            return;
        }

        String sql = """
                MERGE INTO agri_field_classifications (
                    source_id, il, season, quarter, crops, is_train, is_test,
                    status, source_created_at, fetched_at
                )
                KEY (source_id, il)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.batchUpdate(sql, yazilacakSatirlar);
        System.out.println("Sınıflandırma: " + yazilacakSatirlar.size() + " satır yazıldı.");
    }

    private List<String> illeriAyikla(String name) {
        String[] parts = name.split("-", -1);
        if (parts.length < 4) return List.of();

        List<String> iller = new ArrayList<>();
        for (int i = 3; i < parts.length; i++) {
            String token = parts[i].strip();
            if (token.isEmpty()) continue;
            String il = token.split(",", -1)[0].strip();
            if (!il.isEmpty()) iller.add(il);
        }
        return iller.stream().distinct().sorted().toList();
    }

    private String turkceNormalize(String metin) {
        String s = metin.strip();
        s = s.replace("İ", "i").replace("I", "ı");
        return s.toLowerCase(Locale.ROOT);
    }


    private OffsetDateTime parseTarih(String metin) {
        if (metin == null || metin.isBlank()) return null;
        try {
            return OffsetDateTime.parse(metin);
        } catch (Exception e1) {
            try {
                return Instant.parse(metin).atOffset(java.time.ZoneOffset.UTC);
            } catch (Exception e2) {
                System.out.println("UYARI: source_created_at parse edilemedi: '" + metin + "'");
                return null;
            }
        }
    }

    // ─────────────────────────── YARDIMCILAR ───────────────────────────

    private String metin(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private Integer tamsayi(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asInt();
    }

    private Double ondalik(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asDouble();
    }

    private String kirp(String metin, int uzunluk) {
        if (metin == null) return "";
        return metin.length() <= uzunluk ? metin : metin.substring(0, uzunluk);
    }

    private void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}