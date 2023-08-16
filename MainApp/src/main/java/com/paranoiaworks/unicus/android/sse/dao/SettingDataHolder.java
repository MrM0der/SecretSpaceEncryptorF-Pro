package com.paranoiaworks.unicus.android.sse.dao;

import android.content.Context;
import android.content.res.Resources;

import com.paranoiaworks.unicus.android.sse.R;
import com.paranoiaworks.unicus.android.sse.StaticApp;
import com.paranoiaworks.unicus.android.sse.config.SettingsUpdater;
import com.paranoiaworks.unicus.android.sse.utils.DBHelper;
import com.paranoiaworks.unicus.android.sse.utils.Encryptor;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Keeps Application Settings
 * 
 * @author Paranoia Works
 * @version 1.3.2
 * @related DB table BLOB_REP(ID = SETTINGS), data.xml
 */
public class SettingDataHolder implements Serializable {

	private static final long serialVersionUID = 10L;	
	public static final String SDH_DBPREFIX = "SETTINGS";
	private static Lock lock = new ReentrantLock();
	private static SettingDataHolder INSTANCE = null;
	private static Map<String, Object> sessionCacheObjectMap = null;
		
	private long dateCreated;
	private Map<String, String> data = new HashMap<String, String>();
	private Map<String, Object> persistentDataObjectMap;
	private transient DBHelper dbHelper;
	
	private SettingDataHolder() 
	{
		this.dateCreated = System.currentTimeMillis();
	}	 

	private static void init(DBHelper dbHelper)
	{
		if(!dbHelper.isDBReady()) throw new Error("DB is not ready");
		SettingDataHolder sdhTemp = loadSDH(dbHelper); // try to load from DB

		if (sdhTemp != null) {
			INSTANCE = sdhTemp;
		}
		else {
			INSTANCE = new SettingDataHolder(); // if nothing in DB, create the new one
		}

		INSTANCE.setDBHelper(dbHelper);
		INSTANCE.loadDefaultValues();
			
		SettingsUpdater.update(INSTANCE);
	}

	/** Get only instance of the SettingDataHolder */
	public static synchronized SettingDataHolder getInstance(DBHelper dbHelper)
	{
		lock.lock();
		try {
			if(INSTANCE == null) init(dbHelper);
			return INSTANCE;
		} finally {
			lock.unlock();
		}
	}

	/** Get Session Data Object if Available */
	public static Object getSessionCacheObject(String key)
	{
		if(sessionCacheObjectMap == null ) createSessionCacheObjectMap();
		return sessionCacheObjectMap.get(key);
	}

	/** Add or Replace Session Data Object */
	public static void addOrReplaceSessionCacheObject(String key, Object dataObject)
	{
		if(sessionCacheObjectMap == null ) createSessionCacheObjectMap();
		Object test = sessionCacheObjectMap.get(key);
		if(test != null) sessionCacheObjectMap.remove(key);
		sessionCacheObjectMap.put(key, dataObject);
	}

	/** Replace current Instance with new - only for really special tasks */
	public void replaceInstance(SettingDataHolder injectedSdh)
	{
		lock.lock();
		INSTANCE = injectedSdh;
		save();
		init(dbHelper);
		lock.unlock();
	}

	/** Get DBHelper */
	public DBHelper getDBHelper()
	{
		return this.dbHelper;
	}

	/** Set DBHelper */
	private void setDBHelper(DBHelper dbHelper)
	{
		this.dbHelper = dbHelper;
	}
	
	/** Get data when SettingDataHolder object has been created */
	public long getDateCreated() {
		return dateCreated;
	}
	
	/** Get number of stored Settings Items */
	public int getItemsCount() {
		return data.size();
	}
	
	/** Get Item by Category and Item names - as String value */
	public String getItem(String categoryName, String itemName)
	{
		String itemCode = categoryName + ":" + itemName;
		return data.get(itemCode);
	}
	
	/** Get Item by Category and Item names - as int value */
	public int getItemAsInt(String categoryName, String itemName)
	{
		return Integer.parseInt(getItem(categoryName, itemName));
	}
	
	/** Get Item by Category and Item names - as boolean value */
	public boolean getItemAsBoolean(String categoryName, String itemName)
	{
		String boolString = getItem(categoryName, itemName);
		if(boolString == null) return false;
		return Boolean.parseBoolean(boolString);
	}
	
	/** Add or Replace Item identified by Category and Item names */
	public void addOrReplaceItem(String categoryName, String itemName, String item)
	{
		lock.lock();
		try {
			String itemCode = categoryName + ":" + itemName;
			String test = data.get(itemCode);
			if(test != null) data.remove(itemCode);
			data.put(itemCode, item);
		} finally {
			lock.unlock();
		}
	}
	
