package com.paranoiaworks.unicus.android.sse.components;

import android.content.Context;
import android.text.Html;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.paranoiaworks.unicus.android.sse.R;

/**
 * Custom "Show Message" + "Ask Question" dialog
 * 
 * @author Paranoia Works
 * @version 1.0.2
 */ 
public class CustomAlertDialog extends SecureDialog {
	
	private Button continueButton;
	private Button cancelButton;
	private Button okButton;
	private TextView textTV;
	private TextView titleTV;
	private ImageView iconIV;
	private boolean customFlag = false;

	private CustomAlertDialog(Context context){
		super(context, R.style.Dialog_CustomAlert);
		getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
	}

	public static CustomAlertDialog getMessageDialog(Context context, String title, String message, int iconDrawableResource) {
		CustomAlertDialog cad = new CustomAlertDialog(context);
		cad.init(title, message, false, iconDrawableResource);
		return cad;
	}

	public static CustomAlertDialog getQuestionDialog(Context context, String title, String question, int iconDrawableResource) {
		CustomAlertDialog cad = new CustomAlertDialog(context);
		cad.init(title, question, true, iconDrawableResource);
		return cad;
	}

	public static CustomAlertDialog getQuestionDialog(Context context, String title, String question, boolean infoMode) {
		return getQuestionDialog(context, title, question, infoMode, false);
	}

	public static CustomAlertDialog getQuestionDialog(Context context, String title, String question, boolean infoMode, boolean redIcon) {
		CustomAlertDialog cad = new CustomAlertDialog(context);
		if(redIcon)
			cad.init(title, question, true, infoMode ? R.drawable.info_icon_red_large : R.drawable.ask_icon_red_large);
		else
			cad.init(title, question, true, infoMode ? R.drawable.info_icon_large : R.drawable.ask_icon_large);
		String positiveText = infoMode ? context.getResources().getString(R.string.common_continue_text) : context.getResources().getString(R.string.common_yes_text);
		String negativeText = infoMode ? context.getResources().getString(R.string.common_cancel_text) : context.getResources().getString(R.string.common_no_text);
		cad.getPositiveButton().setText(positiveText);
		cad.getNegativeButton().setText(negativeText);
		return cad;
	}

	public Button getPositiveButton() {
		return continueButton;
	}

	public Button getNegativeButton() {
		return cancelButton;
	}

	public Button getNeutralButton() {
		return okButton;
	}

	public boolean getCustomFlag() {
		return customFlag;
	}

	public void setCustomFlag(boolean customFlag) {
		this.customFlag = customFlag;
	}

	private void init(String title, String text, boolean question, int icon)
	{
		this.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.lc_customalert_dialog);
		this.setCancelable(true);
		this.setCanceledOnTouchOutside(false);

		iconIV = (ImageView) this.findViewById(R.id.CAD_icon);
		cancelButton = (Button) this.findViewById(R.id.CAD_cancel);
		continueButton = (Button) this.findViewById(R.id.CAD_continue);
		okButton = (Button) this.findViewById(R.id.CAD_ok);
		titleTV = (TextView) this.findViewById(R.id.CAD_title);
		textTV = (TextView) this.findViewById(R.id.CAD_text);

		iconIV.setImageResource(icon);
		titleTV.setText(Html.fromHtml(title));
		textTV.setText(Html.fromHtml(text));

		if(question) {
			this.findViewById(R.id.CAD_container_ok).setVisibility(View.GONE);
		}
		else {
			continueButton.setVisibility(View.GONE);
			cancelButton.setVisibility(View.GONE);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_SEARCH) {
			return true;
		} else return super.onKeyDown(keyCode, event);
	}
}
