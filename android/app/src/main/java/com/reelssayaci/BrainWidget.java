package com.reelssayaci;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.widget.RemoteViews;

/** Ana ekran aracı: beyin ve bugünkü reels sayısı. */
public class BrainWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        update(context, manager, ids);
    }

    static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        if (manager == null) return;
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, BrainWidget.class));
        if (ids != null && ids.length > 0) update(context, manager, ids);
    }

    private static void update(Context context, AppWidgetManager manager, int[] ids) {
        Store store = new Store(context);
        int count = store.count();
        int limit = store.limit();
        int health = Brain.health(count, limit);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_brain);
        views.setTextViewText(R.id.widget_count, count + " / " + limit);
        views.setTextViewText(R.id.widget_health, count >= limit ? "Bugünlük bitti" : "Beyin %" + health);
        views.setTextColor(R.id.widget_health, Brain.statusColor(health));
        views.setImageViewBitmap(R.id.widget_brain, render(context, health));
        PendingIntent open = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_root, open);
        manager.updateAppWidget(ids, views);
    }

    /** Araçlar özel görünüm barındıramaz; beyni resme çizip veriyoruz. */
    private static Bitmap render(Context context, int health) {
        int size = Ui.dp(context, 110);
        BrainView brain = new BrainView(context.getApplicationContext());
        brain.setHealth(health);
        int spec = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY);
        brain.measure(spec, spec);
        brain.layout(0, 0, size, size);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        brain.draw(new Canvas(bitmap));
        return bitmap;
    }
}
