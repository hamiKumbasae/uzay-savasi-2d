'use strict';

class Yildiz {
    constructor() {
        this.x = Math.random() * GENISLIK;
        this.y = Math.random() * YUKSEKLIK;
        this.hiz = 45 + Math.random() * 210;
        this.boyut = 1 + rastgeleInt(4);
        const p = 150 + rastgeleInt(106);
        this.renk = rgb([p, p, 255]);
    }

    guncelle(dt) {
        this.y += this.hiz * dt;
        if (this.y > YUKSEKLIK + 5) {
            this.y = -5;
            this.x = Math.random() * GENISLIK;
        }
    }

    ciz(g) {
        g.fillStyle = this.renk;
        Cizim.ovalDolu(g, Math.trunc(this.x), Math.trunc(this.y), this.boyut, this.boyut);
    }
}

class Parcacik {
    constructor(x, y, vx, vy, omur, boyut, renk) {
        Object.assign(this, { x, y, vx, vy, omur, boyut, renk });
        this.baslangicOmur = omur;
    }

    guncelle(dt) {
        this.x += this.vx * dt;
        this.y += this.vy * dt;
        this.omur -= dt;
    }

    ciz(g) {
        const oran = Math.max(0, this.omur / this.baslangicOmur);
        g.fillStyle = rgb(this.renk, Math.max(0, Math.min(255, Math.trunc(255 * oran))));
        const b = Math.max(1, Math.trunc(this.boyut * oran));
        Cizim.ovalDolu(g, Math.trunc(this.x) - Math.trunc(b / 2), Math.trunc(this.y) - Math.trunc(b / 2), b, b);
    }

    aktifMi() { return this.omur > 0; }
}

class Mermi {
    constructor(x, y, vx, vy, hasar, oyuncuMermisi, delici, takipEden, renk) {
        Object.assign(this, { x, y, vx, vy, hasar, oyuncuMermisi, delici, takipEden, renk });
        this.genislik = delici ? 12 : (takipEden ? 12 : 7);
        this.yukseklik = delici ? 80 : (takipEden ? 22 : 17);
        this.aktif = true;
    }

    guncelle(dt, hedefX, hedefY) {
        if (this.takipEden && hedefX != null && hedefY != null) {
            const dx = hedefX - this.x;
            const dy = hedefY - this.y;
            const uzunluk = Math.hypot(dx, dy);
            if (uzunluk > 1) {
                const k = Math.min(1, dt * 5.5);
                this.vx += (dx / uzunluk * 480 - this.vx) * k;
                this.vy += (dy / uzunluk * 480 - this.vy) * k;
            }
        }
        this.x += this.vx * dt;
        this.y += this.vy * dt;
    }

    ciz(g) {
        const ix = Math.trunc(this.x);
        const iy = Math.trunc(this.y);
        const w = this.genislik;
        const h = this.yukseklik;
        if (this.delici) {
            g.fillStyle = rgb(this.renk, 65);
            Cizim.yuvarlakDolu(g, ix - 5, iy, w + 10, h, 12);
            g.fillStyle = rgb(this.renk);
            Cizim.yuvarlakDolu(g, ix, iy, w, h, 8);
        } else if (this.takipEden) {
            g.fillStyle = rgb(RENK.ORANGE);
            Cizim.ovalDolu(g, ix - 5, iy + 14, 10, 16);
            g.fillStyle = rgb(this.renk);
            Cizim.yuvarlakDolu(g, ix, iy, w, h, 7);
            g.strokeStyle = rgb(RENK.WHITE);
            g.lineWidth = 2;
            Cizim.cizgi(g, ix + 2, iy + 4, ix + w - 2, iy + 4);
        } else if (!this.oyuncuMermisi) {
            // Düşman mermileri koyu arka planlarda kaybolmasın diye parlak çizilir.
            g.fillStyle = rgb([255, 35, 80], 75);
            Cizim.ovalDolu(g, ix - 7, iy - 5, w + 14, h + 10);
            g.fillStyle = rgb([255, 215, 80]);
            Cizim.yuvarlakDolu(g, ix - 1, iy, w + 2, h, 7);
            g.fillStyle = rgb(RENK.WHITE);
            Cizim.ovalDolu(g, ix + Math.trunc(w / 2) - 2, iy + 3, 4, 6);
        } else {
            g.fillStyle = rgb(this.renk);
            Cizim.yuvarlakDolu(g, ix, iy, w, h, 5);
            g.strokeStyle = rgb(RENK.WHITE);
            Cizim.cizgi(g, ix + Math.trunc(w / 2), iy + 2, ix + Math.trunc(w / 2), iy + h - 2);
        }
    }

