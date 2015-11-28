package com.levipayne.quizapp.CustomObjects;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.ArrayList;

/**
 * This class is used to support alternative layouts of radiobuttons within a radio group, since layouts nested inside
 * radio groups breaks functionality. This class allows any group of radiobuttons to be kept track of.
 */
public class CustomRadioGroup extends RadioGroup {
    private ArrayList<RadioButton> mButtons;
    private OnCheckedChangeListener mChangeListener;
    private int mCheckedId;
    private int mCheckedIndex;

    public CustomRadioGroup(Context context) {
        super(context);

        mCheckedId = -1;
        mCheckedIndex = -1;
    }

    public CustomRadioGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        super.setOnCheckedChangeListener(listener);

        mChangeListener = listener;
    }

    @Override
    public int getCheckedRadioButtonId() {
        if (super.getCheckedRadioButtonId() == -1)
            return mCheckedId;
        else
            return super.getCheckedRadioButtonId();
    }

    public void addViews(ArrayList<RadioButton> buttons) {
        this.mButtons = buttons;

        // Set listeners for buttons
        setListeners();
    }

    public void setListeners() {
        for (RadioButton button : mButtons) {
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    ArrayList<RadioButton> copy = new ArrayList<RadioButton>();
                    copy.addAll(mButtons);
                    copy.remove((RadioButton) v);
                    for (RadioButton button : copy) {
                        button.setChecked(false);
                    }

                    mCheckedId = v.getId();
                    mCheckedIndex = mButtons.indexOf((RadioButton)v);

                    // Notify listener
                    if (mChangeListener != null)
                        mChangeListener.onCheckedChanged(CustomRadioGroup.this, mButtons.indexOf((RadioButton)v));
                }
            });
        }
    }

    public int getCheckedIndex() {
        if (mCheckedIndex != -1)
            return mCheckedIndex;
        else {
            return super.indexOfChild(super.findViewById(getCheckedRadioButtonId()));
        }
    }

    public void checkIndex(int index) {
        if (mButtons != null) {
            mButtons.get(index).setChecked(true);
            mButtons.get(index).callOnClick();
        }
        else {
            ((RadioButton)super.getChildAt(index)).setChecked(true);
        }
    }
}
