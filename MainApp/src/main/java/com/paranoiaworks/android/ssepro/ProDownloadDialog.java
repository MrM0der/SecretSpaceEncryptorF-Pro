package com.paranoiaworks.android.ssepro;

import android.app.Activity;
import android.content.Context;
import android.text.ClipboardManager;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.paranoiaworks.unicus.android.sse.CryptActivity;
import com.paranoiaworks.unicus.android.sse.R;
import com.paranoiaworks.unicus.android.sse.components.ImageToast;
import com.paranoiaworks.unicus.android.sse.components.SecureDialog;
import com.paranoiaworks.unicus.android.sse.dao.SettingDataHolder;
import com.paranoiaworks.unicus.android.sse.utils.Encryptor;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;

public class ProDownloadDialog extends SecureDialog {

	final String codeString = "PRO_VERSION_DOWNLOAD_CODE";
	
	TextView mainText;
	EditText codeET;
	Button endButton;
	Button copyButton;
	Activity context;
	
	public ProDownloadDialog(View v) 
	{
		this(getActivityFromContext(v.getContext()));
	}
	
	public ProDownloadDialog(Activity context) 
	{
		super(context, R.style.Dialog_CustomAlert);
		this.context = context;
		init();
	}
	
	public ProDownloadDialog(Activity context, int theme) 
	{
		super(context, theme);
		this.context = context;
		init();
	}

	private void init()
	{		
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.lc_pro_download_dialog);
		this.setCanceledOnTouchOutside(false);
		
		mainText = (TextView)findViewById(R.id.PDD_Description);
		codeET = (EditText)findViewById(R.id.PDD_Code);
		
		mainText.setText(Html.fromHtml(context.getResources().getString(R.string.common_pro_downloadDesktop_details)));
		codeET.setBackgroundResource(R.drawable.d_edittext_readonly);
		
		copyButton = (Button)findViewById(R.id.PDD_CopyButton);
		copyButton.setOnClickListener(new View.OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	ClipboardManager ClipMan = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
		    	ClipMan.setText(codeET.getText().toString());
		    	new ImageToast(context.getResources().getString(R.string.common_textCopiedToSystemClipboard), ImageToast.TOAST_IMAGE_OK, context).show();
		    }
	    });
			
		endButton = (Button)findViewById(R.id.PDD_CancelButton);
		endButton.setOnClickListener(new View.OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	cancel();
		    }
	    });
		
		SettingDataHolder sdh = ((CryptActivity)context).getSettingDataHolder();
		String code = (String)sdh.getPersistentDataObject(codeString);
		
		if(code == null)
		{
			String input = "0" + Encryptor.getMD5Hash(Encryptor.getRandomBA(16)).substring(0, 6);			
			try {
				code = Helpers.byteArrayToHexString(new Encryptor("ProVersionCode-YesItIsOpenSource".toCharArray(), 6).encrypt(input.getBytes(), false));
			} catch (Exception e) {
				e.printStackTrace();
			}
			sdh.addOrReplacePersistentDataObject(codeString, code);
			sdh.save();
		}
		
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < code.length(); ++i)
		{
			sb.append(code.charAt(i));
			if((i + 1) % 4 == 0 && i != 0) sb.append(" ");
		}		
		codeET.setText(sb.toString().trim());
	}
}
