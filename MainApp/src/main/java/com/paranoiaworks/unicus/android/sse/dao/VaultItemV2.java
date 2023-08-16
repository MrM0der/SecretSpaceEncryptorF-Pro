package com.paranoiaworks.unicus.android.sse.dao;

import com.paranoiaworks.unicus.android.sse.misc.StringSentinel;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Leaf object of Password Vault object structure
 * Item Password Vault - code has similar structure as VaultV2.java
 * 
 * @author Paranoia Works
 * @version 2.0.3
 * @related VaultV2.java, VaultFolderV2.java
 */
public class VaultItemV2 implements Serializable, Comparable<VaultItemV2>{

	private static final long serialVersionUID = VaultV2.serialVersionUID;

	public static final int SEARCH_IN_TITLE = 0;
	public static final int SEARCH_IN_TITLE_ACCOUNT = 1;
	public static final int SEARCH_IN_URL = 2;
	public static final int SEARCH_IN_ALL = 3;
	
	private static final String TAG_ITEM_NAME = "NAME";
	private static final String TAG_PASSWORD = "PWD";
	private static final String TAG_COMMENT = "CMT";
	private static final String TAG_ACCOUNT = "ACC";
	private static final String TAG_URL = "URL";
	private static final String TAG_COLOR_CODE = "CLR";
	private static final String TAG_DATE_CREATED = "DC";
	private static final String TAG_DATE_MODIFIED = "DM";
	private static final String TAG_KEM_ALGORITHM = "KEM_ALG";
	private static final String TAG_KEM_PRIVATE_KEY = "KEM_SK";
	private static final String TAG_KEM_PUBLIC_KEY = "KEM_PK";
	private static final String TAG_KEM_SHARED_SECRET = "KEM_SS";
	private static final String TAG_KEM_SHARED_SECRET_ENCAPSULATED = "KEM_SSE";
	private static final String TAG_KEM_SHARED_SECRET_EXTRACTED = "KEM_SSEX";
	private static final String TAG_EXTENDED_ITEM_FLAG = "EXT";
	private static final String TAG_KEM_ITEM_FLAG = "KEM";
	private Map itemMainMap;
	private List<StringSentinel[]> customElements;
	private int special = 0;
	private transient boolean selected = false;
	private transient VaultFolderV2 parentFolder; // Only for search result purposes

	public static final int SPEC_GOBACKITEM = 1;
	public static final int SPEC_NEWITEM = 2;
	public static final int SPEC_SEARCH = 3;
	
	public VaultItemV2()
	{
		this.itemMainMap = new HashMap();
		this.itemMainMap.put(TAG_DATE_CREATED, System.currentTimeMillis());
		this.itemMainMap.put(TAG_ITEM_NAME, new StringSentinel("".toCharArray(), VaultV2.SS_GROUP_ID));
		this.itemMainMap.put(TAG_PASSWORD, new StringSentinel("".toCharArray(), VaultV2.SS_GROUP_ID));
		this.itemMainMap.put(TAG_COMMENT, new StringSentinel("".toCharArray(), VaultV2.SS_GROUP_ID));
		this.itemMainMap.put(TAG_COLOR_CODE, -1);
	}

	public char[] getItemName()
	{
		return ((StringSentinel)itemMainMap.get(TAG_ITEM_NAME)).getString();
	}

	public StringSentinel getItemNameSS()
	{
		return (StringSentinel)itemMainMap.get(TAG_ITEM_NAME);
	}

	public void setItemName(char[] itemName)
	{
		itemMainMap.put(TAG_ITEM_NAME, new StringSentinel(Helpers.trim(itemName), VaultV2.SS_GROUP_ID));
	}

	public char[] getItemPassword()
	{
		return ((StringSentinel)itemMainMap.get(TAG_PASSWORD)).getString();
	}

	public StringSentinel getItemPasswordSS()
	{
		return (StringSentinel)itemMainMap.get(TAG_PASSWORD);
	}

