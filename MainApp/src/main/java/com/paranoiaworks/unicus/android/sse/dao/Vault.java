package com.paranoiaworks.unicus.android.sse.dao;

import java.io.Serializable;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.DataFormatException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

import com.paranoiaworks.unicus.android.sse.CryptActivity;
import com.paranoiaworks.unicus.android.sse.StaticApp;

/**
 * Root object of Password Vault object structure
 * Keeps VaultFolder objects
 *
 * OBSOLETE - Replaced by Version 2 - VaultV2.java
 * 
 * @author Paranoia Works
 * @version 1.0.7
 * @related Vaultfolder.java, VaultItem.java
 */
public final class Vault implements Serializable{
	
	protected static final long serialVersionUID = 10L;
	public static final String id = "ROOT_" + serialVersionUID;
	private static int activeInstanceCounter = 0;
	
	public static final int COMMENT_MAXCHARS = 8000; // XML import
	public static final int URL_MAXCHARS = 2048;
	public static final int CE_NAME_MAXCHARS = 300;
	public static final int CE_VALUE_MAXCHARS = 2048;

	private transient String stampHash = null;
	private transient boolean lockedDataChanges = false;
	private transient boolean integrityCheckFailed = false;
	
	private List<VaultFolder> folders;
	private long dateCreated;
	private Map<Integer, Object> vaultFutureMap; // for other attributes (future attributes)
	
	
	private Vault() 
	{
		this.dateCreated = System.currentTimeMillis();
		this.folders = new ArrayList<VaultFolder>();
		this.vaultFutureMap = new HashMap<Integer, Object>();
	}	 
	 
	public static Vault getInstance() 
	{
		++activeInstanceCounter;
		return new Vault();
	}
	
