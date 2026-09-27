package uzaysavasi;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;

public final class Boss {
    private double x;
    private double y = -190;
    private final int genislik = 220;
    private final int yukseklik = 150;
    private final int maksimumCan;
    private int can;
    private double zaman;
    private double atisSayaci = 1.2;
    private int saldiriSirasi;
    private boolean aktif = true;

    public Boss(int ekranGenisligi, int bolum) {
        x = ekranGenisligi / 2.0 - genislik / 2.0;
        maksimumCan = 95 + bolum * 32;
        can = maksimumCan;
    }

    public void guncelle(double dt, int ekranGenisligi) {
        zaman += dt;
        if (y < 70) {
            y += 75 * dt;
        } else {
            x = ekranGenisligi / 2.0 - genislik / 2.0 + Math.sin(zaman * 0.9) * 210;
        }
        atisSayaci -= dt;
    }

    public boolean saldiriHazirMi() {
        if (atisSayaci > 0 || y < 40) return false;
        atisSayaci = Math.max(0.55, 1.45 - zaman * 0.004);
        saldiriSirasi = (saldiriSirasi + 1) % 3;
        return true;
    }

    public int getSaldiriSirasi() { return saldiriSirasi; }

    public void ciz(Graphics2D g) {
        int ix = (int) x;
        int iy = (int) y;
        Polygon p = new Polygon();
        p.addPoint(ix + 15, iy + 25);
        p.addPoint(ix + genislik / 2, iy);
        p.addPoint(ix + genislik - 15, iy + 25);
        p.addPoint(ix + genislik, iy + yukseklik - 25);
        p.addPoint(ix + genislik / 2, iy + yukseklik);
        p.addPoint(ix, iy + yukseklik - 25);
        g.setColor(new Color(145, 35, 210));
        g.fillPolygon(p);
        g.setColor(new Color(255, 80, 230));
        g.setStroke(new BasicStroke(4f));
        g.drawPolygon(p);
        g.setColor(new Color(30, 15, 65));
        g.fillOval(ix + 70, iy + 32, 80, 65);
        g.setColor(Color.RED);
        g.fillOval(ix + 98, iy + 53, 24, 24);

        g.setColor(new Color(20, 20, 35));
        g.fillRoundRect(130, 18, 540, 24, 12, 12);
        g.setColor(new Color(255, 65, 110));
        g.fillRoundRect(134, 22, (int) (532 * (can / (double) maksimumCan)), 16, 8, 8);
        g.setColor(Color.WHITE);
        g.drawString("BOSS", 72, 37);
    }

    public boolean hasarAl(int miktar) {
        can -= miktar;
        if (can <= 0) aktif = false;
        return !aktif;
    }

    public Rectangle sinirlariGetir() {
        return new Rectangle((int) x + 10, (int) y + 8, genislik - 20, yukseklik - 16);
    }

    public double getMerkezX() { return x + genislik / 2.0; }
    public double getMerkezY() { return y + yukseklik / 2.0; }
    public boolean aktifMi() { return aktif; }
}
