package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AeroConfig;
import org.telegram.messenger.AeroDeletedStore;
import org.telegram.messenger.AeroEditHistoryStore;
import org.telegram.messenger.AeroTextUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * Rich sub-screens for AeroGram Settings (matching NovaGram.mp4 video detail screens):
 * About AeroGram guide cards, Active Accounts, Real Analytics, Ghost details,
 * Text Style picker, Auto-Postfix editor, New Uzbek Alphabet converter, and Translate language picker.
 */
public class AeroSubSettingsActivity extends UniversalFragment {

    public static final int MODE_ABOUT = 1;
    public static final int MODE_ACCOUNTS = 2;
    public static final int MODE_STATS = 3;
    public static final int MODE_GHOST = 4;
    public static final int MODE_TEXT_STYLE = 5;
    public static final int MODE_POSTFIX = 6;
    public static final int MODE_NEW_ALPHABET = 7;
    public static final int MODE_TRANSLATE_LANG = 8;

    private static final int ID_GHOST_MASTER = 100;
    private static final int ID_GHOST_READ = 101;
    private static final int ID_GHOST_TYPING = 102;
    private static final int ID_GHOST_OFFLINE = 103;
    private static final int ID_GHOST_READ_ON_SEND = 104;
    private static final int ID_GHOST_STORY = 105;

    private static final int ID_POSTFIX_TOGGLE = 200;
    private static final int ID_NEW_ALPHABET_TOGGLE = 300;
    private static final int ID_NEW_ALPHABET_CONVERT_DIALOG = 301;
    private static final int ID_ACCOUNT_BASE = 400;
    private static final int ID_STYLE_BASE = 500;
    private static final int ID_LANG_BASE = 600;

    private static final String[] LANG_CODES = new String[] { "uz", "en", "ru", "tr", "de", "fr", "es", "ar" };
    private static final String[] LANG_NAMES = new String[] {
        "O‘zbekcha", "English", "Русский", "Türkçe", "Deutsch", "Français", "Español", "العربية"
    };

    private final int mode;

    public AeroSubSettingsActivity(int mode) {
        this.mode = mode;
    }

    @Override
    protected boolean useSimpleActionBar() {
        return true;
    }

    @Override
    protected org.telegram.ui.Components.RecyclerListView getListViewForSimpleGlass() {
        return listView;
    }

    @Override
    public boolean isSupportEdgeToEdge() {
        return true;
    }

    @Override
    protected CharSequence getTitle() {
        switch (mode) {
            case MODE_ABOUT: return "AeroGram haqida";
            case MODE_ACCOUNTS: return "Barcha akkauntlar";
            case MODE_STATS: return "AeroGram Statistika";
            case MODE_GHOST: return "Ghost Mode (Ko‘rinmaslik)";
            case MODE_TEXT_STYLE: return "Matn uslubi";
            case MODE_POSTFIX: return "Avto-postfiks";
            case MODE_NEW_ALPHABET: return "Yangi alifbo";
            case MODE_TRANSLATE_LANG: return "Tarjima tili";
            default: return "AeroGram";
        }
    }

