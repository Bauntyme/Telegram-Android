package org.telegram.messenger;

import android.text.TextUtils;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

public class AeroTextUtils {

    /**
     * Converts standard Uzbek Latin digraphs/apostrophes into the 2027-proposal alphabet
     * characters shown in the UI (sh -> ş, ch -> ç, o‘/o' -> ō, g‘/g' -> ḡ, ng -> ñ).
     */
    public static String convertNewUzbekAlphabet(String input) {
        if (TextUtils.isEmpty(input)) {
            return input;
        }
        String out = input
            .replace("O‘", "Ō").replace("Oʻ", "Ō").replace("O'", "Ō").replace("O`", "Ō")
            .replace("o‘", "ō").replace("oʻ", "ō").replace("o'", "ō").replace("o`", "ō")
            .replace("G‘", "Ḡ").replace("Gʻ", "Ḡ").replace("G'", "Ḡ").replace("G`", "Ḡ")
            .replace("g‘", "ḡ").replace("gʻ", "ḡ").replace("g'", "ḡ").replace("g`", "ḡ")
            .replace("SH", "Ş").replace("Sh", "Ş").replace("sh", "ş")
            .replace("CH", "Ç").replace("Ch", "Ç").replace("ch", "ç");
        return out;
    }

    public static String applyOutgoingDecorations(String message) {
        if (message == null) {
            return null;
        }
        String result = message;
        if (AeroConfig.newUzbekAlphabet) {
            result = convertNewUzbekAlphabet(result);
        }
        if (AeroConfig.autoPostfixEnabled && !TextUtils.isEmpty(AeroConfig.autoPostfixText)) {
            result = result + " " + AeroConfig.autoPostfixText.trim();
        }
        if (AeroConfig.heartEffect && !result.endsWith("❤️")) {
            result = result + " ❤️";
        }
        return result;
    }

    public static ArrayList<TLRPC.MessageEntity> applyTextStyle(String text, ArrayList<TLRPC.MessageEntity> existingEntities, int styleMode) {
        if (TextUtils.isEmpty(text) || styleMode == AeroConfig.STYLE_NONE) {
            return existingEntities;
        }
        TLRPC.MessageEntity entity;
        switch (styleMode) {
            case AeroConfig.STYLE_BOLD:
                entity = new TLRPC.TL_messageEntityBold();
                break;
            case AeroConfig.STYLE_ITALIC:
                entity = new TLRPC.TL_messageEntityItalic();
                break;
            case AeroConfig.STYLE_MONO:
                entity = new TLRPC.TL_messageEntityCode();
                break;
            case AeroConfig.STYLE_STRIKE:
                entity = new TLRPC.TL_messageEntityStrike();
                break;
            case AeroConfig.STYLE_UNDERLINE:
                entity = new TLRPC.TL_messageEntityUnderline();
                break;
            case AeroConfig.STYLE_SPOILER:
                entity = new TLRPC.TL_messageEntitySpoiler();
                break;
            default:
                return existingEntities;
        }
        entity.offset = 0;
        entity.length = text.length();
        ArrayList<TLRPC.MessageEntity> result = existingEntities != null ? new ArrayList<>(existingEntities) : new ArrayList<>();
        result.add(entity);
        return result;
    }

    /**
     * Checks whether an incoming user dialog has a phone number from a different country code
     * than the signed-in user (defaulting to Uzbekistan +998 if unavailable) and is not a saved contact.
     */
    public static boolean isForeignNonContact(int account, TLRPC.User sender) {
        if (!AeroConfig.blockForeignNumbers || sender == null || sender.self || sender.contact || sender.bot) {
            return false;
        }
        if (TextUtils.isEmpty(sender.phone)) {
            return false;
        }
        String senderPhone = sender.phone.replaceAll("\\D", "");
        if (senderPhone.isEmpty()) {
            return false;
        }
        TLRPC.User self = UserConfig.getInstance(account).getCurrentUser();
        String selfPhone = self != null && !TextUtils.isEmpty(self.phone) ? self.phone.replaceAll("\\D", "") : "";
        String homePrefix = "998";
        if (selfPhone.length() >= 3) {
            if (selfPhone.startsWith("1") || selfPhone.startsWith("7")) {
                homePrefix = selfPhone.substring(0, 1);
            } else if (selfPhone.startsWith("998") || selfPhone.startsWith("996") || selfPhone.startsWith("992") || selfPhone.startsWith("380")) {
                homePrefix = selfPhone.substring(0, 3);
            } else {
                homePrefix = selfPhone.substring(0, 2);
            }
        }
        return !senderPhone.startsWith(homePrefix);
    }
}
