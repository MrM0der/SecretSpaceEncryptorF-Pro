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
import com.paranoiaworks.unicus.android.sse.misc.StringSentinel;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;

import sse.org.bouncycastle.crypto.digests.Blake2bDigest;

/**
 * Root object of Password Vault object structure
 * Keeps VaultFolder objects
 * 
 * @author Paranoia Works
 * @version 2.0.1
 * @related VaultfolderV2.java, VaultItemV2.java
 */
public final class VaultV2 implements Serializable {
	
	protected static final long serialVersionUID = 20L;
	public static final int SS_GROUP_ID = 0;
	public static final String id = "ROOT_" + serialVersionUID;
	
	public static final int COMMENT_MAXCHARS = 8000; // XML import
	public static final int URL_MAXCHARS = 2048;
	public static final int CE_NAME_MAXCHARS = 300;
	public static final int CE_VALUE_MAXCHARS = 2048;
	public static final int KEM_SECRET_MAXCHARS = 300;
	public static final int KEM_KEYS_MAXCHARS = 20000;

	private transient String stampHash = null;
	private transient boolean lockedDataChanges = false;
	
	private List<VaultFolderV2> folders;
	private long dateCreated;
	private Map<Integer, Object> vaultFutureMap; // for other attributes (future attributes)
	
	
	private VaultV2()
	{
		this.dateCreated = System.currentTimeMillis();
		this.folders = new ArrayList<VaultFolderV2>();
		this.vaultFutureMap = new HashMap<Integer, Object>();
	}	 
	 
	public static VaultV2 getInstance()
	{
		return new VaultV2();
	}
	
