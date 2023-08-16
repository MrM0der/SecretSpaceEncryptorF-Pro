package com.paranoiaworks.unicus.android.sse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.paranoiaworks.unicus.android.sse.components.HtmlAlertDialog;
import com.paranoiaworks.unicus.android.sse.components.SimpleHTMLDialog;
import com.paranoiaworks.unicus.android.sse.dao.ActivityMessage;
import com.paranoiaworks.unicus.android.sse.services.FileEncryptionService;
import com.paranoiaworks.unicus.android.sse.services.ObjectKeeperDummyService;
import com.paranoiaworks.unicus.android.sse.utils.ComponentProvider;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;

/**
 * Application "Main Menu" activity class
 * 
 * @author Paranoia Works
 * @version 1.1.19
 */
public class MainActivity extends CryptActivity {

	private LinearLayout passwordVaultButton;
	private LinearLayout messageEncButton;
	private LinearLayout fileEncButton;
	private LinearLayout otherUtilsButton;
	private View settingsButton;
	private View helpButton;
	private View exitButton;
	private SimpleHTMLDialog simpleHTMLDialog;
	
	private LinearLayout containerAL;
	private LinearLayout containerBL;
	
	private static boolean readyForDestroy = false;
	
	
	public MainActivity()
	{		
		super();
	}
	
	/** Enable Main Activity for destroy */
	public static void setReadyForDestroy()
	{
		readyForDestroy = true;
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
		initApp();
		renderLayout();
		boolean fileSystemOk = Helpers.makeBasicAppDirs(this);

		if(!fileSystemOk && !Helpers.isAndroid11PorHflavor()) {
			showInfoDialogRed(getResources().getString(R.string.common_permission_files_cantAccessFile));
		}

		//Helpers.checkAllAppDirectoriesAndShowError(this);
		
	    try {
			HtmlAlertDialog whatsNewDialog = new HtmlAlertDialog(this, getResources().getString(R.string.alerts_dir) + 
					getResources().getString(R.string.whatsNewFile), getResources().getString(R.string.common_whatsNew));
			whatsNewDialog.addValue("VERSION_FLAVOR", StaticApp.VERSION_FLAVOR);
			whatsNewDialog.show();
		} catch (Exception e) {
			e.printStackTrace();
		}

    }
    
    /** Application Initialization */
    private void initApp()
    { 	

    }   	
    	
