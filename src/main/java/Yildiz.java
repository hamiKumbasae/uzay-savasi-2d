import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;

public final class Yildiz {
    private double x;
    private double y;
    private final double hiz;
    private final int boyut;
    private final Color renk;

    public Yildiz(Random random, int genislik, int yukseklik) {
        x = random.nextDouble() * genislik;
        y = random.nextDouble() * yukseklik;
        hiz = 45 + random.nextDouble() * 210;
        boyut = 1 + random.nextInt(4);
        int parlaklik = 150 + random.nextInt(106);
        renk = new Color(parlaklik, parlaklik, 255);
    }

    public void guncelle(double dt, int genislik, int yukseklik) {
        y += hiz * dt;
        if (y > yukseklik + 5) {
            y = -5;
            x = Math.random() * genislik;
        }
    }

    public void ciz(Graphics2D g) {
        g.setColor(renk);
        g.fillOval((int) x, (int) y, boyut, boyut);
    }
}
