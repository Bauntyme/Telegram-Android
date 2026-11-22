package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
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
import org.telegram.messenger.AeroTextUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;
import org.telegram.ui.Components.UniversalRecyclerView;

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
    public static final int MODE_NEW_ALPHABET = 12;
    public static final int MODE_POSTFIX = 13;
    public static final int MODE_AUTO_TRANSLATE = 14;
    public static final int MODE_GUIDE = 15;

    private static final int MENU_REFRESH = 1;

    private final int mode;
    private boolean fetchingStats = false;

    // Cached custom views so reloadList() never recreates views or causes scroll/toggle flicker
    private View statsHeaderView;
    private View statsUsersCardView;
    private TextView statsUsersValueView;
    private View statsAccountsCardView;
    private TextView statsAccountsValueView;
    private View aboutIntroView;
    private View guideIntroView;
    private View alphabetPreviewView;
    private View postfixEditorView;
    private EditText postfixEditText;
    private TextView postfixPreviewView;

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
            case MODE_TEXT_STYLE: return AeroConfig.tr("Matn uslubi", "Матн услуби");
            case MODE_FOLDER_STYLE: return AeroConfig.tr("Papkalar uslubi", "Папкалар услуби");
            case MODE_REMINDER_DELAY: return AeroConfig.tr("Eslatma kechikishi", "Эслатма кечикиши");
            case MODE_NEW_ALPHABET: return AeroConfig.tr("Yangi o‘zbek alifbosi", "Янги ўзбек алифбоси");
            case MODE_POSTFIX: return AeroConfig.tr("Avto-postfiks", "Авто-постфикс");
            case MODE_AUTO_TRANSLATE: return AeroConfig.tr("Avto-tarjima", "Авто-таржима");
            case MODE_GUIDE: return AeroConfig.tr("AeroGram Qo‘llanmasi", "AeroGram Қўлланмаси");
            default: return AeroConfig.tr("AeroGram haqida", "AeroGram ҳақида");
        }
    }

    @Override
    public View createView(Context context) {
        statsHeaderView = null;
        statsUsersCardView = null;
        statsUsersValueView = null;
        statsAccountsCardView = null;
        statsAccountsValueView = null;
        aboutIntroView = null;
        guideIntroView = null;
        alphabetPreviewView = null;
        postfixEditorView = null;
        postfixEditText = null;
        postfixPreviewView = null;
        View root = super.createView(context);
        if (mode == MODE_STATS) {
            ActionBarMenu menu = actionBar.createMenu();
            menu.addItem(MENU_REFRESH, R.drawable.msg_reset);
            actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
                @Override
                public void onItemClick(int id) {
                    if (id == -1) {
                        finishFragment();
                    } else if (id == MENU_REFRESH) {
                        fetchGitHubDownloadsIfNeeded(true);
                        if (getParentActivity() != null) {
                            Toast.makeText(getParentActivity(), AeroConfig.tr("Yangilanmoqda...", "Янгиланмоқда..."), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mode == MODE_STATS) {
            fetchGitHubDownloadsIfNeeded(false);
        }
    }

    private void reloadList() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(false);
        }
    }

    private static String formatSpacedInt(int value) {
        String s = String.valueOf(Math.max(0, value));
        StringBuilder sb = new StringBuilder();
        int len = s.length();
        for (int i = 0; i < len; i++) {
            if (i > 0 && (len - i) % 3 == 0) {
                sb.append(' ');
            }
            sb.append(s.charAt(i));
        }
        return sb.toString();
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
                if (statsUsersValueView != null) {
                    statsUsersValueView.setText(formatSpacedInt(Math.max(1, AeroConfig.cachedCommunityUsers)));
                }
            });
        });
    }

    private View getOrCreateAnalyticsHeader(Context context) {
        if (statsHeaderView != null) {
            return statsHeaderView;
        }
        LinearLayout wrap = new LinearLayout(context);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setGravity(Gravity.CENTER_HORIZONTAL);
        wrap.setPadding(dp(20), dp(26), dp(20), dp(22));

        GradientDrawable circle = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{0xFF38BDF8, 0xFF0284C7, 0xFF1E3A8A}
        );
        circle.setCornerRadius(dp(36));

        FrameLayout emblem = new FrameLayout(context);
        emblem.setBackground(circle);
        ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.ic_aerogram_monochrome);
        icon.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        emblem.addView(icon, LayoutHelper.createFrame(38, 38, Gravity.CENTER));
        wrap.addView(emblem, LayoutHelper.createLinear(72, 72, Gravity.CENTER_HORIZONTAL));

        TextView title = new TextView(context);
        title.setText(AeroConfig.tr("AeroGram Analitika", "AeroGram Аналитика"));
        title.setTypeface(AndroidUtilities.bold());
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        wrap.addView(title, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 14, 0, 0));

        TextView sub = new TextView(context);
        sub.setText(AeroConfig.tr("Ilovadan foydalanish statistikasi", "Иловадан фойдаланиш статистикаси"));
        sub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
        sub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, getResourceProvider()));
        wrap.addView(sub, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 0));

        statsHeaderView = wrap;
        return statsHeaderView;
    }

    private View getOrCreateStatMetricCard(Context context, boolean usersCard, String label, int bigValue, String infoTitle, String infoMessage) {
        View cached = usersCard ? statsUsersCardView : statsAccountsCardView;
        TextView valView = usersCard ? statsUsersValueView : statsAccountsValueView;
        if (cached != null && valView != null) {
            valView.setText(formatSpacedInt(bigValue));
            return cached;
        }

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(18), dp(20), dp(18));

        TextView val = new TextView(context);
        val.setText(formatSpacedInt(bigValue));
        val.setTypeface(AndroidUtilities.bold());
        val.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 28);
        val.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        card.addView(val, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout labelRow = new LinearLayout(context);
        labelRow.setOrientation(LinearLayout.HORIZONTAL);
        labelRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView lbl = new TextView(context);
        lbl.setText(label);
        lbl.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.5f);
        lbl.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, getResourceProvider()));
        labelRow.addView(lbl, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        ImageView infoBtn = new ImageView(context);
        infoBtn.setImageResource(R.drawable.msg_info);
        infoBtn.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, getResourceProvider()), PorterDuff.Mode.SRC_IN));
        infoBtn.setPadding(dp(6), dp(4), dp(6), dp(4));
        infoBtn.setOnClickListener(v -> {
            if (getParentActivity() != null) {
                new AlertDialog.Builder(getParentActivity())
                    .setTitle(infoTitle)
                    .setMessage(infoMessage)
                    .setPositiveButton("OK", null)
                    .show();
            }
        });
        labelRow.addView(infoBtn, LayoutHelper.createLinear(28, 28, Gravity.CENTER_VERTICAL, 4, 0, 0, 0));

        card.addView(labelRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));

        if (usersCard) {
            statsUsersCardView = card;
            statsUsersValueView = val;
        } else {
            statsAccountsCardView = card;
            statsAccountsValueView = val;
        }
        return card;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (mode == MODE_ABOUT) {
            fillAboutItems(items);
        } else if (mode == MODE_GUIDE) {
            fillGuideItems(items);
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
                AeroConfig.tr("Yashirin (Spoiler)", "Яширин (Spoiler)")
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
        } else if (mode == MODE_NEW_ALPHABET) {
            fillNewAlphabetItems(items);
        } else if (mode == MODE_POSTFIX) {
            fillPostfixItems(items);
        } else if (mode == MODE_AUTO_TRANSLATE) {
            fillAutoTranslateItems(items);
        }
    }

    private void fillStatsItems(ArrayList<UItem> items) {
        if (getParentActivity() == null) return;
        items.add(UItem.asCustom(10, getOrCreateAnalyticsHeader(getParentActivity())));
        items.add(UItem.asShadow(null));

        int totalUsers = Math.max(1, AeroConfig.cachedCommunityUsers);
        items.add(UItem.asCustom(11, getOrCreateStatMetricCard(
            getParentActivity(),
            true,
            AeroConfig.tr("Jami foydalanuvchilar", "Жами фойдаланувчилар"),
            totalUsers,
            AeroConfig.tr("Jami foydalanuvchilar", "Жами фойдаланувчилар"),
            AeroConfig.tr(
                "Bauntyme/AeroGram relizlaridan rasmiy APK yuklab olgan foydalanuvchilarning umumiy soni.",
                "Bauntyme/AeroGram релизларидан расмий APK юклаб олган фойдаланувчиларнинг умумий сони."
            )
        )));

        items.add(UItem.asShadow(null));

        int activeCount = 0;
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated()) activeCount++;
        }
        items.add(UItem.asCustom(12, getOrCreateStatMetricCard(
            getParentActivity(),
            false,
            AeroConfig.tr("Faol akkauntlar", "Фаол аккаунтлар"),
            activeCount,
            AeroConfig.tr("Faol akkauntlar", "Фаол аккаунтлар"),
            AeroConfig.tr(
                "Ushbu qurilmada tizimga kirgan va faol ishlayotgan AeroGram akkauntlari soni.",
                "Ушбу қурилмада тизимга кирган ва фаол ишлаётган AeroGram аккаунтлари сони."
            )
        )));
    }

    private void fillNewAlphabetItems(ArrayList<UItem> items) {
        if (getParentActivity() != null) {
            items.add(UItem.asCustom(10, getOrCreateAlphabetPreview(getParentActivity())));
            items.add(UItem.asShadow(null));
        }
        items.add(AeroRowCell.Factory.ofSwitch(1, 0xFF7C3AED, R.drawable.msg_text_check,
            AeroConfig.tr("Yangi alifboda yuborish", "Янги алифбода юбориш"),
            AeroConfig.tr("Yozgan xabaringizda sh→ş, ch→ç, o‘→ō, g‘→ḡ almashtiradi", "Ёзган хабарингизда sh→ş, ch→ç, o‘→ō, g‘→ḡ алмаштиради"),
            AeroConfig.newUzbekAlphabet, AeroRowCell.BADGE_AERO));
        items.add(AeroRowCell.Factory.ofSwitch(2, 0xFF6366F1, R.drawable.msg_viewchats,
            AeroConfig.tr("Chatlarni yangi alifboda ko‘rsatish", "Чатларни янги алифбода кўрсатиш"),
            AeroConfig.tr("Kiruvchi va chatdagi matnlarni avtomatik ş ç ō ḡ ko‘rinishida o‘qiysiz", "Кирувчи ва чатдаги матнларни автоматик ş ç ō ḡ кўринишида ўқийсиз"),
            AeroConfig.newUzbekAlphabetInChats, AeroRowCell.BADGE_NEW));
        items.add(AeroRowCell.Factory.ofSwitch(3, 0xFF8B5CF6, R.drawable.msg_translate,
            AeroConfig.tr("Interfeysda Kirill yozuvi (Lotin ↔ Кирилл)", "Интерфейсда Кирилл ёзуви (Лотин ↔ Кирилл)"),
            AeroConfig.tr("AeroGram menyu va izohlarini kirill yoki lotin alifbosiga o‘giradi", "AeroGram меню ва изоҳларини кирилл ёки лотин алифбосига ўгиради"),
            AeroConfig.uiCyrillic, AeroRowCell.BADGE_NONE));
        items.add(UItem.asShadow(null));
        items.add(AeroRowCell.Factory.ofNav(4, 0xFF0EA5E9, R.drawable.msg_text_outlined,
            AeroConfig.tr("Matn konverteri (Lotin ↔ Кирилл / ş ç ō ḡ)", "Матн конвертери (Лотин ↔ Кирилл / ş ç ō ḡ)"),
            AeroConfig.tr("Istalgan matnni tezkor o‘girish va nusxalash", "Исталган матнни тезкор ўгириш ва нусхалаш"),
            AeroConfig.tr("Ochish", "Очиш"), AeroRowCell.BADGE_AERO));
    }

    private View getOrCreateAlphabetPreview(Context context) {
        if (alphabetPreviewView != null) {
            return alphabetPreviewView;
        }
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(20), dp(20), dp(20), dp(18));

        TextView big = new TextView(context);
        big.setText("ş  ç  ō  ḡ");
        big.setTypeface(AndroidUtilities.bold());
        big.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30);
        big.setTextColor(0xFF7C3AED);
        box.addView(big, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));

        TextView example = new TextView(context);
        example.setText(AeroConfig.tr(
            "«O‘zbekiston shahri chiroyli g‘alaba» → «Ōzbekiston şahri çiroyli ḡalaba»",
            "«O‘zbekiston shahri chiroyli g‘alaba» → «Ōzbekiston şahri çiroyli ḡalaba»"
        ));
        example.setGravity(Gravity.CENTER);
        example.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        example.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, getResourceProvider()));
        box.addView(example, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 8, 0, 0));

        alphabetPreviewView = box;
        return alphabetPreviewView;
    }

    private void fillPostfixItems(ArrayList<UItem> items) {
        items.add(AeroRowCell.Factory.ofSwitch(1, 0xFFA855F7, R.drawable.msg_recent,
            AeroConfig.tr("Avto-postfiksni yoqish", "Авто-постфиксни ёқиш"),
            AeroConfig.tr("Har bir yuborilayotgan xabar oxiriga avtomatik qo‘shiladi", "Ҳар бир юборилаётган хабар охирига автоматик қўшилади"),
            AeroConfig.autoPostfixEnabled, AeroRowCell.BADGE_AERO));
        items.add(UItem.asShadow(null));
        if (getParentActivity() != null) {
            items.add(UItem.asCustom(10, getOrCreatePostfixEditor(getParentActivity())));
        }
    }

    private View getOrCreatePostfixEditor(Context context) {
        if (postfixEditorView != null) {
            if (postfixEditText != null && !TextUtils.equals(postfixEditText.getText(), AeroConfig.autoPostfixText)) {
                postfixEditText.setText(AeroConfig.autoPostfixText);
            }
            if (postfixPreviewView != null) {
                String p = TextUtils.isEmpty(AeroConfig.autoPostfixText) ? "" : (" " + AeroConfig.autoPostfixText.trim());
                postfixPreviewView.setText(AeroConfig.tr("Namuna: «Assalomu alaykum" + p + "»", "Намуна: «Ассалому алайкум" + p + "»"));
            }
            return postfixEditorView;
        }
        LinearLayout wrap = new LinearLayout(context);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(18), dp(14), dp(18), dp(16));

        TextView lbl = new TextView(context);
        lbl.setText(AeroConfig.tr("Postfiks matni", "Постфикс матни"));
        lbl.setTypeface(AndroidUtilities.bold());
        lbl.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        lbl.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        wrap.addView(lbl);

        EditText et = new EditText(context);
        et.setText(AeroConfig.autoPostfixText);
        et.setHint(AeroConfig.tr("Masalan: — AeroGram orqali", "Масалан: — AeroGram орқали"));
        et.setImeOptions(EditorInfo.IME_ACTION_DONE);
        et.setSingleLine(true);
        et.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        et.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        et.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText, getResourceProvider()));
        wrap.addView(et, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 8));
        postfixEditText = et;

        TextView preview = new TextView(context);
        String p = TextUtils.isEmpty(AeroConfig.autoPostfixText) ? "" : (" " + AeroConfig.autoPostfixText.trim());
        preview.setText(AeroConfig.tr("Namuna: «Assalomu alaykum" + p + "»", "Намуна: «Ассалому алайкум" + p + "»"));
        preview.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        preview.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, getResourceProvider()));
        wrap.addView(preview, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));
        postfixPreviewView = preview;

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setCornerRadius(dp(10));
        btnBg.setColor(0xFF0284C7);
        TextView saveBtn = new TextView(context);
        saveBtn.setText(AeroConfig.tr("Saqlash", "Сақлаш"));
        saveBtn.setTypeface(AndroidUtilities.bold());
        saveBtn.setTextColor(Color.WHITE);
        saveBtn.setGravity(Gravity.CENTER);
        saveBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.5f);
        saveBtn.setPadding(dp(16), dp(11), dp(16), dp(11));
        saveBtn.setBackground(btnBg);
        saveBtn.setOnClickListener(v -> {
            String txt = et.getText() == null ? "" : et.getText().toString();
            AeroConfig.autoPostfixText = txt;
            AeroConfig.saveString("autoPostfixText", txt);
            if (!TextUtils.isEmpty(txt.trim())) {
                AeroConfig.autoPostfixEnabled = true;
                AeroConfig.saveBool("autoPostfixEnabled", true);
            }
            String p2 = TextUtils.isEmpty(txt) ? "" : (" " + txt.trim());
            preview.setText(AeroConfig.tr("Namuna: «Assalomu alaykum" + p2 + "»", "Намуна: «Ассалому алайкум" + p2 + "»"));
            AndroidUtilities.hideKeyboard(et);
            reloadList();
            Toast.makeText(context, AeroConfig.tr("Postfiks saqlandi", "Постфикс сақланди"), Toast.LENGTH_SHORT).show();
        });
        wrap.addView(saveBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        postfixEditorView = wrap;
        return postfixEditorView;
    }

    private void fillAutoTranslateItems(ArrayList<UItem> items) {
        items.add(AeroRowCell.Factory.ofSwitch(1, 0xFF0284C7, R.drawable.msg_translate,
            AeroConfig.tr("Avtomatik chat tarjimasi", "Автоматик чат таржимаси"),
            AeroConfig.tr("Xorijiy tildagi xabarlarni avtomatik tarjima qiladi", "Хорижий тилдаги хабарларни автоматик таржима қилади"),
            AeroConfig.autoTranslate, AeroRowCell.BADGE_NEW));
        items.add(AeroRowCell.Factory.ofSwitch(2, 0xFF0369A1, R.drawable.msg_forward,
            AeroConfig.tr("Yuborilayotgan xabarlarni avto-tarjima", "Юборилаётган хабарларни авто-таржима"),
            AeroConfig.tr("Yuborishdan oldin matnni tanlangan tilga o‘giradi", "Юборишдан олдин матнни танланган тилга ўгиради"),
            AeroConfig.autoTranslateOutgoing, AeroRowCell.BADGE_NONE));
        items.add(AeroRowCell.Factory.ofSwitch(3, 0xFF0891B2, R.drawable.msg_text_outlined,
            AeroConfig.tr("Yuborishda tarjimani so‘rash", "Юборишда таржимани сўраш"),
            AeroConfig.tr("Yuborish tugmasini bosganda tarjima oynasini ko‘rsatadi", "Юбориш тугмасини босганда таржима ойнасини кўрсатади"),
            AeroConfig.askBeforeTranslateSend, AeroRowCell.BADGE_NONE));
        items.add(AeroRowCell.Factory.ofSwitch(4, 0xFF06B6D4, R.drawable.msg_translate,
            AeroConfig.tr("Butun chatni tarjima qilish", "Бутун чатни таржима қилиш"),
            AeroConfig.tr("Chat menyusida to‘liq tarjima panelini yoqadi", "Чат менюсида тўлиқ таржима панелини ёқади"),
            AeroConfig.translateWholeChat, AeroRowCell.BADGE_NONE));
        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader(AeroConfig.tr("Maqsadli tarjima tili", "Мақсадли таржима тили")));
        for (int i = 0; i < LANGUAGES.length; i++) {
            items.add(UItem.asRadio(100 + i, AeroConfig.tr(LANGUAGES[i][1], LANGUAGES[i][2]))
                .setChecked(LANGUAGES[i][0].equalsIgnoreCase(AeroConfig.translateTargetLang)));
        }
    }

    private void showAlphabetConverterDialog() {
        if (getParentActivity() == null) return;
        Context ctx = getParentActivity();
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(10), dp(20), dp(6));

        EditText et = new EditText(ctx);
        et.setHint(AeroConfig.tr("Matnni kiriting...", "Матнни киритинг..."));
        et.setMinLines(2);
        et.setMaxLines(5);
        et.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        et.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        box.addView(et);

        TextView resultTv = new TextView(ctx);
        resultTv.setTextIsSelectable(true);
        resultTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.5f);
        resultTv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        resultTv.setPadding(0, dp(10), 0, dp(6));
        box.addView(resultTv);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView btnNew = new TextView(ctx);
        btnNew.setText("ş ç ō ḡ");
        btnNew.setTypeface(AndroidUtilities.bold());
        btnNew.setPadding(dp(10), dp(8), dp(10), dp(8));
        btnNew.setTextColor(0xFF7C3AED);
        btnNew.setOnClickListener(v -> {
            String src = et.getText() == null ? "" : et.getText().toString();
            resultTv.setText(AeroTextUtils.convertNewUzbekAlphabet(src));
        });
        row.addView(btnNew);

        TextView btnCyr = new TextView(ctx);
        btnCyr.setText("→ Кирилл");
        btnCyr.setTypeface(AndroidUtilities.bold());
        btnCyr.setPadding(dp(10), dp(8), dp(10), dp(8));
        btnCyr.setTextColor(0xFF0284C7);
        btnCyr.setOnClickListener(v -> {
            String src = et.getText() == null ? "" : et.getText().toString();
            resultTv.setText(AeroTextUtils.convertUzbekLatinToCyrillic(src));
        });
        row.addView(btnCyr);

        TextView btnLat = new TextView(ctx);
        btnLat.setText("→ Lotin");
        btnLat.setTypeface(AndroidUtilities.bold());
        btnLat.setPadding(dp(10), dp(8), dp(10), dp(8));
        btnLat.setTextColor(0xFF10B981);
        btnLat.setOnClickListener(v -> {
            String src = et.getText() == null ? "" : et.getText().toString();
            resultTv.setText(AeroTextUtils.convertUzbekCyrillicToLatin(src));
        });
        row.addView(btnLat);

        box.addView(row);

        new AlertDialog.Builder(ctx)
            .setTitle(AeroConfig.tr("Matn konverteri", "Матн конвертери"))
            .setView(box)
            .setPositiveButton(AeroConfig.tr("Natijani nusxalash", "Натижани нусхалаш"), (d, w) -> {
                String out = resultTv.getText() == null ? "" : resultTv.getText().toString();
                if (!TextUtils.isEmpty(out)) {
                    AndroidUtilities.addToClipboard(out);
                    Toast.makeText(ctx, AeroConfig.tr("Nusxalandi", "Нусхаланди"), Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(AeroConfig.tr("Yopish", "Ёпиш"), null)
            .show();
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
            "Cheksiz akkaunt ulash mumkin (∞) • Hozir faol: " + active + " ta",
            "Чексиз аккаунт улаш мумкин (∞) • Ҳозир фаол: " + active + " та"
        )));
        items.add(AeroRowCell.Factory.ofNav(500, 0xFF6366F1, R.drawable.msg_groups_create,
            AeroConfig.tr("Yangi akkaunt qo‘shish", "Янги аккаунт қўшиш"),
            AeroConfig.tr("Boshqa raqam orqali tizimga kirish", "Бошқа рақам орқали тизимга кириш"),
            "+", AeroRowCell.BADGE_NONE));
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

    private static final class GuideEntry {
        final int color;
        final int icon;
        final String latTitle, cyrTitle;
        final String latOn, cyrOn;
        final String latHow, cyrHow;
        final String latOff, cyrOff;

        GuideEntry(int color, int icon,
                   String latTitle, String cyrTitle,
                   String latOn, String cyrOn,
                   String latHow, String cyrHow,
                   String latOff, String cyrOff) {
            this.color = color;
            this.icon = icon;
            this.latTitle = latTitle;
            this.cyrTitle = cyrTitle;
            this.latOn = latOn;
            this.cyrOn = cyrOn;
            this.latHow = latHow;
            this.cyrHow = cyrHow;
            this.latOff = latOff;
            this.cyrOff = cyrOff;
        }
    }

    private static final GuideEntry[] GUIDE_CARDS = {
        new GuideEntry(0xFF10B981, R.drawable.msg_groups,
            "Cheksiz Akkauntlar (∞)",
            "Чексиз Аккаунтлар (∞)",
            "Standart holatda doim ochiq (Premium talab qilinmaydi)",
            "Стандарт ҳолатда доим очиқ (Premium талаб қилинмайди)",
            "AeroGram ilovasiga istalgancha (5, 10, 20 va undan ko‘p) Telegram akkauntlarini ulashingiz mumkin. Profil menyusi, Sozlamalar yoki «AeroGram Sozlamalari → Barcha akkauntlar → Yangi akkaunt qo‘shish» orqali bemalol qo‘shing va bir bosishda almashtiring.",
            "AeroGram иловасига исталганча (5, 10, 20 ва ундан кўп) Telegram аккаунтларини улашингиз мумкин. Профил менюси, Созламалар ёки «AeroGram Созламалари → Барча аккаунтлар → Янги аккаунт қўшиш» орқали бемалол қўшинг ва бир босишда алмаштиринг.",
            "Akkaunt ichidan «Chiqish» (Log out) orqali istalgan akkauntni uzishingiz mumkin.",
            "Аккаунт ичидан «Чиқиш» (Log out) орқали исталган аккаунтни узишингиз мумкин."),
        new GuideEntry(0xFF0EA5E9, R.drawable.msg_saved,
            "1-martalik Rasm, Video, Golos & Dumaloq video saqlash (?? reply)",
            "1-марталик Расм, Видео, Голос & Думалоқ видео сақлаш (?? reply)",
            "Standart holatda yoqilgan (Xabarlar tarixi va Himoya → 1-martalik rasm, video va goloslarni saqlash)",
            "Стандарт ҳолатда ёқилган (Хабарлар тарихи ва Ҳимоя → 1-марталик расм, видео ва голосларни сақлаш)",
            "Chatdagi istalgan 1-martalik rasm, video, ovozli xabar (golos) yoki dumaloq videoga Reply qilib «??» yozib yuboring — xabar chatga ketmaydi (suhbatdosh bilmaydi), fayl esa darhol Saved Messages ga doimiy nusxa bo‘lib tushadi! Shuningdek, xabarni bosib «Saved Messages ga saqlash» yoki ko‘rish oynasidagi saqlash tugmasidan foydalanishingiz mumkin.",
            "Чатдаги исталган 1-марталик расм, видео, овозли хабар (голос) ёки думалоқ видеога Reply қилиб «??» ёзиб юборинг — хабар чатга кетмайди (суҳбатдош билмайди), файл эса дарҳол Saved Messages га доимий нусха бўлиб тушади! Шунингдек, хабарни босиб «Saved Messages га сақлаш» ёки кўриш ойнасидаги сақлаш тугмасидан фойдаланишингиз мумкин.",
            "1-martalik media saqlash kalitini o‘chiring.", "1-марталик медиа сақлаш калитини ўчиринг."),
        new GuideEntry(0xFF6366F1, R.drawable.msg_stealth_locked,
            "Ghost rejimi (Ko‘rinmaslik)", "Ghost режими (Кўринмаслик)",
            "Sozlamalar → Ghost rejimi (yoki chat tepasidagi Ghost tugmasi)", "Созламалар → Ghost режими (ёки чат тепасидаги Ghost тугмаси)",
            "Xabar o‘qiganda ikki ko‘k belgi ketmaydi, onlayn va «yozmoqda...» holati yashiriladi, hikoyalar maxfiy ko‘riladi. «Yozganda o‘qilgan qilish» yoqilsa, faqat javob yozganingizda ko‘k belgi ketadi.",
            "Хабар ўқиганда икки кўк белги кетмайди, онлайн ва «ёзмоқда...» ҳолати яширилади, ҳикоялар махфий кўрилади. «Ёзганда ўқилган қилиш» ёқилса, фақат жавоб ёзганингизда кўк белги кетади.",
            "Ghost kalitlarini yoki chatdagi Ghost tugmasini o‘chiring.", "Ghost калитларини ёки чатдаги Ghost тугмасини ўчиринг."),
        new GuideEntry(0xFFEF4444, R.drawable.msg_delete,
            "O‘chirilgan xabarlarni saqlash (Anti-Delete)", "Ўчирилган хабарларни сақлаш (Anti-Delete)",
            "Standart holatda yoqilgan", "Стандарт ҳолатда ёқилган",
            "Suhbatdosh xabarni o‘chirsa ham u chatda saqlanib qoladi va ustida «🗑 O‘chirilgan» yozuvi chiqadi. O‘zingiz o‘chirgan xabar esa tasdiqdan so‘ng darhol o‘chadi.",
            "Суҳбатдош хабарни ўчирса ҳам у чатда сақланиб қолади ва устида «🗑 Ўчирилган» ёзуви чиқади. Ўзингиз ўчирган хабар эса тасдиқдан сўнг дарҳол ўчади.",
            "AeroGram Sozlamalari → O‘chirilgan xabarlarni saqlash.", "AeroGram Созламалари → Ўчирилган хабарларни сақлаш."),
        new GuideEntry(0xFFF97316, R.drawable.msg_recent,
            "Tahrirlar tarixi (Eski → Yangi)", "Таҳрирлар тарихи (Эски → Янги)",
            "Standart holatda yoqilgan", "Стандарт ҳолатда ёқилган",
            "Xabar tahrirlanganda avvalgi matn yuqorida, yangi matn esa pastida ko‘rinadi; xabarni bosib «Tahrirlar tarixi» menyusi orqali barcha eski versiyalarni ko‘rish va nusxalash mumkin.",
            "Хабар таҳрирланганда аввалги матн юқорида, янги матн эса пастида кўринади; хабарни босиб «Таҳрирлар тарихи» менюси орқали барча эски версияларни кўриш ва нусхалаш мумкин.",
            "AeroGram Sozlamalari → Tahrirlar tarixini saqlash.", "AeroGram Созламалари → Таҳрирлар тарихини сақлаш."),
        new GuideEntry(0xFF0F766E, R.drawable.msg_pin_code,
            "Yashirin va PIN-qulflangan chatlar", "Яширин ва ПИН-қулфланган чатлар",
            "Himoya → AeroGram PIN kodini sozlash / Qulflangan & Yashirin chatlar", "Ҳимоя → AeroGram ПИН кодини созлаш / Қулфланган & Яширин чатлар",
            "4 xonali PIN kod o‘rnating. Tanlangan chatlar PIN kodsiz ochilmaydi; yashirin chatlar esa umumiy ro‘yxatda umuman ko‘rinmaydi (vaqtincha ochish uchun PIN kiriting).",
            "4 хонали ПИН код ўрнатинг. Танланган чатлар ПИН кодсиз очилмайди; яширин чатлар эса умумий рўйхатда умуман кўринмайди (вақтинча очиш учун ПИН киритинг).",
            "Chat tanlov ro‘yxatidan belgini olib tashlang.", "Чат танлов рўйхатидан белгини олиб ташланг."),
        new GuideEntry(0xFFDC2626, R.drawable.passcode_check,
            "Skreenshot va chet el raqamlari himoyasi", "Скриншот ва чет эл рақамлари ҳимояси",
            "Himoya → Skreenshot himoyasi / Chet el raqamlarini bloklash", "Ҳимоя → Скриншот ҳимояси / Чет эл рақамларини блоклаш",
            "Skreenshot himoyasi ekran tasvirga olishni taqiqlaydi. Chet el blokirovkasi +998 bo‘lmagan notanish xalqaro raqamlardan keladigan spamni to‘sadi.",
            "Скриншот ҳимояси экран тасвирга олишни тақиқлайди. Чет эл блокировкаси +998 бўлмаган нотаниш халқаро рақамлардан келадиган спамни тўсади.",
            "Himoya bo‘limidagi kalitlarni o‘chiring.", "Ҳимоя бўлимидаги калитларни ўчиринг."),
        new GuideEntry(0xFF0284C7, R.drawable.msg_translate,
            "Avto-tarjima (Kiruvchi va Chiquvchi)", "Авто-таржима (Кирувчи ва Чиқувчи)",
            "Xabarlar → Avto-tarjima", "Хабарлар → Авто-таржима",
            "Xorijiy tildagi xabarlarni chat ichida avtomatik tarjima qiladi, shuningdek yozayotgan xabaringizni yuborishdan oldin tanlangan tilga (masalan Ingliz/Rus) o‘giradi.",
            "Хорижий тилдаги хабарларни чат ичида автоматик таржима қилади, шунингдек ёзаётган хабарингизни юборишдан олдин танланган тилга (масалан Инглиз/Рус) ўгиради.",
            "Avto-tarjima bo‘limidan o‘chiring.", "Авто-таржима бўлимидан ўчиринг."),
        new GuideEntry(0xFF2563EB, R.drawable.msg_voicechat,
            "Ovoz → Matn (STT) & Ovoz tarjimasi", "Овоз → Матн (STT) & Овоз таржимаси",
            "Ovoz → Matn (STT) bo‘limi", "Овоз → Матн (STT) бўлими",
            "Ovozli va dumaloq xabarlar ostidagi «A→» belgisini bossangiz, ovozni matnga aylantiradi; agar STT tili yoki Avto-tarjima tanlangan bo‘lsa, matn darhol tarjima bilan chiqadi.",
            "Овозли ва думалоқ хабарлар остидаги «A→» белгисини боссангиз, овозни матнга айлантиради; агар STT тили ёки Авто-таржима танланган бўлса, матн дарҳол таржима билан чиқади.",
            "Ovoz → Matn kalitini o‘chiring.", "Овоз → Матн калитини ўчиринг."),
        new GuideEntry(0xFF7C3AED, R.drawable.msg_text_check,
            "Yangi o‘zbek alifbosi (ş ç ō ḡ) & Konverter", "Янги ўзбек алифбоси (ş ç ō ḡ) & Конвертер",
            "Xabarlar → Yangi o‘zbek alifbosi", "Хабарлар → Янги ўзбек алифбоси",
            "Yuboriladigan yoki chatdagi matnlarda sh→ş, ch→ç, o‘→ō, g‘→ḡ shakllarini avtomatik qo‘llaydi hamda tayyor Lotin ↔ Kirill konverterini o‘z ichiga oladi.",
            "Юбориладиган ёки чатдаги матнларда sh→ş, ch→ç, o‘→ō, g‘→ḡ шаклларини автоматик қўллайди ҳамда тайёр Лотин ↔ Кирилл конвертерини ўз ичига олади.",
            "Yangi o‘zbek alifbosi bo‘limidan o‘chiring.", "Янги ўзбек алифбоси бўлимидан ўчиринг."),
        new GuideEntry(0xFFA855F7, R.drawable.msg_text_outlined,
            "Matn uslubi, Avto-postfiks va ❤️ Effekti", "Матн услуби, Авто-постфикс ва ❤️ Эффекти",
            "Xabarlar → Matn uslubi / Avto-postfiks / Yurakcha", "Хабарлар → Матн услуби / Авто-постфикс / Юракча",
            "Yuboriladigan har bir matnni Qalin, Kursiv, Monoshift yoki Spoiler ko‘rinishida bezaydi, oxiriga shaxsiy imzo yoki ❤️ belgisini qo‘shadi.",
            "Юбориладиган ҳар бир матнни Қалин, Курсив, Моношифт ёки Спойлер кўринишида безайди, охирига шахсий имзо ёки ❤️ белгисини қўшади.",
            "Matn uslubini Oddiy ga qo‘ying yoki kalitlarni o‘chiring.", "Матн услубини Оддий га қўйинг ёки калитларни ўчиринг."),
        new GuideEntry(0xFF3B82F6, R.drawable.menu_reply,
            "Double-Tap, Muallifsiz Forward va Stiker tasdiqlash", "Double-Tap, Муаллифсиз Forward ва Стикер тасдиқлаш",
            "Xabarlar → Ikki marta bosganda / Forward / Stiker tasdiqlash", "Хабарлар → Икки марта босганда / Forward / Стикер тасдиқлаш",
            "Xabarni ikki marta bosish amalini (Reply, Copy, Translate, Reaksiya) sozlaydi, forward da muallif/izohni yashiradi va stiker/GIF bosilganda tasdiq so‘raydi.",
            "Хабарни икки марта босиш амалини (Reply, Copy, Translate, Реакция) созлайди, forward да муаллиф/изоҳни яширади ва стикер/GIF босилганда тасдиқ сўрайди.",
            "Tegishli kalitlardan o‘zgartiring.", "Тегишли калитлардан ўзгартиринг."),
        new GuideEntry(0xFF10B981, R.drawable.msg_calls,
            "Qo‘ng‘iroq tasdiqlash & Qo‘ng‘iroq yozib olish", "Қўнғироқ тасдиқлаш & Қўнғироқ ёзиб олиш",
            "Chat va Qo‘ng‘iroqlar → Tasdiqlash / Yozib olish", "Чат ва Қўнғироқлар → Тасдиқлаш / Ёзиб олиш",
            "Tasodifiy qo‘ng‘iroq qilishdan himoyalaydi. «Qo‘ng‘iroqlarni yozib olish» yoqilsa, suhbat ulanganda audio .m4a formatda AeroGramCalls papkasiga yozib boriladi (ayrim Android qurilmalarda tizim cheklovi sabab faqat mikrofon ovozi yozilishi mumkin).",
            "Тасодифий қўнғироқ қилишдан ҳимоялайди. «Қўнғироқларни ёзиб олиш» ёқилса, суҳбат уланганда аудио .m4a форматда AeroGramCalls папкасига ёзиб борилади (айрим Android қурилмаларда тизим чеклови сабаб фақат микрофон овози ёзилиши мумкин).",
            "Kalitlarni o‘chiring.", "Калитларни ўчиринг."),
        new GuideEntry(0xFFF59E0B, R.drawable.msg_notifications,
            "O‘qilmagan xabar eslatmasi (Reminder)", "Ўқилмаган хабар эслатмаси (Reminder)",
            "Tarmoq, Papkalar va Eslatmalar → Eslatma yoqish / Vaqt / Ovoz", "Тармоқ, Папкалар ва Эслатмалар → Эслатма ёқиш / Вақт / Овоз",
            "Ovozsiz (Mute) qilinmagan chatda xabarni belgilangan daqiqadan ko‘proq vaqt o‘qimasangiz, ilova ichida eslatma chiqaradi va xohlasangiz signal chaladi.",
            "Овозсиз (Mute) қилинмаган чатда хабарни белгиланган дақиқадан кўпроқ вақт ўқимасангиз, илова ичида эслатма чиқаради ва хоҳласангиз сигнал чалади.",
            "Eslatma kalitini o‘chiring.", "Эслатма калитини ўчиринг."),
        new GuideEntry(0xFF06B6D4, R.drawable.msg_camera,
            "Galereyadan dumaloq video & Kamera sozlamalari", "Галереядан думалоқ видео & Камера созламалари",
            "Interfeys → Galereyadan dumaloq / Kamera tanlash / Orqa kamera", "Интерфейс → Галереядан думалоқ / Камера танлаш / Орқа камера",
            "Galereyadan 65 soniyagacha bo‘lgan oddiy videoni dumaloq video-xabar qilib yuborishingiz, kamera rejimiga o‘tganda Old/Orqa kamerani tanlashingiz yoki doim orqa kamera bilan boshlashingiz mumkin.",
            "Галереядан 65 сониягача бўлган оддий видеони думалоқ видео-хабар қилиб юборишингиз, камера режимига ўтганда Олд/Орқа камерани танлашингиз ёки доим орқа камера билан бошлашингиз мумкин.",
            "Tegishli kalitlarni o‘chiring.", "Тегишли калитларни ўчиринг."),
        new GuideEntry(0xFF3B82F6, R.drawable.msg_folders,
            "Papkalar, Cheksiz Pin, Yorug‘ Aksent, Stories HD, Ilova Ikonkasi & Proksi",
            "Папкалар, Чексиз Пин, Ёруғ Аксент, Stories HD, Илова Иконкаси & Прокси",
            "Tarmoq & Interfeys bo‘limlari", "Тармоқ & Интерфейс бўлимлари",
            "Papkalarni ikonka uslubiga o‘tkazish, 5 tadan ko‘p chatni qadash (Pin), 1.2K o‘rniga aniq raqamlar, Xabar ID/DC/soniyalar, Yorug‘ mavzuda AeroGram osmon aksenti, Stories HD yuklash, 6 ta ilova ikonkasi va AeroGramProxy hammasi shu yerda!",
            "Папкаларни иконка услубига ўтказиш, 5 тадан кўп чатни қадаш (Pin), 1.2K ўрнига аниқ рақамлар, Хабар ID/DC/сониялар, Ёруғ мавзуда AeroGram осмон акценти, Stories HD юклаш, 6 та илова иконкаси ва AeroGramProxy ҳаммаси шу ерда!",
            "Har bir funksiya alohida kalit bilan boshqariladi.", "Ҳар бир функция алоҳида калит билан бошқарилади.")
    };

    private View getOrCreateAboutIntro(Context context) {
        if (aboutIntroView != null) {
            return aboutIntroView;
        }
        LinearLayout wrap = new LinearLayout(context);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(20), dp(18), dp(20), dp(18));

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setCornerRadius(dp(6));
        badgeBg.setColor(0xFF0284C7);
        TextView badge = new TextView(context);
        badge.setText("AEROGRAM v1.4.2 • BUILD 1008");
        badge.setTypeface(AndroidUtilities.bold());
        badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        badge.setTextColor(Color.WHITE);
        badge.setPadding(dp(10), dp(4), dp(10), dp(4));
        badge.setBackground(badgeBg);
        wrap.addView(badge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        TextView desc = new TextView(context);
        desc.setText(AeroConfig.tr(
            "AeroGram — Telegram bazasida yaratilgan tezkor, maxfiy va qulay mijoz. Ghost rejimi, Anti-Delete, 1-martalik media saqlash (?? reply), Tahrirlar tarixi, PIN qulf, Avto-tarjima va O‘zbek tili uchun maxsus vositalarni o‘z ichiga oladi.",
            "AeroGram — Telegram базасида яратилган тезкор, махфий ва қулай мижоз. Ghost режими, Anti-Delete, 1-марталик медиа сақлаш (?? reply), Таҳрирлар тарихи, ПИН қулф, Авто-таржима ва Ўзбек тили учун махсус воситаларни ўз ичига олади."
        ));
        desc.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        desc.setLineSpacing(dp(2), 1.0f);
        desc.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        wrap.addView(desc, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 10, 0, 0));

        aboutIntroView = wrap;
        return aboutIntroView;
    }

    private View getOrCreateGuideIntro(Context context) {
        if (guideIntroView != null) {
            return guideIntroView;
        }
        LinearLayout wrap = new LinearLayout(context);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(20), dp(18), dp(20), dp(18));

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setCornerRadius(dp(6));
        badgeBg.setColor(0xFF0EA5E9);
        TextView badge = new TextView(context);
        badge.setText(AeroConfig.tr("QANDAY ISHLATILADI?", "ҚАНДАЙ ИШЛАТИЛАДИ?"));
        badge.setTypeface(AndroidUtilities.bold());
        badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        badge.setTextColor(Color.WHITE);
        badge.setPadding(dp(10), dp(4), dp(10), dp(4));
        badge.setBackground(badgeBg);
        wrap.addView(badge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        TextView desc = new TextView(context);
        desc.setText(AeroConfig.tr(
            "Quyida AeroGram dagi har bir maxsus funksiyani qayerdan yoqish, amalda qanday ishlatish va kerak bo‘lsa qanday o‘chirish bo‘yicha to‘liq yo‘riqnoma keltirilgan.",
            "Қуйида AeroGram даги ҳар бир махсус функцияни қаердан ёқиш, амалда қандай ишлатиш ва керак бўлса қандай ўчириш бўйича тўлиқ йўриқнома келтирилган."
        ));
        desc.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        desc.setLineSpacing(dp(2), 1.0f);
        desc.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        wrap.addView(desc, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 10, 0, 0));

        guideIntroView = wrap;
        return guideIntroView;
    }

    private void fillAboutItems(ArrayList<UItem> items) {
        if (getParentActivity() != null) {
            items.add(UItem.asCustom(1, getOrCreateAboutIntro(getParentActivity())));
            items.add(UItem.asShadow(null));
        }

        items.add(UItem.asHeader(AeroConfig.tr("Ilova ma’lumotlari va Ko‘rsatkichlar", "Илова маълумотлари ва Кўрсаткичлар")));
        items.add(AeroRowCell.Factory.ofNav(200, 0xFF0EA5E9, R.drawable.msg_help,
            AeroConfig.tr("AeroGram Qo‘llanmasi", "AeroGram Қўлланмаси"),
            AeroConfig.tr("Barcha funksiyalarni yoqish va ishlatish yo‘riqnomasini ochish", "Барча функцияларни ёқиш ва ишлатиш йўриқномасини очиш"),
            AeroConfig.tr("Ochish", "Очиш"), AeroRowCell.BADGE_NEW));
        items.add(AeroRowCell.Factory.ofNav(201, 0xFF10B981, R.drawable.msg_stats,
            AeroConfig.tr("AeroGram Analitika", "AeroGram Аналитика"),
            AeroConfig.tr("Saqlangan o‘chirilganlar: " + AeroDeletedStore.getTotalSavedCount()
                + " • Tahrirlar: " + AeroEditHistoryStore.getTotalCount()
                + " • Spam bloklar: " + AeroConfig.statBlockedForeignCount,
                "Сақланган ўчирилганлар: " + AeroDeletedStore.getTotalSavedCount()
                + " • Таҳрирлар: " + AeroEditHistoryStore.getTotalCount()
                + " • Спам блоклар: " + AeroConfig.statBlockedForeignCount),
            AeroConfig.tr("Batafsil", "Батафсил"), AeroRowCell.BADGE_AERO));

        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader(AeroConfig.tr("Rasmiy havolalar", "Расмий ҳаволалар")));
        items.add(AeroRowCell.Factory.ofNav(202, 0xFF0284C7, R.drawable.msg_info,
            AeroConfig.tr("Rasmiy veb-sayt", "Расмий веб-сайт"),
            "https://aerogram-itz8.onrender.com/",
            AeroConfig.tr("Ochish", "Очиш"), AeroRowCell.BADGE_NONE));
        items.add(AeroRowCell.Factory.ofNav(203, 0xFF6366F1, R.drawable.msg_share,
            "GitHub Relizlar",
            "https://github.com/Bauntyme/AeroGram/releases",
            "v1.4.2", AeroRowCell.BADGE_NONE));

        items.add(UItem.asShadow(AeroConfig.tr(
            "AeroGram v1.4.2 (Build 1008) • Telegram Android ochiq manbali loyihasi asosida tuzilgan.",
            "AeroGram v1.4.2 (Build 1008) • Telegram Android очиқ манбали лойиҳаси асосида тузилган."
        )));
    }

    private void fillGuideItems(ArrayList<UItem> items) {
        if (getParentActivity() != null) {
            items.add(UItem.asCustom(1, getOrCreateGuideIntro(getParentActivity())));
            items.add(UItem.asShadow(null));
        }
        for (int i = 0; i < GUIDE_CARDS.length; i++) {
            items.add(GuideCardFactory.ofGuide(100 + i, GUIDE_CARDS[i], AeroConfig.uiCyrillic));
        }
        items.add(UItem.asShadow(AeroConfig.tr(
            "Istalgan funksiyani AeroGram Sozlamalari orqali darhol yoqishingiz yoki o‘chirishingiz mumkin.",
            "Исталган функцияни AeroGram Созламалари орқали дарҳол ёқишингиз ёки ўчиришингиз мумкин."
        )));
    }

    public static class GuideCardCell extends LinearLayout implements Theme.Colorable {
        private final ImageView iconView;
        private final FrameLayout iconBox;
        private final TextView titleView;
        private final TextView bodyView;
        private final Theme.ResourcesProvider resourcesProvider;
        private boolean needDivider;

        public GuideCardCell(Context context, Theme.ResourcesProvider resourcesProvider) {
            super(context);
            this.resourcesProvider = resourcesProvider;
            setOrientation(VERTICAL);
            setWillNotDraw(false);
            setPadding(dp(18), dp(14), dp(18), dp(14));

            LinearLayout header = new LinearLayout(context);
            header.setOrientation(HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);

            iconBox = new FrameLayout(context);
            iconView = new ImageView(context);
            iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            iconView.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
            iconBox.addView(iconView, LayoutHelper.createFrame(18, 18, Gravity.CENTER));
            header.addView(iconBox, LayoutHelper.createLinear(32, 32, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

            titleView = new TextView(context);
            titleView.setTypeface(AndroidUtilities.bold());
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15.5f);
            header.addView(titleView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

            addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            bodyView = new TextView(context);
            bodyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
            bodyView.setLineSpacing(dp(2.5f), 1.0f);
            addView(bodyView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));

            updateColors();
        }

        public void bind(GuideEntry entry, boolean divider) {
            this.needDivider = divider;
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(9));
            bg.setColor(entry.color);
            iconBox.setBackground(bg);
            iconView.setImageResource(entry.icon);
            titleView.setText(AeroConfig.tr(entry.latTitle, entry.cyrTitle));

            String l1 = AeroConfig.tr("YOQISH: ", "ЁҚИШ: ");
            String v1 = AeroConfig.tr(entry.latOn, entry.cyrOn);
            String l2 = AeroConfig.tr("ISHLASHI: ", "ИШЛАШИ: ");
            String v2 = AeroConfig.tr(entry.latHow, entry.cyrHow);
            String l3 = AeroConfig.tr("O‘CHIRISH: ", "ЎЧИРИШ: ");
            String v3 = AeroConfig.tr(entry.latOff, entry.cyrOff);

            SpannableStringBuilder sb = new SpannableStringBuilder();
            appendLabeled(sb, l1, v1 + "\n", 0xFF10B981);
            appendLabeled(sb, l2, v2 + "\n", 0xFF0284C7);
            appendLabeled(sb, l3, v3, 0xFFEF4444);
            bodyView.setText(sb);
            updateColors();
        }

        private void appendLabeled(SpannableStringBuilder sb, String label, String value, int labelColor) {
            int start = sb.length();
            sb.append(label);
            sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new ForegroundColorSpan(labelColor), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.append(value);
        }

        @Override
        public void updateColors() {
            titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
            bodyView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            super.onDraw(canvas);
            if (needDivider && Theme.dividerPaint != null) {
                canvas.drawLine(dp(62), getMeasuredHeight() - 1, getMeasuredWidth(), getMeasuredHeight() - 1, Theme.dividerPaint);
            }
        }
    }

    public static class GuideCardFactory extends UItem.UItemFactory<GuideCardCell> {
        private static final GuideCardFactory INSTANCE = new GuideCardFactory();
        static { setup(INSTANCE); }

        public static UItem ofGuide(int id, GuideEntry entry, boolean cyrillic) {
            setup(INSTANCE);
            UItem item = UItem.ofFactory(GuideCardFactory.class);
            item.id = id;
            item.object = entry;
            item.checked = cyrillic;
            return item;
        }

        @Override
        public boolean isClickable() {
            return false;
        }

        @Override
        public GuideCardCell createView(Context context, RecyclerListView listView, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            return new GuideCardCell(context, resourcesProvider);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView listView) {
            if (item.object instanceof GuideEntry) {
                ((GuideCardCell) view).bind((GuideEntry) item.object, divider);
            }
        }

        @Override
        public boolean equals(UItem a, UItem b) {
            return a.id == b.id && a.object == b.object;
        }

        @Override
        public boolean contentsEquals(UItem a, UItem b) {
            return a.id == b.id && a.object == b.object && a.checked == b.checked;
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (mode == MODE_GUIDE || mode == MODE_STATS) {
            return;
        } else if (mode == MODE_ABOUT) {
            if (item.id == 200) {
                presentFragment(new AeroSubSettingsActivity(MODE_GUIDE));
            } else if (item.id == 201) {
                presentFragment(new AeroSubSettingsActivity(MODE_STATS));
            } else if (item.id == 202 && getParentActivity() != null) {
                Browser.openUrl(getParentActivity(), "https://aerogram-itz8.onrender.com/");
            } else if (item.id == 203 && getParentActivity() != null) {
                Browser.openUrl(getParentActivity(), "https://github.com/Bauntyme/AeroGram/releases");
            }
            return;
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
                presentFragment(new LoginActivity(UserConfig.allocateAccountSlot()));
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
        } else if (mode == MODE_NEW_ALPHABET) {
            if (item.id == 1) {
                AeroConfig.newUzbekAlphabet = !AeroConfig.newUzbekAlphabet;
                AeroConfig.saveBool("newUzbekAlphabet", AeroConfig.newUzbekAlphabet);
                reloadList();
            } else if (item.id == 2) {
                AeroConfig.newUzbekAlphabetInChats = !AeroConfig.newUzbekAlphabetInChats;
                AeroConfig.saveBool("newUzbekAlphabetInChats", AeroConfig.newUzbekAlphabetInChats);
                reloadList();
            } else if (item.id == 3) {
                AeroConfig.uiCyrillic = !AeroConfig.uiCyrillic;
                AeroConfig.saveBool("uiCyrillic", AeroConfig.uiCyrillic);
                if (actionBar != null) actionBar.setTitle(getTitle());
                reloadList();
            } else if (item.id == 4) {
                showAlphabetConverterDialog();
            }
        } else if (mode == MODE_POSTFIX) {
            if (item.id == 1) {
                AeroConfig.autoPostfixEnabled = !AeroConfig.autoPostfixEnabled;
                AeroConfig.saveBool("autoPostfixEnabled", AeroConfig.autoPostfixEnabled);
                reloadList();
            }
        } else if (mode == MODE_AUTO_TRANSLATE) {
            if (item.id == 1) {
                AeroConfig.autoTranslate = !AeroConfig.autoTranslate;
                AeroConfig.saveBool("autoTranslate", AeroConfig.autoTranslate);
                reloadList();
            } else if (item.id == 2) {
                AeroConfig.autoTranslateOutgoing = !AeroConfig.autoTranslateOutgoing;
                AeroConfig.saveBool("autoTranslateOutgoing", AeroConfig.autoTranslateOutgoing);
                reloadList();
            } else if (item.id == 3) {
                AeroConfig.askBeforeTranslateSend = !AeroConfig.askBeforeTranslateSend;
                AeroConfig.saveBool("askBeforeTranslateSend", AeroConfig.askBeforeTranslateSend);
                reloadList();
            } else if (item.id == 4) {
                AeroConfig.translateWholeChat = !AeroConfig.translateWholeChat;
                AeroConfig.saveBool("translateWholeChat", AeroConfig.translateWholeChat);
                reloadList();
            } else if (item.id >= 100 && item.id - 100 < LANGUAGES.length) {
                AeroConfig.translateTargetLang = LANGUAGES[item.id - 100][0];
                AeroConfig.saveString("translateTargetLang", AeroConfig.translateTargetLang);
                reloadList();
            }
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
