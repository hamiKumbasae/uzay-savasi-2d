'use strict';

/**
 * Tek bir oyun oturumu: gemiler, mermiler, çarpışmalar, skor ve HUD.
 * Ekran akışını değiştiren olayları dinleyici üzerinden oyuna bildirir:
 * bildirim(mesaj), oyunBitti(), bolumTamamlandi(), zafer().
 */
class OyunDunyasi {
    constructor(mod, baslangicBolumu, gemi, zorluk, kayit, ses, dinleyici) {
        this.oyunModu = mod;
        this.zorluk = zorluk;
        this.kayit = kayit;
        this.ses = ses;
        this.dinleyici = dinleyici;

        this.oyuncuMermileri = [];
        this.dusmanMermileri = [];
        this.dusmanlar = [];
        this.asteroitler = [];
        this.guclendirmeler = [];
        this.parcaciklar = [];
        this.seviyeSistemi = new SeviyeSistemi();
        this.hikayeBolumu = HIKAYE_BOLUMLERI[0];
        this.boss = null;

        this.ekranSarsintisi = 0;
        this.skor = 0;
        this.coin = 0;
        this.sonrakiBolum = 0;
        this.skorKaydedildi = false;
        this.bolum = mod === OYUN_MODU.HIKAYE ? Math.max(1, Math.min(5, baslangicBolumu)) : 1;
        this.oyuncu = new Oyuncu(GENISLIK / 2 - 28, YUKSEKLIK - 120, gemi,
            kayit.getGelistirme('hiz'), kayit.getGelistirme('can'),
            kayit.getGelistirme('hasar'), kayit.getGelistirme('bomba'));
        this.bolumuHazirla(false);
        dinleyici.bildirim(this.aktifBolumBasligi());
    }

    bolumuHazirla(oyuncuyuKoru) {
        for (const liste of [this.oyuncuMermileri, this.dusmanMermileri, this.dusmanlar,
            this.asteroitler, this.guclendirmeler, this.parcaciklar]) {
            liste.length = 0;
        }
        this.boss = null;
        this.bossBekleniyor = false;
        this.oldurulenDusman = 0;
        this.yokEdilenAsteroit = 0;
        this.bolumSuresi = 0;
        this.dusmanDogmaSayaci = 0.65;
        this.asteroitDogmaSayaci = 1.5;
        this.atisBekleme = 0;
        if (this.oyunModu === OYUN_MODU.HIKAYE) {
            this.hikayeBolumu = hikayeBolumu(this.bolum);
            this.hedefDusman = this.hikayeBolumu.dusman;
        } else {
            this.hedefDusman = 18 + (this.bolum - 1) * 4;
        }
        if (oyuncuyuKoru) this.oyuncu.canEkle(1);
    }

    /** girdi: { sol, sag, yukari, asagi, ates, hedef: {x, y} | null } */
    guncelle(dt, girdi) {
        this.bolumSuresi += dt;
        this.oyuncu.guncelle(girdi, dt);
        this.karaDelikKuvvetiUygula(dt);
        this.atisBekleme = Math.max(0, this.atisBekleme - dt);

        if (girdi.ates && this.atisBekleme <= 0) this.oyuncuAtesEt();

        if (this.boss === null && !this.bossBekleniyor) {
            this.dusmanVeAsteroitOlustur(dt);
            if (this.bolumHedefiTamamMi()) this.bossOlustur();
        }

        this.dusmanlariGuncelle(dt);
        this.asteroitleriGuncelle(dt);
        this.mermileriGuncelle(dt);
        this.guclendirmeleriGuncelle(dt);
        if (this.boss !== null) this.bossGuncelle(dt);
        this.carpismalariKontrolEt();

        if (!this.oyuncu.hayattaMi()) {
            this.kazanciKaydet();
            this.dinleyici.oyunBitti();
        }
    }

    /** Oyun dursa da (duraklatma, oyun sonu) patlama ve sarsıntı sönmeye devam eder. */
    efektleriGuncelle(dt) {
        this.parcaciklar.forEach(p => p.guncelle(dt));
        this.parcaciklar = this.parcaciklar.filter(p => p.aktifMi());
        this.ekranSarsintisi = Math.max(0, this.ekranSarsintisi - dt * 18);
    }

