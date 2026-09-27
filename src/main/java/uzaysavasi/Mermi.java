package uzaysavasi;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public final class Mermi {
    private double x;
    private double y;
    private double vx;
    private double vy;
    private final int genislik;
    private final int yukseklik;
    private final int hasar;
    private final boolean oyuncuMermisi;
    private final boolean delici;
    private final boolean takipEden;
    private final Color renk;
    private boolean aktif = true;

    public Mermi(double x, double y, double vx, double vy, int hasar,
            boolean oyuncuMermisi, boolean delici, boolean takipEden, Color renk) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.hasar = hasar;
        this.oyuncuMermisi = oyuncuMermisi;
        this.delici = delici;
        this.takipEden = takipEden;
        this.renk = renk;
        this.genislik = delici ? 12 : (takipEden ? 12 : 7);
        this.yukseklik = delici ? 80 : (takipEden ? 22 : 17);
    }

    public void guncelle(double dt, Double hedefX, Double hedefY) {
        if (takipEden && hedefX != null && hedefY != null) {
            double dx = hedefX - x;
            double dy = hedefY - y;
            double uzunluk = Math.hypot(dx, dy);
            if (uzunluk > 1) {
                double hedefVx = dx / uzunluk * 480;
                double hedefVy = dy / uzunluk * 480;
                vx += (hedefVx - vx) * Math.min(1, dt * 5.5);
                vy += (hedefVy - vy) * Math.min(1, dt * 5.5);
            }
        }
        x += vx * dt;
        y += vy * dt;
    }

    public void ciz(Graphics2D g) {
        if (delici) {
            g.setColor(new Color(renk.getRed(), renk.getGreen(), renk.getBlue(), 65));
            g.fillRoundRect((int) x - 5, (int) y, genislik + 10, yukseklik, 12, 12);
            g.setColor(renk);
            g.fillRoundRect((int) x, (int) y, genislik, yukseklik, 8, 8);
        } else if (takipEden) {
            g.setColor(Color.ORANGE);
            g.fillOval((int) x - 5, (int) y + 14, 10, 16);
            g.setColor(renk);
            g.fillRoundRect((int) x, (int) y, genislik, yukseklik, 7, 7);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2f));
            g.drawLine((int) x + 2, (int) y + 4, (int) x + genislik - 2, (int) y + 4);
        } else {
            int ix = (int) x;
            int iy = (int) y;

            // Düşman mermileri koyu arka planlarda kaybolmasın diye parlak hale getirilir.
            if (!oyuncuMermisi) {
                g.setColor(new Color(255, 35, 80, 75));
                g.fillOval(ix - 7, iy - 5, genislik + 14, yukseklik + 10);
                g.setColor(new Color(255, 215, 80));
                g.fillRoundRect(ix - 1, iy, genislik + 2, yukseklik, 7, 7);
                g.setColor(Color.WHITE);
                g.fillOval(ix + genislik / 2 - 2, iy + 3, 4, 6);
            } else {
                g.setColor(renk);
                g.fillRoundRect(ix, iy, genislik, yukseklik, 5, 5);
                g.setColor(Color.WHITE);
                g.drawLine(ix + genislik / 2, iy + 2,
                        ix + genislik / 2, iy + yukseklik - 2);
            }
        }
    }

    public Rectangle sinirlariGetir() {
        if (!oyuncuMermisi && !delici && !takipEden) {
            return new Rectangle((int) x + 1, (int) y + 2,
                    Math.max(3, genislik - 2), Math.max(7, yukseklik - 4));
        }
        return new Rectangle((int) x, (int) y, genislik, yukseklik);
    }

    public boolean ekranDisindaMi(int genislik, int yukseklik) {
        return x < -120 || x > genislik + 120 || y < -140 || y > yukseklik + 140;
    }

    public int getHasar() { return hasar; }
    public boolean deliciMi() { return delici; }
    public boolean aktifMi() { return aktif; }
    public void yokEt() { aktif = false; }
}
