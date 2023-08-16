package com.paranoiaworks.unicus.android.sse.components;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.text.ClipboardManager;
import android.text.Html;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.paranoiaworks.unicus.android.sse.CryptActivity;
import com.paranoiaworks.unicus.android.sse.R;
import com.paranoiaworks.unicus.android.sse.adapters.BasicListAdapter;
import com.paranoiaworks.unicus.android.sse.dao.ActivityMessage;
import com.paranoiaworks.unicus.android.sse.dao.SettingDataHolder;
import com.paranoiaworks.unicus.android.sse.misc.ExtendedEntropyProvider;
import com.paranoiaworks.unicus.android.sse.utils.ComponentProvider;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;
import com.paranoiaworks.unicus.android.sse.utils.PasswordGenerator;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Password Generator Dialog
 *
 * @author Paranoia Works
 * @version 1.0.13
 * @related PasswordGenerator.java
 */
public class PasswordGeneratorDialog extends SecureDialog {

private final List<String> charsetsList = new ArrayList<String>();
private final List<boolean[]> charsetsConfList = new ArrayList<boolean[]>();
private final List<Object> defaultSettings = new ArrayList<Object>();
private final int DEFAULT_LENGTH = 12;
public static int PGD_HANDLER_COLLECTION_COMPLETED = -1001;
public static int PGD_HANDLER_COLLECTION_INPROGRESS = -1002;

private SpinnerAdapter charsetSA;
private Activity context;
private Spinner charsetS;
private SecureEditText lengthET;
private SecureEditText passwordET;
private SecureEditText customCharSetET;
private Button setButton;
private Button toClipboardButton;
private Button generateButton;
private CheckBox excludeCB;
private LinearLayout customCharSetContainer;
private ExtendedEntropyProvider eep;

private Integer messageCode = null;
private boolean buttonLock = false;

public PasswordGeneratorDialog(View v) {
	this(getActivityFromContext(v.getContext()));
}

public PasswordGeneratorDialog(Activity context) {
	super(context, R.style.Dialog_CustomAlert);
	this.context = context;
	this.init();
}

public PasswordGeneratorDialog(View v, int messageCode) {
	this(getActivityFromContext(v.getContext()), messageCode);
}

public PasswordGeneratorDialog(Activity context, int messageCode) {
	super(context, R.style.Dialog_CustomAlert);
	this.context = context;
	this.messageCode = messageCode;
	this.init();
}

private void init() {
	{
		charsetsList.add("123");
		boolean[] t = {false, false, true, false};
		charsetsConfList.add(t);
	}
	{
		charsetsList.add("abc");
		boolean[] t = {true, false, false, false};
		charsetsConfList.add(t);
	}
	{
		charsetsList.add("ABC");
		boolean[] t = {false, true, false, false};
		charsetsConfList.add(t);
	}
	{
		charsetsList.add("123 + abc");
		boolean[] t = {true, false, true, false};
		charsetsConfList.add(t);
	}
	{
		charsetsList.add("123 + ABC");
		boolean[] t = {false, true, true, false};
		charsetsConfList.add(t);
	}
	{
		charsetsList.add("abc + ABC");
		boolean[] t = {true, true, false, false};
		charsetsConfList.add(t);
	}
	{
		charsetsList.add("123 + abc + ABC");
		boolean[] t = {true, true, true, false};
		charsetsConfList.add(t);
	}
	{
		charsetsList.add("ASCII 33-126");
		boolean[] t = {true, true, true, true};
		charsetsConfList.add(t);
	}

	charsetsList.add(context.getResources().getString(R.string.passwordGeneratorDialog_customCharSet));

	defaultSettings.add(6);
	defaultSettings.add(12);
	defaultSettings.add(true);

	eep = new ExtendedEntropyProvider(context);

	this.setContentView(R.layout.lc_passwordgenerator_dialog);
	this.setCanceledOnTouchOutside(false);
	this.setTitle(context.getResources().getString(R.string.passwordGeneratorDialog_passwordGenerator_text));
	charsetSA = new BasicListAdapter(context, charsetsList);
	charsetS = (Spinner) findViewById(R.id.PWGD_charsetSpinner);
	charsetS.setAdapter(charsetSA);
	setButton = (Button) findViewById(R.id.PWGD_setButton);
	toClipboardButton = (Button) findViewById(R.id.PWGD_toClipboardButton);
	generateButton = (Button) findViewById(R.id.PWGD_generateButton);
	lengthET = (SecureEditText) findViewById(R.id.PWGD_length);
	passwordET = (SecureEditText) findViewById(R.id.PWGD_passwordField);
	customCharSetET = (SecureEditText) findViewById(R.id.PWGD_customCharSetET);
	excludeCB = (CheckBox) findViewById(R.id.PWGD_excludeCheckBox);
	customCharSetContainer = (LinearLayout) findViewById(R.id.PWGD_customCharSetContainer);

	if (this.messageCode == null) setButton.setVisibility(Button.GONE);
	else toClipboardButton.setVisibility(Button.GONE);

	passwordET.setTransformationMethod(null);
	excludeCB.setText(Html.fromHtml(context.getResources().getString(R.string.passwordGeneratorDialog_excludeCharacters)));

	List<Object> savedSettings = (List) settingDataHolder.getPersistentDataObject("PASSWORD_GENERATOR_SETTINGS");
	List<Object> settings = savedSettings != null ? savedSettings : defaultSettings;

	charsetS.setSelection((Integer) settings.get(0));
	lengthET.setText("");
	lengthET.append(Integer.toString((Integer) settings.get(1)));
	excludeCB.setChecked((Boolean) settings.get(2));
	if (settings.size() > 3) customCharSetET.setText((String) settings.get(3));

	this.setOnCancelListener(new DialogInterface.OnCancelListener() {
		@Override
		public void onCancel(DialogInterface dialogInterface) {
			eep.stopCollectors();
			saveCurrentSettting(settingDataHolder);
		}
	});

	generateButton.setOnClickListener(new android.view.View.OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			startGeneration();
		}
	});

	toClipboardButton.setOnClickListener(new android.view.View.OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			char[] tempPw = passwordET.toCharArray();
			tempPw = Helpers.trim(tempPw);
			setToSystemClipboard(CharBuffer.wrap(tempPw));
			Arrays.fill(tempPw, '\u0000');
			ComponentProvider.getShowMessageDialog(
					context,
					context.getResources().getString(R.string.common_copyToClipboard_text),
					context.getResources().getString(R.string.common_passwordCopiedToClipboard_text) + "<br/><br/>" + context.getResources().getString(R.string.common_copyToClipboardWarning),
					ComponentProvider.DRAWABLE_ICON_INFO_BLUE).show();
			return;
		}
	});

	setButton.setOnClickListener(new android.view.View.OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			buttonLock = true;
			if (messageCode != null) {
				char[] tempPw = passwordET.toCharArray();
				tempPw = Helpers.trim(tempPw);

				CryptActivity ca = (CryptActivity) context;
				ca.setMessage(new ActivityMessage(messageCode, null, null, tempPw));
			}

			cancel();
			return;
		}
	});

	charsetS.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView adapter, View v, int i, long lng) {
			if (i != 8) {
				customCharSetContainer.setVisibility(View.GONE);
				excludeCB.setVisibility(View.VISIBLE);
			} else {
				customCharSetContainer.setVisibility(View.VISIBLE);
				excludeCB.setVisibility(View.GONE);
			}
		}

		@Override
		public void onNothingSelected(AdapterView arg0) {
			// N/A
		}
	});

	this.setOnKeyListener(new DialogInterface.OnKeyListener() {
		@Override
		public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
			if (keyCode == KeyEvent.KEYCODE_SEARCH) {
				return true;
			}
			return false;
		}
	});

	startGeneration();
}

