package com.paranoiaworks.unicus.android.sse.components;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.text.Editable;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.nio.CharBuffer;
import java.util.Arrays;

/**
 * Set/Get Text as char[]
 *
 * @author Paranoia Works
 * @version 1.0.0
 */

@SuppressLint("AppCompatCustomView")
public class TextViewMod extends TextView {
    public TextViewMod(Context context) {
        super(context);
    }

    public TextViewMod(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TextViewMod(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TextViewMod(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public char[] toCharArray()
    {
        CharSequence currentText = this.getText();
        char[] chArray = new char[currentText.length()];
        for(int i=0; i < currentText.length(); ++i) {
            chArray[i] = currentText.charAt(i);
        }
        return chArray;
    }

    public void setCharArray(char[] text)
    {
        setText(CharBuffer.wrap(text));
    }

    public void setCharArrayAndWipe(char[] text)
    {
        setText(CharBuffer.wrap(text));
        Arrays.fill(text, '\u0000');
    }
}
