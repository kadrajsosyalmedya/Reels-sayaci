package com.reelssayaci;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Instagram Reels, YouTube Shorts ve TikTok'ta kaydırılan videoları sayar,
 * sayacı ekranda gösterir ve günlük limit dolunca bu ekranları kapatır.
 */
public class ReelsService extends AccessibilityService {
    private static final long CHECK_DELAY_MS = 250;
    private static final long SCAN_INTERVAL_MS = 1500;
    private static final long BLOCK_COOLDOWN_MS = 2000;
    private static final int SCAN_NODE_BUDGET = 300;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, ScrollTracker> trackers = new HashMap<>();
    /** Taramada bulunan, listede olmayan reels görünüm kimlikleri; sonraki kontroller hızlı olsun diye. */
    private final Set<String> learnedIds = new HashSet<>();
    private final Rect rect = new Rect();

    private Store store;
    private Overlay overlay;
    private App currentApp;
    private boolean inReels;
    private boolean checkPending;
    private long lastScanAt;
    private long lastBlockAt = -BLOCK_COOLDOWN_MS;

    private final Runnable checkRunnable = () -> {
        checkPending = false;
        checkScreen();
    };

    public static boolean isEnabled(Context context) {
        String enabled = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        ComponentName me = new ComponentName(context, ReelsService.class);
        for (String part : enabled.split(":")) {
            if (me.equals(ComponentName.unflattenFromString(part))) return true;
        }
        return false;
    }

    @Override
    protected void onServiceConnected() {
        store = new Store(this);
        overlay = new Overlay(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (store == null) return;
        CharSequence pkg = event.getPackageName();
        if (pkg == null || getPackageName().contentEquals(pkg)) return;

        App app = App.fromPackage(pkg);
        int type = event.getEventType();
        if (app == null) {
            // Klavye veya bildirim paneli de pencere açar; ön plandaki uygulama gerçekten değişti mi bak.
            if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && currentApp != null) scheduleCheck();
            return;
        }
        if (app != currentApp) {
            leave();
            currentApp = app;
        }
        if (type == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            onScroll(app, event);
        } else {
            scheduleCheck();
        }
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public boolean onUnbind(Intent intent) {
        handler.removeCallbacksAndMessages(null);
        if (overlay != null) overlay.hide();
        return super.onUnbind(intent);
    }

    private void scheduleCheck() {
        if (checkPending) return;
        checkPending = true;
        handler.postDelayed(checkRunnable, CHECK_DELAY_MS);
    }

    /** Ön plandaki ekran reels ekranı mı? Öyleyse sayacı göster, limit dolduysa kapat. */
    private void checkScreen() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        App app = App.fromPackage(root.getPackageName());
        if (app == null) {
            leave();
            return;
        }
        if (app != currentApp) {
            leave();
            currentApp = app;
        }
        setInReels(app, app.wholeApp || hasReelsView(app, root));
    }

    private void setInReels(App app, boolean value) {
        // Limit dolduysa reels ekranına girilemez. Zaten içerideyse limitteki son reels izlenir,
        // bir sonrakine kaydırınca onScroll kapatır.
        if (value && !inReels && store.limitReached()) {
            block(app);
            return;
        }
        if (value != inReels) {
            inReels = value;
            if (!value) trackers.clear();
        }
        if (value) {
            overlay.show(store.count(), store.limit());
        } else {
            overlay.hide();
        }
    }

    private void leave() {
        currentApp = null;
        inReels = false;
        trackers.clear();
        if (overlay != null) overlay.hide();
    }

    private boolean hasReelsView(App app, AccessibilityNodeInfo root) {
        String prefix = root.getPackageName() + ":id/";
        for (String id : app.exactIds) {
            if (visibleById(root, prefix + id)) return true;
        }
        for (String id : learnedIds) {
            if (id.startsWith(prefix) && visibleById(root, id)) return true;
        }
        long now = SystemClock.uptimeMillis();
        if (now - lastScanAt < SCAN_INTERVAL_MS) return false;
        lastScanAt = now;
        String found = scan(app, root, new int[]{SCAN_NODE_BUDGET});
        if (found == null) return false;
        learnedIds.add(found);
        return true;
    }

    private static boolean visibleById(AccessibilityNodeInfo root, String id) {
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(id);
        if (nodes == null) return false;
        for (AccessibilityNodeInfo n : nodes) {
            if (n != null && n.isVisibleToUser()) return true;
        }
        return false;
    }

    private static String scan(App app, AccessibilityNodeInfo node, int[] budget) {
        if (node == null || budget[0]-- <= 0) return null;
        String id = node.getViewIdResourceName();
        if (app.isReelsViewId(id) && node.isVisibleToUser()) return id;
        for (int i = 0; i < node.getChildCount() && budget[0] > 0; i++) {
            String found = scan(app, node.getChild(i), budget);
            if (found != null) return found;
        }
        return null;
    }

    private void onScroll(App app, AccessibilityEvent event) {
        if (isHorizontal(event)) return;
        AccessibilityNodeInfo source = event.getSource();
        if (source == null) return;
        String viewId = source.getViewIdResourceName();

        if (app.isReelsViewId(viewId)) {
            if (!inReels) setInReels(app, true);
        } else {
            // Instagram ve YouTube'da yalnızca reels/shorts ekranındayken; TikTok'ta her yerde.
            if (!app.wholeApp && !inReels) {
                scheduleCheck();
                return;
            }
            // Yorumlar gibi listeler değil, öğeleri tam ekran olan sayfalı akış olmalı.
            if (!isFullScreenPager(source)) return;
            if (app.wholeApp && !inReels) setInReels(app, true);
        }
        if (!inReels) return; // setInReels engellediyse

        String key = app.key + "|" + viewId;
        ScrollTracker tracker = trackers.get(key);
        if (tracker == null) {
            tracker = new ScrollTracker();
            trackers.put(key, tracker);
        }
        if (!tracker.onScroll(SystemClock.uptimeMillis(), event.getFromIndex(), event.getItemCount())) return;

        if (store.limitReached()) {
            block(app);
            return;
        }
        int count = store.increment(app);
        overlay.update(count, store.limit(), true);
    }

    private static boolean isHorizontal(AccessibilityEvent event) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false;
        int dx = Math.abs(event.getScrollDeltaX());
        int dy = Math.abs(event.getScrollDeltaY());
        return dx > 1 && dx > dy;
    }

    private boolean isFullScreenPager(AccessibilityNodeInfo source) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        source.getBoundsInScreen(rect);
        if (rect.height() < dm.heightPixels * 0.6f || rect.width() < dm.widthPixels * 0.8f) return false;
        int tallest = 0;
        int children = Math.min(source.getChildCount(), 4);
        for (int i = 0; i < children; i++) {
            AccessibilityNodeInfo child = source.getChild(i);
            if (child == null) continue;
            child.getBoundsInScreen(rect);
            tallest = Math.max(tallest, rect.height());
        }
        return tallest >= dm.heightPixels * 0.45f;
    }

    private void block(App app) {
        long now = SystemClock.uptimeMillis();
        inReels = false;
        trackers.clear();
        overlay.hide();
        if (now - lastBlockAt < BLOCK_COOLDOWN_MS) return;
        lastBlockAt = now;

        performGlobalAction(GLOBAL_ACTION_HOME);
        Intent intent = new Intent(this, BlockActivity.class)
                .putExtra(BlockActivity.EXTRA_APP, app.title)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        handler.postDelayed(() -> startActivity(intent), 300);
    }
}
