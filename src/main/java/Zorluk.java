public enum Zorluk {
    KOLAY("Kolay", 0.80, 0.78, 0.70, 1.00),
    ORTA("Orta", 1.00, 1.00, 1.00, 1.20),
    ZOR("Zor", 1.28, 1.30, 1.35, 1.50);

    private final String ad;
    private final double dusmanHizCarpani;
    private final double dogmaSuresiCarpani;
    private final double hasarCarpani;
    private final double skorCarpani;

    Zorluk(String ad, double dusmanHizCarpani, double dogmaSuresiCarpani,
            double hasarCarpani, double skorCarpani) {
        this.ad = ad;
        this.dusmanHizCarpani = dusmanHizCarpani;
        this.dogmaSuresiCarpani = dogmaSuresiCarpani;
        this.hasarCarpani = hasarCarpani;
        this.skorCarpani = skorCarpani;
    }

    public String getAd() {
        return ad;
    }

    public double getDusmanHizCarpani() {
        return dusmanHizCarpani;
    }

    public double getDogmaSuresiCarpani() {
        return dogmaSuresiCarpani;
    }

    public double getHasarCarpani() {
        return hasarCarpani;
    }

    public double getSkorCarpani() {
        return skorCarpani;
    }
}
