package com.paranoiaworks.unicus.android.sse;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;

import com.paranoiaworks.unicus.android.sse.dao.ActivityMessage;
import com.paranoiaworks.unicus.android.sse.dao.ApplicationStatusBean;
import com.paranoiaworks.unicus.android.sse.dao.PasswordAttributes;
import com.paranoiaworks.unicus.android.sse.dao.SettingDataHolder;
import com.paranoiaworks.unicus.android.sse.utils.ComponentProvider;
import com.paranoiaworks.unicus.android.sse.utils.DBHelper;
import com.paranoiaworks.unicus.android.sse.utils.Encryptor;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;

import java.security.MessageDigest;

/**
 * Base parent class for other activities
 *
 * @author Paranoia Works
 * @version 1.0.20
 */
public abstract class CryptActivity extends Activity {

	Encryptor encryptor;
	PasswordAttributes passwordAttributes;
	private ActivityMessage message;
	private boolean noCustomTitle = false;
	private String activitySettingId;
	private boolean buttonsLock = false;
	protected DBHelper dbHelper;
	protected SettingDataHolder settingDataHolder;
	
	private View titleWrapper;
	private TextView title;
	private TextView titleRight;
	
	private long lastClickTime = 0;
	private long screenLockTime = 0;
	private int screenLockDelay = 0;
	
	public ApplicationStatusBean asb;
	
	private int running = 0;

	private static Object[] temporaryObjectStorage = null;
	
