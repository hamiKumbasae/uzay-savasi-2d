import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;

public final class Oyuncu {
    private double x;
    private double y;
    private final int genislik = 56;
    private final int yukseklik = 72;

    private final GemiTuru gemiTuru;
    private final double temelHareketHizi;
    private int maksimumCan;
    private int can;
    private final double temelHasarCarpani;
    private final double temelAtisCarpani;

    private double seviyeHizCarpani = 1.0;
    private double seviyeHasarCarpani = 1.0;
    private double seviyeAtisCarpani = 1.0;

    private double dokunulmazlikSuresi;
    private double kalkanSuresi;
    private double hizliAtesSuresi;
    private double ozelSilahSuresi;
    private SilahTuru kaliciSilahTuru = SilahTuru.TEKLI;
    private SilahTuru silahTuru = SilahTuru.TEKLI;
    private int bombaSayisi;

    public Oyuncu(double x, double y, GemiTuru gemiTuru,
            int hizGelistirmesi, int canGelistirmesi, int hasarGelistirmesi,
            int bombaGelistirmesi) {
        this.x = x;
        this.y = y;
        this.gemiTuru = gemiTuru;
        this.temelHareketHizi = (330 + hizGelistirmesi * 24) * gemiTuru.getHizCarpani();
        this.maksimumCan = Math.max(1,
                (int) Math.round((3 + canGelistirmesi) * gemiTuru.getCanCarpani()));
        this.can = maksimumCan;
        this.temelHasarCarpani = (1.0 + hasarGelistirmesi * 0.14) * gemiTuru.getHasarCarpani();
        this.temelAtisCarpani = gemiTuru.getAtisCarpani();
        this.bombaSayisi = 1 + bombaGelistirmesi;
    }

    public void guncelle(boolean sol, boolean sag, boolean yukari, boolean asagi,
            double dt, int ekranGenisligi, int ekranYuksekligi) {
        double dx = 0;
        double dy = 0;
        if (sol) dx -= 1;
        if (sag) dx += 1;
        if (yukari) dy -= 1;
        if (asagi) dy += 1;

        double uzunluk = Math.hypot(dx, dy);
        if (uzunluk > 0) {
            dx /= uzunluk;
            dy /= uzunluk;
        }

        double hareketHizi = temelHareketHizi * seviyeHizCarpani;
        x += dx * hareketHizi * dt;
        y += dy * hareketHizi * dt;

        sinirla(ekranGenisligi, ekranYuksekligi);

        dokunulmazlikSuresi = Math.max(0, dokunulmazlikSuresi - dt);
        kalkanSuresi = Math.max(0, kalkanSuresi - dt);
        hizliAtesSuresi = Math.max(0, hizliAtesSuresi - dt);
        if (ozelSilahSuresi > 0) {
            ozelSilahSuresi = Math.max(0, ozelSilahSuresi - dt);
            if (ozelSilahSuresi == 0) silahTuru = kaliciSilahTuru;
        }
    }

    public void disKuvvetUygula(double kuvvetX, double kuvvetY, double dt,
            int ekranGenisligi, int ekranYuksekligi) {
        x += kuvvetX * dt;
        y += kuvvetY * dt;
        sinirla(ekranGenisligi, ekranYuksekligi);
    }

    private void sinirla(int ekranGenisligi, int ekranYuksekligi) {
        x = Math.max(8, Math.min(ekranGenisligi - genislik - 8, x));
        y = Math.max(70, Math.min(ekranYuksekligi - yukseklik - 15, y));
    }

