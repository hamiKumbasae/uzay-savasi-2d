import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

public final class KayitYoneticisi {
    private final Path klasor = Path.of(System.getProperty("user.home"), ".uzay_savasi_2d");
    private final Path ayarDosyasi = klasor.resolve("kayit.properties");
    private final Path skorDosyasi = klasor.resolve("skorlar.txt");
    private final Properties properties = new Properties();

    public KayitYoneticisi() {
        yukle();
    }

    private void yukle() {
        try {
            Files.createDirectories(klasor);
            if (Files.exists(ayarDosyasi)) {
                try (InputStream in = Files.newInputStream(ayarDosyasi)) {
                    properties.load(in);
                }
            }
        } catch (IOException ignored) {
        }
    }

    public int getCoin() { return intGetir("coin", 0); }
    public void setCoin(int coin) { properties.setProperty("coin", String.valueOf(Math.max(0, coin))); kaydet(); }
    public boolean sesAcikMi() { return Boolean.parseBoolean(properties.getProperty("ses", "true")); }
    public void setSesAcik(boolean acik) { properties.setProperty("ses", String.valueOf(acik)); kaydet(); }
    public int getGelistirme(String ad) { return intGetir("gelistirme." + ad, 0); }
    public void setGelistirme(String ad, int seviye) { properties.setProperty("gelistirme." + ad, String.valueOf(seviye)); kaydet(); }
    public int getSeciliGemi() { return intGetir("seciliGemi", 0); }
    public void setSeciliGemi(int index) { properties.setProperty("seciliGemi", String.valueOf(index)); kaydet(); }
    public int getAcikHikayeBolumu() { return Math.max(1, Math.min(5, intGetir("hikaye.acikBolum", 1))); }
    public void setAcikHikayeBolumu(int bolum) {
        int yeniDeger = Math.max(getAcikHikayeBolumu(), Math.max(1, Math.min(5, bolum)));
        properties.setProperty("hikaye.acikBolum", String.valueOf(yeniDeger));
        kaydet();
    }
    public boolean hikayeTamamlandiMi() { return Boolean.parseBoolean(properties.getProperty("hikaye.tamamlandi", "false")); }
    public void setHikayeTamamlandi(boolean tamamlandi) { properties.setProperty("hikaye.tamamlandi", String.valueOf(tamamlandi)); kaydet(); }
    public int getEnYuksekOyuncuSeviyesi() { return Math.max(1, intGetir("istatistik.enYuksekSeviye", 1)); }
    public void setEnYuksekOyuncuSeviyesi(int seviye) {
        properties.setProperty("istatistik.enYuksekSeviye", String.valueOf(Math.max(getEnYuksekOyuncuSeviyesi(), seviye)));
        kaydet();
    }

    private int intGetir(String anahtar, int varsayilan) {
        try { return Integer.parseInt(properties.getProperty(anahtar, String.valueOf(varsayilan))); }
        catch (NumberFormatException e) { return varsayilan; }
    }

    private void kaydet() {
        try {
            Files.createDirectories(klasor);
            try (OutputStream out = Files.newOutputStream(ayarDosyasi,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                properties.store(out, "Uzay Savasi 2D kayit dosyasi");
            }
        } catch (IOException ignored) {
        }
    }

    public List<Integer> yuksekSkorlar() {
        List<Integer> skorlar = new ArrayList<>();
        try {
            if (Files.exists(skorDosyasi)) {
                for (String satir : Files.readAllLines(skorDosyasi)) {
                    try { skorlar.add(Integer.parseInt(satir.trim())); }
                    catch (NumberFormatException ignored) { }
                }
            }
        } catch (IOException ignored) { }
        skorlar.sort(Comparator.reverseOrder());
        return skorlar.stream().limit(5).toList();
    }

    public void skorEkle(int skor) {
        List<Integer> skorlar = new ArrayList<>(yuksekSkorlar());
        skorlar.add(Math.max(0, skor));
        skorlar.sort(Comparator.reverseOrder());
        try {
            Files.createDirectories(klasor);
            List<String> satirlar = skorlar.stream().limit(5).map(String::valueOf).toList();
            Files.write(skorDosyasi, satirlar, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) { }
    }
}
