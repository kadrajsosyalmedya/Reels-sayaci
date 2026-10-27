package com.reelssayaci;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

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
    private static final int BACK_ATTEMPTS = 3;
    /** Limitteki son reels'e tanınan süre; sonra kaydırma algılanmasa bile kapatılır. */
    private static final long LAST_REEL_GRACE_MS = 30_000;
    /** Son reels sayıldıktan bu kadar sonra gelen kaydırma yeni bir harekettir. */
    private static final long NEXT_GESTURE_MS = 1500;
    private static final int SCAN_NODE_BUDGET = 300;
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "sayac";

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
    private boolean backingOut;
    private long lastScanAt;
    private long lastBlockAt = -BLOCK_COOLDOWN_MS;
    private long lastCountAt = -LAST_REEL_GRACE_MS;

    private final Runnable checkRunnable = () -> {
        checkPending = false;
        checkScreen();
    };

    /** Servis bu süreçte gerçekten çalışıyor mu? Ayar açık görünse de Xiaomi süreci öldürebiliyor. */
    static volatile boolean running;

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
        running = true;
        store = new Store(this);
        overlay = new Overlay(this);
        startKeepAlive();
        BrainWidget.updateAll(this);
    }

    /**
     * Kalıcı bildirimle ön plan servisine geçer. Xiaomi, Oppo gibi telefonlar aksi halde
     * uygulama kapatılınca sayacı da öldürüyor. Bildirimde bugünkü sayı da görünür.
     */
    private void startKeepAlive() {
        try {
            NotificationManager nm = getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Sayaç", NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            nm.createNotificationChannel(channel);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, buildNotification());
            }
        } catch (RuntimeException ignored) {
            // Ön plana geçilemezse sayaç yine çalışır, sadece kapatılmaya daha açıktır.
        }
    }

    private void updateNotification() {
        try {
            getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, buildNotification());
        } catch (RuntimeException ignored) {
        }
    }

    private Notification buildNotification() {
        int count = store.count();
        int limit = store.limit();
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Bugün " + count + " / " + limit + " reels")
                .setContentText(Brain.mood(Brain.health(count, limit)))
                .setContentIntent(open)
                .setOngoing(true)
                .setShowWhen(false)
                .build();
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
        running = false;
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
        // Limit dolduysa reels ekranına girilemez. Limitteki son reels'i izlemek için kısa bir
        // süre tanınır; bir sonrakine kaydırınca ya da süre bitince kapanır.
        if (value && store.limitReached()
                && (!inReels || SystemClock.uptimeMillis() - lastCountAt > LAST_REEL_GRACE_MS)) {
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
        if (hasKnownReelsView(app, root)) return true;
        long now = SystemClock.uptimeMillis();
        if (now - lastScanAt < SCAN_INTERVAL_MS) return false;
        lastScanAt = now;
        String found = scan(app, root, new int[]{SCAN_NODE_BUDGET});
        if (found == null) return false;
        learnedIds.add(found);
        return true;
    }

    /** Taramasız, hızlı kontrol: bilinen ve öğrenilmiş kimliklerle. */
    private boolean hasKnownReelsView(App app, AccessibilityNodeInfo root) {
        String prefix = root.getPackageName() + ":id/";
        for (String id : app.exactIds) {
            if (visibleById(root, prefix + id)) return true;
        }
        for (String id : learnedIds) {
            if (id.startsWith(prefix) && visibleById(root, id)) return true;
        }
        return false;
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
        long now = SystemClock.uptimeMillis();
        boolean newReel = tracker.onScroll(now, event.getFromIndex(), event.getItemCount());

        if (store.limitReached()) {
            // Uygulama yeniden açılınca akışın sırası değişebilir ve sayaç yeni reels'i kaçırabilir;
            // limit dolduktan sonra her yeni kaydırma hareketi kapatır.
            if (newReel || now - lastCountAt > NEXT_GESTURE_MS) block(app);
            return;
        }
        if (!newReel) return;
        lastCountAt = now;
        int count = store.increment(app);
        overlay.update(count, store.limit(), true);
        updateNotification();
        BrainWidget.updateAll(this);
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
        if (backingOut || now - lastBlockAt < BLOCK_COOLDOWN_MS) return;
        lastBlockAt = now;

        if (!app.wholeApp) {
            // Instagram/YouTube açık kalsın (mesajlar, normal videolar); sadece Reels/Shorts'tan çık.
            Toast.makeText(this, app.title + " bugünlük kapalı 🧠 Limitin doldu.", Toast.LENGTH_LONG).show();
            backingOut = true;
            backOutOfReels(app, 0);
            return;
        }
        leaveToBlockScreen(app);
    }

    /** Geri tuşuna basar; hâlâ Reels/Shorts ekranındaysa tekrar dener, olmazsa ana ekrana döner. */
    private void backOutOfReels(App app, int attempt) {
        if (attempt >= BACK_ATTEMPTS) {
            backingOut = false;
            leaveToBlockScreen(app);
            return;
        }
        performGlobalAction(GLOBAL_ACTION_BACK);
        handler.postDelayed(() -> {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null && App.fromPackage(root.getPackageName()) == app && hasKnownReelsView(app, root)) {
                backOutOfReels(app, attempt + 1);
            } else {
                backingOut = false;
                lastBlockAt = SystemClock.uptimeMillis();
            }
        }, 700);
    }

    private void leaveToBlockScreen(App app) {
        performGlobalAction(GLOBAL_ACTION_HOME);
        Intent intent = new Intent(this, BlockActivity.class)
                .putExtra(BlockActivity.EXTRA_APP, app.title)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        handler.postDelayed(() -> startActivity(intent), 300);
    }
}
