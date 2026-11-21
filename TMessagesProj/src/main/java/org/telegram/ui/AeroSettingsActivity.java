package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AeroConfig;
import org.telegram.messenger.AeroDeletedStore;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.IconBackgroundColors;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

public class AeroSettingsActivity extends UniversalFragment {

    public static final int MODE_HUB = 0;
    public static final int MODE_GHOST = 1;
    public static final int MODE_ANTI_DELETE = 2;
    public static final int MODE_CHATS = 3;
    public static final int MODE_SECURITY = 4;

    private static final int ID_QUICK_GHOST = 10;
    private static final int ID_CAT_GHOST = 11;
    private static final int ID_CAT_ANTI_DELETE = 12;
    private static final int ID_CAT_CHATS = 13;
    private static final int ID_CAT_SECURITY = 14;
    private static final int ID_RESET_ALL = 15;

    private static final int ID_GHOST_READ = 20;
    private static final int ID_GHOST_TYPING = 21;
    private static final int ID_GHOST_OFFLINE = 22;
    private static final int ID_GHOST_READ_ON_INTERACT = 23;
    private static final int ID_GHOST_STORY = 24;

    private static final int ID_SAVE_DELETED = 30;
    private static final int ID_CLEAR_DELETED = 31;

    private static final int ID_FWD_NO_AUTHOR = 40;
    private static final int ID_FWD_NO_CAPTION = 41;
    private static final int ID_SECONDS_IN_TIME = 42;
    private static final int ID_NO_ROUND_NUMBERS = 43;

    private static final int ID_SCREENSHOT_PROTECT = 50;

    private final int mode;
    private FrameLayout heroWrapper;
    private TextView ghostPillView;
    private TextView antiDeletePillView;

    public AeroSettingsActivity() {
        this(MODE_HUB);
    }

