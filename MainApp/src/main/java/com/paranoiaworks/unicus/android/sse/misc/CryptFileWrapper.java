package com.paranoiaworks.unicus.android.sse.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;

import android.content.Context;
import android.net.Uri;

import com.paranoiaworks.unicus.android.sse.utils.Encryptor;

import androidx.documentfile.provider.DocumentFile;

/**
 * Wrapper for unifying File And DocumentFile objects  
 * 
 * @author Paranoia Works
 * @version 1.1.4
 */ 
public class CryptFileWrapper implements Comparable<CryptFileWrapper>, Serializable {
	
	private static final long serialVersionUID = 1L;
	public static final int MODE_FILE = 0;
	public static final int MODE_DOCUMENT_FILE = 1;
	
	private int mode = MODE_FILE;

	private CryptFile cryptFile = null;
	private String uriForSerialization;
	private String originalName;
	
	private transient DocumentFile documentFile = null;
	private transient Context context = null;
	private transient long[] cachedDirectoryStats = null;

	private boolean encryptedDF = false;
	private boolean canWrite = true;
	private boolean specialType = false;
	private transient boolean selectedDF = false;
	private transient boolean backDirDF = false;
	private transient boolean unfinishedDF = false;

	private transient String nameCached = null;
	private transient Boolean isDirectoryCached = null;
	private transient Boolean isFileCached = null;
	private transient Long lengthCached = null;
	private transient Long lastModifiedCached = null;
	private transient Integer writePermissionLevelCached = null;
	
	public CryptFileWrapper(CryptFile file) 
	{
		this.cryptFile = file;
		this.mode = MODE_FILE;
		this.originalName = this.getName();
	}
	
	public CryptFileWrapper(DocumentFile documentFile, Context context) 
	{
		this.documentFile = documentFile;
		this.context = context;
		this.mode = MODE_DOCUMENT_FILE;
		this.uriForSerialization = documentFile.getUri().toString();
		this.originalName = this.getName();
		processCommon();
	}
	
	public CryptFileWrapper(String path, int mode, Context context) 
	{
		this.mode = mode;
		if (mode == MODE_DOCUMENT_FILE)
		{
			this.documentFile = DocumentFile.fromFile(new CryptFile(path));
			this.context = context;
			this.uriForSerialization = documentFile.getUri().toString();
			processCommon();
		}
		else
		{
			this.cryptFile = new CryptFile(path);
		}
		this.originalName = this.getName();
	}
	
	public CryptFileWrapper(String dirPath, String name, int mode, Context context) 
	{
		this.mode = mode;
		if (mode == MODE_DOCUMENT_FILE)
		{
			this.documentFile = DocumentFile.fromFile(new CryptFile(dirPath, name));
			this.context = context;
			this.uriForSerialization = documentFile.getUri().toString();
			processCommon();
		}
		else
		{
			this.cryptFile = new CryptFile(dirPath, name);
		}
		this.originalName = this.getName();
	}

	public int getMode() 
	{
		return mode;
	}
	
	public DocumentFile getDocumentFile() 
	{
		return documentFile;
	}
	
	public CryptFile getFile() 
	{
		return cryptFile;
	}
	
	public Context getContext() 
	{
		return context;
	}
	
	public CryptFileWrapper tryToCreateCFWfromSerializationString(Context context)
	{
		CryptFileWrapper cfp = new CryptFileWrapper(DocumentFile.fromTreeUri(context, Uri.parse(uriForSerialization)), context);
		return cfp;
	}
	
	public int getWritePermissionLevelForDir()
	{
		if(!this.isDirectory()) {
			writePermissionLevelCached = 0;
			return writePermissionLevelCached;
		}
		if (mode == MODE_DOCUMENT_FILE)
		{
			return 1;  // not used
		}
		else
		{
			if(writePermissionLevelCached != null) return writePermissionLevelCached;

			boolean ok = writeTestFile(this);
			if(ok) {
				writePermissionLevelCached = 2;
				return writePermissionLevelCached;
			}
			CryptFileWrapper df = new CryptFileWrapper(cryptFile.getAbsolutePath(), MODE_DOCUMENT_FILE, context);
			if(writeTestFile(df)) {
				writePermissionLevelCached = 1;
			}
			else {
				writePermissionLevelCached = 0;
			}
			return writePermissionLevelCached;
		}
	}
	
