#!/bin/bash
echo "[1/3] Eski sistem durduruluyor..."
docker compose down

echo "[2/3] Maven ile yeni kodlar paketleniyor..."
mvn clean package -DskipTests

echo "[3/3] Docker imajları yeniden oluşturulup başlatılıyor..."
docker compose up --build -d

echo "Her şey hazır! Tarayıcını yenileyebilirsin."