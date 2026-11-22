package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.telegram.messenger.AeroConfig;
import org.telegram.messenger.AeroDeletedStore;
import org.telegram.messenger.AeroEditHistoryStore;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;
import java.util.Locale;

public class AeroSettingsActivity extends UniversalFragment {

    private static final int MENU_SEARCH = 1;
    private static final int MENU_SCRIPT = 2;
    private static final int MENU_SHARE = 3;

    // Header & Accounts
    private static final int ID_ABOUT = 2;
    private static final int ID_GUIDE = 4;
    private static final int ID_ACCOUNTS = 3;

    // 1. Privacy & Ghost Mode
    private static final int ID_GHOST_MASTER = 10;
    private static final int ID_GHOST_DONT_READ = 11;
    private static final int ID_GHOST_READ_ON_INTERACT = 12;
    private static final int ID_GHOST_OFFLINE = 13;
    private static final int ID_GHOST_TYPING = 14;
    private static final int ID_GHOST_STORY = 15;
    private static final int ID_SAVE_DELETED = 16;
    private static final int ID_EDIT_HISTORY = 17;
    private static final int ID_SAVE_ONCE_MEDIA = 24;
    private static final int ID_SCREENSHOT_PROTECT = 18;
    private static final int ID_BLOCK_FOREIGN = 19;
    private static final int ID_CHAT_LOCK_ENABLE = 20;
    private static final int ID_CHAT_LOCK_CHATS = 21;
    private static final int ID_HIDDEN_CHATS = 22;
    private static final int ID_SET_PIN = 23;

    // 2. Translate & Voice-to-Text
    private static final int ID_AUTO_TRANSLATE_PAGE = 30;
    private static final int ID_TRANSLATE_LANG = 31;
    private static final int ID_VOICE_TO_TEXT = 35;
    private static final int ID_VOICE_LANG = 36;
    private static final int ID_AUTO_TRANSLATE_VOICE = 37;

    // 3. Messages & Sending
    private static final int ID_TEXT_STYLE = 40;
    private static final int ID_NEW_ALPHABET_PAGE = 41;
    private static final int ID_POSTFIX_PAGE = 42;
    private static final int ID_HEART_EFFECT = 44;
    private static final int ID_AUTO_STICKER = 45;
    private static final int ID_FWD_NO_AUTHOR = 46;
    private static final int ID_FWD_NO_CAPTION = 47;
    private static final int ID_DOUBLE_TAP = 48;
    private static final int ID_CONFIRM_CALL = 49;
    private static final int ID_CONFIRM_SEND_MEDIA = 50;
    private static final int ID_SHOW_ID_DC = 51;
    private static final int ID_SHOW_SECONDS = 52;
    private static final int ID_JUMP_FIRST = 53;
    private static final int ID_CHANNEL_HISTORY = 54;

    // 4. Network, Folders & Reminders
    private static final int ID_AERO_PROXY = 60;
    private static final int ID_REMINDER_ENABLE = 61;
    private static final int ID_REMINDER_DELAY = 62;
    private static final int ID_REMINDER_SOUND = 63;
    private static final int ID_FOLDER_STYLE = 64;
    private static final int ID_ADMIN_FOLDERS = 65;
    private static final int ID_RECOMMENDED_FOLDERS = 66;
    private static final int ID_HIDE_FOLDERS = 67;
    private static final int ID_AUTO_ACCEPT_REQUESTS = 68;

    // 5. Interface, Stories, Calls & App Icon
    private static final int ID_APP_ICON = 70;
    private static final int ID_SHOW_STORIES = 71;
    private static final int ID_SAVE_STORIES_HD = 72;
    private static final int ID_SHOW_GHOST_BTN = 73;
    private static final int ID_LIGHT_BRAND_ACCENT = 74;
    private static final int ID_RECORD_CALLS = 75;
    private static final int ID_DISABLE_ROUNDING = 76;
    private static final int ID_UNLIMITED_PINS = 77;
    private static final int ID_SHOW_MUTUAL = 78;
    private static final int ID_GALLERY_ROUND = 79;
    private static final int ID_ASK_CAMERA = 80;
    private static final int ID_CAMERA_HOLD = 81;
    private static final int ID_PREFER_BACK_CAM = 82;
    private static final int ID_DISABLE_AUTODOWNLOAD = 83;
    private static final int ID_SCRIPT_CYRILLIC = 84;

    // 6. Data & Sharing
    private static final int ID_CLEAR_HISTORY_STORE = 90;
    private static final int ID_SHARE_SETTINGS_LINK = 91;
    private static final int ID_RESET_ALL = 92;

    private String searchQuery = "";
    private boolean searching = false;
    private ActionBarMenuItem scriptItem;

    @Override
    protected CharSequence getTitle() {
        return AeroConfig.tr("AeroGram Sozlamalari", "AeroGram Созламалари");
    }

