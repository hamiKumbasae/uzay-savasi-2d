'use strict';

/** Efektleri ilk dokunuşta bir kez üretir; aynı efekt yeniden çalınınca öncekini keser. */
class SesYoneticisi {
    static EFEKTLER = {
        atis: [45, 0.18, 720],
        patlama: [120, 0.36, 105],
        guclendirme: [55, 0.18, 440, 660, 880],
        hasar: [100, 0.28, 190, 120],
        menu: [45, 0.14, 520],
        boss: [90, 0.35, 180, 150, 120, 90],
        bolum: [75, 0.22, 330, 440, 660, 880],
        seviyeAtlama: [65, 0.24, 440, 554, 659, 880],
        zafer: [95, 0.28, 392, 523, 659, 784, 1046]
    };

    constructor() {
        this.etkin = true;
        this.baglam = null;
        this.tamponlar = {};
        this.calanlar = {};
    }

    /** Tarayıcılar sesi ancak bir kullanıcı hareketinden sonra açar. */
    hazirla() {
        if (this.baglam) {
            if (this.baglam.state === 'suspended') this.baglam.resume();
            return;
        }
        const Baglam = window.AudioContext || window.webkitAudioContext;
        if (!Baglam) return;
        try {
            this.baglam = new Baglam();
        } catch (e) {
            return;
        }
        for (const [ad, [sure, ses, ...frekanslar]] of Object.entries(SesYoneticisi.EFEKTLER)) {
            this.tamponlar[ad] = this.tamponOlustur(sure, ses, frekanslar);
        }
    }

    setEtkin(etkin) {
        this.etkin = etkin;
    }

    tamponOlustur(tonSuresiMs, ses, frekanslar) {
        const hiz = 22050;
        const n = Math.trunc(tonSuresiMs * hiz / 1000);
        const tampon = this.baglam.createBuffer(1, n * frekanslar.length, hiz);
        const veri = tampon.getChannelData(0);
        let k = 0;
        for (const f of frekanslar) {
            for (let i = 0; i < n; i++) {
                veri[k++] = Math.sin(2 * Math.PI * f * i / hiz) * ses * (1 - i / n);
            }
        }
        return tampon;
    }

    cal(ad) {
        if (!this.etkin || !this.baglam) return;
        const onceki = this.calanlar[ad];
        if (onceki) {
            try { onceki.stop(); } catch (e) { /* zaten bitmiş */ }
        }
        const kaynak = this.baglam.createBufferSource();
        kaynak.buffer = this.tamponlar[ad];
        kaynak.connect(this.baglam.destination);
        kaynak.start();
        this.calanlar[ad] = kaynak;
    }

    atis() { this.cal('atis'); }
    patlama() { this.cal('patlama'); }
    guclendirme() { this.cal('guclendirme'); }
    hasar() { this.cal('hasar'); }
    menu() { this.cal('menu'); }
    boss() { this.cal('boss'); }
    bolum() { this.cal('bolum'); }
    seviyeAtlama() { this.cal('seviyeAtlama'); }
    zafer() { this.cal('zafer'); }
}

/** Coin, geliştirme, ilerleme ve skorları tarayıcıda saklar. */
class KayitYoneticisi {
    static AYAR_ANAHTARI = 'uzaySavasi2d.kayit';
    static SKOR_ANAHTARI = 'uzaySavasi2d.skorlar';

    constructor() {
        this.ozellikler = KayitYoneticisi.oku(KayitYoneticisi.AYAR_ANAHTARI) || {};
        const skorlar = KayitYoneticisi.oku(KayitYoneticisi.SKOR_ANAHTARI);
        this.skorlar = Array.isArray(skorlar) ? skorlar.filter(Number.isFinite) : [];
    }

    static oku(anahtar) {
        try {
            const metin = localStorage.getItem(anahtar);
            return metin ? JSON.parse(metin) : null;
        } catch (e) {
            return null;
        }
    }

    static yaz(anahtar, deger) {
        try {
            localStorage.setItem(anahtar, JSON.stringify(deger));
        } catch (e) {
            // Gizli sekme veya kapalı depolama: oyun kayıtsız devam eder.
        }
    }

    intGetir(anahtar, varsayilan) {
        const deger = Number.parseInt(this.ozellikler[anahtar], 10);
        return Number.isFinite(deger) ? deger : varsayilan;
    }

    ayarla(anahtar, deger) {
        this.ozellikler[anahtar] = deger;
        KayitYoneticisi.yaz(KayitYoneticisi.AYAR_ANAHTARI, this.ozellikler);
    }

    getCoin() { return this.intGetir('coin', 0); }
    setCoin(coin) { this.ayarla('coin', Math.max(0, coin)); }
    sesAcikMi() { return this.ozellikler.ses !== false; }
    setSesAcik(acik) { this.ayarla('ses', acik); }
    getGelistirme(ad) { return this.intGetir('gelistirme.' + ad, 0); }
    setGelistirme(ad, seviye) { this.ayarla('gelistirme.' + ad, seviye); }
    getSeciliGemi() { return this.intGetir('seciliGemi', 0); }
    setSeciliGemi(index) { this.ayarla('seciliGemi', index); }
    getAcikHikayeBolumu() { return Math.max(1, Math.min(5, this.intGetir('hikaye.acikBolum', 1))); }
    setAcikHikayeBolumu(bolum) {
        this.ayarla('hikaye.acikBolum',
            Math.max(this.getAcikHikayeBolumu(), Math.max(1, Math.min(5, bolum))));
    }
    hikayeTamamlandiMi() { return this.ozellikler['hikaye.tamamlandi'] === true; }
    setHikayeTamamlandi(tamamlandi) { this.ayarla('hikaye.tamamlandi', tamamlandi); }
    getEnYuksekOyuncuSeviyesi() { return Math.max(1, this.intGetir('istatistik.enYuksekSeviye', 1)); }
    setEnYuksekOyuncuSeviyesi(seviye) {
        this.ayarla('istatistik.enYuksekSeviye', Math.max(this.getEnYuksekOyuncuSeviyesi(), seviye));
    }

    yuksekSkorlar() {
        return [...this.skorlar].sort((a, b) => b - a).slice(0, 5);
    }

    skorEkle(skor) {
        this.skorlar = [...this.yuksekSkorlar(), Math.max(0, skor)].sort((a, b) => b - a).slice(0, 5);
        KayitYoneticisi.yaz(KayitYoneticisi.SKOR_ANAHTARI, this.skorlar);
    }
}

class SeviyeSistemi {
    constructor() {
        this.seviye = 1;
        this.mevcutXp = 0;
    }

    xpEkle(miktar) {
        if (miktar <= 0) return 0;
        const onceki = this.seviye;
        this.mevcutXp += miktar;
        while (this.mevcutXp >= this.gerekenXp()) {
            this.mevcutXp -= this.gerekenXp();
            this.seviye++;
        }
        return this.seviye - onceki;
    }

    gerekenXp() { return 100 + (this.seviye - 1) * 55; }
    ilerlemeOrani() { return Math.min(1, this.mevcutXp / this.gerekenXp()); }
}