	public static Vault getInstance(String xml, CryptActivity cryptActivity) throws DataFormatException
	{
		Vault vaultInstance = getInstance();
		
		InputSource src = new InputSource(new StringReader(xml)); 
		Node tempNode = null;
		
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			dbFactory.setIgnoringComments(true);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(src);
			doc.getDocumentElement().normalize();
					
			NodeList rootNodeList = doc.getChildNodes();
			for(int i = 0; i < rootNodeList.getLength(); ++i)
			{
				tempNode = rootNodeList.item(i);
				if(isWhiteSpaceTextNode(tempNode)) continue;
				break;
			}
			
			NodeList folderList = tempNode.getChildNodes();
			for(int i = 0; i < folderList.getLength(); ++i) //Folders
			{
				tempNode = folderList.item(i);

				if(isWhiteSpaceTextNode(tempNode)) continue;
				if(!tempNode.hasChildNodes()) continue;
				
				String folderNameS = getValueByTagName(tempNode, "Name");
				String folderCommentS = getValueByTagName(tempNode, "Comment");
				String folderPositionS = getValueByTagName(tempNode, "Position");
				String folderIconCodeS= getValueByTagName(tempNode, "IconCode");
				
				if(folderNameS == null || folderNameS.equals("")) throw new DataFormatException("Folder(" + i + "):Name: " + cryptActivity.getStringResource("common_xmlParsing_incorrectData"));
				if(folderNameS.length() > 300) throw new DataFormatException("Folder(" + i + "):Name: " + cryptActivity.getStringResource("common_xmlParsing_incorrectSize") + " (>300)");
				
				if(folderCommentS == null) folderCommentS = "";
				if(folderCommentS.length() > COMMENT_MAXCHARS) throw new DataFormatException("Folder(" + i + "):Comment: " + cryptActivity.getStringResource("common_xmlParsing_incorrectSize") + " (>" + COMMENT_MAXCHARS + ")");
				
				Integer folderPosition = null;
				if(folderPositionS == null) folderPositionS = "";
				try{folderPosition = Integer.parseInt(folderPositionS);} catch (Exception e){}
				if(!folderPositionS.equals("") && folderPosition == null) 
					throw new DataFormatException("Folder(" + i + "):Position: " + cryptActivity.getStringResource("common_xmlParsing_incorrectData") + " " + folderPositionS);
				
				Integer folderIconCode = null;
				if(folderIconCodeS == null) folderIconCodeS = "";
				try{folderIconCode = Integer.parseInt(folderIconCodeS);} catch (Exception e){}
				if(!folderIconCodeS.equals("") && folderIconCode == null) 
					throw new DataFormatException("Folder(" + i + "):IconCode: " + cryptActivity.getStringResource("common_xmlParsing_incorrectData") + " " + folderIconCodeS);
				
				// Set Folder Values
				VaultFolder newFolder = new VaultFolder();
				newFolder.setFolderName(folderNameS);
				newFolder.setFolderComment(folderCommentS);
				if(folderPosition != null) newFolder.setAttribute(VaultFolder.VAULTFOLDER_ATTRIBUTE_POSITION, folderPosition);
				if(folderIconCode != null) newFolder.setColorCode(folderIconCode);
				vaultInstance.addFolder(newFolder);
						
				NodeList itemList = null;
				try{itemList = ((Element)tempNode).getElementsByTagName("Items").item(0).getChildNodes();} catch (Exception e){}
				if(itemList == null) throw new DataFormatException("Items List: " + cryptActivity.getStringResource("common_xmlParsing_incorrectData"));
				for(int j = 0; j < itemList.getLength(); ++j) // Items
				{
					tempNode = itemList.item(j);
					if(isWhiteSpaceTextNode(tempNode)) continue;
					if(!tempNode.hasChildNodes()) continue;

					String itemNameS = getValueByTagName(tempNode, "Name");
					String itemPasswordS = getValueByTagName(tempNode, "Password");
					String itemCommentS = getValueByTagName(tempNode, "Comment");
					String itemIconCodeS = getValueByTagName(tempNode, "IconCode");
					String itemModifiedS = getValueByTagName(tempNode, "Modified");
					
					if(itemNameS == null || itemNameS.equals("")) throw new DataFormatException("Item(" + i + "/" + j + "):Name: " + cryptActivity.getStringResource("common_xmlParsing_incorrectData"));
					if(itemNameS.length() > 300) throw new DataFormatException("Item(" + i + "/" + j + "):Name: " + cryptActivity.getStringResource("common_xmlParsing_incorrectSize") + " (>300)");
					
					if(itemPasswordS == null) itemPasswordS = "";
					if(itemPasswordS.length() > 1024) throw new DataFormatException("Item(" + i + "/" + j + "):Password: " + cryptActivity.getStringResource("common_xmlParsing_incorrectSize") + " (>1024)");
					
					if(itemCommentS == null) itemCommentS = "";
					if(itemCommentS.length() > COMMENT_MAXCHARS) throw new DataFormatException("Item(" + i + "/" + j + "):Comment: " + cryptActivity.getStringResource("common_xmlParsing_incorrectSize") + " (>" + COMMENT_MAXCHARS + ")");
					
					Integer itemIconCode = null;
					if(itemIconCodeS == null) itemIconCodeS = "";
					try{itemIconCode = Integer.parseInt(itemIconCodeS);} catch (Exception e){}
					if(!itemIconCodeS.equals("") && itemIconCode == null) 
						throw new DataFormatException("Item(" + i + "/" + j + "):IconCode: " + cryptActivity.getStringResource("common_xmlParsing_incorrectData") + " " + itemIconCodeS);
					
					Date itemModified = null;
					if(itemModifiedS == null) itemModifiedS = "";
					SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
					try{itemModified = dateParser.parse(itemModifiedS.replaceAll("T", " "));} catch (Exception e){}
					if(!itemModifiedS.equals("") && itemModified == null) 
						throw new DataFormatException("Item(" + i + "/" + j + "):Modified: " + cryptActivity.getStringResource("common_xmlParsing_incorrectData") + " " + itemModifiedS);
					if(itemModified == null) itemModified = new Date();
					
					// Set Item Values
					VaultItem newItem= new VaultItem();
					newItem.setItemName(itemNameS);
					newItem.setItemPassword(itemPasswordS);
					newItem.setItemComment(itemCommentS);
					newItem.setDateModified(itemModified);
					if(itemIconCode != null) newItem.setColorCode(itemIconCode);

					// Extended Item
					if(tempNode.getNodeName().equals("ExtendedItem"))
					{
						newItem.enableExtendedItem();

						String itemAccountS = getValueByTagName(tempNode, "Account");
						String itemUrlS = getValueByTagName(tempNode, "URL");

						if(itemAccountS == null) itemAccountS = "";
						if(itemAccountS.length() > 300) throw new DataFormatException("Item(" + i + "/" + j + "):Account: " + cryptActivity.getStringResource("common_xmlParsing_incorrectSize") + " (>300)");

						if(itemUrlS == null) itemUrlS = "";
						if(itemUrlS.length() > URL_MAXCHARS) throw new DataFormatException("Item(" + i + "/" + j + "):URL: " + cryptActivity.getStringResource("common_xmlParsing_incorrectSize") + " (>" + URL_MAXCHARS + ")");

						newItem.setItemAccount(itemAccountS);
						newItem.setItemURL(itemUrlS);

						NodeList customElementsList = null;
						try{customElementsList = ((Element)tempNode).getElementsByTagName("CustomElements").item(0).getChildNodes();} catch (Exception e){}

						if(customElementsList != null)
						{
							for (int k = 0; k < customElementsList.getLength(); ++k) // Custom Elements
							{
								tempNode = customElementsList.item(k);
								if (isWhiteSpaceTextNode(tempNode)) continue;
								if (!tempNode.hasChildNodes()) continue;

								String ceNameS = getValueByTagName(tempNode, "Name");
								String ceValueS = getValueByTagName(tempNode, "Value");

								if(ceNameS == null) ceNameS = "";
								if(ceNameS.length() > CE_NAME_MAXCHARS) throw new DataFormatException("Item(" + i + "/" + j + "/" + k + "):Custom Element Name: " + cryptActivity.getStringResource("common_xmlParsing_incorrectSize") + " (>" + CE_NAME_MAXCHARS + ")");

								if(ceValueS == null) ceValueS = "";
								if(ceValueS.length() > CE_VALUE_MAXCHARS) throw new DataFormatException("Item(" + i + "/" + j + "/" + k + "):Custom Element Value: " + cryptActivity.getStringResource("common_xmlParsing_incorrectSize") + " (>" + CE_VALUE_MAXCHARS + ")");

								newItem.addCustomElement(ceNameS, ceValueS);
							}
						}
					}

					newFolder.addItem(newItem);
				}
				newFolder.notifyItemDataSetChanged();
			}
			
		} catch (Exception e) {
			throw new DataFormatException(e.getLocalizedMessage());
		}
		
