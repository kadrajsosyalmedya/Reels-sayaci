package com.reelssayaci;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ScrollTrackerTest {
    /** Bir kaydırma hareketini taklit eder: art arda 100 ms arayla gelen olaylar. */
    private static int swipe(ScrollTracker t, long[] clock, int... fromIndexes) {
        int counted = 0;
        for (int from : fromIndexes) {
            if (t.onScroll(clock[0], from, 20)) counted++;
            clock[0] += 100;
        }
        clock[0] += 2000; // video izleniyor
        return counted;
    }

    @Test
    public void eachSwipeToNextReelCountsOnce() {
        ScrollTracker t = new ScrollTracker();
        long[] clock = {10_000};
        assertEquals(1, swipe(t, clock, 0)); // ilk reels ekrana geldi
        assertEquals(1, swipe(t, clock, 0, 0, 0, 1, 1));
        assertEquals(1, swipe(t, clock, 1, 1, 2, 2));
        assertEquals(1, swipe(t, clock, 2, 3));
    }

    @Test
    public void scrollingBackDoesNotCount() {
        ScrollTracker t = new ScrollTracker();
        long[] clock = {10_000};
        swipe(t, clock, 0);
        swipe(t, clock, 0, 1);
        swipe(t, clock, 1, 2);
        assertEquals(0, swipe(t, clock, 2, 1));
        assertEquals(0, swipe(t, clock, 1, 2)); // tekrar ileri, ama zaten izlendi
        assertEquals(1, swipe(t, clock, 2, 3));
    }

    @Test
    public void halfSwipeThatSnapsBackDoesNotCount() {
        ScrollTracker t = new ScrollTracker();
        long[] clock = {10_000};
        swipe(t, clock, 0);
        assertEquals(0, swipe(t, clock, 0, 0, 0));
        assertEquals(1, swipe(t, clock, 0, 1));
    }

    @Test
    public void withoutIndexEachGestureCounts() {
        ScrollTracker t = new ScrollTracker();
        long[] clock = {10_000};
        assertEquals(1, swipe(t, clock, -1, -1, -1, -1));
        assertEquals(1, swipe(t, clock, -1, -1));
        assertEquals(1, swipe(t, clock, -1));
    }

    @Test
    public void indexThatNeverChangesFallsBackToGestures() {
        ScrollTracker t = new ScrollTracker();
        long[] clock = {10_000};
        int counted = 0;
        for (int i = 0; i < 10; i++) counted += swipe(t, clock, 0, 0, 0);
        // İlk hareket + güvenilmez bulunana kadar kaçanlar hariç hepsi sayılmalı.
        assertTrue("sayılan: " + counted, counted >= 8);
    }

    @Test
    public void feedReloadStartsCountingAgain() {
        ScrollTracker t = new ScrollTracker();
        long[] clock = {10_000};
        swipe(t, clock, 0);
        swipe(t, clock, 0, 1);
        swipe(t, clock, 1, 2);
        assertTrue(t.onScroll(clock[0], 0, 5)); // daha kısa yeni liste
        clock[0] += 2000;
        assertTrue(t.onScroll(clock[0], 1, 5));
    }

    @Test
    public void veryFastEventsCountOnlyOnce() {
        ScrollTracker t = new ScrollTracker();
        assertTrue(t.onScroll(1000, 0, 20));
        assertFalse(t.onScroll(1100, 1, 20));
    }
}
