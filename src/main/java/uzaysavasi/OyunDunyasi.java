package uzaysavasi;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.IntPredicate;

/** Tek bir oyun oturumu: gemiler, mermiler, çarpışmalar, skor ve HUD. */
final class OyunDunyasi {

    /** Oturumda olan ve ekran akışını değiştiren olayları panele bildirir. */
    interface Dinleyici {
        void bildirim(String mesaj);
        void oyunBitti();
        void bolumTamamlandi();
        void zafer();
    }

    private static final int GENISLIK = OyunPaneli.GENISLIK;
    private static final int YUKSEKLIK = OyunPaneli.YUKSEKLIK;

    private final Random random = new Random();
    private final KayitYoneticisi kayit;
    private final SesYoneticisi ses;
    private final Dinleyici dinleyici;
    private final OyunModu oyunModu;
    private final Zorluk zorluk;

    private final List<Mermi> oyuncuMermileri = new ArrayList<>();
    private final List<Mermi> dusmanMermileri = new ArrayList<>();
    private final List<Dusman> dusmanlar = new ArrayList<>();
    private final List<Asteroit> asteroitler = new ArrayList<>();
    private final List<Guclendirme> guclendirmeler = new ArrayList<>();
    private final List<Parcacik> parcaciklar = new ArrayList<>();

    private final SeviyeSistemi seviyeSistemi = new SeviyeSistemi();
    private final Oyuncu oyuncu;
    private HikayeBolumu hikayeBolumu = HikayeBolumu.ILK_TEMAS;
    private Boss boss;

    private double bolumSuresi;
    private double dusmanDogmaSayaci;
    private double asteroitDogmaSayaci;
    private double atisBekleme;
    private double ekranSarsintisi;
    private int skor;
    private int coin;
    private int bolum;
    private int sonrakiBolum;
    private int oldurulenDusman;
    private int yokEdilenAsteroit;
    private int hedefDusman;
    private boolean bossBekleniyor;
    private boolean skorKaydedildi;

    OyunDunyasi(OyunModu mod, int baslangicBolumu, GemiTuru gemi, Zorluk zorluk,
            KayitYoneticisi kayit, SesYoneticisi ses, Dinleyici dinleyici) {
        this.oyunModu = mod;
        this.zorluk = zorluk;
        this.kayit = kayit;
        this.ses = ses;
        this.dinleyici = dinleyici;
        bolum = mod == OyunModu.HIKAYE
                ? Math.max(1, Math.min(5, baslangicBolumu))
                : 1;
        oyuncu = new Oyuncu(GENISLIK / 2.0 - 28, YUKSEKLIK - 120, gemi,
                kayit.getGelistirme("hiz"), kayit.getGelistirme("can"),
                kayit.getGelistirme("hasar"), kayit.getGelistirme("bomba"));
        bolumuHazirla(false);
        dinleyici.bildirim(aktifBolumBasligi());
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
        if (oyuncuyuKoru) oyuncu.canEkle(1);
    }

    void guncelle(double dt, IntPredicate tus) {
        bolumSuresi += dt;
        boolean sol = tus.test(KeyEvent.VK_LEFT) || tus.test(KeyEvent.VK_A);
        boolean sag = tus.test(KeyEvent.VK_RIGHT) || tus.test(KeyEvent.VK_D);
        boolean yukari = tus.test(KeyEvent.VK_UP) || tus.test(KeyEvent.VK_W);
        boolean asagi = tus.test(KeyEvent.VK_DOWN) || tus.test(KeyEvent.VK_S);
        oyuncu.guncelle(sol, sag, yukari, asagi, dt, GENISLIK, YUKSEKLIK);
        karaDelikKuvvetiUygula(dt);
        atisBekleme = Math.max(0, atisBekleme - dt);

        if (tus.test(KeyEvent.VK_SPACE) && atisBekleme <= 0) oyuncuAtesEt();

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

        if (!oyuncu.hayattaMi()) {
            kazanciKaydet();
            dinleyici.oyunBitti();
        }
    }