    public AeroSettingsActivity(int mode) {
        this.mode = mode;
    }

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
        switch (mode) {
            case MODE_GHOST:
                return "Ghost Mode";
            case MODE_ANTI_DELETE:
                return "O‘chirilgan xabarlar";
            case MODE_CHATS:
                return "Chatlar va Ko‘rinish";
            case MODE_SECURITY:
                return "Xavfsizlik va Himoya";
            case MODE_HUB:
            default:
                return "AeroGram Settings";
        }
    }

    @Override
    public View createView(Context context) {
        setHasOwnBackground(true);
        fragmentView = super.createView(context);
        listView.setSections();
        listView.setSectionsDrawBackground(true);
        if (mode == MODE_HUB) {
            heroWrapper = createHeroCard(context);
        }
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null && listView.adapter != null) {
            updateHeroPills();
            listView.adapter.update(true);
        }
    }

    private FrameLayout createHeroCard(Context context) {
        FrameLayout outer = new FrameLayout(context);
        outer.setPadding(dp(16), dp(12), dp(16), dp(12));

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(16));

        GradientDrawable cardBg = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[] { 0xFF0EA5E9, 0xFF0284C7, 0xFF312E81 }
        );
        cardBg.setCornerRadius(dp(18));
        card.setBackground(cardBg);

        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        ImageView logo = new ImageView(context);
        logo.setImageResource(R.drawable.ic_aerogram_logo);
        topRow.addView(logo, LayoutHelper.createLinear(54, 54));

        LinearLayout titleCol = new LinearLayout(context);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setPadding(dp(14), 0, 0, 0);

        TextView titleView = new TextView(context);
        titleView.setText("AeroGram");
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleCol.addView(titleView);

        TextView subView = new TextView(context);
        subView.setText("v" + BuildConfig.BUILD_VERSION_STRING + " • Maxsus Edition");
        subView.setTextColor(0xDDF0F9FF);
        subView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        titleCol.addView(subView);

        topRow.addView(titleCol, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        card.addView(topRow);

        LinearLayout pillsRow = new LinearLayout(context);
        pillsRow.setOrientation(LinearLayout.HORIZONTAL);
        pillsRow.setPadding(0, dp(14), 0, 0);

        ghostPillView = createStatusPill(context);
        antiDeletePillView = createStatusPill(context);
        pillsRow.addView(ghostPillView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));
        pillsRow.addView(antiDeletePillView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        card.addView(pillsRow);
        outer.addView(card, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        updateHeroPills();
        return outer;
    }

    private TextView createStatusPill(Context context) {
        TextView tv = new TextView(context);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(dp(10), dp(4), dp(10), dp(4));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x33FFFFFF);
        bg.setCornerRadius(dp(50));
        tv.setBackground(bg);
        return tv;
    }

    private void updateHeroPills() {
        if (ghostPillView != null) {
            ghostPillView.setText(AeroConfig.isAnyGhostActive() ? "Ghost: Yoqiq" : "Ghost: O‘chiq");
        }
        if (antiDeletePillView != null) {
            int count = AeroDeletedStore.getTotalSavedCount();
            antiDeletePillView.setText(
                AeroConfig.saveDeletedMessages
                    ? ("Anti-Delete: Yoqiq (" + count + ")")
                    : "Anti-Delete: O‘chiq"
            );
        }
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        switch (mode) {
            case MODE_GHOST:
                items.add(UItem.asHeader("Ghost Mode (Ko‘rinmaslik)"));
                items.add(UItem.asButtonCheck(
                    ID_GHOST_READ,
                    "O‘qilganlik belgisini yubormaslik",
                    "Xabarni ochsangiz ham suhbatdoshga ikki belgi ketmaydi"
                ).setChecked(AeroConfig.ghostDontReadMessages));
                items.add(UItem.asButtonCheck(
                    ID_GHOST_TYPING,
                    "Yozayotganini yashirish",
                    "\"yozmoqda...\" va \"ovoz yozmoqda...\" yuborilmaydi"
                ).setChecked(AeroConfig.ghostDontSendTyping));
                items.add(UItem.asButtonCheck(
                    ID_GHOST_OFFLINE,
                    "Doimiy Offline rejimi",
                    "Ilovada bo‘lsangiz ham statusingizni offline saqlaydi"
                ).setChecked(AeroConfig.ghostStayOffline));
                items.add(UItem.asButtonCheck(
                    ID_GHOST_READ_ON_INTERACT,
                    "Javob yozganda o‘qilgan qilish",
                    "Chatga xabar yozganingizdagina o‘qilgan deb belgilanadi"
                ).setChecked(AeroConfig.ghostReadOnInteract));
                items.add(UItem.asButtonCheck(
                    ID_GHOST_STORY,
                    "Story ko‘rganlikni yashirish",
                    "Hikoyalarni (Stories) anonim ko‘rish imkoniyati"
                ).setChecked(AeroConfig.ghostHideStoryViews));
                items.add(UItem.asShadow("Barcha Ghost Mode sozlamalari standart holatda o‘chiq bo‘ladi. Faqat o‘zingizga keraklisini yoqib oling."));
                break;

            case MODE_ANTI_DELETE:
                items.add(UItem.asHeader("O‘chirilgan xabarlar"));
                items.add(UItem.asButtonCheck(
                    ID_SAVE_DELETED,
                    "O‘chirilgan xabarlarni saqlash",
                    "Shaxsiy chatlar, guruhlar va kanallarda o‘chirilgan barcha xabarlarni saqlab qoladi"
                ).setChecked(AeroConfig.saveDeletedMessages));
                items.add(UItem.asShadow("Yoqilganda kimdir xabarni o‘chirsa ham chatdan o‘chmaydi va yonida \"O‘chirilgan\" belgisi bilan saqlanib qoladi."));
                int totalSaved = AeroDeletedStore.getTotalSavedCount();
                items.add(UItem.asButton(ID_CLEAR_DELETED, R.drawable.msg_delete, "Saqlangan tarixni tozalash", totalSaved + " ta").red());
                break;

            case MODE_CHATS:
                items.add(UItem.asHeader("Uzatish (Forward) va Ko‘rinish"));
                items.add(UItem.asButtonCheck(
                    ID_FWD_NO_AUTHOR,
                    "Muallif nomini yashirib uzatish",
                    "Xabar uzatilganda asl muallif havolasisiz boradi"
                ).setChecked(AeroConfig.forwardWithoutAuthor));
                items.add(UItem.asButtonCheck(
                    ID_FWD_NO_CAPTION,
                    "Sarlavha (caption) siz uzatish",
                    "Rasm yoki video uzatilganda tagidagi matn olib tashlanadi"
                ).setChecked(AeroConfig.forwardWithoutCaption));
                items.add(UItem.asButtonCheck(
                    ID_SECONDS_IN_TIME,
                    "Xabar vaqtida sekundni ko‘rsatish",
                    "Masalan: 14:22:08 ko‘rinishida ko‘rsatiladi"
                ).setChecked(AeroConfig.showSecondsInTime));
                items.add(UItem.asButtonCheck(
                    ID_NO_ROUND_NUMBERS,
                    "Raqamlarni yaxlitlamaslik",
                    "14.8K o‘rniga aniq obunachi sonini chiqaradi"
                ).setChecked(AeroConfig.disableNumberRounding));
                items.add(UItem.asShadow("Ushbu qulayliklar ham standart holatda o‘chiq bo‘ladi."));
                break;

            case MODE_SECURITY:
                items.add(UItem.asHeader("Maxfiylik himoyasi"));
                items.add(UItem.asButtonCheck(
                    ID_SCREENSHOT_PROTECT,
                    "Skrinshotdan himoya (FLAG_SECURE)",
                    "Ekranni rasmga olish va yozib olishni cheklaydi"
                ).setChecked(AeroConfig.screenshotProtection));
                items.add(UItem.asShadow("FLAG_SECURE standart holatda o‘chiq bo‘ladi, xohlagan paytingiz yoqib qo‘yishingiz mumkin."));
                break;

            case MODE_HUB:
            default:
                if (heroWrapper != null) {
                    items.add(UItem.asCustom(heroWrapper));
                }
                items.add(UItem.asHeader("Tezkor boshqaruv"));
                items.add(UItem.asButtonCheck(
                    ID_QUICK_GHOST,
                    "Tezkor Ghost Mode",
                    "Bir bosishda barcha ko‘rinmaslik rejimlarini yoqish yoki o‘chirish"
                ).setChecked(AeroConfig.isAnyGhostActive()));
                items.add(UItem.asButtonCheck(
                    ID_SAVE_DELETED,
                    "O‘chirilgan xabarlarni saqlash",
                    "Shaxsiy chat, guruh va kanallarda o‘chirilgan xabarlarni saqlaydi"
                ).setChecked(AeroConfig.saveDeletedMessages));
                items.add(UItem.asShadow(null));

                items.add(UItem.asHeader("Kengaytirilgan bo‘limlar"));
                items.add(SettingsActivity.SettingCell.Factory.of(
                    ID_CAT_GHOST,
                    IconBackgroundColors.PURPLE.top,
                    IconBackgroundColors.PURPLE.bottom,
                    R.drawable.msg_stealth_locked,
                    "Ghost Mode sozlamalari",
                    "O‘qish, yozish, onlayn va story ko‘rish",
                    AeroConfig.isAnyGhostActive() ? "Yoqiq" : "O‘chiq"
                ));
                items.add(SettingsActivity.SettingCell.Factory.of(
                    ID_CAT_ANTI_DELETE,
                    IconBackgroundColors.RED.top,
                    IconBackgroundColors.RED.bottom,
                    R.drawable.msg_delete,
                    "O‘chirilgan xabarlar (Anti-Delete)",
                    "O‘chirilgan xabarlarni saqlash va tarixni tozalash",
                    AeroConfig.saveDeletedMessages ? "Yoqiq" : "O‘chiq"
                ));
                items.add(SettingsActivity.SettingCell.Factory.of(
                    ID_CAT_CHATS,
                    IconBackgroundColors.GREEN.top,
                    IconBackgroundColors.GREEN.bottom,
                    R.drawable.settings_chat,
                    "Chatlar va Ko‘rinish",
                    "Uzatish, sekundli vaqt, aniq raqamlar"
                ));
                items.add(SettingsActivity.SettingCell.Factory.of(
                    ID_CAT_SECURITY,
                    IconBackgroundColors.BLUE_DEEP.top,
                    IconBackgroundColors.BLUE_DEEP.bottom,
                    R.drawable.settings_privacy,
                    "Xavfsizlik va Skrinshot",
                    "FLAG_SECURE ekran himoyasi",
                    AeroConfig.screenshotProtection ? "Yoqiq" : "O‘chiq"
                ));
                items.add(UItem.asShadow("Barcha AeroGram maxsus funksiyalari dastlab o‘chirilgan bo‘ladi."));
                items.add(UItem.asButton(ID_RESET_ALL, R.drawable.msg_reset, "AeroGram sozlamalarini standartga qaytarish").red());
                items.add(UItem.asShadow(null));
                break;
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_CAT_GHOST:
                presentFragment(new AeroSettingsActivity(MODE_GHOST));
                return;
            case ID_CAT_ANTI_DELETE:
                presentFragment(new AeroSettingsActivity(MODE_ANTI_DELETE));
                return;
            case ID_CAT_CHATS:
                presentFragment(new AeroSettingsActivity(MODE_CHATS));
                return;
            case ID_CAT_SECURITY:
                presentFragment(new AeroSettingsActivity(MODE_SECURITY));
                return;
            case ID_QUICK_GHOST:
                AeroConfig.toggleMasterGhost(!AeroConfig.isAnyGhostActive());
                if (AeroConfig.ghostStayOffline) {
                    MessagesController.getInstance(currentAccount).updateTimerProc();
                }
                break;
            case ID_GHOST_READ:
                AeroConfig.ghostDontReadMessages = !AeroConfig.ghostDontReadMessages;
                AeroConfig.saveBool("ghostDontReadMessages", AeroConfig.ghostDontReadMessages);
                break;
            case ID_GHOST_TYPING:
                AeroConfig.ghostDontSendTyping = !AeroConfig.ghostDontSendTyping;
                AeroConfig.saveBool("ghostDontSendTyping", AeroConfig.ghostDontSendTyping);
                break;
            case ID_GHOST_OFFLINE:
                AeroConfig.ghostStayOffline = !AeroConfig.ghostStayOffline;
                AeroConfig.saveBool("ghostStayOffline", AeroConfig.ghostStayOffline);
                MessagesController.getInstance(currentAccount).updateTimerProc();
                break;
            case ID_GHOST_READ_ON_INTERACT:
                AeroConfig.ghostReadOnInteract = !AeroConfig.ghostReadOnInteract;
                AeroConfig.saveBool("ghostReadOnInteract", AeroConfig.ghostReadOnInteract);
                break;
            case ID_GHOST_STORY:
                AeroConfig.ghostHideStoryViews = !AeroConfig.ghostHideStoryViews;
                AeroConfig.saveBool("ghostHideStoryViews", AeroConfig.ghostHideStoryViews);
                break;
            case ID_SAVE_DELETED:
                AeroConfig.saveDeletedMessages = !AeroConfig.saveDeletedMessages;
                AeroConfig.saveBool("saveDeletedMessages", AeroConfig.saveDeletedMessages);
                getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                break;
            case ID_CLEAR_DELETED:
                confirmClearDeleted();
                return;
            case ID_FWD_NO_AUTHOR:
                AeroConfig.forwardWithoutAuthor = !AeroConfig.forwardWithoutAuthor;
                AeroConfig.saveBool("forwardWithoutAuthor", AeroConfig.forwardWithoutAuthor);
                break;
            case ID_FWD_NO_CAPTION:
                AeroConfig.forwardWithoutCaption = !AeroConfig.forwardWithoutCaption;
                AeroConfig.saveBool("forwardWithoutCaption", AeroConfig.forwardWithoutCaption);
                break;
            case ID_SECONDS_IN_TIME:
                AeroConfig.showSecondsInTime = !AeroConfig.showSecondsInTime;
                AeroConfig.saveBool("showSecondsInTime", AeroConfig.showSecondsInTime);
                getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                break;
            case ID_NO_ROUND_NUMBERS:
                AeroConfig.disableNumberRounding = !AeroConfig.disableNumberRounding;
                AeroConfig.saveBool("disableNumberRounding", AeroConfig.disableNumberRounding);
                getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_ALL);
                break;
            case ID_SCREENSHOT_PROTECT:
                AeroConfig.screenshotProtection = !AeroConfig.screenshotProtection;
                AeroConfig.saveBool("screenshotProtection", AeroConfig.screenshotProtection);
                LaunchActivity.invalidateAeroSecurity();
                break;
            case ID_RESET_ALL:
                confirmResetAll();
                return;
            default:
                return;
        }
        updateHeroPills();
        listView.adapter.update(true);
    }

    private void confirmClearDeleted() {
        new AlertDialog.Builder(getParentActivity())
            .setTitle("Tarixni tozalash")
            .setMessage("Saqlangan barcha o‘chirilgan xabarlar belgilari tozalansinmi?")
            .setPositiveButton(LocaleController.getString(R.string.Clear), (dialog, which) -> {
                AeroDeletedStore.clearAll();
                updateHeroPills();
                listView.adapter.update(true);
                BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, "Tarix tozalandi").show();
            })
            .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
            .show();
    }

    private void confirmResetAll() {
        new AlertDialog.Builder(getParentActivity())
            .setTitle("Standart holatga qaytarish")
            .setMessage("Barcha AeroGram Settings tugmalari o‘chiriladi (Ghost Mode va Anti-Delete ham).")
            .setPositiveButton(LocaleController.getString(R.string.OK), (dialog, which) -> {
                AeroConfig.resetAll();
                LaunchActivity.invalidateAeroSecurity();
                updateHeroPills();
                listView.adapter.update(true);
                BulletinFactory.of(this).createSimpleBulletin(R.raw.contact_check, "Barcha sozlamalar o‘chirildi").show();
            })
            .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
            .show();
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
