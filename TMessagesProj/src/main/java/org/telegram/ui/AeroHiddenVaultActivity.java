package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AeroConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Discreet Maxfiy Chatlar Seyfi (Hidden Chats Vault) for AeroGram v1.5.0.
 * Opens hidden conversations directly without exposing them in the public dialog list.
 */
public class AeroHiddenVaultActivity extends UniversalFragment {

    private static final int MENU_ADD = 1;
    private static final int ID_HERO = 9001;
    private static final int ID_STEALTH_HIDE = 9010;
    private static final int ID_ADD_CHATS = 9011;
    private static final int ID_CHANGE_PIN = 9012;
    private static final int ID_TEMP_UNHIDE = 9013;
    private static final int BASE_CHAT_ID = 10000;

    private final ArrayList<Long> displayedDialogIds = new ArrayList<>();
    private LinearLayout heroCardView;
    private TextView heroCountBadge;

    @Override
    protected CharSequence getTitle() {
        return AeroConfig.tr("Maxfiy Chatlar Seyfi", "Махфий Чатлар Сейфи");
    }

    @Override
    public View createView(Context context) {
        View root = super.createView(context);
        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(MENU_ADD, R.drawable.msg_add);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_ADD) {
                    presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_HIDDEN_CHATS));
                }
            }
        });
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadList();
    }

    @Override
    public void onFragmentDestroy() {
        if (AeroConfig.hiddenChatsUnlocked) {
            AeroConfig.hiddenChatsUnlocked = false;
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
        }
        super.onFragmentDestroy();
    }

    private void reloadList() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(false);
        }
    }

    private View getOrCreateHeroCard(Context context) {
        if (heroCardView == null) {
            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(18), dp(16), dp(18), dp(16));

            LinearLayout outer = new LinearLayout(context);
            outer.setOrientation(LinearLayout.VERTICAL);
            outer.setPadding(dp(14), dp(10), dp(14), dp(4));

            GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF064E3B, 0xFF047857, 0xFF0EA5E9}
            );
            bg.setCornerRadius(dp(18));
            card.setBackground(bg);

            LinearLayout topRow = new LinearLayout(context);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            ImageView lockIcon = new ImageView(context);
            lockIcon.setImageResource(R.drawable.msg_secret);
            lockIcon.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
            topRow.addView(lockIcon, LayoutHelper.createLinear(22, 22, Gravity.CENTER_VERTICAL, 0, 0, 10, 0));

            TextView title = new TextView(context);
            title.setText(AeroConfig.tr("Maxfiy Chatlar Seyfi", "Махфий Чатлар Сейфи"));
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
            title.setTypeface(AndroidUtilities.bold());
            title.setTextColor(Color.WHITE);
            topRow.addView(title, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

            TextView badge = new TextView(context);
            badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
            badge.setTypeface(AndroidUtilities.bold());
            badge.setTextColor(0xFFECFDF5);
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setCornerRadius(dp(10));
            badgeBg.setColor(0x33FFFFFF);
            badge.setBackground(badgeBg);
            badge.setPadding(dp(10), dp(4), dp(10), dp(4));
            heroCountBadge = badge;
            topRow.addView(badge);

            card.addView(topRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            TextView desc = new TextView(context);
            desc.setText(AeroConfig.tr(
                "Bu yerdagi chatlar umumiy ro‘yxat va Qidiruvdan yashiringan. Seyfni ochish uchun bosh ekranda «AeroGram» yozuvini bosib turing yoki Qidiruvga PIN kodingizni yozing.",
                "Бу ердаги чатлар умумий рўйхат ва Қидирувдан яширинган. Сейфни очиш учун бош экранда «AeroGram» ёзувини босиб туринг ёки Қидирувга ПИН кодингизни ёзинг."
            ));
            desc.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.8f);
            desc.setTextColor(0xE6FFFFFF);
            desc.setLineSpacing(dp(2), 1.0f);
            card.addView(desc, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 10, 0, 0));

            outer.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            heroCardView = outer;
        }
        if (heroCountBadge != null) {
            heroCountBadge.setText(AeroConfig.hiddenDialogIds.size() + AeroConfig.tr(" ta chat", " та чат"));
        }
        return heroCardView;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (getParentActivity() != null) {
            items.add(UItem.asCustom(ID_HERO, getOrCreateHeroCard(getParentActivity())));
        }

        items.add(UItem.asHeader(AeroConfig.tr("Yashirilgan suhbatlar", "Яширилган суҳбатлар")));
        displayedDialogIds.clear();
        List<Long> sortedIds = new ArrayList<>(AeroConfig.hiddenDialogIds);
        Collections.sort(sortedIds);

        MessagesController mc = MessagesController.getInstance(currentAccount);
        if (sortedIds.isEmpty()) {
            items.add(AeroRowCell.Factory.ofNav(ID_ADD_CHATS, 0xFF059669, R.drawable.msg_add,
                AeroConfig.tr("Hozircha seyf bo‘sh — chat qo‘shing", "Ҳозирча сейф бўш — чат қўшинг"),
                AeroConfig.tr("Yoki asosiy ro‘yxatda chatni bosib turib «Maxfiy seyfga yashirish»ni tanlang", "Ёки асосий рўйхатда чатни босиб туриб «Махфий сейфга яшириш»ни танланг"),
                "+", AeroRowCell.BADGE_NEW));
        } else {
            for (int i = 0; i < sortedIds.size(); i++) {
                long did = sortedIds.get(i);
                displayedDialogIds.add(did);
                String title = resolveDialogTitle(mc, did);
                TLRPC.Dialog dialog = mc.dialogs_dict.get(did);
                int unread = dialog != null ? dialog.unread_count : 0;
                String sub = unread > 0
                    ? unread + AeroConfig.tr(" ta o‘qilmagan xabar • Uzoq bosib chiqarish mumkin", " та ўқилмаган хабар • Узоқ босиб чиқариш мумкин")
                    : AeroConfig.tr("Ochish uchun bosing • Chiqarish uchun bosib turing", "Очиш учун босинг • Чиқариш учун босиб туринг");
                String pill = unread > 0 ? String.valueOf(unread) : AeroConfig.tr("Ochish", "Очиш");
                items.add(AeroRowCell.Factory.ofNav(BASE_CHAT_ID + i, 0xFF059669, R.drawable.msg_secret,
                    title, sub, pill, unread > 0 ? AeroRowCell.BADGE_NEW : AeroRowCell.BADGE_NONE));
            }
        }

        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader(AeroConfig.tr("Seyf boshqaruvi va Maxfiylik", "Сейф бошқаруви ва Махфийлик")));
        items.add(AeroRowCell.Factory.ofNav(ID_ADD_CHATS, 0xFF0EA5E9, R.drawable.msg_viewchats,
            AeroConfig.tr("Chatlarni tanlash / qo‘shish", "Чатларни танлаш / қўшиш"),
            AeroConfig.tr("Seyfga yangi chatlar qo‘shish yoki olib tashlash", "Сейфга янги чатлар қўшиш ёки олиб ташлаш"),
            String.valueOf(AeroConfig.hiddenDialogIds.size()), AeroRowCell.BADGE_NONE));
        items.add(AeroRowCell.Factory.ofSwitch(ID_STEALTH_HIDE, 0xFF0F766E, R.drawable.msg_stealth_locked,
            AeroConfig.tr("Sozlamalarda maxfiy bo‘limni yashirish (Stealth)", "Созламаларда махфий бўлимни яшириш (Stealth)"),
            AeroConfig.tr("PIN va yashirin chatlar bo‘lganda Sozlamalar ro‘yxatida ko‘rinmaydi", "ПИН ва яширин чатлар бўлганда Созламалар рўйхатида кўринмайди"),
            AeroConfig.stealthVaultMode, AeroRowCell.BADGE_AERO));
        items.add(AeroRowCell.Factory.ofNav(ID_CHANGE_PIN, 0xFF0284C7, R.drawable.msg_pin_code,
            AeroConfig.tr("Seyf PIN kodini o‘zgartirish", "Сейф ПИН кодини ўзгартириш"),
            TextUtils.isEmpty(AeroConfig.chatLockPin) ? AeroConfig.tr("O‘rnatilmagan", "Ўрнатилмаган") : AeroConfig.tr("Faol PIN • Qidiruv orqali ochish kodi", "Фаол ПИН • Қидирув орқали очиш коди"),
            "****", AeroRowCell.BADGE_NONE));
        items.add(AeroRowCell.Factory.ofSwitch(ID_TEMP_UNHIDE, 0xFF6366F1, R.drawable.msg_views,
            AeroConfig.tr("Vaqtincha umumiy ro‘yxatda ko‘rsatish", "Вақтинча умумий рўйхатда кўрсатиш"),
            AeroConfig.tr("Seyfdan chiqqandan so‘ng avtomatik yana yashiriladi", "Сейфдан чиққандан сўнг автоматик яна яширилади"),
            AeroConfig.hiddenChatsUnlocked, AeroRowCell.BADGE_NONE));

        items.add(UItem.asShadow(AeroConfig.tr(
            "Maslahat: Seyfni istalgan payt bosh sahifadagi AeroGram logosini bosib turib yoki Qidiruvga PIN kodingizni kiritib ochishingiz mumkin.",
            "Маслаҳат: Сейфни исталган пайт бош саҳифадаги AeroGram логосини босиб туриб ёки Қидирувга ПИН кодингизни киритиб очишингиз мумкин."
        )));
    }

    private String resolveDialogTitle(MessagesController mc, long did) {
        if (DialogObject.isUserDialog(did)) {
            String name = mc.getPeerName(did);
            return TextUtils.isEmpty(name) ? ("User " + did) : name;
        }
        TLRPC.Chat chat = mc.getChat(-did);
        if (chat != null && !TextUtils.isEmpty(chat.title)) {
            return chat.title;
        }
        return "Chat " + did;
    }

    private void openHiddenDialog(long did) {
        Bundle args = new Bundle();
        if (DialogObject.isEncryptedDialog(did)) {
            args.putInt("enc_id", DialogObject.getEncryptedChatId(did));
        } else if (DialogObject.isUserDialog(did)) {
            args.putLong("user_id", did);
        } else {
            args.putLong("chat_id", -did);
        }
        AeroConfig.unlockDialogForSession(did);
        presentFragment(new ChatActivity(args));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item == null) return;
        if (item.id >= BASE_CHAT_ID) {
            int idx = item.id - BASE_CHAT_ID;
            if (idx >= 0 && idx < displayedDialogIds.size()) {
                openHiddenDialog(displayedDialogIds.get(idx));
            }
        } else if (item.id == ID_ADD_CHATS) {
            presentFragment(new AeroSubSettingsActivity(AeroSubSettingsActivity.MODE_HIDDEN_CHATS));
        } else if (item.id == ID_STEALTH_HIDE) {
            AeroConfig.stealthVaultMode = !AeroConfig.stealthVaultMode;
            AeroConfig.saveBool("stealthVaultMode", AeroConfig.stealthVaultMode);
            reloadList();
        } else if (item.id == ID_CHANGE_PIN && getParentActivity() != null) {
            AeroUiHelper.showSetPinDialog(getParentActivity(), this::reloadList);
        } else if (item.id == ID_TEMP_UNHIDE) {
            AeroConfig.hiddenChatsUnlocked = !AeroConfig.hiddenChatsUnlocked;
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
            reloadList();
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        if (item != null && item.id >= BASE_CHAT_ID && getParentActivity() != null) {
            int idx = item.id - BASE_CHAT_ID;
            if (idx >= 0 && idx < displayedDialogIds.size()) {
                final long did = displayedDialogIds.get(idx);
                String name = item.text != null ? item.text.toString() : "Chat";
                new AlertDialog.Builder(getParentActivity())
                    .setTitle(AeroConfig.tr("Seyfdan chiqarish", "Сейфдан чиқариш"))
                    .setMessage(AeroConfig.tr(
                        "«" + name + "» suhbati umumiy chatlar ro‘yxatiga qaytarilsinmi?",
                        "«" + name + "» суҳбати умумий чатлар рўйхатига қайтарилсинми?"
                    ))
                    .setPositiveButton(AeroConfig.tr("Qaytarish", "Қайтариш"), (d, w) -> {
                        AeroConfig.setDialogHidden(did, false);
                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
                        reloadList();
                        Toast.makeText(getParentActivity(), AeroConfig.tr("Seyfdan chiqarildi", "Сейфдан чиқарилди"), Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(AeroConfig.tr("Bekor qilish", "Бекор қилиш"), null)
                    .show();
                return true;
            }
        }
        return false;
    }
}
