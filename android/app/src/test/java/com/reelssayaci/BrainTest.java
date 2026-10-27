package com.reelssayaci;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class BrainTest {
    @Test
    public void healthGoesFromFullToEmpty() {
        assertEquals(100, Brain.health(0, 50));
        assertEquals(50, Brain.health(25, 50));
        assertEquals(0, Brain.health(50, 50));
        assertEquals(0, Brain.health(80, 50));
        assertEquals(0, Brain.health(3, 0));
    }

    @Test
    public void colorsMatchStopsAtEnds() {
        assertEquals(0xFFFF8FB8, Brain.brainColor(100));
        assertEquals(0xFF93A86A, Brain.brainColor(0));
        assertEquals(0xFF4ADE80, Brain.statusColor(100));
        assertEquals(0xFFEF4444, Brain.statusColor(0));
        assertNotEquals(Brain.brainColor(60), Brain.brainColor(40));
    }

    @Test
    public void appsAreRecognizedByPackageAndViewId() {
        assertEquals(App.INSTAGRAM, App.fromPackage("com.instagram.android"));
        assertEquals(App.YOUTUBE, App.fromPackage("com.google.android.youtube"));
        assertEquals(App.TIKTOK, App.fromPackage("com.zhiliaoapp.musically"));
        assertEquals(null, App.fromPackage("com.whatsapp"));
        assertEquals(true, App.INSTAGRAM.isReelsViewId("com.instagram.android:id/clips_viewer_view_pager"));
        assertEquals(false, App.INSTAGRAM.isReelsViewId("com.instagram.android:id/clips_tab"));
        assertEquals(true, App.YOUTUBE.isReelsViewId("com.google.android.youtube:id/reel_recycler"));
        assertEquals(false, App.YOUTUBE.isReelsViewId(null));
    }
}
