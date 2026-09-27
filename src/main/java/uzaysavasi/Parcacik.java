package uzaysavasi;

import java.awt.Color;
import java.awt.Graphics2D;

public final class Parcacik {
    private double x;
    private double y;
    private final double vx;
    private final double vy;
    private double omur;
    private final double baslangicOmur;
    private final int boyut;
    private final Color renk;

    public Parcacik(double x, double y, double vx, double vy,
            double omur, int boyut, Color renk) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.omur = omur;
        this.baslangicOmur = omur;
        this.boyut = boyut;
        this.renk = renk;
    }

    public void guncelle(double dt) {
        x += vx * dt;
        y += vy * dt;
        omur -= dt;
    }

    public void ciz(Graphics2D g) {
        float oran = (float) Math.max(0, omur / baslangicOmur);
        int alpha = Math.max(0, Math.min(255, (int) (255 * oran)));
        g.setColor(new Color(renk.getRed(), renk.getGreen(), renk.getBlue(), alpha));
        int b = Math.max(1, (int) (boyut * oran));
        g.fillOval((int) x - b / 2, (int) y - b / 2, b, b);
    }

    public boolean aktifMi() { return omur > 0; }
}
