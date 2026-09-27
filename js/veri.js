'use strict';

const OYUN_MODU = {
    HIKAYE: { ad: 'Hikâye Modu' },
    ENDLESS: { ad: 'Endless Mod' }
};

// Sıra önemli: Endless modda dalgalar bu sırayla döner.
const TEMALAR = ['UZAY_BOSLUGU', 'ASTEROIT_ALANI', 'DUSMAN_USSU', 'KARA_DELIK', 'FINAL'];
const TEMA_ADI = {
    UZAY_BOSLUGU: 'Uzay Boşluğu',
    ASTEROIT_ALANI: 'Asteroit Alanı',
    DUSMAN_USSU: 'Düşman Üssü',
    KARA_DELIK: 'Kara Delik Çevresi',
    FINAL: 'Final Nebulası'
};

// Sıra önemli: kalıcı silah yalnızca daha güçlü bir silahla değişir.
const SILAHLAR = ['TEKLI', 'CIFTLI', 'UCLU', 'LAZER', 'TAKIP_FUZESI'];
const SILAH_ADI = {
    TEKLI: 'Tekli',
    CIFTLI: 'Çiftli',
    UCLU: 'Üçlü',
    LAZER: 'Lazer',
    TAKIP_FUZESI: 'Füze'
};

const GUCLENDIRMELER = {
    CIFT_MERMI: { ad: 'Çift Mermi', renk: [70, 255, 130], simge: '2x' },
    UCLU_MERMI: { ad: 'Üçlü Mermi', renk: [255, 225, 70], simge: '3x' },
    HIZLI_ATES: { ad: 'Hızlı Ateş', renk: [255, 190, 40], simge: 'R' },
    KALKAN: { ad: 'Kalkan', renk: [50, 210, 255], simge: 'S' },
    CAN: { ad: '+1 Can', renk: [255, 70, 90], simge: '+' },
    LAZER: { ad: 'Lazer', renk: [210, 80, 255], simge: 'L' },
    TAKIP_FUZESI: { ad: 'Takip Füzesi', renk: [255, 120, 40], simge: 'M' },
    BOMBA: { ad: '+1 Bomba', renk: [90, 90, 100], simge: 'B' }
};
const GUCLENDIRME_TURLERI = Object.keys(GUCLENDIRMELER);

const GEMILER = [
    { ad: 'Denge Gemisi', renk: [45, 220, 255], hiz: 1.00, can: 1.00, hasar: 1.00, atis: 1.00,
        aciklama: 'Her alanda dengeli' },
    { ad: 'Hızlı Gemi', renk: [90, 255, 130], hiz: 1.28, can: 0.82, hasar: 0.92, atis: 0.90,
        aciklama: 'Çok hızlı, daha az dayanıklı' },
    { ad: 'Tank Gemisi', renk: [255, 170, 45], hiz: 0.82, can: 1.48, hasar: 0.90, atis: 1.12,
        aciklama: 'Yavaş ama çok dayanıklı' },
    { ad: 'Saldırı Gemisi', renk: [255, 70, 110], hiz: 0.94, can: 0.92, hasar: 1.42, atis: 1.08,
        aciklama: 'Yüksek mermi hasarı' }
];

const ZORLUKLAR = [
    { ad: 'Kolay', dusmanHiz: 0.80, dogmaSuresi: 0.78, hasar: 0.70, skor: 1.00 },
    { ad: 'Orta', dusmanHiz: 1.00, dogmaSuresi: 1.00, hasar: 1.00, skor: 1.20 },
    { ad: 'Zor', dusmanHiz: 1.28, dogmaSuresi: 1.30, hasar: 1.35, skor: 1.50 }
];

const HIKAYE_BOLUMLERI = [
    { numara: 1, ad: 'İlk Temas', aciklama: 'Sınır bölgesindeki düşman filosunu durdur.',
        tema: 'UZAY_BOSLUGU', dusman: 16, asteroit: 0, sure: 0 },
    { numara: 2, ad: 'Asteroit Koridoru', aciklama: 'Asteroitleri parçala ve bölgedeki avcıları temizle.',
        tema: 'ASTEROIT_ALANI', dusman: 14, asteroit: 12, sure: 0 },
    { numara: 3, ad: 'Düşman Üssü', aciklama: 'Zırhlı ve ateşçi birlikleri aşarak üs komutanına ulaş.',
        tema: 'DUSMAN_USSU', dusman: 22, asteroit: 0, sure: 0 },
    { numara: 4, ad: 'Kara Delik Kaçışı', aciklama: 'Çekim kuvvetine dayan, hayatta kal ve koruma filosunu yok et.',
        tema: 'KARA_DELIK', dusman: 16, asteroit: 6, sure: 45 },
    { numara: 5, ad: 'Galaksi İçin Son Savaş', aciklama: 'Final filosunu temizle ve ana komuta gemisini yok et.',
        tema: 'FINAL', dusman: 28, asteroit: 8, sure: 0 }
];

function hikayeBolumu(numara) {
    return HIKAYE_BOLUMLERI.find(b => b.numara === numara) || HIKAYE_BOLUMLERI[0];
}

function hedefTamamMi(b, dusman, asteroit, sure) {
    return dusman >= b.dusman && asteroit >= b.asteroit && sure >= b.sure;
}

function hedefMetni(b, dusman, asteroit, sure) {
    let metin = `Düşman ${Math.min(dusman, b.dusman)}/${b.dusman}`;
    if (b.asteroit > 0) metin += `  Asteroit ${Math.min(asteroit, b.asteroit)}/${b.asteroit}`;
    if (b.sure > 0) metin += `  Süre ${Math.trunc(Math.min(sure, b.sure))}/${b.sure} sn`;
    return metin;
}
