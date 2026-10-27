package com.reelssayaci;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Ana ekran: beyin, bugünkü sayaç, limit ayarı ve sayacı açma düğmesi. */
public class MainActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private Store store;
    private BrainView brain;
    private TextView healthText;
    private TextView moodText;
    private TextView countText;
    private TextView remainingText;
    private TextView breakdownText;
    private TextView limitText;
    private LinearLayout serviceOff;
    private TextView serviceOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new Store(this);
        setContentView(build());
    }

    @Override
    protected void onResume() {
        super.onResume();
        store.prefs().registerOnSharedPreferenceChangeListener(this);
        refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        store.prefs().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        refresh();
    }

    private void refresh() {
        int count = store.count();
        int limit = store.limit();
        int health = Brain.health(count, limit);

        brain.setHealth(health);
        healthText.setText("Beyin sağlığı %" + health);
        healthText.setTextColor(Brain.statusColor(health));
        moodText.setText(Brain.mood(health));
        countText.setText(count + " / " + limit);
        remainingText.setText(count >= limit
                ? "Limit doldu. Reels, Shorts ve TikTok yarına kadar kapalı."
                : (limit - count) + " reels hakkın kaldı");
        breakdownText.setText("Instagram " + store.count(App.INSTAGRAM)
                + "  ·  YouTube " + store.count(App.YOUTUBE)
                + "  ·  TikTok " + store.count(App.TIKTOK)
                + "\nDün: " + store.yesterday() + " reels");
        limitText.setText(String.valueOf(limit));

        boolean enabled = ReelsService.isEnabled(this);
        serviceOff.setVisibility(enabled ? View.GONE : View.VISIBLE);
        serviceOn.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    private View build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BG);
        scroll.setFillViewport(true);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = Ui.dp(this, 20);
        col.setPadding(pad, Ui.dp(this, 28), pad, pad);
        scroll.addView(col);

        col.addView(Ui.text(this, "Reels Sayacı", 26, Ui.TEXT, true));
        col.addView(Ui.text(this, "Doomscroll'a dur de 🧠", 14, Ui.MUTED, false));

        brain = new BrainView(this);
        brain.setRing(true);
        LinearLayout.LayoutParams brainLp = new LinearLayout.LayoutParams(Ui.dp(this, 220), Ui.dp(this, 220));
        brainLp.topMargin = Ui.dp(this, 16);
        col.addView(brain, brainLp);

        healthText = Ui.text(this, "", 22, Ui.TEXT, true);
        col.addView(healthText);
        moodText = Ui.text(this, "", 16, Ui.TEXT, false);
        moodText.setPadding(0, Ui.dp(this, 4), 0, 0);
        col.addView(moodText);

        LinearLayout countCard = card(col);
        countCard.addView(Ui.text(this, "Bugün izlenen reels", 14, Ui.MUTED, false));
        countText = Ui.text(this, "", 44, Ui.TEXT, true);
        countCard.addView(countText);
        remainingText = Ui.text(this, "", 15, Ui.ACCENT, true);
        countCard.addView(remainingText);
        breakdownText = Ui.text(this, "", 13, Ui.MUTED, false);
        breakdownText.setPadding(0, Ui.dp(this, 10), 0, 0);
        countCard.addView(breakdownText);

        serviceOff = card(col);
        serviceOff.addView(Ui.text(this, "⚠️ Sayaç kapalı", 18, 0xFFFACC15, true));
        TextView offInfo = Ui.text(this,
                "Saymak ve engellemek için Erişilebilirlik izni gerekiyor. Açılan listede "
                        + "\"Reels Sayacı\"nı bulup aç.", 14, Ui.TEXT, false);
        offInfo.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 12));
        serviceOff.addView(offInfo);
        Button enable = Ui.button(this, "Sayacı aç", Ui.ACCENT, 0xFF1B1526);
        enable.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        serviceOff.addView(enable, fullWidth());
        TextView restricted = Ui.text(this,
                "\"Kısıtlanmış ayar\" uyarısı çıkarsa: aşağıdaki düğmeyle uygulama bilgisine git, "
                        + "sağ üstteki ⋮ menüsünden \"Kısıtlanmış ayarlara izin ver\"i seç, sonra tekrar dene.",
                12, Ui.MUTED, false);
        restricted.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 8));
        serviceOff.addView(restricted);
        Button appInfo = Ui.button(this, "Uygulama bilgisi", 0xFF2C2442, Ui.TEXT);
        appInfo.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null))));
        serviceOff.addView(appInfo, fullWidth());

        serviceOn = Ui.text(this, "✅ Sayaç açık: Reels, Shorts ve TikTok takip ediliyor", 14, 0xFF4ADE80, true);
        LinearLayout.LayoutParams onLp = fullWidth();
        onLp.topMargin = Ui.dp(this, 16);
        col.addView(serviceOn, onLp);

        LinearLayout limitCard = card(col);
        limitCard.addView(Ui.text(this, "Günlük limit", 14, Ui.MUTED, false));
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        Button minus = Ui.button(this, "−", 0xFF2C2442, Ui.TEXT);
        Button plus = Ui.button(this, "+", 0xFF2C2442, Ui.TEXT);
        limitText = Ui.text(this, "", 34, Ui.TEXT, true);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(Ui.dp(this, 64), Ui.dp(this, 56));
        LinearLayout.LayoutParams valueLp = new LinearLayout.LayoutParams(Ui.dp(this, 110), LinearLayout.LayoutParams.WRAP_CONTENT);
        row.addView(minus, btnLp);
        row.addView(limitText, valueLp);
        row.addView(plus, btnLp);
        limitCard.addView(row);
        minus.setOnClickListener(v -> store.setLimit(store.limit() - Store.LIMIT_STEP));
        plus.setOnClickListener(v -> {
            if (!store.setLimit(store.limit() + Store.LIMIT_STEP)) {
                Toast.makeText(this, "Limit doldu, bugün artıramazsın. Yarın görüşürüz 🙂", Toast.LENGTH_LONG).show();
            }
        });
        TextView limitInfo = Ui.text(this, "Limit dolunca o gün artırılamaz. Sayaç her gece 00:00'da sıfırlanır.",
                12, Ui.MUTED, false);
        limitInfo.setPadding(0, Ui.dp(this, 8), 0, 0);
        limitCard.addView(limitInfo);

        return scroll;
    }

    private LinearLayout card(LinearLayout parent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setBackground(Ui.rounded(this, Ui.CARD, 20));
        int p = Ui.dp(this, 18);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = fullWidth();
        lp.topMargin = Ui.dp(this, 16);
        parent.addView(card, lp);
        return card;
    }

    private static LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }
}
