package uzaysavasi;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/**
 * Efektleri açılışta bir kez üretir; çalarken hazır klibi başa sarar.
 * Böylece her atışta ses aygıtına yeni bağlantı açılmaz.
 */
public final class SesYoneticisi {
    private static final float ORNEKLEME_HIZI = 22050f;

    private final Clip atis = klipOlustur(45, 0.18, 720);
    private final Clip patlama = klipOlustur(120, 0.36, 105);
    private final Clip guclendirme = klipOlustur(55, 0.18, 440, 660, 880);
    private final Clip hasar = klipOlustur(100, 0.28, 190, 120);
    private final Clip menu = klipOlustur(45, 0.14, 520);
    private final Clip boss = klipOlustur(90, 0.35, 180, 150, 120, 90);
    private final Clip bolum = klipOlustur(75, 0.22, 330, 440, 660, 880);
    private final Clip seviyeAtlama = klipOlustur(65, 0.24, 440, 554, 659, 880);
    private final Clip zafer = klipOlustur(95, 0.28, 392, 523, 659, 784, 1046);
    private boolean etkin = true;

    public void setEtkin(boolean etkin) { this.etkin = etkin; }
    public boolean etkinMi() { return etkin; }

    public void atis() { cal(atis); }
    public void patlama() { cal(patlama); }
    public void guclendirme() { cal(guclendirme); }
    public void hasar() { cal(hasar); }
    public void menu() { cal(menu); }
    public void boss() { cal(boss); }
    public void bolum() { cal(bolum); }
    public void seviyeAtlama() { cal(seviyeAtlama); }
    public void zafer() { cal(zafer); }

    private void cal(Clip klip) {
        if (!etkin || klip == null) return;
        klip.stop();
        klip.setFramePosition(0);
        klip.start();
    }

    /** Frekansları sırayla çalan, her tonu sona doğru sönen 16 bit mono bir klip üretir. */
    private static Clip klipOlustur(int tonSuresiMs, double ses, int... frekanslar) {
        int tonOrnekSayisi = (int) (tonSuresiMs * ORNEKLEME_HIZI / 1000.0);
        byte[] veri = new byte[tonOrnekSayisi * frekanslar.length * 2];
        int k = 0;
        for (int frekans : frekanslar) {
            for (int i = 0; i < tonOrnekSayisi; i++) {
                double faz = 2.0 * Math.PI * frekans * i / ORNEKLEME_HIZI;
                double zarf = 1.0 - i / (double) tonOrnekSayisi;
                short deger = (short) (Math.sin(faz) * Short.MAX_VALUE * ses * zarf);
                veri[k++] = (byte) deger;
                veri[k++] = (byte) (deger >> 8);
            }
        }
        try {
            Clip klip = AudioSystem.getClip();
            klip.open(new AudioFormat(ORNEKLEME_HIZI, 16, 1, true, false), veri, 0, veri.length);
            return klip;
        } catch (Exception e) {
            // Ses aygıtı yoksa oyun sessiz devam eder.
            return null;
        }
    }
}
