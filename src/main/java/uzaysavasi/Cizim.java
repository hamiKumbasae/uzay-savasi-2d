package uzaysavasi;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;

/** Her karede yeniden oluşturulmasın diye yazı tiplerini saklar. */
final class Cizim {
    private static final Map<String, Font> FONTLAR = new HashMap<>();

    private Cizim() {
    }

    static Font font(int stil, int boyut) {
        return font("Arial", stil, boyut);
    }

    static Font font(String aile, int stil, int boyut) {
        return FONTLAR.computeIfAbsent(aile + "/" + stil + "/" + boyut,
                anahtar -> new Font(aile, stil, boyut));
    }

    static void ortaliYazi(Graphics2D g, String metin, int y, int boyut, Color renk) {
        g.setFont(font(Font.BOLD, boyut));
        g.setColor(renk);
        int sw = g.getFontMetrics().stringWidth(metin);
        g.drawString(metin, OyunPaneli.GENISLIK / 2 - sw / 2, y);
    }
}