    sinirlar() {
        if (!this.oyuncuMermisi && !this.delici && !this.takipEden) {
            return dikdortgen(Math.trunc(this.x) + 1, Math.trunc(this.y) + 2,
                Math.max(3, this.genislik - 2), Math.max(7, this.yukseklik - 4));
        }
        return dikdortgen(this.x, this.y, this.genislik, this.yukseklik);
    }

    ekranDisindaMi() {
        return this.x < -120 || this.x > GENISLIK + 120 || this.y < -140 || this.y > YUKSEKLIK + 140;
    }

    yokEt() { this.aktif = false; }
}

class Dusman {
    constructor(bolum, hizCarpani, zorunluTur) {
        this.tur = zorunluTur || Dusman.rastgeleTur(bolum);
        const ozellik = {
            HIZLI: [38, 52, 1, (235 + bolum * 7), 140, [255, 90, 100]],
            ZIRHLI: [64, 72, 5 + Math.trunc(bolum / 3), (95 + bolum * 4), 360, [170, 125, 255]],
            ZIKZAK: [48, 58, 2, (145 + bolum * 5), 220, [255, 205, 55]],
            ATESCI: [54, 64, 3 + Math.trunc(bolum / 5), (110 + bolum * 4), 300, [255, 95, 205]],
            NORMAL: [48, 58, 2 + Math.trunc(bolum / 6), (125 + bolum * 5), 100, [85, 240, 130]]
        }[this.tur];
        [this.genislik, this.yukseklik, this.maksimumCan, this.hiz, this.puan, this.renk] = ozellik;
        this.hiz *= hizCarpani;
        this.can = this.maksimumCan;
        this.x = 20 + Math.random() * Math.max(1, GENISLIK - this.genislik - 40);
        this.baslangicX = this.x;
        this.y = -this.yukseklik - rastgeleInt(140);
        this.atisAraligi = 1.4 + Math.random() * 1.7;
        this.atisSayaci = Math.random() * this.atisAraligi;
        this.zaman = 0;
        this.aktif = true;
    }

    static rastgeleTur(bolum) {
        const r = rastgeleInt(100);
        if (bolum >= 3 && r < 13) return 'ATESCI';
        if (bolum >= 2 && r < 28) return 'ZIKZAK';
        if (bolum >= 2 && r < 42) return 'ZIRHLI';
        if (r < 62) return 'HIZLI';
        return 'NORMAL';
    }

    guncelle(dt, oyuncuX) {
        this.zaman += dt;
        this.y += this.hiz * dt;
        if (this.tur === 'ZIKZAK') {
            this.x = this.baslangicX + Math.sin(this.zaman * 3.8) * 95;
        } else if (this.tur === 'ATESCI') {
            this.x += Math.sign(oyuncuX - this.merkezX()) * 42 * dt;
        }
        this.atisSayaci -= dt;
    }

    atesEtmeliMi() {
        if (this.tur !== 'ATESCI' || this.atisSayaci > 0) return false;
        this.atisSayaci = this.atisAraligi;
        return true;
    }

    ciz(g) {
        const ix = Math.trunc(this.x);
        const iy = Math.trunc(this.y);
        const w = this.genislik;
        const h = this.yukseklik;
        const mx = ix + Math.trunc(w / 2);
        const govde = [[ix, iy], [mx, iy + 15], [ix + w, iy], [ix + w - 8, iy + h],
            [mx, iy + h - 12], [ix + 8, iy + h]];
        g.fillStyle = rgb(this.renk);
        Cizim.cokgen(g, govde, true);
        g.strokeStyle = rgb(parlak(this.renk));
        Cizim.cokgen(g, govde, false);
        g.fillStyle = rgb([40, 30, 90]);
        Cizim.ovalDolu(g, mx - 9, iy + 20, 18, 22);

        if (this.maksimumCan > 2) {
            g.fillStyle = rgb([30, 30, 45]);
            g.fillRect(ix, iy - 9, w, 5);
            g.fillStyle = rgb(RENK.RED);
            g.fillRect(ix, iy - 9, Math.trunc(w * (this.can / this.maksimumCan)), 5);
        }
    }

