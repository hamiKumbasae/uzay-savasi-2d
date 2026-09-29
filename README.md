# Uzay Savaşı 2D — Hikâye, Endless ve Seviye Sistemi

2D uzay savaşı oyunu. İki sürümü var:

- **Web sürümü** (`index.html`, `css/`, `js/`): tarayıcıda, bilgisayarda ve telefonda oynanır. Kurulum gerekmez; `index.html` dosyasını açmak yeterli.
- **Java sürümü** (`src/main/java/uzaysavasi/`): Java Swing ile masaüstü uygulaması.
- **Android sürümü** (`android/`): web sürümünün [Capacitor](https://capacitorjs.com) ile paketlenmiş hâli, telefona APK olarak kurulur.

İki sürüm aynı oyun kurallarını kullanır. Web sürümünde ayrıca dokunmatik kontrol var: parmakla sürükle, ateş otomatik; ekrandaki `II` duraklatır, `B` bomba atar. Kayıtlar web sürümünde tarayıcıda (localStorage), Java sürümünde `~/.uzay_savasi_2d` klasöründe tutulur.

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

## Android (Capacitor)

Web sürümü, kodu değiştirilmeden Android uygulaması olarak paketlenir. Oyun dosyaları uygulamanın içindedir, internet gerekmez, reklam yoktur. Kayıtlar uygulama verisinde saklanır ve uygulama güncellenince korunur. Uygulama dikey ve tam ekran açılır.

- Ekranda parmakla sürükle, ateş otomatik; `II` duraklatır, `B` bomba atar.
- Android'in geri tuşu oyunda duraklatır, duraklatılmışken ve alt ekranlarda ana menüye döner, ana menüde uygulamayı arka plana alır.
- Uygulamadan çıkınca, bildirim açılınca ya da ekran kilitlenince oyun kendiliğinden duraklar.

**Hazır test APK'sı:** `main` ya da `claude/` dallarına her gönderimde GitHub Actions APK'yı derler ve [Test APK sürümüne](https://github.com/hamiKumbasae/uzay-savasi-2d/releases/tag/test-apk) yükler: [uzay-savasi-2d.apk](https://github.com/hamiKumbasae/uzay-savasi-2d/releases/download/test-apk/uzay-savasi-2d.apk). Telefonda indirip açınca "bilinmeyen kaynaklardan yükleme" izni istenir. Tüm test APK'ları aynı test anahtarıyla imzalanır (`android/app/test.keystore`, mağaza anahtarı değildir); yeni sürüm eskisinin üstüne kurulur, kayıtlar silinmez.

Kendin derlemek için gerekenler: Node.js 22+, Android Studio (Android SDK ve JDK 21).

```
npm install
npm run sync      # web dosyalarını www/ klasörüne kopyalar ve android/ projesine aktarır
npm run open      # Android Studio'da açar (Run ile telefona kurulur)
npm run apk       # ya da doğrudan test APK'sı: android/app/build/outputs/apk/debug/app-debug.apk
```

`index.html`, `css/` veya `js/` değiştiğinde `npm run sync` yeniden çalıştırılmalı. `www/` üretilen bir klasördür, depoya eklenmez.

## IntelliJ IDEA ile açma

1. IntelliJ IDEA'yı aç.
2. **File → Open** ile `uzay-savasi-2d` klasörünü seç.
3. Sorulursa **Trust Project** seç.
4. Project SDK olarak Java 17 veya daha yeni bir JDK seç.
5. `src/main/java/uzaysavasi/Main.java` dosyasını aç.
6. `main` metodunun yanındaki yeşil çalıştırma simgesine bas.

## Terminal ile çalıştırma

Projenin ana klasöründe:

```bash
mkdir -p out
javac --release 17 -encoding UTF-8 -d out src/main/java/uzaysavasi/*.java
java -cp out uzaysavasi.Main
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

## Kod yapısı

Web sürümü (`js/`):

- `oyun.js` — oyun döngüsü, menü ekranları, klavye/fare/dokunmatik girişi, ekrana sığdırma
- `dunya.js` — tek bir oyun oturumu: düşmanlar, mermiler, çarpışmalar, skor, HUD
- `varliklar.js` — oyuncu, düşman, boss, mermi, asteroit, güçlendirme, parçacık, yıldız
- `veri.js` — gemiler, zorluklar, hikâye bölümleri, silah ve güçlendirme adları
- `altyapi.js` — ses, kayıt ve seviye sistemi
- `cizim.js` — ekran boyutu, renkler ve çizim yardımcıları

Java sürümü (`src/main/java/uzaysavasi/`):

- `OyunPaneli` — oyun döngüsü, menü ekranları, klavye/fare girişi
- `OyunDunyasi` — tek bir oyun oturumu: düşmanlar, mermiler, çarpışmalar, skor, HUD
- `SesYoneticisi` — efektleri açılışta üretir, çalarken hazır klibi kullanır
- `KayitYoneticisi` — coin, geliştirme, ilerleme ve skor kaydı
- Diğer sınıflar oyundaki tek tek nesneler (`Oyuncu`, `Dusman`, `Boss`, `Mermi`...) ve türleri (`*Turu`, `Zorluk`, `HikayeBolumu`)