    /** Oyun dursa da (duraklatma, oyun sonu) patlama ve sarsıntı sönmeye devam eder. */
    void efektleriGuncelle(double dt) {
        parcaciklar.forEach(p -> p.guncelle(dt));
        parcaciklar.removeIf(p -> !p.aktifMi());
        ekranSarsintisi = Math.max(0, ekranSarsintisi - dt * 18);
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

    /** Temaya özgü düşman türünü seçer; null dönerse Dusman kendi rastgele türünü seçer. */
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
        dinleyici.bildirim(oyunModu == OyunModu.HIKAYE && bolum == 5
                ? "FİNAL BOSS YAKLAŞIYOR!" : "BOSS YAKLAŞIYOR!");
    }

    private void karaDelikKuvvetiUygula(double dt) {
        if (temaGetir() != BolumTemasi.KARA_DELIK) return;
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
            // Ekranın altından çıkan düşman oyuncuya hasar vermeden kaybolur.
            if (d.ekranDisindaMi(YUKSEKLIK)) d.yokEt();
        }
        dusmanlar.removeIf(d -> !d.aktifMi());
    }

    private void asteroitleriGuncelle(double dt) {
        asteroitler.forEach(a -> a.guncelle(dt));
        asteroitler.removeIf(a -> !a.aktifMi() || a.ekranDisindaMi(GENISLIK, YUKSEKLIK));
    }

    private void mermileriGuncelle(double dt) {
        for (Mermi m : oyuncuMermileri) {
            Double[] hedef = enYakinHedef(m);
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
                kazanciKaydet();
                ses.zafer();
                dinleyici.zafer();
            } else {
                sonrakiBolum = bolum + 1;
                kayit.setAcikHikayeBolumu(sonrakiBolum);
                ses.bolum();
                dinleyici.bolumTamamlandi();
            }
        } else {
            bolum++;
            bolumuHazirla(true);
            ses.bolum();
            dinleyici.bildirim(aktifBolumBasligi());
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
            dinleyici.bildirim("SEVİYE " + yeniSeviye + "! "
                    + (silahMesaji.isEmpty() ? odul : silahMesaji));
        }
        kayit.setEnYuksekOyuncuSeviyesi(seviyeSistemi.getSeviye());
    }

    private String seviyeyeGoreSilahAc(int seviye) {
        SilahTuru tur = switch (seviye) {
            case 3 -> SilahTuru.CIFTLI;
            case 5 -> SilahTuru.UCLU;
            case 8 -> SilahTuru.LAZER;
            case 10 -> SilahTuru.TAKIP_FUZESI;
            default -> null;
        };
        if (tur == null) return "";
        oyuncu.kaliciSilahAc(tur);
        return switch (tur) {
            case CIFTLI -> "Çift mermi kalıcı açıldı";
            case UCLU -> "Üçlü mermi kalıcı açıldı";
            case LAZER -> "Lazer kalıcı açıldı";
            default -> "Takip füzesi kalıcı açıldı";
        };
    }

    private void oyuncuHasarAl(int hasar, String kaynak) {
        int gercekHasar = Math.max(1,
                (int) Math.round(hasar * zorluk.getHasarCarpani()));
        if (oyuncu.hasarAl(gercekHasar)) {
            ekranSarsintisi = 12;
            ses.hasar();
            patlama(oyuncu.getMerkezX(), oyuncu.getMerkezY(), Color.RED, 15);
            dinleyici.bildirim(kaynak);
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
        dinleyici.bildirim(tur.getAd());
    }

    void bombaKullan() {
        if (!oyuncu.bombaKullan()) return;
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
        dinleyici.bildirim("MEGA BOMBA!");
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

    /** Skor, coin ve seviyeyi bir oturumda yalnızca bir kez kaydeder. */
    void kazanciKaydet() {
        if (skorKaydedildi) return;
        kayit.skorEkle(skor);
        kayit.setCoin(kayit.getCoin() + coin);
        kayit.setEnYuksekOyuncuSeviyesi(seviyeSistemi.getSeviye());
        skorKaydedildi = true;
    }

    void sonrakiHikayeBolumunuBaslat() {
        bolum = sonrakiBolum;
        bolumuHazirla(true);
        dinleyici.bildirim(aktifBolumBasligi());
    }

    BolumTemasi temaGetir() {
        if (oyunModu == OyunModu.HIKAYE) return HikayeBolumu.numaradan(bolum).getTema();
        BolumTemasi[] temalar = BolumTemasi.values();
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

    void ciz(Graphics2D g) {
        asteroitler.forEach(a -> a.ciz(g));
        guclendirmeler.forEach(p -> p.ciz(g));
        dusmanlar.forEach(d -> d.ciz(g));
        if (boss != null) boss.ciz(g);
        oyuncuMermileri.forEach(m -> m.ciz(g));
        dusmanMermileri.forEach(m -> m.ciz(g));
        oyuncu.ciz(g);
        parcaciklar.forEach(p -> p.ciz(g));
        hudCiz(g);
    }

    private void hudCiz(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 155));
        g.fillRoundRect(12, 10, 270, 102, 18, 18);
        g.fillRoundRect(GENISLIK - 322, 10, 310, 102, 18, 18);
        g.setFont(Cizim.font(Font.BOLD, 17));
        g.setColor(Color.WHITE);
        g.drawString("Skor: " + skor, 26, 34);
        g.drawString("Coin: " + coin, 26, 58);
        g.drawString(oyunModu == OyunModu.HIKAYE ? "Bölüm: " + bolum + "/5"
                : "Dalga: " + bolum, 26, 82);
        g.drawString("Oyuncu seviyesi: " + seviyeSistemi.getSeviye(), 26, 105);

        g.setFont(Cizim.font(Font.BOLD, 15));
        g.drawString(hedefMetni(), GENISLIK - 306, 34);
        g.drawString("Silah: " + oyuncu.getSilahTuru().getAd(), GENISLIK - 306, 58);
        g.drawString("Kalıcı: " + oyuncu.getKaliciSilahTuru().getAd(), GENISLIK - 306, 80);
        g.drawString("Bomba: " + oyuncu.getBombaSayisi(), GENISLIK - 306, 102);

        xpCubuguCiz(g);
        for (int i = 0; i < oyuncu.getMaksimumCan(); i++) {
            g.setColor(i < oyuncu.getCan() ? new Color(255, 65, 90) : new Color(80, 45, 55));
            kalpCiz(g, 20 + i * 26, 145, 19);
        }
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
        g.setFont(Cizim.font(Font.BOLD, 12));
        String metin = "XP " + seviyeSistemi.getMevcutXp() + "/" + seviyeSistemi.gerekenXpGetir();
        int sw = g.getFontMetrics().stringWidth(metin);
        g.drawString(metin, GENISLIK / 2 - sw / 2, y + 13);
    }

    private void kalpCiz(Graphics2D g, int x, int y, int b) {
        g.fillOval(x, y, b / 2 + 2, b / 2 + 2);
        g.fillOval(x + b / 2 - 2, y, b / 2 + 2, b / 2 + 2);
        int[] xs = {x, x + b, x + b / 2};
        int[] ys = {y + b / 3, y + b / 3, y + b};
        g.fillPolygon(xs, ys, 3);
    }

    double getEkranSarsintisi() { return ekranSarsintisi; }
    OyunModu getOyunModu() { return oyunModu; }
    int getBolum() { return bolum; }
    int getSonrakiBolum() { return sonrakiBolum; }
    int getSkor() { return skor; }
    int getCoin() { return coin; }
    int getOyuncuSeviyesi() { return seviyeSistemi.getSeviye(); }
    SilahTuru getKaliciSilahTuru() { return oyuncu.getKaliciSilahTuru(); }
}