	public static VaultV2 getInstance(String xml, CryptActivity cryptActivity) throws DataFormatException
	{
		VaultV2 vaultInstance = getInstance();
		
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
				VaultFolderV2 newFolder = new VaultFolderV2();
				newFolder.setFolderName(folderNameS.toCharArray());
				newFolder.setFolderComment(folderCommentS.toCharArray());
				if(folderPosition != null) newFolder.setAttribute(VaultFolderV2.VAULTFOLDER_ATTRIBUTE_POSITION, folderPosition);
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
					String itemPasswordS = null;
					try{ itemPasswordS = getValueByTagName(tempNode, "Password");} catch (Exception e){}
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
					VaultItemV2 newItem= new VaultItemV2();
					newItem.setItemName(itemNameS.toCharArray());
					newItem.setItemPassword(itemPasswordS.toCharArray());
					newItem.setItemComment(itemCommentS.toCharArray());
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

						newItem.setItemAccount(itemAccountS.toCharArray());
						newItem.setItemURL(itemUrlS.toCharArray());

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

								newItem.addCustomElement(ceNameS.toCharArray(), ceValueS.toCharArray());
							}
						}
					}

					// KEM Item
					if(tempNode.getNodeName().equals("KemItem"))
					{
						newItem.enableKemItem();

						String itemAlgorithmS = getValueByTagName(tempNode, "Algorithm");
						String itemPrivateKeyS = getValueByTagName(tempNode, "PrivateKey");
						String itemPublicKeyS = getValueByTagName(tempNode, "PublicKey");
						String itemSecretS = getValueByTagName(tempNode, "Secret");
						String itemSecretEncapsulatedS = getValueByTagName(tempNode, "SecretEncapsulated");
						String itemSecretExtractedS = getValueByTagName(tempNode, "SecretExtracted");

						if(itemAlgorithmS == null) itemAlgorithmS = "";
						if(itemAlgorithmS.length() > 300 || itemAlgorithmS.length() < 1) throw new DataFormatException("Item(" + i + "/" + j + "):Algorithm: " + cryptActivity.getStringResource("common_xmlParsing_incorrectSize") + "1-300");
						
						if(itemPrivateKeyS == null) itemPrivateKeyS = "";
						if(itemPrivateKeyS.length() > KEM_KEYS_MAXCHARS) throw new DataFormatException("Item(" + i + "/" + j + "):PrivateKey: " + cryptActivity.getStringResource("common_xmlParsing_incorrectSize") + " (>" + KEM_KEYS_MAXCHARS + ")");

						if(itemPublicKeyS == null) itemPublicKeyS = "";
						if(itemPublicKeyS.length() > KEM_KEYS_MAXCHARS) throw new DataFormatException("Item(" + i + "/" + j + "):PublicKey: " + cryptActivity.getStringResource("common_xmlParsing_incorrectSize") + " (>" + KEM_KEYS_MAXCHARS + ")");

						if(itemSecretS == null) itemSecretS = "";
						if(itemSecretS.length() > KEM_SECRET_MAXCHARS) throw new DataFormatException("Item(" + i + "/" + j + "):Secret: " + cryptActivity.getStringResource("common_xmlParsing_incorrectSize") + " (>" + KEM_SECRET_MAXCHARS + ")");

						if(itemSecretEncapsulatedS == null) itemSecretEncapsulatedS = "";
						if(itemSecretEncapsulatedS.length() > KEM_KEYS_MAXCHARS) throw new DataFormatException("Item(" + i + "/" + j + "):SecretEncapsulated: " + cryptActivity.getStringResource("common_xmlParsing_incorrectSize") + " (>" + KEM_KEYS_MAXCHARS + ")");

						if(itemSecretExtractedS == null) itemSecretExtractedS = "";
						if(itemSecretExtractedS.length() > KEM_SECRET_MAXCHARS) throw new DataFormatException("Item(" + i + "/" + j + "):SecretExtractedS: " + cryptActivity.getStringResource("common_xmlParsing_incorrectSize") + " (>" + KEM_SECRET_MAXCHARS + ")");

						newItem.setItemKemAlgorithm(itemAlgorithmS.toCharArray());
						newItem.setItemKemPrivateKey(itemPrivateKeyS.toCharArray());
						newItem.setItemKemPublicKey(itemPublicKeyS.toCharArray());
						newItem.setItemKemSharedSecret(itemSecretS.toCharArray());
						newItem.setItemKemSharedSecretEncapsulated(itemSecretEncapsulatedS.toCharArray());
						newItem.setItemKemSharedSecretExtracted(itemSecretExtractedS.toCharArray());
					}

					newFolder.addItem(newItem);
				}
				newFolder.notifyItemDataSetChanged();
			}
			
		} catch (Exception e) {
			//e.printStackTrace();
			throw new DataFormatException(e.getLocalizedMessage());
		}
		
		vaultInstance.notifyFolderDataSetChanged();
		
		return vaultInstance;
	}
	
	public int getFolderCount()
	{
		return folders.size();
	}
	
	public boolean addFolder (VaultFolderV2 vf)
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
		
		VaultFolderV2 vf = getFolderByIndex(i);
		
		if(!hashCode.equals(vf.getFolderSecurityHash())) return false;
		
		folders.remove(i);
		
		lockedDataChanges = false;
		return true;
	}
	
	/** Notify Folder Data Changed to perform Sorting */
	public void notifyFolderDataSetChanged()
	{
		List<VaultFolderV2> alphabeticalOrder = new ArrayList<VaultFolderV2>();
		Map<Integer, VaultFolderV2> customOrder = new HashMap<Integer, VaultFolderV2>();
		List<VaultFolderV2> outputList = new ArrayList<VaultFolderV2>();
		
    	for(VaultFolderV2 vaultFolder : folders)
        {    
    		Integer order = (Integer)vaultFolder.getAttribute(VaultFolderV2.VAULTFOLDER_ATTRIBUTE_POSITION);
    		if(order == null || order == 0 || order > folders.size())
    		{
    			vaultFolder.setAttribute(VaultFolderV2.VAULTFOLDER_ATTRIBUTE_POSITION, null); //reset position to default
    			alphabeticalOrder.add(vaultFolder);
    		}
    		else
    		{
    			VaultFolderV2 testExistFolder = customOrder.get(order);
    			if(testExistFolder == null)
    				customOrder.put(order, vaultFolder);
    			else if(testExistFolder.getDateModified() > vaultFolder.getDateModified())
    			{
    				vaultFolder.setAttribute(VaultFolderV2.VAULTFOLDER_ATTRIBUTE_POSITION, null); //reset position to default
    				alphabeticalOrder.add(vaultFolder);
    			}
    			else
    			{
    				testExistFolder.setAttribute(VaultFolderV2.VAULTFOLDER_ATTRIBUTE_POSITION, null); //reset position to default
    				alphabeticalOrder.add(testExistFolder);
    				customOrder.put(order, vaultFolder);
    			}			
    		}
        }
    	
		Collections.sort(alphabeticalOrder);
		
		try {
			Iterator<VaultFolderV2> alphabeticalOrderIterator = alphabeticalOrder.listIterator();
			for(int i = 1; i <= folders.size(); i++)
			{
				VaultFolderV2 tempFolder = customOrder.get(i);
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
	
	public VaultFolderV2 getFolderByIndex(int i)
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

	/** Generate Hash Stamp for current object state */	
	public String generateNewStampHash()
    {
		stampHash = getBlake2Hash(Helpers.longToBytes(System.currentTimeMillis()), 128);
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
			VaultFolderV2 vf = this.getFolderByIndex(i);
			xml.append("\t<Folder>\n");
			
			xml.append("\t\t<Name>");
			xml.append("<![CDATA[" + escapeCdataEndToken(String.valueOf(vf.getFolderName())) + "]]>");
			xml.append("</Name>\n");
			xml.append("\t\t<Comment>");
			xml.append("<![CDATA[" + escapeCdataEndToken(String.valueOf(vf.getFolderComment())) + "]]>");
			xml.append("</Comment>\n");
			xml.append("\t\t<Position>");
			Integer position = (Integer)vf.getAttribute(VaultFolderV2.VAULTFOLDER_ATTRIBUTE_POSITION);
			if(position != null) xml.append(position);
			xml.append("</Position>\n");
			xml.append("\t\t<IconCode>");
			xml.append(vf.getColorCode());
			xml.append("</IconCode>\n");	
			
			xml.append("\t\t<Items>\n");
			for(int j = 0; j < vf.getItemCount(); ++j)
			{
				VaultItemV2 vi = vf.getItemByIndex(j);
				if(vi.isExtendedItem()) xml.append("\t\t\t<ExtendedItem>\n");
				else if(vi.isKemItem()) xml.append("\t\t\t<KemItem>\n");
				else xml.append("\t\t\t<Item>\n");
				
				xml.append("\t\t\t\t");
				xml.append("<Name>");
				xml.append("<![CDATA[" + escapeCdataEndToken(String.valueOf(vi.getItemName())) + "]]>");
				xml.append("</Name>\n");
				xml.append("\t\t\t\t");
				if(vi.isExtendedItem()) {
					xml.append("<Account>");
					xml.append("<![CDATA[" + escapeCdataEndToken(String.valueOf(vi.getItemAccount())) + "]]>");
					xml.append("</Account>\n");
					xml.append("\t\t\t\t");
				}
				if(!vi.isKemItem()) {
					xml.append("<Password>");
					xml.append("<![CDATA[" + escapeCdataEndToken(String.valueOf(vi.getItemPassword())) + "]]>");
					xml.append("</Password>\n");
					xml.append("\t\t\t\t");
				}
				if(vi.isExtendedItem()) {
					xml.append("<URL>");
					xml.append("<![CDATA[" + escapeCdataEndToken(String.valueOf(vi.getItemUrl())) + "]]>");
					xml.append("</URL>\n");
					xml.append("\t\t\t\t");
				}
				if(vi.isKemItem()) {
					xml.append("<Algorithm>");
					xml.append("<![CDATA[" + escapeCdataEndToken(String.valueOf(vi.getItemKemAlgorithm())) + "]]>");
					xml.append("</Algorithm>\n");
					xml.append("\t\t\t\t");
					char[] kemTemp = vi.getItemKemPrivateKey();
					if(kemTemp != null) {
						xml.append("<PrivateKey>");
						xml.append("<![CDATA[" + escapeCdataEndToken(String.valueOf(kemTemp)) + "]]>");
						xml.append("</PrivateKey>\n");
						xml.append("\t\t\t\t");
					}
					kemTemp = vi.getItemKemPublicKey();
					if(kemTemp != null) {
						xml.append("<PublicKey>");
						xml.append("<![CDATA[" + escapeCdataEndToken(String.valueOf(kemTemp)) + "]]>");
						xml.append("</PublicKey>\n");
						xml.append("\t\t\t\t");
					}
					kemTemp = vi.getItemKemSharedSecret();
					if(kemTemp != null) {
						xml.append("<Secret>");
						xml.append("<![CDATA[" + escapeCdataEndToken(String.valueOf(kemTemp)) + "]]>");
						xml.append("</Secret>\n");
						xml.append("\t\t\t\t");
					}
					kemTemp = vi.getItemKemSharedSecretEncapsulated();
					if(kemTemp != null) {
						xml.append("<SecretEncapsulated>");
						xml.append("<![CDATA[" + escapeCdataEndToken(String.valueOf(kemTemp)) + "]]>");
						xml.append("</SecretEncapsulated>\n");
						xml.append("\t\t\t\t");
					}
					kemTemp = vi.getItemKemSharedSecretExtracted();
					if(kemTemp != null) {
						xml.append("<SecretExtracted>");
						xml.append("<![CDATA[" + escapeCdataEndToken(String.valueOf(kemTemp)) + "]]>");
						xml.append("</SecretExtracted>\n");
						xml.append("\t\t\t\t");
					}
				}
				xml.append("<Comment>");
				xml.append("<![CDATA[" + escapeCdataEndToken(String.valueOf(vi.getItemComment())) + "]]>");
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
						xml.append("<![CDATA[" + escapeCdataEndToken(String.valueOf(((StringSentinel[])vi.getCustomElements().get(k))[0].getString())) + "]]>");
						xml.append("</Name>\n");
						xml.append("\t\t\t\t\t\t");
						xml.append("<Value>");
						xml.append("<![CDATA[" + escapeCdataEndToken(String.valueOf(((StringSentinel[])vi.getCustomElements().get(k))[1].getString())) + "]]>");
						xml.append("</Value>\n");
						xml.append("\t\t\t\t\t</Element>\n");
					}
					xml.append("\t\t\t\t</CustomElements>\n");
				}

				if(vi.isExtendedItem()) xml.append("\t\t\t</ExtendedItem>\n");
				else if(vi.isKemItem()) xml.append("\t\t\t</KemItem>\n");
				else xml.append("\t\t\t</Item>\n");
			}
			xml.append("\t\t</Items>\n");
			xml.append("\t</Folder>\n");
		}
		
		xml.append("</Vault>");
		return xml.toString();
	}

	protected static String getBlake2Hash(byte[] data, int outputSizeBits)
	{
		byte[] hash = new byte[outputSizeBits / 8];
		Blake2bDigest digester = new Blake2bDigest(outputSizeBits);
		digester.update(data, 0, data.length);
		digester.doFinal(hash, 0);
		return Helpers.byteArrayToHexString(hash);
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