	public void setItemPassword(char[] itemPassword)
	{
		if(itemPassword != null)
			itemMainMap.put(TAG_PASSWORD, new StringSentinel(Helpers.trim(itemPassword), VaultV2.SS_GROUP_ID));
	}

	public char[] getItemComment()
	{
		return ((StringSentinel)itemMainMap.get(TAG_COMMENT)).getString();
	}

	public void setItemComment(char[] itemComment)
	{
		if(itemComment != null)
			itemMainMap.put(TAG_COMMENT, new StringSentinel(Helpers.trim(itemComment), VaultV2.SS_GROUP_ID));
	}

	public int getColorCode()
	{
		return (int)itemMainMap.get(TAG_COLOR_CODE);
	}

	public void setColorCode(int colorCode)
	{
		itemMainMap.put(TAG_COLOR_CODE, colorCode);
	}

	public long getDateModified()
	{
		return (long)itemMainMap.get(TAG_DATE_MODIFIED);
	}

	public void setDateModified()
	{
		itemMainMap.put(TAG_DATE_MODIFIED, System.currentTimeMillis());
	}
	
	public void setDateModified(Date date)
	{
		itemMainMap.put(TAG_DATE_MODIFIED, date.getTime());
	}

	public Map getItemMainMap()
	{
		return itemMainMap;
	}

	public void addOrReplaceItemMainMap(Map itemMainMap)
	{
		this.itemMainMap = itemMainMap;
	}

	public long getDateCreated()
	{
		return (long)itemMainMap.get(TAG_DATE_CREATED);
	}

	//+ Extended Item
	public void enableExtendedItem()
	{
		this.itemMainMap.put(TAG_EXTENDED_ITEM_FLAG, true);
	}

	public boolean isExtendedItem()
	{
		Boolean test = (Boolean)itemMainMap.get(TAG_EXTENDED_ITEM_FLAG);
		if(test == null || !test) return false;
		else return true;
	}

	public char[] getItemAccount()
	{
		StringSentinel data = (StringSentinel)itemMainMap.get(TAG_ACCOUNT);
		if(data != null) return data.getString();
		else return "".toCharArray();
	}

	public StringSentinel getItemAccountSS()
	{
		return (StringSentinel)itemMainMap.get(TAG_ACCOUNT);
	}

	public void setItemAccount(char[] account)
	{
		if(account != null)
			itemMainMap.put(TAG_ACCOUNT, new StringSentinel(Helpers.trim(account), VaultV2.SS_GROUP_ID));
	}

	public char[] getItemUrl()
	{
		StringSentinel data = (StringSentinel)itemMainMap.get(TAG_URL);
		if(data != null) return data.getString();
		else return "".toCharArray();
	}

	public void setItemURL(char[] url)
	{
		if(url != null)
			itemMainMap.put(TAG_URL, new StringSentinel(Helpers.trim(url), VaultV2.SS_GROUP_ID));
	}

	public void addOrReplaceCustomElements(List<StringSentinel[]> customElements)
	{
		this.customElements = customElements;
	}

	public void addCustomElement(char[] name, char[] value)
	{
		StringSentinel[] element = new StringSentinel[2];
		element[0] = new StringSentinel(Helpers.trim(name), VaultV2.SS_GROUP_ID);
		element[1] = new StringSentinel(Helpers.trim(value), VaultV2.SS_GROUP_ID);
		addCustomElement(element);
	}

	public void addCustomElement(StringSentinel[] element)
	{
		getCustomElements().add(element);
	}

	public List<StringSentinel[]> getCustomElements()
	{
		if(customElements == null) customElements = new ArrayList<StringSentinel[]>();
		return customElements;
	}

	public List<StringSentinel[]> getCustomElementsClone()
	{
		List celClone = new ArrayList<StringSentinel[]>();

		for(int i = 0; i < getCustomElements().size(); ++i)
		{
			StringSentinel[] currentElement = (StringSentinel[])customElements.get(i);
			StringSentinel[] currentElementClone = new StringSentinel[2];
			try {
				currentElementClone[0] = (StringSentinel)currentElement[0].clone();
				currentElementClone[1] = (StringSentinel)currentElement[1].clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}

			celClone.add(currentElementClone);
		}

		return celClone;
	}
	//- Extended Item

