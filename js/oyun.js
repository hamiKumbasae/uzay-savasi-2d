'use strict';

// Oyun döngüsü, menü ekranları, klavye/fare/dokunmatik girişi. Oyunun kendisi OyunDunyasi'nda.
(() => {
    const tuval = document.getElementById('oyun');
    const g = tuval.getContext('2d');
    const ses = new SesYoneticisi();
    const kayit = new KayitYoneticisi();
    const yildizlar = Array.from({ length: 105 }, () => new Yildiz());
    const arkaPlanlar = {};

    const MENU_SECENEKLERI = ['HİKÂYE MODU', 'ENDLESS MOD', 'BÖLÜM SEÇ', 'GEMİ SEÇ',
        'GELİŞTİRMELER', 'ZORLUK', 'YÜKSEK SKORLAR', 'KONTROLLER', 'SES'];
    const GELISTIRME_ADLARI = ['Hareket Hızı', 'Atış Hızı', 'Mermi Hasarı',
        'Maksimum Can', 'Kalkan Süresi', 'Bomba Kapasitesi'];
    const GELISTIRME_ANAHTARLARI = ['hiz', 'atis', 'hasar', 'can', 'kalkan', 'bomba'];
    const OYUN_EKRANLARI = ['OYNUYOR', 'DURAKLATILDI', 'OYUN_BITTI', 'BOLUM_GECISI', 'ZAFER'];

    let durum = 'ANA_MENU';
    let zorlukIndex = 1;
    let gemiIndex = Math.max(0, Math.min(GEMILER.length - 1, kayit.getSeciliGemi()));
    let seciliGemi = GEMILER[gemiIndex];
    let dunya = null;
    let menuIndex = 0;
    let gelistirmeIndex = 0;
    let hikayeSecimIndex = kayit.getAcikHikayeBolumu() - 1;
    let fareMenu = -1;
    let fareBolum = -1;
    let bildirimMetni = '';
    let bildirimSuresi = 0;
    let cizimOlcegi = 1;
    // Dokunmatik ekranda ipuçları ve bomba düğmesi baştan dokunmaya göre gösterilir.
    let dokunmatik = window.matchMedia('(pointer: coarse)').matches;
    let surukleme = null;
    let dugmeler = [];
    const basili = new Set();

    ses.setEtkin(kayit.sesAcikMi());

    const dinleyici = {
        bildirim(mesaj) {
            bildirimMetni = mesaj;
            bildirimSuresi = 2.6;
        },
        oyunBitti() { oyunDurumunuDegistir('OYUN_BITTI'); },
        bolumTamamlandi() { oyunDurumunuDegistir('BOLUM_GECISI'); },
        zafer() { oyunDurumunuDegistir('ZAFER'); }
    };

    function oyunDurumunuDegistir(yeni) {
        durum = yeni;
        if (yeni !== 'OYNUYOR') surukleme = null;
    }

    function oyunBaslat(mod, baslangicBolumu) {
        dunya = new OyunDunyasi(mod, baslangicBolumu, seciliGemi, ZORLUKLAR[zorlukIndex], kayit, ses, dinleyici);
        oyunDurumunuDegistir('OYNUYOR');
    }

    function anaMenuyeDon() {
        oyunDurumunuDegistir('ANA_MENU');
        bildirimSuresi = 0;
    }

    function kaydedipMenuyeDon() {
        dunya.kazanciKaydet();
        anaMenuyeDon();
    }

    function yenidenBaslat() {
        const mod = dunya.oyunModu;
        oyunBaslat(mod, mod === OYUN_MODU.HIKAYE ? dunya.bolum : 1);
    }

    function sonrakiBolum() {
        dunya.sonrakiHikayeBolumunuBaslat();
        oyunDurumunuDegistir('OYNUYOR');
    }

    function sesiDegistir() {
        ses.setEtkin(!ses.etkin);
        kayit.setSesAcik(ses.etkin);
    }

    function menuSeciminiCalistir() {
        ses.menu();
        switch (menuIndex) {
            case 0: oyunBaslat(OYUN_MODU.HIKAYE, kayit.getAcikHikayeBolumu()); break;
            case 1: oyunBaslat(OYUN_MODU.ENDLESS, 1); break;
            case 2:
                hikayeSecimIndex = kayit.getAcikHikayeBolumu() - 1;
                durum = 'HIKAYE_BOLUM_SECIMI';
                break;
            case 3: durum = 'GEMI_SECIMI'; break;
            case 4: durum = 'GELISTIRMELER'; break;
            case 5: zorlukIndex = (zorlukIndex + 1) % ZORLUKLAR.length; break;
            case 6: durum = 'YUKSEK_SKORLAR'; break;
            case 7: durum = 'KONTROLLER'; break;
            case 8: sesiDegistir(); break;
        }
    }

    function seciliHikayeBolumunuBaslat() {
        const bolumNo = hikayeSecimIndex + 1;
        if (bolumNo > kayit.getAcikHikayeBolumu()) {
            ses.hasar();
            dinleyici.bildirim('Bu bölüm henüz kilitli');
            return;
        }
        oyunBaslat(OYUN_MODU.HIKAYE, bolumNo);
    }

    function gemiDegistir(yon) {
        gemiIndex = (gemiIndex + yon + GEMILER.length) % GEMILER.length;
    }

    function gemiSec() {
        seciliGemi = GEMILER[gemiIndex];
        kayit.setSeciliGemi(gemiIndex);
        ses.menu();
        anaMenuyeDon();
    }

    function gelistirmeFiyati(seviye) {
        return 60 * (seviye + 1) * (seviye + 1);
    }

    function gelistirmeSatinAl() {
        const anahtar = GELISTIRME_ANAHTARLARI[gelistirmeIndex];
        const seviye = kayit.getGelistirme(anahtar);
        if (seviye >= 5) {
            dinleyici.bildirim('Bu geliştirme maksimum seviyede');
            return;
        }
        const fiyat = gelistirmeFiyati(seviye);
        if (kayit.getCoin() < fiyat) {
            dinleyici.bildirim('Yeterli coin yok');
            ses.hasar();
            return;
        }
        kayit.setCoin(kayit.getCoin() - fiyat);
        kayit.setGelistirme(anahtar, seviye + 1);
        ses.guclendirme();
        dinleyici.bildirim('Geliştirme satın alındı');
    }

    // --- Döngü ---

    let oncekiZaman = performance.now();

    function kare(simdi) {
        const dt = Math.min(0.035, Math.max(0.001, (simdi - oncekiZaman) / 1000));
        oncekiZaman = simdi;
        yildizlar.forEach(y => y.guncelle(dt));
        if (durum === 'OYNUYOR') dunya.guncelle(dt, girdiOku());
        if (dunya) dunya.efektleriGuncelle(dt);
        bildirimSuresi = Math.max(0, bildirimSuresi - dt);
        ciz();
        requestAnimationFrame(kare);
    }

    function tus(...kodlar) {
        return kodlar.some(k => basili.has(k));
    }

    function girdiOku() {
        return {
            sol: tus('ArrowLeft', 'KeyA'),
            sag: tus('ArrowRight', 'KeyD'),
            yukari: tus('ArrowUp', 'KeyW'),
            asagi: tus('ArrowDown', 'KeyS'),
            // Dokunmatikte parmak ekrandayken ateş otomatik.
            ates: tus('Space') || surukleme !== null,
            hedef: surukleme ? { x: surukleme.x + surukleme.ofsetX, y: surukleme.y + surukleme.ofsetY } : null
        };
    }

    // --- Çizim ---

    // Alt ekranlar 820 yüksekliğe göre tasarlandı; uzun (telefon) ekranda dikeyde ortalanır.
    let ofset = 0;

    function menuOfseti() {
        return Math.max(0, (YUKSEKLIK - MENU_YUKSEKLIGI) / 2);
    }

    function ortala() {
        ofset = menuOfseti();
        g.translate(0, ofset);
    }

    function ciz() {
        g.setTransform(cizimOlcegi, 0, 0, cizimOlcegi, 0, 0);
        g.lineWidth = 1;
        g.textBaseline = 'alphabetic';
        dugmeler = [];
        ofset = 0;
        const sarsinti = dunya ? dunya.ekranSarsintisi : 0;
        if (sarsinti > 0) {
            g.translate((Math.random() - 0.5) * sarsinti, (Math.random() - 0.5) * sarsinti);
        }
        arkaPlanCiz();
        switch (durum) {
            case 'ANA_MENU': anaMenuCiz(); break;
            case 'HIKAYE_BOLUM_SECIMI': hikayeBolumSecimiCiz(); break;
            case 'GEMI_SECIMI': gemiSecimiCiz(); break;
            case 'GELISTIRMELER': gelistirmelerCiz(); break;
            case 'YUKSEK_SKORLAR': yuksekSkorCiz(); break;
            case 'KONTROLLER': kontrollerCiz(); break;
            case 'OYNUYOR': oyunCiz(false); break;
            case 'DURAKLATILDI': oyunCiz(true); break;
            case 'BOLUM_GECISI': bolumGecisiCiz(); break;
            case 'OYUN_BITTI': oyunBittiCiz(); break;
            case 'ZAFER': zaferCiz(); break;
        }
        if (!oyunEkraniMi() && bildirimSuresi > 0) {
            Cizim.ortaliYazi(g, bildirimMetni, 712, 20, RENK.YELLOW);
        }
    }

    function oyunEkraniMi() {
        return OYUN_EKRANLARI.includes(durum);
    }

    /** Hem çizer hem de tıklanabilir alan olarak kaydeder. */
    function dugme(metin, x, y, w, h, eylem, vurgulu = false) {
        g.fillStyle = vurgulu ? rgb([40, 130, 180], 225) : rgb([10, 20, 50], 215);
        Cizim.yuvarlakDolu(g, x, y, w, h, 16);
        g.strokeStyle = vurgulu ? rgb(RENK.YELLOW) : rgb([150, 220, 255]);
        g.lineWidth = 2;
        Cizim.yuvarlakCizgi(g, x, y, w, h, 16);
        g.lineWidth = 1;
        g.fillStyle = rgb(RENK.WHITE);
        g.font = Cizim.font('bold', 20);
        const sw = Math.min(g.measureText(metin).width, w - 16);
        Cizim.sigdirYazi(g, metin, x + w / 2 - sw / 2, y + h / 2 + 7, w - 16, 20);
        dugmeler.push({ x, y: y + ofset, w, h, eylem });
    }

    function geriDugmesi() {
        dugme('← GERİ', 16, 10, 120, 42, anaMenuyeDon);
    }

    function ikiDugme(sol, solEylem, sag, sagEylem, y) {
        dugme(sol, GENISLIK / 2 - 185, y, 175, 50, solEylem, true);
        dugme(sag, GENISLIK / 2 + 10, y, 175, 50, sagEylem);
    }

    function arkaPlanCiz() {
        const tema = oyunEkraniMi() ? dunya.temaGetir() : 'UZAY_BOSLUGU';
        if (!arkaPlanlar[tema]) arkaPlanlar[tema] = gradyanOlustur(tema);
        g.drawImage(arkaPlanlar[tema], 0, 0, GENISLIK, YUKSEKLIK);
        yildizlar.forEach(y => y.ciz(g));
        if (tema === 'DUSMAN_USSU') {
            g.fillStyle = rgb([255, 50, 90], 25);
            for (let x = 0; x < GENISLIK; x += 80) g.fillRect(x, 0, 4, YUKSEKLIK);
        } else if (tema === 'KARA_DELIK') {
            g.fillStyle = rgb([130, 50, 230], 45);
            Cizim.ovalDolu(g, GENISLIK / 2 - 220, 125, 440, 440);
            g.fillStyle = rgb([0, 0, 0], 220);
            Cizim.ovalDolu(g, GENISLIK / 2 - 120, 225, 240, 240);
        } else if (tema === 'FINAL') {
            g.fillStyle = rgb([255, 30, 120], 28);
            Cizim.ovalDolu(g, GENISLIK * 0.1, 130, 390, 390);
            g.fillStyle = rgb([90, 30, 255], 25);
            Cizim.ovalDolu(g, GENISLIK * 0.475, 260, 380, 380);
        }
    }

    /** Temanın dikey renk geçişini bir kez çizer; sonraki karelerde hazır resim kullanılır. */
    function gradyanOlustur(tema) {
        const [ust, alt] = {
            ASTEROIT_ALANI: [[18, 10, 28], [45, 24, 16]],
            DUSMAN_USSU: [[5, 18, 35], [35, 5, 18]],
            KARA_DELIK: [[8, 3, 18], [24, 4, 45]],
            FINAL: [[42, 3, 24], [8, 4, 40]]
        }[tema] || [[3, 7, 28], [12, 20, 58]];
        const resim = document.createElement('canvas');
        resim.width = GENISLIK;
        resim.height = YUKSEKLIK;
        const rg = resim.getContext('2d');
        for (let y = 0; y < YUKSEKLIK; y += 4) {
            const t = y / YUKSEKLIK;
            rg.fillStyle = rgb(ust.map((u, i) => Math.trunc(u * (1 - t) + alt[i] * t)));
            rg.fillRect(0, y, GENISLIK, 4);
        }
        return resim;
    }

    function karart(opaklik) {
        g.fillStyle = rgb([0, 0, 0], opaklik);
        g.fillRect(0, 0, GENISLIK, YUKSEKLIK);
        ortala();
    }

    function oyunCiz(duraklatildi) {
        dunya.ciz(g);
        if (bildirimSuresi > 0) Cizim.ortaliYazi(g, bildirimMetni, 232, 22, RENK.YELLOW);
        if (duraklatildi) {
            karart(180);
            Cizim.ortaliYazi(g, 'DURAKLATILDI', 360, 44, RENK.WHITE);
            if (!dokunmatik) {
                Cizim.ortaliYazi(g, 'Devam etmek için P', 410, 22, RENK.LIGHT_GRAY);
                Cizim.ortaliYazi(g, 'Ana menü için ESC', 444, 18, RENK.LIGHT_GRAY);
            }
            ikiDugme('DEVAM', () => oyunDurumunuDegistir('OYNUYOR'), 'MENÜ', kaydedipMenuyeDon, 490);
            return;
        }
        if (durum !== 'OYNUYOR') return;
        dugme('II', GENISLIK - 72, 122, 56, 44, () => oyunDurumunuDegistir('DURAKLATILDI'));
        if (dokunmatik) {
            dugme(`B ${dunya.oyuncu.bombaSayisi}`, GENISLIK - 110, YUKSEKLIK - 90, 90, 70,
                () => dunya.bombaKullan());
        }
    }

    /** Ana menü uzun ekranda düğmeleri büyütür ve dikeyde ortalar. */
    function menuDuzeni() {
        const aralik = Math.min(72, Math.max(52, (YUKSEKLIK - 340) / MENU_SECENEKLERI.length));
        const toplam = aralik * MENU_SECENEKLERI.length;
        return {
            aralik,
            bas: Math.max(175, (YUKSEKLIK - toplam) / 2 + 30),
            w: Math.min(470, GENISLIK - 60),
            h: Math.round(aralik * 0.75)
        };
    }

    function menuButonAlani(i) {
        const d = menuDuzeni();
        return { x: GENISLIK / 2 - d.w / 2, y: d.bas + i * d.aralik, w: d.w, h: d.h };
    }

    function anaMenuCiz() {
        const d = menuDuzeni();
        Cizim.ortaliYazi(g, 'UZAY SAVAŞI', d.bas - 79, 52, [80, 230, 255]);
        Cizim.ortaliYazi(g, 'HİKÂYE + ENDLESS + XP SİSTEMİ', d.bas - 43, 20, [255, 200, 60]);
        for (let i = 0; i < MENU_SECENEKLERI.length; i++) {
            const alan = menuButonAlani(i);
            const secili = i === menuIndex || i === fareMenu;
            g.fillStyle = secili ? rgb([40, 130, 180], 215) : rgb([10, 20, 50], 190);
            Cizim.yuvarlakDolu(g, alan.x, alan.y, alan.w, alan.h, 16);
            g.strokeStyle = secili ? rgb(RENK.YELLOW) : rgb([150, 220, 255]);
            g.lineWidth = secili ? 3 : 1.5;
            Cizim.yuvarlakCizgi(g, alan.x, alan.y, alan.w, alan.h, 16);
            const metin = i === 0 ? `HİKÂYE MODU - DEVAM: BÖLÜM ${kayit.getAcikHikayeBolumu()}`
                : i === 5 ? `ZORLUK: ${ZORLUKLAR[zorlukIndex].ad.toLocaleUpperCase('tr')}`
                    : i === 8 ? `SES: ${ses.etkin ? 'AÇIK' : 'KAPALI'}`
                        : MENU_SECENEKLERI[i];
            g.font = Cizim.font('bold', 19);
            g.fillStyle = secili ? rgb(RENK.YELLOW) : rgb([150, 220, 255]);
            g.fillText(metin, GENISLIK / 2 - g.measureText(metin).width / 2, alan.y + alan.h / 2 + 7);
        }
        g.lineWidth = 1;
        g.font = Cizim.font('normal', 15);
        g.fillStyle = rgb(RENK.LIGHT_GRAY);
        g.fillText(dokunmatik ? 'Seçmek için dokun' : '↑ ↓ / Fare: Seç   ENTER / Tıkla: Onayla',
            20, YUKSEKLIK - 25);
        const bilgi = `Coin: ${kayit.getCoin()}   En yüksek seviye: ${kayit.getEnYuksekOyuncuSeviyesi()}`;
        g.fillText(bilgi, GENISLIK - 20 - g.measureText(bilgi).width, YUKSEKLIK - 25);
        if (kayit.hikayeTamamlandiMi()) {
            Cizim.ortaliYazi(g, 'Hikâye tamamlandı! Bölümleri yeniden oynayabilirsin.',
                YUKSEKLIK - 60, 15, [120, 255, 150]);
        }
    }

    function bolumSecimAlani(i) {
        const w = Math.min(650, GENISLIK - 40);
        return { x: (GENISLIK - w) / 2, y: 135 + i * 112, w, h: 82 };
    }

    function hikayeBolumSecimiCiz() {
        ortala();
        geriDugmesi();
        Cizim.ortaliYazi(g, 'HİKÂYE BÖLÜMLERİ', 85, 40, RENK.CYAN);
        const acikBolum = kayit.getAcikHikayeBolumu();
        HIKAYE_BOLUMLERI.forEach((hb, i) => {
            const alan = bolumSecimAlani(i);
            const acik = hb.numara <= acikBolum;
            const secili = i === hikayeSecimIndex || i === fareBolum;
            g.fillStyle = !acik ? rgb([35, 35, 48], 205) : secili ? rgb([40, 105, 155], 220) : rgb([10, 25, 55], 205);
            Cizim.yuvarlakDolu(g, alan.x, alan.y, alan.w, alan.h, 18);
            const kenar = !acik ? RENK.GRAY : secili ? RENK.YELLOW : RENK.CYAN;
            g.strokeStyle = rgb(kenar);
            g.lineWidth = secili ? 3 : 1.5;
            Cizim.yuvarlakCizgi(g, alan.x, alan.y, alan.w, alan.h, 18);
            g.fillStyle = rgb(kenar);
            Cizim.sigdirYazi(g, `Bölüm ${hb.numara} - ${hb.ad}`, alan.x + 20, alan.y + 31, alan.w - 40, 21);
            g.fillStyle = rgb(acik ? RENK.WHITE : RENK.GRAY);
            Cizim.sigdirYazi(g, acik ? hb.aciklama : 'KİLİTLİ - Önceki bölümü tamamla',
                alan.x + 20, alan.y + 58, alan.w - 40, 15, 'normal');
        });
        g.lineWidth = 1;
        Cizim.ortaliYazi(g, dokunmatik ? 'Başlatmak için bölüme dokun' : '↑ ↓ seç   ENTER başlat   ESC geri',
            760, 18, RENK.LIGHT_GRAY);
    }

    function gemiSecimiCiz() {
        ortala();
        geriDugmesi();
        Cizim.ortaliYazi(g, 'GEMİ SEÇİMİ', 110, 42, RENK.CYAN);
        const gemi = GEMILER[gemiIndex];
        const pw = Math.min(550, GENISLIK - 40);
        const px = (GENISLIK - pw) / 2;
        g.fillStyle = 'rgba(0,0,0,0.61)';
        Cizim.yuvarlakDolu(g, px, 175, pw, 430, 28);
        new Oyuncu(GENISLIK / 2 - 28, 235, gemi, 0, 0, 0, 0).ciz(g);
        g.lineWidth = 1;
        dugme('‹', px + 20, 240, 64, 80, () => gemiDegistir(-1));
        dugme('›', px + pw - 84, 240, 64, 80, () => gemiDegistir(1));
        Cizim.ortaliYazi(g, gemi.ad, 390, 30, gemi.renk);
        Cizim.ortaliYazi(g, gemi.aciklama, 430, 19, RENK.WHITE);
        ozellikCubuguCiz('Hız', px, pw, 480, gemi.hiz / 1.5);
        ozellikCubuguCiz('Dayanıklılık', px, pw, 520, gemi.can / 1.5);
        ozellikCubuguCiz('Hasar', px, pw, 560, gemi.hasar / 1.5);
        dugme('BU GEMİYİ SEÇ', GENISLIK / 2 - 120, 620, 240, 46, gemiSec, true);
        Cizim.ortaliYazi(g, dokunmatik ? '‹ › ile gemi değiştir' : '← → gemi değiştir   ENTER seç   ESC geri',
            700, 18, RENK.LIGHT_GRAY);
    }

    function ozellikCubuguCiz(ad, px, pw, y, oran) {
        g.font = Cizim.font('bold', 17);
        g.fillStyle = rgb(RENK.WHITE);
        g.fillText(ad, px + 30, y);
        const x = px + 160;
        const w = pw - 190;
        g.fillStyle = rgb([45, 45, 65]);
        Cizim.yuvarlakDolu(g, x, y - 17, w, 18, 10);
        g.fillStyle = rgb([50, 220, 255]);
        Cizim.yuvarlakDolu(g, x, y - 17, Math.trunc(w * Math.min(1, oran)), 18, 10);
    }

    function gelistirmeAlani(i) {
        const w = Math.min(590, GENISLIK - 40);
        return { x: (GENISLIK - w) / 2, y: 205 + i * 82 - 42, w, h: 64 };
    }

    function gelistirmelerCiz() {
        ortala();
        geriDugmesi();
        Cizim.ortaliYazi(g, 'GELİŞTİRMELER', 90, 40, RENK.YELLOW);
        Cizim.ortaliYazi(g, `Coin: ${kayit.getCoin()}`, 128, 21, RENK.WHITE);
        GELISTIRME_ADLARI.forEach((ad, i) => {
            const y = 205 + i * 82;
            const alan = gelistirmeAlani(i);
            const seviye = kayit.getGelistirme(GELISTIRME_ANAHTARLARI[i]);
            const secili = i === gelistirmeIndex;
            g.fillStyle = secili ? rgb([45, 110, 160], 210) : rgb([10, 20, 45], 185);
            Cizim.yuvarlakDolu(g, alan.x, alan.y, alan.w, alan.h, 18);
            g.strokeStyle = rgb(secili ? RENK.YELLOW : RENK.CYAN);
            Cizim.yuvarlakCizgi(g, alan.x, alan.y, alan.w, alan.h, 18);
            g.fillStyle = rgb(secili ? RENK.YELLOW : RENK.CYAN);
            Cizim.sigdirYazi(g, ad, alan.x + 25, y - 9, alan.w * 0.53 - 35, 20);
            g.font = Cizim.font('normal', 17);
            g.fillStyle = rgb(RENK.WHITE);
            g.fillText(`Seviye: ${seviye}/5`, alan.x + alan.w * 0.53, y - 9);
            g.fillText(seviye >= 5 ? 'MAKSİMUM' : `Fiyat: ${gelistirmeFiyati(seviye)}`, alan.x + alan.w * 0.75, y - 9);
        });
        dugme('SATIN AL', GENISLIK / 2 - 100, 650, 200, 44, gelistirmeSatinAl, true);
        Cizim.ortaliYazi(g, dokunmatik ? 'Seçmek için dokun, sonra SATIN AL' : '↑ ↓ seç   ENTER satın al   ESC geri',
            745, 18, RENK.LIGHT_GRAY);
    }

    function yuksekSkorCiz() {
        ortala();
        geriDugmesi();
        Cizim.ortaliYazi(g, 'YÜKSEK SKORLAR', 125, 42, RENK.YELLOW);
        const skorlar = kayit.yuksekSkorlar();
        if (skorlar.length === 0) Cizim.ortaliYazi(g, 'Henüz kayıtlı skor yok', 330, 24, RENK.LIGHT_GRAY);
        skorlar.forEach((skor, i) => {
            Cizim.ortaliYazi(g, `${i + 1}.   ${skor}`, 245 + i * 65, 30, i === 0 ? RENK.YELLOW : RENK.WHITE);
        });
        Cizim.ortaliYazi(g, `En yüksek oyuncu seviyesi: ${kayit.getEnYuksekOyuncuSeviyesi()}`, 610, 21, RENK.CYAN);
    }

    function kontrollerCiz() {
        ortala();
        geriDugmesi();
        Cizim.ortaliYazi(g, 'KONTROLLER VE SEVİYE SİSTEMİ', 110, 36, RENK.CYAN);
        const satirlar = [
            'WASD / Ok Tuşları  : Hareket',
            'SPACE              : Ateş',
            'B                  : Mega bomba',
            'P                  : Duraklat',
            'M                  : Sesi aç/kapat',
            'R                  : Oyun bitince yeniden başlat',
            'ESC                : Menüye dön',
            'Dokunmatik         : Sürükle, ateş otomatik',
            '',
            'Düşman ve asteroit yok ederek XP kazanırsın.',
            'Sv.3 Çiftli, Sv.5 Üçlü, Sv.8 Lazer, Sv.10 Füze.'
        ];
        // Eş aralıklı yazı hizayı korur; dar ekranda tüm satırlar birlikte küçülür.
        const aile = 'Menlo, Consolas, monospace';
        g.font = Cizim.font('bold', 18, aile);
        const enUzun = Math.max(...satirlar.map(s => g.measureText(s).width));
        const boyut = Math.min(18, Math.floor(18 * (GENISLIK - 40) / enUzun));
        g.font = Cizim.font('bold', boyut, aile);
        g.fillStyle = rgb(RENK.WHITE);
        const x = (GENISLIK - enUzun * boyut / 18) / 2;
        satirlar.forEach((s, i) => g.fillText(s, x, 175 + i * 46));
    }

    function karartmaCiz(opaklik) {
        oyunCiz(false);
        karart(opaklik);
    }

    function bolumGecisiCiz() {
        karartmaCiz(218);
        const bolum = dunya.bolum;
        Cizim.ortaliYazi(g, `BÖLÜM ${bolum} TAMAMLANDI`, 255, 42, RENK.YELLOW);
        Cizim.ortaliYazi(g, hikayeBolumu(bolum).ad, 305, 25, RENK.CYAN);
        Cizim.ortaliYazi(g, `Skor: ${dunya.skor}   Coin: ${dunya.coin}   Oyuncu seviyesi: ${dunya.seviyeSistemi.seviye}`,
            370, 21, RENK.WHITE);
        const sonraki = hikayeBolumu(dunya.sonrakiBolum);
        Cizim.ortaliYazi(g, `Sıradaki: Bölüm ${sonraki.numara} - ${sonraki.ad}`, 440, 25, [120, 230, 255]);
        Cizim.ortaliYazi(g, sonraki.aciklama, 480, 17, RENK.LIGHT_GRAY);
        Cizim.ortaliYazi(g, 'Devam etmek için ENTER', 560, 23, RENK.WHITE);
        Cizim.ortaliYazi(g, 'Kaydedip menüye dönmek için ESC', 600, 17, RENK.LIGHT_GRAY);
        ikiDugme('DEVAM', sonrakiBolum, 'MENÜ', kaydedipMenuyeDon, 635);
    }

    function oyunBittiCiz() {
        karartmaCiz(205);
        const mod = dunya.oyunModu;
        Cizim.ortaliYazi(g, 'OYUN BİTTİ', 275, 56, [255, 70, 100]);
        Cizim.ortaliYazi(g, mod.ad, 325, 24, RENK.CYAN);
        Cizim.ortaliYazi(g, `Skor: ${dunya.skor}`, 370, 32, RENK.WHITE);
        Cizim.ortaliYazi(g, `Kazanılan coin: ${dunya.coin}`, 414, 24, RENK.YELLOW);
        Cizim.ortaliYazi(g, `Oyuncu seviyesi: ${dunya.seviyeSistemi.seviye}`, 453, 22, RENK.WHITE);
        Cizim.ortaliYazi(g, mod === OYUN_MODU.HIKAYE
            ? `Ulaşılan hikâye bölümü: ${dunya.bolum}`
            : `Ulaşılan endless dalgası: ${dunya.bolum}`, 490, 20, RENK.LIGHT_GRAY);
        Cizim.ortaliYazi(g, 'R: Bölümü yeniden başlat', 555, 22, RENK.CYAN);
        Cizim.ortaliYazi(g, 'ESC: Ana menü', 595, 20, RENK.LIGHT_GRAY);
        ikiDugme('YENİDEN', yenidenBaslat, 'MENÜ', anaMenuyeDon, 630);
    }

    function zaferCiz() {
        karartmaCiz(215);
        Cizim.ortaliYazi(g, 'GALAKSİ KURTARILDI!', 245, 48, RENK.YELLOW);
        Cizim.ortaliYazi(g, 'HİKÂYE MODUNU TAMAMLADIN', 300, 27, RENK.CYAN);
        Cizim.ortaliYazi(g, `Final skor: ${dunya.skor}`, 370, 30, RENK.WHITE);
        Cizim.ortaliYazi(g, `Toplanan coin: ${dunya.coin}`, 415, 23, RENK.YELLOW);
        Cizim.ortaliYazi(g, `Ulaşılan oyuncu seviyesi: ${dunya.seviyeSistemi.seviye}`, 455, 22, RENK.WHITE);
        Cizim.ortaliYazi(g, `Silah: ${SILAH_ADI[dunya.oyuncu.kaliciSilah]}`, 492, 20, RENK.LIGHT_GRAY);
        Cizim.ortaliYazi(g, 'R: Hikâyeyi baştan oyna', 565, 22, RENK.CYAN);
        Cizim.ortaliYazi(g, 'ESC: Ana menü', 605, 20, RENK.LIGHT_GRAY);
        ikiDugme('BAŞTAN', () => oyunBaslat(OYUN_MODU.HIKAYE, 1), 'MENÜ', anaMenuyeDon, 640);
    }

    // --- Klavye ---

    const KAYDIRMA_TUSLARI = ['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'Space'];

    window.addEventListener('keydown', e => {
        ses.hazirla();
        if (KAYDIRMA_TUSLARI.includes(e.code)) e.preventDefault();
        basili.add(e.code);
        if (e.repeat) return;
        const k = e.code;
        switch (durum) {
            case 'ANA_MENU':
                if (k === 'ArrowUp') menuIndex = (menuIndex - 1 + MENU_SECENEKLERI.length) % MENU_SECENEKLERI.length;
                else if (k === 'ArrowDown') menuIndex = (menuIndex + 1) % MENU_SECENEKLERI.length;
                else if (k === 'Enter') menuSeciminiCalistir();
                break;
            case 'HIKAYE_BOLUM_SECIMI': {
                const n = HIKAYE_BOLUMLERI.length;
                if (k === 'ArrowUp') hikayeSecimIndex = (hikayeSecimIndex - 1 + n) % n;
                else if (k === 'ArrowDown') hikayeSecimIndex = (hikayeSecimIndex + 1) % n;
                else if (k === 'Enter') seciliHikayeBolumunuBaslat();
                else if (k === 'Escape') anaMenuyeDon();
                break;
            }
            case 'GEMI_SECIMI':
                if (k === 'ArrowLeft') gemiDegistir(-1);
                else if (k === 'ArrowRight') gemiDegistir(1);
                else if (k === 'Enter') gemiSec();
                else if (k === 'Escape') anaMenuyeDon();
                break;
            case 'GELISTIRMELER': {
                const n = GELISTIRME_ADLARI.length;
                if (k === 'ArrowUp') gelistirmeIndex = (gelistirmeIndex - 1 + n) % n;
                else if (k === 'ArrowDown') gelistirmeIndex = (gelistirmeIndex + 1) % n;
                else if (k === 'Enter') gelistirmeSatinAl();
                else if (k === 'Escape') anaMenuyeDon();
                break;
            }
            case 'YUKSEK_SKORLAR':
            case 'KONTROLLER':
                if (k === 'Escape' || k === 'Enter') anaMenuyeDon();
                break;
            case 'OYNUYOR':
                if (k === 'KeyB') dunya.bombaKullan();
                else if (k === 'KeyP') oyunDurumunuDegistir('DURAKLATILDI');
                else if (k === 'KeyM') sesiDegistir();
                else if (k === 'Escape') kaydedipMenuyeDon();
                break;
            case 'DURAKLATILDI':
                if (k === 'KeyP') oyunDurumunuDegistir('OYNUYOR');
                else if (k === 'Escape') kaydedipMenuyeDon();
                break;
            case 'BOLUM_GECISI':
                if (k === 'Enter') sonrakiBolum();
                else if (k === 'Escape') kaydedipMenuyeDon();
                break;
            case 'OYUN_BITTI':
                if (k === 'KeyR') yenidenBaslat();
                else if (k === 'Escape') anaMenuyeDon();
                break;
            case 'ZAFER':
                if (k === 'KeyR') oyunBaslat(OYUN_MODU.HIKAYE, 1);
                else if (k === 'Escape') anaMenuyeDon();
                break;
        }
    });

    window.addEventListener('keyup', e => basili.delete(e.code));

    // --- Fare ve dokunmatik ---

    function konum(e) {
        const r = tuval.getBoundingClientRect();
        return { x: (e.clientX - r.left) * GENISLIK / r.width, y: (e.clientY - r.top) * YUKSEKLIK / r.height };
    }

    /** Dikeyde ortalanmış alt ekranların kendi koordinatına çevirir. */
    function menuNoktasi(p) {
        return { x: p.x, y: p.y - menuOfseti() };
    }

    function alanIndeksi(p, adet, alanFn) {
        for (let i = 0; i < adet; i++) if (iceride(alanFn(i), p)) return i;
        return -1;
    }

    tuval.addEventListener('pointerdown', e => {
        ses.hazirla();
        if (e.pointerType === 'touch') dokunmatik = true;
        e.preventDefault();
        const p = konum(e);
        const d = dugmeler.find(b => iceride(b, p));
        if (d) {
            d.eylem();
            return;
        }
        if (durum === 'ANA_MENU') {
            const i = alanIndeksi(p, MENU_SECENEKLERI.length, menuButonAlani);
            if (i >= 0) {
                menuIndex = i;
                menuSeciminiCalistir();
            }
        } else if (durum === 'HIKAYE_BOLUM_SECIMI') {
            const i = alanIndeksi(menuNoktasi(p), HIKAYE_BOLUMLERI.length, bolumSecimAlani);
            if (i >= 0) {
                hikayeSecimIndex = i;
                seciliHikayeBolumunuBaslat();
            }
        } else if (durum === 'GELISTIRMELER') {
            const i = alanIndeksi(menuNoktasi(p), GELISTIRME_ADLARI.length, gelistirmeAlani);
            if (i >= 0) gelistirmeIndex = i;
        } else if (durum === 'OYNUYOR' && surukleme === null) {
            // Parmak gemiyi örtmesin diye gemi, dokunulan noktaya göre aynı mesafede kalır.
            try {
                tuval.setPointerCapture(e.pointerId);
            } catch (hata) {
                // Yakalama desteklenmiyorsa sürükleme yine çalışır; yalnızca tuval dışına taşınca kopar.
            }
            const o = dunya.oyuncu;
            surukleme = { id: e.pointerId, x: p.x, y: p.y, ofsetX: o.merkezX() - p.x, ofsetY: o.merkezY() - p.y };
        }
    });

    tuval.addEventListener('pointermove', e => {
        const p = konum(e);
        if (surukleme && e.pointerId === surukleme.id) {
            surukleme.x = p.x;
            surukleme.y = p.y;
        }
        if (e.pointerType === 'mouse') {
            fareMenu = durum === 'ANA_MENU' ? alanIndeksi(p, MENU_SECENEKLERI.length, menuButonAlani) : -1;
            fareBolum = durum === 'HIKAYE_BOLUM_SECIMI'
                ? alanIndeksi(menuNoktasi(p), HIKAYE_BOLUMLERI.length, bolumSecimAlani) : -1;
        }
    });

    function suruklemeBitir(e) {
        if (surukleme && e.pointerId === surukleme.id) surukleme = null;
    }
    tuval.addEventListener('pointerup', suruklemeBitir);
    tuval.addEventListener('pointercancel', suruklemeBitir);
    tuval.addEventListener('pointerleave', () => {
        fareMenu = -1;
        fareBolum = -1;
    });

    // Sekme gizlenince ya da pencere odağı kaybedince oyun kendiliğinden durur.
    function odakKaybi() {
        basili.clear();
        surukleme = null;
        if (durum === 'OYNUYOR') oyunDurumunuDegistir('DURAKLATILDI');
    }
    document.addEventListener('visibilitychange', () => {
        if (document.hidden) odakKaybi();
    });
    window.addEventListener('blur', odakKaybi);
    document.addEventListener('pause', odakKaybi); // Android uygulaması (Capacitor) arka plana geçince

    /**
     * Android geri tuşu: oyunda duraklatır, duraklatılmışken ve alt ekranlarda ana menüye döner.
     * Ana menüde false döner; uygulama arka plana alınır.
     */
    function geri() {
        switch (durum) {
            case 'ANA_MENU': return false;
            case 'OYNUYOR': oyunDurumunuDegistir('DURAKLATILDI'); break;
            case 'DURAKLATILDI':
            case 'BOLUM_GECISI': kaydedipMenuyeDon(); break;
            default: anaMenuyeDon();
        }
        return true;
    }

    // --- Ekrana sığdırma ---

    function boyutla() {
        if (alaniAyarla(window.innerWidth, window.innerHeight)) {
            for (const tema of Object.keys(arkaPlanlar)) delete arkaPlanlar[tema];
            yildizlar.forEach(y => {
                y.x = Math.random() * GENISLIK;
                y.y = Math.random() * YUKSEKLIK;
            });
            // Ekran döndürülünce alan değişir; oyuncu hazır olana kadar oyun bekler.
            if (durum === 'OYNUYOR') oyunDurumunuDegistir('DURAKLATILDI');
        }
        const olcek = Math.min(window.innerWidth / GENISLIK, window.innerHeight / YUKSEKLIK);
        const dpr = window.devicePixelRatio || 1;
        tuval.style.width = `${GENISLIK * olcek}px`;
        tuval.style.height = `${YUKSEKLIK * olcek}px`;
        tuval.width = Math.round(GENISLIK * olcek * dpr);
        tuval.height = Math.round(YUKSEKLIK * olcek * dpr);
        cizimOlcegi = tuval.width / GENISLIK;
    }
    window.addEventListener('resize', boyutla);
    boyutla();

    window.uzaySavasi = { durum: () => durum, dunya: () => dunya, geri };
    requestAnimationFrame(kare);
})();