    hasarAl(miktar) {
        this.can -= miktar;
        if (this.can <= 0) this.aktif = false;
        return !this.aktif;
    }

    sinirlar() {
        return dikdortgen(Math.trunc(this.x) + 4, Math.trunc(this.y) + 4, this.genislik - 8, this.yukseklik - 8);
    }

    ekranDisindaMi() { return this.y > YUKSEKLIK + 40; }
    merkezX() { return this.x + this.genislik / 2; }
    merkezY() { return this.y + this.yukseklik / 2; }
    xpDegeri() { return { NORMAL: 10, HIZLI: 15, ZIRHLI: 25, ZIKZAK: 20, ATESCI: 30 }[this.tur]; }
    yokEt() { this.aktif = false; }
}

class Asteroit {
    /** Ekranın üstünden gelen yeni asteroit. */
    static yeni(bolum, buyuk) {
        const boyut = buyuk ? 62 + rastgeleInt(28) : 28 + rastgeleInt(22);
        const a = new Asteroit(
            Math.random() * Math.max(1, GENISLIK - boyut), -boyut - rastgeleInt(100),
            -45 + Math.random() * 90, 105 + bolum * 6 + Math.random() * 80,
            boyut, buyuk, i => 0.38 + Math.random() * 0.12);
        a.can = buyuk ? 4 : 1;
        return a;
    }

    /** Büyük asteroit parçalanınca çıkan küçük parça. */
    static parca(x, y, vx, vy, boyut) {
        return new Asteroit(x, y, vx, vy, boyut, false, i => 0.40 + ((i * 37) % 11) / 100);
    }

    constructor(x, y, vx, vy, boyut, buyuk, yaricapOrani) {
        Object.assign(this, { x, y, vx, vy, boyut, buyuk });
        this.can = 1;
        this.donus = 0;
        this.aktif = true;
        this.sekil = [];
        for (let i = 0; i < 10; i++) {
            const aci = Math.PI * 2 * i / 10;
            const r = boyut * yaricapOrani(i);
            this.sekil.push([Math.trunc(Math.cos(aci) * r), Math.trunc(Math.sin(aci) * r)]);
        }
    }

    guncelle(dt) {
        this.x += this.vx * dt;
        this.y += this.vy * dt;
        this.donus += dt * 1.4;
    }

    ciz(g) {
        const b = this.boyut;
        const cx = Math.trunc(this.x) + Math.trunc(b / 2);
        const cy = Math.trunc(this.y) + Math.trunc(b / 2);
        const cos = Math.cos(this.donus);
        const sin = Math.sin(this.donus);
        const noktalar = this.sekil.map(([sx, sy]) =>
            [cx + Math.trunc(sx * cos - sy * sin), cy + Math.trunc(sx * sin + sy * cos)]);
        g.fillStyle = rgb([100, 86, 78]);
        Cizim.cokgen(g, noktalar, true);
        g.strokeStyle = rgb([155, 135, 115]);
        Cizim.cokgen(g, noktalar, false);
        g.fillStyle = rgb([65, 55, 50]);
        Cizim.ovalDolu(g, cx - Math.trunc(b / 7), cy - Math.trunc(b / 6), Math.trunc(b / 4), Math.trunc(b / 5));
        Cizim.ovalDolu(g, cx + Math.trunc(b / 8), cy + Math.trunc(b / 10), Math.trunc(b / 5), Math.trunc(b / 6));
    }

    hasarAl(miktar) {
        this.can -= miktar;
        if (this.can <= 0) this.aktif = false;
        return !this.aktif;
    }

    sinirlar() {
        return dikdortgen(Math.trunc(this.x) + 3, Math.trunc(this.y) + 3, this.boyut - 6, this.boyut - 6);
    }

