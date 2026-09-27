'use strict';

// Oyun alanının mantıksal boyutu; ekrana sığdırmak için ölçeklenir.
// Bilgisayarda 800×820. Dikey telefonda alan daraltılıp ekran boyunca uzatılır,
// böylece ekranın tamamı kullanılır ve yazılar okunacak kadar büyük kalır.
let GENISLIK = 800;
let YUKSEKLIK = 820;
const MENU_YUKSEKLIGI = 820;

/** Ekran boyutuna göre alanı ayarlar; boyut değiştiyse true döner. */
function alaniAyarla(ekranGenisligi, ekranYuksekligi) {
    const oran = ekranYuksekligi / ekranGenisligi;
    const genislik = ekranGenisligi < 700 && oran > 1.2 ? 560 : 800;
    const yukseklik = Math.round(Math.min(1400, Math.max(820, genislik * oran)));
    const degisti = genislik !== GENISLIK || yukseklik !== YUKSEKLIK;
    GENISLIK = genislik;
    YUKSEKLIK = yukseklik;
    return degisti;
}

const RENK = {
    CYAN: [0, 255, 255],
    YELLOW: [255, 255, 0],
    ORANGE: [255, 200, 0],
    MAGENTA: [255, 0, 255],
    RED: [255, 0, 0],
    WHITE: [255, 255, 255],
    LIGHT_GRAY: [192, 192, 192],
    GRAY: [128, 128, 128]
};

function rgb(c, alfa) {
    return alfa === undefined
        ? `rgb(${c[0]},${c[1]},${c[2]})`
        : `rgba(${c[0]},${c[1]},${c[2]},${alfa / 255})`;
}

/** Java'daki Color.brighter() ile aynı sonucu verir. */
function parlak(c) {
    let [r, g, b] = c;
    const i = 3;
    if (r === 0 && g === 0 && b === 0) return [i, i, i];
    if (r > 0 && r < i) r = i;
    if (g > 0 && g < i) g = i;
    if (b > 0 && b < i) b = i;
    return [r, g, b].map(v => Math.min(Math.trunc(v / 0.7), 255));
}

function rastgeleInt(n) {
    return Math.floor(Math.random() * n);
}

function dikdortgen(x, y, w, h) {
    return { x: Math.trunc(x), y: Math.trunc(y), w: Math.trunc(w), h: Math.trunc(h) };
}

function kesisir(a, b) {
    return a.w > 0 && a.h > 0 && b.w > 0 && b.h > 0
        && a.x < b.x + b.w && b.x < a.x + a.w
        && a.y < b.y + b.h && b.y < a.y + a.h;
}

function iceride(r, p) {
    return p.x >= r.x && p.x < r.x + r.w && p.y >= r.y && p.y < r.y + r.h;
}

const Cizim = {
    font(stil, boyut, aile = 'Arial, Helvetica, sans-serif') {
        return `${stil} ${boyut}px ${aile}`;
    },

    yol(g, x, y, w, h, yay) {
        const r = Math.max(0, Math.min(yay / 2, w / 2, h / 2));
        g.beginPath();
        g.moveTo(x + r, y);
        g.arcTo(x + w, y, x + w, y + h, r);
        g.arcTo(x + w, y + h, x, y + h, r);
        g.arcTo(x, y + h, x, y, r);
        g.arcTo(x, y, x + w, y, r);
        g.closePath();
    },

    yuvarlakDolu(g, x, y, w, h, yay) {
        if (w <= 0 || h <= 0) return;
        this.yol(g, x, y, w, h, yay);
        g.fill();
    },

    yuvarlakCizgi(g, x, y, w, h, yay) {
        this.yol(g, x, y, w, h, yay);
        g.stroke();
    },

    ovalDolu(g, x, y, w, h) {
        if (w <= 0 || h <= 0) return;
        g.beginPath();
        g.ellipse(x + w / 2, y + h / 2, w / 2, h / 2, 0, 0, Math.PI * 2);
        g.fill();
    },

    ovalCizgi(g, x, y, w, h) {
        g.beginPath();
        g.ellipse(x + w / 2, y + h / 2, w / 2, h / 2, 0, 0, Math.PI * 2);
        g.stroke();
    },

    cokgen(g, noktalar, dolu) {
        g.beginPath();
        g.moveTo(noktalar[0][0], noktalar[0][1]);
        for (let i = 1; i < noktalar.length; i++) g.lineTo(noktalar[i][0], noktalar[i][1]);
        g.closePath();
        if (dolu) g.fill(); else g.stroke();
    },

    cizgi(g, x1, y1, x2, y2) {
        g.beginPath();
        g.moveTo(x1, y1);
        g.lineTo(x2, y2);
        g.stroke();
    },

    /** Metni en fazla maksGenislik kadar yer kaplayacak şekilde, gerekirse küçülterek yazar. */
    sigdirYazi(g, metin, x, y, maksGenislik, boyut, stil = 'bold', aile) {
        g.font = this.font(stil, boyut, aile);
        const sw = g.measureText(metin).width;
        if (sw > maksGenislik) g.font = this.font(stil, Math.floor(boyut * maksGenislik / sw), aile);
        g.fillText(metin, x, y);
        return Math.min(sw, maksGenislik);
    },

    ortaliYazi(g, metin, y, boyut, renk) {
        g.font = this.font('bold', boyut);
        g.fillStyle = rgb(renk);
        const maks = GENISLIK - 30;
        const sw = Math.min(g.measureText(metin).width, maks);
        this.sigdirYazi(g, metin, GENISLIK / 2 - sw / 2, y, maks, boyut);
    }
};