    dusmanVeAsteroitOlustur(dt) {
        this.dusmanDogmaSayaci -= dt;
        let temelAralik = Math.max(0.34, 1.15 - this.bolum * 0.045);
        if (this.oyunModu === OYUN_MODU.HIKAYE && this.bolum === 5) temelAralik *= 0.78;
        const aralik = temelAralik * this.zorluk.dogmaSuresi;
        if (this.dusmanDogmaSayaci <= 0) {
            this.dusmanlar.push(new Dusman(this.bolum, this.zorluk.dusmanHiz, this.dusmanTuruSec()));
            this.dusmanDogmaSayaci = aralik * (0.78 + Math.random() * 0.48);
        }

        if (this.asteroitOlusturulmali()) {
            this.asteroitDogmaSayaci -= dt;
            if (this.asteroitDogmaSayaci <= 0) {
                const buyukOlasiligi = this.temaGetir() === 'ASTEROIT_ALANI' ? 0.38 : 0.22;
                this.asteroitler.push(Asteroit.yeni(this.bolum, Math.random() < buyukOlasiligi));
                this.asteroitDogmaSayaci = Math.max(0.65, 2.25 - this.bolum * 0.08)
                    * (0.72 + Math.random());
            }
        }
    }

    /** Temaya özgü düşman türünü seçer; null dönerse Dusman kendi rastgele türünü seçer. */
    dusmanTuruSec() {
        const r = rastgeleInt(100);
        switch (this.temaGetir()) {
            case 'ASTEROIT_ALANI': return r < 26 ? 'HIZLI' : r < 48 ? 'ZIKZAK' : null;
            case 'DUSMAN_USSU': return r < 28 ? 'ATESCI' : r < 57 ? 'ZIRHLI' : null;
            case 'KARA_DELIK': return r < 34 ? 'ZIKZAK' : r < 58 ? 'HIZLI' : null;
            case 'FINAL': return r < 25 ? 'ATESCI' : r < 48 ? 'ZIRHLI' : r < 70 ? 'ZIKZAK' : null;
            default: return null;
        }
    }

    asteroitOlusturulmali() {
        const tema = this.temaGetir();
        return tema === 'ASTEROIT_ALANI' || tema === 'KARA_DELIK' || tema === 'FINAL'
            || (this.oyunModu === OYUN_MODU.ENDLESS && this.bolum >= 2);
    }

    bolumHedefiTamamMi() {
        if (this.oyunModu === OYUN_MODU.HIKAYE) {
            return hedefTamamMi(this.hikayeBolumu, this.oldurulenDusman,
                this.yokEdilenAsteroit, this.bolumSuresi);
        }
        return this.oldurulenDusman >= this.hedefDusman;
    }

    finalMi() {
        return this.oyunModu === OYUN_MODU.HIKAYE && this.bolum === 5;
    }

    bossOlustur() {
        this.bossBekleniyor = true;
        this.dusmanlar.length = 0;
        this.asteroitler.length = 0;
        this.dusmanMermileri.length = 0;
        this.boss = new Boss(this.finalMi() ? 8 : this.bolum);
        this.ses.boss();
        this.dinleyici.bildirim(this.finalMi() ? 'FİNAL BOSS YAKLAŞIYOR!' : 'BOSS YAKLAŞIYOR!');
    }

    karaDelikKuvvetiUygula(dt) {
        if (this.temaGetir() !== 'KARA_DELIK') return;
        const dx = GENISLIK / 2 - this.oyuncu.merkezX();
        const dy = 340 - this.oyuncu.merkezY();
        const mesafe = Math.max(90, Math.hypot(dx, dy));
        const kuvvet = 5400 / mesafe;
        this.oyuncu.disKuvvetUygula(dx / mesafe * kuvvet, dy / mesafe * kuvvet, dt);
    }

    dusmanlariGuncelle(dt) {
        for (const d of this.dusmanlar) {
            d.guncelle(dt, this.oyuncu.merkezX());
            if (d.atesEtmeliMi()) this.dusmanAtesEt(d.merkezX(), d.merkezY(), 300, false);
            // Ekranın altından çıkan düşman oyuncuya hasar vermeden kaybolur.
            if (d.ekranDisindaMi()) d.yokEt();
        }
        this.dusmanlar = this.dusmanlar.filter(d => d.aktif);
    }