	//+ KEM Item
	public void enableKemItem()
	{
		this.itemMainMap.put(TAG_KEM_ITEM_FLAG, true);
	}

	public boolean isKemItem()
	{
		Boolean test = (Boolean)itemMainMap.get(TAG_KEM_ITEM_FLAG);
		if(test == null || !test) return false;
		else return true;
	}

	public char[] getItemKemAlgorithm()
	{
		StringSentinel data = (StringSentinel)itemMainMap.get(TAG_KEM_ALGORITHM);
		if(data != null) return data.getString();
		else return "".toCharArray();
	}

	public void setItemKemAlgorithm(char[] kemAlgorithm)
	{
		if(kemAlgorithm != null)
			itemMainMap.put(TAG_KEM_ALGORITHM, new StringSentinel(Helpers.trim(kemAlgorithm), VaultV2.SS_GROUP_ID));
	}

	public char[] getItemKemPrivateKey()
	{
		StringSentinel data  = (StringSentinel)itemMainMap.get(TAG_KEM_PRIVATE_KEY);
		if(data != null) return data.getString();
		else return "".toCharArray();
	}

	public void setItemKemPrivateKey(char[] kemPrivateKey)
	{
		if(kemPrivateKey != null)
			itemMainMap.put(TAG_KEM_PRIVATE_KEY, new StringSentinel(Helpers.trim(kemPrivateKey), VaultV2.SS_GROUP_ID));
	}

	public char[] getItemKemPublicKey()
	{
		StringSentinel data = (StringSentinel)itemMainMap.get(TAG_KEM_PUBLIC_KEY);
		if(data != null) return data.getString();
		else return "".toCharArray();
	}

	public void setItemKemPublicKey(char[] kemPublicKey)
	{
		if(kemPublicKey != null)
			itemMainMap.put(TAG_KEM_PUBLIC_KEY, new StringSentinel(Helpers.trim(kemPublicKey), VaultV2.SS_GROUP_ID));
	}

	public char[] getItemKemSharedSecret()
	{
		StringSentinel data = (StringSentinel)itemMainMap.get(TAG_KEM_SHARED_SECRET);
		if(data != null) return data.getString();
		else return "".toCharArray();
	}

	public void setItemKemSharedSecret(char[] kemSharedSecret)
	{
		if(kemSharedSecret != null)
			itemMainMap.put(TAG_KEM_SHARED_SECRET, new StringSentinel(Helpers.trim(kemSharedSecret), VaultV2.SS_GROUP_ID));
	}

	public char[] getItemKemSharedSecretEncapsulated()
	{
		StringSentinel data = (StringSentinel)itemMainMap.get(TAG_KEM_SHARED_SECRET_ENCAPSULATED);
		if(data != null) return data.getString();
		else return "".toCharArray();
	}

	public void setItemKemSharedSecretEncapsulated(char[] kemSharedSecretEncapsulated)
	{
		if(kemSharedSecretEncapsulated != null)
			itemMainMap.put(TAG_KEM_SHARED_SECRET_ENCAPSULATED, new StringSentinel(Helpers.trim(kemSharedSecretEncapsulated), VaultV2.SS_GROUP_ID));
	}

	public char[] getItemKemSharedSecretExtracted()
	{
		StringSentinel data = (StringSentinel)itemMainMap.get(TAG_KEM_SHARED_SECRET_EXTRACTED);
		if(data != null) return data.getString();
		else return "".toCharArray();
	}

