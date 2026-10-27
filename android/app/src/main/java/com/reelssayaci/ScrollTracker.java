package com.reelssayaci;

/**
 * Bir kaydırıcının olaylarına bakıp ekrana yeni bir reels gelip gelmediğine karar verir.
 *
 * Kaydırıcı ilk görünen öğenin sırasını bildiriyorsa (RecyclerView, ViewPager) o sıra şimdiye
 * kadar görülen en büyük sırayı geçtiğinde sayılır: geri kaydırıp eski videoya dönmek veya
 * yarım bırakılan bir kaydırma sayılmaz. Sıra bilgisi yoksa ya da güvenilmez çıkarsa, araya
 * sessizlik giren her kaydırma hareketi bir reels sayılır.
 */
public final class ScrollTracker {
    /** Bu kadar süre olay gelmezse sonraki kaydırma yeni bir hareket sayılır. */
    static final long GESTURE_GAP_MS = 700;
    /** İki sayım arasındaki en kısa süre; tek kaydırmanın iki kez sayılmasını önler. */
    static final long MIN_COUNT_GAP_MS = 350;
    /** Sıra değişmeden üst üste bu kadar hareket olursa sıra bilgisine güvenilmez. */
    static final int STALE_GESTURES_LIMIT = 2;

    private long lastEventAt = Long.MIN_VALUE / 2;
    private long lastCountAt = Long.MIN_VALUE / 2;
    private int maxIndex = -1;
    private int lastIndex = -1;
    private int lastItemCount = -1;
    private boolean gestureStarted;
    private boolean indexChangedInGesture;
    private int staleGestures;
    private boolean indexUnreliable;

    /** @return bu olayla birlikte yeni bir reels izlenmeye başlandıysa true */
    public boolean onScroll(long now, int fromIndex, int itemCount) {
        boolean newGesture = now - lastEventAt > GESTURE_GAP_MS;
        lastEventAt = now;

        boolean indexed = !indexUnreliable && fromIndex >= 0 && itemCount > 1;
        if (!indexed) {
            return newGesture && countIfSpaced(now);
        }

        if (newGesture) {
            if (gestureStarted && !indexChangedInGesture) {
                if (++staleGestures >= STALE_GESTURES_LIMIT) {
                    indexUnreliable = true;
                    return countIfSpaced(now);
                }
            } else {
                staleGestures = 0;
            }
            gestureStarted = true;
            indexChangedInGesture = false;
        }

        // Liste baştan yüklendiyse (yenileme) eski en büyük sıra artık anlamsız.
        if (lastItemCount > 0 && itemCount < lastItemCount) {
            maxIndex = -1;
        }
        lastItemCount = itemCount;

        if (lastIndex >= 0 && fromIndex != lastIndex) indexChangedInGesture = true;
        lastIndex = fromIndex;

        if (fromIndex > maxIndex) {
            maxIndex = fromIndex;
            indexChangedInGesture = true;
            return countIfSpaced(now);
        }
        return false;
    }

    private boolean countIfSpaced(long now) {
        if (now - lastCountAt < MIN_COUNT_GAP_MS) return false;
        lastCountAt = now;
        return true;
    }
}