    asteroitleriGuncelle(dt) {
        this.asteroitler.forEach(a => a.guncelle(dt));
        this.asteroitler = this.asteroitler.filter(a => a.aktif && !a.ekranDisindaMi());
    }

    mermileriGuncelle(dt) {
        for (const m of this.oyuncuMermileri) {
            const [hx, hy] = this.enYakinHedef(m);
            m.guncelle(dt, hx, hy);
        }
        this.dusmanMermileri.forEach(m => m.guncelle(dt, this.oyuncu.merkezX(), this.oyuncu.merkezY()));
        this.oyuncuMermileri = this.oyuncuMermileri.filter(m => m.aktif && !m.ekranDisindaMi());
        this.dusmanMermileri = this.dusmanMermileri.filter(m => m.aktif && !m.ekranDisindaMi());
    }

    enYakinHedef(mermi) {
        let enIyi = Infinity;
        let hx = null;
        let hy = null;
        const mb = mermi.sinirlar();
        const mx = mb.x + mb.w / 2;
        const my = mb.y + mb.h / 2;
        for (const d of this.dusmanlar) {
            const mesafe = Math.hypot(d.merkezX() - mx, d.merkezY() - my);
            if (mesafe < enIyi) {
                enIyi = mesafe;
                hx = d.merkezX();
                hy = d.merkezY();
            }
        }
        if (this.boss !== null && this.boss.aktif
            && Math.hypot(this.boss.merkezX() - mx, this.boss.merkezY() - my) < enIyi) {
            hx = this.boss.merkezX();
            hy = this.boss.merkezY();
        }
        return [hx, hy];
    }

    guclendirmeleriGuncelle(dt) {
        this.guclendirmeler.forEach(g => g.guncelle(dt));
        this.guclendirmeler = this.guclendirmeler.filter(g => g.aktif && !g.ekranDisindaMi());
    }

    bossGuncelle(dt) {
        const b = this.boss;
        b.guncelle(dt);
        if (!b.saldiriHazirMi()) return;
        if (b.saldiriSirasi === 0) {
            for (let i = -3; i <= 3; i++) {
                const aci = Math.PI / 2 + i * 0.16;
                this.dusmanMermileri.push(new Mermi(b.merkezX(), b.merkezY(),
                    Math.cos(aci) * 270, Math.sin(aci) * 270, 1, false, false, false, [255, 80, 145]));
            }
        } else if (b.saldiriSirasi === 1) {
            this.dusmanAtesEt(b.merkezX() - 65, b.merkezY(), 390, true);
            this.dusmanAtesEt(b.merkezX() + 65, b.merkezY(), 390, true);
        } else {
            for (let i = 0; i < 12; i++) {
                const aci = Math.PI * 2 * i / 12;
                this.dusmanMermileri.push(new Mermi(b.merkezX(), b.merkezY(),
                    Math.cos(aci) * 220, Math.sin(aci) * 220, 1, false, false, false, [255, 140, 30]));
            }
        }
    }

    oyuncuAtesEt() {
        const o = this.oyuncu;
        const x = o.merkezX();
        const y = o.y - 10;
        const hasar = Math.max(1, Math.round(o.hasarCarpani()));
        const ekle = (mx, my, vx, vy, h, delici, takip, renk) =>
            this.oyuncuMermileri.push(new Mermi(mx, my, vx, vy, h, true, delici, takip, renk));
        switch (o.silah) {
            case 'CIFTLI':
                ekle(x - 17, y, 0, -610, hasar, false, false, RENK.CYAN);
                ekle(x + 10, y, 0, -610, hasar, false, false, RENK.CYAN);
                break;
            case 'UCLU':
                ekle(x - 3, y, 0, -620, hasar, false, false, RENK.YELLOW);
                ekle(x - 3, y, -150, -590, hasar, false, false, RENK.YELLOW);
                ekle(x - 3, y, 150, -590, hasar, false, false, RENK.YELLOW);
                break;
            case 'LAZER':
                ekle(x - 6, y - 65, 0, -720, hasar * 2, true, false, [210, 80, 255]);
                break;
            case 'TAKIP_FUZESI':
                ekle(x - 6, y, 0, -430, hasar * 3, false, true, [255, 150, 35]);
                break;
            default:
                ekle(x - 3, y, 0, -640, hasar, false, false, [60, 230, 255]);
        }
        const temel = o.silah === 'LAZER' ? 0.34 : o.silah === 'TAKIP_FUZESI' ? 0.48 : 0.19;
        const atisSeviyesi = this.kayit.getGelistirme('atis');
        this.atisBekleme = temel * o.atisCarpani() * o.atisSuresiCarpani()
            * Math.max(0.55, 1 - atisSeviyesi * 0.075);
        this.ses.atis();
    }

