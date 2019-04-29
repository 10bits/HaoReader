package com.monke.monkeybook.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;

import androidx.appcompat.widget.ListPopupWindow;

import android.text.InputType;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ArrayAdapter;

import com.google.android.material.textfield.TextInputEditText;
import com.monke.monkeybook.R;
import com.monke.monkeybook.utils.KeyboardUtil;
import com.monke.monkeybook.utils.ScreenUtils;

public class TextInputSpinner extends TextInputEditText {

    CharSequence[] mEntities;
    ListPopupWindow mPopup;

    public TextInputSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    @SuppressLint("PrivateResource")
    public TextInputSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    @SuppressLint("RestrictedApi")
    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        setInputType(InputType.TYPE_NULL);
        setCursorVisible(false);
        Drawable arrow = getResources().getDrawable(R.drawable.ic_arrow_drop_down_black_24dp);
        arrow.setBounds(0, 0, arrow.getIntrinsicWidth(), arrow.getIntrinsicHeight());
        setCompoundDrawablesRelative(null, null, arrow, null);
        AppCompat.setTint(arrow, getResources().getColor(R.color.colorMenuText));

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.TextInputSpinner, defStyleAttr, 0);
        mEntities = a.getTextArray(R.styleable.TextInputSpinner_android_entries);
        a.recycle();

        mPopup = new ListPopupWindow(context, attrs, defStyleAttr, R.style.AppTheme_PopupMenu_Overflow);
        mPopup.setAdapter(new ArrayAdapter<>(context, R.layout.support_simple_spinner_dropdown_item, mEntities));
        mPopup.setOnItemClickListener((parent, view, position, id) -> {
            setSelection(position);
            mPopup.dismiss();
        });
        mPopup.setAnchorView(this);
        mPopup.setVerticalOffset(-ScreenUtils.dpToPx(5));
        mPopup.setHorizontalOffset(ScreenUtils.dpToPx(4));
        mPopup.setWidth(getResources().getDisplayMetrics().widthPixels - ScreenUtils.dpToPx(16));
        mPopup.setModal(true);

        setSelection(0);
    }

    public void setSelection(int position) {
        if (mEntities != null && mEntities.length > 0 && position >= 0 && position < mEntities.length) {
            setText(mEntities[position]);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean touch = super.onTouchEvent(event);
        post(() -> {
            TextInputSpinner.this.requestFocus();
            KeyboardUtil.hideKeyboard(TextInputSpinner.this);
        });

        if(event.getAction()== MotionEvent.ACTION_DOWN){
            if (!mPopup.isShowing()) {
                mPopup.show();
            }
        }
        return touch;
    }
}