    @Override
    public View createView(Context context) {
        View root = super.createView(context);
        ActionBarMenu menu = actionBar.createMenu();

        ActionBarMenuItem searchItem = menu.addItem(MENU_SEARCH, R.drawable.msg_search).setIsSearchField(true);
        searchItem.setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            @Override
            public void onSearchExpand() {
                searching = true;
                reloadList();
            }

            @Override
            public void onSearchCollapse() {
                searching = false;
                searchQuery = "";
                reloadList();
            }

            @Override
            public void onTextChanged(EditText editText) {
                searchQuery = editText.getText() == null ? "" : editText.getText().toString().trim().toLowerCase(Locale.US);
                reloadList();
            }
        });
        searchItem.setSearchFieldHint(AeroConfig.tr("Qidirish...", "Қидириш..."));

        scriptItem = menu.addItem(MENU_SCRIPT, R.drawable.msg_translate);
        scriptItem.setContentDescription(AeroConfig.tr("Lotin / Kirill", "Лотин / Кирилл"));

        ActionBarMenuItem moreItem = menu.addItem(0, R.drawable.ic_ab_other);
        moreItem.addSubItem(MENU_SHARE, R.drawable.msg_share, AeroConfig.tr("Sozlamalar havolasini ulashish", "Созламалар ҳаволасини улашиш"));

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_SCRIPT) {
                    AeroConfig.uiCyrillic = !AeroConfig.uiCyrillic;
                    AeroConfig.saveBool("uiCyrillic", AeroConfig.uiCyrillic);
                    actionBar.setTitle(getTitle());
                    Toast.makeText(getParentActivity(),
                        AeroConfig.uiCyrillic ? "Кирилл ёзуви ёқилди" : "Lotin yozuvi yoqildi",
                        Toast.LENGTH_SHORT).show();
                    reloadList();
                } else if (id == MENU_SHARE) {
                    shareSettingsLink();
                }
            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (actionBar != null) {
            actionBar.setTitle(getTitle());
        }
        reloadList();
    }

    private void reloadList() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(false);
        }
    }

    private boolean matches(String t1, String t2) {
        if (!searching || TextUtils.isEmpty(searchQuery)) {
            return true;
        }
        String a = t1 == null ? "" : t1.toLowerCase(Locale.US);
        String b = t2 == null ? "" : t2.toLowerCase(Locale.US);
        return a.contains(searchQuery) || b.contains(searchQuery);
    }

    private void addSwitchIfMatches(ArrayList<UItem> items, int id, int color, int icon, String latTitle, String cyrTitle, String latSub, String cyrSub, boolean checked, int badge) {
        String title = AeroConfig.tr(latTitle, cyrTitle);
        String sub = AeroConfig.tr(latSub, cyrSub);
        if (matches(latTitle + " " + cyrTitle, latSub + " " + cyrSub)) {
            items.add(AeroRowCell.Factory.ofSwitch(id, color, icon, title, sub, checked, badge));
        }
    }

    private void addNavIfMatches(ArrayList<UItem> items, int id, int color, int icon, String latTitle, String cyrTitle, String latSub, String cyrSub, String value, int badge) {
        String title = AeroConfig.tr(latTitle, cyrTitle);
        String sub = AeroConfig.tr(latSub, cyrSub);
        if (matches(latTitle + " " + cyrTitle, latSub + " " + cyrSub)) {
            items.add(AeroRowCell.Factory.ofNav(id, color, icon, title, sub, value, badge));
        }
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        int activeAccounts = 0;
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated()) {
                activeAccounts++;
            }
        }

        addNavIfMatches(items, ID_ABOUT, 0xFF0284C7, R.drawable.msg_info,
            "AeroGram haqida", "AeroGram ҳақида",
            "Ilova versiyasi (v1.4.0), statistika va rasmiy havolalar", "Илова версияси (v1.4.0), статистика ва расмий ҳаволалар",
            "v1.4.0", AeroRowCell.BADGE_NONE);
        addNavIfMatches(items, ID_GUIDE, 0xFF0EA5E9, R.drawable.msg_help,
            "AeroGram Qo‘llanmasi", "AeroGram Қўлланмаси",
            "Barcha funksiyalarni yoqish, ishlatish va o‘chirish yo‘riqnomasi", "Барча функцияларни ёқиш, ишлатиш ва ўчириш йўриқномаси",
            AeroConfig.tr("To‘liq", "Тўлиқ"), AeroRowCell.BADGE_NEW);

        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(AeroConfig.tr("Akkauntlar", "Аккаунтлар")));
        addNavIfMatches(items, ID_ACCOUNTS, 0xFF10B981, R.drawable.msg_groups,
            "Barcha akkauntlar", "Барча аккаунтлар",
            "Faol: " + activeAccounts + " ta • Maksimal " + UserConfig.MAX_ACCOUNT_COUNT + " tagacha",
            "Фаол: " + activeAccounts + " та • Максимал " + UserConfig.MAX_ACCOUNT_COUNT + " тагача",
            activeAccounts + " / " + UserConfig.MAX_ACCOUNT_COUNT, AeroRowCell.BADGE_NONE);

        items.add(UItem.asShadow(null));

        // 1. Ghost Mode
        items.add(UItem.asHeader(AeroConfig.tr("Ghost rejimi", "Ghost режими")));
        addSwitchIfMatches(items, ID_GHOST_MASTER, 0xFF6366F1, R.drawable.msg_stealth_locked,
            "Barcha Ghost funksiyalari", "Барча Ghost функциялари",
            "O‘qish, onlayn, yozish va story ko‘rishlarini birga yashiradi", "Ўқиш, онлайн, ёзиш ва story кўришларини бирга яширади",
            AeroConfig.isAnyGhostActive(), AeroRowCell.BADGE_AERO);
        addSwitchIfMatches(items, ID_GHOST_DONT_READ, 0xFF4F46E5, R.drawable.msg_views,
            "O‘qilganligini yashirish", "Ўқилганлигини яшириш",
            "Xabarlarni o‘qiganda ikki ko‘k belgi ketmaydi", "Хабарларни ўқиганда икки кўк белги кетмайди",
            AeroConfig.ghostDontReadMessages, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_GHOST_READ_ON_INTERACT, 0xFF818CF8, R.drawable.menu_reply,
            "Yozganda o‘qilgan qilish", "Ёзганда ўқилган қилиш",
            "Faqat siz javob yozganingizda chat o‘qilgan deb belgilanadi", "Фақат сиз жавоб ёзганингизда чат ўқилган деб белгиланади",
            AeroConfig.ghostReadOnInteract, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_GHOST_OFFLINE, 0xFF4338CA, R.drawable.msg_stealth_25min,
            "Onlayn holatini yashirish", "Онлайн ҳолатини яшириш",
            "Ilova ochiq bo‘lsa ham oxirgi kirish vaqti va Onlayn ko‘rinmaydi", "Илова очиқ бўлса ҳам охирги кириш вақти ва Онлайн кўринмайди",
            AeroConfig.ghostStayOffline, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_GHOST_TYPING, 0xFF7C3AED, R.drawable.msg_text_check,
            "Yozish va ovoz yozish holatini yashirish", "Ёзиш ва овоз ёзиш ҳолатини яшириш",
            "Suhbatdoshga «yozmoqda...» statusi yuborilmaydi", "Суҳбатдошга «ёзмоқда...» статуси юборилмайди",
            AeroConfig.ghostDontSendTyping, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_GHOST_STORY, 0xFF9333EA, R.drawable.msg_stories_views,
            "Hikoya (Story) ko‘rishlarini yashirish", "Ҳикоя (Story) кўришларини яшириш",
            "Story ko‘rganingiz muallifga bildirilmaydi", "Story кўрганингиз муаллифга билдирилмайди",
            AeroConfig.ghostHideStoryViews, AeroRowCell.BADGE_NONE);

        items.add(UItem.asShadow(null));

        // 2. Xabarlar tarixi va Himoya
        items.add(UItem.asHeader(AeroConfig.tr("Xabarlar tarixi va Himoya", "Хабарлар тарихи ва Ҳимоя")));
        addSwitchIfMatches(items, ID_SAVE_DELETED, 0xFFEF4444, R.drawable.msg_delete,
            "O‘chirilgan xabarlarni saqlash", "Ўчирилган хабарларни сақлаш",
            "Suhbatdosh o‘chirgan xabar va mediani chatda saqlaydi (o‘zingizniki 1 bosishda o‘chadi)", "Суҳбатдош ўчирган хабар ва медиани чатда сақлайди (ўзингизники 1 босишда ўчади)",
            AeroConfig.saveDeletedMessages, AeroRowCell.BADGE_NEW);
        addSwitchIfMatches(items, ID_EDIT_HISTORY, 0xFFF97316, R.drawable.msg_recent,
            "Tahrirlar tarixini saqlash", "Таҳрирлар тарихини сақлаш",
            "Avval Eski matn, pastida Yangi matn va barcha tahrir versiyalari", "Аввал Эски матн, пастида Янги матн ва барча таҳрир версиялари",
            AeroConfig.saveEditHistory, AeroRowCell.BADGE_NEW);
        addSwitchIfMatches(items, ID_SAVE_ONCE_MEDIA, 0xFF0EA5E9, R.drawable.msg_saved,
            "1-martalik rasm, video va goloslarni saqlash", "1-марталик расм, видео ва голосларни сақлаш",
            "1-martalik media xabarga «??» deb reply qiling yoki menyudan Saved Messages ga saqlang",
            "1-марталик медиа хабарга «??» деб reply қилинг ёки менюдан Saved Messages га сақланг",
            AeroConfig.saveOnceMedia, AeroRowCell.BADGE_NEW);
        addSwitchIfMatches(items, ID_SCREENSHOT_PROTECT, 0xFFDC2626, R.drawable.passcode_check,
            "Skreenshot va ekran yozishdan himoya", "Скриншот ва экран ёзишдан ҳимоя",
            "Ekran tasvirga olishni bloklaydi", "Экран тасвирга олишни блоклайди",
            AeroConfig.screenshotProtection, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_BLOCK_FOREIGN, 0xFFB91C1C, R.drawable.msg_block,
            "Chet el raqamlarini bloklash", "Чет эл рақамларини блоклаш",
            "+998 bo‘lmagan notanishlardan spam kelishini to‘sadi (" + AeroConfig.statBlockedForeignCount + ")",
            "+998 бўлмаган нотанишлардан спам келишини тўсади (" + AeroConfig.statBlockedForeignCount + ")",
            AeroConfig.blockForeignNumbers, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_CHAT_LOCK_ENABLE, 0xFF0F766E, R.drawable.msg_pin_code,
            "Chatni PIN kod bilan qulflash", "Чатни ПИН код билан қулфлаш",
            "Tanlangan chatlarni ochishda AeroGram PIN so‘raydi", "Танланган чатларни очишда AeroGram ПИН сўрайди",
            AeroConfig.chatLockEnabled, AeroRowCell.BADGE_AERO);
        addNavIfMatches(items, ID_SET_PIN, 0xFF0D9488, R.drawable.msg_settings,
            "AeroGram PIN kodini sozlash", "AeroGram ПИН кодини созлаш",
            TextUtils.isEmpty(AeroConfig.chatLockPin) ? AeroConfig.tr("Hali o‘rnatilmagan", "Ҳали ўрнатилмаган") : AeroConfig.tr("PIN kod faol • O‘zgartirish", "ПИН код фаол • Ўзгартириш"),
            TextUtils.isEmpty(AeroConfig.chatLockPin) ? "Ҳали ўрнатилмаган" : "ПИН код фаол • Ўзгартириш",
            TextUtils.isEmpty(AeroConfig.chatLockPin) ? "Yo‘q" : "****", AeroRowCell.BADGE_NONE);
        addNavIfMatches(items, ID_CHAT_LOCK_CHATS, 0xFF14B8A6, R.drawable.msg_viewchats,
            "Qulflangan chatlarni tanlash", "Қулфланган чатларни танлаш",
            AeroConfig.lockedDialogIds.size() + AeroConfig.tr(" ta chat PIN bilan yopilgan", " та чат ПИН билан ёпилган"),
            AeroConfig.lockedDialogIds.size() + " та чат ПИН билан ёпилган",
            String.valueOf(AeroConfig.lockedDialogIds.size()), AeroRowCell.BADGE_NONE);
        addNavIfMatches(items, ID_HIDDEN_CHATS, 0xFF059669, R.drawable.msg_secret,
            "Yashirin chatlar", "Яширин чатлар",
            AeroConfig.hiddenDialogIds.size() + AeroConfig.tr(" ta chat ro‘yxatdan yashirilgan", " та чат рўйхатдан яширилган"),
            AeroConfig.hiddenDialogIds.size() + " та чат рўйхатдан яширилган",
            AeroConfig.hiddenChatsUnlocked ? AeroConfig.tr("Ochiq", "Очиқ") : String.valueOf(AeroConfig.hiddenDialogIds.size()), AeroRowCell.BADGE_NONE);

        items.add(UItem.asShadow(null));

        // 3. Xabarlar va Tarjima (with NovaGram > sub-pages)
        items.add(UItem.asHeader(AeroConfig.tr("Xabarlar va Tarjima", "Хабарлар ва Таржима")));
        addNavIfMatches(items, ID_AUTO_TRANSLATE_PAGE, 0xFF0284C7, R.drawable.msg_translate,
            "Avto-tarjima", "Авто-таржима",
            "Kiruvchi, chiquvchi tarjima va maqsadli tilni sozlash", "Кирувчи, чиқувчи таржима ва мақсадли тилни созлаш",
            (AeroConfig.autoTranslate || AeroConfig.autoTranslateOutgoing ? AeroConfig.tr("Yoqilgan", "Ёқилган") : AeroConfig.tr("O‘chiq", "Ўчиқ"))
                + " • " + AeroConfig.translateTargetLang.toUpperCase(Locale.US),
            AeroRowCell.BADGE_NEW);
        addNavIfMatches(items, ID_NEW_ALPHABET_PAGE, 0xFF7C3AED, R.drawable.msg_text_check,
            "Yangi o‘zbek alifbosi (ş ç ō ḡ)", "Янги ўзбек алифбоси (ş ç ō ḡ)",
            "Yuborish, chatlarda ko‘rsatish va Lotin ↔ Kirill konverteri", "Юбориш, чатларда кўрсатиш ва Лотин ↔ Кирилл конвертери",
            (AeroConfig.newUzbekAlphabet || AeroConfig.newUzbekAlphabetInChats) ? AeroConfig.tr("Yoqilgan", "Ёқилган") : AeroConfig.tr("O‘chiq", "Ўчиқ"),
            AeroRowCell.BADGE_AERO);
        addNavIfMatches(items, ID_POSTFIX_PAGE, 0xFFA855F7, R.drawable.msg_recent,
            "Avto-postfiks", "Авто-постфикс",
            TextUtils.isEmpty(AeroConfig.autoPostfixText)
                ? AeroConfig.tr("Har bir xabar oxiriga avtomatik imzo qo‘shadi", "Ҳар бир хабар охирига автоматик имзо қўшади")
                : AeroConfig.autoPostfixText,
            TextUtils.isEmpty(AeroConfig.autoPostfixText)
                ? "Ҳар бир хабар охирига автоматик имзо қўшади"
                : AeroConfig.autoPostfixText,
            AeroConfig.autoPostfixEnabled ? AeroConfig.tr("Yoqilgan", "Ёқилган") : AeroConfig.tr("O‘chiq", "Ўчиқ"),
            AeroRowCell.BADGE_NONE);
        addNavIfMatches(items, ID_TEXT_STYLE, 0xFF8B5CF6, R.drawable.msg_text_outlined,
            "Matn uslubi", "Матн услуби",
            "Yuboriladigan xabarlarga avtomatik shrift qo‘llash", "Юбориладиган хабарларга автоматик шрифт қўллаш",
            styleLabel(AeroConfig.textStyleMode), AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_HEART_EFFECT, 0xFFEC4899, R.drawable.msg_stories_saved,
            "Yurakcha ❤️ effekti", "Юракча ❤️ эффекти",
            "Yuboriladigan xabar oxiriga yurakcha qo‘shadi", "Юбориладиган хабар охирига юракча қўшади",
            AeroConfig.heartEffect, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_AUTO_STICKER, 0xFFD946EF, R.drawable.msg_sticker,
            "Avto-stiker", "Авто-стикер",
            "Matnga mos kulgich/stiker yuborishni faollashtiradi", "Матнга мос кулгич/стикер юборишни фаоллаштиради",
            AeroConfig.autoSticker, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_FWD_NO_AUTHOR, 0xFF6366F1, R.drawable.msg_forward,
            "Muallifsiz yo‘naltirish", "Муаллифсиз йўналтириш",
            "Forward qilganda asl muallif nomini yashiradi", "Forward қилганда асл муаллиф номини яширади",
            AeroConfig.forwardWithoutAuthor, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_FWD_NO_CAPTION, 0xFF4F46E5, R.drawable.msg_forward_replace,
            "Izohsiz yo‘naltirish", "Изоҳсиз йўналтириш",
            "Forward qilganda media matnini avtomatik olib tashlaydi", "Forward қилганда медиа матнини автоматик олиб ташлайди",
            AeroConfig.forwardWithoutCaption, AeroRowCell.BADGE_NONE);
        addNavIfMatches(items, ID_DOUBLE_TAP, 0xFF3B82F6, R.drawable.menu_reply,
            "Ikki marta bosganda amal", "Икки марта босганда амал",
            "Xabarni ikki marta bosganda nima qilish", "Хабарни икки марта босганда нима қилиш",
            doubleTapLabel(AeroConfig.doubleTapAction), AeroRowCell.BADGE_NEW);

        items.add(UItem.asShadow(null));

        // 4. Ovoz → Matn (STT)
        items.add(UItem.asHeader(AeroConfig.tr("Ovoz → Matn (STT)", "Овоз → Матн (STT)")));
        addSwitchIfMatches(items, ID_VOICE_TO_TEXT, 0xFF2563EB, R.drawable.msg_voicechat,
            "Ovozli xabarlarni matnga aylantirish (STT)", "Овозли хабарларни матнга айлантириш (STT)",
            "Ovozli xabarlar ostida matn (transkripsiya) tugmasini yoqadi", "Овозли хабарлар остида матн (транскрипция) тугмасини ёқади",
            AeroConfig.voiceToTextEnabled, AeroRowCell.BADGE_NEW);
        addNavIfMatches(items, ID_VOICE_LANG, 0xFF1D4ED8, R.drawable.msg_voicechat2,
            "Ovoz → Matn (STT) tili", "Овоз → Матн (STT) тили",
            "Transkripsiya uchun afzal til", "Транскрипция учун афзал тил",
            AeroConfig.voiceToTextLang.toUpperCase(Locale.US), AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_AUTO_TRANSLATE_VOICE, 0xFF3B82F6, R.drawable.msg_translate,
            "Ovoz matnini avto-tarjima qilish", "Овоз матнини авто-таржима қилиш",
            "Matnga o‘girilgan ovozli xabarni tanlangan tilga tarjima qiladi", "Матнга ўгирилган овозли хабарни танланган тилга таржима қилади",
            AeroConfig.autoTranslateVoice, AeroRowCell.BADGE_NONE);

        items.add(UItem.asShadow(null));

        // 5. Chat va Qo‘ng‘iroqlar
        items.add(UItem.asHeader(AeroConfig.tr("Chat va Qo‘ng‘iroqlar", "Чат ва Қўнғироқлар")));
        addSwitchIfMatches(items, ID_CONFIRM_CALL, 0xFF22C55E, R.drawable.msg_calls,
            "Qo‘ng‘iroqdan oldin tasdiqlash", "Қўнғироқдан олдин тасдиқлаш",
            "Tasodifiy qo‘ng‘iroq qilishni oldini oladi", "Тасодифий қўнғироқ қилишни олдини олади",
            AeroConfig.confirmBeforeCall, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_RECORD_CALLS, 0xFF10B981, R.drawable.msg_calls,
            "Qo‘ng‘iroqlarni yozib olish funksiyasi", "Қўнғироқларни ёзиб олиш функцияси",
            "Ovozli qo‘ng‘iroqlar uchun yozuv ruxsati", "Овозли қўнғироқлар учун ёзув рухсати",
            AeroConfig.recordCallsEnabled, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_CONFIRM_SEND_MEDIA, 0xFF10B981, R.drawable.msg_sticker,
            "Stiker va GIF yuborishni tasdiqlash", "Стикер ва GIF юборишни тасдиқлаш",
            "Stiker yoki GIF bosilganda so‘rov chiqaradi", "Стикер ёки GIF босилганда сўров чиқаради",
            AeroConfig.confirmSendMedia, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_SHOW_ID_DC, 0xFF0EA5E9, R.drawable.msg_info,
            "Xabar ID va profil DC ko‘rsatish", "Хабар ID ва профил DC кўрсатиш",
            "Xabar vaqti yonida #ID va foydalanuvchi ID/DC ko‘rinadi", "Хабар вақти ёнида #ID ва фойдаланувчи ID/DC кўринади",
            AeroConfig.showIdAndDc, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_SHOW_SECONDS, 0xFF0284C7, R.drawable.msg_recent,
            "Vaqtni soniyalar bilan ko‘rsatish", "Вақтни сониялар билан кўрсатиш",
            "Masalan: 14:25:09 ko‘rinishida", "Масалан: 14:25:09 кўринишида",
            AeroConfig.showSecondsInTime, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_JUMP_FIRST, 0xFF0369A1, R.drawable.chats_pin,
            "Birinchi xabarga o‘tish tugmasi", "Биринчи хабарга ўтиш тугмаси",
            "Chat menyusiga «Birinchi xabarga o‘tish» bandini qo‘shadi", "Чат менюсига «Биринчи хабарга ўтиш» бандини қўшади",
            AeroConfig.jumpToFirstMessageButton, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_CHANNEL_HISTORY, 0xFF1E40AF, R.drawable.msg_viewreplies,
            "Kanal so‘nggi harakatlari tugmasi", "Канал сўнгги ҳаракатлари тугмаси",
            "Adminlar uchun Recent Actions menyusi", "Админлар учун Recent Actions менюси",
            AeroConfig.channelHistoryButton, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_SHOW_GHOST_BTN, 0xFF6366F1, R.drawable.msg_stealth_locked,
            "Chatlarda tezkor Ghost tugmasi", "Чатларда тезкор Ghost тугмаси",
            "Yuqori panelda Ghost rejimini bir bosishda almashtiradi", "Юқори панелда Ghost режимини бир босишда алмаштиради",
            AeroConfig.showGhostButtonInChats, AeroRowCell.BADGE_NONE);

        items.add(UItem.asShadow(null));

        // 6. Tarmoq, Papkalar va Eslatmalar
        items.add(UItem.asHeader(AeroConfig.tr("Tarmoq, Papkalar va Eslatmalar", "Тармоқ, Папкалар ва Эслатмалар")));
        addNavIfMatches(items, ID_AERO_PROXY, 0xFF0EA5E9, R.drawable.proxy_check,
            "AeroGramProxy", "AeroGramProxy",
            "Tezkor proksi serverlarni boshqarish va yoqish", "Тезкор прокси серверларни бошқариш ва ёқиш",
            SharedConfig.isProxyEnabled() ? AeroConfig.tr("Yoqilgan", "Ёқилган") : AeroConfig.tr("Sozlash", "Созлаш"), AeroRowCell.BADGE_AERO);
        addSwitchIfMatches(items, ID_REMINDER_ENABLE, 0xFFF59E0B, R.drawable.msg_notifications,
            "O‘qilmagan xabar eslatmasi", "Ўқилмаган хабар эслатмаси",
            "Javob berilmagan xabarlarni eslatib turadi", "Жавоб берилмаган хабарларни эслатиб туради",
            AeroConfig.unreadReminderEnabled, AeroRowCell.BADGE_NONE);
        addNavIfMatches(items, ID_REMINDER_DELAY, 0xFFD97706, R.drawable.msg_recent,
            "Eslatma vaqti", "Эслатма вақти",
            "Eslatma chiqarish oralig‘i", "Эслатма чиқариш оралиғи",
            AeroConfig.unreadReminderMinutes + " min", AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_REMINDER_SOUND, 0xFFB45309, R.drawable.msg_notifications,
            "Eslatma ovozi", "Эслатма овози",
            "Eslatmada ovozli signal chalish", "Эслатмада овозли сигнал чалиш",
            AeroConfig.unreadReminderSound, AeroRowCell.BADGE_NONE);
        addNavIfMatches(items, ID_FOLDER_STYLE, 0xFF3B82F6, R.drawable.msg_folders,
            "Papkalar uslubi", "Папкалар услуби",
            "Chat papkalari ko‘rinishini tanlang", "Чат папкалари кўринишини танланг",
            folderStyleLabel(AeroConfig.folderStyle), AeroRowCell.BADGE_NEW);
        addNavIfMatches(items, ID_ADMIN_FOLDERS, 0xFF2563EB, R.drawable.msg_folders_groups,
            "Papkalar va Admin boshqaruvi", "Папкалар ва Админ бошқаруви",
            "Chat papkalari va boshqaruv bo‘limini ochish", "Чат папкалари ва бошқарув бўлимини очиш",
            AeroConfig.tr("Ochish", "Очиш"), AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_RECOMMENDED_FOLDERS, 0xFF1D4ED8, R.drawable.msg_folders_channels,
            "Tavsiya etilgan papkalar", "Тавсия этилган папкалар",
            "Papkalar bo‘limida Shaxsiy, Guruhlar, Kanallar tavsiyasini ko‘rsatadi", "Папкалар бўлимида Шахсий, Гуруҳлар, Каналлар тавсиясини кўрсатади",
            AeroConfig.recommendedFoldersEnabled, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_HIDE_FOLDERS, 0xFF4338CA, R.drawable.msg_folders,
            "Chatlar ro‘yxatida papkalarni yashirish", "Чатлар рўйхатида папкаларни яшириш",
            "Yuqori papka qatorini ko‘rinmas qiladi", "Юқори папка қаторини кўринмас қилади",
            AeroConfig.hideFolders, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_AUTO_ACCEPT_REQUESTS, 0xFF16A34A, R.drawable.msg_groups_create,
            "Qo‘shilish so‘rovlarini avto-qabul qilish", "Қўшилиш сўровларини авто-қабул қилиш",
            "Guruh/kanal so‘rovlar sahifasi ochilganda barchasini tasdiqlaydi", "Гуруҳ/канал сўровлар саҳифаси очилганда барчасини тасдиқлайди",
            AeroConfig.autoAcceptJoinRequests, AeroRowCell.BADGE_NEW);

        items.add(UItem.asShadow(null));

        // 7. Interfeys, Hikoyalar va Ikonka
        items.add(UItem.asHeader(AeroConfig.tr("Interfeys, Hikoyalar va Ikonka", "Интерфейс, Ҳикоялар ва Иконка")));
        addNavIfMatches(items, ID_APP_ICON, 0xFF0284C7, R.drawable.ic_aerogram_monochrome,
            "Ilova ikonkasi", "Илова иконкаси",
            "6 ta maxsus AeroGram ikonkasidan tanlang", "6 та махсус AeroGram иконкасидан танланг",
            getCurrentIconTitle(), AeroRowCell.BADGE_AERO);
        addSwitchIfMatches(items, ID_SHOW_STORIES, 0xFFEC4899, R.drawable.msg_stories_add,
            "Stories panelini ko‘rsatish", "Stories панелини кўрсатиш",
            "Bosh ekranda hikoyalar qatorini ko‘rsatadi", "Бош экранда ҳикоялар қаторини кўрсатади",
            AeroConfig.showStoriesBar, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_SAVE_STORIES_HD, 0xFFDB2777, R.drawable.msg_stories_save,
            "Hikoyalarni HD sifatda saqlash", "Ҳикояларни HD сифатда сақлаш",
            "Story larni maksimal sifatda yuklaydi va galereyaga saqlaydi", "Story ларни максимал сифатда юклайди ва галереяга сақлайди",
            AeroConfig.saveStoriesHd, AeroRowCell.BADGE_NEW);
        addSwitchIfMatches(items, ID_LIGHT_BRAND_ACCENT, 0xFF0EA5E9, R.drawable.msg_colors,
            "Yorug‘ mavzuda AeroGram aksenti", "Ёруғ мавзуда AeroGram акценти",
            "Yorug‘ rejimda AeroGram osmon rangini faol saqlaydi", "Ёруғ режимда AeroGram осмон рангини фаол сақлайди",
            AeroConfig.lightThemeBrandAccent, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_DISABLE_ROUNDING, 0xFF3B82F6, R.drawable.msg_stats,
            "Raqamlarni yaxlitlamaslik", "Рақамларни яхлитламаслик",
            "1.2K o‘rniga aniq 1245 ko‘rsatiladi", "1.2K ўрнига аниқ 1245 кўрсатилади",
            AeroConfig.disableNumberRounding, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_UNLIMITED_PINS, 0xFF6366F1, R.drawable.msg_pin,
            "Cheksiz chat qadash (Pin)", "Чексиз чат қадаш (Pin)",
            "5 ta chat cheklovisiz xohlagancha chatni qadang", "5 та чат чекловисиз хоҳлаганча чатни қаданг",
            AeroConfig.unlimitedPins, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_SHOW_MUTUAL, 0xFF14B8A6, R.drawable.msg_groups,
            "O‘zaro kontakt belgisi", "Ўзаро контакт белгиси",
            "Sizni ham kontaktga qo‘shgan foydalanuvchilarni ajratib ko‘rsatadi", "Сизни ҳам контактга қўшган фойдаланувчиларни ажратиб кўрсатади",
            AeroConfig.showMutualBadge, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_GALLERY_ROUND, 0xFF06B6D4, R.drawable.msg_camera,
            "Galereyadan dumaloq video", "Галереядан думалоқ видео",
            "Oddiy videoni yumaloq video-xabar sifatida yuborish", "Оддий видеони юмалоқ видео-хабар сифатида юбориш",
            AeroConfig.galleryRoundVideo, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_ASK_CAMERA, 0xFF0EA5E9, R.drawable.msg_camera,
            "Kamera tanlash oynasi", "Камера танлаш ойнаси",
            "Video-xabardan oldin kamera turini so‘raydi", "Видео-хабардан олдин камера турини сўрайди",
            AeroConfig.askCameraType, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_CAMERA_HOLD, 0xFF0284C7, R.drawable.msg_camera,
            "Ushlab turganda kamera almashtirish", "Ушлаб турганда камера алмаштириш",
            "Kamera tugmasini bosib turib oldi/orqaga o‘tish", "Камера тугмасини босиб туриб олди/орқага ўтиш",
            AeroConfig.cameraSwitchOnHold, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_PREFER_BACK_CAM, 0xFF1E40AF, R.drawable.msg_camera,
            "Orqa kameradan boshlash", "Орқа камерадан бошлаш",
            "Dumaloq video doim orqa kamera bilan ochiladi", "Думалоқ видео доим орқа камера билан очилади",
            AeroConfig.preferBackCamera, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_DISABLE_AUTODOWNLOAD, 0xFFEF4444, R.drawable.msg_download,
            "Avto-yuklab olishni o‘chirish", "Авто-юклаб олишни ўчириш",
            "Trafikni tejash uchun media avtomatik yuklanmaydi", "Трафикни тежаш учун медиа автоматик юкланмайди",
            AeroConfig.disableAutoDownload, AeroRowCell.BADGE_NONE);
        addSwitchIfMatches(items, ID_SCRIPT_CYRILLIC, 0xFF8B5CF6, R.drawable.msg_translate,
            "Kirill alifbosi interfeysi (Lotin ↔ Кирилл)", "Кирилл алифбоси интерфейси (Лотин ↔ Кирилл)",
            "AeroGram bo‘limlarini kirill yozuviga o‘giradi", "AeroGram бўлимларини кирилл ёзувига ўгиради",
            AeroConfig.uiCyrillic, AeroRowCell.BADGE_NONE);

        items.add(UItem.asShadow(null));

        // 8. Ma'lumotlar va Tiklash
        items.add(UItem.asHeader(AeroConfig.tr("Ma’lumotlar, Havola va Tiklash", "Маълумотлар, Ҳавола ва Тиклаш")));
        int totalSaved = AeroDeletedStore.getTotalSavedCount() + AeroEditHistoryStore.getTotalCount();
        addNavIfMatches(items, ID_CLEAR_HISTORY_STORE, 0xFFEF4444, R.drawable.msg_delete,
            "Saqlangan o‘chirilgan va tahrirlarni tozalash", "Сақланган ўчирилган ва таҳрирларни тозалаш",
            AeroDeletedStore.getTotalSavedCount() + AeroConfig.tr(" o‘chirilgan • ", " ўчирилган • ") + AeroEditHistoryStore.getTotalCount() + AeroConfig.tr(" tahrir", " таҳрир"),
            AeroDeletedStore.getTotalSavedCount() + " ўчирилган • " + AeroEditHistoryStore.getTotalCount() + " таҳрир",
            totalSaved + " ta", AeroRowCell.BADGE_NONE);
        addNavIfMatches(items, ID_SHARE_SETTINGS_LINK, 0xFF0EA5E9, R.drawable.msg_share,
            "AeroGram sozlamalarini ulashish", "AeroGram созламаларини улашиш",
            "AeroGram ilovasi va sozlamalar havolasini yuborish", "AeroGram иловаси ва созламалар ҳаволасини юбориш",
            "", AeroRowCell.BADGE_NONE);
        addNavIfMatches(items, ID_RESET_ALL, 0xFF64748B, R.drawable.msg_reset,
            "Sozlamalarni standart holatga qaytarish", "Созламаларни стандарт ҳолатга қайтариш",
            "Barcha AeroGram parametrlarini tiklaydi", "Барча AeroGram параметрларини тиклайди",
            "", AeroRowCell.BADGE_NONE);

        items.add(UItem.asShadow(AeroConfig.tr(
            "AeroGram v1.4.0 • Istalgan sozlamani bosib turib uning havolasini nusxalashingiz mumkin.",
            "AeroGram v1.4.0 • Исталган созламани босиб туриб унинг ҳаволасини нусхалашингиз мумкин."
        )));
    }

    private String getCurrentIconTitle() {
        for (LauncherIconController.LauncherIcon ic : LauncherIconController.LauncherIcon.values()) {
            if (LauncherIconController.isEnabled(ic)) {
                return LocaleController.getString(ic.title);
            }
        }
        return "Aero Sky";
    }

    private String styleLabel(int mode) {
        switch (mode) {
            case AeroConfig.STYLE_BOLD: return AeroConfig.tr("Qalin", "Қалин");
            case AeroConfig.STYLE_ITALIC: return AeroConfig.tr("Kursiv", "Курсив");
            case AeroConfig.STYLE_MONO: return "Monoshift";
            case AeroConfig.STYLE_STRIKE: return AeroConfig.tr("Chizilgan", "Чизилган");
            case AeroConfig.STYLE_UNDERLINE: return AeroConfig.tr("Tagchizilgan", "Тагчизилган");
            case AeroConfig.STYLE_SPOILER: return "Spoiler";
            default: return AeroConfig.tr("Oddiy", "Оддий");
        }
    }

    private String doubleTapLabel(int mode) {
        switch (mode) {
            case AeroConfig.DOUBLE_TAP_REPLY: return AeroConfig.tr("Javob berish", "Жавоб бериш");
            case AeroConfig.DOUBLE_TAP_COPY: return AeroConfig.tr("Nusxalash", "Нусхалаш");
            case AeroConfig.DOUBLE_TAP_TRANSLATE: return AeroConfig.tr("Tarjima", "Таржима");
            case AeroConfig.DOUBLE_TAP_NONE: return AeroConfig.tr("O‘chirilgan", "Ўчирилган");
            default: return AeroConfig.tr("Reaksiya", "Реакция");
        }
    }

    private String folderStyleLabel(int mode) {
        if (mode == AeroConfig.FOLDER_STYLE_ICON_TEXT) return AeroConfig.tr("Ikonka + Matn", "Иконка + Матн");
        if (mode == AeroConfig.FOLDER_STYLE_ICON) return AeroConfig.tr("Faqat ikonka", "Фақат иконка");
        return AeroConfig.tr("Matn", "Матн");
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_ABOUT:
                presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_ABOUT));
                break;
            case ID_GUIDE:
                presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_GUIDE));
                break;
            case ID_ACCOUNTS:
                presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_ACCOUNTS));
                break;

            case ID_GHOST_MASTER:
                AeroConfig.toggleMasterGhost(!AeroConfig.isAnyGhostActive());
                MessagesController.forceAeroOfflineStatusAll(true);
                reloadList();
                break;
            case ID_GHOST_DONT_READ:
                AeroConfig.ghostDontReadMessages = !AeroConfig.ghostDontReadMessages;
                AeroConfig.saveBool("ghostDontReadMessages", AeroConfig.ghostDontReadMessages);
                reloadList();
                break;
            case ID_GHOST_READ_ON_INTERACT:
                AeroConfig.ghostReadOnInteract = !AeroConfig.ghostReadOnInteract;
                AeroConfig.saveBool("ghostReadOnInteract", AeroConfig.ghostReadOnInteract);
                reloadList();
                break;
            case ID_GHOST_OFFLINE:
                AeroConfig.ghostStayOffline = !AeroConfig.ghostStayOffline;
                AeroConfig.saveBool("ghostStayOffline", AeroConfig.ghostStayOffline);
                MessagesController.forceAeroOfflineStatusAll(true);
                reloadList();
                break;
            case ID_GHOST_TYPING:
                AeroConfig.ghostDontSendTyping = !AeroConfig.ghostDontSendTyping;
                AeroConfig.saveBool("ghostDontSendTyping", AeroConfig.ghostDontSendTyping);
                reloadList();
                break;
            case ID_GHOST_STORY:
                AeroConfig.ghostHideStoryViews = !AeroConfig.ghostHideStoryViews;
                AeroConfig.saveBool("ghostHideStoryViews", AeroConfig.ghostHideStoryViews);
                reloadList();
                break;
            case ID_SAVE_DELETED:
                AeroConfig.saveDeletedMessages = !AeroConfig.saveDeletedMessages;
                AeroConfig.saveBool("saveDeletedMessages", AeroConfig.saveDeletedMessages);
                reloadList();
                break;
            case ID_EDIT_HISTORY:
                AeroConfig.saveEditHistory = !AeroConfig.saveEditHistory;
                AeroConfig.saveBool("saveEditHistory", AeroConfig.saveEditHistory);
                reloadList();
                break;
            case ID_SAVE_ONCE_MEDIA:
                AeroConfig.saveOnceMedia = !AeroConfig.saveOnceMedia;
                AeroConfig.saveBool("saveOnceMedia", AeroConfig.saveOnceMedia);
                reloadList();
                break;
            case ID_SCREENSHOT_PROTECT:
                AeroConfig.screenshotProtection = !AeroConfig.screenshotProtection;
                AeroConfig.saveBool("screenshotProtection", AeroConfig.screenshotProtection);
                LaunchActivity.invalidateAeroSecurity();
                reloadList();
                break;
            case ID_BLOCK_FOREIGN:
                AeroConfig.blockForeignNumbers = !AeroConfig.blockForeignNumbers;
                AeroConfig.saveBool("blockForeignNumbers", AeroConfig.blockForeignNumbers);
                reloadList();
                break;
            case ID_CHAT_LOCK_ENABLE:
                if (!AeroConfig.chatLockEnabled && TextUtils.isEmpty(AeroConfig.chatLockPin) && getParentActivity() != null) {
                    AeroUiHelper.showSetPinDialog(getParentActivity(), this::reloadList);
                } else {
                    AeroConfig.chatLockEnabled = !AeroConfig.chatLockEnabled;
                    AeroConfig.saveBool("chatLockEnabled", AeroConfig.chatLockEnabled);
                    reloadList();
                }
                break;
            case ID_SET_PIN:
                if (getParentActivity() != null) {
                    AeroUiHelper.showSetPinDialog(getParentActivity(), this::reloadList);
                }
                break;
            case ID_CHAT_LOCK_CHATS:
                if (TextUtils.isEmpty(AeroConfig.chatLockPin) && getParentActivity() != null) {
                    AeroUiHelper.showSetPinDialog(getParentActivity(), () -> presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_LOCKED_CHATS)));
                } else {
                    presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_LOCKED_CHATS));
                }
                break;
            case ID_HIDDEN_CHATS:
                if (AeroConfig.hiddenDialogIds.isEmpty()) {
                    presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_HIDDEN_CHATS));
                } else if (getParentActivity() != null) {
                    AeroUiHelper.promptPinDialog(getParentActivity(), AeroConfig.tr("Yashirin chatlarni boshqarish", "Яширин чатларни бошқариш"), () -> {
                        AeroConfig.hiddenChatsUnlocked = true;
                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
                        presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_HIDDEN_CHATS));
                    });
                }
                break;

            case ID_AUTO_TRANSLATE_PAGE:
                presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_AUTO_TRANSLATE));
                break;
            case ID_TRANSLATE_LANG:
                presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_TRANSLATE_LANG));
                break;
            case ID_VOICE_TO_TEXT:
                AeroConfig.voiceToTextEnabled = !AeroConfig.voiceToTextEnabled;
                AeroConfig.saveBool("voiceToTextEnabled", AeroConfig.voiceToTextEnabled);
                reloadList();
                break;
            case ID_VOICE_LANG:
                presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_STT_LANG));
                break;
            case ID_AUTO_TRANSLATE_VOICE:
                AeroConfig.autoTranslateVoice = !AeroConfig.autoTranslateVoice;
                AeroConfig.saveBool("autoTranslateVoice", AeroConfig.autoTranslateVoice);
                reloadList();
                break;

            case ID_TEXT_STYLE:
                presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_TEXT_STYLE));
                break;
            case ID_NEW_ALPHABET_PAGE:
                presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_NEW_ALPHABET));
                break;
            case ID_POSTFIX_PAGE:
                presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_POSTFIX));
                break;
            case ID_HEART_EFFECT:
                AeroConfig.heartEffect = !AeroConfig.heartEffect;
                AeroConfig.saveBool("heartEffect", AeroConfig.heartEffect);
                reloadList();
                break;
            case ID_AUTO_STICKER:
                AeroConfig.autoSticker = !AeroConfig.autoSticker;
                AeroConfig.saveBool("autoSticker", AeroConfig.autoSticker);
                reloadList();
                break;
            case ID_FWD_NO_AUTHOR:
                AeroConfig.forwardWithoutAuthor = !AeroConfig.forwardWithoutAuthor;
                AeroConfig.saveBool("forwardWithoutAuthor", AeroConfig.forwardWithoutAuthor);
                reloadList();
                break;
            case ID_FWD_NO_CAPTION:
                AeroConfig.forwardWithoutCaption = !AeroConfig.forwardWithoutCaption;
                AeroConfig.saveBool("forwardWithoutCaption", AeroConfig.forwardWithoutCaption);
                reloadList();
                break;
            case ID_DOUBLE_TAP:
                presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_DOUBLE_TAP));
                break;
            case ID_CONFIRM_CALL:
                AeroConfig.confirmBeforeCall = !AeroConfig.confirmBeforeCall;
                AeroConfig.saveBool("confirmBeforeCall", AeroConfig.confirmBeforeCall);
                reloadList();
                break;
            case ID_CONFIRM_SEND_MEDIA:
                AeroConfig.confirmSendMedia = !AeroConfig.confirmSendMedia;
                AeroConfig.saveBool("confirmSendMedia", AeroConfig.confirmSendMedia);
                reloadList();
                break;
            case ID_SHOW_ID_DC:
                AeroConfig.showIdAndDc = !AeroConfig.showIdAndDc;
                AeroConfig.saveBool("showIdAndDc", AeroConfig.showIdAndDc);
                reloadList();
                break;
            case ID_SHOW_SECONDS:
                AeroConfig.showSecondsInTime = !AeroConfig.showSecondsInTime;
                AeroConfig.saveBool("showSecondsInTime", AeroConfig.showSecondsInTime);
                reloadList();
                break;
            case ID_JUMP_FIRST:
                AeroConfig.jumpToFirstMessageButton = !AeroConfig.jumpToFirstMessageButton;
                AeroConfig.saveBool("jumpToFirstMessageButton", AeroConfig.jumpToFirstMessageButton);
                reloadList();
                break;
            case ID_CHANNEL_HISTORY:
                AeroConfig.channelHistoryButton = !AeroConfig.channelHistoryButton;
                AeroConfig.saveBool("channelHistoryButton", AeroConfig.channelHistoryButton);
                reloadList();
                break;

            case ID_AERO_PROXY:
                AeroConfig.aeroProxyEnabled = true;
                AeroConfig.saveBool("aeroProxyEnabled", true);
                presentFragment(new ProxyListActivity());
                break;
            case ID_REMINDER_ENABLE:
                AeroConfig.unreadReminderEnabled = !AeroConfig.unreadReminderEnabled;
                AeroConfig.saveBool("unreadReminderEnabled", AeroConfig.unreadReminderEnabled);
                reloadList();
                break;
            case ID_REMINDER_DELAY:
                presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_REMINDER_DELAY));
                break;
            case ID_REMINDER_SOUND:
                AeroConfig.unreadReminderSound = !AeroConfig.unreadReminderSound;
                AeroConfig.saveBool("unreadReminderSound", AeroConfig.unreadReminderSound);
                reloadList();
                break;
            case ID_FOLDER_STYLE:
                presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_FOLDER_STYLE));
                break;
            case ID_ADMIN_FOLDERS:
                AeroConfig.adminFoldersEnabled = true;
                AeroConfig.saveBool("adminFoldersEnabled", true);
                presentFragment(new FiltersSetupActivity());
                break;
            case ID_RECOMMENDED_FOLDERS:
                AeroConfig.recommendedFoldersEnabled = !AeroConfig.recommendedFoldersEnabled;
                AeroConfig.saveBool("recommendedFoldersEnabled", AeroConfig.recommendedFoldersEnabled);
                reloadList();
                break;
            case ID_HIDE_FOLDERS:
                AeroConfig.hideFolders = !AeroConfig.hideFolders;
                AeroConfig.saveBool("hideFolders", AeroConfig.hideFolders);
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogFiltersUpdated);
                reloadList();
                break;
            case ID_AUTO_ACCEPT_REQUESTS:
                AeroConfig.autoAcceptJoinRequests = !AeroConfig.autoAcceptJoinRequests;
                AeroConfig.saveBool("autoAcceptJoinRequests", AeroConfig.autoAcceptJoinRequests);
                reloadList();
                break;

            case ID_APP_ICON:
                showIconPickerDialog();
                break;
            case ID_SHOW_STORIES:
                AeroConfig.showStoriesBar = !AeroConfig.showStoriesBar;
                AeroConfig.saveBool("showStoriesBar", AeroConfig.showStoriesBar);
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.storiesUpdated);
                reloadList();
                break;
            case ID_SAVE_STORIES_HD:
                AeroConfig.saveStoriesHd = !AeroConfig.saveStoriesHd;
                AeroConfig.saveBool("saveStoriesHd", AeroConfig.saveStoriesHd);
                reloadList();
                break;
            case ID_SHOW_GHOST_BTN:
                AeroConfig.showGhostButtonInChats = !AeroConfig.showGhostButtonInChats;
                AeroConfig.saveBool("showGhostButtonInChats", AeroConfig.showGhostButtonInChats);
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
                reloadList();
                break;
            case ID_LIGHT_BRAND_ACCENT:
                AeroConfig.lightThemeBrandAccent = !AeroConfig.lightThemeBrandAccent;
                AeroConfig.saveBool("lightThemeBrandAccent", AeroConfig.lightThemeBrandAccent);
                AeroUiHelper.notifyLightBrandAccentChanged();
                reloadList();
                break;
            case ID_RECORD_CALLS:
                AeroConfig.recordCallsEnabled = !AeroConfig.recordCallsEnabled;
                AeroConfig.saveBool("recordCallsEnabled", AeroConfig.recordCallsEnabled);
                reloadList();
                break;
            case ID_DISABLE_ROUNDING:
                AeroConfig.disableNumberRounding = !AeroConfig.disableNumberRounding;
                AeroConfig.saveBool("disableNumberRounding", AeroConfig.disableNumberRounding);
                reloadList();
                break;
            case ID_UNLIMITED_PINS:
                AeroConfig.unlimitedPins = !AeroConfig.unlimitedPins;
                AeroConfig.saveBool("unlimitedPins", AeroConfig.unlimitedPins);
                reloadList();
                break;
            case ID_SHOW_MUTUAL:
                AeroConfig.showMutualBadge = !AeroConfig.showMutualBadge;
                AeroConfig.saveBool("showMutualBadge", AeroConfig.showMutualBadge);
                reloadList();
                break;
            case ID_GALLERY_ROUND:
                AeroConfig.galleryRoundVideo = !AeroConfig.galleryRoundVideo;
                AeroConfig.saveBool("galleryRoundVideo", AeroConfig.galleryRoundVideo);
                reloadList();
                break;
            case ID_ASK_CAMERA:
                AeroConfig.askCameraType = !AeroConfig.askCameraType;
                AeroConfig.saveBool("askCameraType", AeroConfig.askCameraType);
                reloadList();
                break;
            case ID_CAMERA_HOLD:
                AeroConfig.cameraSwitchOnHold = !AeroConfig.cameraSwitchOnHold;
                AeroConfig.saveBool("cameraSwitchOnHold", AeroConfig.cameraSwitchOnHold);
                reloadList();
                break;
            case ID_PREFER_BACK_CAM:
                AeroConfig.preferBackCamera = !AeroConfig.preferBackCamera;
                AeroConfig.saveBool("preferBackCamera", AeroConfig.preferBackCamera);
                reloadList();
                break;
            case ID_DISABLE_AUTODOWNLOAD:
                AeroConfig.disableAutoDownload = !AeroConfig.disableAutoDownload;
                AeroConfig.saveBool("disableAutoDownload", AeroConfig.disableAutoDownload);
                reloadList();
                break;
            case ID_SCRIPT_CYRILLIC:
                AeroConfig.uiCyrillic = !AeroConfig.uiCyrillic;
                AeroConfig.saveBool("uiCyrillic", AeroConfig.uiCyrillic);
                if (actionBar != null) {
                    actionBar.setTitle(getTitle());
                }
                reloadList();
                break;

            case ID_CLEAR_HISTORY_STORE:
                if (getParentActivity() != null) {
                    new AlertDialog.Builder(getParentActivity())
                        .setTitle(AeroConfig.tr("Saqlanganlarni tozalash", "Сақланганларни тозалаш"))
                        .setMessage(AeroConfig.tr("Barcha saqlangan o‘chirilgan xabar va tahrirlar tarixi o‘chirilsinmi?", "Барча сақланган ўчирилган хабар ва таҳрирлар тарихи ўчирилсинми?"))
                        .setPositiveButton(AeroConfig.tr("Tozalash", "Тозалаш"), (d, w) -> {
                            AeroDeletedStore.clearAll();
                            AeroEditHistoryStore.clearAll();
                            reloadList();
                            Toast.makeText(getParentActivity(), AeroConfig.tr("Tozalandi", "Тозаланди"), Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(AeroConfig.tr("Bekor qilish", "Бекор қилиш"), null)
                        .show();
                }
                break;
            case ID_SHARE_SETTINGS_LINK:
                shareSettingsLink();
                break;
            case ID_RESET_ALL:
                if (getParentActivity() != null) {
                    new AlertDialog.Builder(getParentActivity())
                        .setTitle(AeroConfig.tr("Sozlamalarni tiklash", "Созламаларни тиклаш"))
                        .setMessage(AeroConfig.tr("Barcha AeroGram sozlamalari standart holatga qaytarilsinmi?", "Барча AeroGram созламалари стандарт ҳолатга қайтарилсинми?"))
                        .setPositiveButton(AeroConfig.tr("Tiklash", "Тиклаш"), (d, w) -> {
                            AeroConfig.resetAll();
                            MessagesController.forceAeroOfflineStatusAll(true);
                            LaunchActivity.invalidateAeroSecurity();
                            reloadList();
                            Toast.makeText(getParentActivity(), AeroConfig.tr("Sozlamalar tiklandi", "Созламалар тикланди"), Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(AeroConfig.tr("Bekor qilish", "Бекор қилиш"), null)
                        .show();
                }
                break;
        }
    }

    private void showIconPickerDialog() {
        if (getParentActivity() == null) return;
        LauncherIconController.LauncherIcon[] vals = LauncherIconController.LauncherIcon.values();
        CharSequence[] names = new CharSequence[vals.length];
        for (int i = 0; i < vals.length; i++) {
            String suffix = LauncherIconController.isEnabled(vals[i]) ? " ✓" : "";
            names[i] = LocaleController.getString(vals[i].title) + suffix;
        }
        new AlertDialog.Builder(getParentActivity())
            .setTitle(AeroConfig.tr("Ilova ikonkasi", "Илова иконкаси"))
            .setItems(names, (d, which) -> {
                LauncherIconController.LauncherIcon chosen = vals[which];
                LauncherIconController.setIcon(chosen);
                reloadList();
                Toast.makeText(getParentActivity(),
                    LocaleController.formatString(R.string.AppIconChangedTo, LocaleController.getString(chosen.title)),
                    Toast.LENGTH_SHORT).show();
            })
            .setNeutralButton(AeroConfig.tr("Chat sozlamalarida ko‘rish", "Чат созламаларида кўриш"), (d, w) -> {
                presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC));
            })
            .setNegativeButton(AeroConfig.tr("Yopish", "Ёпиш"), null)
            .show();
    }

    private void shareSettingsLink() {
        String link = "https://aerogram.website/download.html";
        AndroidUtilities.addToClipboard(link);
        if (getParentActivity() != null) {
            try {
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, AeroConfig.tr(
                    "AeroGram — Ghost, Anti-Delete va Tezkor Telegram mijozini yuklab oling: " + link,
                    "AeroGram — Ghost, Anti-Delete ва Тезкор Telegram мижозини юклаб олинг: " + link
                ));
                getParentActivity().startActivity(Intent.createChooser(share, AeroConfig.tr("Ulashish", "Улашиш")));
            } catch (Exception e) {
                Toast.makeText(getParentActivity(), AeroConfig.tr("Havola nusxalandi", "Ҳавола нусхаланди"), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        if (item != null && item.id > 0) {
            String link = "aerogram://settings/" + item.id;
            AndroidUtilities.addToClipboard(link);
            if (getParentActivity() != null) {
                Toast.makeText(getParentActivity(),
                    AeroConfig.tr("Sozlama havolasi nusxalandi: ", "Созлама ҳаволаси нусхаланди: ") + link,
                    Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return false;
    }
}