    dusmanAtesEt(x, y, hiz, takip) {
        const dx = this.oyuncu.merkezX() - x;
        const dy = this.oyuncu.merkezY() - y;
        const uzunluk = Math.max(1, Math.hypot(dx, dy));
        this.dusmanMermileri.push(new Mermi(x, y, dx / uzunluk * hiz, dy / uzunluk * hiz,
            1, false, false, takip, [255, 65, 100]));
    }

    carpismalariKontrolEt() {
        for (const m of this.oyuncuMermileri) {
            if (!m.aktif) continue;
            for (const d of this.dusmanlar) {
                if (d.aktif && kesisir(m.sinirlar(), d.sinirlar())) {
                    const oldu = d.hasarAl(m.hasar);
                    const mb = m.sinirlar();
                    this.carpmaParcacigi(mb.x + mb.w / 2, mb.y + mb.h / 2, RENK.CYAN, 7);
                    if (!m.delici) m.yokEt();
                    if (oldu) this.dusmanOlduruldu(d);
                    break;
                }
            }
            if (m.aktif && this.boss !== null && this.boss.aktif
                && kesisir(m.sinirlar(), this.boss.sinirlar())) {
                const oldu = this.boss.hasarAl(m.hasar);
                const mb = m.sinirlar();
                this.carpmaParcacigi(mb.x + mb.w / 2, mb.y + mb.h / 2, RENK.MAGENTA, 8);
                if (!m.delici) m.yokEt();
                if (oldu) this.bossOlduruldu();
            }
            for (const a of this.asteroitler) {
                if (m.aktif && a.aktif && kesisir(m.sinirlar(), a.sinirlar())) {
                    const oldu = a.hasarAl(m.hasar);
                    this.carpmaParcacigi(a.merkezX(), a.merkezY(), [170, 130, 90], 8);
                    if (!m.delici) m.yokEt();
                    if (oldu) this.asteroitYokOldu(a);
                    break;
                }
            }
        }

        const oyuncuSiniri = this.oyuncu.sinirlar();
        for (const m of this.dusmanMermileri) {
            if (m.aktif && kesisir(m.sinirlar(), oyuncuSiniri)) {
                m.yokEt();
                this.oyuncuHasarAl(1, 'DÜŞMAN MERMİSİ!');
            }
        }
        for (const d of this.dusmanlar) {
            if (d.aktif && kesisir(d.sinirlar(), oyuncuSiniri)) {
                d.yokEt();
                this.oyuncuHasarAl(d.tur === 'ZIRHLI' ? 2 : 1, 'DÜŞMAN GEMİSİNE ÇARPTIN!');
                this.patlama(d.merkezX(), d.merkezY(), [255, 90, 80], 24);
            }
        }
        for (const a of this.asteroitler) {
            if (a.aktif && kesisir(a.sinirlar(), oyuncuSiniri)) {
                a.yokEt();
                this.oyuncuHasarAl(a.buyuk ? 2 : 1,
                    a.buyuk ? 'BÜYÜK ASTEROİDE ÇARPTIN!' : 'ASTEROİDE ÇARPTIN!');
                this.patlama(a.merkezX(), a.merkezY(), [160, 120, 90], 24);
            }
        }
        if (this.boss !== null && this.boss.aktif && kesisir(this.boss.sinirlar(), oyuncuSiniri)) {
            this.oyuncuHasarAl(2, "BOSS'A ÇARPTIN!");
        }
        for (const g of this.guclendirmeler) {
            if (g.aktif && kesisir(g.sinirlar(), oyuncuSiniri)) {
                g.yokEt();
                this.guclendirmeUygula(g.tur);
            }
        }
    }

