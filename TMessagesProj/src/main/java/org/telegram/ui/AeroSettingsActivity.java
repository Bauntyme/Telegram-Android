package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import org.telegram.messenger.AeroConfig;
import org.telegram.messenger.AeroDeletedStore;
import org.telegram.messenger.AeroEditHistoryStore;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.SimpleActionBarMenu;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;
import java.util.Locale;

/**
 * AeroGram Settings screen styled faithfully after NovaGram's clean iOS/Telegram grouped design,
 * with live search filtering, colored rounded-square badges, YANGI / AERO pills, and 2 custom AeroGram features.
 */
public class AeroSettingsActivity extends UniversalFragment {

    private static final int ID_ABOUT = 1;
    private static final int ID_ACCOUNTS = 2;

    // Chat section
    private static final int ID_SAVE_DELETED = 10;
    private static final int ID_EDIT_HISTORY = 11;
    private static final int ID_JUMP_FIRST = 12;
    private static final int ID_GHOST_BUTTON = 13;
    private static final int ID_GHOST_DETAILS = 14;
    private static final int ID_CAMERA_HOLD = 15;
    private static final int ID_GALLERY_ROUND_VIDEO = 16;
    private static final int ID_FWD_NO_AUTHOR = 17;
    private static final int ID_FWD_NO_CAPTION = 18;

    // Interface section
    private static final int ID_HIDE_FOLDERS = 20;
    private static final int ID_STORIES_PANEL = 21;
    private static final int ID_MUTUAL_BADGE = 22;
    private static final int ID_UNLIMITED_PINS = 23;

    // Messages section
    private static final int ID_TEXT_STYLE = 30;
    private static final int ID_AUTO_POSTFIX = 31;
    private static final int ID_TRANSLATE_BTN = 32;
    private static final int ID_TRANSLATE_LANG = 33;
    private static final int ID_NEW_ALPHABET = 34;
    private static final int ID_AUTO_STICKER = 35;
    private static final int ID_HEART_EFFECT = 36;

    // Security section
    private static final int ID_BLOCK_FOREIGN = 40;
    private static final int ID_DISABLE_AUTODL = 41;
    private static final int ID_CONFIRM_MEDIA = 42;
    private static final int ID_SCREENSHOT_PROTECT = 43;

    // Functions & AeroGram Unique section
    private static final int ID_CHANNEL_HISTORY = 50;
    private static final int ID_SECONDS_TIME = 51;
    private static final int ID_NO_ROUND_NUMS = 52;
    private static final int ID_CONFIRM_CALL = 53;       // AeroGram Unique #1
    private static final int ID_SHOW_ID_DC = 54;         // AeroGram Unique #2
    private static final int ID_SHARE_AERO = 55;

    // Maintenance
    private static final int ID_CLEAR_HISTORY = 60;
    private static final int ID_RESET_ALL = 61;

    private String searchQuery = "";
    private ActionBarMenuItem searchItem;

    @Override
    protected boolean useSimpleActionBar() {
        return true;
    }

    @Override
    protected RecyclerListView getListViewForSimpleGlass() {
        return listView;
    }

    @Override
    public boolean isSupportEdgeToEdge() {
        return true;
    }

    @Override
    protected CharSequence getTitle() {
        return "AeroGram";
    }

