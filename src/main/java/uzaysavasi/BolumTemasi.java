package uzaysavasi;

public enum BolumTemasi {
    UZAY_BOSLUGU("Uzay Boşluğu"),
    ASTEROIT_ALANI("Asteroit Alanı"),
    DUSMAN_USSU("Düşman Üssü"),
    KARA_DELIK("Kara Delik Çevresi"),
    FINAL("Final Nebulası");

    private final String ad;

    BolumTemasi(String ad) {
        this.ad = ad;
    }

    public String getAd() {
        return ad;
    }
}
