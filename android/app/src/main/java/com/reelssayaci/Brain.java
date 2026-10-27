package com.reelssayaci;

/** İzlenen reels sayısından "beyin sağlığı"nı, ruh halini ve renklerini hesaplar. */
public final class Brain {
    private Brain() {}

    /** 0 reels = %100, limit = %0. */
    public static int health(int count, int limit) {
        if (limit <= 0) return 0;
        int h = Math.round(100f * (limit - count) / limit);
        return Math.max(0, Math.min(100, h));
    }

    public static String mood(int health) {
        if (health >= 80) return "Beynin bomba gibi! 💪";
        if (health >= 60) return "Beynin iyi, ama dikkat 🙂";
        if (health >= 40) return "Beynin yorulmaya başladı 😐";
        if (health >= 20) return "Beynin eriyor... 🫠";
        if (health > 0) return "Beynin alarm veriyor! 🚨";
        return "Beynin tükendi, bugünlük bitti 💤";
    }

    // Sağlıklı pembeden hasta yeşilimsi griye.
    private static final int[] BRAIN_STOPS = {0xFF93A86A, 0xFFD4C060, 0xFFFFB870, 0xFFFFA3A3, 0xFFFF8FB8};
    // Yeşil -> sarı -> kırmızı.
    private static final int[] STATUS_STOPS = {0xFFEF4444, 0xFFF97316, 0xFFFACC15, 0xFFA3E635, 0xFF4ADE80};

    public static int brainColor(int health) {
        return gradient(BRAIN_STOPS, health);
    }

    /** Beynin dış çizgisi ve kıvrımları için koyu ton. */
    public static int brainOutline(int health) {
        return darken(brainColor(health), 0.32f);
    }

    public static int statusColor(int health) {
        return gradient(STATUS_STOPS, health);
    }

    static int gradient(int[] stops, int health) {
        float t = Math.max(0, Math.min(100, health)) / 100f * (stops.length - 1);
        int i = Math.min((int) t, stops.length - 2);
        return lerp(stops[i], stops[i + 1], t - i);
    }

    static int lerp(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = Math.round(ar + (br - ar) * t);
        int g = Math.round(ag + (bg - ag) * t);
        int bl = Math.round(ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    static int darken(int c, float amount) {
        float k = 1f - amount;
        int r = Math.round(((c >> 16) & 0xFF) * k);
        int g = Math.round(((c >> 8) & 0xFF) * k);
        int b = Math.round((c & 0xFF) * k);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
