package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;

/**
 * Renders the AeroGram icon mark followed by the bold "AeroGram" wordmark.
 * Supports ColorFilter and alpha seamlessly inside DialogStoriesCell, DialogsActivity, and IntroActivity.
 */
public class AeroLogoDrawable extends Drawable {

    private final Drawable icon;
    private final TextPaint textPaint;
    private final int iconSize;
    private final int gap;
    private final String label;
    private final int totalWidth;
    private final int totalHeight;
    private int currentAlpha = 255;

    public AeroLogoDrawable(Context context) {
        this(context, 21, 19.5f);
    }

    public AeroLogoDrawable(Context context, int iconSizeDp, float textSizeDp) {
        this.label = "AeroGram";
        this.iconSize = dp(iconSizeDp);
        this.gap = dp(6);
        this.icon = context.getResources().getDrawable(R.drawable.ic_aerogram_monochrome).mutate();
        this.textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        this.textPaint.setColor(0xFFFFFFFF);
        this.textPaint.setTypeface(AndroidUtilities.bold());
        this.textPaint.setTextSize(dp(textSizeDp));

        int textWidth = (int) Math.ceil(this.textPaint.measureText(this.label));
        this.totalWidth = this.iconSize + this.gap + textWidth + dp(2);
        this.totalHeight = Math.max(this.iconSize, dp(24));
        setBounds(0, 0, this.totalWidth, this.totalHeight);
    }

    @Override
    public int getIntrinsicWidth() {
        return totalWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return totalHeight;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        int left = bounds.left;
        int centerY = bounds.centerY();

        if (icon != null) {
            int iconTop = centerY - iconSize / 2;
            icon.setBounds(left, iconTop, left + iconSize, iconTop + iconSize);
            icon.setAlpha(currentAlpha);
            icon.draw(canvas);
        }

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float baseline = centerY - (fm.ascent + fm.descent) / 2f;
        textPaint.setAlpha(currentAlpha);
        canvas.drawText(label, left + iconSize + gap, baseline, textPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        currentAlpha = alpha;
        if (icon != null) {
            icon.setAlpha(alpha);
        }
        textPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        if (icon != null) {
            icon.setColorFilter(colorFilter);
        }
        textPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
