package com.paranoiaworks.unicus.android.sse.components;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.text.Editable;
import android.util.AttributeSet;
import android.widget.EditText;

import com.paranoiaworks.unicus.android.sse.CryptActivity;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;

import java.nio.CharBuffer;
import java.util.Arrays;

/**
 * Suppress Autofill + Set/Get Text as char[]
 *
 * @author Paranoia Works
 * @version 1.1.2
 */

@SuppressLint("AppCompatCustomView")
public class SecureEditText extends EditText {

    private boolean autofillEnabled = false;

    public SecureEditText(Context context) {
        super(context);
        setAutoFill(context);
    }

    public SecureEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAutoFill(context);
    }

    public SecureEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setAutoFill(context);
    }

    public char[] toCharArray()
    {
        Editable currentText = this.getText();
        char[] chArray = new char[currentText.length()];
        currentText.getChars(0, currentText.length(), chArray, 0);
        return chArray;
    }

    public char[] toLowerCaseCharArray()
    {
        char[] chArray = toCharArray();
        for(int i = 0; i < chArray.length; ++i)
            chArray[i] = Character.toLowerCase(chArray[i]);
        return chArray;
    }

    public char[] getPassword()
    {
        return toCharArray();
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

    private void setAutoFill(Context context)
    {
        Activity a = Helpers.getActivityFromContext(context);
        if(a != null && a instanceof CryptActivity)
            autofillEnabled = ((CryptActivity)a).getSettingDataHolder().getItemAsBoolean("SC_Common", "SI_AllowExternalAutofill");
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SecureEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public int getAutofillType() {
        if(autofillEnabled) return super.getAutofillType();
        else return AUTOFILL_TYPE_NONE;
    }
}