    ekranDisindaMi() {
        return this.y > YUKSEKLIK + 80 || this.x < -100 || this.x > GENISLIK + 100;
    }

    merkezX() { return this.x + this.boyut / 2; }
    merkezY() { return this.y + this.boyut / 2; }
    xpDegeri() { return this.buyuk ? 20 : 8; }
    yokEt() { this.aktif = false; }
}

class Boss {
    constructor(bolum) {
        this.genislik = 220;
        this.yukseklik = 150;
        this.x = GENISLIK / 2 - this.genislik / 2;
        this.y = -190;
        this.maksimumCan = 95 + bolum * 32;
        this.can = this.maksimumCan;
        this.zaman = 0;
        this.atisSayaci = 1.2;
        this.saldiriSirasi = 0;
        this.aktif = true;
    }

    guncelle(dt) {
        this.zaman += dt;
        if (this.y < 70) {
            this.y += 75 * dt;
        } else {
            // Dar ekranda boss kenardan taşmasın diye salınım daralır.
            const salinim = Math.min(210, GENISLIK / 2 - this.genislik / 2 - 10);
            this.x = GENISLIK / 2 - this.genislik / 2 + Math.sin(this.zaman * 0.9) * salinim;
        }
        this.atisSayaci -= dt;
    }

    saldiriHazirMi() {
        if (this.atisSayaci > 0 || this.y < 40) return false;
        this.atisSayaci = Math.max(0.55, 1.45 - this.zaman * 0.004);
        this.saldiriSirasi = (this.saldiriSirasi + 1) % 3;
        return true;
    }

    ciz(g) {
        const ix = Math.trunc(this.x);
        const iy = Math.trunc(this.y);
        const w = this.genislik;
        const h = this.yukseklik;
        const govde = [[ix + 15, iy + 25], [ix + w / 2, iy], [ix + w - 15, iy + 25],
            [ix + w, iy + h - 25], [ix + w / 2, iy + h], [ix, iy + h - 25]];
        g.fillStyle = rgb([145, 35, 210]);
        Cizim.cokgen(g, govde, true);
        g.strokeStyle = rgb([255, 80, 230]);
        g.lineWidth = 4;
        Cizim.cokgen(g, govde, false);
        g.fillStyle = rgb([30, 15, 65]);
        Cizim.ovalDolu(g, ix + 70, iy + 32, 80, 65);
        g.fillStyle = rgb(RENK.RED);
        Cizim.ovalDolu(g, ix + 98, iy + 53, 24, 24);

        const cubukX = Math.round(GENISLIK * 0.1625);
        const cubukW = Math.round(GENISLIK * 0.675);
        // Can çubuğu HUD panellerinin üstüne binmesin diye kalplerin altında durur.
        g.fillStyle = rgb([20, 20, 35]);
        Cizim.yuvarlakDolu(g, cubukX, 176, cubukW, 24, 12);
        g.fillStyle = rgb([255, 65, 110]);
        Cizim.yuvarlakDolu(g, cubukX + 4, 180, Math.trunc((cubukW - 8) * (this.can / this.maksimumCan)), 16, 8);
        g.fillStyle = rgb(RENK.WHITE);
        g.font = Cizim.font('bold', 14);
        g.fillText('BOSS', cubukX - 58, 195);
    }

    hasarAl(miktar) {
        this.can -= miktar;
        if (this.can <= 0) this.aktif = false;
        return !this.aktif;
    }

    sinirlar() {
        return dikdortgen(Math.trunc(this.x) + 10, Math.trunc(this.y) + 8, this.genislik - 20, this.yukseklik - 16);
    }

    merkezX() { return this.x + this.genislik / 2; }
    merkezY() { return this.y + this.yukseklik / 2; }
}

class Guclendirme {
    constructor(x, y, tur) {
        Object.assign(this, { x, y, tur });
        this.zaman = 0;
        this.aktif = true;
    }

    guncelle(dt) {
        this.y += 125 * dt;
        this.zaman += dt;
    }