    @Override
    public View createView(Context context) {
        View root = super.createView(context);
        if (simpleActionBar != null) {
            final SimpleActionBarMenu menu = simpleActionBar.createMenu();
            searchItem = menu.addItem(0, R.drawable.outline_header_search, getResourceProvider())
                .setIsSearchField(true)
                .setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
                    @Override
                    public void onSearchExpand() {
                        searchQuery = "";
                        if (listView != null && listView.adapter != null) {
                            listView.adapter.update(false);
                        }
                    }

                    @Override
                    public void onSearchCollapse() {
                        searchQuery = "";
                        if (listView != null && listView.adapter != null) {
                            listView.adapter.update(true);
                        }
                    }

                    @Override
                    public void onTextChanged(EditText editText) {
                        searchQuery = editText != null && editText.getText() != null
                            ? editText.getText().toString().trim().toLowerCase(Locale.ROOT)
                            : "";
                        if (listView != null && listView.adapter != null) {
                            listView.adapter.update(true);
                        }
                    }
                });
            searchItem.setSearchFieldHint("Sozlamalarni qidirish...");
        }
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(false);
        }
    }

    @Override
    public boolean onBackPressed(boolean invoked) {
        if (simpleActionBar != null && simpleActionBar.isSearchFieldVisible()) {
            if (invoked) simpleActionBar.closeSearchField();
            return false;
        }
        return super.onBackPressed(invoked);
    }

    private boolean matches(String title, String subtitle) {
        if (TextUtils.isEmpty(searchQuery)) {
            return true;
        }
        String t = title != null ? title.toLowerCase(Locale.ROOT) : "";
        String s = subtitle != null ? subtitle.toLowerCase(Locale.ROOT) : "";
        return t.contains(searchQuery) || s.contains(searchQuery);
    }

    private void addNavIfMatch(ArrayList<UItem> target, int id, int color, int icon, String title, String subtitle, String value, int badge) {
        if (matches(title, subtitle)) {
            target.add(AeroRowCell.Factory.ofNav(id, color, icon, title, subtitle, value, badge));
        }
    }

    private void addSwitchIfMatch(ArrayList<UItem> target, int id, int color, int icon, String title, String subtitle, boolean checked, int badge) {
        if (matches(title, subtitle)) {
            target.add(AeroRowCell.Factory.ofSwitch(id, color, icon, title, subtitle, checked, badge));
        }
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        ArrayList<UItem> top = new ArrayList<>();
        addNavIfMatch(top, ID_ABOUT, 0xFF007AFF, R.drawable.msg_info, "AeroGram haqida", null, null, AeroRowCell.BADGE_NONE);
        if (!top.isEmpty()) {
            items.addAll(top);
            items.add(UItem.asShadow(null));
        }

        ArrayList<UItem> accounts = new ArrayList<>();
        addNavIfMatch(accounts, ID_ACCOUNTS, 0xFF2563EB, R.drawable.msg_contacts, "Barcha akkauntlar", null, UserConfig.getActivatedAccountsCount() + " ta", AeroRowCell.BADGE_NONE);
        if (!accounts.isEmpty()) {
            items.add(UItem.asHeader("AKKAUNTLAR"));
            items.addAll(accounts);
            items.add(UItem.asShadow(null));
        }

        ArrayList<UItem> chat = new ArrayList<>();
        addSwitchIfMatch(chat, ID_SAVE_DELETED, 0xFFEF4444, R.drawable.msg_delete,
            "O‘chirilgan xabarlar", "O‘chirilgan xabarlarni «O‘chirilgan» belgisi bilan saqlash",
            AeroConfig.saveDeletedMessages, AeroRowCell.BADGE_NONE);
        addSwitchIfMatch(chat, ID_EDIT_HISTORY, 0xFF22C55E, R.drawable.msg_recent,
            "Tahrirlar tarixi", "Tahrirlangan xabarni bosib oldingi versiyalarini ko‘rish",
            AeroConfig.saveEditHistory, AeroRowCell.BADGE_NONE);
        addSwitchIfMatch(chat, ID_JUMP_FIRST, 0xFF0EA5E9, R.drawable.msg_go_up,
            "Birinchi xabarga o‘tish", "Chat menyusiga «Birinchi xabarga o‘tish» tugmasini qo‘shish",
            AeroConfig.jumpToFirstMessageButton, AeroRowCell.BADGE_NONE);
        addSwitchIfMatch(chat, ID_GHOST_BUTTON, 0xFF64748B, R.drawable.msg_stealth_locked,
            "«Ghost» rejimi tugmasi", "Chatlar ro‘yxati tepasida tez yoqib-o‘chirish tugmasi",
            AeroConfig.showGhostButtonInChats, AeroRowCell.BADGE_NONE);
        addNavIfMatch(chat, ID_GHOST_DETAILS, 0xFF6366F1, R.drawable.msg_stories_stealth2,
            "Ghost Mode batafsil", "O‘qish, yozish, onlayn va story ko‘rinmasligi",
            AeroConfig.isAnyGhostActive() ? "Yoqiq" : "O‘chiq", AeroRowCell.BADGE_NONE);
        addSwitchIfMatch(chat, ID_CAMERA_HOLD, 0xFFF59E0B, R.drawable.msg_videocall,
            "Kamerani tanlash", "Video-xabar tugmasini bosib turganda kamerani tanlash",
            AeroConfig.cameraSwitchOnHold, AeroRowCell.BADGE_NONE);
        addSwitchIfMatch(chat, ID_GALLERY_ROUND_VIDEO, 0xFF14B8A6, R.drawable.msg_round_play_m,
            "Galereyadan dumaloq video", "Galereyadagi videoni dumaloq video-xabar ko‘rinishida yuborish",
            AeroConfig.galleryRoundVideo, AeroRowCell.BADGE_NEW);
        addSwitchIfMatch(chat, ID_FWD_NO_AUTHOR, 0xFF06B6D4, R.drawable.msg_forward,
            "Muallifsiz uzatish", "Uzatilgan xabarda asl muallif nomini yashirish",
            AeroConfig.forwardWithoutAuthor, AeroRowCell.BADGE_NEW);
        addSwitchIfMatch(chat, ID_FWD_NO_CAPTION, 0xFF38BDF8, R.drawable.msg_stories_caption,
            "Sarlavhasiz uzatish", "Media uzatganda tagidagi matnni (caption) olib tashlash",
            AeroConfig.forwardWithoutCaption, AeroRowCell.BADGE_NONE);
        if (!chat.isEmpty()) {
            items.add(UItem.asHeader("CHAT"));
            items.addAll(chat);
            items.add(UItem.asShadow(null));
        }

        ArrayList<UItem> iface = new ArrayList<>();
        addSwitchIfMatch(iface, ID_HIDE_FOLDERS, 0xFF3B82F6, R.drawable.msg_folders,
            "Papkalarni yashirish", "Chatlar ro‘yxati tepasidagi papkalar qatorini yashirish",
            AeroConfig.hideFolders, AeroRowCell.BADGE_NONE);
        addSwitchIfMatch(iface, ID_STORIES_PANEL, 0xFF10B981, R.drawable.msg_stories_saved,
            "Hikoyalar (Stories) paneli", "Chatlar ro‘yxati tepasida hikoyalarni ko‘rsatish",
            AeroConfig.showStoriesBar, AeroRowCell.BADGE_NONE);
        addSwitchIfMatch(iface, ID_MUTUAL_BADGE, 0xFFF59E0B, R.drawable.msg_contacts_name,
            "O‘zaro kontakt belgisi", "Ikkala tomonlama saqlangan kontaktlar yonida 🤝 belgisi",
            AeroConfig.showMutualBadge, AeroRowCell.BADGE_NONE);
        addSwitchIfMatch(iface, ID_UNLIMITED_PINS, 0xFF22C55E, R.drawable.msg_pin,
            "Cheksiz qadash (Pin)", "5 tadan ko‘p chatlarni lokal ravishda yuqoriga qadash",
            AeroConfig.unlimitedPins, AeroRowCell.BADGE_NEW);
        if (!iface.isEmpty()) {
            items.add(UItem.asHeader("INTERFEYS"));
            items.addAll(iface);
            items.add(UItem.asShadow(null));
        }

        ArrayList<UItem> msgs = new ArrayList<>();
        addNavIfMatch(msgs, ID_TEXT_STYLE, 0xFF8B5CF6, R.drawable.msg_edit,
            "Matn uslubi", "Yuboriladigan xabarlar uchun doimiy format",
            AeroConfig.getTextStyleLabel(), AeroRowCell.BADGE_NONE);
        addNavIfMatch(msgs, ID_AUTO_POSTFIX, 0xFFEC4899, R.drawable.msg_stories_caption,
            "Avto-postfiks", "Har bir chiqish xabari oxiriga imzo qo‘shish",
            AeroConfig.autoPostfixEnabled ? "Yoqiq" : "O‘chiq", AeroRowCell.BADGE_NONE);
        addSwitchIfMatch(msgs, ID_TRANSLATE_BTN, 0xFF6366F1, R.drawable.msg_translate,
            "Tarjima tugmasi", "Xabar menyusida tezkor «Tarjima qilish» tugmasi",
            AeroConfig.showTranslateButton, AeroRowCell.BADGE_NONE);
        addNavIfMatch(msgs, ID_TRANSLATE_LANG, 0xFF4F46E5, R.drawable.msg_language,
            "Tarjima tili", null, AeroConfig.getTranslateLanguageLabel(), AeroRowCell.BADGE_NONE);
        addNavIfMatch(msgs, ID_NEW_ALPHABET, 0xFF0EA5E9, R.drawable.msg_stories_order,
            "Yangi alifbo (ş ç ō ḡ)", "Chiqish o‘zbekcha matnlarni yangi alifboga o‘girish",
            AeroConfig.newUzbekAlphabet ? "Yoqiq" : "O‘chiq", AeroRowCell.BADGE_NEW);
        addSwitchIfMatch(msgs, ID_AUTO_STICKER, 0xFF0D9488, R.drawable.msg_sticker,
            "Avto-stiker", "Har bir matnli xabardan keyin oxirgi stikerni yuborish",
            AeroConfig.autoSticker, AeroRowCell.BADGE_NONE);
        addSwitchIfMatch(msgs, ID_HEART_EFFECT, 0xFFF43F5E, R.drawable.msg_reactions_filled,
            "Yurakcha effekti", "Yuborilgan xabarga ❤️ belgisini avtomatik qo‘shish",
            AeroConfig.heartEffect, AeroRowCell.BADGE_NONE);
        if (!msgs.isEmpty()) {
            items.add(UItem.asHeader("XABARLAR"));
            items.addAll(msgs);
            items.add(UItem.asShadow(null));
        }

        ArrayList<UItem> sec = new ArrayList<>();
        addSwitchIfMatch(sec, ID_BLOCK_FOREIGN, 0xFFEF4444, R.drawable.msg_block2,
            "Xorijiy raqamlarni bloklash", "Kontaktda bo‘lmagan chet el raqamlari xabarlarini ovozsiz qilish",
            AeroConfig.blockForeignNumbers, AeroRowCell.BADGE_NONE);
        addSwitchIfMatch(sec, ID_DISABLE_AUTODL, 0xFFD97706, R.drawable.msg_download,
            "Avtoyuklashni o‘chirish", "Mobil internet va Wi-Fi’da media avtoyuklanishini to‘xtatish",
            AeroConfig.disableAutoDownload, AeroRowCell.BADGE_NONE);
        addSwitchIfMatch(sec, ID_CONFIRM_MEDIA, 0xFFF97316, R.drawable.msg_report,
            "Yuborishdan oldin so‘rash", "Stiker va GIF yuborishdan oldin tasdiqlash oynasi",
            AeroConfig.confirmBeforeSendMedia, AeroRowCell.BADGE_NONE);
        addSwitchIfMatch(sec, ID_SCREENSHOT_PROTECT, 0xFF2563EB, R.drawable.settings_privacy,
            "Skrinshotdan himoya (FLAG_SECURE)", "Ekranni rasmga va videoga olishni cheklash",
            AeroConfig.screenshotProtection, AeroRowCell.BADGE_NONE);
        if (!sec.isEmpty()) {
            items.add(UItem.asHeader("XAVFSIZLIK"));
            items.addAll(sec);
            items.add(UItem.asShadow(null));
        }

        ArrayList<UItem> extra = new ArrayList<>();
        addSwitchIfMatch(extra, ID_CHANNEL_HISTORY, 0xFF4F46E5, R.drawable.msg_recent,
            "Kanal tarixi tugmasi", "Admin kanallarida «Oxirgi amallar» menyusiga tez o‘tish",
            AeroConfig.channelHistoryButton, AeroRowCell.BADGE_NEW);
        addSwitchIfMatch(extra, ID_SECONDS_TIME, 0xFF0891B2, R.drawable.msg_speed,
            "Vaqtda sekundni ko‘rsatish", "Xabar vaqtini 14:22:08 ko‘rinishida chiqarish",
            AeroConfig.showSecondsInTime, AeroRowCell.BADGE_NONE);
        addSwitchIfMatch(extra, ID_NO_ROUND_NUMS, 0xFF059669, R.drawable.msg_stats,
            "Raqamlarni yaxlitlamaslik", "14.8K o‘rniga aniq obunachi va ko‘rish soni",
            AeroConfig.disableNumberRounding, AeroRowCell.BADGE_NONE);
        addSwitchIfMatch(extra, ID_CONFIRM_CALL, 0xFF7C3AED, R.drawable.msg_calls,
            "Qo‘ng‘iroqdan oldin tasdiqlash", "Tasodifiy ovozli yoki video qo‘ng‘iroq ketib qolishidan himoya",
            AeroConfig.confirmBeforeCall, AeroRowCell.BADGE_AERO);
        addSwitchIfMatch(extra, ID_SHOW_ID_DC, 0xFF0284C7, R.drawable.msg_info,
            "Xabar ID va Profil DC ko‘rsatish", "Xabarlarda #ID va profilda Telegram ID hamda Data Center",
            AeroConfig.showIdAndDc, AeroRowCell.BADGE_AERO);
        addNavIfMatch(extra, ID_SHARE_AERO, 0xFF0EA5E9, R.drawable.msg_link,
            "AeroGram haqida ulashish", "Ilova tavsifini do‘stlarga yuborish", null, AeroRowCell.BADGE_NEW);
        if (!extra.isEmpty()) {
            items.add(UItem.asHeader("FUNKSIYALAR VA AEROGRAM MAXSUS"));
            items.addAll(extra);
            items.add(UItem.asShadow("Barcha qo‘shimcha funksiyalar standart holatda o‘chiq bo‘ladi. Faqat o‘zingizga keraklisini yoqing."));
        }

        if (TextUtils.isEmpty(searchQuery)) {
            int totalHistory = AeroDeletedStore.getTotalSavedCount() + AeroEditHistoryStore.getTotalCount();
            items.add(UItem.asButton(ID_CLEAR_HISTORY, R.drawable.msg_delete, "Saqlangan tarixlarni tozalash", totalHistory + " ta").red());
            items.add(UItem.asButton(ID_RESET_ALL, R.drawable.msg_reset, "AeroGram sozlamalarini standartga qaytarish").red());
            items.add(UItem.asShadow(null));
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_ABOUT:
                presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_ABOUT));
                return;
            case ID_ACCOUNTS:
                presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_ACCOUNTS));
                return;
            case ID_GHOST_DETAILS:
                presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_GHOST));
                return;
            case ID_TEXT_STYLE:
                presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_TEXT_STYLE));
                return;
            case ID_AUTO_POSTFIX:
                presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_POSTFIX));
                return;
            case ID_TRANSLATE_LANG:
                presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_TRANSLATE_LANG));
                return;
            case ID_NEW_ALPHABET:
                presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_NEW_ALPHABET));
                return;

            case ID_SAVE_DELETED:
                AeroConfig.saveDeletedMessages = !AeroConfig.saveDeletedMessages;
                AeroConfig.saveBool("saveDeletedMessages", AeroConfig.saveDeletedMessages);
                getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                break;
            case ID_EDIT_HISTORY:
                AeroConfig.saveEditHistory = !AeroConfig.saveEditHistory;
                AeroConfig.saveBool("saveEditHistory", AeroConfig.saveEditHistory);
                break;
            case ID_JUMP_FIRST:
                AeroConfig.jumpToFirstMessageButton = !AeroConfig.jumpToFirstMessageButton;
                AeroConfig.saveBool("jumpToFirstMessageButton", AeroConfig.jumpToFirstMessageButton);
                break;
            case ID_GHOST_BUTTON:
                AeroConfig.showGhostButtonInChats = !AeroConfig.showGhostButtonInChats;
                AeroConfig.saveBool("showGhostButtonInChats", AeroConfig.showGhostButtonInChats);
                getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                break;
            case ID_CAMERA_HOLD:
                AeroConfig.cameraSwitchOnHold = !AeroConfig.cameraSwitchOnHold;
                AeroConfig.saveBool("cameraSwitchOnHold", AeroConfig.cameraSwitchOnHold);
                break;
            case ID_GALLERY_ROUND_VIDEO:
                AeroConfig.galleryRoundVideo = !AeroConfig.galleryRoundVideo;
                AeroConfig.saveBool("galleryRoundVideo", AeroConfig.galleryRoundVideo);
                break;
            case ID_FWD_NO_AUTHOR:
                AeroConfig.forwardWithoutAuthor = !AeroConfig.forwardWithoutAuthor;
                AeroConfig.saveBool("forwardWithoutAuthor", AeroConfig.forwardWithoutAuthor);
                break;
            case ID_FWD_NO_CAPTION:
                AeroConfig.forwardWithoutCaption = !AeroConfig.forwardWithoutCaption;
                AeroConfig.saveBool("forwardWithoutCaption", AeroConfig.forwardWithoutCaption);
                break;

            case ID_HIDE_FOLDERS:
                AeroConfig.hideFolders = !AeroConfig.hideFolders;
                AeroConfig.saveBool("hideFolders", AeroConfig.hideFolders);
                getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
                break;
            case ID_STORIES_PANEL:
                AeroConfig.showStoriesBar = !AeroConfig.showStoriesBar;
                AeroConfig.saveBool("showStoriesBar", AeroConfig.showStoriesBar);
                getNotificationCenter().postNotificationName(NotificationCenter.storiesUpdated);
                break;
            case ID_MUTUAL_BADGE:
                AeroConfig.showMutualBadge = !AeroConfig.showMutualBadge;
                AeroConfig.saveBool("showMutualBadge", AeroConfig.showMutualBadge);
                getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                break;
            case ID_UNLIMITED_PINS:
                AeroConfig.unlimitedPins = !AeroConfig.unlimitedPins;
                AeroConfig.saveBool("unlimitedPins", AeroConfig.unlimitedPins);
                break;

            case ID_TRANSLATE_BTN:
                AeroConfig.showTranslateButton = !AeroConfig.showTranslateButton;
                AeroConfig.saveBool("showTranslateButton", AeroConfig.showTranslateButton);
                break;
            case ID_AUTO_STICKER:
                AeroConfig.autoSticker = !AeroConfig.autoSticker;
                AeroConfig.saveBool("autoSticker", AeroConfig.autoSticker);
                break;
            case ID_HEART_EFFECT:
                AeroConfig.heartEffect = !AeroConfig.heartEffect;
                AeroConfig.saveBool("heartEffect", AeroConfig.heartEffect);
                break;

            case ID_BLOCK_FOREIGN:
                AeroConfig.blockForeignNumbers = !AeroConfig.blockForeignNumbers;
                AeroConfig.saveBool("blockForeignNumbers", AeroConfig.blockForeignNumbers);
                break;
            case ID_DISABLE_AUTODL:
                AeroConfig.disableAutoDownload = !AeroConfig.disableAutoDownload;
                AeroConfig.saveBool("disableAutoDownload", AeroConfig.disableAutoDownload);
                break;
            case ID_CONFIRM_MEDIA:
                AeroConfig.confirmBeforeSendMedia = !AeroConfig.confirmBeforeSendMedia;
                AeroConfig.saveBool("confirmBeforeSendMedia", AeroConfig.confirmBeforeSendMedia);
                break;
            case ID_SCREENSHOT_PROTECT:
                AeroConfig.screenshotProtection = !AeroConfig.screenshotProtection;
                AeroConfig.saveBool("screenshotProtection", AeroConfig.screenshotProtection);
                LaunchActivity.invalidateAeroSecurity();
                break;

            case ID_CHANNEL_HISTORY:
                AeroConfig.channelHistoryButton = !AeroConfig.channelHistoryButton;
                AeroConfig.saveBool("channelHistoryButton", AeroConfig.channelHistoryButton);
                break;
            case ID_SECONDS_TIME:
                AeroConfig.showSecondsInTime = !AeroConfig.showSecondsInTime;
                AeroConfig.saveBool("showSecondsInTime", AeroConfig.showSecondsInTime);
                getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                break;
            case ID_NO_ROUND_NUMS:
                AeroConfig.disableNumberRounding = !AeroConfig.disableNumberRounding;
                AeroConfig.saveBool("disableNumberRounding", AeroConfig.disableNumberRounding);
                getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                break;
            case ID_CONFIRM_CALL:
                AeroConfig.confirmBeforeCall = !AeroConfig.confirmBeforeCall;
                AeroConfig.saveBool("confirmBeforeCall", AeroConfig.confirmBeforeCall);
                break;
            case ID_SHOW_ID_DC:
                AeroConfig.showIdAndDc = !AeroConfig.showIdAndDc;
                AeroConfig.saveBool("showIdAndDc", AeroConfig.showIdAndDc);
                getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                break;
            case ID_SHARE_AERO:
                if (getParentActivity() != null) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, "AeroGram — Ghost Mode, Anti-Delete, Tahrirlar tarixi va maxsus funksiyali tezkor Telegram!");
                    getParentActivity().startActivity(Intent.createChooser(intent, LocaleController.getString(R.string.ShareFile)));
                }
                return;
            case ID_CLEAR_HISTORY:
                confirmClearHistory();
                return;
            case ID_RESET_ALL:
                confirmResetAll();
                return;
            default:
                return;
        }
        listView.adapter.update(true);
    }

    private void confirmClearHistory() {
        if (getParentActivity() == null) return;
        new AlertDialog.Builder(getParentActivity())
            .setTitle("Tarixlarni tozalash")
            .setMessage("Saqlangan barcha o‘chirilgan xabarlar va tahrirlar tarixi tozalansinmi?")
            .setPositiveButton(LocaleController.getString(R.string.Clear), (d, w) -> {
                AeroDeletedStore.clearAll();
                AeroEditHistoryStore.clearAll();
                listView.adapter.update(true);
                BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, "Barcha tarix tozalandi").show();
            })
            .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
            .show();
    }

    private void confirmResetAll() {
        if (getParentActivity() == null) return;
        new AlertDialog.Builder(getParentActivity())
            .setTitle("Standart holatga qaytarish")
            .setMessage("Barcha AeroGram Settings tugmalari dastlabki holatga qaytariladi.")
            .setPositiveButton(LocaleController.getString(R.string.OK), (d, w) -> {
                AeroConfig.resetAll();
                LaunchActivity.invalidateAeroSecurity();
                getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
                getNotificationCenter().postNotificationName(NotificationCenter.storiesUpdated);
                listView.adapter.update(true);
                BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, "Barcha sozlamalar tiklandi").show();
            })
            .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
            .show();
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
