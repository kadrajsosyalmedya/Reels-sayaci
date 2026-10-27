package com.reelssayaci;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Limit dolunca Reels/Shorts/TikTok yerine açılan ekran. */
public class BlockActivity extends Activity {
    static final String EXTRA_APP = "app";

    private static final String[] TIPS = {
            "💧 Bir bardak su iç",
            "🚶 10 dakika yürüyüşe çık",
            "📖 Birkaç sayfa kitap oku",
            "🧘 5 derin nefes al",
            "📞 Bir arkadaşını ara",
            "🎧 Sevdiğin bir şarkıyı dinle",
            "✍️ Yarın için bir hedef yaz",
            "🛏️ Ekranı kapat, gözlerini dinlendir",
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Store store = new Store(this);
        String app = getIntent().getStringExtra(EXTRA_APP);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setBackgroundColor(Ui.BG);
        int pad = Ui.dp(this, 28);
        col.setPadding(pad, pad, pad, pad);

        BrainView brain = new BrainView(this);
        brain.setHealth(0);
        col.addView(brain, new LinearLayout.LayoutParams(Ui.dp(this, 180), Ui.dp(this, 180)));

        col.addView(Ui.text(this, "Bugünlük bu kadar! 🛑", 26, Ui.TEXT, true));
        TextView info = Ui.text(this, "Bugün " + store.count() + " reels izledin. "
                + (app != null ? app + " yarına kadar kapalı. " : "")
                + "Beynin dinlenmeyi hak ediyor.", 16, Ui.MUTED, false);
        info.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 20));
        col.addView(info);

        List<String> tips = new ArrayList<>();
        Collections.addAll(tips, TIPS);
        Collections.shuffle(tips);
        LinearLayout tipCard = new LinearLayout(this);
        tipCard.setOrientation(LinearLayout.VERTICAL);
        tipCard.setBackground(Ui.rounded(this, Ui.CARD, 20));
        int p = Ui.dp(this, 18);
        tipCard.setPadding(p, p, p, p);
        tipCard.addView(Ui.text(this, "Bunun yerine şunu dene:", 14, Ui.ACCENT, true));
        for (int i = 0; i < 3; i++) {
            TextView tip = Ui.text(this, tips.get(i), 16, Ui.TEXT, false);
            tip.setPadding(0, Ui.dp(this, 8), 0, 0);
            tipCard.addView(tip);
        }
        col.addView(tipCard, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        Button ok = Ui.button(this, "Tamam, telefonu bırakıyorum", Ui.ACCENT, 0xFF1B1526);
        ok.setOnClickListener(v -> goHome());
        LinearLayout.LayoutParams okLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        okLp.topMargin = Ui.dp(this, 24);
        col.addView(ok, okLp);

        TextView reset = Ui.text(this, "Sayaç gece 00:00'da sıfırlanır.", 12, Ui.MUTED, false);
        reset.setPadding(0, Ui.dp(this, 12), 0, 0);
        col.addView(reset);

        setContentView(col);
    }

    @Override
    public void onBackPressed() {
        goHome();
    }

    private void goHome() {
        startActivity(new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }
}