	public static final int COMMON_MESSAGE_SET_ENCRYPTOR = -101;
	public static final int COMMON_MESSAGE_CONFIRM_EXIT = -102;	
	public static final int RESTART_PASSWORDVAULTACTIVITY = 8001;
	public static final int EXIT_CASCADE = 8888;	
	

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);

		dbHelper = DBHelper.initDB(this.getApplicationContext());
    	settingDataHolder = SettingDataHolder.getInstance(dbHelper);
    	
		if(asb == null) asb = dbHelper.getAppStatus();
    	
    	if(activitySettingId != null) // resolve activity preferences
    	{
    		noCustomTitle = settingDataHolder.getItemAsBoolean(activitySettingId, "SI_HideTitleBar");
    	}
    	
    	boolean preventScreenshots = settingDataHolder.getItemAsBoolean("SC_Common", "SI_PreventScreenshots");
    	if(preventScreenshots) getWindow().setFlags(LayoutParams.FLAG_SECURE, LayoutParams.FLAG_SECURE);
    	
    	if(noCustomTitle)requestWindowFeature(Window.FEATURE_NO_TITLE);
    	else requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
    }
    
    @Override
    public void setContentView(int layoutResId) {
    	/* TODO for more complex solution of titlebar
    	LinearLayout masterLayout = (LinearLayout)getLayoutInflater().inflate(R.layout.l_cryptactivity_master, null);
    	View currentLayout = (View)getLayoutInflater().inflate(layoutResId, masterLayout);
    	titleFull = (View)masterLayout.findViewById(R.id.app_title);
    	titleLeft = (TextView)masterLayout.findViewById(R.id.app_title_left);
    	titleRight = (TextView)masterLayout.findViewById(R.id.app_title_right);
    	super.setContentView(masterLayout);
    	*/
    	
    	// Custom Title Bar
    	super.setContentView(layoutResId);
    	if(noCustomTitle) return;
    	getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.lu_application_title);
    	titleWrapper = (View)findViewById(R.id.app_title);
    	title = (TextView)findViewById(R.id.title);
    	titleRight = (TextView)findViewById(R.id.app_title_right);
    }
    
	/**+ Activity Direct Messaging */
    public ActivityMessage getMessage() {
		return message;
	}

	public void setMessage(ActivityMessage message) {
		this.message = message;
		processMessage();
	}
	
	public void resetMessage() {
		this.message = null;
	}
	
	abstract void processMessage();
	//- Activity Direct Messaging
    
	
	/**+ TitleBar regarding methods */
	@Override
    public void setTitle(CharSequence text) {
    	if(title != null) title.setText(text);
    	super.setTitle(text);
    }
    
    @Override
    public void setTitle(int resid) {
    	if(title != null) title.setText(getResources().getString(resid));
    	super.setTitle(resid);
    }
    
    public void setTitleRight(CharSequence text) {
    	if(titleRight != null) titleRight.setText(text);
    }
    
    public void setTitleRightTag(Object tag) {
    	if(titleRight != null) titleRight.setTag(tag);
    }
    
    public Object getTitleRightTag() {
    	return titleRight.getTag();
    }
    
    public void resolveActivityPreferences(String activitySettingId) {
    	this.activitySettingId = activitySettingId;
    }
    //- TitleBar regarding methods
    
	/** Get SettingDataHolder */
	public SettingDataHolder getSettingDataHolder()
    {
    	return settingDataHolder;
    }
    
    /** Convert DP to PX */
    public int dpToPx(float dp)
    {
    	float scale = this.getResources().getDisplayMetrics().density;
    	return (int)(dp * scale + 0.5f);
    }
    
    /** Convert PX to DP */
    public float pxToDp(int px)
    {
    	float scale = this.getResources().getDisplayMetrics().density;
    	return ((float)px - 0.5f) / scale;
    }
    
	/** Get String Resource dynamically by Identifier */
	public String getStringResource(String name)
    {
    	String resText = null;
    	int resID = getStringResID(name);
    	if(resID == 0) resText = name;
    	else resText = getResources().getString(resID);
    	
    	return resText;
    }
    
    private int getStringResID(String name)
    {
    	return getResources().getIdentifier(name, "string", this.getPackageName());
    }
	
    /** Disable "Search button" */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SEARCH) {            
            return true;
        } else return super.onKeyDown(keyCode, event);
    }
    
    /** Button Lock Flag */
    void activateButtonsLock()
	{
		buttonsLock = true;
	}
	
	void deactivateButtonsLock()
	{
		buttonsLock = false;
	}
	
	boolean isButtonsLockActivated()
	{
		return buttonsLock;
	}
	
    /** Tablet or Not */
    public boolean isTablet()
    {	
    	return (getResources().getString(R.string.screen_type).equalsIgnoreCase("yes"));
    }

	void wipeEncryptor()
	{
		if(encryptor != null)
		{
			Encryptor tempEncryptor = encryptor;
			encryptor = null;
			tempEncryptor.wipeMasterKeys();
		}
	}
    
    void showErrorDialog(Throwable e)
    {
    	ComponentProvider.getShowMessageDialog(this, 
    			getResources().getString(R.string.common_message_text), 
    			e.getLocalizedMessage(), 
    			ComponentProvider.DRAWABLE_ICON_CANCEL).show();
    }
    
    void showErrorDialog(String message) 
    {
			ComponentProvider.getShowMessageDialog(this, 
					getResources().getString(R.string.common_message_text), 
					message, 
					ComponentProvider.DRAWABLE_ICON_CANCEL).show();
    }
    
    void showInfoDialog(String message)
    {
			ComponentProvider.getShowMessageDialog(this, 
					getResources().getString(R.string.common_message_text), 
					message, 
					ComponentProvider.DRAWABLE_ICON_INFO_BLUE).show();
    }

	void showInfoDialogRed(String message)
	{
		ComponentProvider.getShowMessageDialog(this,
				getResources().getString(R.string.common_message_text),
				message,
				ComponentProvider.DRAWABLE_ICON_INFO_RED).show();
	}

	boolean detectDoubleClick()
	{
		return detectDoubleClick(true);
	}
    
    boolean detectDoubleClick(boolean resetCounter)
    {
    	long span = 1500;
    	
    	if(SystemClock.elapsedRealtime() - lastClickTime < span) {
            return true;
        }
    	if(resetCounter) lastClickTime = SystemClock.elapsedRealtime();
        return false;
    }

    void setScreenLockTime(int delay)
	{
		this.screenLockTime = android.os.Build.VERSION.SDK_INT >= 17 ? SystemClock.elapsedRealtimeNanos() : 0;
		this.screenLockDelay = delay > 0 ? delay : 0;
	}

	boolean checkScreenAutoUnlock()
	{
		if(android.os.Build.VERSION.SDK_INT >= 17 && screenLockDelay > 0) {
			return (SystemClock.elapsedRealtimeNanos() - this.screenLockTime) / 1000000000 < screenLockDelay * 60 ? true : false;
		}
		else return false;
	}

	/** When other methods are not safe (not a good practise, but they are not any "official" safe methods for passing objects and saving states in Android) */
    public static synchronized void setTemporaryObject(Object object, byte[] verificationTag)
	{
		temporaryObjectStorage = new Object[2];

		temporaryObjectStorage[0] = object;
		temporaryObjectStorage[1] = verificationTag;
	}

	public static Object getTemporaryObject(byte[] verificationTag)
	{
		try {
			if(Helpers.isEqualTimeConstant(verificationTag, (byte[])temporaryObjectStorage[1]))
				return temporaryObjectStorage[0];
			else return null;
		} catch (Exception e) {
			return null;
		}
	}

	public static void removeTemporaryObject()
	{
		temporaryObjectStorage = null;
	}
}
