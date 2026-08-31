# tarim-icmal

Postgres'teki `v_icmal` view'ını okuyup JSON olarak sunan basit bir Spring Boot API'si.

## Kurulum

1. `src/main/resources/application.properties` içindeki veritabanı bilgilerini doldur
   (host, port, dbname, kullanıcı, şifre — Docker container'ından aldığın bilgiler).

2. Terminalde proje klasöründe:
   ```
   mvn spring-boot:run
   ```
   (Maven kurulu değilse: `./mvnw spring-boot:run` — ama bu proje wrapper içermiyor,
   gerekirse ben ekleyebilirim, ya da IntelliJ/Eclipse gibi bir IDE ile de
   doğrudan çalıştırabilirsin.)

3. Uygulama ayağa kalkınca (~10-20 saniye), tarayıcıda dene:
   - http://localhost:8080/api/icmal
   - http://localhost:8080/api/icmal?il=Şırnak
   - http://localhost:8080/api/icmal?urun=PAMUK
   - http://localhost:8080/api/icmal?il=Şırnak&urun=PAMUK

Her biri JSON formatında sonuç döndürmeli.

## Proje yapısı

```
src/main/java/com/almina/tarimicmal/
├── TarimIcmalApplication.java   ← başlangıç noktası
└── icmal/
    ├── IcmalRow.java             ← bir satırı temsil eden veri sınıfı
    ├── IcmalRepository.java      ← v_icmal'i sorgulayan SQL kodu
    └── IcmalController.java      ← /api/icmal endpoint'i
```

## Notlar

- JPA/Hibernate kullanılmadı, düz `JdbcTemplate` kullanıldı — çünkü `v_icmal`
  bir view, doğal bir primary key'i yok ve sadece okuma yapıyoruz.
- Frontend'in (React, Vue, düz HTML, ne kullanıyorsan) bu `/api/icmal`
  endpoint'ine `fetch`/`axios` ile istek atıp gelen JSON'u tabloya/grafiğe
  dökmesi yeterli.