		vaultInstance.notifyFolderDataSetChanged();
		
		return vaultInstance;
	}
	
	public int getFolderCount()
	{
		return folders.size();
	}
	
	public boolean addFolder (VaultFolder vf)
	{
		if(lockedDataChanges) return false;
		lockedDataChanges = true;
		
		folders.add(vf);
		
		lockedDataChanges = false;
		return true;
	}
	
	public boolean removeFolderWithIndex(int i, String hashCode)
	{
		if(lockedDataChanges) return false;
		lockedDataChanges = true;
		
		VaultFolder vf = getFolderByIndex(i);
		
		if(!hashCode.equals(vf.getFolderSecurityHash())) return false;
		
		folders.remove(i);
		
		lockedDataChanges = false;
		return true;
	}
	
	/** Notify Folder Data Changed to perform Sorting */
	public void notifyFolderDataSetChanged()
	{
		List<VaultFolder> alphabeticalOrder = new ArrayList<VaultFolder>();
		Map<Integer, VaultFolder> customOrder = new HashMap<Integer, VaultFolder>();
		List<VaultFolder> outputList = new ArrayList<VaultFolder>();
		
    	for(VaultFolder vaultFolder : folders)
        {    
    		Integer order = (Integer)vaultFolder.getAttribute(VaultFolder.VAULTFOLDER_ATTRIBUTE_POSITION);
    		if(order == null || order == 0 || order > folders.size())
    		{
    			vaultFolder.setAttribute(VaultFolder.VAULTFOLDER_ATTRIBUTE_POSITION, null); //reset position to default
    			alphabeticalOrder.add(vaultFolder);
    		}
    		else
    		{
    			VaultFolder testExistFolder = customOrder.get(order);
    			if(testExistFolder == null)
    				customOrder.put(order, vaultFolder);
    			else if(testExistFolder.getDateModified() > vaultFolder.getDateModified())
    			{
    				vaultFolder.setAttribute(VaultFolder.VAULTFOLDER_ATTRIBUTE_POSITION, null); //reset position to default
    				alphabeticalOrder.add(vaultFolder);
    			}
    			else
    			{
    				testExistFolder.setAttribute(VaultFolder.VAULTFOLDER_ATTRIBUTE_POSITION, null); //reset position to default
    				alphabeticalOrder.add(testExistFolder);
    				customOrder.put(order, vaultFolder);
    			}			
    		}
        }
    	
		Collections.sort(alphabeticalOrder);
		
		try {
			Iterator<VaultFolder> alphabeticalOrderIterator = alphabeticalOrder.listIterator();
			for(int i = 1; i <= folders.size(); i++)
			{
				VaultFolder tempFolder = customOrder.get(i);
				if(tempFolder != null) outputList.add(tempFolder);
				else outputList.add(alphabeticalOrderIterator.next());
			}
		} catch (Exception e) {
			outputList = null;
			e.printStackTrace();
		}

		if(outputList != null && folders.size() == outputList.size()) folders = outputList;
		else Collections.sort(folders);
	}
	
	public VaultFolder getFolderByIndex(int i)
	{
		return folders.get(i);
	}
	
	public long getDateCreated() {
		return dateCreated;
	}
	
	public String getCurrentStampHash()
	{
		return stampHash;
	}
	
	public void setStampHashFromDB(String dbsh)
	{
		if(stampHash != null) throw new IllegalStateException("Current StampHash has to be null.");
		stampHash = dbsh;
	}
	
	public boolean isIntegrityCheckFailed() {
		return integrityCheckFailed;
	}

	public void setIntegrityCheckFailed(boolean integrityCheckFailed) {
		this.integrityCheckFailed = integrityCheckFailed;
	}

	/** Generate Hash Stamp for current object state */	
	public String generateNewStampHash()
    {
    	String timeStamp = getMD5Hash(Long.toHexString(System.currentTimeMillis()));
    	stampHash = timeStamp;
    	return stampHash;
    }
	
	/** Get Vault structure and data as XML */
	public String asXML()
	{
		StringBuilder xml = new StringBuilder();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
		xml.append("<Vault xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"https://paranoiaworks.mobi/sse/xsd/ssevault.xsd\">\n");
		
		for(int i = 0; i < this.getFolderCount(); ++i)
		{
			VaultFolder vf = this.getFolderByIndex(i);
			xml.append("\t<Folder>\n");
			
			xml.append("\t\t<Name>");
			xml.append("<![CDATA[" + escapeCdataEndToken(vf.getFolderName()) + "]]>");
			xml.append("</Name>\n");
			xml.append("\t\t<Comment>");
			xml.append("<![CDATA[" + escapeCdataEndToken(vf.getFolderComment()) + "]]>");
			xml.append("</Comment>\n");
			xml.append("\t\t<Position>");
			Integer position = (Integer)vf.getAttribute(VaultFolder.VAULTFOLDER_ATTRIBUTE_POSITION);
			if(position != null) xml.append(position);
			xml.append("</Position>\n");
			xml.append("\t\t<IconCode>");
			xml.append(vf.getColorCode());
			xml.append("</IconCode>\n");	
			
			xml.append("\t\t<Items>\n");
			for(int j = 0; j < vf.getItemCount(); ++j)
			{
				VaultItem vi = vf.getItemByIndex(j);
				if(vi.isExtendedItem()) xml.append("\t\t\t<ExtendedItem>\n");
					else xml.append("\t\t\t<Item>\n");
				
				xml.append("\t\t\t\t");
				xml.append("<Name>");
				xml.append("<![CDATA[" + escapeCdataEndToken(vi.getItemName()) + "]]>");
				xml.append("</Name>\n");
				xml.append("\t\t\t\t");
				if(vi.isExtendedItem()) {
					xml.append("<Account>");
					xml.append("<![CDATA[" + escapeCdataEndToken(vi.getItemAccount()) + "]]>");
					xml.append("</Account>\n");
					xml.append("\t\t\t\t");
				}
				xml.append("<Password>");
				xml.append("<![CDATA[" + escapeCdataEndToken(vi.getItemPassword()) + "]]>");
				xml.append("</Password>\n");
				xml.append("\t\t\t\t");
				if(vi.isExtendedItem()) {
					xml.append("<URL>");
					xml.append("<![CDATA[" + escapeCdataEndToken(vi.getItemUrl()) + "]]>");
					xml.append("</URL>\n");
					xml.append("\t\t\t\t");
				}
				xml.append("<Comment>");
				xml.append("<![CDATA[" + escapeCdataEndToken(vi.getItemComment()) + "]]>");
				xml.append("</Comment>\n");
				xml.append("\t\t\t\t");
				xml.append("<Modified>");
				xml.append(sdf.format(vi.getDateModified()).replaceAll("\\s", "T"));
				xml.append("</Modified>\n");
				xml.append("\t\t\t\t");
				xml.append("<IconCode>");
				xml.append(vi.getColorCode());
				xml.append("</IconCode>\n");
				if(vi.isExtendedItem())
				{
					List cel = vi.getCustomElements();
					xml.append("\t\t\t\t<CustomElements>\n");
					for (int k = 0; k < cel.size(); ++k )
					{
						xml.append("\t\t\t\t\t<Element>\n");
						xml.append("\t\t\t\t\t\t");
						xml.append("<Name>");
						xml.append("<![CDATA[" + escapeCdataEndToken(((String[])vi.getCustomElements().get(k))[0]) + "]]>");
						xml.append("</Name>\n");
						xml.append("\t\t\t\t\t\t");
						xml.append("<Value>");
						xml.append("<![CDATA[" + escapeCdataEndToken(((String[])vi.getCustomElements().get(k))[1]) + "]]>");
						xml.append("</Value>\n");
						xml.append("\t\t\t\t\t</Element>\n");
					}
					xml.append("\t\t\t\t</CustomElements>\n");
				}

				if(vi.isExtendedItem()) xml.append("\t\t\t</ExtendedItem>\n");
					else xml.append("\t\t\t</Item>\n");
			}
			xml.append("\t\t</Items>\n");
			xml.append("\t</Folder>\n");
		}
		
		xml.append("</Vault>");
		return xml.toString();
	}
	
	protected static String getMD5Hash (String s)
	{
    	byte[] text = s.getBytes();
    	MessageDigest m = null;
		try {
			m = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	    m.update(text, 0, text.length);
	    String hash = new BigInteger(1, m.digest()).toString(16);
		while (hash.length() < 32) hash = "0" + hash;
		return hash;
	}
	
	/* XML parsing helper */
	private static boolean isWhiteSpaceTextNode(Node node)
	{
		if (node instanceof Text) {
	        String value = node.getNodeValue().trim();
	        if (value.equals("") ) {
	            return true;
	        }
	    }
		return false;
	}
	
	/* XML parsing helper */
	private static String getValueByTagName(Node node, String tagName) throws DataFormatException
	{
		String value = null;
		if (node instanceof Element) {
			try {				
				StringBuilder builder = new StringBuilder();
				NodeList nodeList = ((Element)node).getElementsByTagName(tagName).item(0).getChildNodes();
				for(int i = 0; i < nodeList.getLength(); ++i) {
					builder.append(nodeList.item(i).getNodeValue());
				}
				if(builder.length() > 0) value = builder.toString().trim();
				
			} catch (Exception e) {
				throw new DataFormatException(e.getLocalizedMessage());
			}
	    }
		return value;
	}
	
	/* XML helper */
	private static String escapeCdataEndToken(String text)
	{
		return text.replaceAll("]]>", "]]]]><![CDATA[>");
	}
}
