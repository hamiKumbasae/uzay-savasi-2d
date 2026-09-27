package uzaysavasi;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public final class Guclendirme {
    private double x;
    private double y;
    private final GuclendirmeTuru tur;
    private double zaman;
    private boolean aktif = true;

    public Guclendirme(double x, double y, GuclendirmeTuru tur) {
        this.x = x;
        this.y = y;
        this.tur = tur;
    }

    public void guncelle(double dt) {
        y += 125 * dt;
        zaman += dt;
    }

    public void ciz(Graphics2D g) {
        int ix = (int) x;
        int iy = (int) y;
        int nabiz = (int) (Math.sin(zaman * 6) * 3);
        Color renk = renkGetir();
        g.setColor(new Color(renk.getRed(), renk.getGreen(), renk.getBlue(), 75));
        g.fillOval(ix - 5 - nabiz, iy - 5 - nabiz, 46 + nabiz * 2, 46 + nabiz * 2);
        g.setColor(renk);
        g.fillRoundRect(ix, iy, 36, 36, 12, 12);
        g.setColor(Color.WHITE);
        g.setFont(Cizim.font(Font.BOLD, 14));
        String simge = simgeGetir();
        int sw = g.getFontMetrics().stringWidth(simge);
        g.drawString(simge, ix + 18 - sw / 2, iy + 23);
    }

    private Color renkGetir() {
        return switch (tur) {
            case CAN -> new Color(255, 70, 90);
            case KALKAN -> new Color(50, 210, 255);
            case HIZLI_ATES -> new Color(255, 190, 40);
            case LAZER -> new Color(210, 80, 255);
            case TAKIP_FUZESI -> new Color(255, 120, 40);
            case BOMBA -> new Color(90, 90, 100);
            case CIFT_MERMI -> new Color(70, 255, 130);
            case UCLU_MERMI -> new Color(255, 225, 70);
        };
    }

    private String simgeGetir() {
        return switch (tur) {
            case CAN -> "+";
            case KALKAN -> "S";
            case HIZLI_ATES -> "R";
            case LAZER -> "L";
            case TAKIP_FUZESI -> "M";
            case BOMBA -> "B";
            case CIFT_MERMI -> "2x";
            case UCLU_MERMI -> "3x";
        };
    }

    public Rectangle sinirlariGetir() { return new Rectangle((int) x, (int) y, 36, 36); }
    public boolean ekranDisindaMi(int yukseklik) { return y > yukseklik + 50; }
    public GuclendirmeTuru getTur() { return tur; }
    public boolean aktifMi() { return aktif; }
    public void yokEt() { aktif = false; }
}
