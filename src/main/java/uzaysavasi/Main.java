package uzaysavasi;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame pencere = new JFrame("Uzay Savaşı 2D");
            OyunPaneli oyunPaneli = new OyunPaneli();

            pencere.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            pencere.setResizable(false);
            pencere.add(oyunPaneli);
            pencere.pack();
            pencere.setLocationRelativeTo(null);
            pencere.setVisible(true);

            oyunPaneli.oyunuBaslat();
        });
    }
}
