package uzaysavasi;

public enum HikayeBolumu {
    ILK_TEMAS(
            1,
            "İlk Temas",
            "Sınır bölgesindeki düşman filosunu durdur.",
            BolumTemasi.UZAY_BOSLUGU,
            16,
            0,
            0
    ),
    ASTEROIT_KORIDORU(
            2,
            "Asteroit Koridoru",
            "Asteroitleri parçala ve bölgedeki avcıları temizle.",
            BolumTemasi.ASTEROIT_ALANI,
            14,
            12,
            0
    ),
    DUSMAN_USSU(
            3,
            "Düşman Üssü",
            "Zırhlı ve ateşçi birlikleri aşarak üs komutanına ulaş.",
            BolumTemasi.DUSMAN_USSU,
            22,
            0,
            0
    ),
    KARA_DELIK_KACISI(
            4,
            "Kara Delik Kaçışı",
            "Çekim kuvvetine dayan, hayatta kal ve koruma filosunu yok et.",
            BolumTemasi.KARA_DELIK,
            16,
            6,
            45
    ),
    FINAL_SAVASI(
            5,
            "Galaksi İçin Son Savaş",
            "Final filosunu temizle ve ana komuta gemisini yok et.",
            BolumTemasi.FINAL,
            28,
            8,
            0
    );

    private final int numara;
    private final String ad;
    private final String aciklama;
    private final BolumTemasi tema;
    private final int dusmanHedefi;
    private final int asteroitHedefi;
    private final int sureHedefi;

    HikayeBolumu(int numara, String ad, String aciklama, BolumTemasi tema,
            int dusmanHedefi, int asteroitHedefi, int sureHedefi) {
        this.numara = numara;
        this.ad = ad;
        this.aciklama = aciklama;
        this.tema = tema;
        this.dusmanHedefi = dusmanHedefi;
        this.asteroitHedefi = asteroitHedefi;
        this.sureHedefi = sureHedefi;
    }

    public boolean hedefTamamMi(int dusman, int asteroit, double sure) {
        return dusman >= dusmanHedefi
                && asteroit >= asteroitHedefi
                && sure >= sureHedefi;
    }

    public String hedefMetni(int dusman, int asteroit, double sure) {
        StringBuilder metin = new StringBuilder();
        metin.append("Düşman ").append(Math.min(dusman, dusmanHedefi))
                .append('/').append(dusmanHedefi);
        if (asteroitHedefi > 0) {
            metin.append("  Asteroit ").append(Math.min(asteroit, asteroitHedefi))
                    .append('/').append(asteroitHedefi);
        }
        if (sureHedefi > 0) {
            metin.append("  Süre ").append((int) Math.min(sure, sureHedefi))
                    .append('/').append(sureHedefi).append(" sn");
        }
        return metin.toString();
    }

    public static HikayeBolumu numaradan(int numara) {
        for (HikayeBolumu bolum : values()) {
            if (bolum.numara == numara) return bolum;
        }
        return ILK_TEMAS;
    }

    public int getNumara() { return numara; }
    public String getAd() { return ad; }
    public String getAciklama() { return aciklama; }
    public BolumTemasi getTema() { return tema; }
    public int getDusmanHedefi() { return dusmanHedefi; }
}
