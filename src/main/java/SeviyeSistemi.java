public final class SeviyeSistemi {
    private int seviye = 1;
    private int mevcutXp;
    private int toplamXp;

    public int xpEkle(int miktar) {
        if (miktar <= 0) return 0;
        int oncekiSeviye = seviye;
        mevcutXp += miktar;
        toplamXp += miktar;
        while (mevcutXp >= gerekenXpGetir()) {
            int gereken = gerekenXpGetir();
            mevcutXp -= gereken;
            seviye++;
        }
        return seviye - oncekiSeviye;
    }

    public int gerekenXpGetir() {
        return 100 + (seviye - 1) * 55;
    }

    public double ilerlemeOraniGetir() {
        return Math.min(1.0, mevcutXp / (double) gerekenXpGetir());
    }

    public int getSeviye() { return seviye; }
    public int getMevcutXp() { return mevcutXp; }
    public int getToplamXp() { return toplamXp; }
}
