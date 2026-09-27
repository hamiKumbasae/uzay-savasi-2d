package uzaysavasi;

import java.awt.Color;

public enum GemiTuru {
    DENGELI("Denge Gemisi", new Color(45, 220, 255), 1.00, 1.00, 1.00, 1.00,
            "Her alanda dengeli"),
    HIZLI("Hızlı Gemi", new Color(90, 255, 130), 1.28, 0.82, 0.92, 0.90,
            "Çok hızlı, daha az dayanıklı"),
    TANK("Tank Gemisi", new Color(255, 170, 45), 0.82, 1.48, 0.90, 1.12,
            "Yavaş ama çok dayanıklı"),
    SALDIRI("Saldırı Gemisi", new Color(255, 70, 110), 0.94, 0.92, 1.42, 1.08,
            "Yüksek mermi hasarı");

    private final String ad;
    private final Color renk;
    private final double hizCarpani;
    private final double canCarpani;
    private final double hasarCarpani;
    private final double atisCarpani;
    private final String aciklama;

    GemiTuru(String ad, Color renk, double hizCarpani, double canCarpani,
            double hasarCarpani, double atisCarpani, String aciklama) {
        this.ad = ad;
        this.renk = renk;
        this.hizCarpani = hizCarpani;
        this.canCarpani = canCarpani;
        this.hasarCarpani = hasarCarpani;
        this.atisCarpani = atisCarpani;
        this.aciklama = aciklama;
    }

    public String getAd() { return ad; }
    public Color getRenk() { return renk; }
    public double getHizCarpani() { return hizCarpani; }
    public double getCanCarpani() { return canCarpani; }
    public double getHasarCarpani() { return hasarCarpani; }
    public double getAtisCarpani() { return atisCarpani; }
    public String getAciklama() { return aciklama; }
}
