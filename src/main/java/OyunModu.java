public enum OyunModu {
    HIKAYE("Hikâye Modu"),
    ENDLESS("Endless Mod");

    private final String ad;

    OyunModu(String ad) {
        this.ad = ad;
    }

    public String getAd() {
        return ad;
    }
}
