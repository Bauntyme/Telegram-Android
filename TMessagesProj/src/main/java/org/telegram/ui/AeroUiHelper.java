package org.telegram.ui;

import android.content.Context;
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
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.TranslateController;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;

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
}
