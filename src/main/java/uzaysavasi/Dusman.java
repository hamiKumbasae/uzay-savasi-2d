package uzaysavasi;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Random;

public final class Dusman {
    private double x;
    private double y;
    private final double baslangicX;
    private final int genislik;
    private final int yukseklik;
    private int can;
    private final int maksimumCan;
    private final double hiz;
    private final DusmanTuru tur;
    private final int puan;
    private final Color renk;
    private double zaman;
    private double atisSayaci;
    private final double atisAraligi;
    private boolean aktif = true;

    public Dusman(Random random, int ekranGenisligi, int bolum,
            double hizCarpani, DusmanTuru zorunluTur) {
        tur = zorunluTur != null ? zorunluTur : rastgeleTur(random, bolum);
        switch (tur) {
            case HIZLI -> {
                genislik = 38; yukseklik = 52; maksimumCan = 1;
                hiz = (235 + bolum * 7) * hizCarpani; puan = 140;
                renk = new Color(255, 90, 100);
            }
            case ZIRHLI -> {
                genislik = 64; yukseklik = 72; maksimumCan = 5 + bolum / 3;
                hiz = (95 + bolum * 4) * hizCarpani; puan = 360;
                renk = new Color(170, 125, 255);
            }
            case ZIKZAK -> {
                genislik = 48; yukseklik = 58; maksimumCan = 2;
                hiz = (145 + bolum * 5) * hizCarpani; puan = 220;
                renk = new Color(255, 205, 55);
            }
            case ATESCI -> {
                genislik = 54; yukseklik = 64; maksimumCan = 3 + bolum / 5;
                hiz = (110 + bolum * 4) * hizCarpani; puan = 300;
                renk = new Color(255, 95, 205);
            }
            default -> {
                genislik = 48; yukseklik = 58; maksimumCan = 2 + bolum / 6;
                hiz = (125 + bolum * 5) * hizCarpani; puan = 100;
                renk = new Color(85, 240, 130);
            }
        }
        can = maksimumCan;
        x = 20 + random.nextDouble() * Math.max(1, ekranGenisligi - genislik - 40);
        baslangicX = x;
        y = -yukseklik - random.nextInt(140);
        atisAraligi = 1.4 + random.nextDouble() * 1.7;
        atisSayaci = random.nextDouble() * atisAraligi;
    }

    private DusmanTuru rastgeleTur(Random random, int bolum) {
        int r = random.nextInt(100);
        if (bolum >= 3 && r < 13) return DusmanTuru.ATESCI;
        if (bolum >= 2 && r < 28) return DusmanTuru.ZIKZAK;
        if (bolum >= 2 && r < 42) return DusmanTuru.ZIRHLI;
        if (r < 62) return DusmanTuru.HIZLI;
        return DusmanTuru.NORMAL;
    }

    public void guncelle(double dt, double oyuncuX) {
        zaman += dt;
        y += hiz * dt;
        if (tur == DusmanTuru.ZIKZAK) {
            x = baslangicX + Math.sin(zaman * 3.8) * 95;
        } else if (tur == DusmanTuru.ATESCI) {
            x += Math.signum(oyuncuX - getMerkezX()) * 42 * dt;
        }
        atisSayaci -= dt;
    }

    public boolean atesEtmeliMi() {
        if (tur != DusmanTuru.ATESCI || atisSayaci > 0) return false;
        atisSayaci = atisAraligi;
        return true;
    }

    public void ciz(Graphics2D g) {
        int ix = (int) x;
        int iy = (int) y;
        int merkezX = ix + genislik / 2;
        Polygon p = new Polygon();
        p.addPoint(ix, iy);
        p.addPoint(merkezX, iy + 15);
        p.addPoint(ix + genislik, iy);
        p.addPoint(ix + genislik - 8, iy + yukseklik);
        p.addPoint(merkezX, iy + yukseklik - 12);
        p.addPoint(ix + 8, iy + yukseklik);

        g.setColor(renk);
        g.fillPolygon(p);
        g.setColor(renk.brighter());
        g.drawPolygon(p);
        g.setColor(new Color(40, 30, 90));
        g.fillOval(merkezX - 9, iy + 20, 18, 22);

        if (maksimumCan > 2) {
            g.setColor(new Color(30, 30, 45));
            g.fillRect(ix, iy - 9, genislik, 5);
            g.setColor(Color.RED);
            g.fillRect(ix, iy - 9, (int) (genislik * (can / (double) maksimumCan)), 5);
        }
    }

    public boolean hasarAl(int miktar) {
        can -= miktar;
        if (can <= 0) aktif = false;
        return !aktif;
    }

    public Rectangle sinirlariGetir() {
        return new Rectangle((int) x + 4, (int) y + 4, genislik - 8, yukseklik - 8);
    }

    public boolean ekranDisindaMi(int yukseklik) { return y > yukseklik + 40; }
    public double getMerkezX() { return x + genislik / 2.0; }
    public double getMerkezY() { return y + yukseklik / 2.0; }
    public int getPuan() { return puan; }
    public int getXpDegeri() {
        return switch (tur) {
            case NORMAL -> 10;
            case HIZLI -> 15;
            case ZIRHLI -> 25;
            case ZIKZAK -> 20;
            case ATESCI -> 30;
        };
    }
    public DusmanTuru getTur() { return tur; }
    public boolean aktifMi() { return aktif; }
    public void yokEt() { aktif = false; }
}