    ciz(g) {
        const ix = Math.trunc(this.x);
        const iy = Math.trunc(this.y);
        const nabiz = Math.trunc(Math.sin(this.zaman * 6) * 3);
        const { renk, simge } = GUCLENDIRMELER[this.tur];
        g.fillStyle = rgb(renk, 75);
        Cizim.ovalDolu(g, ix - 5 - nabiz, iy - 5 - nabiz, 46 + nabiz * 2, 46 + nabiz * 2);
        g.fillStyle = rgb(renk);
        Cizim.yuvarlakDolu(g, ix, iy, 36, 36, 12);
        g.fillStyle = rgb(RENK.WHITE);
        g.font = Cizim.font('bold', 14);
        const sw = g.measureText(simge).width;
        g.fillText(simge, ix + 18 - sw / 2, iy + 23);
    }

    sinirlar() { return dikdortgen(this.x, this.y, 36, 36); }
    ekranDisindaMi() { return this.y > YUKSEKLIK + 50; }
    yokEt() { this.aktif = false; }
}

class Oyuncu {
    constructor(x, y, gemi, hizG, canG, hasarG, bombaG) {
        this.x = x;
        this.y = y;
        this.genislik = 56;
        this.yukseklik = 72;
        this.gemi = gemi;
        this.temelHareketHizi = (330 + hizG * 24) * gemi.hiz;
        this.maksimumCan = Math.max(1, Math.round((3 + canG) * gemi.can));
        this.can = this.maksimumCan;
        this.temelHasarCarpani = (1 + hasarG * 0.14) * gemi.hasar;
        this.temelAtisCarpani = gemi.atis;
        this.bombaSayisi = 1 + bombaG;
        this.seviyeHizCarpani = 1;
        this.seviyeHasarCarpani = 1;
        this.seviyeAtisCarpani = 1;
        this.dokunulmazlikSuresi = 0;
        this.kalkanSuresi = 0;
        this.hizliAtesSuresi = 0;
        this.ozelSilahSuresi = 0;
        this.kaliciSilah = 'TEKLI';
        this.silah = 'TEKLI';
    }

    /**
     * Klavyede yön tuşlarıyla, dokunmatikte parmağın gösterdiği hedefe doğru ilerler.
     * Hedefe giderken hız sınırlı; parmak ne kadar hızlı kayarsa kaysın gemi ışınlanmaz.
     */
    guncelle(girdi, dt) {
        const hareketHizi = this.temelHareketHizi * this.seviyeHizCarpani;
        if (girdi.hedef) {
            const dx = girdi.hedef.x - this.merkezX();
            const dy = girdi.hedef.y - this.merkezY();
            const mesafe = Math.hypot(dx, dy);
            if (mesafe > 0) {
                const k = Math.min(1, hareketHizi * 1.8 * dt / mesafe);
                this.x += dx * k;
                this.y += dy * k;
            }
        } else {
            let dx = (girdi.sag ? 1 : 0) - (girdi.sol ? 1 : 0);
            let dy = (girdi.asagi ? 1 : 0) - (girdi.yukari ? 1 : 0);
            const uzunluk = Math.hypot(dx, dy);
            if (uzunluk > 0) {
                dx /= uzunluk;
                dy /= uzunluk;
            }
            this.x += dx * hareketHizi * dt;
            this.y += dy * hareketHizi * dt;
        }
        this.sinirla();

        this.dokunulmazlikSuresi = Math.max(0, this.dokunulmazlikSuresi - dt);
        this.kalkanSuresi = Math.max(0, this.kalkanSuresi - dt);
        this.hizliAtesSuresi = Math.max(0, this.hizliAtesSuresi - dt);
        if (this.ozelSilahSuresi > 0) {
            this.ozelSilahSuresi = Math.max(0, this.ozelSilahSuresi - dt);
            if (this.ozelSilahSuresi === 0) this.silah = this.kaliciSilah;
        }
    }

    disKuvvetUygula(kx, ky, dt) {
        this.x += kx * dt;
        this.y += ky * dt;
        this.sinirla();
    }

    sinirla() {
        this.x = Math.max(8, Math.min(GENISLIK - this.genislik - 8, this.x));
        this.y = Math.max(70, Math.min(YUKSEKLIK - this.yukseklik - 15, this.y));
    }

