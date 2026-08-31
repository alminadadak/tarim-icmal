-- Sistem ayarlarını ve senkronizasyon linklerini tutacağımız tablo
--
-- H2'YE GECIS NOTU (guncelleme): Onceki denemede "key"/"value" kolonlarini
-- cift tirnak icine almistik ama bu, kodun HER YERDE tutarli sekilde
-- tirnak kullanmasini gerektiriyor - ImportController.java'da tirnaksiz
-- "value" kullanilinca patladi (VALUE, H2'de "NEXT VALUE FOR sequence"
-- sozdiziminde kullanilan ozel bir kelime). Daha saglam cozum: kolon
-- isimlerini tamamen degistirmek, boylece tirnaklamayla ugrasmaya hic
-- gerek kalmiyor.
CREATE TABLE IF NOT EXISTS app_settings (
    setting_key   VARCHAR(255) PRIMARY KEY,
    setting_value VARCHAR
);