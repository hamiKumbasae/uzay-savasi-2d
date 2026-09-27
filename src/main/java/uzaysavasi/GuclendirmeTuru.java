package uzaysavasi;

public enum GuclendirmeTuru {
    CIFT_MERMI("Çift Mermi"),
    UCLU_MERMI("Üçlü Mermi"),
    HIZLI_ATES("Hızlı Ateş"),
    KALKAN("Kalkan"),
    CAN("+1 Can"),
    LAZER("Lazer"),
    TAKIP_FUZESI("Takip Füzesi"),
    BOMBA("+1 Bomba");

    private final String ad;

    GuclendirmeTuru(String ad) {
        this.ad = ad;
    }

    public String getAd() {
        return ad;
    }
}
