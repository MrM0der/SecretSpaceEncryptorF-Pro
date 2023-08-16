package com.paranoiaworks.unicus.android.sse.dao;

import com.paranoiaworks.unicus.android.sse.misc.StringSentinel;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Part of Password Vault object structure
 * Folder Password Vault - code has similar structure as VaultV2.java
 * Keeps VaultItems objects
 * 
 * @author Paranoia Works
 * @version 2.0.2
 * @related VaultV2.java, VaultItemV2.java
 */
public class VaultFolderV2 implements Serializable, Comparable<VaultFolderV2> {

	private static final long serialVersionUID = VaultV2.serialVersionUID;
	
	public static final int VAULTFOLDER_ATTRIBUTE_POSITION = 201;

	private static final String TAG_FOLDER_NAME = "NAME";
	private static final String TAG_COMMENT = "CMT";
	private static final String TAG_COLOR_CODE = "CLR";
	private static final String TAG_DATE_CREATED = "DC";
	private static final String TAG_DATE_MODIFIED = "DM";
	private List<VaultItemV2> items;
	private Map folderMainMap;
	private Map<Integer, Object> folderAttributeMap;
	
	private transient boolean lockedDataChanges = false;
	private transient boolean isSearchResult = false;
	
	public VaultFolderV2()
	{
		this.items = new ArrayList<VaultItemV2>();
		this.folderMainMap = new HashMap();
		this.folderAttributeMap = new HashMap<Integer, Object>();
		this.folderMainMap.put(TAG_DATE_CREATED, System.currentTimeMillis());
		this.folderMainMap.put(TAG_FOLDER_NAME, new StringSentinel("???".toCharArray(), VaultV2.SS_GROUP_ID));
		this.folderMainMap.put(TAG_COMMENT, new StringSentinel("???".toCharArray(), VaultV2.SS_GROUP_ID));
		this.folderMainMap.put(TAG_COLOR_CODE, -1);
	}
	
	public int getItemCount()
	{
		return items.size();
	}
	
	public boolean addItem (VaultItemV2 vi)
	{
		if(lockedDataChanges) return false;
		lockedDataChanges = true;
		
		items.add(vi);
		
		lockedDataChanges = false;
		return true;
	}
	
	public boolean removeItemWithIndex(int i, String hashCode)
	{
		if(lockedDataChanges) return false;
		lockedDataChanges = true;
		
		VaultItemV2 vi = getItemByIndex(i);
		
		if(!hashCode.equals(vi.getItemSecurityHash())) return false;
		
		items.remove(i);
		
		lockedDataChanges = false;
		return true;
	}
	
	public void notifyItemDataSetChanged()
	{
		Collections.sort(items);
	}
	
	public List<VaultItemV2> getItemList()
	{
		return items;
	}
	
	public VaultItemV2 getItemByIndex(int i)
	{
		return items.get(i);
	}

	public Integer getIndexByReference(VaultItemV2 item)
	{
		for(int i = 0; i < getItemCount(); ++i) {
			if(item == getItemByIndex(i)) return i;
		}
		return null;
	}
	
	public char[] getFolderName()
	{
		return ((StringSentinel)folderMainMap.get(TAG_FOLDER_NAME)).getString();
	}

	public StringSentinel getFolderNameSS()
	{
		return (StringSentinel)folderMainMap.get(TAG_FOLDER_NAME);
	}
	
	public void setFolderName(char[] folderName)
	{
		folderMainMap.put(TAG_FOLDER_NAME, new StringSentinel(Helpers.trim(folderName), VaultV2.SS_GROUP_ID));
		setDateModified();
	}
	
	public char[] getFolderComment()
	{
		return ((StringSentinel)folderMainMap.get(TAG_COMMENT)).getString();
	}
	
	public void setFolderComment(char[] folderComment)
	{
		folderMainMap.put(TAG_COMMENT, new StringSentinel(Helpers.trim(folderComment), VaultV2.SS_GROUP_ID));
		setDateModified();
	}

	public int getColorCode()
	{
		return (int)folderMainMap.get(TAG_COLOR_CODE);
	}

	public void setColorCode(int colorCode)
	{
		folderMainMap.put(TAG_COLOR_CODE, colorCode);
		setDateModified();
	}

	public long getDateCreated()
	{
		return (long)folderMainMap.get(TAG_DATE_CREATED);
	}

	public long getDateModified()
	{
		return (long)folderMainMap.get(TAG_DATE_MODIFIED);
	}

	public void setDateModified()
	{
		folderMainMap.put(TAG_DATE_MODIFIED, System.currentTimeMillis());
	}

	public void setDateModified(Date date)
	{
		folderMainMap.put(TAG_DATE_MODIFIED, date.getTime());
	}
	
	public String getFolderSecurityHash()
	{
		StringSentinel tempDataSS = ((StringSentinel)folderMainMap.get(TAG_FOLDER_NAME));
		byte[] tempData = tempDataSS == null ? new byte[1] : tempDataSS.getEncryptedData();
		return VaultV2.getBlake2Hash(Helpers.concat(tempData, Helpers.longToBytes(getDateCreated()), Helpers.longToBytes(this.hashCode())), 256);
	}

	public Integer getItemPosition(String itemHash) throws IllegalStateException
	{
		int counter = 0;
		Integer position = null;
		for(int i = 0; i < getItemCount(); ++i)
		{
			if(getItemByIndex(i).getItemSecurityHash().equalsIgnoreCase(itemHash))
			{
				position = i;
				++counter;
			}
		}
		if(counter > 1) throw new IllegalStateException("Hash Collision");

		return position;
	}

	public boolean isSearchResult()
	{
		return this.isSearchResult;
	}

	public void setIsSearchResult(boolean isSearchResult)
	{
		this.isSearchResult = isSearchResult;
	}
	
	public Object getAttribute(int attributeID)
	{
		return folderAttributeMap.get(attributeID);
	}
	
	public void setAttribute(int attributeID, Object attribute)
	{
		folderAttributeMap.put(attributeID, attribute);
		setDateModified();
	}

	public Map<Integer, Object> getAttributes()
	{
		return folderAttributeMap;
	}

	public void setAttributes(Map<Integer, Object> attributes)
	{
		folderAttributeMap = attributes;
	}
	
	public int compareTo(VaultFolderV2 folder)
	{
		int i = this.getFolderNameSS().compareTo(folder.getFolderNameSS());
		if(i != 0) return i;
		if(this.getColorCode() > folder.getColorCode()) return 1;
		if(this.getColorCode() < folder.getColorCode()) return -1;
		if(this.getDateCreated() > folder.getDateCreated()) return 1;
		else return -1;
	}
}
