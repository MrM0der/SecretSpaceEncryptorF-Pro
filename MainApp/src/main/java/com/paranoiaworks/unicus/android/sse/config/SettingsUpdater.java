package com.paranoiaworks.unicus.android.sse.config;

import com.paranoiaworks.unicus.android.sse.dao.ApplicationStatusBean;
import com.paranoiaworks.unicus.android.sse.dao.SettingDataHolder;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Helper - Update historical settings data to the current version + other updates
 * 
 * @author Paranoia Works
 * @version 1.0.11
 */ 
public class SettingsUpdater {

	public static void update(SettingDataHolder sdh)
	{
		boolean changed = false;
		ApplicationStatusBean asb = sdh.getDBHelper().getAppStatus();
		
		//+ Blowfish-256 and GOST28147 -> Blowfish-448
		try {
			int encryptAlgorithmCode = sdh.getItemAsInt("SC_PasswordVault", "SI_Algorithm");
			if(encryptAlgorithmCode == 3 || encryptAlgorithmCode == 5)
			{
				sdh.addOrReplaceItem("SC_PasswordVault", "SI_Algorithm", Integer.toString(6));
				changed = true;
			}		
			encryptAlgorithmCode = sdh.getItemAsInt("SC_MessageEnc", "SI_Algorithm");
			if(encryptAlgorithmCode == 3 || encryptAlgorithmCode == 5)
			{
				sdh.addOrReplaceItem("SC_MessageEnc", "SI_Algorithm", Integer.toString(6));
				changed = true;
			}	
			encryptAlgorithmCode = sdh.getItemAsInt("SC_FileEnc", "SI_Algorithm");
			if(encryptAlgorithmCode == 3 || encryptAlgorithmCode == 5)
			{
				sdh.addOrReplaceItem("SC_FileEnc", "SI_Algorithm", Integer.toString(6));
				changed = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}			
		//-

		//+ Unsupported algorithms -> Default
		try {
			boolean notPro = sdh.getLicenceLevel() < 1;
			int encryptAlgorithmCode = sdh.getItemAsInt("SC_PasswordVault", "SI_Algorithm");
			if(notPro && (encryptAlgorithmCode == 7 || encryptAlgorithmCode == 8 || encryptAlgorithmCode == 9))
			{
				sdh.addOrReplaceItem("SC_PasswordVault", "SI_Algorithm", Integer.toString(0));
				changed = true;
			}
			encryptAlgorithmCode = sdh.getItemAsInt("SC_MessageEnc", "SI_Algorithm");
			if(notPro && (encryptAlgorithmCode == 7 || encryptAlgorithmCode == 8 || encryptAlgorithmCode == 9))
			{
				sdh.addOrReplaceItem("SC_MessageEnc", "SI_Algorithm", Integer.toString(6));
				changed = true;
			}
			encryptAlgorithmCode = sdh.getItemAsInt("SC_FileEnc", "SI_Algorithm");
			if(notPro && (encryptAlgorithmCode == 7 || encryptAlgorithmCode == 8 || encryptAlgorithmCode == 9))
			{
				sdh.addOrReplaceItem("SC_FileEnc", "SI_Algorithm", Integer.toString(0));
				changed = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		//-
		
		//+ Activate UNICODE password support for chosen languages
		try {
			Locale locale = sdh.getDBHelper().getContext().getResources().getConfiguration().locale;
			
			if(asb.getNumberOfRuns() == 0)
			{
				String languageISO3 = locale.getISO3Language().toLowerCase();
				Set<String> langCodesSet = new HashSet<String>();
				langCodesSet.add("zho");
				langCodesSet.add("jpn");
				langCodesSet.add("kor");
				langCodesSet.add("tha");
				langCodesSet.add("rus");
				langCodesSet.add("ukr");
				langCodesSet.add("heb");
				langCodesSet.add("ell");
				langCodesSet.add("ara");
				langCodesSet.add("hin");
				
				if(langCodesSet.contains(languageISO3))
				{
					sdh.addOrReplaceItem("SC_Common", "SI_AllowUnicodePasswords", Boolean.toString(true));
					changed = true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}			
		//-
		
		//+ Activate screenshot taking protection on Android 5+
		try {
			if(asb.getNumberOfRuns() == 0 && android.os.Build.VERSION.SDK_INT >= 21)
			{
				sdh.addOrReplaceItem("SC_Common", "SI_PreventScreenshots", Boolean.toString(true));
				changed = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}			
		//-

		//+ Activate file exists warning dialog in File Encryptor
		try {
			Boolean warningForced = (Boolean)sdh.getPersistentDataObject("FILE_REPLACE_WARNING_FORCED");
			if(warningForced == null || !warningForced)
			{
				sdh.addOrReplaceItem("SC_FileEnc", "SI_EncFileExistWarning", Boolean.toString(true));
				sdh.addOrReplacePersistentDataObject("FILE_REPLACE_WARNING_FORCED", new Boolean(true));
				changed = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		//-

		//+ Set Enc/Dec folders to "Same as Source" on older Androids
		try {
			if(asb.getNumberOfRuns() == 0 && android.os.Build.VERSION.SDK_INT < 21)
			{
				sdh.addOrReplaceItem("SC_FileEnc", "SI_EncFileDestination", Integer.toString(0));
				sdh.addOrReplaceItem("SC_FileEnc", "SI_DecFileDestination", Integer.toString(0));
				changed = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		//-

		//+ Binary Screen Lock -> Screen lock time
		try {
			boolean screenLockPW = sdh.getItemAsBoolean("SC_PasswordVault", "SI_LockScreen");
			if(screenLockPW) {
				sdh.addOrReplaceItem("SC_PasswordVault", "SI_LockScreenTime", Integer.toString(0));
				sdh.addOrReplaceItem("SC_PasswordVault", "SI_LockScreen", Boolean.toString(false));
				changed = true;
			}
			boolean screenLockTE = sdh.getItemAsBoolean("SC_MessageEnc", "SI_LockScreenTE");
			if(screenLockTE) {
				sdh.addOrReplaceItem("SC_MessageEnc", "SI_LockScreenTimeTE", Integer.toString(0));
				sdh.addOrReplaceItem("SC_MessageEnc", "SI_LockScreenTE", Boolean.toString(false));
				changed = true;
			}
			boolean screenLockFE = sdh.getItemAsBoolean("SC_FileEnc", "SI_LockScreenFE");
			if(screenLockFE) {
				sdh.addOrReplaceItem("SC_FileEnc", "SI_LockScreenTimeFE", Integer.toString(0));
				sdh.addOrReplaceItem("SC_FileEnc", "SI_LockScreenFE", Boolean.toString(false));
				changed = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		//-
		
		if(changed) sdh.save();
	}
}
