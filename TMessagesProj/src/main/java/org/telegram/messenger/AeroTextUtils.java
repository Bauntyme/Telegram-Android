package org.telegram.messenger;

import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ClickableSpan;

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
            .replace("O‘", "Ō").replace("Oʻ", "Ō").replace("O'", "Ō").replace("O`", "Ō").replace("O’", "Ō")
            .replace("o‘", "ō").replace("oʻ", "ō").replace("o'", "ō").replace("o`", "ō").replace("o’", "ō")
            .replace("G‘", "Ḡ").replace("Gʻ", "Ḡ").replace("G'", "Ḡ").replace("G`", "Ḡ").replace("G’", "Ḡ")
            .replace("g‘", "ḡ").replace("gʻ", "ḡ").replace("g'", "ḡ").replace("g`", "ḡ").replace("g’", "ḡ")
            .replace("SH", "Ş").replace("Sh", "Ş").replace("sh", "ş")
            .replace("CH", "Ç").replace("Ch", "Ç").replace("ch", "ç");
        return out;
    }

    /**
     * Applies 2027-proposal Uzbek Latin digraph replacements in-place on a SpannableStringBuilder
     * AFTER entities have been applied, skipping clickable links/@mentions so spans stay aligned.
     */
    public static void applyNewUzbekAlphabetInPlace(SpannableStringBuilder sb) {
        if (sb == null || sb.length() < 2) {
            return;
        }
        int i = 0;
        while (i < sb.length() - 1) {
            ClickableSpan[] links = sb.getSpans(i, i + 2, ClickableSpan.class);
            if (links != null && links.length > 0) {
                i++;
                continue;
            }
            char c1 = sb.charAt(i);
            char c2 = sb.charAt(i + 1);
            String repl = null;
            if ((c1 == 'O' || c1 == 'o') && (c2 == '‘' || c2 == 'ʻ' || c2 == '\'' || c2 == '`' || c2 == '’')) {
                repl = (c1 == 'O') ? "Ō" : "ō";
            } else if ((c1 == 'G' || c1 == 'g') && (c2 == '‘' || c2 == 'ʻ' || c2 == '\'' || c2 == '`' || c2 == '’')) {
                repl = (c1 == 'G') ? "Ḡ" : "ḡ";
            } else if ((c1 == 'S' || c1 == 's') && (c2 == 'H' || c2 == 'h')) {
                repl = (c1 == 'S') ? "Ş" : "ş";
            } else if ((c1 == 'C' || c1 == 'c') && (c2 == 'H' || c2 == 'h')) {
                repl = (c1 == 'C') ? "Ç" : "ç";
            }
            if (repl != null) {
                sb.replace(i, i + 2, repl);
            }
            i++;
        }
    }

    public static String convertUzbekLatinToCyrillic(String input) {
        if (TextUtils.isEmpty(input)) {
            return input;
        }
        String s = input
            .replace("O‘", "Ў").replace("Oʻ", "Ў").replace("O'", "Ў").replace("O`", "Ў").replace("O’", "Ў").replace("Ō", "Ў")
            .replace("o‘", "ў").replace("oʻ", "ў").replace("o'", "ў").replace("o`", "ў").replace("o’", "ў").replace("ō", "ў")
            .replace("G‘", "Ғ").replace("Gʻ", "Ғ").replace("G'", "Ғ").replace("G`", "Ғ").replace("G’", "Ғ").replace("Ḡ", "Ғ")
            .replace("g‘", "ғ").replace("gʻ", "ғ").replace("g'", "ғ").replace("g`", "ғ").replace("g’", "ғ").replace("ḡ", "ғ")
            .replace("SH", "Ш").replace("Sh", "Ш").replace("sh", "ш").replace("Ş", "Ш").replace("ş", "ш")
            .replace("CH", "Ч").replace("Ch", "Ч").replace("ch", "ч").replace("Ç", "Ч").replace("ç", "ч")
            .replace("NG", "НГ").replace("Ng", "Нг").replace("ng", "нг")
            .replace("YO", "Ё").replace("Yo", "Ё").replace("yo", "ё")
            .replace("YU", "Ю").replace("Yu", "Ю").replace("yu", "ю")
            .replace("YA", "Я").replace("Ya", "Я").replace("ya", "я")
            .replace("YE", "Е").replace("Ye", "Е").replace("ye", "е")
            .replace("TS", "Ц").replace("Ts", "Ц").replace("ts", "ц");
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case 'A': out.append('А'); break;
                case 'a': out.append('а'); break;
                case 'B': out.append('Б'); break;
                case 'b': out.append('б'); break;
                case 'D': out.append('Д'); break;
                case 'd': out.append('д'); break;
                case 'E': out.append('Э'); break;
                case 'e': out.append('э'); break;
                case 'F': out.append('Ф'); break;
                case 'f': out.append('ф'); break;
                case 'G': out.append('Г'); break;
                case 'g': out.append('г'); break;
                case 'H': out.append('Ҳ'); break;
                case 'h': out.append('ҳ'); break;
                case 'I': out.append('И'); break;
                case 'i': out.append('и'); break;
                case 'J': out.append('Ж'); break;
                case 'j': out.append('ж'); break;
                case 'K': out.append('К'); break;
                case 'k': out.append('к'); break;
                case 'L': out.append('Л'); break;
                case 'l': out.append('л'); break;
                case 'M': out.append('М'); break;
                case 'm': out.append('м'); break;
                case 'N': out.append('Н'); break;
                case 'n': out.append('н'); break;
                case 'O': out.append('О'); break;
                case 'o': out.append('о'); break;
                case 'P': out.append('П'); break;
                case 'p': out.append('п'); break;
                case 'Q': out.append('Қ'); break;
                case 'q': out.append('қ'); break;
                case 'R': out.append('Р'); break;
                case 'r': out.append('р'); break;
                case 'S': out.append('С'); break;
                case 's': out.append('с'); break;
                case 'T': out.append('Т'); break;
                case 't': out.append('т'); break;
                case 'U': out.append('У'); break;
                case 'u': out.append('у'); break;
                case 'V': out.append('В'); break;
                case 'v': out.append('в'); break;
                case 'X': out.append('Х'); break;
                case 'x': out.append('х'); break;
                case 'Y': out.append('Й'); break;
                case 'y': out.append('й'); break;
                case 'Z': out.append('З'); break;
                case 'z': out.append('з'); break;
                case '‘': case 'ʻ': case '\'': case '’': out.append('ъ'); break;
                default: out.append(c); break;
            }
        }
        return out.toString();
    }

    public static String convertUzbekCyrillicToLatin(String input) {
        if (TextUtils.isEmpty(input)) {
            return input;
        }
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case 'А': out.append('A'); break;
                case 'а': out.append('a'); break;
                case 'Б': out.append('B'); break;
                case 'б': out.append('b'); break;
                case 'В': out.append('V'); break;
                case 'в': out.append('v'); break;
                case 'Г': out.append('G'); break;
                case 'г': out.append('g'); break;
                case 'Д': out.append('D'); break;
                case 'д': out.append('d'); break;
                case 'Е': out.append(i == 0 || "аеёиоуўэюяАЕЁИОУЎЭЮЯ ".indexOf(input.charAt(i - 1)) >= 0 ? "Ye" : "E"); break;
                case 'е': out.append(i == 0 || "аеёиоуўэюяАЕЁИОУЎЭЮЯ ".indexOf(input.charAt(i - 1)) >= 0 ? "ye" : "e"); break;
                case 'Ё': out.append("Yo"); break;
                case 'ё': out.append("yo"); break;
                case 'Ж': out.append('J'); break;
                case 'ж': out.append('j'); break;
                case 'З': out.append('Z'); break;
                case 'з': out.append('z'); break;
                case 'И': out.append('I'); break;
                case 'и': out.append('i'); break;
                case 'Й': out.append('Y'); break;
                case 'й': out.append('y'); break;
                case 'К': out.append('K'); break;
                case 'к': out.append('k'); break;
                case 'Қ': out.append('Q'); break;
                case 'қ': out.append('q'); break;
                case 'Л': out.append('L'); break;
                case 'л': out.append('l'); break;
                case 'М': out.append('M'); break;
                case 'м': out.append('m'); break;
                case 'Н': out.append('N'); break;
                case 'н': out.append('n'); break;
                case 'О': out.append('O'); break;
                case 'о': out.append('o'); break;
                case 'Ў': out.append("O‘"); break;
                case 'ў': out.append("o‘"); break;
                case 'П': out.append('P'); break;
                case 'п': out.append('p'); break;
                case 'Р': out.append('R'); break;
                case 'р': out.append('r'); break;
                case 'С': out.append('S'); break;
                case 'с': out.append('s'); break;
                case 'Т': out.append('T'); break;
                case 'т': out.append('t'); break;
                case 'У': out.append('U'); break;
                case 'у': out.append('u'); break;
                case 'Ф': out.append('F'); break;
                case 'ф': out.append('f'); break;
                case 'Х': out.append('X'); break;
                case 'х': out.append('x'); break;
                case 'Ҳ': out.append('H'); break;
                case 'ҳ': out.append('h'); break;
                case 'Ц': out.append("Ts"); break;
                case 'ц': out.append("ts"); break;
                case 'Ч': out.append("Ch"); break;
                case 'ч': out.append("ch"); break;
                case 'Ш': out.append("Sh"); break;
                case 'ш': out.append("sh"); break;
                case 'Ғ': out.append("G‘"); break;
                case 'ғ': out.append("g‘"); break;
                case 'Ъ': case 'ъ': out.append('’'); break;
                case 'Ь': case 'ь': break;
                case 'Э': out.append('E'); break;
                case 'э': out.append('e'); break;
                case 'Ю': out.append("Yu"); break;
                case 'ю': out.append("yu"); break;
                case 'Я': out.append("Ya"); break;
                case 'я': out.append("ya"); break;
                default: out.append(c); break;
            }
        }
        return out.toString();
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
