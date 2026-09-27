package uzaysavasi;

public enum SilahTuru {
    TEKLI("Tekli"),
    CIFTLI("Çiftli"),
    UCLU("Üçlü"),
    LAZER("Lazer"),
    TAKIP_FUZESI("Füze");

    private final String ad;

    SilahTuru(String ad) {
        this.ad = ad;
    }

    public String getAd() {
        return ad;
    }
}
