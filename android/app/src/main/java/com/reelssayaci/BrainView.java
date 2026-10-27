package com.reelssayaci;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Beyin sağlığına göre renk ve yüz ifadesi değişen beyin çizimi.
 * Tüm çizim 100x100'lük bir kutuda yapılır ve görünümün boyutuna ölçeklenir.
 */
public class BrainView extends View {
    // Beynin dış hatlarını oluşturan daireler (x, y, yarıçap).
    private static final float[][] LOBES = {
            {30, 38, 17}, {21, 54, 14}, {32, 66, 15}, {44, 28, 15},
            {70, 38, 17}, {79, 54, 14}, {68, 66, 15}, {56, 28, 15},
            {50, 52, 24},
    };
    private static final float[] EYES_X = {41, 59};
    private static final float EYES_Y = 50;
    private static final int FACE = 0xFF3A1F2B;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path[] folds = {
            quad(38, 22, 6, 4, 4, 10), quad(62, 22, -6, 4, -4, 10),
            quad(20, 46, 6, -4, 10, 1), quad(80, 46, -6, -4, -10, 1),
            quad(24, 64, 4, -5, 9, -2), quad(76, 64, -4, -5, -9, -2),
            quad(50, 16, -3, 6, 0, 12),
    };
    private final Path path = new Path();
    private final RectF oval = new RectF();

    private int health = 100;
    private boolean ring;

    public BrainView(Context context) {
        this(context, null);
    }

    public BrainView(Context context, AttributeSet attrs) {
        super(context, attrs);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setHealth(int health) {
        if (this.health == health) return;
        this.health = health;
        invalidate();
    }

    /** Beynin etrafına sağlık yüzdesini gösteren halka çizer. */
    public void setRing(boolean ring) {
        this.ring = ring;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int fallback = Math.round(100 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(resolveSize(fallback, widthMeasureSpec), resolveSize(fallback, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float size = Math.min(getWidth(), getHeight());
        if (size <= 0) return;
        canvas.save();
        canvas.translate((getWidth() - size) / 2f, (getHeight() - size) / 2f);
        canvas.scale(size / 100f, size / 100f);
        if (ring) {
            drawRing(canvas);
            canvas.scale(0.72f, 0.72f, 50, 50);
        }
        drawBrain(canvas);
        canvas.restore();
    }

    private void drawRing(Canvas canvas) {
        stroke.setStrokeWidth(4.5f);
        stroke.setColor(0x26FFFFFF);
        canvas.drawCircle(50, 50, 46, stroke);
        if (health > 0) {
            stroke.setColor(Brain.statusColor(health));
            oval.set(4, 4, 96, 96);
            canvas.drawArc(oval, -90, 360f * health / 100f, false, stroke);
        }
    }

    private void drawBrain(Canvas canvas) {
        int body = Brain.brainColor(health);
        int outline = Brain.brainOutline(health);

        if (health < 25) drawSteam(canvas);

        // Önce kalın kontur, sonra dolgu: üst üste binen dairelerden tek parça bir siluet çıkar.
        stroke.setStrokeWidth(6);
        stroke.setColor(outline);
        for (float[] l : LOBES) canvas.drawCircle(l[0], l[1], l[2], stroke);
        fill.setColor(body);
        for (float[] l : LOBES) canvas.drawCircle(l[0], l[1], l[2], fill);

        fill.setColor(0x40FFFFFF);
        canvas.drawCircle(33, 30, 5, fill);

        stroke.setStrokeWidth(2.4f);
        for (Path fold : folds) canvas.drawPath(fold, stroke);

        drawFace(canvas, outline);

        if (health >= 80) {
            fill.setColor(0xFFFFE66D);
            drawSparkle(canvas, 12, 18, 5);
            drawSparkle(canvas, 89, 14, 4);
        }
        if (health >= 10 && health < 50) drawSweat(canvas);
    }

    private void drawFace(Canvas canvas, int outline) {
        if (health >= 60) {
            fill.setColor(0x73FF5C8A);
            canvas.drawCircle(34, 57, 3.8f, fill);
            canvas.drawCircle(66, 57, 3.8f, fill);
        }

        for (float x : EYES_X) {
            if (health < 10) {
                // Bayılmış: X gözler
                stroke.setColor(FACE);
                stroke.setStrokeWidth(2.2f);
                canvas.drawLine(x - 3, EYES_Y - 3, x + 3, EYES_Y + 3, stroke);
                canvas.drawLine(x + 3, EYES_Y - 3, x - 3, EYES_Y + 3, stroke);
            } else if (health < 40) {
                // Yorgun: yarı kapalı gözler
                fill.setColor(FACE);
                oval.set(x - 3.6f, EYES_Y - 3.6f, x + 3.6f, EYES_Y + 3.6f);
                canvas.drawArc(oval, 0, 180, true, fill);
                stroke.setColor(outline);
                stroke.setStrokeWidth(1.8f);
                canvas.drawLine(x - 4.2f, EYES_Y, x + 4.2f, EYES_Y, stroke);
            } else {
                fill.setColor(FACE);
                canvas.drawCircle(x, EYES_Y, 3.6f, fill);
                fill.setColor(0xFFFFFFFF);
                canvas.drawCircle(x + 1.2f, EYES_Y - 1.2f, 1.2f, fill);
            }
        }

        stroke.setColor(FACE);
        stroke.setStrokeWidth(2.6f);
        path.reset();
        if (health < 10) {
            // Dalgalı ağız
            path.moveTo(42, 61);
            for (int i = 0; i < 4; i++) path.rQuadTo(2, i % 2 == 0 ? -3 : 3, 4, 0);
        } else {
            float smile = (health - 50) / 50f;
            path.moveTo(42, 60);
            path.quadTo(50, 60 + smile * 8, 58, 60);
        }
        canvas.drawPath(path, stroke);
    }

    private void drawSteam(Canvas canvas) {
        stroke.setColor(0xB39CA3AF);
        stroke.setStrokeWidth(2);
        for (float x : new float[]{36, 50, 64}) {
            path.reset();
            path.moveTo(x, 12);
            path.rQuadTo(-3, -3, 0, -6);
            path.rQuadTo(3, -3, 0, -6);
            canvas.drawPath(path, stroke);
        }
    }

    private void drawSweat(Canvas canvas) {
        fill.setColor(0xFF7DD3FC);
        path.reset();
        path.moveTo(80, 18);
        path.lineTo(83.6f, 26);
        path.lineTo(76.4f, 26);
        path.close();
        canvas.drawPath(path, fill);
        canvas.drawCircle(80, 27.5f, 4, fill);
    }

    private void drawSparkle(Canvas canvas, float cx, float cy, float s) {
        path.reset();
        path.moveTo(cx, cy - s);
        path.quadTo(cx, cy, cx + s, cy);
        path.quadTo(cx, cy, cx, cy + s);
        path.quadTo(cx, cy, cx - s, cy);
        path.quadTo(cx, cy, cx, cy - s);
        canvas.drawPath(path, fill);
    }

    private static Path quad(float x, float y, float dx1, float dy1, float dx2, float dy2) {
        Path p = new Path();
        p.moveTo(x, y);
        p.rQuadTo(dx1, dy1, dx2, dy2);
        return p;
    }
}
