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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class OyunPaneli extends JPanel implements ActionListener, KeyListener,
        MouseListener, MouseMotionListener {

    public static final int GENISLIK = 800;
    public static final int YUKSEKLIK = 820;

    private final Timer zamanlayici = new Timer(16, this);
    private final Set<Integer> basiliTuslar = new HashSet<>();
    private final Random random = new Random();
    private final SesYoneticisi ses = new SesYoneticisi();
    private final KayitYoneticisi kayit = new KayitYoneticisi();

    private final List<Yildiz> yildizlar = new ArrayList<>();
    private final List<Mermi> oyuncuMermileri = new ArrayList<>();
    private final List<Mermi> dusmanMermileri = new ArrayList<>();
    private final List<Dusman> dusmanlar = new ArrayList<>();
    private final List<Asteroit> asteroitler = new ArrayList<>();
    private final List<Guclendirme> guclendirmeler = new ArrayList<>();
    private final List<Parcacik> parcaciklar = new ArrayList<>();

    private OyunDurumu durum = OyunDurumu.ANA_MENU;
    private OyunModu oyunModu = OyunModu.HIKAYE;
    private Zorluk zorluk = Zorluk.ORTA;
    private GemiTuru seciliGemi = GemiTuru.DENGELI;
    private HikayeBolumu hikayeBolumu = HikayeBolumu.ILK_TEMAS;
    private SeviyeSistemi seviyeSistemi = new SeviyeSistemi();
    private Oyuncu oyuncu;
    private Boss boss;

    private long oncekiZaman;
    private double gecenSure;
    private double bolumSuresi;
    private double dusmanDogmaSayaci;
    private double asteroitDogmaSayaci;
    private double atisBekleme;
    private double ekranSarsintisi;
    private int skor;
    private int coin;
    private int bolum = 1;
    private int sonrakiBolum;
    private int oldurulenDusman;
    private int yokEdilenAsteroit;
    private int hedefDusman;
    private boolean bossBekleniyor;
    private int menuIndex;
    private int gemiIndex;
    private int gelistirmeIndex;
    private int hikayeSecimIndex;
    private int mouseUzerindekiMenu = -1;
    private int mouseUzerindekiBolum = -1;
    private boolean skorKaydedildi;
    private String bildirim = "";
    private double bildirimSuresi;

    private final String[] menuSecenekleri = {
            "HİKÂYE MODU",
            "ENDLESS MOD",
            "BÖLÜM SEÇ",
            "GEMİ SEÇ",
            "GELİŞTİRMELER",
            "ZORLUK: ORTA",
            "YÜKSEK SKORLAR",
            "KONTROLLER",
            "SES: AÇIK",
            "ÇIKIŞ"
    };

    private final String[] gelistirmeAdlari = {
            "Hareket Hızı",
            "Atış Hızı",
            "Mermi Hasarı",
            "Maksimum Can",
            "Kalkan Süresi",
            "Bomba Kapasitesi"
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
        if (durum == OyunDurumu.OYNUYOR) oyunGuncelle(dt);
        parcaciklar.forEach(p -> p.guncelle(dt));
        parcaciklar.removeIf(p -> !p.aktifMi());
        ekranSarsintisi = Math.max(0, ekranSarsintisi - dt * 18);
        bildirimSuresi = Math.max(0, bildirimSuresi - dt);
        repaint();
    }

    private void oyunBaslat(OyunModu mod, int baslangicBolumu) {
        oyunModu = mod;
        bolum = mod == OyunModu.HIKAYE
                ? Math.max(1, Math.min(5, baslangicBolumu))
                : 1;
        skor = 0;
        coin = 0;
        gecenSure = 0;
        skorKaydedildi = false;
        seviyeSistemi = new SeviyeSistemi();
        oyuncu = new Oyuncu(GENISLIK / 2.0 - 28, YUKSEKLIK - 120, seciliGemi,
                kayit.getGelistirme("hiz"), kayit.getGelistirme("can"),
                kayit.getGelistirme("hasar"), kayit.getGelistirme("bomba"));
        bolumuHazirla(false);
        durum = OyunDurumu.OYNUYOR;
        bildirimGoster(aktifBolumBasligi());
        requestFocusInWindow();
    }

    private void bolumuHazirla(boolean oyuncuyuKoru) {
        oyuncuMermileri.clear();
        dusmanMermileri.clear();
        dusmanlar.clear();
        asteroitler.clear();
        guclendirmeler.clear();
        parcaciklar.clear();
        boss = null;
        bossBekleniyor = false;
        oldurulenDusman = 0;
        yokEdilenAsteroit = 0;
        bolumSuresi = 0;
        dusmanDogmaSayaci = 0.65;
        asteroitDogmaSayaci = 1.5;
        atisBekleme = 0;
        if (oyunModu == OyunModu.HIKAYE) {
            hikayeBolumu = HikayeBolumu.numaradan(bolum);
            hedefDusman = hikayeBolumu.getDusmanHedefi();
        } else {
            hedefDusman = 18 + (bolum - 1) * 4;
        }
        if (oyuncuyuKoru && oyuncu != null) oyuncu.canEkle(1);
    }

    private void oyunGuncelle(double dt) {
        gecenSure += dt;
        bolumSuresi += dt;
        boolean sol = tus(KeyEvent.VK_LEFT) || tus(KeyEvent.VK_A);
        boolean sag = tus(KeyEvent.VK_RIGHT) || tus(KeyEvent.VK_D);
        boolean yukari = tus(KeyEvent.VK_UP) || tus(KeyEvent.VK_W);
        boolean asagi = tus(KeyEvent.VK_DOWN) || tus(KeyEvent.VK_S);
        oyuncu.guncelle(sol, sag, yukari, asagi, dt, GENISLIK, YUKSEKLIK);
        karaDelikKuvvetiUygula(dt);
        atisBekleme = Math.max(0, atisBekleme - dt);

        if (tus(KeyEvent.VK_SPACE) && atisBekleme <= 0) oyuncuAtesEt();

        if (boss == null && !bossBekleniyor) {
            dusmanVeAsteroitOlustur(dt);
            if (bolumHedefiTamamMi()) bossOlustur();
        }

        dusmanlariGuncelle(dt);
        asteroitleriGuncelle(dt);
        mermileriGuncelle(dt);
        guclendirmeleriGuncelle(dt);
        if (boss != null) bossGuncelle(dt);
        carpismalariKontrolEt();

        if (!oyuncu.hayattaMi()) oyunBitti();
    }

    private void dusmanVeAsteroitOlustur(double dt) {
        dusmanDogmaSayaci -= dt;
        double temelAralik = Math.max(0.34, 1.15 - bolum * 0.045);
        if (oyunModu == OyunModu.HIKAYE && bolum == 5) temelAralik *= 0.78;
        double aralik = temelAralik * zorluk.getDogmaSuresiCarpani();
        if (dusmanDogmaSayaci <= 0) {
            dusmanlar.add(new Dusman(random, GENISLIK, bolum,
                    zorluk.getDusmanHizCarpani(), dusmanTuruSec()));
            dusmanDogmaSayaci = aralik * (0.78 + random.nextDouble() * 0.48);
        }

        if (asteroitOlusturulmali()) {
            asteroitDogmaSayaci -= dt;
            if (asteroitDogmaSayaci <= 0) {
                double buyukOlasiligi = temaGetir() == BolumTemasi.ASTEROIT_ALANI ? 0.38 : 0.22;
                asteroitler.add(new Asteroit(random, GENISLIK, bolum,
                        random.nextDouble() < buyukOlasiligi));
                asteroitDogmaSayaci = Math.max(0.65, 2.25 - bolum * 0.08)
                        * (0.72 + random.nextDouble());
            }
        }
    }

    private DusmanTuru dusmanTuruSec() {
        int r = random.nextInt(100);
        return switch (temaGetir()) {
            case ASTEROIT_ALANI -> r < 26 ? DusmanTuru.HIZLI
                    : r < 48 ? DusmanTuru.ZIKZAK : null;
            case DUSMAN_USSU -> r < 28 ? DusmanTuru.ATESCI
                    : r < 57 ? DusmanTuru.ZIRHLI : null;
            case KARA_DELIK -> r < 34 ? DusmanTuru.ZIKZAK
                    : r < 58 ? DusmanTuru.HIZLI : null;
            case FINAL -> r < 25 ? DusmanTuru.ATESCI
                    : r < 48 ? DusmanTuru.ZIRHLI
                    : r < 70 ? DusmanTuru.ZIKZAK : null;
            default -> null;
        };
    }

    private boolean asteroitOlusturulmali() {
        BolumTemasi tema = temaGetir();
        return tema == BolumTemasi.ASTEROIT_ALANI
                || tema == BolumTemasi.KARA_DELIK
                || tema == BolumTemasi.FINAL
                || (oyunModu == OyunModu.ENDLESS && bolum >= 2);
    }

    private boolean bolumHedefiTamamMi() {
        if (oyunModu == OyunModu.HIKAYE) {
            return hikayeBolumu.hedefTamamMi(oldurulenDusman, yokEdilenAsteroit, bolumSuresi);
        }
        return oldurulenDusman >= hedefDusman;
    }

    private void bossOlustur() {
        bossBekleniyor = true;
        dusmanlar.clear();
        asteroitler.clear();
        dusmanMermileri.clear();
        int bossGucu = oyunModu == OyunModu.HIKAYE && bolum == 5 ? 8 : bolum;
        boss = new Boss(GENISLIK, bossGucu);
        ses.boss();
        bildirimGoster(oyunModu == OyunModu.HIKAYE && bolum == 5
                ? "FİNAL BOSS YAKLAŞIYOR!" : "BOSS YAKLAŞIYOR!");
    }

    private void karaDelikKuvvetiUygula(double dt) {
        if (temaGetir() != BolumTemasi.KARA_DELIK || oyuncu == null) return;
        double merkezX = GENISLIK / 2.0;
        double merkezY = 340;
        double dx = merkezX - oyuncu.getMerkezX();
        double dy = merkezY - oyuncu.getMerkezY();
        double mesafe = Math.max(90, Math.hypot(dx, dy));
        double kuvvet = 5400.0 / mesafe;
        oyuncu.disKuvvetUygula(dx / mesafe * kuvvet, dy / mesafe * kuvvet,
                dt, GENISLIK, YUKSEKLIK);
    }

    private void dusmanlariGuncelle(double dt) {
        for (Dusman d : dusmanlar) {
            d.guncelle(dt, oyuncu.getMerkezX());
            if (d.atesEtmeliMi()) dusmanAtesEt(d.getMerkezX(), d.getMerkezY(), 300, false);
            if (d.ekranDisindaMi(YUKSEKLIK)) {
                // Ekranın altından çıkan düşman artık oyuncuya görünmez hasar vermez.
                d.yokEt();
            }
        }
        dusmanlar.removeIf(d -> !d.aktifMi());
    }

    private void asteroitleriGuncelle(double dt) {
        asteroitler.forEach(a -> a.guncelle(dt));
        asteroitler.removeIf(a -> !a.aktifMi() || a.ekranDisindaMi(GENISLIK, YUKSEKLIK));
    }

    private void mermileriGuncelle(double dt) {
        for (Mermi m : oyuncuMermileri) {
            Double[] hedef = m.oyuncuMermisiMi() ? enYakinHedef(m) : new Double[]{null, null};
            m.guncelle(dt, hedef[0], hedef[1]);
        }
        dusmanMermileri.forEach(m -> m.guncelle(dt, oyuncu.getMerkezX(), oyuncu.getMerkezY()));
        oyuncuMermileri.removeIf(m -> !m.aktifMi() || m.ekranDisindaMi(GENISLIK, YUKSEKLIK));
        dusmanMermileri.removeIf(m -> !m.aktifMi() || m.ekranDisindaMi(GENISLIK, YUKSEKLIK));
    }

    private Double[] enYakinHedef(Mermi mermi) {
        double enIyi = Double.MAX_VALUE;
        Double hx = null;
        Double hy = null;
        Rectangle mb = mermi.sinirlariGetir();
        double mx = mb.getCenterX();
        double my = mb.getCenterY();
        for (Dusman d : dusmanlar) {
            double mesafe = Math.hypot(d.getMerkezX() - mx, d.getMerkezY() - my);
            if (mesafe < enIyi) {
                enIyi = mesafe;
                hx = d.getMerkezX();
                hy = d.getMerkezY();
            }
        }
        if (boss != null && boss.aktifMi()) {
            double mesafe = Math.hypot(boss.getMerkezX() - mx, boss.getMerkezY() - my);
            if (mesafe < enIyi) {
                hx = boss.getMerkezX();
                hy = boss.getMerkezY();
            }
        }
        return new Double[]{hx, hy};
    }

    private void guclendirmeleriGuncelle(double dt) {
        guclendirmeler.forEach(g -> g.guncelle(dt));
        guclendirmeler.removeIf(g -> !g.aktifMi() || g.ekranDisindaMi(YUKSEKLIK));
    }

    private void bossGuncelle(double dt) {
        boss.guncelle(dt, GENISLIK);
        if (!boss.saldiriHazirMi()) return;
        int tur = boss.getSaldiriSirasi();
        if (tur == 0) {
            for (int i = -3; i <= 3; i++) {
                double aci = Math.PI / 2 + i * 0.16;
                dusmanMermileri.add(new Mermi(boss.getMerkezX(), boss.getMerkezY(),
                        Math.cos(aci) * 270, Math.sin(aci) * 270, 1,
                        false, false, false, new Color(255, 80, 145)));
            }
        } else if (tur == 1) {
            dusmanAtesEt(boss.getMerkezX() - 65, boss.getMerkezY(), 390, true);
            dusmanAtesEt(boss.getMerkezX() + 65, boss.getMerkezY(), 390, true);
        } else {
            for (int i = 0; i < 12; i++) {
                double aci = Math.PI * 2 * i / 12.0;
                dusmanMermileri.add(new Mermi(boss.getMerkezX(), boss.getMerkezY(),
                        Math.cos(aci) * 220, Math.sin(aci) * 220, 1,
                        false, false, false, new Color(255, 140, 30)));
            }
        }
    }

    private void oyuncuAtesEt() {
        double x = oyuncu.getMerkezX();
        double y = oyuncu.getMermiY() - 10;
        int temelHasar = Math.max(1, (int) Math.round(oyuncu.getHasarCarpani()));
        SilahTuru silah = oyuncu.getSilahTuru();
        switch (silah) {
            case CIFTLI -> {
                oyuncuMermileri.add(new Mermi(x - 17, y, 0, -610, temelHasar,
                        true, false, false, Color.CYAN));
                oyuncuMermileri.add(new Mermi(x + 10, y, 0, -610, temelHasar,
                        true, false, false, Color.CYAN));
            }
            case UCLU -> {
                oyuncuMermileri.add(new Mermi(x - 3, y, 0, -620, temelHasar,
                        true, false, false, Color.YELLOW));
                oyuncuMermileri.add(new Mermi(x - 3, y, -150, -590, temelHasar,
                        true, false, false, Color.YELLOW));
                oyuncuMermileri.add(new Mermi(x - 3, y, 150, -590, temelHasar,
                        true, false, false, Color.YELLOW));
            }
            case LAZER -> oyuncuMermileri.add(new Mermi(x - 6, y - 65, 0, -720,
                    temelHasar * 2, true, true, false, new Color(210, 80, 255)));
            case TAKIP_FUZESI -> oyuncuMermileri.add(new Mermi(x - 6, y, 0, -430,
                    temelHasar * 3, true, false, true, new Color(255, 150, 35)));
            default -> oyuncuMermileri.add(new Mermi(x - 3, y, 0, -640,
                    temelHasar, true, false, false, new Color(60, 230, 255)));
        }
        double temel = switch (silah) {
            case LAZER -> 0.34;
            case TAKIP_FUZESI -> 0.48;
            default -> 0.19;
        };
        int atisSeviyesi = kayit.getGelistirme("atis");
        atisBekleme = temel * oyuncu.getAtisCarpani() * oyuncu.getAtisSuresiCarpani()
                * Math.max(0.55, 1.0 - atisSeviyesi * 0.075);
        ses.atis();
    }

    private void dusmanAtesEt(double x, double y, double hiz, boolean takip) {
        double dx = oyuncu.getMerkezX() - x;
        double dy = oyuncu.getMerkezY() - y;
        double uzunluk = Math.max(1, Math.hypot(dx, dy));
        dusmanMermileri.add(new Mermi(x, y, dx / uzunluk * hiz, dy / uzunluk * hiz,
                1, false, false, takip, new Color(255, 65, 100)));
    }

    private void carpismalariKontrolEt() {
        for (Mermi m : oyuncuMermileri) {
            if (!m.aktifMi()) continue;
            for (Dusman d : dusmanlar) {
                if (d.aktifMi() && m.sinirlariGetir().intersects(d.sinirlariGetir())) {
                    boolean oldu = d.hasarAl(m.getHasar());
                    carpmaParcacigi(m.sinirlariGetir().getCenterX(),
                            m.sinirlariGetir().getCenterY(), Color.CYAN, 7);
                    if (!m.deliciMi()) m.yokEt();
                    if (oldu) dusmanOlduruldu(d);
                    break;
                }
            }
            if (m.aktifMi() && boss != null && boss.aktifMi()
                    && m.sinirlariGetir().intersects(boss.sinirlariGetir())) {
                boolean oldu = boss.hasarAl(m.getHasar());
                carpmaParcacigi(m.sinirlariGetir().getCenterX(),
                        m.sinirlariGetir().getCenterY(), Color.MAGENTA, 8);
                if (!m.deliciMi()) m.yokEt();
                if (oldu) bossOlduruldu();
            }
            for (Asteroit a : asteroitler) {
                if (m.aktifMi() && a.aktifMi()
                        && m.sinirlariGetir().intersects(a.sinirlariGetir())) {
                    boolean oldu = a.hasarAl(m.getHasar());
                    carpmaParcacigi(a.getMerkezX(), a.getMerkezY(),
                            new Color(170, 130, 90), 8);
                    if (!m.deliciMi()) m.yokEt();
                    if (oldu) asteroitYokOldu(a);
                    break;
                }
            }
        }

        Rectangle oyuncuSiniri = oyuncu.sinirlariGetir();
        for (Mermi m : dusmanMermileri) {
            if (m.aktifMi() && m.sinirlariGetir().intersects(oyuncuSiniri)) {
                m.yokEt();
                oyuncuHasarAl(1, "DÜŞMAN MERMİSİ!");
            }
        }
        for (Dusman d : dusmanlar) {
            if (d.aktifMi() && d.sinirlariGetir().intersects(oyuncuSiniri)) {
                d.yokEt();
                oyuncuHasarAl(d.getTur() == DusmanTuru.ZIRHLI ? 2 : 1,
                        "DÜŞMAN GEMİSİNE ÇARPTIN!");
                patlama(d.getMerkezX(), d.getMerkezY(), new Color(255, 90, 80), 24);
            }
        }
        for (Asteroit a : asteroitler) {
            if (a.aktifMi() && a.sinirlariGetir().intersects(oyuncuSiniri)) {
                a.yokEt();
                oyuncuHasarAl(a.buyukMu() ? 2 : 1,
                        a.buyukMu() ? "BÜYÜK ASTEROİDE ÇARPTIN!" : "ASTEROİDE ÇARPTIN!");
                patlama(a.getMerkezX(), a.getMerkezY(), new Color(160, 120, 90), 24);
            }
        }
        if (boss != null && boss.aktifMi()
                && boss.sinirlariGetir().intersects(oyuncuSiniri)) {
            oyuncuHasarAl(2, "BOSS'A ÇARPTIN!");
        }
        for (Guclendirme g : guclendirmeler) {
            if (g.aktifMi() && g.sinirlariGetir().intersects(oyuncuSiniri)) {
                g.yokEt();
                guclendirmeUygula(g.getTur());
            }
        }
    }

    private void dusmanOlduruldu(Dusman d) {
        skor += (int) Math.round(d.getPuan() * zorluk.getSkorCarpani());
        coin += 1 + d.getPuan() / 180;
        oldurulenDusman++;
        xpEkle(d.getXpDegeri());
        patlama(d.getMerkezX(), d.getMerkezY(), Color.ORANGE, 22);
        ses.patlama();
        if (random.nextDouble() < 0.14) guclendirmeBirak(d.getMerkezX(), d.getMerkezY());
    }

    private void asteroitYokOldu(Asteroit a) {
        skor += a.buyukMu() ? 160 : 55;
        coin += a.buyukMu() ? 2 : 0;
        yokEdilenAsteroit++;
        xpEkle(a.getXpDegeri());
        patlama(a.getMerkezX(), a.getMerkezY(), new Color(175, 130, 90), 20);
        ses.patlama();
        if (a.buyukMu()) {
            for (int i = 0; i < 2; i++) {
                asteroitler.add(new Asteroit(a.getMerkezX() - 18, a.getMerkezY() - 18,
                        i == 0 ? -130 : 130, 190 + random.nextDouble() * 70,
                        Math.max(24, a.getBoyut() / 2)));
            }
        }
    }

    private void bossOlduruldu() {
        skor += (int) Math.round((2500 + bolum * 500) * zorluk.getSkorCarpani());
        coin += 35 + bolum * 5;
        xpEkle(250 + bolum * 55);
        patlama(boss.getMerkezX(), boss.getMerkezY(), new Color(255, 70, 220), 85);
        ses.patlama();
        boss = null;
        bossBekleniyor = false;

        if (oyunModu == OyunModu.HIKAYE) {
            if (bolum >= 5) {
                kayit.setHikayeTamamlandi(true);
                kayit.setAcikHikayeBolumu(5);
                oturumKazanciniKaydet();
                durum = OyunDurumu.ZAFER;
                ses.zafer();
            } else {
                sonrakiBolum = bolum + 1;
                kayit.setAcikHikayeBolumu(sonrakiBolum);
                durum = OyunDurumu.BOLUM_GECISI;
                ses.bolum();
            }
        } else {
            bolum++;
            bolumuHazirla(true);
            ses.bolum();
            bildirimGoster(aktifBolumBasligi());
        }
    }

    private void xpEkle(int miktar) {
        int eskiSeviye = seviyeSistemi.getSeviye();
        int atlanan = seviyeSistemi.xpEkle(miktar);
        if (atlanan <= 0) return;
        for (int yeniSeviye = eskiSeviye + 1;
                yeniSeviye <= seviyeSistemi.getSeviye(); yeniSeviye++) {
            String odul = oyuncu.seviyeOduluUygula(yeniSeviye);
            String silahMesaji = seviyeyeGoreSilahAc(yeniSeviye);
            ses.seviyeAtlama();
            bildirimGoster("SEVİYE " + yeniSeviye + "! "
                    + (silahMesaji.isEmpty() ? odul : silahMesaji));
        }
        kayit.setEnYuksekOyuncuSeviyesi(seviyeSistemi.getSeviye());
    }

    private String seviyeyeGoreSilahAc(int seviye) {
        if (seviye == 3) {
            oyuncu.kaliciSilahAc(SilahTuru.CIFTLI);
            return "Çift mermi kalıcı açıldı";
        }
        if (seviye == 5) {
            oyuncu.kaliciSilahAc(SilahTuru.UCLU);
            return "Üçlü mermi kalıcı açıldı";
        }
        if (seviye == 8) {
            oyuncu.kaliciSilahAc(SilahTuru.LAZER);
            return "Lazer kalıcı açıldı";
        }
        if (seviye == 10) {
            oyuncu.kaliciSilahAc(SilahTuru.TAKIP_FUZESI);
            return "Takip füzesi kalıcı açıldı";
        }
        return "";
    }

    private void oyuncuHasarAl(int hasar) {
        oyuncuHasarAl(hasar, "HASAR ALDIN!");
    }

    private void oyuncuHasarAl(int hasar, String kaynak) {
        int gercekHasar = Math.max(1,
                (int) Math.round(hasar * zorluk.getHasarCarpani()));
        if (oyuncu.hasarAl(gercekHasar)) {
            ekranSarsintisi = 12;
            ses.hasar();
            patlama(oyuncu.getMerkezX(), oyuncu.getMerkezY(), Color.RED, 15);
            bildirimGoster(kaynak);
        }
    }

    private void guclendirmeBirak(double x, double y) {
        GuclendirmeTuru[] turler = GuclendirmeTuru.values();
        GuclendirmeTuru tur = turler[random.nextInt(turler.length)];
        guclendirmeler.add(new Guclendirme(x - 18, y - 18, tur));
    }

    private void guclendirmeUygula(GuclendirmeTuru tur) {
        switch (tur) {
            case CIFT_MERMI -> oyuncu.silahVer(SilahTuru.CIFTLI, 13);
            case UCLU_MERMI -> oyuncu.silahVer(SilahTuru.UCLU, 11);
            case HIZLI_ATES -> oyuncu.hizliAtesVer(10);
            case KALKAN -> oyuncu.kalkanVer(6 + kayit.getGelistirme("kalkan") * 1.2);
            case CAN -> oyuncu.canEkle(1);
            case LAZER -> oyuncu.silahVer(SilahTuru.LAZER, 9);
            case TAKIP_FUZESI -> oyuncu.silahVer(SilahTuru.TAKIP_FUZESI, 10);
            case BOMBA -> oyuncu.bombaEkle();
        }
        ses.guclendirme();
        bildirimGoster(guclendirmeAdi(tur));
    }

    private String guclendirmeAdi(GuclendirmeTuru tur) {
        return switch (tur) {
            case CIFT_MERMI -> "Çift Mermi";
            case UCLU_MERMI -> "Üçlü Mermi";
            case HIZLI_ATES -> "Hızlı Ateş";
            case KALKAN -> "Kalkan";
            case CAN -> "+1 Can";
            case LAZER -> "Lazer";
            case TAKIP_FUZESI -> "Takip Füzesi";
            case BOMBA -> "+1 Bomba";
        };
    }

    private void bombaKullan() {
        if (durum != OyunDurumu.OYNUYOR || !oyuncu.bombaKullan()) return;
        for (Dusman d : dusmanlar) {
            if (d.aktifMi()) {
                skor += d.getPuan() / 2;
                coin += 1;
                oldurulenDusman++;
                xpEkle(Math.max(3, d.getXpDegeri() / 2));
                d.yokEt();
                patlama(d.getMerkezX(), d.getMerkezY(), Color.ORANGE, 12);
            }
        }
        for (Asteroit a : asteroitler) {
            if (a.aktifMi()) {
                yokEdilenAsteroit++;
                xpEkle(Math.max(2, a.getXpDegeri() / 2));
                a.yokEt();
            }
        }
        dusmanMermileri.clear();
        if (boss != null) boss.hasarAl(25);
        ekranSarsintisi = 22;
        ses.patlama();
        bildirimGoster("MEGA BOMBA!");
    }

    private void patlama(double x, double y, Color renk, int adet) {
        for (int i = 0; i < adet; i++) {
            double aci = random.nextDouble() * Math.PI * 2;
            double hiz = 70 + random.nextDouble() * 260;
            parcaciklar.add(new Parcacik(x, y, Math.cos(aci) * hiz,
                    Math.sin(aci) * hiz, 0.35 + random.nextDouble() * 0.65,
                    5 + random.nextInt(10), i % 3 == 0 ? Color.YELLOW : renk));
        }
    }

    private void carpmaParcacigi(double x, double y, Color renk, int adet) {
        for (int i = 0; i < adet; i++) {
            double aci = random.nextDouble() * Math.PI * 2;
            double hiz = 60 + random.nextDouble() * 120;
            parcaciklar.add(new Parcacik(x, y, Math.cos(aci) * hiz,
                    Math.sin(aci) * hiz, 0.18 + random.nextDouble() * 0.25,
                    3 + random.nextInt(5), renk));
        }
    }

    private void oyunBitti() {
        durum = OyunDurumu.OYUN_BITTI;
        oturumKazanciniKaydet();
    }

    private void oturumKazanciniKaydet() {
        if (skorKaydedildi) return;
        kayit.skorEkle(skor);
        kayit.setCoin(kayit.getCoin() + coin);
        kayit.setEnYuksekOyuncuSeviyesi(seviyeSistemi.getSeviye());
        skorKaydedildi = true;
    }

    private void sonrakiHikayeBolumunuBaslat() {
        bolum = sonrakiBolum;
        bolumuHazirla(true);
        durum = OyunDurumu.OYNUYOR;
        bildirimGoster(aktifBolumBasligi());
        requestFocusInWindow();
    }

    private void yenidenBaslat() {
        int baslangic = oyunModu == OyunModu.HIKAYE ? bolum : 1;
        oyunBaslat(oyunModu, baslangic);
    }

    private BolumTemasi temaGetir() {
        if (oyunModu == OyunModu.HIKAYE) return HikayeBolumu.numaradan(bolum).getTema();
        BolumTemasi[] temalar = {
                BolumTemasi.UZAY_BOSLUGU,
                BolumTemasi.ASTEROIT_ALANI,
                BolumTemasi.DUSMAN_USSU,
                BolumTemasi.KARA_DELIK,
                BolumTemasi.FINAL
        };
        return temalar[(bolum - 1) % temalar.length];
    }

    private String aktifBolumBasligi() {
        if (oyunModu == OyunModu.HIKAYE) {
            return "Bölüm " + bolum + ": " + hikayeBolumu.getAd();
        }
        return "Endless Dalga " + bolum + ": " + temaGetir().getAd();
    }

    private String hedefMetni() {
        if (oyunModu == OyunModu.HIKAYE) {
            return hikayeBolumu.hedefMetni(oldurulenDusman, yokEdilenAsteroit, bolumSuresi);
        }
        return "Düşman " + Math.min(oldurulenDusman, hedefDusman) + "/" + hedefDusman;
    }

    private void bildirimGoster(String mesaj) {
        bildirim = mesaj;
        bildirimSuresi = 2.6;
    }

    private boolean tus(int keyCode) { return basiliTuslar.contains(keyCode); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        if (ekranSarsintisi > 0) {
            g2.translate((random.nextDouble() - 0.5) * ekranSarsintisi,
                    (random.nextDouble() - 0.5) * ekranSarsintisi);
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
        g2.dispose();
    }

    private void arkaPlanCiz(Graphics2D g) {
        Color ust;
        Color alt;
        BolumTemasi tema = oyunEkraniDurumuMu() ? temaGetir() : BolumTemasi.UZAY_BOSLUGU;
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
        for (int y = 0; y < YUKSEKLIK; y += 4) {
            double t = y / (double) YUKSEKLIK;
            int r = (int) (ust.getRed() * (1 - t) + alt.getRed() * t);
            int gr = (int) (ust.getGreen() * (1 - t) + alt.getGreen() * t);
            int b = (int) (ust.getBlue() * (1 - t) + alt.getBlue() * t);
            g.setColor(new Color(r, gr, b));
            g.fillRect(0, y, GENISLIK, 4);
        }
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

    private boolean oyunEkraniDurumuMu() {
        return durum == OyunDurumu.OYNUYOR
                || durum == OyunDurumu.DURAKLATILDI
                || durum == OyunDurumu.OYUN_BITTI
                || durum == OyunDurumu.BOLUM_GECISI
                || durum == OyunDurumu.ZAFER;
    }

    private void oyunCiz(Graphics2D g, boolean duraklatildi) {
        asteroitler.forEach(a -> a.ciz(g));
        guclendirmeler.forEach(p -> p.ciz(g));
        dusmanlar.forEach(d -> d.ciz(g));
        if (boss != null) boss.ciz(g);
        oyuncuMermileri.forEach(m -> m.ciz(g));
        dusmanMermileri.forEach(m -> m.ciz(g));
        if (oyuncu != null) oyuncu.ciz(g);
        parcaciklar.forEach(p -> p.ciz(g));
        hudCiz(g);
        if (duraklatildi) {
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(0, 0, GENISLIK, YUKSEKLIK);
            ortaliYazi(g, "DURAKLATILDI", 360, 44, Color.WHITE);
            ortaliYazi(g, "Devam etmek için P", 410, 22, Color.LIGHT_GRAY);
            ortaliYazi(g, "Ana menü için ESC", 444, 18, Color.LIGHT_GRAY);
        }
    }

    private void hudCiz(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 155));
        g.fillRoundRect(12, 10, 270, 102, 18, 18);
        g.fillRoundRect(GENISLIK - 322, 10, 310, 102, 18, 18);
        g.setFont(new Font("Arial", Font.BOLD, 17));
        g.setColor(Color.WHITE);
        g.drawString("Skor: " + skor, 26, 34);
        g.drawString("Coin: " + coin, 26, 58);
        g.drawString(oyunModu == OyunModu.HIKAYE ? "Bölüm: " + bolum + "/5"
                : "Dalga: " + bolum, 26, 82);
        g.drawString("Oyuncu seviyesi: " + seviyeSistemi.getSeviye(), 26, 105);

        g.setFont(new Font("Arial", Font.BOLD, 15));
        g.drawString(hedefMetni(), GENISLIK - 306, 34);
        g.drawString("Silah: " + silahAdi(oyuncu.getSilahTuru()), GENISLIK - 306, 58);
        g.drawString("Kalıcı: " + silahAdi(oyuncu.getKaliciSilahTuru()), GENISLIK - 306, 80);
        g.drawString("Bomba: " + oyuncu.getBombaSayisi(), GENISLIK - 306, 102);

        xpCubuguCiz(g);
        for (int i = 0; i < oyuncu.getMaksimumCan(); i++) {
            g.setColor(i < oyuncu.getCan() ? new Color(255, 65, 90) : new Color(80, 45, 55));
            kalpCiz(g, 20 + i * 26, 145, 19);
        }
        if (bildirimSuresi > 0) ortaliYazi(g, bildirim, 190, 22, Color.YELLOW);
    }

    private void xpCubuguCiz(Graphics2D g) {
        int x = 146;
        int y = 121;
        int w = 508;
        int h = 16;
        g.setColor(new Color(10, 15, 35, 210));
        g.fillRoundRect(x, y, w, h, 9, 9);
        g.setColor(new Color(65, 190, 255));
        g.fillRoundRect(x, y, (int) (w * seviyeSistemi.ilerlemeOraniGetir()), h, 9, 9);
        g.setColor(Color.WHITE);
        g.drawRoundRect(x, y, w, h, 9, 9);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        String metin = "XP " + seviyeSistemi.getMevcutXp() + "/" + seviyeSistemi.gerekenXpGetir();
        int sw = g.getFontMetrics().stringWidth(metin);
        g.drawString(metin, GENISLIK / 2 - sw / 2, y + 13);
    }

    private String silahAdi(SilahTuru tur) {
        return switch (tur) {
            case TEKLI -> "Tekli";
            case CIFTLI -> "Çiftli";
            case UCLU -> "Üçlü";
            case LAZER -> "Lazer";
            case TAKIP_FUZESI -> "Füze";
        };
    }

    private void kalpCiz(Graphics2D g, int x, int y, int b) {
        g.fillOval(x, y, b / 2 + 2, b / 2 + 2);
        g.fillOval(x + b / 2 - 2, y, b / 2 + 2, b / 2 + 2);
        int[] xs = {x, x + b, x + b / 2};
        int[] ys = {y + b / 3, y + b / 3, y + b};
        g.fillPolygon(xs, ys, 3);
    }

    private void anaMenuCiz(Graphics2D g) {
        ortaliYazi(g, "UZAY SAVAŞI", 96, 52, new Color(80, 230, 255));
        ortaliYazi(g, "HİKÂYE + ENDLESS + XP SİSTEMİ", 132, 20,
                new Color(255, 200, 60));
        g.setFont(new Font("Arial", Font.BOLD, 19));
        for (int i = 0; i < menuSecenekleri.length; i++) {
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
                default -> menuSecenekleri[i];
            };
            int sw = g.getFontMetrics().stringWidth(metin);
            g.drawString(metin, GENISLIK / 2 - sw / 2, y + 7);
        }
        g.setFont(new Font("Arial", Font.PLAIN, 15));
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("↑ ↓ / Fare: Seç   ENTER / Tıkla: Onayla", 20, YUKSEKLIK - 25);
        g.drawString("Coin: " + kayit.getCoin() + "   En yüksek seviye: "
                + kayit.getEnYuksekOyuncuSeviyesi(), GENISLIK - 300, YUKSEKLIK - 25);
        if (kayit.hikayeTamamlandiMi()) {
            ortaliYazi(g, "Hikâye tamamlandı! Bölümleri yeniden oynayabilirsin.",
                    760, 15, new Color(120, 255, 150));
        }
    }

    private Rectangle menuButonAlani(int index) {
        return new Rectangle(GENISLIK / 2 - 235, 195 + index * 52 - 29, 470, 39);
    }

    private void hikayeBolumSecimiCiz(Graphics2D g) {
        ortaliYazi(g, "HİKÂYE BÖLÜMLERİ", 85, 40, Color.CYAN);
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
            g.setFont(new Font("Arial", Font.BOLD, 21));
            g.drawString("Bölüm " + hb.getNumara() + " - " + hb.getAd(),
                    alan.x + 20, alan.y + 31);
            g.setFont(new Font("Arial", Font.PLAIN, 15));
            g.setColor(acik ? Color.WHITE : Color.GRAY);
            g.drawString(acik ? hb.getAciklama() : "KİLİTLİ - Önceki bölümü tamamla",
                    alan.x + 20, alan.y + 58);
        }
        ortaliYazi(g, "↑ ↓ seç   ENTER başlat   ESC geri", 760, 18, Color.LIGHT_GRAY);
    }

    private Rectangle bolumSecimAlani(int index) {
        return new Rectangle(75, 135 + index * 112, 650, 82);
    }

    private void gemiSecimiCiz(Graphics2D g) {
        ortaliYazi(g, "GEMİ SEÇİMİ", 110, 42, Color.CYAN);
        GemiTuru gemi = GemiTuru.values()[gemiIndex];
        g.setColor(new Color(0, 0, 0, 155));
        g.fillRoundRect(125, 175, 550, 430, 28, 28);
        Oyuncu onizleme = new Oyuncu(GENISLIK / 2.0 - 28, 235, gemi, 0, 0, 0, 0);
        onizleme.ciz(g);
        ortaliYazi(g, gemi.getAd(), 390, 30, gemi.getRenk());
        ortaliYazi(g, gemi.getAciklama(), 430, 19, Color.WHITE);
        ozellikCubuguCiz(g, "Hız", 205, 480, gemi.getHizCarpani() / 1.5);
        ozellikCubuguCiz(g, "Dayanıklılık", 205, 520, gemi.getCanCarpani() / 1.5);
        ozellikCubuguCiz(g, "Hasar", 205, 560, gemi.getHasarCarpani() / 1.5);
        ortaliYazi(g, "← → gemi değiştir   ENTER seç   ESC geri", 675, 18, Color.LIGHT_GRAY);
    }

    private void ozellikCubuguCiz(Graphics2D g, String ad, int x, int y, double oran) {
        g.setFont(new Font("Arial", Font.BOLD, 17));
        g.setColor(Color.WHITE);
        g.drawString(ad, x, y);
        g.setColor(new Color(45, 45, 65));
        g.fillRoundRect(x + 145, y - 17, 270, 18, 10, 10);
        g.setColor(new Color(50, 220, 255));
        g.fillRoundRect(x + 145, y - 17, (int) (270 * Math.min(1, oran)), 18, 10, 10);
    }

    private void gelistirmelerCiz(Graphics2D g) {
        ortaliYazi(g, "GELİŞTİRMELER", 90, 40, Color.YELLOW);
        ortaliYazi(g, "Coin: " + kayit.getCoin(), 128, 21, Color.WHITE);
        for (int i = 0; i < gelistirmeAdlari.length; i++) {
            int y = 205 + i * 82;
            int seviye = gelistirmeSeviyesi(i);
            int fiyat = gelistirmeFiyati(seviye);
            boolean secili = i == gelistirmeIndex;
            g.setColor(secili ? new Color(45, 110, 160, 210)
                    : new Color(10, 20, 45, 185));
            g.fillRoundRect(105, y - 42, 590, 64, 18, 18);
            g.setColor(secili ? Color.YELLOW : Color.CYAN);
            g.drawRoundRect(105, y - 42, 590, 64, 18, 18);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString(gelistirmeAdlari[i], 130, y - 9);
            g.setFont(new Font("Arial", Font.PLAIN, 17));
            g.setColor(Color.WHITE);
            g.drawString("Seviye: " + seviye + "/5", 420, y - 9);
            g.drawString(seviye >= 5 ? "MAKSİMUM" : "Fiyat: " + fiyat, 550, y - 9);
        }
        ortaliYazi(g, "↑ ↓ seç   ENTER satın al   ESC geri", 735, 18, Color.LIGHT_GRAY);
    }

    private int gelistirmeSeviyesi(int index) {
        return kayit.getGelistirme(gelistirmeAnahtari(index));
    }

    private String gelistirmeAnahtari(int index) {
        return switch (index) {
            case 0 -> "hiz";
            case 1 -> "atis";
            case 2 -> "hasar";
            case 3 -> "can";
            case 4 -> "kalkan";
            default -> "bomba";
        };
    }

    private int gelistirmeFiyati(int seviye) {
        return 60 * (seviye + 1) * (seviye + 1);
    }

    private void gelistirmeSatinAl() {
        int seviye = gelistirmeSeviyesi(gelistirmeIndex);
        if (seviye >= 5) {
            bildirimGoster("Bu geliştirme maksimum seviyede");
            return;
        }
        int fiyat = gelistirmeFiyati(seviye);
        if (kayit.getCoin() < fiyat) {
            bildirimGoster("Yeterli coin yok");
            ses.hasar();
            return;
        }
        kayit.setCoin(kayit.getCoin() - fiyat);
        kayit.setGelistirme(gelistirmeAnahtari(gelistirmeIndex), seviye + 1);
        ses.guclendirme();
        bildirimGoster("Geliştirme satın alındı");
    }

    private void yuksekSkorCiz(Graphics2D g) {
        ortaliYazi(g, "YÜKSEK SKORLAR", 125, 42, Color.YELLOW);
        List<Integer> skorlar = kayit.yuksekSkorlar();
        if (skorlar.isEmpty()) ortaliYazi(g, "Henüz kayıtlı skor yok", 330, 24, Color.LIGHT_GRAY);
        for (int i = 0; i < skorlar.size(); i++) {
            ortaliYazi(g, (i + 1) + ".   " + skorlar.get(i), 245 + i * 65, 30,
                    i == 0 ? Color.YELLOW : Color.WHITE);
        }
        ortaliYazi(g, "En yüksek oyuncu seviyesi: " + kayit.getEnYuksekOyuncuSeviyesi(),
                610, 21, Color.CYAN);
        ortaliYazi(g, "ESC ile geri dön", 700, 18, Color.LIGHT_GRAY);
    }

    private void kontrollerCiz(Graphics2D g) {
        ortaliYazi(g, "KONTROLLER VE SEVİYE SİSTEMİ", 88, 36, Color.CYAN);
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
        g.setFont(new Font("Monospaced", Font.BOLD, 18));
        g.setColor(Color.WHITE);
        for (int i = 0; i < satirlar.length; i++) g.drawString(satirlar[i], 90, 175 + i * 49);
        ortaliYazi(g, "ESC ile geri dön", 740, 18, Color.LIGHT_GRAY);
    }

    private void bolumGecisiCiz(Graphics2D g) {
        oyunCiz(g, false);
        g.setColor(new Color(0, 0, 0, 218));
        g.fillRect(0, 0, GENISLIK, YUKSEKLIK);
        ortaliYazi(g, "BÖLÜM " + bolum + " TAMAMLANDI", 255, 42, Color.YELLOW);
        ortaliYazi(g, HikayeBolumu.numaradan(bolum).getAd(), 305, 25, Color.CYAN);
        ortaliYazi(g, "Skor: " + skor + "   Coin: " + coin
                + "   Oyuncu seviyesi: " + seviyeSistemi.getSeviye(),
                370, 21, Color.WHITE);
        HikayeBolumu sonraki = HikayeBolumu.numaradan(sonrakiBolum);
        ortaliYazi(g, "Sıradaki: Bölüm " + sonrakiBolum + " - " + sonraki.getAd(),
                440, 25, new Color(120, 230, 255));
        ortaliYazi(g, sonraki.getAciklama(), 480, 17, Color.LIGHT_GRAY);
        ortaliYazi(g, "Devam etmek için ENTER", 560, 23, Color.WHITE);
        ortaliYazi(g, "Kaydedip menüye dönmek için ESC", 600, 17, Color.LIGHT_GRAY);
    }

    private void oyunBittiCiz(Graphics2D g) {
        oyunCiz(g, false);
        g.setColor(new Color(0, 0, 0, 205));
        g.fillRect(0, 0, GENISLIK, YUKSEKLIK);
        ortaliYazi(g, "OYUN BİTTİ", 275, 56, new Color(255, 70, 100));
        ortaliYazi(g, oyunModu.getAd(), 325, 24, Color.CYAN);
        ortaliYazi(g, "Skor: " + skor, 370, 32, Color.WHITE);
        ortaliYazi(g, "Kazanılan coin: " + coin, 414, 24, Color.YELLOW);
        ortaliYazi(g, "Oyuncu seviyesi: " + seviyeSistemi.getSeviye(), 453, 22, Color.WHITE);
        ortaliYazi(g, oyunModu == OyunModu.HIKAYE
                ? "Ulaşılan hikâye bölümü: " + bolum
                : "Ulaşılan endless dalgası: " + bolum, 490, 20, Color.LIGHT_GRAY);
        ortaliYazi(g, "R: Bölümü yeniden başlat", 555, 22, Color.CYAN);
        ortaliYazi(g, "ESC: Ana menü", 595, 20, Color.LIGHT_GRAY);
    }

    private void zaferCiz(Graphics2D g) {
        oyunCiz(g, false);
        g.setColor(new Color(0, 0, 0, 215));
        g.fillRect(0, 0, GENISLIK, YUKSEKLIK);
        ortaliYazi(g, "GALAKSİ KURTARILDI!", 245, 48, Color.YELLOW);
        ortaliYazi(g, "HİKÂYE MODUNU TAMAMLADIN", 300, 27, Color.CYAN);
        ortaliYazi(g, "Final skor: " + skor, 370, 30, Color.WHITE);
        ortaliYazi(g, "Toplanan coin: " + coin, 415, 23, Color.YELLOW);
        ortaliYazi(g, "Ulaşılan oyuncu seviyesi: " + seviyeSistemi.getSeviye(),
                455, 22, Color.WHITE);
        ortaliYazi(g, "Silah: " + silahAdi(oyuncu.getKaliciSilahTuru()),
                492, 20, Color.LIGHT_GRAY);
        ortaliYazi(g, "R: Hikâyeyi baştan oyna", 565, 22, Color.CYAN);
        ortaliYazi(g, "ESC: Ana menü", 605, 20, Color.LIGHT_GRAY);
    }

    private void ortaliYazi(Graphics2D g, String metin, int y, int boyut, Color renk) {
        g.setFont(new Font("Arial", Font.BOLD, boyut));
        g.setColor(renk);
        int sw = g.getFontMetrics().stringWidth(metin);
        g.drawString(metin, GENISLIK / 2 - sw / 2, y);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        basiliTuslar.add(code);
        if (durum == OyunDurumu.ANA_MENU) {
            if (code == KeyEvent.VK_UP) {
                menuIndex = (menuIndex - 1 + menuSecenekleri.length) % menuSecenekleri.length;
            } else if (code == KeyEvent.VK_DOWN) {
                menuIndex = (menuIndex + 1) % menuSecenekleri.length;
            } else if (code == KeyEvent.VK_ENTER) {
                menuSeciminiCalistir();
            }
        } else if (durum == OyunDurumu.HIKAYE_BOLUM_SECIMI) {
            if (code == KeyEvent.VK_UP) {
                hikayeSecimIndex = (hikayeSecimIndex - 1 + HikayeBolumu.values().length)
                        % HikayeBolumu.values().length;
            } else if (code == KeyEvent.VK_DOWN) {
                hikayeSecimIndex = (hikayeSecimIndex + 1) % HikayeBolumu.values().length;
            } else if (code == KeyEvent.VK_ENTER) {
                seciliHikayeBolumunuBaslat();
            } else if (code == KeyEvent.VK_ESCAPE) {
                durum = OyunDurumu.ANA_MENU;
            }
        } else if (durum == OyunDurumu.GEMI_SECIMI) {
            if (code == KeyEvent.VK_LEFT) {
                gemiIndex = (gemiIndex - 1 + GemiTuru.values().length) % GemiTuru.values().length;
            } else if (code == KeyEvent.VK_RIGHT) {
                gemiIndex = (gemiIndex + 1) % GemiTuru.values().length;
            } else if (code == KeyEvent.VK_ENTER) {
                seciliGemi = GemiTuru.values()[gemiIndex];
                kayit.setSeciliGemi(gemiIndex);
                ses.menu();
                durum = OyunDurumu.ANA_MENU;
            } else if (code == KeyEvent.VK_ESCAPE) {
                durum = OyunDurumu.ANA_MENU;
            }
        } else if (durum == OyunDurumu.GELISTIRMELER) {
            if (code == KeyEvent.VK_UP) {
                gelistirmeIndex = (gelistirmeIndex - 1 + gelistirmeAdlari.length)
                        % gelistirmeAdlari.length;
            } else if (code == KeyEvent.VK_DOWN) {
                gelistirmeIndex = (gelistirmeIndex + 1) % gelistirmeAdlari.length;
            } else if (code == KeyEvent.VK_ENTER) {
                gelistirmeSatinAl();
            } else if (code == KeyEvent.VK_ESCAPE) {
                durum = OyunDurumu.ANA_MENU;
            }
        } else if (durum == OyunDurumu.YUKSEK_SKORLAR
                || durum == OyunDurumu.KONTROLLER) {
            if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_ENTER) {
                durum = OyunDurumu.ANA_MENU;
            }
        } else if (durum == OyunDurumu.OYNUYOR) {
            if (code == KeyEvent.VK_B) {
                bombaKullan();
            } else if (code == KeyEvent.VK_P) {
                durum = OyunDurumu.DURAKLATILDI;
            } else if (code == KeyEvent.VK_M) {
                sesiDegistir();
            } else if (code == KeyEvent.VK_ESCAPE) {
                oturumKazanciniKaydet();
                durum = OyunDurumu.ANA_MENU;
            }
        } else if (durum == OyunDurumu.DURAKLATILDI) {
            if (code == KeyEvent.VK_P) {
                durum = OyunDurumu.OYNUYOR;
            } else if (code == KeyEvent.VK_ESCAPE) {
                oturumKazanciniKaydet();
                durum = OyunDurumu.ANA_MENU;
            }
        } else if (durum == OyunDurumu.BOLUM_GECISI) {
            if (code == KeyEvent.VK_ENTER) {
                sonrakiHikayeBolumunuBaslat();
            } else if (code == KeyEvent.VK_ESCAPE) {
                oturumKazanciniKaydet();
                durum = OyunDurumu.ANA_MENU;
            }
        } else if (durum == OyunDurumu.OYUN_BITTI) {
            if (code == KeyEvent.VK_R) {
                yenidenBaslat();
            } else if (code == KeyEvent.VK_ESCAPE) {
                durum = OyunDurumu.ANA_MENU;
            }
        } else if (durum == OyunDurumu.ZAFER) {
            if (code == KeyEvent.VK_R) {
                oyunBaslat(OyunModu.HIKAYE, 1);
            } else if (code == KeyEvent.VK_ESCAPE) {
                durum = OyunDurumu.ANA_MENU;
            }
        }
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
            bildirimGoster("Bu bölüm henüz kilitli");
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
            for (int i = 0; i < menuSecenekleri.length; i++) {
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
