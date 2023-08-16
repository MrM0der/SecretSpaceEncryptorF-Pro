package com.paranoiaworks.unicus.android.sse.components;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.paranoiaworks.unicus.android.sse.CryptActivity;
import com.paranoiaworks.unicus.android.sse.R;
import com.paranoiaworks.unicus.android.sse.adapters.BasicListAdapter;
import com.paranoiaworks.unicus.android.sse.dao.ActivityMessage;
import com.paranoiaworks.unicus.android.sse.dao.SettingDataHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Search Dialog for Password Vault
 * 
 * @author Paranoia Works
 * @version 1.0.0
 */
public class PWVSearchDialog extends SecureDialog {
	
    private final List<String> searchInList = new ArrayList<String>();

	private SpinnerAdapter seachInSA;
	private Activity context;
	private SettingDataHolder sdh;
    private Spinner seachInS;
    private SecureEditText searchET;
    private Button cancelButton;
    private Button searchButton;
    
    private Integer messageCode = null;
    
	public PWVSearchDialog(View v)
	{
		this(getActivityFromContext(v.getContext()));
	}	
	
	public PWVSearchDialog(Activity context)
	{
		super(context, R.style.Dialog_CustomAlert);
		this.context = context;
		this.sdh =((CryptActivity)context).getSettingDataHolder();
		this.init();
	}

	public PWVSearchDialog(View v, int messageCode)
	{
		this(getActivityFromContext(v.getContext()), messageCode);
	}	
	
	public PWVSearchDialog(Activity context, int messageCode)
	{
		super(context, R.style.Dialog_CustomAlert);
		this.context = context;
		this.sdh =((CryptActivity)context).getSettingDataHolder();
		this.messageCode = messageCode;
		this.init();
	}
		
	private void init()
	{
		searchInList.add(context.getResources().getString(R.string.common_title_text));
		searchInList.add(context.getResources().getString(R.string.common_title_text) + " + " + context.getResources().getString(R.string.common_account_text));
		searchInList.add(context.getResources().getString(R.string.common_url_text));
		searchInList.add(context.getResources().getString(R.string.pwv_search_inAllFields));

		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.lc_pwv_search_dialog);
		this.setCanceledOnTouchOutside(false);
		searchET = (SecureEditText)findViewById(R.id.PWVSD_searchET);
		seachInSA = new BasicListAdapter(context, searchInList);
		seachInS = (Spinner)findViewById(R.id.PWVSD_searchInSpinner);
		seachInS.setAdapter(seachInSA);
		cancelButton = (Button)findViewById(R.id.PWVSD_cancelButton);
		searchButton = (Button)findViewById(R.id.PWVSD_searchButton);

		Integer savedPosition = (Integer)sdh.getSessionCacheObject("PWVSD_SeachInS_Position");
		if(savedPosition != null) seachInS.setSelection(savedPosition);

		searchButton.setOnClickListener(new android.view.View.OnClickListener()
	    {
		    @Override
		    public synchronized void onClick(View v) 
		    {
				Object[] searchFor = new Object[2];
				searchFor[0] = searchET.toLowerCaseCharArray();
				searchFor[1] = seachInS.getSelectedItemPosition();

				if(((char[])searchFor[0]).length < 1) return;

				CryptActivity ca = (CryptActivity)context;
				ca.setMessage(new ActivityMessage(messageCode, null, null, searchFor));
				cancel();
		    }
	    });
	    
	    cancelButton.setOnClickListener(new android.view.View.OnClickListener()
	    {
		    @Override
		    public synchronized void onClick(View v) 
		    {
				cancel();
		    }
	    });

		seachInS.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView adapter, View v, int i, long lng)
			{
				sdh.addOrReplaceSessionCacheObject("PWVSD_SeachInS_Position", i);
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
	}
}
