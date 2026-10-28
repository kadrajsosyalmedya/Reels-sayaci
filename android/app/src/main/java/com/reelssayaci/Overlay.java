package com.reelssayaci;

import android.accessibilityservice.AccessibilityService;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Reels/Shorts/TikTok açıkken ekranın solunda duran küçük sayaç: beyin + "23 / 50".
 * Erişilebilirlik katmanı olduğu için ekstra izin istemez; dokunmaları engellemez.
 */
final class Overlay {
    private final AccessibilityService service;
    private final WindowManager wm;
    private LinearLayout root;
    private BrainView brain;
    private TextView countText;
    private TextView healthText;
    private boolean shown;

    Overlay(AccessibilityService service) {
        this.service = service;
        this.wm = (WindowManager) service.getSystemService(AccessibilityService.WINDOW_SERVICE);
    }

    void show(int count, int limit, long passMs) {
        if (root == null) build();
        update(count, limit, passMs, false);
        if (shown) return;
        try {
            wm.addView(root, params());
            shown = true;
        } catch (RuntimeException ignored) {
            // Pencere eklenemediyse (servis kapanıyor vb.) bir sonraki olayda tekrar denenir.
        }
    }

    void hide() {
        if (!shown) return;
        shown = false;
        try {
            wm.removeViewImmediate(root);
        } catch (RuntimeException ignored) {
        }
    }

    void update(int count, int limit, long passMs, boolean pulse) {
        if (root == null) return;
        int health = Brain.health(count, limit);
        brain.setHealth(health);
        countText.setText(count + " / " + limit);
        if (passMs > 0) {
            healthText.setText("⏱ İzin " + Store.formatRemaining(passMs));
            healthText.setTextColor(0xFFFACC15);
        } else {
            healthText.setText(count >= limit ? "Son reels!" : "Beyin %" + health);
            healthText.setTextColor(Brain.statusColor(health));
        }
        if (pulse) {
            brain.animate().cancel();
            brain.setScaleX(1.3f);
            brain.setScaleY(1.3f);
            brain.animate().scaleX(1f).scaleY(1f).setDuration(350).start();
        }
    }

    private void build() {
        root = new LinearLayout(service);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setBackground(Ui.rounded(service, 0xCC000000, 22));
        int p = Ui.dp(service, 6);
        root.setPadding(p, p, Ui.dp(service, 12), p);

        brain = new BrainView(service);
        root.addView(brain, new LinearLayout.LayoutParams(Ui.dp(service, 40), Ui.dp(service, 40)));

        LinearLayout texts = new LinearLayout(service);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(Ui.dp(service, 6), 0, 0, 0);
        countText = Ui.text(service, "", 16, 0xFFFFFFFF, true);
        healthText = Ui.text(service, "", 11, 0xFFFFFFFF, true);
        countText.setGravity(Gravity.START);
        healthText.setGravity(Gravity.START);
        texts.addView(countText);
        texts.addView(healthText);
        root.addView(texts);
        root.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }

    private WindowManager.LayoutParams params() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = Ui.dp(service, 8);
        lp.y = Math.round(service.getResources().getDisplayMetrics().heightPixels * 0.22f);
        return lp;
    }
}