	public void setItemKemSharedSecretExtracted(char[] kemSharedSecretExtracted)
	{
		if(kemSharedSecretExtracted != null)
			itemMainMap.put(TAG_KEM_SHARED_SECRET_EXTRACTED, new StringSentinel(Helpers.trim(kemSharedSecretExtracted), VaultV2.SS_GROUP_ID));
	}
	//- KEM Item

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public String getItemSecurityHash()
	{
		StringSentinel tempDataSS = ((StringSentinel)itemMainMap.get(TAG_ITEM_NAME));
		byte[] tempData = tempDataSS == null ? new byte[1] : tempDataSS.getEncryptedData();
		return VaultV2.getBlake2Hash(Helpers.concat(tempData, Helpers.longToBytes(getDateCreated()), Helpers.longToBytes(this.hashCode())), 256);
	}

	public VaultFolderV2 getParentFolder()
	{
		return this.parentFolder;
	}

	public void setParentFolder(VaultFolderV2 parentFolder)
	{
		this.parentFolder = parentFolder;
	}
	
	public boolean isSpecial()
	{
		if(special == 0) return false;
		return true;
	}
	
	public int getSpecialCode() 
	{
		return this.special;
	}
	
	public static VaultItemV2 getSpecial(int code)
	{
		return getSpecial(code, null);
	}
	
	/** Get "Go Back" or "New Item" item */
	public static VaultItemV2 getSpecial(int code, Object tag)
	{
		VaultItemV2 specItem = null;
		switch (code) 
        {
	        case SPEC_GOBACKITEM: // "go to Folder Layout Item" (first in list of Items)
	        	specItem = new VaultItemV2();
	        	specItem.special = SPEC_GOBACKITEM;
	        	specItem.setItemName((char[])tag);
	        	break;
	        case SPEC_NEWITEM:  // "create new item Item" (last in list of Items)
	        	specItem = new VaultItemV2();
	        	specItem.special = SPEC_NEWITEM;
	        	break;
			case SPEC_SEARCH:  // "search" (last in list of Items for search results)
				specItem = new VaultItemV2();
				specItem.special = SPEC_SEARCH;
				break;
	        default: return null;
        }
		return specItem;
	}

	public int searchIndexOf(char [] searchFor, int searchIn)
	{
		switch (searchIn)
		{
			case SEARCH_IN_TITLE: {
				String[] tagList = {TAG_ITEM_NAME};
				return search(searchFor, tagList, false);
			}
			case SEARCH_IN_TITLE_ACCOUNT: {
				String[] tagList = {TAG_ITEM_NAME, TAG_ACCOUNT};
				return search(searchFor, tagList, false);
			}
			case SEARCH_IN_URL: {
				String[] tagList = {TAG_URL};
				return search(searchFor, tagList, false);
			}
			case SEARCH_IN_ALL: {
				String[] tagList = {TAG_ITEM_NAME, TAG_ACCOUNT, TAG_URL, TAG_COMMENT, TAG_KEM_ALGORITHM};
				return search(searchFor, tagList, true);
			}
			default: return -1;
		}
	}

	private int search(char [] searchFor, String[] tagList, boolean extendedItems)
	{
		int index = -1;
		for(int i = 0; i < tagList.length; ++i) {
			StringSentinel currentSS = (StringSentinel)itemMainMap.get(tagList[i]);
			if(currentSS != null) index = currentSS.searchIndexOf(searchFor);
			if(index > -1) break;
		}
		if(extendedItems && index < 0 && customElements != null) {
			for (int i = 0; i < customElements.size(); ++i) {
				StringSentinel[] currentElement = (StringSentinel[]) customElements.get(i);
				index = ((StringSentinel) currentElement[0]).searchIndexOf(searchFor);
				if (index > -1) break;
				index = ((StringSentinel) currentElement[1]).searchIndexOf(searchFor);
				if (index > -1) break;
			}
		}
		return index;
	}
	
	public int compareTo(VaultItemV2 item)
	{
		int i = this.getItemNameSS().compareTo(item.getItemNameSS());
		if(i != 0) return i;
		if(this.getColorCode() > item.getColorCode()) return 1;
		if(this.getColorCode() < item.getColorCode()) return -1;
		if(this.getDateCreated() > item.getDateCreated()) return 1;
		else return -1;
	}
}
