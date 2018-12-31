package com.monke.monkeybook.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.core.view.ViewCompat;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.monke.monkeybook.R;

public class ScrimInsetsLinearLayout extends LinearLayout {
    private Drawable mInsetForeground;
    private boolean mConsumeInsets;

    private Rect mInsets;
    private Rect mTempRect = new Rect();

    private OnInsetsCallback mOnInsetsCallback;

    public ScrimInsetsLinearLayout(Context context) {
        super(context);
        init(context, null, 0);
    }

    public ScrimInsetsLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public ScrimInsetsLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        final TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.ScrimInsetsLinearLayout, defStyle, 0);
        if (a == null) {
            return;
        }
        mInsetForeground = a.getDrawable(R.styleable.ScrimInsetsLinearLayout_appInsetForeground);
        mConsumeInsets = a.getBoolean(R.styleable.ScrimInsetsLinearLayout_appConsumeInsets, true);
        a.recycle();

        setWillNotDraw(true);

        ViewCompat.setOnApplyWindowInsetsListener(this, (v, insets) -> {
            if (!mConsumeInsets) {
                if(mOnInsetsCallback != null){
                    mOnInsetsCallback.onInsetsChanged(new Rect(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()));
                }
                return insets.consumeSystemWindowInsets();
            }

            if (null == ScrimInsetsLinearLayout.this.mInsets) {
                ScrimInsetsLinearLayout.this.mInsets = new Rect();
            }

            ScrimInsetsLinearLayout.this.mInsets.set(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            ScrimInsetsLinearLayout.this.onInsetsChanged(ScrimInsetsLinearLayout.this.mInsets);
            ScrimInsetsLinearLayout.this.setWillNotDraw(!insets.hasSystemWindowInsets() || ScrimInsetsLinearLayout.this.mInsetForeground == null);
            ViewCompat.postInvalidateOnAnimation(ScrimInsetsLinearLayout.this);
            return insets.consumeSystemWindowInsets();
        });
    }


    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (mInsets != null && mInsetForeground != null) {
            int sc = canvas.save();
            canvas.translate(getScrollX(), getScrollY());

            // Top
            mTempRect.set(0, 0, width, mInsets.top);
            mInsetForeground.setBounds(mTempRect);
            mInsetForeground.draw(canvas);

            // Bottom
            mTempRect.set(0, height - mInsets.bottom, width, height);
            mInsetForeground.setBounds(mTempRect);
            mInsetForeground.draw(canvas);

            // Left
            mTempRect.set(0, mInsets.top, mInsets.left, height - mInsets.bottom);
            mInsetForeground.setBounds(mTempRect);
            mInsetForeground.draw(canvas);

            // Right
            mTempRect.set(width - mInsets.right, mInsets.top, width, height - mInsets.bottom);
            mInsetForeground.setBounds(mTempRect);
            mInsetForeground.draw(canvas);

            canvas.restoreToCount(sc);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mInsetForeground != null) {
            mInsetForeground.setCallback(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mInsetForeground != null) {
            mInsetForeground.setCallback(null);
        }
    }


    protected void onInsetsChanged(Rect insets){
        if(mOnInsetsCallback != null){
            mOnInsetsCallback.onInsetsChanged(insets);
        }

        if(!mConsumeInsets){
            return;
        }

        setPadding(0, insets.top, 0, insets.bottom);
    }

    public void applyWindowInsets(Rect insets){
        if(insets == null) return;
        if (null == ScrimInsetsLinearLayout.this.mInsets) {
            ScrimInsetsLinearLayout.this.mInsets = new Rect();
        }

        ScrimInsetsLinearLayout.this.mInsets.set(insets.left, insets.top, insets.right, insets.bottom);
        ScrimInsetsLinearLayout.this.onInsetsChanged(insets);
        ScrimInsetsLinearLayout.this.setWillNotDraw(ScrimInsetsLinearLayout.this.mInsetForeground == null);
        ViewCompat.postInvalidateOnAnimation(ScrimInsetsLinearLayout.this);
    }

    /**
     * Allows the calling container to specify a callback for custom processing when insets change (i.e. when
     * {@link #fitSystemWindows(Rect)} is called. This is useful for setting padding on UI elements based on
     * UI chrome insets (e.g. a Google Map or a ListView). When using with ListView or GridView, remember to set
     * clipToPadding to false.
     */
    public void setOnInsetsCallback(OnInsetsCallback onInsetsCallback) {
        mOnInsetsCallback = onInsetsCallback;
    }

    public interface OnInsetsCallback {
        void onInsetsChanged(Rect insets);
    }
}