	/** Get Item Value NAME (for selector) */
	public String getItemValueName(String categoryName, String itemName)
	{
		return getItemValueNames(categoryName, itemName).get(
				Integer.parseInt(getItem(categoryName, itemName)));
	}
	
	/** Get Item Value Names List */
	public List<String> getItemValueNames(String categoryName, String itemName)
	{
		List<String> names = new ArrayList<String>();
		Context cn = dbHelper.getContext();
		Resources res = cn.getResources();
    	String[] settingsItemsTemp = res.getStringArray(R.array.settings_items_pro);
    	
        for(String settingItem : settingsItemsTemp) 
        { 
        	String[] parAndDir = settingItem.split("!!");
        	String[] parameters = parAndDir[0].split("\\|\\|");
        	if(!parameters[0].equals(categoryName) || !parameters[1].equals(itemName)) continue;	
        	names = Arrays.asList(parameters[4].split("\\|"));
        }
        return names;
	}
	
	/** Get Persistent Data Object */
	public Object getPersistentDataObject(String key)
	{
		if(persistentDataObjectMap == null ) createPersistentDataObjectMap();
		return persistentDataObjectMap.get(key);
	}

	/** Get Persistent Data Object as Boolean */
	public boolean getPersistentDataObjectAsBoolean(String key)
	{
		Boolean bool = (Boolean)getPersistentDataObject(key);
		if(bool == null) return false;
		return bool;
	}
	
	/** Add or Replace Persistent Data Object */
	public void addOrReplacePersistentDataObject(String key, Object dataObject)
	{
		lock.lock();
		try {
			if(persistentDataObjectMap == null ) createPersistentDataObjectMap();
			Object test = persistentDataObjectMap.get(key);
			if(test != null) persistentDataObjectMap.remove(key);
			persistentDataObjectMap.put(key, dataObject);
		} finally {
			lock.unlock();
		}
	}

	public int getLicenceLevel() {
		Integer licenseLevel = (Integer)getPersistentDataObject(StaticApp.LICENSE_LEVEL_TAG);
		if(licenseLevel != null) return licenseLevel;
		else return 0;
	}

	private void createPersistentDataObjectMap()
	{
		persistentDataObjectMap = Collections.synchronizedMap(new HashMap<String, Object>());
	}

	private static void createSessionCacheObjectMap()
	{
		sessionCacheObjectMap = Collections.synchronizedMap(new HashMap<String, Object>());
	}
		
	/** Get default values for the newly created SettingDataHolder object or Update Current (from data.xml) */
	private void loadDefaultValues()
	{
		Context cn = dbHelper.getContext();
		Resources res = cn.getResources();
    	String[] settingsItemsTemp = res.getStringArray(R.array.settings_items_pro);
    	
    	String itemCode = null;
    	String[] parameters = null;
    	String defaultValue = null;
    	
        for(String settingItem : settingsItemsTemp) 
        { 
        	String[] parAndDir = settingItem.split("!!");
        	parameters = parAndDir[0].split("\\|\\|");
        	itemCode = parameters[0] + ":" + parameters[1];
        	defaultValue = parameters[parameters.length - 1];
    		
    		if(data.get(itemCode) != null) continue;
    		data.put(itemCode, defaultValue);   	
        } 
	}
	
	/** Load SettingDataHolder object from DB*/
	private static SettingDataHolder loadSDH(DBHelper dbHelper)
	{
		try {
			StringBuffer dbhs = new StringBuffer();
			ArrayList otherData = new ArrayList();
			byte[] sdh = dbHelper.getBlobData(SDH_DBPREFIX, dbhs, otherData);
			int version = ((Integer)otherData.get(0)).intValue();

			SettingDataHolder settingDataHolder = null;

			if(version == 0)
				settingDataHolder = (SettingDataHolder)Encryptor.unzipObject(sdh, null);
			else if(version == 1)
				settingDataHolder = (SettingDataHolder)Encryptor.decompressObjectLZMA(sdh);

			return settingDataHolder;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/** Save SettingDataHolder object to DB*/
	public synchronized void save() 
	{
		lock.lock();
		byte[] shd;
		try {
			SettingDataHolder sdhForSave = INSTANCE;
			if(sdhForSave == null || sdhForSave.getItemsCount() < 1) throw new Error("sdhForSave is invalid");
			shd = Encryptor.compressObjectLZMA(sdhForSave);
			dbHelper.insertUpdateBlobData(SDH_DBPREFIX, shd, 1);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
	}
}
