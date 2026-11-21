package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;

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

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.Switch;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.HashSet;

/**
 * Native AeroGram Settings row matching NovaGram's clean card row styling:
 * rounded-square colored icon badge on the left, title + optional YANGI/AERO pill + subtitle in the center,
 * and either a native Telegram Switch or right value + chevron arrow on the right.
 */
public class AeroRowCell extends LinearLayout implements Theme.Colorable {

    public static final int FLAG_SWITCH = 1;
    public static final int BADGE_NONE = 0;
    public static final int BADGE_NEW = 0x10;   // Green "YANGI" badge
    public static final int BADGE_AERO = 0x20;  // Blue "AERO" badge

    private final Theme.ResourcesProvider resourcesProvider;
    private final FrameLayout iconBox;
    private final GradientDrawable iconBg;
    private final ImageView iconView;
    private final LinearLayout textColumn;
    private final LinearLayout titleRow;
    private final TextView titleView;
    private final TextView badgeView;
    private final GradientDrawable badgeBg;
    private final TextView subtitleView;
    private final TextView valueView;
    private final ImageView chevronView;
    private final Switch switchView;

    private boolean twoLines;
    private int currentId = -1;

    public AeroRowCell(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(dp(16), dp(8), dp(16), dp(8));

        iconBg = new GradientDrawable();
        iconBg.setCornerRadius(dp(9));

        iconBox = new FrameLayout(context);
        iconBox.setBackground(iconBg);

        iconView = new ImageView(context);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconView.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        iconBox.addView(iconView, LayoutHelper.createFrame(18, 18, Gravity.CENTER));

        textColumn = new LinearLayout(context);
        textColumn.setOrientation(VERTICAL);

        titleRow = new LinearLayout(context);
        titleRow.setOrientation(HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15.5f);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setMaxLines(1);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        titleRow.addView(titleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 1f));

        badgeBg = new GradientDrawable();
        badgeBg.setCornerRadius(dp(5));

        badgeView = new TextView(context);
        badgeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9.5f);
        badgeView.setTypeface(AndroidUtilities.bold());
        badgeView.setTextColor(Color.WHITE);
        badgeView.setPadding(dp(5), dp(1.5f), dp(5), dp(1.5f));
        badgeView.setBackground(badgeBg);
        badgeView.setVisibility(GONE);
        titleRow.addView(badgeView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0));
        ((LinearLayout.LayoutParams) badgeView.getLayoutParams()).leftMargin = dp(6);

        textColumn.addView(titleRow, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        subtitleView = new TextView(context);
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        subtitleView.setMaxLines(2);
        subtitleView.setEllipsize(TextUtils.TruncateAt.END);
        textColumn.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        valueView = new TextView(context);
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.5f);
        valueView.setMaxLines(1);
        valueView.setEllipsize(TextUtils.TruncateAt.END);
        valueView.setVisibility(GONE);

        chevronView = new ImageView(context);
        chevronView.setImageResource(R.drawable.msg_arrowright);
        chevronView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        chevronView.setVisibility(GONE);

        switchView = new Switch(context, resourcesProvider);
        switchView.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
        switchView.setFocusable(false);
        switchView.setClickable(false);
        switchView.setVisibility(GONE);

        addView(iconBox, LayoutHelper.createLinear(30, 30, Gravity.CENTER_VERTICAL, 0, 0, 14, 0));
        addView(textColumn, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL, 0, 0, 10, 0));
        addView(valueView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 0, 0, 6, 0));
        addView(chevronView, LayoutHelper.createLinear(16, 16, Gravity.CENTER_VERTICAL));
        addView(switchView, LayoutHelper.createLinear(37, 20, Gravity.CENTER_VERTICAL));

        updateColors();
    }

    public void bind(UItem item) {
        boolean sameItem = (currentId == item.id);
        currentId = item.id;

        iconBg.setColor(item.intValue != 0 ? item.intValue : 0xFF0EA5E9);
        iconView.setImageResource(item.iconResId);
        titleView.setText(item.text);

        int badgeType = item.flags & (BADGE_NEW | BADGE_AERO);
        if (badgeType == BADGE_NEW) {
            badgeView.setText("YANGI");
            badgeBg.setColor(0xFF16A34A);
            badgeView.setVisibility(VISIBLE);
        } else if (badgeType == BADGE_AERO) {
            badgeView.setText("AERO");
            badgeBg.setColor(0xFF0284C7);
            badgeView.setVisibility(VISIBLE);
        } else {
            badgeView.setVisibility(GONE);
        }

        twoLines = !TextUtils.isEmpty(item.subtext);
        subtitleView.setVisibility(twoLines ? VISIBLE : GONE);
        subtitleView.setText(item.subtext);

        boolean isSwitch = (item.flags & FLAG_SWITCH) != 0;
        if (isSwitch) {
            switchView.setVisibility(VISIBLE);
            switchView.setChecked(item.checked, sameItem);
            valueView.setVisibility(GONE);
            chevronView.setVisibility(GONE);
        } else {
            switchView.setVisibility(GONE);
            boolean hasValue = !TextUtils.isEmpty(item.textValue);
            valueView.setVisibility(hasValue ? VISIBLE : GONE);
            valueView.setText(item.textValue);
            chevronView.setVisibility(VISIBLE);
        }

        updateColors();
    }

    @Override
    public void updateColors() {
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
        valueView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
        chevronView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider), PorterDuff.Mode.SRC_IN));
    }

    @Override
    public void addColorKeys(HashSet<Integer> colors) {
        colors.add(Theme.key_windowBackgroundWhiteBlackText);
        colors.add(Theme.key_windowBackgroundWhiteGrayText2);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int minH = dp(twoLines ? 58 : 48);
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        );
        if (getMeasuredHeight() < minH) {
            setMeasuredDimension(getMeasuredWidth(), minH);
        }
    }

    public static class Factory extends UItem.UItemFactory<AeroRowCell> {
        static { setup(new Factory()); }

        @Override
        public AeroRowCell createView(Context context, RecyclerListView listView, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            return new AeroRowCell(context, resourcesProvider);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView listView) {
            ((AeroRowCell) view).bind(item);
        }

        @Override
        public boolean equals(UItem a, UItem b) {
            return a.id == b.id && (a.flags & FLAG_SWITCH) == (b.flags & FLAG_SWITCH);
        }

        @Override
        public boolean contentsEquals(UItem a, UItem b) {
            return a.id == b.id
                && a.checked == b.checked
                && a.flags == b.flags
                && a.intValue == b.intValue
                && a.iconResId == b.iconResId
                && TextUtils.equals(a.text, b.text)
                && TextUtils.equals(a.subtext, b.subtext)
                && TextUtils.equals(a.textValue, b.textValue);
        }

        public static UItem ofSwitch(int id, int color, int icon, CharSequence title, CharSequence subtitle, boolean checked, int badge) {
            UItem item = UItem.ofFactory(Factory.class);
            item.id = id;
            item.intValue = color;
            item.iconResId = icon;
            item.text = title;
            item.subtext = subtitle;
            item.checked = checked;
            item.flags = FLAG_SWITCH | badge;
            return item;
        }

        public static UItem ofNav(int id, int color, int icon, CharSequence title, CharSequence subtitle, CharSequence value, int badge) {
            UItem item = UItem.ofFactory(Factory.class);
            item.id = id;
            item.intValue = color;
            item.iconResId = icon;
            item.text = title;
            item.subtext = subtitle;
            item.textValue = value;
            item.flags = badge;
            return item;
        }
    }
}
