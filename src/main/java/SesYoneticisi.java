import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SesYoneticisi {
    private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "oyun-sesi");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean etkin = true;

    public void setEtkin(boolean etkin) { this.etkin = etkin; }
    public boolean etkinMi() { return etkin; }

    public void atis() { ton(720, 45, 0.18); }
    public void patlama() { ton(105, 120, 0.36); }
    public void guclendirme() { siraliTon(new int[]{440, 660, 880}, 55, 0.18); }
    public void hasar() { siraliTon(new int[]{190, 120}, 100, 0.28); }
    public void menu() { ton(520, 45, 0.14); }
    public void boss() { siraliTon(new int[]{180, 150, 120, 90}, 90, 0.35); }
    public void bolum() { siraliTon(new int[]{330, 440, 660, 880}, 75, 0.22); }
    public void seviyeAtlama() { siraliTon(new int[]{440, 554, 659, 880}, 65, 0.24); }
    public void zafer() { siraliTon(new int[]{392, 523, 659, 784, 1046}, 95, 0.28); }

    private void siraliTon(int[] frekanslar, int sureMs, double ses) {
        if (!etkin) return;
        executor.submit(() -> {
            for (int f : frekanslar) {
                if (!etkin) break;
                tonuCal(f, sureMs, ses);
            }
        });
    }

    private void ton(int frekans, int sureMs, double ses) {
        if (!etkin) return;
        executor.submit(() -> tonuCal(frekans, sureMs, ses));
    }

    private void tonuCal(int frekans, int sureMs, double ses) {
        try {
            float sampleRate = 22050f;
            AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, false);
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format);
            line.start();
            int ornekSayisi = (int) (sureMs * sampleRate / 1000.0);
            byte[] buffer = new byte[Math.min(1024, ornekSayisi)];
            int yazilan = 0;
            while (yazilan < ornekSayisi) {
                int adet = Math.min(buffer.length, ornekSayisi - yazilan);
                for (int i = 0; i < adet; i++) {
                    double faz = 2.0 * Math.PI * frekans * (yazilan + i) / sampleRate;
                    double zarf = 1.0 - (yazilan + i) / (double) ornekSayisi;
                    buffer[i] = (byte) (Math.sin(faz) * 127 * ses * zarf);
                }
                line.write(buffer, 0, adet);
                yazilan += adet;
            }
            line.drain();
            line.close();
        } catch (Exception ignored) {
            // Ses aygıtı yoksa oyun sessiz devam eder.
        }
    }
}
