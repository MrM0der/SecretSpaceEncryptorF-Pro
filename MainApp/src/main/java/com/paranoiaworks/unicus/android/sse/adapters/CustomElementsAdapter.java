package com.paranoiaworks.unicus.android.sse.adapters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;

import com.paranoiaworks.unicus.android.sse.R;
import com.paranoiaworks.unicus.android.sse.components.SecureEditText;
import com.paranoiaworks.unicus.android.sse.components.TextViewMod;

import com.paranoiaworks.unicus.android.sse.dao.VaultV2;
import com.paranoiaworks.unicus.android.sse.misc.StringSentinel;
import com.paranoiaworks.unicus.android.sse.utils.ComponentProvider;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;

/**
 * Adapter for custom elements in Password Vault -> ItemDetail
 * 
 * @author Paranoia Works
 * @version 1.1.0
 */
public class CustomElementsAdapter extends BaseAdapter {

	public static final int MODE_READ = 0;
	public static final int MODE_EDIT = 1;

	private Context context;
	private List<StringSentinel[]> elements;
	private int mode = MODE_READ;
	private float fontSizeMultiplier = 1.0F;

    public CustomElementsAdapter(Context c, List<StringSentinel[]> elements, float fontSizeMultiplier) {
    	context = c;
        this.elements = elements;
		this.fontSizeMultiplier = fontSizeMultiplier;
    }
	
	static class ViewHolder {
		public TextViewMod nameTV;
		public SecureEditText nameET;
		public SecureEditText valueET;
		public Button upButton;
		public Button downButton;
		public Button removeButton;
		public Button toClipboardButton;
		public ViewGroup editButtonsContainer;
	}

	public List<StringSentinel[]> getDataSet()
	{
		return this.elements;
	}
	
    public int getCount() {
        return elements.size();
    }
    
