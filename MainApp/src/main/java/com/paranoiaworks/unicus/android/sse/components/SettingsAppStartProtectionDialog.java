package com.paranoiaworks.unicus.android.sse.components;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

import com.lambdaworks.crypto.SCrypt;
import com.paranoiaworks.android.sse.interfaces.SettingsCheckBoxCustom;
import com.paranoiaworks.unicus.android.sse.R;
import com.paranoiaworks.unicus.android.sse.config.ScryptParams;
import com.paranoiaworks.unicus.android.sse.dao.SettingDataHolder;
import com.paranoiaworks.unicus.android.sse.utils.Encryptor;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Custom CheckBox Dialog (extended settings)
 *
 * @author Paranoia Works
 * @version 1.0.8
 */
public class SettingsAppStartProtectionDialog extends SecureDialog implements SettingsCheckBoxCustom {

	private Activity context;
	private int dialogMode = -1;
	
	private SecureEditText passwordEditText1;
	private SecureEditText passwordEditText2;
	private CheckBox passCB;
	private Button okButton;
	private Button cancelButton;
	private Object tag;
	private Handler handler;

	private boolean blockCancellation = false;
	private boolean buttonsBlock = false;
	
	public final static int SASPD_MODE_DISABLE_PROTECTION = 1;
	public final static int SASPD_MODE_ENABLE_PROTECTION = 2;
	
	public final static String PERSISTENT_DATA_OBJECT_LAUNCH_PASSWORD = "PERSISTENT_DATA_OBJECT_LAUNCH_PASSWORD";
	
	public SettingsAppStartProtectionDialog(View v, Handler handler) 
	{
		this(getActivityFromContext(v.getContext()), handler);
	}	
	
	public SettingsAppStartProtectionDialog(Activity context, Handler handler) 
	{
		super(context);
		this.context = context;
		this.handler = handler;
	}
	
	public void setCustomTitle(String customTitle)
	{
		this.setTitle(customTitle);
	}	
	
	public void setTag(Object tag)
	{
		this.tag = tag;
	}	
	
	public Object getTag()
	{
		return this.tag;
	}
	
	@Override
	public void doOnCheck() {
		this.dialogMode = SASPD_MODE_ENABLE_PROTECTION;
		this.init();
		super.show();
	}

	@Override
	public void doOnUncheck() {
		this.dialogMode = SASPD_MODE_DISABLE_PROTECTION;
		this.init();	
		super.show();
	}

	public static boolean checkConsistency(SettingDataHolder settingDataHolder) {
		List<byte[]> passwordPackage = new ArrayList<byte[]>();
		passwordPackage = (List<byte[]>)settingDataHolder.getPersistentDataObject(PERSISTENT_DATA_OBJECT_LAUNCH_PASSWORD);
		return (passwordPackage != null) ? true : false;
	}
	
	@Override
	public void show(){
		// not used
	}
	
