/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class ImportingService extends Service implements NotificationCenter.NotificationCenterDelegate {

    private NotificationCompat.Builder builder;
    private final android.util.SparseBooleanArray observedAccounts = new android.util.SparseBooleanArray();

    private void attachAccountObservers(int a) {
        if (observedAccounts.get(a)) {
            return;
        }
        observedAccounts.put(a, true);
        NotificationCenter.getInstance(a).addObserver(this, NotificationCenter.historyImportProgressChanged);
        NotificationCenter.getInstance(a).addObserver(this, NotificationCenter.stickersImportProgressChanged);
    }

    public ImportingService() {
        super();
        for (int a = 0; a < UserConfig.getAccountSlotCount(); a++) {
            attachAccountObservers(a);
        }
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.accountSlotAllocated);
    }

    public IBinder onBind(Intent arg2) {
        return null;
    }

    public void onDestroy() {
        super.onDestroy();
        try {
            stopForeground(true);
        } catch (Throwable ignore) {

        }
        NotificationManagerCompat.from(ApplicationLoader.applicationContext).cancel(5);
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.accountSlotAllocated);
        for (int i = 0; i < observedAccounts.size(); i++) {
            int a = observedAccounts.keyAt(i);
            NotificationCenter.getInstance(a).removeObserver(this, NotificationCenter.historyImportProgressChanged);
            NotificationCenter.getInstance(a).removeObserver(this, NotificationCenter.stickersImportProgressChanged);
        }
        observedAccounts.clear();
        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("destroy import service");
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.accountSlotAllocated) {
            if (args != null && args.length > 0 && args[0] instanceof Integer) {
                attachAccountObservers((Integer) args[0]);
            }
            return;
        }
        if (id == NotificationCenter.historyImportProgressChanged || id == NotificationCenter.stickersImportProgressChanged) {
            if (!hasImportingStickers() && !hasImportingStickers()) {
                stopSelf();
            }
        }
    }

    private boolean hasImportingHistory() {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (SendMessagesHelper.getInstance(a).isImportingHistory()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasImportingStickers() {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (SendMessagesHelper.getInstance(a).isImportingStickers()) {
                return true;
            }
        }
        return false;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!hasImportingStickers() && !hasImportingHistory()) {
            stopSelf();
            return Service.START_NOT_STICKY;
        }
        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("start import service");
        }
        if (builder == null) {
            NotificationsController.checkOtherNotificationsChannel();
            builder = new NotificationCompat.Builder(ApplicationLoader.applicationContext);
            builder.setSmallIcon(android.R.drawable.stat_sys_upload);
            builder.setWhen(System.currentTimeMillis());
            builder.setChannelId(NotificationsController.OTHER_NOTIFICATIONS_CHANNEL);
            builder.setContentTitle(LocaleController.getString(R.string.AppName));
            if (hasImportingHistory()) {
                builder.setTicker(LocaleController.getString(R.string.ImporImportingService));
                builder.setContentText(LocaleController.getString(R.string.ImporImportingService));
            } else {
                builder.setTicker(LocaleController.getString(R.string.ImporImportingStickersService));
                builder.setContentText(LocaleController.getString(R.string.ImporImportingStickersService));
            }
        }
        builder.setProgress(100, 0, true);
        try {
            startForeground(5, builder.build());
        } catch (Throwable e) {
            // ForegroundServiceStartNotAllowedException, e.g. when the dataSync time limit is exhausted
            FileLog.e(e);
        }
        NotificationManagerCompat.from(ApplicationLoader.applicationContext).notify(5, builder.build());
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onTimeout(int startId, int fgsType) {
        // dataSync time limit is exhausted (6 hours a day in background since Android 15), the service must stop now
        FileLog.d("ImportingService timeout, type = " + fgsType);
        try {
            stopForeground(true);
        } catch (Throwable ignore) {}
        stopSelf();
    }
}
