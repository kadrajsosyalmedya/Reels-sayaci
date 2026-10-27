package com.reelssayaci;

/** Takip edilen uygulamalar ve kısa video ekranlarını tanımak için kullanılan görünüm kimlikleri. */
public enum App {
    INSTAGRAM("instagram", "Instagram Reels", false,
            new String[]{"com.instagram.android"},
            new String[]{"clips_viewer_view_pager", "clips_viewer_container", "clips_video_container", "root_clips_layout"},
            new String[]{"clips_viewer", "clips_video", "root_clips"}),
    YOUTUBE("youtube", "YouTube Shorts", false,
            new String[]{"com.google.android.youtube", "app.revanced.android.youtube", "app.rvx.android.youtube"},
            new String[]{"reel_recycler", "reel_player_page_container", "reel_watch_player", "reel_watch_fragment_root"},
            new String[]{"reel_recycler", "reel_player", "reel_watch"}),
    // TikTok'un tamamı kısa video akışı, bu yüzden uygulamanın kendisi engellenir.
    TIKTOK("tiktok", "TikTok", true,
            new String[]{"com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.zhiliaoapp.musically.go", "com.ss.android.ugc.aweme"},
            new String[0],
            new String[0]);

    public final String key;
    public final String title;
    public final boolean wholeApp;
    final String[] packages;
    final String[] exactIds;
    final String[] idFragments;

    App(String key, String title, boolean wholeApp, String[] packages, String[] exactIds, String[] idFragments) {
        this.key = key;
        this.title = title;
        this.wholeApp = wholeApp;
        this.packages = packages;
        this.exactIds = exactIds;
        this.idFragments = idFragments;
    }

    public static App fromPackage(CharSequence pkg) {
        if (pkg == null) return null;
        String p = pkg.toString();
        for (App app : values()) {
            for (String candidate : app.packages) {
                if (candidate.equals(p)) return app;
            }
        }
        return null;
    }

    /** "paket:id/isim" biçimindeki kimliğin bu uygulamanın kısa video ekranına ait olup olmadığı. */
    public boolean isReelsViewId(String viewId) {
        if (viewId == null) return false;
        int slash = viewId.indexOf(":id/");
        String name = slash >= 0 ? viewId.substring(slash + 4) : viewId;
        for (String fragment : idFragments) {
            if (name.contains(fragment)) return true;
        }
        return false;
    }
}