	private void init()
	{
		this.setContentView(R.layout.lc_settings_app_start_protection_dialog);
		this.setCancelable(false);
		this.setCanceledOnTouchOutside(false);

    	passwordEditText1 = (SecureEditText)this.findViewById(R.id.SASPD_passwordEditText1);
    	passwordEditText2 = (SecureEditText)this.findViewById(R.id.SASPD_passwordEditText2);
    	cancelButton = (Button)this.findViewById(R.id.SASPD_cancelButton);
    	okButton = (Button)this.findViewById(R.id.SASPD_okButton);
    	passCB = (CheckBox)this.findViewById(R.id.SASPD_passwordCheckBox);
    	
    	passwordEditText1.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    	passwordEditText1.setTransformationMethod(new PasswordTransformationMethod());
    	passwordEditText2.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    	passwordEditText2.setTransformationMethod(new PasswordTransformationMethod());

    	// prepare layout for mode
    	switch (dialogMode) 
        {        
        	case SASPD_MODE_DISABLE_PROTECTION:
        	{
        		passwordEditText1.setHint("");
        		passwordEditText2.setVisibility(EditText.GONE);
        		this.setTitle(Helpers.capitalizeFirstLetter(context.getResources().getString(R.string.passwordDialog_oldPasswordHint)));
        		break;
        	}    		
        	case SASPD_MODE_ENABLE_PROTECTION:
        	{
        		this.setTitle(context.getResources().getString(R.string.passwordDialog_title_set));  
        		break;
        	}
        	default:
        		throw new IllegalArgumentException("unknown mode");
        }
    	   	  	
    	if (dialogMode != SASPD_MODE_DISABLE_PROTECTION)
    	{
	    	passwordEditText1.addTextChangedListener((new TextWatcher()
	    	{
	            public void afterTextChanged (Editable s)
	            {
	            }
	            public void beforeTextChanged(CharSequence s, int start, int count, int after)
	            {
	            }
	            public void onTextChanged(CharSequence s, int start, int before, int count)
	            {
					char[] tempPass = passwordEditText1.getPassword();
					if(passCB.isChecked()) passwordEditText2.setCharArray(tempPass);
					Arrays.fill(tempPass, '\u0000');
	            }
	    	}));
    	}

    	
    	// OK Button
    	okButton.setOnClickListener(new android.view.View.OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	if(buttonsBlock) return;

				char[] P1 = passwordEditText1.getPassword();
				P1 = Helpers.trim(P1);

				char[] P2 = passwordEditText2.getPassword();
				P2 = Helpers.trim(P2);

				try {
					if(P1.length < 1)
					{
						new ImageToast(context.getResources().getString(R.string.passwordDialog_noPassword),
								ImageToast.TOAST_IMAGE_CANCEL, context).show();
						throw new IllegalArgumentException();
					}
					if(dialogMode == SASPD_MODE_ENABLE_PROTECTION)
					{
						if(!Arrays.equals(P1, P2))
						{
							new ImageToast(context.getResources().getString(R.string.passwordDialog_passwordNotMatch),
									ImageToast.TOAST_IMAGE_CANCEL, context).show();
							throw new IllegalArgumentException();
						}
					}
					else if((dialogMode == SASPD_MODE_DISABLE_PROTECTION))
					{
						List<byte[]> passwordPackage = (List<byte[]>)settingDataHolder.getPersistentDataObject(PERSISTENT_DATA_OBJECT_LAUNCH_PASSWORD);

						if(passwordPackage != null)
						{
							byte[] hashedPassword = getPasswordDerivative(P1, passwordPackage.get(1));
							if(!Helpers.isEqualTimeConstant(hashedPassword, passwordPackage.get(0)))
							{
								passwordEditText1.setText("");
								new ImageToast(context.getResources().getString(R.string.passwordDialog_invalidCurrentPassword),
										ImageToast.TOAST_IMAGE_CANCEL, context).show();
								throw new IllegalArgumentException();
							}
						}
					}
				} catch (IllegalArgumentException e) {
					Arrays.fill(P1, '\u0000');
					Arrays.fill(P2, '\u0000');
					return;
				}

				buttonsBlock = true;
		    	
		    	if(dialogMode == SASPD_MODE_ENABLE_PROTECTION) 
		    	{
		    		byte[] salt = Encryptor.getRandomBA(64);
		    		byte[] hashedPassword = getPasswordDerivative(P1, salt);
					byte[] hashedPasswordVerification = getPasswordDerivative(P1, salt);

					if(!Helpers.isEqualTimeConstant(hashedPassword, hashedPasswordVerification)) throw new IllegalStateException("Impossible Hardware Failure");
		    		
		    		List<byte[]> passwordPackage = new ArrayList<byte[]>();
		    		passwordPackage.add(hashedPassword);
		    		passwordPackage.add(salt);

					settingDataHolder.addOrReplacePersistentDataObject(PERSISTENT_DATA_OBJECT_LAUNCH_PASSWORD, passwordPackage);
					settingDataHolder.save();
		    	}
		    	else if(dialogMode == SASPD_MODE_DISABLE_PROTECTION)
		    	{
		    		//sdh.addOrReplacePersistentDataObject(PERSISTENT_DATA_OBJECT_LAUNCH_PASSWORD, null);
		    		//sdh.save();
		    	}
				
		    	handler.sendMessage(Message.obtain(handler, SettingsCheckBoxCustom.OK));

				Arrays.fill(P1, '\u0000');
		    	Arrays.fill(P2, '\u0000');

		    	cancel();
		    }
	    });
    	
    	
    	// Cancel Button
    	cancelButton.setOnClickListener(new android.view.View.OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	if(!blockCancellation) 
		    	{
		    		handler.sendMessage(Message.obtain(handler, SettingsCheckBoxCustom.CANCEL, tag));
		    		cancel();
		    	}
		    	else {
	        		Toast.makeText(context, context.getResources().getString(R.string.passwordDialog_cannotCancel), 
	        				Toast.LENGTH_SHORT).show();
		    	}	
		    }
	    });

    	
    	// CheckBox Show Password
    	passCB.setText("  " + context.getResources().getString(R.string.passwordDialog_showPassword));
    	passCB.setOnCheckedChangeListener(new OnCheckedChangeListener()
    	{
    	    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    	    {
    	        if (isChecked)
    	        {
					char[] tempPass = passwordEditText1.getPassword();
    	        	passwordEditText2.setFocusable(false);
    	        	passwordEditText2.setFocusableInTouchMode(false);
    	        	passwordEditText2.setEnabled(false);
    	        	passwordEditText1.setTransformationMethod(null);
					passwordEditText2.setCharArray(tempPass);
    	        	if(passwordEditText1.length() > 0) passwordEditText1.setSelection(passwordEditText1.length());
					Arrays.fill(tempPass, '\u0000');
    	        } else {
    	        	passwordEditText2.setFocusable(true);
    	        	passwordEditText2.setFocusableInTouchMode(true);
    	        	passwordEditText2.setEnabled(true);
    	        	passwordEditText1.setTransformationMethod(new PasswordTransformationMethod());
    	        	passwordEditText1.setSelection(passwordEditText1.length());
    	        }
    	    }
    	});

		passwordEditText1.requestFocus();
    	this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	}
	
	/** Get password derivative */
    public static byte[] getPasswordDerivative(char[] password, byte[] salt)
    {
    	password = Encryptor.convertToCodePoints(password);
    	
    	ScryptParams sp = ScryptParams.getParameters(ScryptParams.APP_CODE_AUTHENTICATION, 2);   	
    	int dkLen = 64;   	
    	
    	byte[] output = null;
    	try {
			byte[] passwordB = Helpers.toBytes(password, true);
    		output = SCrypt.scrypt(Encryptor.getSHA3Hash(passwordB, 512), salt, sp.getN(), sp.getR(), sp.getP(), dkLen);
			Arrays.fill(passwordB, (byte) 0);
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		}

		Arrays.fill(password, '\u0000');

		return output;
    }
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SEARCH) {            
            return true;
        } else return super.onKeyDown(keyCode, event);
    }
}
