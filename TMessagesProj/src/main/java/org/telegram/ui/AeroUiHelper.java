package org.telegram.ui;

import android.content.Context;
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.AeroConfig;
import org.telegram.messenger.AeroEditHistoryStore;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AeroUiHelper {

    /**
     * AeroGram Unique Feature #1: Confirms before initiating a voice or video call
     * when AeroConfig.confirmBeforeCall is enabled.
     */
    public static void confirmAndStartCall(Context context, String targetName, boolean isVideo, Runnable action) {
        if (action == null) {
            return;
        }
        if (!AeroConfig.confirmBeforeCall || context == null) {
            action.run();
            return;
        }
        String name = TextUtils.isEmpty(targetName) ? "suhbatdoshga" : targetName;
        String callKind = isVideo ? "Video qo‘ng‘iroq" : "Ovozli qo‘ng‘iroq";
        new AlertDialog.Builder(context)
            .setTitle(callKind + "ni tasdiqlash")
            .setMessage(name + " bilan " + callKind.toLowerCase(Locale.ROOT) + " boshlansinmi?")
            .setPositiveButton("Qo‘ng‘iroq qilish", (d, w) -> action.run())
            .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
            .show();
    }

    /**
     * Asks confirmation before sending stickers/GIFs when confirmBeforeSendMedia is enabled.
     */
    public static boolean maybeConfirmSendMedia(Context context, String label, Runnable onConfirm) {
        if (!AeroConfig.confirmBeforeSendMedia || context == null || onConfirm == null) {
            return false;
        }
        String kind = TextUtils.isEmpty(label) ? "media" : label;
        new AlertDialog.Builder(context)
            .setTitle("Yuborishni tasdiqlash")
            .setMessage("Ushbu " + kind + " yuborilsinmi?")
            .setPositiveButton(LocaleController.getString(R.string.Send), (d, w) -> onConfirm.run())
            .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
            .show();
        return true;
    }

    /**
     * Shows a timeline dialog containing all previously saved edited versions of a message.
     */
    public static void showEditHistoryDialog(Context context, MessageObject msg) {
        if (context == null || msg == null) {
            return;
        }
        ArrayList<AeroEditHistoryStore.EditEntry> entries = AeroEditHistoryStore.getHistory(
            msg.currentAccount, msg.getDialogId(), msg.getId()
        );

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(18);
        container.setPadding(pad, AndroidUtilities.dp(10), pad, AndroidUtilities.dp(6));

        SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
        StringBuilder copyAll = new StringBuilder();

        if (entries.isEmpty()) {
            TextView tv = new TextView(context);
            tv.setText("Bu xabar bo‘yicha saqlangan tahrirlar topilmadi.\n(Tahrirlar tarixi yoqilgandan keyingi o‘zgarishlar saqlanadi.)");
            tv.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
            tv.setTextSize(14);
            container.addView(tv);
        } else {
            for (int i = entries.size() - 1; i >= 0; i--) {
                AeroEditHistoryStore.EditEntry entry = entries.get(i);
                TextView timeTv = new TextView(context);
                String timeText = entry.date > 0
                    ? fmt.format(new Date((long) entry.date * 1000L))
                    : ("Versiya #" + (i + 1));
                timeTv.setText("• " + timeText);
                timeTv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
                timeTv.setTextSize(12);
                timeTv.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(2));
                container.addView(timeTv);

                TextView bodyTv = new TextView(context);
                bodyTv.setText(entry.text);
                bodyTv.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
                bodyTv.setTextSize(15);
                bodyTv.setTextIsSelectable(true);
                container.addView(bodyTv);

                if (copyAll.length() > 0) copyAll.append("\n---\n");
                copyAll.append(timeText).append(":\n").append(entry.text);
            }
        }

        ScrollView scroll = new ScrollView(context);
        scroll.addView(container);

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
            .setTitle("Tahrirlar tarixi")
            .setView(scroll)
            .setPositiveButton(LocaleController.getString(R.string.OK), null);

        if (copyAll.length() > 0) {
            final String clipText = copyAll.toString();
            builder.setNeutralButton(LocaleController.getString(R.string.Copy), (d, w) -> {
                AndroidUtilities.addToClipboard(clipText);
            });
        }
        builder.show();
    }

    /**
     * Camera selector dialog when long-pressing video-note button with cameraSwitchOnHold enabled.
     */
    public static void showCameraChoiceDialog(Context context, Runnable onSelected) {
        if (context == null) {
            return;
        }
        String[] options = new String[] {
            "Old kamera (Selfi)" + (!AeroConfig.preferBackCamera ? " ✓" : ""),
            "Orqa kamera (Asosiy)" + (AeroConfig.preferBackCamera ? " ✓" : "")
        };
        new AlertDialog.Builder(context)
            .setTitle("Video-xabar kamerasini tanlang")
            .setItems(options, (d, which) -> {
                AeroConfig.preferBackCamera = (which == 1);
                AeroConfig.saveBool("preferBackCamera", AeroConfig.preferBackCamera);
                if (onSelected != null) {
                    onSelected.run();
                }
            })
            .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
            .show();
    }
}
