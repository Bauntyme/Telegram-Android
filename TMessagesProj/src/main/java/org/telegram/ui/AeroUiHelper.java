package org.telegram.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.telegram.messenger.AeroConfig;
import org.telegram.messenger.AeroEditHistoryStore;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.TranslateController;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;

import java.io.File;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AeroUiHelper {

    public static void confirmAndStartCall(Context context, String targetName, boolean isVideo, Runnable proceed) {
        if (context == null || proceed == null) {
            return;
        }
        if (!AeroConfig.confirmBeforeCall) {
            proceed.run();
            return;
        }
        String name = TextUtils.isEmpty(targetName) ? AeroConfig.tr("foydalanuvchi", "фойдаланувчи") : targetName;
        String title = isVideo
            ? AeroConfig.tr("Video qo‘ng‘iroqni tasdiqlang", "Видео қўнғироқни тасдиқланг")
            : AeroConfig.tr("Qo‘ng‘iroqni tasdiqlang", "Қўнғироқни тасдиқланг");
        String actionText = isVideo
            ? AeroConfig.tr("Video qo‘ng‘iroq qilish", "Видео қўнғироқ қилиш")
            : AeroConfig.tr("Qo‘ng‘iroq qilish", "Қўнғироқ қилиш");
        new AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(AeroConfig.tr(name + " ga qo‘ng‘iroq qilinsinmi?", name + " га қўнғироқ қилинсинми?"))
            .setPositiveButton(actionText, (d, w) -> proceed.run())
            .setNegativeButton(AeroConfig.tr("Bekor qilish", "Бекор қилиш"), null)
            .show();
    }

    public static boolean maybeConfirmSendMedia(Context context, String label, Runnable proceed) {
        if (context == null || proceed == null) {
            return false;
        }
        if (!AeroConfig.confirmSendMedia) {
            return false;
        }
        new AlertDialog.Builder(context)
            .setTitle(AeroConfig.tr("Yuborishni tasdiqlang", "Юборишни тасдиқланг"))
            .setMessage(AeroConfig.tr(
                "Haqiqatan ham ushbu " + label + "ni yubormoqchimisiz?",
                "Ҳақиқатан ҳам ушбу " + label + "ни юбормоқчимисиз?"
            ))
            .setPositiveButton(AeroConfig.tr("Yuborish", "Юбориш"), (d, w) -> proceed.run())
            .setNegativeButton(AeroConfig.tr("Bekor qilish", "Бекор қилиш"), null)
            .show();
        return true;
    }

    /**
     * Shows chronological edit history: Oldest (Eski) at the top down to Yangi (Hozirgi) at the bottom ("pasida").
     */
    public static void showEditHistoryDialog(Context context, MessageObject msg) {
        if (context == null || msg == null) {
            return;
        }
        ArrayList<AeroEditHistoryStore.EditEntry> entries = AeroEditHistoryStore.getHistory(
            msg.currentAccount, msg.getDialogId(), msg.getId()
        );
        String currentText = msg.messageOwner != null && !TextUtils.isEmpty(msg.messageOwner.message)
            ? msg.messageOwner.message
            : "";

        if (entries.isEmpty()) {
            new AlertDialog.Builder(context)
                .setTitle(AeroConfig.tr("Tahrirlar tarixi", "Таҳрирлар тарихи"))
                .setMessage(AeroConfig.tr(
                    "Bu xabar uchun tahrir tarixi hali yozilmagan.\n\nHozirgi matn:\n" + (TextUtils.isEmpty(currentText) ? "(matn yo‘q)" : currentText),
                    "Бу хабар учун таҳрир тарихи ҳали ёзилмаган.\n\nҲозирги матн:\n" + (TextUtils.isEmpty(currentText) ? "(матн йўқ)" : currentText)
                ))
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        SimpleDateFormat fmt = new SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            AeroEditHistoryStore.EditEntry entry = entries.get(i);
            String timeStr = entry.editDate > 0 ? fmt.format(new Date((long) entry.editDate * 1000L)) : "Avvalgi";
            sb.append("📝 ");
            if (entries.size() == 1) {
                sb.append(AeroConfig.tr("Eski matn", "Эски матн"));
            } else {
                sb.append(AeroConfig.tr("Eski #", "Эски #")).append(i + 1);
            }
            sb.append(" (").append(timeStr).append("):\n");
            sb.append(TextUtils.isEmpty(entry.text) ? "(bo‘sh)" : entry.text);
            sb.append("\n\n──────────────\n\n");
        }
        String editTimeStr = msg.messageOwner != null && msg.messageOwner.edit_date > 0
            ? fmt.format(new Date((long) msg.messageOwner.edit_date * 1000L))
            : AeroConfig.tr("Hozirgi", "Ҳозирги");
        sb.append("✨ ").append(AeroConfig.tr("Yangi (Hozirgi)", "Янги (Ҳозирги)")).append(" (").append(editTimeStr).append("):\n");
        sb.append(TextUtils.isEmpty(currentText) ? "(bo‘sh)" : currentText);

        final String oldestText = entries.get(0).text;
        final String fullTimeline = sb.toString();

        new AlertDialog.Builder(context)
            .setTitle(AeroConfig.tr("Tahrirlar tarixi (Eski → Yangi)", "Таҳрирлар тарихи (Эски → Янги)"))
            .setMessage(fullTimeline)
            .setPositiveButton("OK", null)
            .setNeutralButton(AeroConfig.tr("Eskisini nusxalash", "Эскисини нусхалаш"), (d, w) -> {
                AndroidUtilities.addToClipboard(oldestText);
                Toast.makeText(context, AeroConfig.tr("Eski matn nusxalandi", "Эски матн нусхаланди"), Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(AeroConfig.tr("Yangisini nusxalash", "Янгисини нусхалаш"), (d, w) -> {
                AndroidUtilities.addToClipboard(currentText);
                Toast.makeText(context, AeroConfig.tr("Yangi matn nusxalandi", "Янги матн нусхаланди"), Toast.LENGTH_SHORT).show();
            })
            .show();
    }

    public static void showCameraChoiceDialog(Context context, Runnable onCameraModeChanged) {
        if (context == null) {
            return;
        }
        CharSequence[] items = new CharSequence[]{
            AeroConfig.tr("Oldingi kamera (Selfie)", "Олдинги камера (Селфи)"),
            AeroConfig.tr("Orqa kamera (Asosiy)", "Орқа камера (Асосий)")
        };
        new AlertDialog.Builder(context)
            .setTitle(AeroConfig.tr("Dumaloq video kamerasi", "Думалоқ видео камераси"))
            .setItems(items, (d, which) -> {
                boolean preferBack = (which == 1);
                AeroConfig.preferBackCamera = preferBack;
                AeroConfig.saveBool("preferBackCamera", preferBack);
                if (onCameraModeChanged != null) {
                    onCameraModeChanged.run();
                }
                Toast.makeText(context,
                    preferBack
                        ? AeroConfig.tr("Orqa kamera tanlandi", "Орқа камера танланди")
                        : AeroConfig.tr("Oldingi kamera tanlandi", "Олдинги камера танланди"),
                    Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(AeroConfig.tr("Bekor qilish", "Бекор қилиш"), null)
            .show();
    }

    public static void promptPinDialog(Context context, String title, Runnable onUnlocked) {
        if (context == null || onUnlocked == null) {
            return;
        }
        if (TextUtils.isEmpty(AeroConfig.chatLockPin)) {
            showSetPinDialog(context, () -> promptPinDialog(context, title, onUnlocked));
            return;
        }
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(20);
        box.setPadding(pad, AndroidUtilities.dp(8), pad, AndroidUtilities.dp(4));

        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setTransformationMethod(PasswordTransformationMethod.getInstance());
        input.setHint(AeroConfig.tr("4 xonali PIN kodni kiriting", "4 хонали ПИН кодни киритинг"));
        input.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        input.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        input.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        box.addView(input);

        new AlertDialog.Builder(context)
            .setTitle(title)
            .setView(box)
            .setPositiveButton(AeroConfig.tr("Ochish", "Очиш"), (d, w) -> {
                String val = input.getText() == null ? "" : input.getText().toString().trim();
                if (TextUtils.equals(val, AeroConfig.chatLockPin)) {
                    onUnlocked.run();
                } else {
                    Toast.makeText(context, AeroConfig.tr("PIN kod noto‘g‘ri!", "ПИН код нотўғри!"), Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(AeroConfig.tr("Bekor qilish", "Бекор қилиш"), null)
            .show();
    }

    public static void showSetPinDialog(Context context, Runnable onSaved) {
        if (context == null) {
            return;
        }
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(20);
        box.setPadding(pad, AndroidUtilities.dp(8), pad, AndroidUtilities.dp(4));

        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setTransformationMethod(PasswordTransformationMethod.getInstance());
        input.setHint(AeroConfig.tr("Yangi PIN kod (kamida 4 raqam)", "Янги ПИН код (камида 4 рақам)"));
        input.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        input.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        box.addView(input);

        new AlertDialog.Builder(context)
            .setTitle(AeroConfig.tr("AeroGram Chat PIN kodi", "AeroGram Чат ПИН коди"))
            .setView(box)
            .setPositiveButton(AeroConfig.tr("Saqlash", "Сақлаш"), (d, w) -> {
                String val = input.getText() == null ? "" : input.getText().toString().trim();
                if (val.length() < 4) {
                    Toast.makeText(context, AeroConfig.tr("Kamida 4 ta raqam kiriting", "Камида 4 та рақам киритинг"), Toast.LENGTH_SHORT).show();
                    return;
                }
                AeroConfig.chatLockPin = val;
                AeroConfig.saveString("chatLockPin", val);
                AeroConfig.chatLockEnabled = true;
                AeroConfig.saveBool("chatLockEnabled", true);
                Toast.makeText(context, AeroConfig.tr("PIN kod saqlandi", "ПИН код сақланди"), Toast.LENGTH_SHORT).show();
                if (onSaved != null) {
                    onSaved.run();
                }
            })
            .setNegativeButton(AeroConfig.tr("Bekor qilish", "Бекор қилиш"), null)
            .show();
    }

    public static void translateOutgoingIfNeeded(
            int account,
            Context context,
            String rawText,
            Utilities.Callback<String> onFinalText
    ) {
        if (onFinalText == null) {
            return;
        }
        if (TextUtils.isEmpty(rawText) || rawText.startsWith("/") || (!AeroConfig.autoTranslateOutgoing && !AeroConfig.askBeforeTranslateSend)) {
            onFinalText.run(rawText);
            return;
        }
        final Runnable doTranslate = () -> {
            TLRPC.TL_messages_translateText req = new TLRPC.TL_messages_translateText();
            req.flags |= 2;
            TLRPC.TL_textWithEntities t = new TLRPC.TL_textWithEntities();
            t.text = rawText;
            req.text.add(t);
            req.to_lang = TranslateController.normalizeLanguage(
                TextUtils.isEmpty(AeroConfig.translateTargetLang) ? "en" : AeroConfig.translateTargetLang
            );
            ConnectionsManager.getInstance(account).sendRequest(req, (res, err) -> {
                String resultText = rawText;
                if (res instanceof TLRPC.TL_messages_translateResult) {
                    ArrayList<TLRPC.TL_textWithEntities> list = ((TLRPC.TL_messages_translateResult) res).result;
                    if (list != null && !list.isEmpty() && !TextUtils.isEmpty(list.get(0).text)) {
                        resultText = list.get(0).text;
                    }
                }
                final String finalOut = resultText;
                AndroidUtilities.runOnUIThread(() -> onFinalText.run(finalOut));
            });
        };

        if (AeroConfig.askBeforeTranslateSend && context != null) {
            String lang = TextUtils.isEmpty(AeroConfig.translateTargetLang) ? "en" : AeroConfig.translateTargetLang.toUpperCase(Locale.US);
            new AlertDialog.Builder(context)
                .setTitle(AeroConfig.tr("Tarjima qilib yuborilsinmi?", "Таржима қилиб юборилсинми?"))
                .setMessage(AeroConfig.tr(
                    "Xabarni [" + lang + "] tiliga tarjima qilib yuborishni xohlaysizmi?\n\nMatn: " + rawText,
                    "Хабарни [" + lang + "] тилига таржима қилиб юборишни хоҳлайсизми?\n\nМатн: " + rawText
                ))
                .setPositiveButton(AeroConfig.tr("Tarjima qilib yuborish", "Таржима қилиб юбориш"), (d, w) -> doTranslate.run())
                .setNegativeButton(AeroConfig.tr("Asl holicha yuborish", "Асл ҳолича юбориш"), (d, w) -> onFinalText.run(rawText))
                .show();
        } else {
            doTranslate.run();
        }
    }

    public static void saveMessageToSavedMessages(int currentAccount, BaseFragment fragment, MessageObject msg) {
        AeroOnceSaver.saveToSavedMessages(currentAccount, fragment, msg);
    }

    private static long lastReminderScanMs = 0L;
    private static long lastReminderShownMs = 0L;

    public static void checkUnreadReminders(int currentAccount) {
        if (!AeroConfig.unreadReminderEnabled || ApplicationLoader.applicationContext == null) {
            return;
        }
        long nowMs = SystemClock.elapsedRealtime();
        if (lastReminderScanMs != 0 && nowMs - lastReminderScanMs < 45_000L) {
            return;
        }
        lastReminderScanMs = nowMs;

        int delayMin = Math.max(1, AeroConfig.unreadReminderMinutes);
        long cooldownMs = delayMin * 60_000L;
        if (lastReminderShownMs == 0) {
            lastReminderShownMs = nowMs - Math.max(0L, cooldownMs - 60_000L);
            return;
        }
        if (nowMs - lastReminderShownMs < cooldownMs) {
            return;
        }

        MessagesController mc = MessagesController.getInstance(currentAccount);
        if (mc == null) {
            return;
        }
        ArrayList<TLRPC.Dialog> dialogs = mc.getAllDialogs();
        if (dialogs == null || dialogs.isEmpty()) {
            return;
        }
        int nowSec = ConnectionsManager.getInstance(currentAccount).getCurrentTime();
        int thresholdSec = delayMin * 60;
        int maxAgeSec = 48 * 3600;
        int overdueCount = 0;

        for (int i = 0, size = dialogs.size(); i < size; i++) {
            TLRPC.Dialog d = dialogs.get(i);
            if (d == null || d.unread_count <= 0) {
                continue;
            }
            if (mc.isDialogMuted(d.id, 0)) {
                continue;
            }
            int ageSec = nowSec - d.last_message_date;
            if (ageSec >= thresholdSec && ageSec <= maxAgeSec) {
                overdueCount++;
            }
        }

        if (overdueCount <= 0) {
            return;
        }
        lastReminderShownMs = nowMs;
        final int finalCount = overdueCount;

        AndroidUtilities.runOnUIThread(() -> {
            String msgText = AeroConfig.tr(
                "⏰ " + finalCount + " ta o‘qilmagan chatda javob kutilmoqda",
                "⏰ " + finalCount + " та ўқилмаган чатда жавоб кутилмоқда"
            );
            try {
                BaseFragment lastFragment = LaunchActivity.getLastFragment();
                if (lastFragment != null && lastFragment.getParentActivity() != null && lastFragment.getFragmentView() != null) {
                    BulletinFactory.of(lastFragment).createImageBulletin(R.drawable.msg_notifications, msgText).show();
                } else {
                    Toast.makeText(ApplicationLoader.applicationContext, msgText, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception ignore) {
                Toast.makeText(ApplicationLoader.applicationContext, msgText, Toast.LENGTH_SHORT).show();
            }

            if (AeroConfig.unreadReminderSound) {
                try {
                    Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    if (alarmUri != null) {
                        Ringtone ringtone = RingtoneManager.getRingtone(ApplicationLoader.applicationContext, alarmUri);
                        if (ringtone != null) {
                            ringtone.play();
                        }
                    }
                } catch (Exception ignore) {}
            }
        });
    }

    private static MediaRecorder callRecorder;
    private static File callRecordFile;

    public static synchronized void startAeroCallRecording(Context context, String peerName) {
        if (!AeroConfig.recordCallsEnabled || context == null || callRecorder != null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 23 && context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            File baseDir = null;
            if (Build.VERSION.SDK_INT < 29) {
                baseDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "AeroGramCalls");
            }
            if (baseDir == null || (!baseDir.exists() && !baseDir.mkdirs())) {
                File ext = context.getExternalFilesDir("AeroGramCalls");
                baseDir = ext != null ? ext : new File(context.getFilesDir(), "AeroGramCalls");
            }
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }
            String safePeer = TextUtils.isEmpty(peerName) ? "call" : peerName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
            if (safePeer.length() > 24) {
                safePeer = safePeer.substring(0, 24);
            }
            String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            callRecordFile = new File(baseDir, "AeroCall_" + safePeer + "_" + stamp + ".m4a");

            MediaRecorder recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(64000);
            recorder.setAudioSamplingRate(32000);
            recorder.setOutputFile(callRecordFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();
            callRecorder = recorder;
        } catch (Throwable t) {
            FileLog.e(t);
            releaseRecorderQuietly();
            if (callRecordFile != null && callRecordFile.exists()) {
                try {
                    callRecordFile.delete();
                } catch (Exception ignore) {}
            }
            callRecordFile = null;
        }
    }

    public static synchronized void stopAeroCallRecording(Context context) {
        if (callRecorder == null) {
            return;
        }
        try {
            callRecorder.stop();
        } catch (Throwable ignore) {}
        releaseRecorderQuietly();

        final File saved = callRecordFile;
        callRecordFile = null;
        if (saved != null && saved.exists() && saved.length() > 1024) {
            if (context != null) {
                try {
                    MediaScannerConnection.scanFile(context.getApplicationContext(), new String[]{saved.getAbsolutePath()}, null, null);
                } catch (Exception ignore) {}
                AndroidUtilities.runOnUIThread(() ->
                    Toast.makeText(
                        context.getApplicationContext(),
                        AeroConfig.tr("🎙 Qo‘ng‘iroq yozuvi saqlandi: ", "🎙 Қўнғироқ ёзуви сақланди: ") + saved.getName(),
                        Toast.LENGTH_LONG
                    ).show()
                );
            }
        } else if (saved != null && saved.exists()) {
            try {
                saved.delete();
            } catch (Exception ignore) {}
        }
    }

    private static void releaseRecorderQuietly() {
        if (callRecorder != null) {
            try {
                callRecorder.reset();
            } catch (Throwable ignore) {}
            try {
                callRecorder.release();
            } catch (Throwable ignore) {}
            callRecorder = null;
        }
    }

    public static void notifyLightBrandAccentChanged() {
        boolean isDark = Theme.getCurrentTheme() != null && Theme.getCurrentTheme().isDark();
        AndroidUtilities.runOnUIThread(() ->
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme, isDark, true)
        );
    }
}