private synchronized void startGeneration() {
	if (buttonLock) return;
	buttonLock = true;

	passwordET.setText("");
	excludeCB.requestFocus();
	generateButton.setEnabled(false);
	toClipboardButton.setEnabled(false);
	setButton.setEnabled(false);
	charsetS.setEnabled(false);
	excludeCB.setEnabled(false);
	lengthET.setEnabled(false);
	customCharSetET.setEnabled(false);
	makeReadOnly(passwordET, true);
	makeReadOnly(lengthET, true);

	eep.reset();
	eep.startCollectors();
	Thread collectorThread = new Thread(new Runnable() {
		public void run() {

			for (int i = 0; i < 9; ++i) {
				pgdHandler.sendMessage(Message.obtain(pgdHandler, PGD_HANDLER_COLLECTION_INPROGRESS));
				try {
					Thread.sleep(220);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			eep.stopCollectors();
			pgdHandler.sendMessage(Message.obtain(pgdHandler, PGD_HANDLER_COLLECTION_COMPLETED));
		}
	});

	collectorThread.start();
}

private synchronized void generate() {
	int position = charsetS.getSelectedItemPosition();
	String lenS = lengthET.getText().toString().trim();
	int length = DEFAULT_LENGTH;
	try {
		length = Integer.parseInt(lenS);
	} catch (NumberFormatException e) {
	}
	if (length > 99) length = 99;
	if (length < 4) length = 4;
	lengthET.setText("");
	lengthET.append(Integer.toString(length));
	PasswordGenerator pg = null;
	if (position != 8) {
		boolean[] conf = charsetsConfList.get(position);
		pg = new PasswordGenerator(conf[0], conf[1], conf[2], conf[3], excludeCB.isChecked());
	} else {
		char[] customCharSet = PasswordGenerator.removeDuplicateChars(Helpers.trim(customCharSetET.toCharArray()));
		customCharSetET.setCharArray(customCharSet);
		pg = new PasswordGenerator(customCharSet);
	}

	pg.setExternalEntropy(eep.getActualDataDigested());
	char[] password = pg.getNewPassword(length);
	passwordET.setText(CharBuffer.wrap(password));
	int textLength = passwordET.toCharArray().length;
	passwordET.setSelection(textLength, textLength);

	Arrays.fill(password, '\u0000');

	buttonLock = false;
}

private void saveCurrentSettting(SettingDataHolder sdh) {
	List<Object> settingsObject = new ArrayList<Object>();
	settingsObject.add(charsetS.getSelectedItemPosition());
	String lenS = lengthET.getText().toString().trim();
	settingsObject.add(lenS.length() > 0 ? Integer.parseInt(lenS) : DEFAULT_LENGTH);
	settingsObject.add(excludeCB.isChecked());
	settingsObject.add(customCharSetET.getText().toString().trim());

	sdh.addOrReplacePersistentDataObject("PASSWORD_GENERATOR_SETTINGS", settingsObject);
	sdh.save();
}

private void makeReadOnly(EditText et, boolean readOnly) {
	et.setFocusableInTouchMode(!readOnly);
	et.setFocusable(!readOnly);
	et.setClickable(!readOnly);
	et.setCursorVisible(!readOnly);
}

@SuppressWarnings("deprecation")
private void setToSystemClipboard(CharSequence text) {
	ClipboardManager ClipMan = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
	ClipMan.setText(text);
}

Handler pgdHandler = new Handler() {
	public void handleMessage(Message msg) {
		if (msg.what == PGD_HANDLER_COLLECTION_COMPLETED) {
			generate();
			generateButton.setEnabled(true);
			toClipboardButton.setEnabled(true);
			setButton.setEnabled(true);
			charsetS.setEnabled(true);
			excludeCB.setEnabled(true);
			lengthET.setEnabled(true);
			customCharSetET.setEnabled(true);
			makeReadOnly(passwordET, false);
			makeReadOnly(lengthET, false);
			passwordET.requestFocus();
			return;
		} else if (msg.what == PGD_HANDLER_COLLECTION_INPROGRESS) {
			char[] currentText = passwordET.toCharArray();
			if (currentText.length >= 4 || currentText.length == 0)
				currentText = (android.os.Build.VERSION.SDK_INT >= 18 ? "⏳" : ".").toCharArray();
			passwordET.setText(new String(currentText) + ".");
		}
	}
};
}
