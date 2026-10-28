package com.reelssayaci;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/** Bildirimdeki "2 dk izin al" düğmesi. */
public class PassReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Store store = new Store(context);
        String message = store.usePass()
                ? "⏱ 2 dakika iznin başladı. Bugün kalan hak: " + store.passesLeft()
                : store.passActive() ? "İznin zaten açık" : "Bugünkü izin hakların bitti";
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