    private View makeAboutCard(Context context, int color, int icon, String title, String enableText, String howText, String disableText) {
        FrameLayout wrap = new FrameLayout(context);
        wrap.setPadding(dp(14), dp(6), dp(14), dp(6));

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite, getResourceProvider()));
        card.setBackground(bg);

        LinearLayout head = new LinearLayout(context);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout badge = new FrameLayout(context);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(color);
        badgeBg.setCornerRadius(dp(8));
        badge.setBackground(badgeBg);
        ImageView iv = new ImageView(context);
        iv.setImageResource(icon);
        iv.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        badge.addView(iv, LayoutHelper.createFrame(18, 18, Gravity.CENTER));
        head.addView(badge, LayoutHelper.createLinear(28, 28, 0, 0, 10, 0));

        TextView titleTv = new TextView(context);
        titleTv.setText(title);
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15.5f);
        titleTv.setTypeface(AndroidUtilities.bold());
        titleTv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        head.addView(titleTv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        card.addView(head);

        TextView bodyTv = new TextView(context);
        bodyTv.setText("YOQISH: " + enableText + "\n\nQANDAY ISHLAYDI: " + howText + "\n\nO‘CHIRISH: " + disableText);
        bodyTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
        bodyTv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, getResourceProvider()));
        bodyTv.setPadding(0, dp(8), 0, 0);
        card.addView(bodyTv);

        wrap.addView(card, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return wrap;
    }

    private View makeInputBoxCard(Context context, String hint, String initial, Utilities.Callback<String> onChanged) {
        FrameLayout wrap = new FrameLayout(context);
        wrap.setPadding(dp(16), dp(8), dp(16), dp(8));

        EditText et = new EditText(context);
        et.setHint(hint);
        et.setText(initial);
        et.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, getResourceProvider()));
        et.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText, getResourceProvider()));
        et.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15.5f);
        et.setBackground(null);
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                onChanged.run(s != null ? s.toString() : "");
            }
        });

        FrameLayout box = new FrameLayout(context);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite, getResourceProvider()));
        box.setBackground(bg);
        box.addView(et, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        wrap.addView(box, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return wrap;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        Context ctx = getContext();
        switch (mode) {
            case MODE_ABOUT:
                items.add(UItem.asHeader("AeroGram nima?"));
                items.add(UItem.asShadow("AeroGram v" + BuildConfig.BUILD_VERSION_STRING + " — rasmiy Telegram bazasida yaratilgan, kengaytirilgan maxfiylik va qulaylik vositalariga ega tezkor ilova."));
                if (ctx != null) {
                    items.add(UItem.asCustom(makeAboutCard(ctx, 0xFF6366F1, R.drawable.msg_stealth_locked,
                        "«Ghost» (Ko‘rinmas) rejimi",
                        "AeroGram Settings → «Ghost» rejimi tugmasi yoki Ghost sozlamalari orqali kerakli bandni yoqing.",
                        "Xabarlarni o‘qish belgisi (ikkinchi galochka), «yozmoqda...» holati, onlayn status va story ko‘rganlikni yubormaydi.",
                        "Tugmani qaytadan o‘chirib qo‘ying — barcha xabarnoma va statuslar odatdagidek tiklanadi.")));
                    items.add(UItem.asCustom(makeAboutCard(ctx, 0xFFEF4444, R.drawable.msg_delete,
                        "O‘chirilgan xabarlarni saqlash",
                        "AeroGram Settings → Chat → O‘chirilgan xabarlar tugmasini yoqing.",
                        "Shaxsiy chat, guruh va kanallarda suhbatdosh o‘chirgan xabarlar chatda qoladi va yonida «O‘chirilgan» belgisi ko‘rinadi.",
                        "Tugmani o‘chirsangiz chatlar odatiy Telegram kabi ishlaydi.")));
                    items.add(UItem.asCustom(makeAboutCard(ctx, 0xFF22C55E, R.drawable.msg_recent,
                        "Tahrirlar tarixi",
                        "AeroGram Settings → Chat → Tahrirlar tarixi ni yoqing.",
                        "Xabar tahrirlanganda avvalgi matn qurilmangizda saqlanadi. Tahrirlangan xabarni bosib «Tahrirlar tarixi»ni oching.",
                        "Tugmani o‘chirib qo‘ying.")));
                    items.add(UItem.asCustom(makeAboutCard(ctx, 0xFF0284C7, R.drawable.msg_calls,
                        "AeroGram Maxsus: Qo‘ng‘iroqni tasdiqlash",
                        "AeroGram Settings → Qo‘shimcha → Qo‘ng‘iroqdan oldin tasdiqlash ni yoqing.",
                        "Chat yoki profildagi qo‘ng‘iroq tugmasi tasodifan bosilganda qo‘ng‘iroq ketishidan oldin tasdiq so‘raydi.",
                        "Tugmani o‘chirib qo‘ying.")));
                    items.add(UItem.asCustom(makeAboutCard(ctx, 0xFF0EA5E9, R.drawable.msg_info,
                        "AeroGram Maxsus: Xabar ID va Profil DC",
                        "AeroGram Settings → Qo‘shimcha → Xabar ID va Profil DC ni yoqing.",
                        "Har bir xabar vaqti yonida #ID hamda foydalanuvchi/kanal profilida ID va Data Center (DC) ko‘rsatiladi.",
                        "Tugmani o‘chirib qo‘ying.")));
                }
                break;

            case MODE_ACCOUNTS:
                int count = UserConfig.getActivatedAccountsCount();
                items.add(UItem.asHeader("Jami faol akkauntlar: " + count + " ta"));
                for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                    UserConfig uc = UserConfig.getInstance(a);
                    if (!uc.isClientActivated()) continue;
                    TLRPC.User u = uc.getCurrentUser();
                    String name = u != null ? UserObject.getUserName(u) : ("Akkaunt #" + (a + 1));
                    String sub = u != null && !TextUtils.isEmpty(u.phone) ? ("+" + u.phone + (u.username != null ? " • @" + u.username : "")) : ("ID: " + uc.getClientUserId());
                    items.add(AeroRowCell.Factory.ofNav(
                        ID_ACCOUNT_BASE + a,
                        a == currentAccount ? 0xFF16A34A : 0xFF2563EB,
                        R.drawable.msg_contacts,
                        name,
                        sub,
                        a == currentAccount ? "Joriy" : "O‘tish",
                        AeroRowCell.BADGE_NONE
                    ));
                }
                items.add(UItem.asShadow("Ro‘yxatdagi istalgan akkauntni bosib darhol o‘sha akkauntga o‘tishingiz mumkin."));
                break;

            case MODE_STATS:
                items.add(UItem.asHeader("Qurilmadagi AeroGram statistikasi"));
                items.add(UItem.asButton(0, R.drawable.msg_delete, "Saqlangan o‘chirilgan xabarlar", AeroDeletedStore.getTotalSavedCount() + " ta"));
                items.add(UItem.asButton(0, R.drawable.msg_recent, "Saqlangan tahrir versiyalari", AeroEditHistoryStore.getTotalCount() + " ta"));
                items.add(UItem.asButton(0, R.drawable.msg_block2, "Filtrlangan xorijiy xabarlar", AeroConfig.statBlockedForeignCount + " ta"));
                items.add(UItem.asButton(0, R.drawable.msg_stealth_locked, "Ghost Mode holati", AeroConfig.isAnyGhostActive() ? "Yoqiq" : "O‘chiq"));
                items.add(UItem.asButton(0, R.drawable.msg_contacts, "Faol akkauntlar soni", UserConfig.getActivatedAccountsCount() + " ta"));
                items.add(UItem.asShadow("Barcha ko‘rsatkichlar faqat sizning qurilmangizdagi haqiqiy lokal holatdan olinadi."));
                break;

            case MODE_GHOST:
                items.add(UItem.asHeader("Tezkor boshqaruv"));
                items.add(UItem.asButtonCheck(
                    ID_GHOST_MASTER,
                    "Ghost Mode (Hammasini yoqish/o‘chirish)",
                    "O‘qish, yozish, onlayn va story ko‘rishni birdaniga boshqaradi"
                ).setChecked(AeroConfig.isAnyGhostActive()));
                items.add(UItem.asShadow(null));
                items.add(UItem.asHeader("Batafsil sozlamalar"));
                items.add(UItem.asButtonCheck(ID_GHOST_READ, "O‘qilganlik belgisini yubormaslik", "Xabarni ochsangiz ham 2 galochka ketmaydi").setChecked(AeroConfig.ghostDontReadMessages));
                items.add(UItem.asButtonCheck(ID_GHOST_TYPING, "Yozayotganini yashirish", "\"yozmoqda...\" va \"ovoz yozmoqda...\" yuborilmaydi").setChecked(AeroConfig.ghostDontSendTyping));
                items.add(UItem.asButtonCheck(ID_GHOST_OFFLINE, "Doimiy Offline rejimi", "Ilovada bo‘lsangiz ham status offline turadi").setChecked(AeroConfig.ghostStayOffline));
                items.add(UItem.asButtonCheck(ID_GHOST_READ_ON_SEND, "Javob yozganda o‘qilgan qilish", "Chatga xabar jo‘natganingizdagina o‘qilgan bo‘ladi").setChecked(AeroConfig.ghostReadOnInteract));
                items.add(UItem.asButtonCheck(ID_GHOST_STORY, "Story ko‘rishni yashirish", "Hikoyalarni anonim ko‘rish").setChecked(AeroConfig.ghostHideStoryViews));
                break;

            case MODE_TEXT_STYLE:
                items.add(UItem.asHeader("Yuboriladigan matn uslubi"));
                String[] styleTitles = new String[] {
                    "Oddiy (uslubsiz)",
                    "Qalin (Bold)",
                    "Kursiv (Italic)",
                    "Monoshirin (Kod)",
                    "Ustiga chizilgan (Strikethrough)",
                    "Tagiga chizilgan (Underline)",
                    "Yashirin matn (Spoiler)"
                };
                for (int i = 0; i < styleTitles.length; i++) {
                    items.add(UItem.asRadio(ID_STYLE_BASE + i, styleTitles[i]).setChecked(AeroConfig.textStyleMode == i));
                }
                items.add(UItem.asShadow("Tanlangan uslub barcha yangi yuboriladigan matnli xabarlarga avtomatik qo‘llanadi."));
                break;

            case MODE_POSTFIX:
                items.add(UItem.asHeader("Avto-postfiks"));
                items.add(UItem.asCheck(ID_POSTFIX_TOGGLE, "Har bir xabar oxiriga matn qo‘shish").setChecked(AeroConfig.autoPostfixEnabled));
                items.add(UItem.asHeader("Qo‘shimcha matn"));
                if (ctx != null) {
                    items.add(UItem.asCustom(makeInputBoxCard(ctx, "Masalan: • @AeroGram", AeroConfig.autoPostfixText, txt -> {
                        AeroConfig.autoPostfixText = txt;
                        AeroConfig.saveString("autoPostfixText", txt);
                    })));
                }
                items.add(UItem.asShadow("Yoqilganda kiritilgan matn yoki imzo har bir chiqish xabari oxiriga avtomatik qo‘shiladi."));
                break;

            case MODE_NEW_ALPHABET:
                items.add(UItem.asHeader("Yangi O‘zbek alifbosi (ş ç ō ḡ)"));
                items.add(UItem.asShadow("Sh -> Ş, Ch -> Ç, O‘ -> Ō, G‘ -> Ḡ ko‘rinishida yozish. Klaviatura almashtirmasdan oddiy yozsangiz, yuborilganda avtomatik o‘giriladi."));
                items.add(UItem.asCheck(ID_NEW_ALPHABET_TOGGLE, "Yangi alifboda yuborish").setChecked(AeroConfig.newUzbekAlphabet));
                items.add(AeroRowCell.Factory.ofNav(ID_NEW_ALPHABET_CONVERT_DIALOG, 0xFF0EA5E9, R.drawable.msg_edit, "Matnni sinab ko‘rish / o‘girish", "Oddiy lotindan yangi alifboga", null, AeroRowCell.BADGE_NONE));
                items.add(UItem.asShadow("O‘girish qurilmangizda mahalliy bajariladi."));
                break;

            case MODE_TRANSLATE_LANG:
                items.add(UItem.asHeader("Tarjima tili"));
                for (int i = 0; i < LANG_CODES.length; i++) {
                    items.add(UItem.asRadio(ID_LANG_BASE + i, LANG_NAMES[i]).setChecked(LANG_CODES[i].equals(AeroConfig.translateTargetLang)));
                }
                break;
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_GHOST_MASTER) {
            AeroConfig.toggleMasterGhost(!AeroConfig.isAnyGhostActive());
            MessagesController.getInstance(currentAccount).updateTimerProc();
        } else if (item.id == ID_GHOST_READ) {
            AeroConfig.ghostDontReadMessages = !AeroConfig.ghostDontReadMessages;
            AeroConfig.saveBool("ghostDontReadMessages", AeroConfig.ghostDontReadMessages);
        } else if (item.id == ID_GHOST_TYPING) {
            AeroConfig.ghostDontSendTyping = !AeroConfig.ghostDontSendTyping;
            AeroConfig.saveBool("ghostDontSendTyping", AeroConfig.ghostDontSendTyping);
        } else if (item.id == ID_GHOST_OFFLINE) {
            AeroConfig.ghostStayOffline = !AeroConfig.ghostStayOffline;
            AeroConfig.saveBool("ghostStayOffline", AeroConfig.ghostStayOffline);
            MessagesController.getInstance(currentAccount).updateTimerProc();
        } else if (item.id == ID_GHOST_READ_ON_SEND) {
            AeroConfig.ghostReadOnInteract = !AeroConfig.ghostReadOnInteract;
            AeroConfig.saveBool("ghostReadOnInteract", AeroConfig.ghostReadOnInteract);
        } else if (item.id == ID_GHOST_STORY) {
            AeroConfig.ghostHideStoryViews = !AeroConfig.ghostHideStoryViews;
            AeroConfig.saveBool("ghostHideStoryViews", AeroConfig.ghostHideStoryViews);
        } else if (item.id == ID_POSTFIX_TOGGLE) {
            AeroConfig.autoPostfixEnabled = !AeroConfig.autoPostfixEnabled;
            AeroConfig.saveBool("autoPostfixEnabled", AeroConfig.autoPostfixEnabled);
        } else if (item.id == ID_NEW_ALPHABET_TOGGLE) {
            AeroConfig.newUzbekAlphabet = !AeroConfig.newUzbekAlphabet;
            AeroConfig.saveBool("newUzbekAlphabet", AeroConfig.newUzbekAlphabet);
        } else if (item.id == ID_NEW_ALPHABET_CONVERT_DIALOG) {
            showAlphabetPreviewDialog();
            return;
        } else if (item.id >= ID_STYLE_BASE && item.id < ID_STYLE_BASE + 10) {
            AeroConfig.textStyleMode = item.id - ID_STYLE_BASE;
            AeroConfig.saveInt("textStyleMode", AeroConfig.textStyleMode);
        } else if (item.id >= ID_LANG_BASE && item.id < ID_LANG_BASE + LANG_CODES.length) {
            AeroConfig.translateTargetLang = LANG_CODES[item.id - ID_LANG_BASE];
            AeroConfig.saveString("translateTargetLang", AeroConfig.translateTargetLang);
        } else if (item.id >= ID_ACCOUNT_BASE && item.id < ID_ACCOUNT_BASE + UserConfig.MAX_ACCOUNT_COUNT) {
            int acc = item.id - ID_ACCOUNT_BASE;
            if (acc != currentAccount && getParentActivity() instanceof LaunchActivity) {
                ((LaunchActivity) getParentActivity()).switchToAccount(acc, true);
            }
            return;
        } else {
            return;
        }
        listView.adapter.update(true);
    }

    private void showAlphabetPreviewDialog() {
        Context ctx = getParentActivity();
        if (ctx == null) return;
        LinearLayout ll = new LinearLayout(ctx);
        ll.setOrientation(LinearLayout.VERTICAL);
        int p = dp(18);
        ll.setPadding(p, dp(10), p, dp(10));

        EditText input = new EditText(ctx);
        input.setHint("Masalan: O'zbekiston chiroyli shahar!");
        ll.addView(input);

        TextView out = new TextView(ctx);
        out.setPadding(0, dp(10), 0, 0);
        out.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        out.setTextIsSelectable(true);
        out.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText, getResourceProvider()));
        ll.addView(out);

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                out.setText(AeroTextUtils.convertNewUzbekAlphabet(s != null ? s.toString() : ""));
            }
        });

        new org.telegram.ui.ActionBar.AlertDialog.Builder(ctx)
            .setTitle("Yangi alifbo konverteri")
            .setView(ll)
            .setPositiveButton(LocaleController.getString(R.string.Copy), (d, w) -> {
                String res = out.getText().toString();
                if (!TextUtils.isEmpty(res)) {
                    AndroidUtilities.addToClipboard(res);
                    BulletinFactory.of(this).createCopyBulletin("Nusxalandi").show();
                }
            })
            .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
            .show();
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
