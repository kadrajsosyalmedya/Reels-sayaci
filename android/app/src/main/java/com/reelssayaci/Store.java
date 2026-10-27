package com.reelssayaci;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Günlük sayaç ve limit. Gün değişince (gece 00:00) sayaç kendiliğinden sıfırlanır. */
public final class Store {
    public static final int DEFAULT_LIMIT = 50;
    public static final int MIN_LIMIT = 5;
    public static final int MAX_LIMIT = 300;
    public static final int LIMIT_STEP = 5;

    static final String PREFS = "reels_sayaci";
    static final String KEY_DAY = "day";
    static final String KEY_COUNT = "count";
    static final String KEY_LIMIT = "limit";
    static final String KEY_YESTERDAY = "yesterday";

    private final SharedPreferences prefs;

    public Store(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public SharedPreferences prefs() {
        return prefs;
    }

    public int count() {
        rollover();
        return prefs.getInt(KEY_COUNT, 0);
    }

    public int count(App app) {
        rollover();
        return prefs.getInt(appKey(app), 0);
    }

    public int yesterday() {
        rollover();
        return prefs.getInt(KEY_YESTERDAY, 0);
    }

    public int limit() {
        return prefs.getInt(KEY_LIMIT, DEFAULT_LIMIT);
    }

    public boolean limitReached() {
        return count() >= limit();
    }

    public int health() {
        return Brain.health(count(), limit());
    }

    /** @return yeni toplam */
    public int increment(App app) {
        rollover();
        int total = prefs.getInt(KEY_COUNT, 0) + 1;
        prefs.edit()
                .putInt(KEY_COUNT, total)
                .putInt(appKey(app), prefs.getInt(appKey(app), 0) + 1)
                .apply();
        return total;
    }

    /**
     * Limit dolduysa o gün limit artırılamaz; yoksa limit tek ayarla kalkardı.
     * @return limit değiştiyse true
     */
    public boolean setLimit(int newLimit) {
        newLimit = Math.max(MIN_LIMIT, Math.min(MAX_LIMIT, newLimit));
        if (newLimit > limit() && limitReached()) return false;
        prefs.edit().putInt(KEY_LIMIT, newLimit).apply();
        return true;
    }

    private void rollover() {
        String today = dayString(0);
        String day = prefs.getString(KEY_DAY, null);
        if (today.equals(day)) return;
        SharedPreferences.Editor e = prefs.edit();
        e.putInt(KEY_YESTERDAY, dayString(-1).equals(day) ? prefs.getInt(KEY_COUNT, 0) : 0);
        e.putString(KEY_DAY, today).putInt(KEY_COUNT, 0);
        for (App app : App.values()) e.putInt(appKey(app), 0);
        e.apply();
    }

    private static String appKey(App app) {
        return "count_" + app.key;
    }

    private static String dayString(int offsetDays) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, offsetDays);
        Date d = c.getTime();
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d);
    }
}
