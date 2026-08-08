# Uzay Savaşı 2D — Hikâye, Endless ve Seviye Sistemi

Java Swing ile hazırlanmış, IntelliJ IDEA'da doğrudan açılabilen 2D uzay savaşı projesidir.

## Yeni eklenen sistemler

- Oyuncu seviyesi ve XP çubuğu
- Düşman, asteroit ve boss türüne göre farklı XP ödülleri
- Seviye atlayınca can, hasar, atış hızı ve hareket hızı gelişimi
- Seviye 3'te kalıcı çift mermi
- Seviye 5'te kalıcı üçlü mermi
- Seviye 8'de kalıcı lazer
- Seviye 10'da kalıcı takip füzesi
- Beş bölümlük hikâye modu
- Bölüm geçiş ekranları
- Bölüm kilidi ve bölüm seçme ekranı
- Hikâye ilerlemesinin kaydedilmesi
- Ayrı Endless modu
- Hikâye tamamlanınca zafer ekranı
- En yüksek oyuncu seviyesinin kaydedilmesi

## Hikâye bölümleri

1. **İlk Temas** — Temel düşman filosu
2. **Asteroit Koridoru** — Düşman ve asteroit hedefleri
3. **Düşman Üssü** — Zırhlı ve ateş eden düşmanlar
4. **Kara Delik Kaçışı** — Çekim kuvveti, süre ve hayatta kalma hedefi
5. **Galaksi İçin Son Savaş** — Yoğun final filosu ve güçlü final boss'u

## Mevcut diğer özellikler

- Tekli, çiftli ve üçlü mermi
- Lazer ve takip füzesi
- Mega bomba
- Normal, hızlı, zırhlı, zikzak ve ateşçi düşmanlar
- Düşman mermileri
- Boss savaşları
- Asteroitler ve parçalanan büyük asteroidler
- Güçlendirmeler
- Dört farklı oyuncu gemisi
- Kalıcı coin geliştirmeleri
- Kolay, orta ve zor seviyeleri
- Parçacık, patlama ve ekran sarsıntısı efektleri
- Ses efektleri
- Yüksek skor kaydı

## Kontroller

- `WASD` veya ok tuşları: Hareket
- `SPACE`: Ateş
- `B`: Mega bomba
- `P`: Duraklat / devam et
- `M`: Sesi aç / kapat
- `R`: Oyun bittikten sonra yeniden başlat
- `ESC`: Geri dön / ana menü
- Menülerde ok tuşları, `ENTER` veya fare kullanılabilir.

## IntelliJ IDEA ile açma

1. ZIP dosyasını çıkar.
2. IntelliJ IDEA'yı aç.
3. **File → Open** seçeneğini kullan.
4. `UzaySavasi2D_TamSurum` ana klasörünü seç.
5. Sorulursa **Trust Project** seç.
6. Project SDK olarak Java 17 veya daha yeni bir JDK seç.
7. `src/main/java/Main.java` dosyasını aç.
8. `main` metodunun yanındaki yeşil çalıştırma simgesine bas.

## Terminal ile çalıştırma

Projenin ana klasöründe:

```bash
mkdir -p out
javac --release 17 -encoding UTF-8 -d out src/main/java/*.java
java -cp out Main
```

## Kayıt dosyaları

Coin, geliştirmeler, hikâye ilerlemesi, en yüksek oyuncu seviyesi ve skorlar kullanıcının ev klasöründeki şu dizinde tutulur:

```text
~/.uzay_savasi_2d
```

## 1.1 görünmeyen çarpışma düzeltmesi

- Ekranın altından çıkan düşmanlar artık can azaltmaz.
- Düşman mermileri daha büyük bir parlama ile görünür.
- Hasar alındığında çarpışma kaynağı ekranda yazılır.
- Düşman mermisinin çarpışma alanı görseliyle daha uyumlu hale getirildi.
