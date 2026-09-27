package uzaysavasi;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Oyun döngüsü, menü ekranları ve klavye/fare girişi. Oyunun kendisi OyunDunyasi'nda. */
public final class OyunPaneli extends JPanel implements ActionListener, KeyListener,
        MouseListener, MouseMotionListener, OyunDunyasi.Dinleyici {

    public static final int GENISLIK = 800;
    public static final int YUKSEKLIK = 820;

    private final Timer zamanlayici = new Timer(16, this);
    private final Set<Integer> basiliTuslar = new HashSet<>();
    private final Random random = new Random();
    private final SesYoneticisi ses = new SesYoneticisi();
    private final KayitYoneticisi kayit = new KayitYoneticisi();
    private final List<Yildiz> yildizlar = new ArrayList<>();
    private final Map<BolumTemasi, BufferedImage> arkaPlanlar = new EnumMap<>(BolumTemasi.class);

    private OyunDurumu durum = OyunDurumu.ANA_MENU;
    private Zorluk zorluk = Zorluk.ORTA;
    private GemiTuru seciliGemi;
    private OyunDunyasi dunya;

    private long oncekiZaman;
    private int menuIndex;
    private int gemiIndex;
    private int gelistirmeIndex;
    private int hikayeSecimIndex;
    private int mouseUzerindekiMenu = -1;
    private int mouseUzerindekiBolum = -1;
    private String bildirim = "";
    private double bildirimSuresi;

    private static final String[] MENU_SECENEKLERI = {
            "HİKÂYE MODU",
            "ENDLESS MOD",
            "BÖLÜM SEÇ",
            "GEMİ SEÇ",
            "GELİŞTİRMELER",
            "ZORLUK",
            "YÜKSEK SKORLAR",
            "KONTROLLER",
            "SES",
            "ÇIKIŞ"
    };

    private static final String[] GELISTIRME_ADLARI = {
            "Hareket Hızı",
            "Atış Hızı",
            "Mermi Hasarı",
            "Maksimum Can",
            "Kalkan Süresi",
            "Bomba Kapasitesi"
    };

    private static final String[] GELISTIRME_ANAHTARLARI = {
            "hiz", "atis", "hasar", "can", "kalkan", "bomba"
    };

    public OyunPaneli() {
        setPreferredSize(new Dimension(GENISLIK, YUKSEKLIK));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        ses.setEtkin(kayit.sesAcikMi());
        GemiTuru[] gemiler = GemiTuru.values();
        gemiIndex = Math.max(0, Math.min(gemiler.length - 1, kayit.getSeciliGemi()));
        seciliGemi = gemiler[gemiIndex];
        hikayeSecimIndex = kayit.getAcikHikayeBolumu() - 1;
        for (int i = 0; i < 105; i++) yildizlar.add(new Yildiz(random, GENISLIK, YUKSEKLIK));
    }

    public void oyunuBaslat() {
        oncekiZaman = System.nanoTime();
        zamanlayici.start();
        requestFocusInWindow();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        long simdi = System.nanoTime();
        double dt = Math.min(0.035, Math.max(0.001,
                (simdi - oncekiZaman) / 1_000_000_000.0));
        oncekiZaman = simdi;
        for (Yildiz yildiz : yildizlar) yildiz.guncelle(dt, GENISLIK, YUKSEKLIK);
        if (durum == OyunDurumu.OYNUYOR) dunya.guncelle(dt, basiliTuslar::contains);
        if (dunya != null) dunya.efektleriGuncelle(dt);
        bildirimSuresi = Math.max(0, bildirimSuresi - dt);
        repaint();
    }

    private void oyunBaslat(OyunModu mod, int baslangicBolumu) {
        dunya = new OyunDunyasi(mod, baslangicBolumu, seciliGemi, zorluk, kayit, ses, this);
        durum = OyunDurumu.OYNUYOR;
        requestFocusInWindow();
    }

    private void anaMenuyeDon() {
        durum = OyunDurumu.ANA_MENU;
        bildirimSuresi = 0;
    }

    // --- OyunDunyasi.Dinleyici ---

    @Override
    public void bildirim(String mesaj) {
        bildirim = mesaj;
        bildirimSuresi = 2.6;
    }

    @Override public void oyunBitti() { durum = OyunDurumu.OYUN_BITTI; }
    @Override public void bolumTamamlandi() { durum = OyunDurumu.BOLUM_GECISI; }
    @Override public void zafer() { durum = OyunDurumu.ZAFER; }

    // --- Çizim ---

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        double sarsinti = dunya != null ? dunya.getEkranSarsintisi() : 0;
        if (sarsinti > 0) {
            g2.translate((random.nextDouble() - 0.5) * sarsinti,
                    (random.nextDouble() - 0.5) * sarsinti);
        }
        arkaPlanCiz(g2);
        switch (durum) {
            case ANA_MENU -> anaMenuCiz(g2);
            case HIKAYE_BOLUM_SECIMI -> hikayeBolumSecimiCiz(g2);
            case GEMI_SECIMI -> gemiSecimiCiz(g2);
            case GELISTIRMELER -> gelistirmelerCiz(g2);
            case YUKSEK_SKORLAR -> yuksekSkorCiz(g2);
            case KONTROLLER -> kontrollerCiz(g2);
            case OYNUYOR -> oyunCiz(g2, false);
            case DURAKLATILDI -> oyunCiz(g2, true);
            case BOLUM_GECISI -> bolumGecisiCiz(g2);
            case OYUN_BITTI -> oyunBittiCiz(g2);
            case ZAFER -> zaferCiz(g2);
        }
        if (!oyunEkraniDurumuMu() && bildirimSuresi > 0) {
            Cizim.ortaliYazi(g2, bildirim, 712, 20, Color.YELLOW);
        }
        g2.dispose();
    }

    private void arkaPlanCiz(Graphics2D g) {
        BolumTemasi tema = oyunEkraniDurumuMu() ? dunya.temaGetir() : BolumTemasi.UZAY_BOSLUGU;
        g.drawImage(arkaPlanlar.computeIfAbsent(tema, OyunPaneli::gradyanOlustur), 0, 0, null);
        yildizlar.forEach(y -> y.ciz(g));
        if (tema == BolumTemasi.DUSMAN_USSU) {
            g.setColor(new Color(255, 50, 90, 25));
            for (int x = 0; x < GENISLIK; x += 80) g.fillRect(x, 0, 4, YUKSEKLIK);
        } else if (tema == BolumTemasi.KARA_DELIK) {
            g.setColor(new Color(130, 50, 230, 45));
            g.fillOval(GENISLIK / 2 - 220, 125, 440, 440);
            g.setColor(new Color(0, 0, 0, 220));
            g.fillOval(GENISLIK / 2 - 120, 225, 240, 240);
        } else if (tema == BolumTemasi.FINAL) {
            g.setColor(new Color(255, 30, 120, 28));
            g.fillOval(80, 130, 390, 390);
            g.setColor(new Color(90, 30, 255, 25));
            g.fillOval(380, 260, 380, 380);
        }
    }

    /** Temanın dikey renk geçişini bir kez resme çizer; sonraki karelerde resim kullanılır. */
    private static BufferedImage gradyanOlustur(BolumTemasi tema) {
        Color ust;
        Color alt;
        switch (tema) {
            case ASTEROIT_ALANI -> {
                ust = new Color(18, 10, 28);
                alt = new Color(45, 24, 16);
            }
            case DUSMAN_USSU -> {
                ust = new Color(5, 18, 35);
                alt = new Color(35, 5, 18);
            }
            case KARA_DELIK -> {
                ust = new Color(8, 3, 18);
                alt = new Color(24, 4, 45);
            }
            case FINAL -> {
                ust = new Color(42, 3, 24);
                alt = new Color(8, 4, 40);
            }
            default -> {
                ust = new Color(3, 7, 28);
                alt = new Color(12, 20, 58);
            }
        }
        BufferedImage resim = new BufferedImage(GENISLIK, YUKSEKLIK, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resim.createGraphics();
        for (int y = 0; y < YUKSEKLIK; y += 4) {
            double t = y / (double) YUKSEKLIK;
            int r = (int) (ust.getRed() * (1 - t) + alt.getRed() * t);
            int gr = (int) (ust.getGreen() * (1 - t) + alt.getGreen() * t);
            int b = (int) (ust.getBlue() * (1 - t) + alt.getBlue() * t);
            g.setColor(new Color(r, gr, b));
            g.fillRect(0, y, GENISLIK, 4);
        }
        g.dispose();
        return resim;
    }

    private boolean oyunEkraniDurumuMu() {
        return durum == OyunDurumu.OYNUYOR
                || durum == OyunDurumu.DURAKLATILDI
                || durum == OyunDurumu.OYUN_BITTI
                || durum == OyunDurumu.BOLUM_GECISI
                || durum == OyunDurumu.ZAFER;
    }

    private void oyunCiz(Graphics2D g, boolean duraklatildi) {
        dunya.ciz(g);
        if (bildirimSuresi > 0) Cizim.ortaliYazi(g, bildirim, 190, 22, Color.YELLOW);
        if (duraklatildi) {
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(0, 0, GENISLIK, YUKSEKLIK);
            Cizim.ortaliYazi(g, "DURAKLATILDI", 360, 44, Color.WHITE);
            Cizim.ortaliYazi(g, "Devam etmek için P", 410, 22, Color.LIGHT_GRAY);
            Cizim.ortaliYazi(g, "Ana menü için ESC", 444, 18, Color.LIGHT_GRAY);
        }
    }

    private void anaMenuCiz(Graphics2D g) {
        Cizim.ortaliYazi(g, "UZAY SAVAŞI", 96, 52, new Color(80, 230, 255));
        Cizim.ortaliYazi(g, "HİKÂYE + ENDLESS + XP SİSTEMİ", 132, 20,
                new Color(255, 200, 60));
        g.setFont(Cizim.font(Font.BOLD, 19));
        for (int i = 0; i < MENU_SECENEKLERI.length; i++) {
            int y = 195 + i * 52;
            Rectangle alan = menuButonAlani(i);
            boolean secili = i == menuIndex || i == mouseUzerindekiMenu;
            g.setColor(secili ? new Color(40, 130, 180, 215)
                    : new Color(10, 20, 50, 190));
            g.fillRoundRect(alan.x, alan.y, alan.width, alan.height, 16, 16);
            g.setColor(secili ? Color.YELLOW : new Color(150, 220, 255));
            g.setStroke(new BasicStroke(secili ? 3f : 1.5f));
            g.drawRoundRect(alan.x, alan.y, alan.width, alan.height, 16, 16);
            String metin = switch (i) {
                case 0 -> "HİKÂYE MODU - DEVAM: BÖLÜM " + kayit.getAcikHikayeBolumu();
                case 5 -> "ZORLUK: " + zorluk.getAd().toUpperCase();
                case 8 -> "SES: " + (ses.etkinMi() ? "AÇIK" : "KAPALI");
                default -> MENU_SECENEKLERI[i];
            };
            int sw = g.getFontMetrics().stringWidth(metin);
            g.drawString(metin, GENISLIK / 2 - sw / 2, y + 7);
        }
        g.setFont(Cizim.font(Font.PLAIN, 15));
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("↑ ↓ / Fare: Seç   ENTER / Tıkla: Onayla", 20, YUKSEKLIK - 25);
        g.drawString("Coin: " + kayit.getCoin() + "   En yüksek seviye: "
                + kayit.getEnYuksekOyuncuSeviyesi(), GENISLIK - 300, YUKSEKLIK - 25);
        if (kayit.hikayeTamamlandiMi()) {
            Cizim.ortaliYazi(g, "Hikâye tamamlandı! Bölümleri yeniden oynayabilirsin.",
                    760, 15, new Color(120, 255, 150));
        }
    }

    private Rectangle menuButonAlani(int index) {
        return new Rectangle(GENISLIK / 2 - 235, 195 + index * 52 - 29, 470, 39);
    }

    private void hikayeBolumSecimiCiz(Graphics2D g) {
        Cizim.ortaliYazi(g, "HİKÂYE BÖLÜMLERİ", 85, 40, Color.CYAN);
        int acikBolum = kayit.getAcikHikayeBolumu();
        for (int i = 0; i < HikayeBolumu.values().length; i++) {
            HikayeBolumu hb = HikayeBolumu.values()[i];
            Rectangle alan = bolumSecimAlani(i);
            boolean acik = hb.getNumara() <= acikBolum;
            boolean secili = i == hikayeSecimIndex || i == mouseUzerindekiBolum;
            g.setColor(!acik ? new Color(35, 35, 48, 205)
                    : secili ? new Color(40, 105, 155, 220)
                    : new Color(10, 25, 55, 205));
            g.fillRoundRect(alan.x, alan.y, alan.width, alan.height, 18, 18);
            g.setColor(!acik ? Color.GRAY : secili ? Color.YELLOW : Color.CYAN);
            g.setStroke(new BasicStroke(secili ? 3f : 1.5f));
            g.drawRoundRect(alan.x, alan.y, alan.width, alan.height, 18, 18);
            g.setFont(Cizim.font(Font.BOLD, 21));
            g.drawString("Bölüm " + hb.getNumara() + " - " + hb.getAd(),
                    alan.x + 20, alan.y + 31);
            g.setFont(Cizim.font(Font.PLAIN, 15));
            g.setColor(acik ? Color.WHITE : Color.GRAY);
            g.drawString(acik ? hb.getAciklama() : "KİLİTLİ - Önceki bölümü tamamla",
                    alan.x + 20, alan.y + 58);
        }
        Cizim.ortaliYazi(g, "↑ ↓ seç   ENTER başlat   ESC geri", 760, 18, Color.LIGHT_GRAY);
    }

    private Rectangle bolumSecimAlani(int index) {
        return new Rectangle(75, 135 + index * 112, 650, 82);
    }

    private void gemiSecimiCiz(Graphics2D g) {
        Cizim.ortaliYazi(g, "GEMİ SEÇİMİ", 110, 42, Color.CYAN);
        GemiTuru gemi = GemiTuru.values()[gemiIndex];
        g.setColor(new Color(0, 0, 0, 155));
        g.fillRoundRect(125, 175, 550, 430, 28, 28);
        Oyuncu onizleme = new Oyuncu(GENISLIK / 2.0 - 28, 235, gemi, 0, 0, 0, 0);
        onizleme.ciz(g);
        Cizim.ortaliYazi(g, gemi.getAd(), 390, 30, gemi.getRenk());
        Cizim.ortaliYazi(g, gemi.getAciklama(), 430, 19, Color.WHITE);
        ozellikCubuguCiz(g, "Hız", 205, 480, gemi.getHizCarpani() / 1.5);
        ozellikCubuguCiz(g, "Dayanıklılık", 205, 520, gemi.getCanCarpani() / 1.5);
        ozellikCubuguCiz(g, "Hasar", 205, 560, gemi.getHasarCarpani() / 1.5);
        Cizim.ortaliYazi(g, "← → gemi değiştir   ENTER seç   ESC geri", 675, 18, Color.LIGHT_GRAY);
    }

    private void ozellikCubuguCiz(Graphics2D g, String ad, int x, int y, double oran) {
        g.setFont(Cizim.font(Font.BOLD, 17));
        g.setColor(Color.WHITE);
        g.drawString(ad, x, y);
        g.setColor(new Color(45, 45, 65));
        g.fillRoundRect(x + 145, y - 17, 270, 18, 10, 10);
        g.setColor(new Color(50, 220, 255));
        g.fillRoundRect(x + 145, y - 17, (int) (270 * Math.min(1, oran)), 18, 10, 10);
    }

    private void gelistirmelerCiz(Graphics2D g) {
        Cizim.ortaliYazi(g, "GELİŞTİRMELER", 90, 40, Color.YELLOW);
        Cizim.ortaliYazi(g, "Coin: " + kayit.getCoin(), 128, 21, Color.WHITE);
        for (int i = 0; i < GELISTIRME_ADLARI.length; i++) {
            int y = 205 + i * 82;
            int seviye = kayit.getGelistirme(GELISTIRME_ANAHTARLARI[i]);
            boolean secili = i == gelistirmeIndex;
            g.setColor(secili ? new Color(45, 110, 160, 210)
                    : new Color(10, 20, 45, 185));
            g.fillRoundRect(105, y - 42, 590, 64, 18, 18);
            g.setColor(secili ? Color.YELLOW : Color.CYAN);
            g.drawRoundRect(105, y - 42, 590, 64, 18, 18);
            g.setFont(Cizim.font(Font.BOLD, 20));
            g.drawString(GELISTIRME_ADLARI[i], 130, y - 9);
            g.setFont(Cizim.font(Font.PLAIN, 17));
            g.setColor(Color.WHITE);
            g.drawString("Seviye: " + seviye + "/5", 420, y - 9);
            g.drawString(seviye >= 5 ? "MAKSİMUM" : "Fiyat: " + gelistirmeFiyati(seviye), 550, y - 9);
        }
        Cizim.ortaliYazi(g, "↑ ↓ seç   ENTER satın al   ESC geri", 735, 18, Color.LIGHT_GRAY);
    }

    private static int gelistirmeFiyati(int seviye) {
        return 60 * (seviye + 1) * (seviye + 1);
    }

    private void gelistirmeSatinAl() {
        String anahtar = GELISTIRME_ANAHTARLARI[gelistirmeIndex];
        int seviye = kayit.getGelistirme(anahtar);
        if (seviye >= 5) {
            bildirim("Bu geliştirme maksimum seviyede");
            return;
        }
        int fiyat = gelistirmeFiyati(seviye);
        if (kayit.getCoin() < fiyat) {
            bildirim("Yeterli coin yok");
            ses.hasar();
            return;
        }
        kayit.setCoin(kayit.getCoin() - fiyat);
        kayit.setGelistirme(anahtar, seviye + 1);
        ses.guclendirme();
        bildirim("Geliştirme satın alındı");
    }

    private void yuksekSkorCiz(Graphics2D g) {
        Cizim.ortaliYazi(g, "YÜKSEK SKORLAR", 125, 42, Color.YELLOW);
        List<Integer> skorlar = kayit.yuksekSkorlar();
        if (skorlar.isEmpty()) {
            Cizim.ortaliYazi(g, "Henüz kayıtlı skor yok", 330, 24, Color.LIGHT_GRAY);
        }
        for (int i = 0; i < skorlar.size(); i++) {
            Cizim.ortaliYazi(g, (i + 1) + ".   " + skorlar.get(i), 245 + i * 65, 30,
                    i == 0 ? Color.YELLOW : Color.WHITE);
        }
        Cizim.ortaliYazi(g, "En yüksek oyuncu seviyesi: " + kayit.getEnYuksekOyuncuSeviyesi(),
                610, 21, Color.CYAN);
        Cizim.ortaliYazi(g, "ESC ile geri dön", 700, 18, Color.LIGHT_GRAY);
    }

    private void kontrollerCiz(Graphics2D g) {
        Cizim.ortaliYazi(g, "KONTROLLER VE SEVİYE SİSTEMİ", 88, 36, Color.CYAN);
        String[] satirlar = {
                "WASD / Ok Tuşları  : Hareket",
                "SPACE              : Ateş",
                "B                  : Mega bomba",
                "P                  : Duraklat",
                "M                  : Sesi aç/kapat",
                "R                  : Oyun bitince yeniden başlat",
                "ESC                : Menüye dön",
                "",
                "Düşman ve asteroit yok ederek XP kazanırsın.",
                "Sv.3 Çiftli, Sv.5 Üçlü, Sv.8 Lazer, Sv.10 Füze açılır."
        };
        g.setFont(Cizim.font("Monospaced", Font.BOLD, 18));
        g.setColor(Color.WHITE);
        for (int i = 0; i < satirlar.length; i++) g.drawString(satirlar[i], 90, 175 + i * 49);
        Cizim.ortaliYazi(g, "ESC ile geri dön", 740, 18, Color.LIGHT_GRAY);
    }

    private void karartmaCiz(Graphics2D g, int opaklik) {
        oyunCiz(g, false);
        g.setColor(new Color(0, 0, 0, opaklik));
        g.fillRect(0, 0, GENISLIK, YUKSEKLIK);
    }

    private void bolumGecisiCiz(Graphics2D g) {
        karartmaCiz(g, 218);
        int bolum = dunya.getBolum();
        Cizim.ortaliYazi(g, "BÖLÜM " + bolum + " TAMAMLANDI", 255, 42, Color.YELLOW);
        Cizim.ortaliYazi(g, HikayeBolumu.numaradan(bolum).getAd(), 305, 25, Color.CYAN);
        Cizim.ortaliYazi(g, "Skor: " + dunya.getSkor() + "   Coin: " + dunya.getCoin()
                + "   Oyuncu seviyesi: " + dunya.getOyuncuSeviyesi(),
                370, 21, Color.WHITE);
        int sonrakiNo = dunya.getSonrakiBolum();
        HikayeBolumu sonraki = HikayeBolumu.numaradan(sonrakiNo);
        Cizim.ortaliYazi(g, "Sıradaki: Bölüm " + sonrakiNo + " - " + sonraki.getAd(),
                440, 25, new Color(120, 230, 255));
        Cizim.ortaliYazi(g, sonraki.getAciklama(), 480, 17, Color.LIGHT_GRAY);
        Cizim.ortaliYazi(g, "Devam etmek için ENTER", 560, 23, Color.WHITE);
        Cizim.ortaliYazi(g, "Kaydedip menüye dönmek için ESC", 600, 17, Color.LIGHT_GRAY);
    }

    private void oyunBittiCiz(Graphics2D g) {
        karartmaCiz(g, 205);
        OyunModu mod = dunya.getOyunModu();
        Cizim.ortaliYazi(g, "OYUN BİTTİ", 275, 56, new Color(255, 70, 100));
        Cizim.ortaliYazi(g, mod.getAd(), 325, 24, Color.CYAN);
        Cizim.ortaliYazi(g, "Skor: " + dunya.getSkor(), 370, 32, Color.WHITE);
        Cizim.ortaliYazi(g, "Kazanılan coin: " + dunya.getCoin(), 414, 24, Color.YELLOW);
        Cizim.ortaliYazi(g, "Oyuncu seviyesi: " + dunya.getOyuncuSeviyesi(), 453, 22, Color.WHITE);
        Cizim.ortaliYazi(g, mod == OyunModu.HIKAYE
                ? "Ulaşılan hikâye bölümü: " + dunya.getBolum()
                : "Ulaşılan endless dalgası: " + dunya.getBolum(), 490, 20, Color.LIGHT_GRAY);
        Cizim.ortaliYazi(g, "R: Bölümü yeniden başlat", 555, 22, Color.CYAN);
        Cizim.ortaliYazi(g, "ESC: Ana menü", 595, 20, Color.LIGHT_GRAY);
    }

    private void zaferCiz(Graphics2D g) {
        karartmaCiz(g, 215);
        Cizim.ortaliYazi(g, "GALAKSİ KURTARILDI!", 245, 48, Color.YELLOW);
        Cizim.ortaliYazi(g, "HİKÂYE MODUNU TAMAMLADIN", 300, 27, Color.CYAN);
        Cizim.ortaliYazi(g, "Final skor: " + dunya.getSkor(), 370, 30, Color.WHITE);
        Cizim.ortaliYazi(g, "Toplanan coin: " + dunya.getCoin(), 415, 23, Color.YELLOW);
        Cizim.ortaliYazi(g, "Ulaşılan oyuncu seviyesi: " + dunya.getOyuncuSeviyesi(),
                455, 22, Color.WHITE);
        Cizim.ortaliYazi(g, "Silah: " + dunya.getKaliciSilahTuru().getAd(),
                492, 20, Color.LIGHT_GRAY);
        Cizim.ortaliYazi(g, "R: Hikâyeyi baştan oyna", 565, 22, Color.CYAN);
        Cizim.ortaliYazi(g, "ESC: Ana menü", 605, 20, Color.LIGHT_GRAY);
    }

    // --- Giriş ---

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        basiliTuslar.add(code);
        switch (durum) {
            case ANA_MENU -> {
                if (code == KeyEvent.VK_UP) {
                    menuIndex = (menuIndex - 1 + MENU_SECENEKLERI.length) % MENU_SECENEKLERI.length;
                } else if (code == KeyEvent.VK_DOWN) {
                    menuIndex = (menuIndex + 1) % MENU_SECENEKLERI.length;
                } else if (code == KeyEvent.VK_ENTER) {
                    menuSeciminiCalistir();
                }
            }
            case HIKAYE_BOLUM_SECIMI -> {
                int adet = HikayeBolumu.values().length;
                if (code == KeyEvent.VK_UP) {
                    hikayeSecimIndex = (hikayeSecimIndex - 1 + adet) % adet;
                } else if (code == KeyEvent.VK_DOWN) {
                    hikayeSecimIndex = (hikayeSecimIndex + 1) % adet;
                } else if (code == KeyEvent.VK_ENTER) {
                    seciliHikayeBolumunuBaslat();
                } else if (code == KeyEvent.VK_ESCAPE) {
                    anaMenuyeDon();
                }
            }
            case GEMI_SECIMI -> {
                int adet = GemiTuru.values().length;
                if (code == KeyEvent.VK_LEFT) {
                    gemiIndex = (gemiIndex - 1 + adet) % adet;
                } else if (code == KeyEvent.VK_RIGHT) {
                    gemiIndex = (gemiIndex + 1) % adet;
                } else if (code == KeyEvent.VK_ENTER) {
                    seciliGemi = GemiTuru.values()[gemiIndex];
                    kayit.setSeciliGemi(gemiIndex);
                    ses.menu();
                    anaMenuyeDon();
                } else if (code == KeyEvent.VK_ESCAPE) {
                    anaMenuyeDon();
                }
            }
            case GELISTIRMELER -> {
                int adet = GELISTIRME_ADLARI.length;
                if (code == KeyEvent.VK_UP) {
                    gelistirmeIndex = (gelistirmeIndex - 1 + adet) % adet;
                } else if (code == KeyEvent.VK_DOWN) {
                    gelistirmeIndex = (gelistirmeIndex + 1) % adet;
                } else if (code == KeyEvent.VK_ENTER) {
                    gelistirmeSatinAl();
                } else if (code == KeyEvent.VK_ESCAPE) {
                    anaMenuyeDon();
                }
            }
            case YUKSEK_SKORLAR, KONTROLLER -> {
                if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_ENTER) anaMenuyeDon();
            }
            case OYNUYOR -> {
                if (code == KeyEvent.VK_B) {
                    dunya.bombaKullan();
                } else if (code == KeyEvent.VK_P) {
                    durum = OyunDurumu.DURAKLATILDI;
                } else if (code == KeyEvent.VK_M) {
                    sesiDegistir();
                } else if (code == KeyEvent.VK_ESCAPE) {
                    kaydedipMenuyeDon();
                }
            }
            case DURAKLATILDI -> {
                if (code == KeyEvent.VK_P) {
                    durum = OyunDurumu.OYNUYOR;
                } else if (code == KeyEvent.VK_ESCAPE) {
                    kaydedipMenuyeDon();
                }
            }
            case BOLUM_GECISI -> {
                if (code == KeyEvent.VK_ENTER) {
                    dunya.sonrakiHikayeBolumunuBaslat();
                    durum = OyunDurumu.OYNUYOR;
                    requestFocusInWindow();
                } else if (code == KeyEvent.VK_ESCAPE) {
                    kaydedipMenuyeDon();
                }
            }
            case OYUN_BITTI -> {
                if (code == KeyEvent.VK_R) {
                    OyunModu mod = dunya.getOyunModu();
                    oyunBaslat(mod, mod == OyunModu.HIKAYE ? dunya.getBolum() : 1);
                } else if (code == KeyEvent.VK_ESCAPE) {
                    anaMenuyeDon();
                }
            }
            case ZAFER -> {
                if (code == KeyEvent.VK_R) {
                    oyunBaslat(OyunModu.HIKAYE, 1);
                } else if (code == KeyEvent.VK_ESCAPE) {
                    anaMenuyeDon();
                }
            }
        }
    }

    private void kaydedipMenuyeDon() {
        dunya.kazanciKaydet();
        anaMenuyeDon();
    }

    private void menuSeciminiCalistir() {
        ses.menu();
        switch (menuIndex) {
            case 0 -> oyunBaslat(OyunModu.HIKAYE, kayit.getAcikHikayeBolumu());
            case 1 -> oyunBaslat(OyunModu.ENDLESS, 1);
            case 2 -> {
                hikayeSecimIndex = kayit.getAcikHikayeBolumu() - 1;
                durum = OyunDurumu.HIKAYE_BOLUM_SECIMI;
            }
            case 3 -> durum = OyunDurumu.GEMI_SECIMI;
            case 4 -> durum = OyunDurumu.GELISTIRMELER;
            case 5 -> zorluk = Zorluk.values()[(zorluk.ordinal() + 1) % Zorluk.values().length];
            case 6 -> durum = OyunDurumu.YUKSEK_SKORLAR;
            case 7 -> durum = OyunDurumu.KONTROLLER;
            case 8 -> sesiDegistir();
            case 9 -> System.exit(0);
            default -> { }
        }
    }

    private void seciliHikayeBolumunuBaslat() {
        int bolumNo = hikayeSecimIndex + 1;
        if (bolumNo > kayit.getAcikHikayeBolumu()) {
            ses.hasar();
            bildirim("Bu bölüm henüz kilitli");
            return;
        }
        oyunBaslat(OyunModu.HIKAYE, bolumNo);
    }

    private void sesiDegistir() {
        ses.setEtkin(!ses.etkinMi());
        kayit.setSesAcik(ses.etkinMi());
    }

    @Override
    public void keyReleased(KeyEvent e) {
        basiliTuslar.remove(e.getKeyCode());
    }

    @Override public void keyTyped(KeyEvent e) { }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseUzerindekiMenu = -1;
        mouseUzerindekiBolum = -1;
        if (durum == OyunDurumu.ANA_MENU) {
            for (int i = 0; i < MENU_SECENEKLERI.length; i++) {
                if (menuButonAlani(i).contains(e.getPoint())) {
                    mouseUzerindekiMenu = i;
                    break;
                }
            }
        } else if (durum == OyunDurumu.HIKAYE_BOLUM_SECIMI) {
            for (int i = 0; i < HikayeBolumu.values().length; i++) {
                if (bolumSecimAlani(i).contains(e.getPoint())) {
                    mouseUzerindekiBolum = i;
                    break;
                }
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        requestFocusInWindow();
        if (durum == OyunDurumu.ANA_MENU && mouseUzerindekiMenu >= 0) {
            menuIndex = mouseUzerindekiMenu;
            menuSeciminiCalistir();
        } else if (durum == OyunDurumu.HIKAYE_BOLUM_SECIMI
                && mouseUzerindekiBolum >= 0) {
            hikayeSecimIndex = mouseUzerindekiBolum;
            seciliHikayeBolumunuBaslat();
        }
    }

    @Override public void mousePressed(MouseEvent e) { }
    @Override public void mouseReleased(MouseEvent e) { }
    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e) {
        mouseUzerindekiMenu = -1;
        mouseUzerindekiBolum = -1;
    }
    @Override public void mouseDragged(MouseEvent e) { }
}