    dusmanOlduruldu(d) {
        this.skor += Math.round(d.puan * this.zorluk.skor);
        this.coin += 1 + Math.trunc(d.puan / 180);
        this.oldurulenDusman++;
        this.xpEkle(d.xpDegeri());
        this.patlama(d.merkezX(), d.merkezY(), RENK.ORANGE, 22);
        this.ses.patlama();
        if (Math.random() < 0.14) this.guclendirmeBirak(d.merkezX(), d.merkezY());
    }

    asteroitYokOldu(a) {
        this.skor += a.buyuk ? 160 : 55;
        this.coin += a.buyuk ? 2 : 0;
        this.yokEdilenAsteroit++;
        this.xpEkle(a.xpDegeri());
        this.patlama(a.merkezX(), a.merkezY(), [175, 130, 90], 20);
        this.ses.patlama();
        if (a.buyuk) {
            for (let i = 0; i < 2; i++) {
                this.asteroitler.push(Asteroit.parca(a.merkezX() - 18, a.merkezY() - 18,
                    i === 0 ? -130 : 130, 190 + Math.random() * 70,
                    Math.max(24, Math.trunc(a.boyut / 2))));
            }
        }
    }

    bossOlduruldu() {
        this.skor += Math.round((2500 + this.bolum * 500) * this.zorluk.skor);
        this.coin += 35 + this.bolum * 5;
        this.xpEkle(250 + this.bolum * 55);
        this.patlama(this.boss.merkezX(), this.boss.merkezY(), [255, 70, 220], 85);
        this.ses.patlama();
        this.boss = null;
        this.bossBekleniyor = false;

        if (this.oyunModu === OYUN_MODU.HIKAYE) {
            if (this.bolum >= 5) {
                this.kayit.setHikayeTamamlandi(true);
                this.kayit.setAcikHikayeBolumu(5);
                this.kazanciKaydet();
                this.ses.zafer();
                this.dinleyici.zafer();
            } else {
                this.sonrakiBolum = this.bolum + 1;
                this.kayit.setAcikHikayeBolumu(this.sonrakiBolum);
                this.ses.bolum();
                this.dinleyici.bolumTamamlandi();
            }
        } else {
            this.bolum++;
            this.bolumuHazirla(true);
            this.ses.bolum();
            this.dinleyici.bildirim(this.aktifBolumBasligi());
        }
    }

    xpEkle(miktar) {
        const eskiSeviye = this.seviyeSistemi.seviye;
        if (this.seviyeSistemi.xpEkle(miktar) <= 0) return;
        for (let yeni = eskiSeviye + 1; yeni <= this.seviyeSistemi.seviye; yeni++) {
            const odul = this.oyuncu.seviyeOduluUygula(yeni);
            const silahMesaji = this.seviyeyeGoreSilahAc(yeni);
            this.ses.seviyeAtlama();
            this.dinleyici.bildirim(`SEVİYE ${yeni}! ${silahMesaji || odul}`);
        }
        this.kayit.setEnYuksekOyuncuSeviyesi(this.seviyeSistemi.seviye);
    }

    seviyeyeGoreSilahAc(seviye) {
        const acilan = {
            3: ['CIFTLI', 'Çift mermi kalıcı açıldı'],
            5: ['UCLU', 'Üçlü mermi kalıcı açıldı'],
            8: ['LAZER', 'Lazer kalıcı açıldı'],
            10: ['TAKIP_FUZESI', 'Takip füzesi kalıcı açıldı']
        }[seviye];
        if (!acilan) return '';
        this.oyuncu.kaliciSilahAc(acilan[0]);
        return acilan[1];
    }

    oyuncuHasarAl(hasar, kaynak) {
        const gercekHasar = Math.max(1, Math.round(hasar * this.zorluk.hasar));
        if (this.oyuncu.hasarAl(gercekHasar)) {
            this.ekranSarsintisi = 12;
            this.ses.hasar();
            this.patlama(this.oyuncu.merkezX(), this.oyuncu.merkezY(), RENK.RED, 15);
            this.dinleyici.bildirim(kaynak);
        }
    }