    public Object getItem(int position) {
        return elements.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public void setMode(int mode) {
    	this.mode = mode;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final ViewHolder holder = new ViewHolder();
		LayoutInflater inflater = ((Activity)context).getLayoutInflater();
		convertView = inflater.inflate(R.layout.lc_customelement_listrow, null, true);
		holder.nameTV = (TextViewMod) convertView.findViewById(R.id.CELR_nameTV);
		holder.nameET = (SecureEditText) convertView.findViewById(R.id.CELR_nameET);
		holder.valueET = (SecureEditText) convertView.findViewById(R.id.CELR_valueET);
		holder.upButton = (Button) convertView.findViewById(R.id.CELR_upButton);
		holder.downButton = (Button) convertView.findViewById(R.id.CELR_downButton);
		holder.removeButton = (Button) convertView.findViewById(R.id.CELR_removeButton);
		holder.toClipboardButton = (Button) convertView.findViewById(R.id.CELR_toClipboardButton);
		holder.editButtonsContainer = (ViewGroup) convertView.findViewById(R.id.CELR_editButtonsWrapper);

		holder.nameET.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float)(holder.nameET.getTextSize() * fontSizeMultiplier));
		holder.valueET.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float)(holder.valueET.getTextSize() * fontSizeMultiplier));
			
		final StringSentinel[] tempElement = elements.get(position);
			
		holder.nameTV.setCharArrayAndWipe(tempElement[0].getString());
		holder.nameET.setCharArrayAndWipe(tempElement[0].getString());
		holder.valueET.setCharArrayAndWipe(tempElement[1].getString());

		if(mode == MODE_READ) {
			makeReadOnlyEditText(holder.valueET);
			makeReadOnlyEditText(holder.nameET);

			holder.nameET.setVisibility(View.GONE);
			holder.nameTV.setVisibility(View.VISIBLE);
			holder.editButtonsContainer.setVisibility(ViewGroup.GONE);
		} else if(mode == MODE_EDIT) {
			makeEditableEditText(holder.valueET);
			makeEditableEditText(holder.nameET);

			holder.nameET.setVisibility(View.VISIBLE);
			holder.nameTV.setVisibility(View.GONE);
			holder.editButtonsContainer.setVisibility(ViewGroup.VISIBLE);

			if(position == 0) {
				holder.upButton.setVisibility(ViewGroup.GONE);
				if(getCount() < 2) holder.downButton.setVisibility(ViewGroup.GONE);
				else holder.downButton.setVisibility(ViewGroup.VISIBLE);
			} else if (position == getCount() - 1) {
				holder.upButton.setVisibility(ViewGroup.VISIBLE);
				holder.downButton.setVisibility(ViewGroup.GONE);
			} else {
				holder.upButton.setVisibility(ViewGroup.VISIBLE);
				holder.downButton.setVisibility(ViewGroup.VISIBLE);
			}

			holder.upButton.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					Collections.swap(elements, position, position - 1);
					handler.sendMessage(Message.obtain(handler, 1));
				}
			});

			holder.downButton.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					Collections.swap(elements, position, position + 1);
					handler.sendMessage(Message.obtain(handler, 1));
				}
			});

			holder.removeButton.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					Handler dialogHandler = new Handler()
					{
						public void handleMessage(Message msg)
						{
							if (msg.what == 1)
							{
								elements.remove(position);
								handler.sendMessage(Message.obtain(handler, 1));
							}
						}
					};
					ComponentProvider.getBaseQuestionDialog(
							context,
							context.getResources().getString(R.string.common_continue_text),
							context.getResources().getString(R.string.pwv_newItemRemoveElementQuestion).replaceAll("<1>", Matcher.quoteReplacement(String.valueOf(tempElement[0].getString()))),
							dialogHandler,
							false).show();
				}
			});

			holder.nameET.addTextChangedListener(new TextWatcher() {
				public void afterTextChanged(Editable s) {
						tempElement[0] = new StringSentinel(Helpers.trim(holder.nameET.toCharArray()), VaultV2.SS_GROUP_ID);
				}
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
				public void onTextChanged(CharSequence s, int start, int before, int count) {}
			});

			holder.valueET.addTextChangedListener(new TextWatcher() {
				public void afterTextChanged(Editable s) {
						tempElement[1] = new StringSentinel(Helpers.trim(holder.valueET.toCharArray()), VaultV2.SS_GROUP_ID);
				}
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
				public void onTextChanged(CharSequence s, int start, int before, int count) {}
			});

		}

		holder.toClipboardButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				handleBasicCopy(holder.valueET.getText().toString().trim());
			}
		});

		return convertView;
	}

	private void makeReadOnlyEditText(EditText et)
	{
		et.setFocusableInTouchMode(false);
		et.setFocusable(false);
		et.setClickable(false);
		et.setCursorVisible(false);
		et.setBackgroundResource(R.drawable.d_edittext_readonly);
		et.setTextColor(Color.BLACK);
	}

	private void makeEditableEditText(EditText et)
	{
		et.setFocusableInTouchMode(true);
		et.setFocusable(true);
		et.setClickable(true);
		et.setCursorVisible(true);
		et.setBackgroundResource(R.drawable.d_edittext);
	}

	private void handleBasicCopy(String text)
	{
		text = text.trim();
		if(text.length() == 0)
		{
			ComponentProvider.getImageToastKO(context.getResources().getString(R.string.common_noTextToCopy), (Activity)context).show();
			return;
		}

		setToSystemClipboard(text);
		ComponentProvider.getImageToastOK(context.getResources().getString(R.string.common_textCopiedToSystemClipboard), (Activity)context).show();
	}

	private void setToSystemClipboard(String text)
	{
		ClipboardManager ClipMan = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
		ClipMan.setText(text);
	}

	final Handler handler = new Handler()
	{
		public void handleMessage(Message msg)
		{
			if (msg.what == 1) {
				notifyDataSetChanged();
			}
		}
	};
}