    public void ciz(Graphics2D g) {
        if (dokunulmazlikSuresi > 0 && ((int) (dokunulmazlikSuresi * 12)) % 2 == 0) return;

        int ix = (int) x;
        int iy = (int) y;
        int merkezX = ix + genislik / 2;

        Polygon govde = new Polygon();
        govde.addPoint(merkezX, iy);
        govde.addPoint(ix + genislik, iy + yukseklik);
        govde.addPoint(merkezX + 12, iy + yukseklik - 15);
        govde.addPoint(merkezX, iy + yukseklik - 8);
        govde.addPoint(merkezX - 12, iy + yukseklik - 15);
        govde.addPoint(ix, iy + yukseklik);

        g.setColor(gemiTuru.getRenk());
        g.fillPolygon(govde);
        g.setColor(gemiTuru.getRenk().brighter());
        g.setStroke(new BasicStroke(2f));
        g.drawPolygon(govde);

        g.setColor(new Color(40, 90, 210));
        g.fillOval(merkezX - 10, iy + 22, 20, 29);

        g.setColor(Color.ORANGE);
        g.fillOval(ix + 10, iy + yukseklik - 2, 12, 22);
        g.fillOval(ix + genislik - 22, iy + yukseklik - 2, 12, 22);
        g.setColor(Color.YELLOW);
        g.fillOval(ix + 13, iy + yukseklik, 6, 14);
        g.fillOval(ix + genislik - 19, iy + yukseklik, 6, 14);

        if (kalkanSuresi > 0) {
            g.setColor(new Color(80, 220, 255, 100));
            g.setStroke(new BasicStroke(3f));
            g.drawOval(ix - 8, iy - 8, genislik + 16, yukseklik + 16);
        }
    }

    public boolean hasarAl(int miktar) {
        if (dokunulmazlikSuresi > 0 || kalkanSuresi > 0) return false;
        can = Math.max(0, can - miktar);
        dokunulmazlikSuresi = 1.7;
        return true;
    }

    public String seviyeOduluUygula(int seviye) {
        canEkle(1);
        String odul = "+1 can";
        if (seviye % 2 == 0) {
            seviyeHasarCarpani += 0.12;
            odul = "Mermi hasarı arttı";
        }
        if (seviye % 3 == 0) {
            seviyeAtisCarpani *= 0.93;
            odul = "Atış hızı arttı";
        }
        if (seviye % 4 == 0) {
            seviyeHizCarpani += 0.06;
            odul = "Hareket hızı arttı";
        }
        if (seviye % 5 == 0) {
            maksimumCan++;
            can = maksimumCan;
            bombaEkle();
            odul = "Maksimum can ve bomba arttı";
        }
        return odul;
    }

    public void kaliciSilahAc(SilahTuru tur) {
        if (silahSirasi(tur) > silahSirasi(kaliciSilahTuru)) {
            kaliciSilahTuru = tur;
            if (ozelSilahSuresi <= 0) silahTuru = tur;
        }
    }

    private int silahSirasi(SilahTuru tur) {
        return switch (tur) {
            case TEKLI -> 0;
            case CIFTLI -> 1;
            case UCLU -> 2;
            case LAZER -> 3;
            case TAKIP_FUZESI -> 4;
        };
    }

    public void canEkle(int miktar) { can = Math.min(maksimumCan, can + miktar); }
    public void kalkanVer(double sure) { kalkanSuresi = Math.max(kalkanSuresi, sure); }
    public void hizliAtesVer(double sure) { hizliAtesSuresi = Math.max(hizliAtesSuresi, sure); }

    public void silahVer(SilahTuru tur, double sure) {
        silahTuru = tur;
        ozelSilahSuresi = sure;
    }

    public void bombaEkle() { bombaSayisi = Math.min(9, bombaSayisi + 1); }

    public boolean bombaKullan() {
        if (bombaSayisi <= 0) return false;
        bombaSayisi--;
        return true;
    }

    public Rectangle sinirlariGetir() {
        return new Rectangle((int) x + 9, (int) y + 8, genislik - 18, yukseklik - 18);
    }

    public double getMerkezX() { return x + genislik / 2.0; }
    public double getMerkezY() { return y + yukseklik / 2.0; }
    public double getMermiY() { return y; }
    public int getCan() { return can; }
    public int getMaksimumCan() { return maksimumCan; }
    public boolean hayattaMi() { return can > 0; }
    public boolean kalkanAktifMi() { return kalkanSuresi > 0; }
    public double getKalkanSuresi() { return kalkanSuresi; }
    public SilahTuru getSilahTuru() { return silahTuru; }
    public SilahTuru getKaliciSilahTuru() { return kaliciSilahTuru; }
    public int getBombaSayisi() { return bombaSayisi; }
    public double getHasarCarpani() { return temelHasarCarpani * seviyeHasarCarpani; }
    public double getAtisCarpani() { return temelAtisCarpani * seviyeAtisCarpani; }
    public double getAtisSuresiCarpani() { return hizliAtesSuresi > 0 ? 0.48 : 1.0; }
    public GemiTuru getGemiTuru() { return gemiTuru; }
}
