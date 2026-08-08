import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Random;

public final class Asteroit {
    private double x;
    private double y;
    private double vx;
    private double vy;
    private final int boyut;
    private int can;
    private final boolean buyuk;
    private double donus;
    private boolean aktif = true;
    private final int[] sekilX;
    private final int[] sekilY;

    public Asteroit(Random random, int ekranGenisligi, int bolum, boolean buyuk) {
        this.buyuk = buyuk;
        boyut = buyuk ? 62 + random.nextInt(28) : 28 + random.nextInt(22);
        can = buyuk ? 4 : 1;
        x = random.nextDouble() * Math.max(1, ekranGenisligi - boyut);
        y = -boyut - random.nextInt(100);
        vx = -45 + random.nextDouble() * 90;
        vy = 105 + bolum * 6 + random.nextDouble() * 80;
        sekilX = new int[10];
        sekilY = new int[10];
        for (int i = 0; i < 10; i++) {
            double aci = Math.PI * 2 * i / 10.0;
            double r = boyut * (0.38 + random.nextDouble() * 0.12);
            sekilX[i] = (int) (Math.cos(aci) * r);
            sekilY[i] = (int) (Math.sin(aci) * r);
        }
    }

    public Asteroit(double x, double y, double vx, double vy, int boyut) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.boyut = boyut;
        this.buyuk = false;
        this.can = 1;
        this.sekilX = new int[10];
        this.sekilY = new int[10];
        for (int i = 0; i < 10; i++) {
            double aci = Math.PI * 2 * i / 10.0;
            double r = boyut * (0.40 + ((i * 37) % 11) / 100.0);
            sekilX[i] = (int) (Math.cos(aci) * r);
            sekilY[i] = (int) (Math.sin(aci) * r);
        }
    }

    public void guncelle(double dt) {
        x += vx * dt;
        y += vy * dt;
        donus += dt * 1.4;
    }

    public void ciz(Graphics2D g) {
        int cx = (int) x + boyut / 2;
        int cy = (int) y + boyut / 2;
        Polygon p = new Polygon();
        double cos = Math.cos(donus);
        double sin = Math.sin(donus);
        for (int i = 0; i < sekilX.length; i++) {
            int rx = (int) (sekilX[i] * cos - sekilY[i] * sin);
            int ry = (int) (sekilX[i] * sin + sekilY[i] * cos);
            p.addPoint(cx + rx, cy + ry);
        }
        g.setColor(new Color(100, 86, 78));
        g.fillPolygon(p);
        g.setColor(new Color(155, 135, 115));
        g.drawPolygon(p);
        g.setColor(new Color(65, 55, 50));
        g.fillOval(cx - boyut / 7, cy - boyut / 6, boyut / 4, boyut / 5);
        g.fillOval(cx + boyut / 8, cy + boyut / 10, boyut / 5, boyut / 6);
    }

    public boolean hasarAl(int miktar) {
        can -= miktar;
        if (can <= 0) aktif = false;
        return !aktif;
    }

    public Rectangle sinirlariGetir() {
        return new Rectangle((int) x + 3, (int) y + 3, boyut - 6, boyut - 6);
    }

    public boolean ekranDisindaMi(int genislik, int yukseklik) {
        return y > yukseklik + 80 || x < -100 || x > genislik + 100;
    }

    public boolean buyukMu() { return buyuk; }
    public double getMerkezX() { return x + boyut / 2.0; }
    public double getMerkezY() { return y + boyut / 2.0; }
    public int getBoyut() { return boyut; }
    public int getXpDegeri() { return buyuk ? 20 : 8; }
    public boolean aktifMi() { return aktif; }
    public void yokEt() { aktif = false; }
}
