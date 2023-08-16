package com.paranoiaworks.unicus.android.sse.components;

import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.WindowManager;

import com.paranoiaworks.unicus.android.sse.CryptActivity;
import com.paranoiaworks.unicus.android.sse.dao.SettingDataHolder;

/**
 * Dialog with screen-shot taking protection
 * 
 * @author Paranoia Works
 * @version 1.0.1
 */

public class SecureDialog extends Dialog {
	
	private boolean protectionActive = true;
	protected SettingDataHolder settingDataHolder;

	public SecureDialog(Context context) {
		super(context);
		setSDH(context);
		setProtection();
	}

	protected SecureDialog(Context context, boolean cancelable,
			OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);
		setSDH(context);
		setProtection();
	}
	
	public SecureDialog(Context context, int themeResId) {
		super(context, themeResId);
		setSDH(context);
		setProtection();
	}

	private void setSDH(Context context)
	{
		try {
			CryptActivity a = getActivityFromContext(context);
			settingDataHolder = a.getSettingDataHolder();
		} catch (Exception e) {
			// swallow
		}
	}

	private void setProtection()
	{
		try {
			protectionActive = settingDataHolder.getItemAsBoolean("SC_Common", "SI_PreventScreenshots");
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(protectionActive)
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
	}

	protected static CryptActivity getActivityFromContext(Context context)
	{
		while (!(context instanceof CryptActivity))
		{
			if (!(context instanceof ContextWrapper)) {
				context = null;
			}
			ContextWrapper contextWrapper = (ContextWrapper) context;
			if (contextWrapper == null) {
				return null;
			}
			context = contextWrapper.getBaseContext();
			if (context == null) {
				return null;
			}
		}
		return (CryptActivity) context;
	}
}
