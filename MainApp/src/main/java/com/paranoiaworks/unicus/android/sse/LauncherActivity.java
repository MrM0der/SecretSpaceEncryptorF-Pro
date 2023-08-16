package com.paranoiaworks.unicus.android.sse;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;

import com.paranoiaworks.unicus.android.sse.components.ImageToast;
import com.paranoiaworks.unicus.android.sse.components.SettingsAppStartProtectionDialog;
import com.paranoiaworks.unicus.android.sse.components.SimplePasswordDialog;
import com.paranoiaworks.unicus.android.sse.misc.CryptFile;
import com.paranoiaworks.unicus.android.sse.misc.CryptFileWrapper;
import com.paranoiaworks.unicus.android.sse.services.ObjectKeeperDummyService;
import com.paranoiaworks.unicus.android.sse.utils.ComponentProvider;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;


/**
 * Application Launcher
 * 
 * @author Paranoia Works
 * @version 1.2.2
 */
public class LauncherActivity extends CryptActivity  {
	
	private SimplePasswordDialog pd = null;

	private boolean readyForDestroy = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
    	dbHelper.updateAppStatus();

    	if(isStoragePermissionGranted())
    		startLaunchWorkflow();
    }

	private void startLaunchWorkflow()
	{
		boolean launchProtection = settingDataHolder.getItemAsBoolean("SC_Common", "SI_AppStartProtection");
		final List<byte[]> passwordPackage = (List<byte[]>)settingDataHolder.getPersistentDataObject(SettingsAppStartProtectionDialog.PERSISTENT_DATA_OBJECT_LAUNCH_PASSWORD);

		if(passwordPackage == null && launchProtection) {
			settingDataHolder.addOrReplaceItem("SC_Common", "SI_AppStartProtection", Boolean.toString(false));
			settingDataHolder.save();
			launchProtection = false;
		}

		if(launchProtection)
		{
			Handler pdHandler = new Handler()
			{
				public void handleMessage(Message msg)
				{
					if (msg.what == SimplePasswordDialog.SPD_HANDLER_OK)
					{
						Object[] attachment = (Object[])msg.obj;
						char[] password = (char[])attachment[0];
						password = Helpers.trim(password);

						byte[] hashedPassword = SettingsAppStartProtectionDialog.getPasswordDerivative(password, passwordPackage.get(1));
						if(Helpers.isEqualTimeConstant(hashedPassword, passwordPackage.get(0)))
						{
							pd.cancel();
							launchApplication();
						}
						else {
							new ImageToast(LauncherActivity.this.getResources().getString(R.string.passwordDialog_invalidPassword),
										ImageToast.TOAST_IMAGE_CANCEL, LauncherActivity.this).show();
							pd.show();
						}
						Arrays.fill(password, '\u0000');
					}
					else {
						readyForDestroy = true;
						finish();
					}
				}
			};
			pd = new SimplePasswordDialog(this, SimplePasswordDialog.SPD_MODE_ENTER_PASSWORD, pdHandler);
			pd.setHideOnly(true);
			pd.show();
		}
		else {
			launchApplication();
		}
	}

	private boolean isStoragePermissionGranted()
	{
		if(Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT <= 29) {
			if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
					== PackageManager.PERMISSION_GRANTED) {
				return true;
			} else {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 333);
				return false;
			}
		}
		else if(Build.VERSION.SDK_INT >= 30) {
			if (Environment.isExternalStorageManager()) return true;
			else {
				try {
					Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
					intent.addCategory("android.intent.category.DEFAULT");
					intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
					startActivityForResult(intent, 333);
				} catch (Exception e) {
					Intent intent = new Intent();
					intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
					startActivityForResult(intent, 333);
				}
				return false;
			}
		}
		else {
			return true;
		}
	}

	private void makeAndVerifyImportExportDir()
	{
		if(settingDataHolder.getItem("SC_Common", "SI_ImportExportPath").equals("???") && !Helpers.isAndroid11PorHflavor())
		{
			List<File> pathList = Helpers.getExtDirectories(this, false);
			if(pathList != null && pathList.size() > 1 && pathList.get(1) != null)
			{
				settingDataHolder.addOrReplaceItem("SC_Common", "SI_ImportExportPath",
						pathList.get(1).getAbsolutePath() + File.separator + getResources().getString(R.string.importExportDir));
				settingDataHolder.save();
			}
		}

		boolean writable = !settingDataHolder.getItem("SC_Common", "SI_ImportExportPath").equals("???") && Helpers.isImportExportDirWritable(settingDataHolder);

		if(writable)
		{
			try {
				File impExpDir = Helpers.getImportExportDir(settingDataHolder);

				if(settingDataHolder.getPersistentDataObject("FAVOURITES_ENCFILES") == null && Build.VERSION.SDK_INT > 20 && Build.VERSION.SDK_INT < 30) {
					settingDataHolder.addOrReplacePersistentDataObject("FAVOURITES_ENCFILES",
							new CryptFileWrapper(new CryptFile(impExpDir.getAbsolutePath() + File.separator + getResources().getString(R.string.filesSubdir))));
				}

				if(settingDataHolder.getItem("SC_PasswordVault", "SI_AutoBackup").equals("///"))
					settingDataHolder.addOrReplaceItem("SC_PasswordVault", "SI_AutoBackup", Helpers.getImportExportPath(settingDataHolder));

			} catch (Exception e) {
				// swallow
			}
		}
		else if(Helpers.isAndroid11PorHflavor())
		{
			try {
				getExternalMediaDirs(); // create media dirs

				File[] externalMedia = getExternalMediaDirs();
				settingDataHolder.addOrReplaceItem("SC_Common", "SI_ImportExportPath", externalMedia[0].getAbsolutePath());

				File impExpDir = externalMedia[0];
				CryptFileWrapper impExpDirWrapper = new CryptFileWrapper(new CryptFile(externalMedia[0]));
				Helpers.getImportExportDir(settingDataHolder);

				if(settingDataHolder.getPersistentDataObject("FAVOURITES_ENCFILES") == null)
					settingDataHolder.addOrReplacePersistentDataObject("FAVOURITES_ENCFILES",
							new CryptFileWrapper(new CryptFile(impExpDir.getAbsolutePath() + File.separator + getResources().getString(R.string.filesSubdir))));

				if(settingDataHolder.getItem("SC_PasswordVault", "SI_AutoBackup").equals("///"))
					settingDataHolder.addOrReplaceItem("SC_PasswordVault", "SI_AutoBackup", Helpers.getImportExportPath(settingDataHolder));

				settingDataHolder.save();
				Helpers.isImportExportDirWritable(settingDataHolder);

			} catch (Exception e) {
				// swallow
			}
		}

	}

	private void launchApplication()
	{
		makeAndVerifyImportExportDir();

		final Intent intent = getIntent();
    	if(intent == null) {
    		runMainActivity();
    		return;
    	}  	
    	
    	final Bundle bundle = intent.getExtras();
    	final String action = intent.getAction();
        final String type = intent.getType();
        
        boolean allowedActions = (action != null && (
        			action.equals(Intent.ACTION_SEND) || 
        			action.equals(Intent.ACTION_SEND_MULTIPLE) ||
        			action.equals(Intent.ACTION_VIEW) ||
        			action.equals(Intent.ACTION_EDIT)));
		
    	if(allowedActions) 
    	{
    		List<CryptFileWrapper> externalFiles = null;
    		
        	Object rawData = null;   	
        	if(bundle != null) rawData = bundle.get(Intent.EXTRA_STREAM); 
    		if(intent.getData() != null || rawData != null)
    			externalFiles = FileEncActivity.getExternalFilesFromIntent(intent, this);
    		
    		if(externalFiles != null)
    		{
	    		android.net.Uri data = intent.getData();
	    		Bundle extras = intent.getExtras();
	
	    		Intent newIntent = new Intent(this, FileEncActivity.class);
	    		newIntent.setAction(action);
	    		if(type != null) newIntent.setType(type);
	    		newIntent.setData(data);
	    		if(extras != null) newIntent.putExtras(extras);
	    		startActivity(newIntent);
    		}
    		/*
    		else if(action.equals(Intent.ACTION_SEND) && type != null && type.equals("text/plain"))
    		{
	    		android.net.Uri data = intent.getData();
	    		Bundle extras = intent.getExtras();
    			
    			Intent newIntent = new Intent(this, MessageEncActivity.class);
	    		newIntent.setAction(action);
	    		if(type != null) newIntent.setType(type);
	    		newIntent.setData(data);
	    		if(extras != null) newIntent.putExtras(extras);
	    		startActivity(newIntent);
    		}
    		*/
    		else {
    			runMainActivity();
    		}
    	}  	
    	else {
    		runMainActivity();
    	}
	}
	
	private void runMainActivity()
	{
		Intent newIntent = new Intent(this, MainActivity.class);
		newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(newIntent);
        finish();
	}
	
	@Override
	void processMessage() {}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 333) {
			if (Build.VERSION.SDK_INT >= 30) {
				if (Environment.isExternalStorageManager()) {
					startLaunchWorkflow();
				}
				else {
					Handler mdHandler = new Handler()
					{
						public void handleMessage(Message msg)
						{
							if (msg.what == 1)
							{
								if(StaticApp.VERSION_FLAVOR.equals("H")) {
									startLaunchWorkflow();
								}
								else {
									readyForDestroy = true;
									finish();
								}
							}
						}
					};

					Dialog md = ComponentProvider.getShowMessageDialog(
							this,
							mdHandler, getResources().getString(R.string.common_message_text),
							getResources().getString(R.string.common_permission_files_explanation)
									+ (StaticApp.VERSION_FLAVOR.equals("H") ? "<br/><br/>" + getResources().getString(R.string.common_permission_files_explanationAppendix) : ""),
							ComponentProvider.DRAWABLE_ICON_CANCEL, null);
					md.setCancelable(false);
					md.show();
				}
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
	{
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if((Build.VERSION.SDK_INT < 30 &&grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) || (Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()))
		{
			startLaunchWorkflow();
		}
		else if(Build.VERSION.SDK_INT >= 23)
		{
			boolean showRationale = permissions.length > 0 && shouldShowRequestPermissionRationale(permissions[0]);

			if(!showRationale)
			{
				Handler mdHandler = new Handler()
				{
					public void handleMessage(Message msg)
					{
						if (msg.what == 1)
						{
							if(StaticApp.VERSION_FLAVOR.equals("H")) {
								startLaunchWorkflow();
							}
							else {
								readyForDestroy = true;
								finish();
							}
						}
					}
				};

				Dialog md = ComponentProvider.getShowMessageDialog(
						this,
						mdHandler, getResources().getString(R.string.common_message_text),
						getResources().getString(R.string.common_permission_files_explanation) + "<br/><br/><font color = 'yellow'>" + getResources().getString(R.string.common_permission_files_goToSettings) + "</font>",
						ComponentProvider.DRAWABLE_ICON_CANCEL, null);

				md.show();
			}
			else
			{
				Handler mdHandler = new Handler()
				{
					public void handleMessage(Message msg)
					{
						if (msg.what == 1)
						{
							if(StaticApp.VERSION_FLAVOR.equals("H")) {
								startLaunchWorkflow();
							}
							else {
								readyForDestroy = true;
								finish();
							}
						}
					}
				};

				Dialog md = ComponentProvider.getShowMessageDialog(
						this,
						mdHandler, getResources().getString(R.string.common_message_text),
						getResources().getString(R.string.common_permission_files_explanation)
								+ (StaticApp.VERSION_FLAVOR.equals("H") ? "<br/><br/>" + getResources().getString(R.string.common_permission_files_explanationAppendix) : ""),
						ComponentProvider.DRAWABLE_ICON_CANCEL, null);
				md.setCancelable(false);
				md.show();
			}
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