    ciz(g) {
        if (this.dokunulmazlikSuresi > 0 && Math.trunc(this.dokunulmazlikSuresi * 12) % 2 === 0) return;

        const ix = Math.trunc(this.x);
        const iy = Math.trunc(this.y);
        const w = this.genislik;
        const h = this.yukseklik;
        const mx = ix + w / 2;
        const govde = [[mx, iy], [ix + w, iy + h], [mx + 12, iy + h - 15], [mx, iy + h - 8],
            [mx - 12, iy + h - 15], [ix, iy + h]];

        g.fillStyle = rgb(this.gemi.renk);
        Cizim.cokgen(g, govde, true);
        g.strokeStyle = rgb(parlak(this.gemi.renk));
        g.lineWidth = 2;
        Cizim.cokgen(g, govde, false);

        g.fillStyle = rgb([40, 90, 210]);
        Cizim.ovalDolu(g, mx - 10, iy + 22, 20, 29);

        g.fillStyle = rgb(RENK.ORANGE);
        Cizim.ovalDolu(g, ix + 10, iy + h - 2, 12, 22);
        Cizim.ovalDolu(g, ix + w - 22, iy + h - 2, 12, 22);
        g.fillStyle = rgb(RENK.YELLOW);
        Cizim.ovalDolu(g, ix + 13, iy + h, 6, 14);
        Cizim.ovalDolu(g, ix + w - 19, iy + h, 6, 14);

        if (this.kalkanSuresi > 0) {
            g.strokeStyle = rgb([80, 220, 255], 100);
            g.lineWidth = 3;
            Cizim.ovalCizgi(g, ix - 8, iy - 8, w + 16, h + 16);
        }
    }

    hasarAl(miktar) {
        if (this.dokunulmazlikSuresi > 0 || this.kalkanSuresi > 0) return false;
        this.can = Math.max(0, this.can - miktar);
        this.dokunulmazlikSuresi = 1.7;
        return true;
    }

    seviyeOduluUygula(seviye) {
        this.canEkle(1);
        let odul = '+1 can';
        if (seviye % 2 === 0) {
            this.seviyeHasarCarpani += 0.12;
            odul = 'Mermi hasarı arttı';
        }
        if (seviye % 3 === 0) {
            this.seviyeAtisCarpani *= 0.93;
            odul = 'Atış hızı arttı';
        }
        if (seviye % 4 === 0) {
            this.seviyeHizCarpani += 0.06;
            odul = 'Hareket hızı arttı';
        }
        if (seviye % 5 === 0) {
            this.maksimumCan++;
            this.can = this.maksimumCan;
            this.bombaEkle();
            odul = 'Maksimum can ve bomba arttı';
        }
        return odul;
    }

    kaliciSilahAc(tur) {
        if (SILAHLAR.indexOf(tur) > SILAHLAR.indexOf(this.kaliciSilah)) {
            this.kaliciSilah = tur;
            if (this.ozelSilahSuresi <= 0) this.silah = tur;
        }
    }

    canEkle(miktar) { this.can = Math.min(this.maksimumCan, this.can + miktar); }
    kalkanVer(sure) { this.kalkanSuresi = Math.max(this.kalkanSuresi, sure); }
    hizliAtesVer(sure) { this.hizliAtesSuresi = Math.max(this.hizliAtesSuresi, sure); }
    silahVer(tur, sure) {
        this.silah = tur;
        this.ozelSilahSuresi = sure;
    }
    bombaEkle() { this.bombaSayisi = Math.min(9, this.bombaSayisi + 1); }
    bombaKullan() {
        if (this.bombaSayisi <= 0) return false;
        this.bombaSayisi--;
        return true;
    }

    sinirlar() {
        return dikdortgen(Math.trunc(this.x) + 9, Math.trunc(this.y) + 8, this.genislik - 18, this.yukseklik - 18);
    }

    merkezX() { return this.x + this.genislik / 2; }
    merkezY() { return this.y + this.yukseklik / 2; }
    hayattaMi() { return this.can > 0; }
    hasarCarpani() { return this.temelHasarCarpani * this.seviyeHasarCarpani; }
    atisCarpani() { return this.temelAtisCarpani * this.seviyeAtisCarpani; }
    atisSuresiCarpani() { return this.hizliAtesSuresi > 0 ? 0.48 : 1.0; }
}
