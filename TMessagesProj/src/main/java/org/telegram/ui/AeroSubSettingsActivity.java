package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AeroConfig;
import org.telegram.messenger.AeroDeletedStore;
import org.telegram.messenger.AeroEditHistoryStore;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class AeroSubSettingsActivity extends UniversalFragment {

    public static final int MODE_ABOUT = 0;
    public static final int MODE_STATS = 1;
    public static final int MODE_TRANSLATE_LANG = 2;
    public static final int MODE_BOTS = 3;
    public static final int MODE_ACCOUNTS = 4;
    public static final int MODE_HIDDEN_CHATS = 5;
    public static final int MODE_LOCKED_CHATS = 6;
    public static final int MODE_STT_LANG = 7;
    public static final int MODE_DOUBLE_TAP = 8;
    public static final int MODE_TEXT_STYLE = 9;
    public static final int MODE_FOLDER_STYLE = 10;
    public static final int MODE_REMINDER_DELAY = 11;

    private final int mode;
    private boolean fetchingStats = false;

    private static final String[][] LANGUAGES = {
        {"uz", "O‘zbekcha (Uzbek)", "Ўзбекча (Uzbek)"},
        {"ru", "Ruscha (Русский)", "Русча (Русский)"},
        {"en", "Inglizcha (English)", "Инглизча (English)"},
        {"tr", "Turkcha (Türkçe)", "Туркча (Türkçe)"},
        {"ar", "Arabcha (العربية)", "Арабча (العربية)"},
        {"kk", "Qozoqcha (Қазақша)", "Қозоқча (Қазақша)"},
        {"tg", "Tojikcha (Тоҷикӣ)", "Тожикча (Тоҷикӣ)"},
        {"ko", "Koreyscha (한국어)", "Корейсча (한국어)"},
        {"zh", "Xitoycha (中文)", "Хитойча (中文)"},
        {"fr", "Fransuzcha (Français)", "Французча (Français)"},
        {"de", "Nemischa (Deutsch)", "Немисча (Deutsch)"}
    };

    private static final String[][] BOTS_VIDEO = {
        {"SaveAsBot", "Instagram, TikTok, Pinterest yuklovchi", "Instagram, TikTok, Pinterest юкловчи"},
        {"youtubednbot", "YouTube video va audio yuklovchi", "YouTube видео ва аудио юкловчи"},
        {"vkmusic_bot", "Musiqa qidirish va MP3 yuklash", "Мусиқа қидириш ва MP3 юклаш"}
    };

    private static final String[][] BOTS_TOOLS = {
        {"pdfbot", "Rasm va hujjatlarni PDF ga o‘girish", "Расм ва ҳужжатларни PDF га ўгириш"},
        {"qr_code_bot", "Tezkor QR kod generatori", "Тезкор QR код генератори"},
        {"lotinkirillbot", "Lotin ↔ Kirill matn konverteri", "Лотин ↔ Кирилл матн конвертери"}
    };

    private static final String[][] BOTS_AI = {
        {"ChatGPT_General_Bot", "AI yordamchi va kod yozish boti", "AI ёрдамчи ва код ёзиш боти"},
        {"transcribe_bot", "Ovozli xabarlarni matnga o‘girish", "Овозли хабарларни матнга ўгириш"},
        {"TranslateBot", "Ko‘p tilli tezkor tarjimon bot", "Кўп тилли тезкор таржимон бот"}
    };

    public AeroSubSettingsActivity(int mode) {
        this.mode = mode;
    }

    @Override
    protected CharSequence getTitle() {
        switch (mode) {
            case MODE_STATS: return AeroConfig.tr("AeroGram Analitika", "AeroGram Аналитика");
            case MODE_BOTS: return AeroConfig.tr("AeroGram Botlari", "AeroGram Ботлари");
            case MODE_ACCOUNTS: return AeroConfig.tr("Barcha akkauntlar", "Барча аккаунтлар");
            case MODE_HIDDEN_CHATS: return AeroConfig.tr("Yashirin chatlar", "Яширин чатлар");
            case MODE_LOCKED_CHATS: return AeroConfig.tr("Qulflangan chatlar", "Қулфланган чатлар");
            case MODE_TRANSLATE_LANG: return AeroConfig.tr("Tarjima tili", "Таржима тили");
            case MODE_STT_LANG: return AeroConfig.tr("Ovoz → Matn tili", "Овоз → Матн тили");
            case MODE_DOUBLE_TAP: return AeroConfig.tr("Ikki marta bosganda", "Икки марта босганда");
            case ID_TEXT_STYLE_TITLE:
            case MODE_TEXT_STYLE: return AeroConfig.tr("Matn uslubi", "Матн услуби");
            case MODE_FOLDER_STYLE: return AeroConfig.tr("Papkalar uslubi", "Папкалар услуби");
            case MODE_REMINDER_DELAY: return AeroConfig.tr("Eslatma kechikishi", "Эслатма кечикиши");
            default: return AeroConfig.tr("AeroGram haqida", "AeroGram ҳақида");
        }
    }

    private static final int ID_TEXT_STYLE_TITLE = 999;

    @Override
    public void onResume() {
        super.onResume();
        if (mode == MODE_STATS) {
            fetchGitHubDownloadsIfNeeded(false);
        }
    }

    private void reloadList() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    private void fetchGitHubDownloadsIfNeeded(boolean force) {
        long now = System.currentTimeMillis();
        if (fetchingStats || (!force && AeroConfig.cachedCommunityUsers > 0 && now - AeroConfig.cachedCommunityFetchMs < 300_000L)) {
            return;
        }
        fetchingStats = true;
        Utilities.globalQueue.postRunnable(() -> {
            int totalDownloads = 0;
            try {
                URL url = new URL("https://api.github.com/repos/Bauntyme/AeroGram/releases?per_page=10");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();
                    JSONArray releases = new JSONArray(sb.toString());
                    for (int i = 0; i < releases.length(); i++) {
                        JSONObject rel = releases.optJSONObject(i);
                        if (rel == null) continue;
                        JSONArray assets = rel.optJSONArray("assets");
                        if (assets == null) continue;
                        for (int j = 0; j < assets.length(); j++) {
                            JSONObject asset = assets.optJSONObject(j);
                            if (asset != null) {
                                totalDownloads += asset.optInt("download_count", 0);
                            }
                        }
                    }
                }
                conn.disconnect();
            } catch (Exception ignore) {
            }
            final int finalDownloads = Math.max(totalDownloads, 1);
            final boolean hasDownloads = totalDownloads > 0;
            AndroidUtilities.runOnUIThread(() -> {
                fetchingStats = false;
                if (hasDownloads || AeroConfig.cachedCommunityUsers == 0) {
                    AeroConfig.cachedCommunityUsers = finalDownloads;
                    AeroConfig.cachedCommunityFetchMs = System.currentTimeMillis();
                    AeroConfig.saveInt("cachedCommunityUsers", finalDownloads);
                    AeroConfig.saveLong("cachedCommunityFetchMs", AeroConfig.cachedCommunityFetchMs);
                }
                reloadList();
            });
        });
    }

    private View createAnalyticsTopHeader(Context context) {
        LinearLayout wrap = new LinearLayout(context);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setGravity(Gravity.CENTER_HORIZONTAL);
        wrap.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(22), AndroidUtilities.dp(20), AndroidUtilities.dp(18));

        GradientDrawable circle = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{0xFF38BDF8, 0xFF0284C7, 0xFF1E3A8A}
        );
        circle.setCornerRadius(AndroidUtilities.dp(24));

        FrameLayout emblem = new FrameLayout(context);
        emblem.setBackground(circle);
        ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.msg_stats);
        icon.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        emblem.addView(icon, LayoutHelper.createFrame(38, 38, Gravity.CENTER));
        wrap.addView(emblem, LayoutHelper.createLinear(72, 72, Gravity.CENTER_HORIZONTAL));

        TextView title = new TextView(context);
        title.setText(AeroConfig.tr("AeroGram Analitika", "AeroGram Аналитика"));
        title.setTypeface(AndroidUtilities.bold());
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        wrap.addView(title, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 12, 0, 0));

        TextView sub = new TextView(context);
        sub.setText(AeroConfig.tr("Ilovadan foydalanish statistikasi", "Иловадан фойдаланиш статистикаси"));
        sub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
        sub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, getResourceProvider()));
        wrap.addView(sub, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 0));

        return wrap;
    }

    private View createStatMetricCard(Context context, String label, String bigValue, String badgeText, int accentColor, View.OnClickListener onInfoClick) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(14), AndroidUtilities.dp(18), AndroidUtilities.dp(14));

        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView lbl = new TextView(context);
        lbl.setText(label);
        lbl.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
        lbl.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, getResourceProvider()));
        topRow.addView(lbl, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        ImageView infoBtn = new ImageView(context);
        infoBtn.setImageResource(R.drawable.msg_info);
        infoBtn.setColorFilter(new PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_IN));
        infoBtn.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
        infoBtn.setOnClickListener(onInfoClick);
        topRow.addView(infoBtn, LayoutHelper.createLinear(30, 30, Gravity.CENTER_VERTICAL));

        card.addView(topRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout valRow = new LinearLayout(context);
        valRow.setOrientation(LinearLayout.HORIZONTAL);
        valRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView val = new TextView(context);
        val.setText(bigValue);
        val.setTypeface(AndroidUtilities.bold());
        val.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);
        val.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        valRow.addView(val, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        if (!TextUtils.isEmpty(badgeText)) {
            GradientDrawable pill = new GradientDrawable();
            pill.setCornerRadius(AndroidUtilities.dp(8));
            pill.setColor((accentColor & 0x00FFFFFF) | 0x22000000);
            TextView badge = new TextView(context);
            badge.setText(badgeText);
            badge.setTypeface(AndroidUtilities.bold());
            badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            badge.setTextColor(accentColor);
            badge.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(4));
            badge.setBackground(pill);
            valRow.addView(badge);
        }

        card.addView(valRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));
        return card;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (mode == MODE_ABOUT) {
            fillAboutItems(items);
        } else if (mode == MODE_STATS) {
            fillStatsItems(items);
        } else if (mode == MODE_BOTS) {
            fillBotsItems(items);
        } else if (mode == MODE_ACCOUNTS) {
            fillAccountsItems(items);
        } else if (mode == MODE_HIDDEN_CHATS || mode == MODE_LOCKED_CHATS) {
            fillChatToggleList(items, mode == MODE_HIDDEN_CHATS);
        } else if (mode == MODE_TRANSLATE_LANG) {
            fillLanguageItems(items, false);
        } else if (mode == MODE_STT_LANG) {
            fillLanguageItems(items, true);
        } else if (mode == MODE_DOUBLE_TAP) {
            String[] labels = {
                AeroConfig.tr("Tez reaksiya bildirish", "Тез реакция билдириш"),
                AeroConfig.tr("Xabarga javob berish (Reply)", "Хабарга жавоб бериш (Reply)"),
                AeroConfig.tr("Matnni nusxalash (Copy)", "Матнини нусхалаш (Copy)"),
                AeroConfig.tr("Matnni tarjima qilish", "Матнини таржима қилиш"),
                AeroConfig.tr("Hech narsa qilmaslik", "Ҳеч нарса қилмаслик")
            };
            for (int i = 0; i < labels.length; i++) {
                items.add(UItem.asRadio(i, labels[i]).setChecked(AeroConfig.doubleTapAction == i));
            }
        } else if (mode == MODE_TEXT_STYLE) {
            String[] styles = {
                AeroConfig.tr("Oddiy (Stilsiz)", "Оддий (Стилсиз)"),
                AeroConfig.tr("Qalin (Bold)", "Қалин (Bold)"),
                AeroConfig.tr("Kursiv (Italic)", "Курсив (Italic)"),
                "Monoshift (Code)",
                AeroConfig.tr("Chizilgan (Strike)", "Чизилган (Strike)"),
                AeroConfig.tr("Tagchizilgan (Underline)", "Тагчизилган (Underline)"),
                "Yashirin (Spoiler)"
            };
            for (int i = 0; i < styles.length; i++) {
                items.add(UItem.asRadio(i, styles[i]).setChecked(AeroConfig.textStyleMode == i));
            }
        } else if (mode == MODE_FOLDER_STYLE) {
            String[] fStyles = {
                AeroConfig.tr("Faqat matn (Masalan: Barchasi)", "Фақат матн (Масалан: Барчаси)"),
                AeroConfig.tr("Ikonka + Matn (💬 Barchasi)", "Иконка + Матн (💬 Барчаси)"),
                AeroConfig.tr("Faqat ikonka (💬 📁 👤)", "Фақат иконка (💬 📁 👤)")
            };
            for (int i = 0; i < fStyles.length; i++) {
                items.add(UItem.asRadio(i, fStyles[i]).setChecked(AeroConfig.folderStyle == i));
            }
        } else if (mode == MODE_REMINDER_DELAY) {
            int[] mins = {5, 15, 30, 60, 120};
            for (int m : mins) {
                items.add(UItem.asRadio(m, m + AeroConfig.tr(" daqiqadan so‘ng", " дақиқадан сўнг")).setChecked(AeroConfig.unreadReminderMinutes == m));
            }
        }
    }

    private void fillStatsItems(ArrayList<UItem> items) {
        if (getParentActivity() == null) return;
        items.add(UItem.asCustom(10, createAnalyticsTopHeader(getParentActivity())));
        items.add(UItem.asShadow(null));

        int totalDownloads = Math.max(1, AeroConfig.cachedCommunityUsers);
        String downloadsText = totalDownloads + (AeroConfig.uiCyrillic ? " та юклаш" : " ta yuklash");

        items.add(UItem.asCustom(11, createStatMetricCard(
            getParentActivity(),
            AeroConfig.tr("AeroGram foydalanuvchilari (Reliz yuklashlari)", "AeroGram фойдаланувчилари (Релиз юклашлари)"),
            downloadsText,
            "GitHub Live",
            0xFF0EA5E9,
            v -> new AlertDialog.Builder(getParentActivity())
                .setTitle(AeroConfig.tr("AeroGram foydalanuvchilari", "AeroGram фойдаланувчилари"))
                .setMessage(AeroConfig.tr(
                    "Bu ko‘rsatkich Bauntyme/AeroGram GitHub relizlaridan rasmiy APK yuklab olishlarning haqiqiy sonini ko‘rsatadi (hech qanday sun’iy yoki yashirin kuzatuv yo‘q).",
                    "Бу кўрсаткич Bauntyme/AeroGram GitHub релизларидан расмий APK юклаб олишларнинг ҳақиқий сонини кўрсатади (ҳеч қандай сунъий ёки яширин кузатув йўқ)."
                ))
                .setPositiveButton("OK", null)
                .show()
        )));

        int activeCount = 0;
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated()) activeCount++;
        }

        items.add(UItem.asShadow(null));
        items.add(UItem.asCustom(12, createStatMetricCard(
            getParentActivity(),
            AeroConfig.tr("Faol akkauntlar (ushbu qurilmada)", "Фаол аккаунтлар (ушбу қурилмада)"),
            activeCount + " / " + UserConfig.MAX_ACCOUNT_COUNT,
            activeCount > 0 ? "ONLINE" : "OFF",
            0xFF10B981,
            v -> new AlertDialog.Builder(getParentActivity())
                .setTitle(AeroConfig.tr("Faol akkauntlar", "Фаол аккаунтлар"))
                .setMessage(AeroConfig.tr(
                    "Qurilmangizda tizimga kirgan va faol Telegram akkauntlar soni hamda ulardagi saqlangan ma’lumotlar.",
                    "Қурилмангизда тизимга кирган ва фаол Telegram аккаунтлар сони ҳамда улардаги сақланган маълумотлар."
                ))
                .setPositiveButton("OK", null)
                .show()
        )));

        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader(AeroConfig.tr("Qurilmadagi faol akkauntlar", "Қурилмадаги фаол аккаунтлар")));

        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            UserConfig uc = UserConfig.getInstance(a);
            if (!uc.isClientActivated()) continue;
            TLRPC.User u = uc.getCurrentUser();
            String name = u != null ? UserObject.getUserName(u) : ("Akkaunt #" + (a + 1));
            String uname = u != null && !TextUtils.isEmpty(u.username) ? "@" + u.username : ("ID: " + uc.getClientUserId());
            boolean isCurrent = (a == currentAccount);
            items.add(AeroRowCell.Factory.ofNav(
                200 + a,
                isCurrent ? 0xFF10B981 : 0xFF64748B,
                R.drawable.msg_groups,
                name,
                uname + (isCurrent ? AeroConfig.tr(" • Joriy faol sessiya", " • Жорий фаол сессия") : AeroConfig.tr(" • Bosing va almashing", " • Босинг ва алмашинг")),
                isCurrent ? "FAOL" : "KIRISH",
                isCurrent ? AeroRowCell.BADGE_NEW : AeroRowCell.BADGE_NONE
            ));
        }

        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader(AeroConfig.tr("Mahalliy faollik ko‘rsatkichlari", "Маҳаллий фаоллик кўрсаткичлари")));
        items.add(UItem.asButton(0, R.drawable.msg_delete,
            AeroConfig.tr("Saqlangan o‘chirilgan xabarlar", "Сақланган ўчирилган хабарлар"),
            AeroDeletedStore.getTotalSavedCount() + " ta"));
        items.add(UItem.asButton(0, R.drawable.msg_recent,
            AeroConfig.tr("Saqlangan tahrir versiyalari", "Сақланган таҳрир версиялари"),
            AeroEditHistoryStore.getTotalCount() + " ta"));
        items.add(UItem.asButton(0, R.drawable.msg_block,
            AeroConfig.tr("To‘silgan chet el xabarlari", "Тўсилган чет эл хабарлари"),
            AeroConfig.statBlockedForeignCount + " ta"));
        items.add(UItem.asButton(99, R.drawable.msg_reset,
            AeroConfig.tr("Statistikani yangilash", "Статистикани янгилаш"),
            fetchingStats ? "..." : "OK"));
    }

    private void fillBotsItems(ArrayList<UItem> items) {
        items.add(UItem.asShadow(AeroConfig.tr(
            "Video, musiqa, hujjat, QR va AI botlarni bitta bosish bilan oching.",
            "Видео, мусиқа, ҳужжат, QR ва AI ботларни битта босиш билан очинг."
        )));

        items.add(UItem.asHeader(AeroConfig.tr("Media va Video yuklovchilar", "Медиа ва Видео юкловчилар")));
        for (int i = 0; i < BOTS_VIDEO.length; i++) {
            items.add(AeroRowCell.Factory.ofNav(100 + i, 0xFFEC4899, R.drawable.msg_bots,
                "@" + BOTS_VIDEO[i][0], AeroConfig.tr(BOTS_VIDEO[i][1], BOTS_VIDEO[i][2]), "Ochish", AeroRowCell.BADGE_AERO));
        }
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(AeroConfig.tr("Fayl, PDF va Konverterlar", "Файл, PDF ва Конвертерлар")));
        for (int i = 0; i < BOTS_TOOLS.length; i++) {
            items.add(AeroRowCell.Factory.ofNav(200 + i, 0xFF8B5CF6, R.drawable.msg_bot,
                "@" + BOTS_TOOLS[i][0], AeroConfig.tr(BOTS_TOOLS[i][1], BOTS_TOOLS[i][2]), "Ochish", AeroRowCell.BADGE_NONE));
        }
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(AeroConfig.tr("AI, Ovoz va Tarjima botlari", "AI, Овоз ва Таржима ботлари")));
        for (int i = 0; i < BOTS_AI.length; i++) {
            items.add(AeroRowCell.Factory.ofNav(300 + i, 0xFF0EA5E9, R.drawable.msg_translate,
                "@" + BOTS_AI[i][0], AeroConfig.tr(BOTS_AI[i][1], BOTS_AI[i][2]), "Ochish", AeroRowCell.BADGE_NEW));
        }
    }

    private void fillAccountsItems(ArrayList<UItem> items) {
        int active = 0;
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            UserConfig uc = UserConfig.getInstance(a);
            if (!uc.isClientActivated()) continue;
            active++;
            TLRPC.User u = uc.getCurrentUser();
            String name = u != null ? UserObject.getUserName(u) : ("Akkaunt #" + (a + 1));
            String sub = (u != null && !TextUtils.isEmpty(u.username) ? "@" + u.username + " • " : "") + "ID: " + uc.getClientUserId();
            boolean curr = (a == currentAccount);
            items.add(AeroRowCell.Factory.ofNav(a, curr ? 0xFF10B981 : 0xFF0EA5E9, R.drawable.msg_groups,
                name, sub, curr ? AeroConfig.tr("Faol", "Фаол") : AeroConfig.tr("Tanlash", "Танлаш"),
                curr ? AeroRowCell.BADGE_NEW : AeroRowCell.BADGE_NONE));
        }
        items.add(UItem.asShadow(AeroConfig.tr(
            "Jami " + UserConfig.MAX_ACCOUNT_COUNT + " tagacha akkaunt ulash mumkin • Hozir faol: " + active + " ta",
            "Жами " + UserConfig.MAX_ACCOUNT_COUNT + " тагача аккаунт улаш мумкин • Ҳозир фаол: " + active + " та"
        )));
        if (active < UserConfig.MAX_ACCOUNT_COUNT) {
            items.add(AeroRowCell.Factory.ofNav(500, 0xFF6366F1, R.drawable.msg_groups_create,
                AeroConfig.tr("Yangi akkaunt qo‘shish", "Янги аккаунт қўшиш"),
                AeroConfig.tr("Boshqa raqam orqali tizimga kirish", "Бошқа рақам орқали тизимга кириш"),
                "+", AeroRowCell.BADGE_NONE));
        }
    }

    private void fillChatToggleList(ArrayList<UItem> items, boolean hiddenMode) {
        ArrayList<TLRPC.Dialog> dialogs = MessagesController.getInstance(currentAccount).getAllDialogs();
        items.add(UItem.asShadow(hiddenMode
            ? AeroConfig.tr("Yashirilgan chatlar umumiy ro‘yxatda ko‘rinmaydi.", "Яширилган чатлар умумий рўйхатда кўринмайди.")
            : AeroConfig.tr("Tanlangan chatlarni ochishda PIN kod so‘raladi.", "Танланган чатларни очишда ПИН код сўралади.")
        ));
        if (hiddenMode) {
            items.add(AeroRowCell.Factory.ofSwitch(9990, 0xFF0EA5E9, R.drawable.msg_secret,
                AeroConfig.tr("Yashirin chatlarni vaqtincha ko‘rsatish", "Яширин чатларни вақтинча кўрсатиш"),
                AeroConfig.tr("Chatlar ro‘yxatida barchasini ko‘rsatadi", "Чатлар рўйхатида барчасини кўрсатади"),
                AeroConfig.hiddenChatsUnlocked, AeroRowCell.BADGE_NONE));
            items.add(UItem.asShadow(null));
        }
        int limit = Math.min(dialogs.size(), 80);
        for (int i = 0; i < limit; i++) {
            TLRPC.Dialog d = dialogs.get(i);
            if (d == null || d.id == 0) continue;
            String title = DialogObject.isUserDialog(d.id)
                ? MessagesController.getInstance(currentAccount).getPeerName(d.id)
                : (MessagesController.getInstance(currentAccount).getChat(-d.id) != null ? MessagesController.getInstance(currentAccount).getChat(-d.id).title : ("Chat " + d.id));
            if (TextUtils.isEmpty(title)) continue;
            boolean checked = hiddenMode ? AeroConfig.isDialogHidden(d.id) : AeroConfig.lockedDialogIds.contains(d.id);
            items.add(UItem.asCheck(i, title).setChecked(checked));
        }
    }

    private void fillLanguageItems(ArrayList<UItem> items, boolean forStt) {
        if (forStt) {
            items.add(UItem.asRadio(100, AeroConfig.tr("Avtomatik aniqlash (Auto)", "Автоматик аниқлаш (Auto)")).setChecked("auto".equalsIgnoreCase(AeroConfig.voiceToTextLang)));
        }
        String current = forStt ? AeroConfig.voiceToTextLang : AeroConfig.translateTargetLang;
        for (int i = 0; i < LANGUAGES.length; i++) {
            items.add(UItem.asRadio(i, AeroConfig.tr(LANGUAGES[i][1], LANGUAGES[i][2])).setChecked(LANGUAGES[i][0].equalsIgnoreCase(current)));
        }
    }

    private static final String[][] GUIDE_ENTRIES = {
        {"Ghost Rejimi", "Ghost Режими",
            "YOQISH: Maxfiylik → Barcha Ghost funksiyalari.\nQANDAY ISHLAYDI: Siz xabarni o‘qisangiz ham suhbatdoshda ikki ko‘k belgi ketmaydi, «onlayn» yoki «yozmoqda» ko‘rinmaydi, hikoyalar maxfiy ko‘riladi.\nO‘CHIRISH: Ghost kalitini o‘chiring yoki chatda Ghost belgisini bosing.",
            "ЁҚИШ: Махфийлик → Барча Ghost функциялари.\nҚАНДАЙ ИШЛАЙДИ: Сиз хабарни ўқисангиз ҳам суҳбатдошда икки кўк белги кетмайди, «онлайн» ёки «ёзмоқда» кўринмайди, ҳикоялар махфий кўрилади.\nЎЧИРИШ: Ghost калитини ўчиринг ёки чатда Ghost белгисини босинг."},
        {"Anti-Delete (O‘chirilgan xabarlar)", "Anti-Delete (Ўчирилган хабарлар)",
            "YOQISH: Standart yoqilgan (O‘chirilgan xabarlarni saqlash).\nQANDAY ISHLAYDI: Boshqa odam xabarni o‘chirsa ham, u chatdan yo‘qolmaydi — darhol «🗑 O‘chirilgan» yozuvi chiqadi, chat qayta ochilganda (zagruzka) va rasmlarda ham saqlanib qoladi.\nO‘CHIRISH: O‘chirilgan xabarlarni saqlash tugmasini o‘chiring.",
            "ЁҚИШ: Стандарт ёқилган (Ўчирилган хабарларни сақлаш).\nҚАНДАЙ ИШЛАЙДИ: Бошқа одам хабарни ўчирса ҳам, у чатдан йўқолмайди — дарҳол «🗑 Ўчирилган» ёзуви чиқади, чат қайта очилганда (загрузка) ва расмларда ҳам сақланиб қолади.\nЎЧИРИШ: Ўчирилган хабарларни сақлаш тугмасини ўчиринг."},
        {"Tahrirlar tarixi (Eski → Yangi)", "Таҳрирлар тарихи (Эски → Янги)",
            "YOQISH: Standart yoqilgan (Tahrirlar tarixini saqlash).\nQANDAY ISHLAYDI: Xabar tahrirlanganda pufakcha ichida avval 📝 Eski matn, uning pastida ✨ Yangi matn avtomatik ko‘rsatiladi. Ustiga bosib barcha eski versiyalarni ko‘rish mumkin.\nO‘CHIRISH: Tahrirlar tarixini saqlash kalitini o‘chiring.",
            "ЁҚИШ: Стандарт ёқилган (Таҳрирлар тарихини сақлаш).\nҚАНДАЙ ИШЛАЙДИ: Хабар таҳрирланганда пуфакча ичида аввал 📝 Эски матн, унинг пастида ✨ Янги матн автоматик кўрсатилади. Устига босиб барча эски версияларни кўриш мумкин.\nЎЧИРИШ: Таҳрирлар тарихини сақлаш калитини ўчиринг."},
        {"Yashirin va PIN Qulflangan Chatlar", "Яширин ва ПИН Қулфланган Чатлар",
            "YOQISH: Maxfiylik → PIN kod o‘rnatish → Chatlarni tanlash.\nQANDAY ISHLAYDI: Qulflangan chat PIN siz ochilmaydi. Yashirilgan chatlar umumiy ro‘yxatda ko‘rinmaydi.\nO‘CHIRISH: Sozlamalardan belgini yeching.",
            "ЁҚИШ: Махфийлик → ПИН код ўрнатиш → Чатларни танлаш.\nҚАНДАЙ ИШЛАЙДИ: Қулфланган чат ПИН сиз очилмайди. Яширилган чатлар умумий рўйхатда кўринмайди.\nЎЧИРИШ: Созламалардан белгини ечинг."},
        {"Avto-tarjima & Ovoz → Matn (STT)", "Авто-таржима & Овоз → Матн (STT)",
            "YOQISH: Tarjima va Ovoz → Avto-tarjima / Yuborishda tarjima / Ovozdan matnga.\nQANDAY ISHLAYДИ: Kiruvchi va chiquvchi matnlar Telegram orqali tanlangan tilga o‘giriladi, ovozli xabarlar matn qilib beriladi.\nO‘CHIRISH: Kalitlarni o‘chiring.",
            "ЁҚИШ: Таржима ва Овоз → Авто-таржима / Юборишда таржима / Овоздан матнга.\nҚАНДАЙ ИШЛАЙДИ: Кирувчи ва чиқувчи матнлар Telegram орқали танланган тилга ўгирилади, овозли хабарлар матн қилиб берилади.\nЎЧИРИШ: Калитларни ўчиринг."},
        {"Xabar bezaklari va Double-Tap", "Хабар безаклари ва Double-Tap",
            "YOQISH: Xabarlar → Matn uslubi / Yangi alifbo / Postfix / Double-Tap.\nQANDAY ISHLAYDI: Yuborilgan matnga Qalin, Kursiv, ❤️ yoki postfix qo‘shiladi. Xabarni ikki marta bosish amali moslanadi.\nO‘CHIRISH: Matn uslubini Oddiy ga qo‘ying.",
            "ЁҚИШ: Хабарлар → Матн услуби / Янги алифбо / Постфикс / Double-Tap.\nҚАНДАЙ ИШЛАЙДИ: Юборилган матнга Қалин, Курсив, ❤️ ёки постфикс қўшилади. Хабарни икки марта босиш амали мосланади.\nЎЧИРИШ: Матн услубини Оддий га қўйинг."},
        {"Papkalar, Proksi, Eslatmalar va Hikoyalar HD", "Папкалар, Прокси, Эслатмалар ва Ҳикоялар HD",
            "YOQISH: Tarmoq, Papkalar → Papkalar uslubi, Avto-qabul, AeroGramProxy, Stories HD.\nQANDAY ISHLAYDI: Papkalar ikonkalarga o‘tadi, guruh so‘rovlari avto-qabul qilinadi, hikoyalar HD sifatda saqlanadi.\nO‘CHIRISH: Har bir bo‘limdagi kalitdan o‘chirish mumkin.",
            "ЁҚИШ: Тармоқ, Папкалар → Папкалар услуби, Авто-қабул, AeroGramProxy, Stories HD.\nҚАНДАЙ ИШЛАЙДИ: Папкалар иконкаларга ўтади, гуруҳ сўровлари авто-қабул қилинади, ҳикоялар HD сифатда сақланади.\nЎЧИРИШ: Ҳар бир бўлимдаги калитдан ўчириш мумкин."},
        {"Concept A Ilova Ikonkalari (6 xil)", "Concept A Илова Иконкалари (6 хил)",
            "YOQISH: Interfeys → Ilova ikonkasi yoki Chat sozlamalari → Ilova ikonkasi.\nQANDAY ISHLAYDI: Bosh ekraningizdagi AeroGram belgisini 6 xil eksklyuziv dizaynga bepul almashtiradi.\nO‘CHIRISH: Klassik Ko‘k ikonkasini qayta tanlang.",
            "ЁҚИШ: Интерфейс → Илова иконкаси ёки Чат созламалари → Илова иконкаси.\nҚАНДАЙ ИШЛАЙДИ: Бош экранингиздаги AeroGram белгисини 6 хил эксклюзив дизайнга бепул алмаштиради.\nЎЧИРИШ: Классик Кўк иконкасини қайта танланг."}
    };

    private void fillAboutItems(ArrayList<UItem> items) {
        items.add(UItem.asShadow(AeroConfig.tr(
            "Istalgan bo‘lim ustiga bosing — undan qanday foydalanish, yoqish va o‘chirish to‘liq ochiladi:",
            "Исталган бўлим устига босинг — ундан қандай фойдаланиш, ёқиш ва ўчириш тўлиқ очилади:"
        )));
        for (int i = 0; i < GUIDE_ENTRIES.length; i++) {
            items.add(AeroRowCell.Factory.ofNav(
                i, 0xFF0284C7, R.drawable.msg_info,
                AeroConfig.tr(GUIDE_ENTRIES[i][0], GUIDE_ENTRIES[i][1]),
                AeroConfig.tr("Batafsil qo‘llanma (Yoqish / Ishlash / O‘chirish)", "Батафсил қўлланма (Ёқиш / Ишлаш / Ўчириш)"),
                "", i == 0 ? AeroRowCell.BADGE_AERO : AeroRowCell.BADGE_NONE
            ));
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (mode == MODE_ABOUT && item.id >= 0 && item.id < GUIDE_ENTRIES.length && getParentActivity() != null) {
            String[] g = GUIDE_ENTRIES[item.id];
            new AlertDialog.Builder(getParentActivity())
                .setTitle(AeroConfig.tr(g[0], g[1]))
                .setMessage(AeroConfig.tr(g[2], g[3]))
                .setPositiveButton("OK", null)
                .show();
        } else if (mode == MODE_STATS) {
            if (item.id == 99) {
                fetchGitHubDownloadsIfNeeded(true);
                Toast.makeText(getParentActivity(), AeroConfig.tr("Yangilanmoqda...", "Янгиланмоқда..."), Toast.LENGTH_SHORT).show();
            } else if (item.id >= 200 && item.id < 200 + UserConfig.MAX_ACCOUNT_COUNT) {
                int acc = item.id - 200;
                if (acc != currentAccount && LaunchActivity.instance != null) {
                    LaunchActivity.instance.switchToAccount(acc, true);
                }
            }
        } else if (mode == MODE_BOTS) {
            String username = null;
            if (item.id >= 100 && item.id < 100 + BOTS_VIDEO.length) {
                username = BOTS_VIDEO[item.id - 100][0];
            } else if (item.id >= 200 && item.id < 200 + BOTS_TOOLS.length) {
                username = BOTS_TOOLS[item.id - 200][0];
            } else if (item.id >= 300 && item.id < 300 + BOTS_AI.length) {
                username = BOTS_AI[item.id - 300][0];
            }
            if (!TextUtils.isEmpty(username)) {
                MessagesController.getInstance(currentAccount).openByUserName(username, this, 0);
            }
        } else if (mode == MODE_ACCOUNTS) {
            if (item.id == 500) {
                presentFragment(new LoginActivity());
            } else if (item.id >= 0 && item.id < UserConfig.MAX_ACCOUNT_COUNT && item.id != currentAccount && LaunchActivity.instance != null) {
                LaunchActivity.instance.switchToAccount(item.id, true);
            }
        } else if (mode == MODE_HIDDEN_CHATS || mode == MODE_LOCKED_CHATS) {
            boolean hiddenMode = (mode == MODE_HIDDEN_CHATS);
            if (hiddenMode && item.id == 9990) {
                AeroConfig.hiddenChatsUnlocked = !AeroConfig.hiddenChatsUnlocked;
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
                reloadList();
                return;
            }
            ArrayList<TLRPC.Dialog> dialogs = MessagesController.getInstance(currentAccount).getAllDialogs();
            if (item.id >= 0 && item.id < dialogs.size()) {
                long did = dialogs.get(item.id).id;
                if (hiddenMode) {
                    AeroConfig.setDialogHidden(did, !AeroConfig.isDialogHidden(did));
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
                } else {
                    AeroConfig.setDialogLocked(did, !AeroConfig.lockedDialogIds.contains(did));
                }
                reloadList();
            }
        } else if (mode == MODE_TRANSLATE_LANG && item.id >= 0 && item.id < LANGUAGES.length) {
            AeroConfig.translateTargetLang = LANGUAGES[item.id][0];
            AeroConfig.saveString("translateTargetLang", AeroConfig.translateTargetLang);
            reloadList();
        } else if (mode == MODE_STT_LANG) {
            if (item.id == 100) {
                AeroConfig.voiceToTextLang = "auto";
            } else if (item.id >= 0 && item.id < LANGUAGES.length) {
                AeroConfig.voiceToTextLang = LANGUAGES[item.id][0];
            }
            AeroConfig.saveString("voiceToTextLang", AeroConfig.voiceToTextLang);
            reloadList();
        } else if (mode == MODE_DOUBLE_TAP && item.id >= 0 && item.id <= 4) {
            AeroConfig.doubleTapAction = item.id;
            AeroConfig.saveInt("doubleTapAction", item.id);
            reloadList();
        } else if (mode == MODE_TEXT_STYLE && item.id >= 0 && item.id <= 6) {
            AeroConfig.textStyleMode = item.id;
            AeroConfig.saveInt("textStyleMode", item.id);
            reloadList();
        } else if (mode == MODE_FOLDER_STYLE && item.id >= 0 && item.id <= 2) {
            AeroConfig.folderStyle = item.id;
            AeroConfig.saveInt("folderStyle", item.id);
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogFiltersUpdated);
            reloadList();
        } else if (mode == MODE_REMINDER_DELAY && item.id > 0) {
            AeroConfig.unreadReminderMinutes = item.id;
            AeroConfig.saveInt("unreadReminderMinutes", item.id);
            reloadList();
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