	public void setCannotWrite()
	{
		this.canWrite = false;
	}

	public String getOriginalName()
	{
		return this.originalName;
	}

	public long[] getCachedDirectoryStats()
	{
		return this.cachedDirectoryStats;
	}
	
	public void setCachedDirectoryStats(long[] directoryStats)
	{
		this.cachedDirectoryStats = directoryStats;
	}

	/** Standard Methods **/
	public boolean isEncrypted() 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			return encryptedDF;
		}
		else
		{
			return cryptFile.isEncrypted();
		}
	}

	public boolean isUnfinished()
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			return unfinishedDF;
		}
		else
		{
			return cryptFile.isUnfinished();
		}
	}

	public boolean isSelected() 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			return selectedDF;
		}
		else
		{
			return cryptFile.isSelected();
		}
	}

	public void setSelected(boolean selected) 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			selectedDF = selected;
		}
		else
		{
			cryptFile.setSelected(selected);
		}	
	}

	public boolean isBackDir() 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			return backDirDF;
		}
		else
		{
			return cryptFile.isBackDir();
		}
	}

	public void setBackDir(boolean backDir) 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			backDirDF = backDir;
		}
		else
		{
			cryptFile.setBackDir(backDir);	
		}
	}
	
	public boolean isSpecialType() 
	{
		return specialType;
	}
	
	public boolean isDirectory() 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			if(isDirectoryCached != null) return isDirectoryCached;
			isDirectoryCached = documentFile.isDirectory();
			return isDirectoryCached;
		}
		else
		{
			return cryptFile.isDirectory();	
		}
	
	}
	
	public boolean isFile() 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			if(isFileCached != null) return isFileCached;
			isFileCached = documentFile.isFile();
			return isFileCached;
		}
		else
		{
			return cryptFile.isFile();	
		}
	
	}
	
	public String getName() 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			if(nameCached != null) return nameCached;

			String name = documentFile.getName();
			if (name == null) 
			{
				specialType = true;
				name = documentFile.getUri().toString();
				if (name != null && name.indexOf(".") > -1)
					name = name.substring(name.lastIndexOf("."), name.length());	
				else name = "***";
			}

			nameCached = name;

			return name;
		}
		else
		{
			return cryptFile.getName();	
		}
	}
	
	public String toString() 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			return documentFile.getName();
		}
		else
		{
			return cryptFile.getName();
		}
	}
	
	public boolean exists() 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			return documentFile.exists();
		}
		else
		{
			return cryptFile.exists();
		}
	}
	
	public boolean existsChild(String childName) 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			if(documentFile.findFile(childName) == null) return false;
			else return true;
		}
		else
		{
			return (new CryptFile(cryptFile.getAbsolutePath() + File.separator + childName)).exists();
		}
	}
	
	public CryptFileWrapper findFile(String childName) 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			DocumentFile docFile = documentFile.findFile(childName);
			if(docFile == null) return null;
			return (new CryptFileWrapper(docFile, context));

		}
		else
		{	CryptFile file = new CryptFile(cryptFile.getAbsolutePath() + File.separator + childName);
			if(!file.exists()) return null;
			return (new CryptFileWrapper(file));
		}
	}
	
	public String getAbsolutePath() 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			return null;
		}
		else 
		{
			return cryptFile.getAbsolutePath();
		}
	}
	
	public CryptFileWrapper getParentFile() 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			DocumentFile parent = documentFile.getParentFile();
			if(documentFile.getParentFile() == null) return null;
			else return new CryptFileWrapper(parent, context);
		}
		else
		{
			if (cryptFile.getParentFile() == null) return null;
			return new CryptFileWrapper(new CryptFile(cryptFile.getParentFile()));	
		}	
	}
	
	public boolean canWrite() 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			return documentFile.canWrite();
		}
		else
		{
			return cryptFile.canWrite() && this.canWrite;
		}	
	}
	
	public long length() 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			return documentFile.length();
		}
		else
		{
			return cryptFile.length();
		}	
	}

	public long lengthCached()
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			if(lengthCached != null) return lengthCached;
			lengthCached = documentFile.length();
			return lengthCached;
		}
		else
		{
			return cryptFile.length();
		}
	}
	
	public long lastModified() 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			if(lastModifiedCached != null) return lastModifiedCached;
			lastModifiedCached = documentFile.lastModified();
			return lastModifiedCached;
		}
		else
		{
			return cryptFile.lastModified();
		}	
	}
	
	public boolean delete() 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			return documentFile.delete();
		}
		else
		{
			return cryptFile.delete();
		}
	}
	
	public String getUniqueIdentifier() 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			return Uri.decode(documentFile.getUri().toString());
		}
		else
		{
			return cryptFile.getAbsolutePath();
		}
	}
	
	public Uri getUri() 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			return documentFile.getUri();
		}
		else
		{
			return Uri.fromFile(cryptFile);
		}
	}
	
	public boolean setLastModified(long time) 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			// not implemented in DocumentFile
			return false;
		}
		else
		{
			return cryptFile.setLastModified(time);
		}
	}

	public boolean renameTo(String name) throws Exception 
	{
		nameCached = null;

		if (mode == MODE_DOCUMENT_FILE)
		{
			return documentFile.renameTo(name);
		}
		else
		{
			CryptFile newFile = new CryptFile(cryptFile.getParent() + File.separator + name);
			if(newFile.exists()) throw new IOException("exists");
			boolean ok = cryptFile.renameTo(newFile);
			cryptFile = newFile;
			cryptFile.processCommon();
			return ok;	
		}
	}

	public CryptFileWrapper createFileWithReplace(String name)
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			CryptFileWrapper child = this.findFile(name);
			if(child != null && child.exists()) child.delete();
			return createFile(name);
		}
		else
		{
			return createFile(name);
		}
	}
	
	public CryptFileWrapper createFile(String name) 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			CryptFileWrapper newFile = new CryptFileWrapper(documentFile.createFile("application/octet-stream", name), context);
			return newFile;
		}
		else
		{
			return new CryptFileWrapper(new CryptFile(cryptFile.getAbsolutePath() + File.separator + name));		
		}
	}
	
	public CryptFileWrapper createDirectory(String name) 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			DocumentFile newDirectory = documentFile.findFile(name);
			if(newDirectory == null) newDirectory = documentFile.createDirectory(name);
			return new CryptFileWrapper(newDirectory, context);
		}
		else
		{
			CryptFile newDir = new CryptFile(cryptFile.getAbsolutePath() + File.separator + name);
			if(newDir.exists() && newDir.isDirectory()) return new CryptFileWrapper(newDir);
						
			int attemptCounter = 0;
			while(true) // Android file system behaves strange sometimes
			{
				++attemptCounter;
				boolean ok = newDir.mkdir();
				if(ok || attemptCounter > 3) break;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}	
			
			if(newDir.exists() && newDir.isDirectory()) return new CryptFileWrapper(newDir);
			else return null;
		}
	}
	
	public CryptFileWrapper createDirectories(String path) 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			String[] dirs = path.split(File.separator);
			DocumentFile newDirectory = documentFile;
			for(int i = 0; i < dirs.length; ++i)
			{
				DocumentFile tempDirectory = newDirectory.findFile(dirs[i]);
				if(tempDirectory == null) tempDirectory = newDirectory.createDirectory(dirs[i]);
				newDirectory = tempDirectory;
			}
			
			return new CryptFileWrapper(newDirectory, context);
		}
		else
		{
			CryptFile newDir = new CryptFile(cryptFile.getAbsolutePath() + File.separator + path);
			if(newDir.exists()) return new CryptFileWrapper(newDir);
			boolean ok = newDir.mkdirs();
			if(ok) return new CryptFileWrapper(newDir);
			else return null;
		}
	}
	
	public InputStream getInputStream() throws IOException 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			return context.getContentResolver().openInputStream(documentFile.getUri());
		}
		else
		{
			return new FileInputStream(cryptFile);	
		}	
	}
	
	public OutputStream getOutputStream() throws IOException 
	{
		if (mode == MODE_DOCUMENT_FILE)
		{
			return context.getContentResolver().openOutputStream(documentFile.getUri());
		}
		else
		{
			return new FileOutputStream(cryptFile);	
		}	
	}
	
	public CryptFileWrapper[] listFiles() 
	{
		return listFiles(false);
	}
	
	public CryptFileWrapper[] listFiles(boolean withWriteCheck) 
	{
		if(!this.isDirectory()) return null;
		if (mode == MODE_DOCUMENT_FILE)
		{
			DocumentFile[] fileList = documentFile.listFiles();
			if(fileList == null) return null;
			CryptFileWrapper[] fileListWrapped = new CryptFileWrapper[fileList.length];
			for(int i = 0; i < fileList.length; ++i) {
				fileListWrapped[i] = new CryptFileWrapper(fileList[i], context);
			}
			return fileListWrapped;
		}
		else
		{
			boolean writeTest = true;
			if (withWriteCheck) writeTest = writeTestFile(this); 
			File[] fileList = cryptFile.listFiles();
			if (fileList == null) return null;
			CryptFileWrapper[] fileListWrapped = new CryptFileWrapper[fileList.length];
			for(int i = 0; i < fileList.length; ++i) {
				CryptFileWrapper newCF = new CryptFileWrapper(new CryptFile(fileList[i]));
				if(!writeTest) newCF.setCannotWrite();
				fileListWrapped[i] = newCF;
			}
			return fileListWrapped;
		}
	}

	public int countFilesInDir()
	{
		if(!this.isDirectory()) return -1;

		if (mode == MODE_DOCUMENT_FILE) {
			DocumentFile[] fileList = documentFile.listFiles();
			if(fileList == null) return -1;
			else return fileList.length;
		}
		else {
			String[] fileNameList = cryptFile.list();
			if(fileNameList == null) return -1;
			else return fileNameList.length;
		}
	}
	
	/* Internal */
	public int compareTo(CryptFileWrapper anotherFile)
	{
		if (this.isBackDir()) return -1;
		if (anotherFile.isBackDir()) return 1;
		
		boolean isThisFileDir = this.isDirectory() ? true : false;
		boolean isAnotherFileDir = anotherFile.isDirectory() ? true : false; 
		
		if (!isAnotherFileDir && isThisFileDir) return -1;
		if (isAnotherFileDir && !isThisFileDir) return 1;
	
		return this.getName().compareToIgnoreCase(anotherFile.getName());
	}	
	
	private void processCommon()
	{
		if(this.getName().endsWith("." + CryptFile.ENC_FILE_EXTENSION) || this.getName().endsWith("." + CryptFile.ENC_FILE_EXTENSION + "." + Encryptor.ENC_FILE_UNFINISHED_EXTENSION))
		{
			/*
			this.encryptedDF = true;
			return;
			*/
			
			byte header[] = new byte[3];
			InputStream testStream = null;
			try {
				testStream = context.getContentResolver().openInputStream(documentFile.getUri());				
				testStream.read(header);
			} catch (Exception e) {
				//e.printStackTrace();
			} finally {
				try {
					testStream.close();
				} catch (Exception e) {
					//e.printStackTrace();
				}
			}
			if (new String(header).equalsIgnoreCase(CryptFile.ENC_FILE_HEADER_PREFIX)) {
				this.encryptedDF = true;
				if (this.getName().endsWith("." + CryptFile.ENC_FILE_EXTENSION + "." + Encryptor.ENC_FILE_UNFINISHED_EXTENSION))
					this.unfinishedDF = true;
			}
		}
	}
	
	private static boolean writeTestFile(CryptFileWrapper dir)
	{
		try {
			String name = null;
			while(true) {
				name = (System.currentTimeMillis() + ".testfile");
				if(!dir.existsChild(name)) break;
			}
			long timeStamp = dir.lastModified();
			CryptFileWrapper testFile = dir.createFile(name);
			OutputStream os = testFile.getOutputStream();
			os.write(0);
			os.flush();
			os.close();
			boolean ok = testFile.delete();
			dir.setLastModified(timeStamp);
			return ok;		
		} catch (Exception e) {
			return false;
		}		
	}
}