    /** Prepare Main Menu Layout */
    private void renderLayout()
    {       	
        this.setContentView(R.layout.la_main);
        setTitle(getResources().getString(R.string.app_name_full));
        
        this.containerAL = (LinearLayout)this.findViewById(R.id.M_containerA);
        this.containerBL = (LinearLayout)this.findViewById(R.id.M_containerB);
        
        TextView text;
        ImageView image;
        
        this.passwordVaultButton = (LinearLayout)getLayoutInflater().inflate(R.layout.lc_square_button_icon, null);
        text = (TextView)passwordVaultButton.findViewById(R.id.text);
        text.setText(getResources().getString(R.string.main_passwordVaultButton));
        image = (ImageView)passwordVaultButton.findViewById(R.id.image);
        image.setImageResource(R.drawable.main_safe);
        passwordVaultButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	if(detectDoubleClick()) return;
		    	
		    	Intent myIntent = new Intent(v.getContext(), PasswordVaultActivity.class);
				myIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivityForResult(myIntent, EXIT_CASCADE);
		    }
	    });
        
        this.messageEncButton = (LinearLayout)getLayoutInflater().inflate(R.layout.lc_square_button_icon, null);
        text = (TextView)messageEncButton.findViewById(R.id.text);
        text.setText(getResources().getString(R.string.main_messageEncButton));
        image = (ImageView)messageEncButton.findViewById(R.id.image);
        image.setImageResource(R.drawable.main_text);
        messageEncButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	if(detectDoubleClick()) return;
		    	
		    	Intent myIntent = new Intent(v.getContext(), MessageEncActivity.class);
				myIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivityForResult(myIntent, EXIT_CASCADE);
		    }
	    });
        
        this.fileEncButton = (LinearLayout)getLayoutInflater().inflate(R.layout.lc_square_button_icon, null);
        text = (TextView)fileEncButton.findViewById(R.id.text);
        text.setText(getResources().getString(R.string.main_fileEncButton));
        image = (ImageView)fileEncButton.findViewById(R.id.image);
        image.setImageResource(R.drawable.main_file);
        fileEncButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	if(detectDoubleClick()) return;
		    	
		    	Intent myIntent = new Intent(v.getContext(), FileEncActivity.class);
				myIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivityForResult(myIntent, EXIT_CASCADE);
		    }
	    });
        
        this.otherUtilsButton = (LinearLayout)getLayoutInflater().inflate(R.layout.lc_square_button_icon, null);
        text = (TextView)otherUtilsButton.findViewById(R.id.text);
        text.setText(getResources().getString(R.string.main_otherUtils));
        image = (ImageView)otherUtilsButton.findViewById(R.id.image);
        image.setImageResource(R.drawable.main_utils);
        otherUtilsButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	if(detectDoubleClick()) return;
		    	
		    	Intent myIntent = new Intent(v.getContext(), OtherUtilsActivity.class);
				myIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivityForResult(myIntent, 0);
		    }
	    });
        
        setLayoutOrientation();
        
    	this.settingsButton = (View)this.findViewById(R.id.M_settingsButton);
	    this.settingsButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
                if(detectDoubleClick()) return;
		    	
		    	Intent myIntent = new Intent(v.getContext(), SettingsActivity.class);
				myIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivityForResult(myIntent, 0);
		    }
	    }); 
	    
	    // Help Button
    	this.helpButton = (View)this.findViewById(R.id.M_helpButton);
	    this.helpButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	if(detectDoubleClick()) return;
		    	
		    	simpleHTMLDialog = new SimpleHTMLDialog(v);
		    	simpleHTMLDialog.addValue("SHOW_UPGRADE_FEATURES", StaticApp.SHOW_UPGRADE_FEATURES ? "1" : "0");
				simpleHTMLDialog.addValue("SHOW_REVIEW_ABOUT", Integer.toString(StaticApp.SHOW_REVIEW_ABOUT));
		    	simpleHTMLDialog.addValue("CN_VERSION", "0");
		    	simpleHTMLDialog.addValue("APP_VERSION_NAME", getResources().getString(R.string.APP_VERSION_NAME) + (StaticApp.VERSION_FLAVOR.length() > 0 ? " " + StaticApp.VERSION_FLAVOR : ""));
		    	simpleHTMLDialog.addValue("APP_YEAR", getResources().getString(R.string.APP_YEAR));
				simpleHTMLDialog.addValue("API_VERSION", Integer.toString(android.os.Build.VERSION.SDK_INT));
		    	if(settingDataHolder.getLicenceLevel() < 1) simpleHTMLDialog.loadURL(getResources().getString(R.string.helpLink_Main));
		    	else simpleHTMLDialog.loadURL(getResources().getString(R.string.helpLink_Main_Pro));
		    	simpleHTMLDialog.show();
		    }
	    });
    	
    	// Exit Application Button
    	this.exitButton = (View)this.findViewById(R.id.M_exitButton);
	    this.exitButton.setOnClickListener(new OnClickListener() 
	    {
		    @Override
		    public void onClick(View v) 
		    {
		    	if(detectDoubleClick()) return;

				boolean runClipboardCleaner = settingDataHolder.getItemAsBoolean("SC_Common", "SI_RunClipboardCleanerOnExit");

				if(!runClipboardCleaner) {
					setReadyForDestroy();
					if(Build.VERSION.SDK_INT >= 16) finishAffinity();
					else finish();
				}
				else {
					final Handler afterHandler = new Handler()
					{
						public void handleMessage(Message msg)
						{
							if (msg.what == 1) {
								setReadyForDestroy();
								if(Build.VERSION.SDK_INT >= 16) finishAffinity();
								else finish();
							}
						}
					};

					Helpers.runClipboardCleaner(MainActivity.this, afterHandler);
				}
		    }
	    });
    }

    void processMessage()
    {
        ActivityMessage am = getMessage();
        if (am == null) return;
        
        int messageCode = am.getMessageCode();
        
        if(messageCode == 0)
        {
            if(am.getMainMessage().equals("upgrade"))
            {
				Intent myIntent = new Intent(this, UpgradeToProActivity.class);
				this.startActivityForResult(myIntent, 0);
            }
        }
    }
    
    /** Solve differences between Portrait and Landscape orientation */ 
	private void setLayoutOrientation()
    {    	
    	int orientation = this.getResources().getConfiguration().orientation;

		DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		int height = displayMetrics.heightPixels;
		int width = displayMetrics.widthPixels;
		double screenRatio = width > 0 && height > 0 ? height / (double)width : 0;
    	
        parametrizeSquareView(passwordVaultButton, screenRatio);
        parametrizeSquareView(messageEncButton, screenRatio);
        parametrizeSquareView(fileEncButton, screenRatio);
        parametrizeSquareView(otherUtilsButton, screenRatio);
        
        containerAL.removeAllViews();
        containerBL.removeAllViews();
    	
    	if(orientation == Configuration.ORIENTATION_PORTRAIT && screenRatio > 1.2)
    	{   	
    		containerBL.setVisibility(View.VISIBLE);
    		containerAL.addView(passwordVaultButton);
            containerAL.addView(messageEncButton);
            containerBL.addView(fileEncButton);
            containerBL.addView(otherUtilsButton);
    	}
    	else
    	{
    		containerBL.setVisibility(View.GONE);
    		containerAL.addView(passwordVaultButton);
            containerAL.addView(messageEncButton);
            containerAL.addView(fileEncButton);
            containerAL.addView(otherUtilsButton);
    	}
    }
	
	/** Render Layout Helper */
	private void parametrizeSquareView(ViewGroup view, double screenRatio)
	{
		float scaler = -1; int borderSize = -1;
		int orientation = this.getResources().getConfiguration().orientation;
		
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		float width = pxToDp(dm.widthPixels);
		float height = pxToDp(dm.heightPixels);
		if(orientation == Configuration.ORIENTATION_PORTRAIT && screenRatio > 1.2){
			scaler = 2;
			borderSize = dpToPx(10);
		} else {
			scaler = 4;
			borderSize = dpToPx(8);
		}
		int size = dpToPx((width - (10 * scaler)) / scaler);

		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size, 1.0f);
		params.setMargins(borderSize, borderSize, borderSize, borderSize);
		view.setLayoutParams(params);
		view.setFocusable(true);	
	}

    @Override
    protected void onStart()
    {
    	super.onStart();
    }
    
    @Override
    public void onConfigurationChanged(Configuration c)
    {
    	setLayoutOrientation();
    	//drawApplicationReport();
    	super.onConfigurationChanged(c);
    }
     
    @Override
    public void onBackPressed()
    {
    	ComponentProvider.getExitDialog(this).show();
    }
    
    @Override
    public void onWindowFocusChanged(boolean b)
    {
    	//drawApplicationReport();
    	super.onWindowFocusChanged(b);
    }
    
    @Override
    public void onResume() {
        super.onResume();

        if(settingDataHolder.getLicenceLevel() < 1) setTitle(getResources().getString(R.string.app_name_full_free));
        else setTitle(getResources().getString(R.string.app_name_full_pro));

		if(!FileEncryptionService.isRunning()) {
			SharedPreferences preferences = getSharedPreferences(FileEncryptionService.NAME, MODE_PRIVATE);
			boolean lastFESJobUnfinished= preferences.getBoolean(FileEncryptionService.FES_NOTFINISHED_TOKEN, false);

			if (lastFESJobUnfinished) {
				showInfoDialogRed(getResources().getString(R.string.fes_forciblyTerminatedWarning));
				preferences.edit().putBoolean(FileEncryptionService.FES_NOTFINISHED_TOKEN, false).commit();
			}
		}
    }
    
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent resultData) 
	{
		if (requestCode == EXIT_CASCADE && resultCode == EXIT_CASCADE) 
		{
			readyForDestroy = true;
			finish();
		}
		else if(resultCode == RESTART_PASSWORDVAULTACTIVITY)
		{
            Intent myIntent = new Intent(this, PasswordVaultActivity.class);
            startActivityForResult(myIntent, EXIT_CASCADE);
		}
	}

	@Override
    public void onDestroy() {
    	if(readyForDestroy)
    	{
    		// Wipeout application
    		try {
				stopService(new Intent(this, ObjectKeeperDummyService.class));
				//android.os.Process.killProcess(android.os.Process.myPid());
			} catch (Throwable e) {
				e.printStackTrace();
			}
    	}
		super.onDestroy();
    }
}