    guclendirmeBirak(x, y) {
        const tur = GUCLENDIRME_TURLERI[rastgeleInt(GUCLENDIRME_TURLERI.length)];
        this.guclendirmeler.push(new Guclendirme(x - 18, y - 18, tur));
    }

    guclendirmeUygula(tur) {
        const o = this.oyuncu;
        switch (tur) {
            case 'CIFT_MERMI': o.silahVer('CIFTLI', 13); break;
            case 'UCLU_MERMI': o.silahVer('UCLU', 11); break;
            case 'HIZLI_ATES': o.hizliAtesVer(10); break;
            case 'KALKAN': o.kalkanVer(6 + this.kayit.getGelistirme('kalkan') * 1.2); break;
            case 'CAN': o.canEkle(1); break;
            case 'LAZER': o.silahVer('LAZER', 9); break;
            case 'TAKIP_FUZESI': o.silahVer('TAKIP_FUZESI', 10); break;
            case 'BOMBA': o.bombaEkle(); break;
        }
        this.ses.guclendirme();
        this.dinleyici.bildirim(GUCLENDIRMELER[tur].ad);
    }

    bombaKullan() {
        if (!this.oyuncu.bombaKullan()) return;
        for (const d of this.dusmanlar) {
            if (d.aktif) {
                this.skor += Math.trunc(d.puan / 2);
                this.coin += 1;
                this.oldurulenDusman++;
                this.xpEkle(Math.max(3, Math.trunc(d.xpDegeri() / 2)));
                d.yokEt();
                this.patlama(d.merkezX(), d.merkezY(), RENK.ORANGE, 12);
            }
        }
        for (const a of this.asteroitler) {
            if (a.aktif) {
                this.yokEdilenAsteroit++;
                this.xpEkle(Math.max(2, Math.trunc(a.xpDegeri() / 2)));
                a.yokEt();
            }
        }
        this.dusmanMermileri.length = 0;
        if (this.boss !== null) this.boss.hasarAl(25);
        this.ekranSarsintisi = 22;
        this.ses.patlama();
        this.dinleyici.bildirim('MEGA BOMBA!');
    }

    patlama(x, y, renk, adet) {
        for (let i = 0; i < adet; i++) {
            const aci = Math.random() * Math.PI * 2;
            const hiz = 70 + Math.random() * 260;
            this.parcaciklar.push(new Parcacik(x, y, Math.cos(aci) * hiz, Math.sin(aci) * hiz,
                0.35 + Math.random() * 0.65, 5 + rastgeleInt(10), i % 3 === 0 ? RENK.YELLOW : renk));
        }
    }

    carpmaParcacigi(x, y, renk, adet) {
        for (let i = 0; i < adet; i++) {
            const aci = Math.random() * Math.PI * 2;
            const hiz = 60 + Math.random() * 120;
            this.parcaciklar.push(new Parcacik(x, y, Math.cos(aci) * hiz, Math.sin(aci) * hiz,
                0.18 + Math.random() * 0.25, 3 + rastgeleInt(5), renk));
        }
    }

    /** Skor, coin ve seviyeyi bir oturumda yalnızca bir kez kaydeder. */
    kazanciKaydet() {
        if (this.skorKaydedildi) return;
        this.kayit.skorEkle(this.skor);
        this.kayit.setCoin(this.kayit.getCoin() + this.coin);
        this.kayit.setEnYuksekOyuncuSeviyesi(this.seviyeSistemi.seviye);
        this.skorKaydedildi = true;
    }

    sonrakiHikayeBolumunuBaslat() {
        this.bolum = this.sonrakiBolum;
        this.bolumuHazirla(true);
        this.dinleyici.bildirim(this.aktifBolumBasligi());
    }

    temaGetir() {
        if (this.oyunModu === OYUN_MODU.HIKAYE) return hikayeBolumu(this.bolum).tema;
        return TEMALAR[(this.bolum - 1) % TEMALAR.length];
    }

    aktifBolumBasligi() {
        if (this.oyunModu === OYUN_MODU.HIKAYE) return `Bölüm ${this.bolum}: ${this.hikayeBolumu.ad}`;
        return `Endless Dalga ${this.bolum}: ${TEMA_ADI[this.temaGetir()]}`;
    }

    hedefMetni() {
        if (this.oyunModu === OYUN_MODU.HIKAYE) {
            return hedefMetni(this.hikayeBolumu, this.oldurulenDusman, this.yokEdilenAsteroit, this.bolumSuresi);
        }
        return `Düşman ${Math.min(this.oldurulenDusman, this.hedefDusman)}/${this.hedefDusman}`;
    }

    ciz(g) {
        this.asteroitler.forEach(a => a.ciz(g));
        this.guclendirmeler.forEach(p => p.ciz(g));
        this.dusmanlar.forEach(d => d.ciz(g));
        if (this.boss !== null) this.boss.ciz(g);
        this.oyuncuMermileri.forEach(m => m.ciz(g));
        this.dusmanMermileri.forEach(m => m.ciz(g));
        this.oyuncu.ciz(g);
        this.parcaciklar.forEach(p => p.ciz(g));
        this.hudCiz(g);
    }

    hudCiz(g) {
        const o = this.oyuncu;
        g.fillStyle = 'rgba(0,0,0,0.61)';
        Cizim.yuvarlakDolu(g, 12, 10, 270, 102, 18);
        Cizim.yuvarlakDolu(g, GENISLIK - 322, 10, 310, 102, 18);
        g.font = Cizim.font('bold', 17);
        g.fillStyle = rgb(RENK.WHITE);
        g.fillText(`Skor: ${this.skor}`, 26, 34);
        g.fillText(`Coin: ${this.coin}`, 26, 58);
        g.fillText(this.oyunModu === OYUN_MODU.HIKAYE ? `Bölüm: ${this.bolum}/5` : `Dalga: ${this.bolum}`, 26, 82);
        g.fillText(`Oyuncu seviyesi: ${this.seviyeSistemi.seviye}`, 26, 105);

        g.font = Cizim.font('bold', 15);
        g.fillText(this.hedefMetni(), GENISLIK - 306, 34);
        g.fillText(`Silah: ${SILAH_ADI[o.silah]}`, GENISLIK - 306, 58);
        g.fillText(`Kalıcı: ${SILAH_ADI[o.kaliciSilah]}`, GENISLIK - 306, 80);
        g.fillText(`Bomba: ${o.bombaSayisi}`, GENISLIK - 306, 102);

        this.xpCubuguCiz(g);
        for (let i = 0; i < o.maksimumCan; i++) {
            g.fillStyle = i < o.can ? rgb([255, 65, 90]) : rgb([80, 45, 55]);
            OyunDunyasi.kalpCiz(g, 20 + i * 26, 145, 19);
        }
    }

    xpCubuguCiz(g) {
        const x = 146;
        const y = 121;
        const w = 508;
        const h = 16;
        g.fillStyle = rgb([10, 15, 35], 210);
        Cizim.yuvarlakDolu(g, x, y, w, h, 9);
        g.fillStyle = rgb([65, 190, 255]);
        Cizim.yuvarlakDolu(g, x, y, Math.trunc(w * this.seviyeSistemi.ilerlemeOrani()), h, 9);
        g.strokeStyle = rgb(RENK.WHITE);
        Cizim.yuvarlakCizgi(g, x, y, w, h, 9);
        g.font = Cizim.font('bold', 12);
        g.fillStyle = rgb(RENK.WHITE);
        const metin = `XP ${this.seviyeSistemi.mevcutXp}/${this.seviyeSistemi.gerekenXp()}`;
        g.fillText(metin, GENISLIK / 2 - g.measureText(metin).width / 2, y + 13);
    }

    static kalpCiz(g, x, y, b) {
        const yarim = Math.trunc(b / 2);
        Cizim.ovalDolu(g, x, y, yarim + 2, yarim + 2);
        Cizim.ovalDolu(g, x + yarim - 2, y, yarim + 2, yarim + 2);
        Cizim.cokgen(g, [[x, y + Math.trunc(b / 3)], [x + b, y + Math.trunc(b / 3)], [x + yarim, y + b]], true);
    }
}
