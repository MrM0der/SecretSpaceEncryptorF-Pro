package com.paranoiaworks.unicus.android.sse.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ContentUris;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.ClipboardManager;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;
import androidx.core.os.EnvironmentCompat;
import androidx.documentfile.provider.DocumentFile;

import com.paranoiaworks.unicus.android.sse.CryptActivity;
import com.paranoiaworks.unicus.android.sse.R;
import com.paranoiaworks.unicus.android.sse.StaticApp;
import com.paranoiaworks.unicus.android.sse.components.SimpleWaitDialog;
import com.paranoiaworks.unicus.android.sse.dao.SettingDataHolder;
import com.paranoiaworks.unicus.android.sse.dao.Vault;
import com.paranoiaworks.unicus.android.sse.dao.VaultFolder;
import com.paranoiaworks.unicus.android.sse.dao.VaultFolderV2;
import com.paranoiaworks.unicus.android.sse.dao.VaultItem;
import com.paranoiaworks.unicus.android.sse.dao.VaultItemV2;
import com.paranoiaworks.unicus.android.sse.dao.VaultV2;
import com.paranoiaworks.unicus.android.sse.misc.CryptFile;
import com.paranoiaworks.unicus.android.sse.misc.CryptFileWrapper;
import com.paranoiaworks.unicus.android.sse.misc.ProgressBarToken;
import com.paranoiaworks.unicus.android.sse.misc.ProgressMessage;
import com.paranoiaworks.unicus.android.sse.misc.StringSentinel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import f5.engine.RawImage;

public class Helpers {
	
	public static final String DATE_FORMAT = "dd/MM/yyyy HH:mm";
	public static final String DATE_FORMAT_DATEONLY = "dd/MM/yyyy";
	public static final String REGEX_REPLACEALL_LASTDOT = "\\.(?!.*\\.)";
	
	public static final String UNIX_FILE_SEPARATOR = "/";
	public static final String WINDOWS_FILE_SEPARATOR = "\\";
	
	public static List<CryptFileWrapper> getExtDirectoriesWrapped(Context context, boolean extended)
	{	
		List<File> list = getExtDirectories(context, extended);	
		List<CryptFileWrapper> listWrapped = new ArrayList<CryptFileWrapper>();
		
		for (int i = 0; i < list.size(); ++i) {
			listWrapped.add(new CryptFileWrapper(new CryptFile(list.get(i))));
		}
		return listWrapped;
	}

	@SuppressLint("NewApi")
	public static List<File> getExtDirectories(Context context, boolean extended)
	{		
		List<File> dirList = new ArrayList<File>();
		String[] possibleDirs = null;

		List<String> pdFinal = new ArrayList<String>();
		try {
			if(extended) {
				if(!isAndroid_11P())
				{
					List<String> pd = getStorageDirectories();
					pdFinal.addAll(pd);
					List<String> pd2 = getExternalMounts(context);
					pdFinal.addAll(pd2);
				}
				
				File[] externalMedia = context.getExternalMediaDirs();
		    	for(int i = 0; i < externalMedia.length; ++i)
		    	{
		    		if(externalMedia[i] == null || externalMedia[i].getAbsolutePath() == null) continue;
		    		String trimmedPath = externalMedia[i].getParentFile().getParentFile().getParentFile().getAbsolutePath();
		    		pdFinal.add(trimmedPath);
		    	}
				
			}
		} catch (Throwable e) {}	
		
		if(pdFinal.size() > 0) 
		{
			possibleDirs = new String[pdFinal.size()];
			for(int i = 0; i < pdFinal.size(); ++i) {
				possibleDirs[i] = pdFinal.get(i);
			}
		}
		
		boolean mExternalStorageAvailable;
		boolean mExternalStorageWriteable;
		String state = Environment.getExternalStorageState();
		
		dirList.add(new File(File.separator)); // ROOT

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		} else {
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		
		File extDir = null;
		if (mExternalStorageAvailable && mExternalStorageWriteable)
		{
			File sd = new File("/sdcard");
			extDir = Environment.getExternalStorageDirectory();

			if(Build.VERSION.SDK_INT >= 26) {
				if (extDir.exists())
					dirList.add(extDir);
				if (sd.exists() && !Arrays.deepEquals(sd.list(), extDir.list()))
					dirList.add(sd);
			}
			else {
				if(sd.exists() && Arrays.deepEquals(sd.list(), extDir.list()))
					dirList.add(sd);
				else
					dirList.add(extDir);
			}
		}
		
		for (int i = 0; possibleDirs != null && i < possibleDirs.length; ++i)
		{	
			if (extDir != null && extDir.getAbsolutePath().equals(possibleDirs[i])) continue;
			if(possibleDirs[i] == null) continue;
			try {
				File testFile = new File(possibleDirs[i]);
				if (testFile.exists() && testFile.isDirectory()) dirList.add(testFile);
			} catch (Exception e) {
				// swallow
			}
		}	
		
		//+ Remove duplicities
		if(extended) 
		{
			Set<File> hs = new LinkedHashSet<File>(dirList);
			hs.addAll(dirList);
			dirList.clear();
			dirList.addAll(hs);
			
			List<File> finalList = new ArrayList<File>();
			for (int i = 0; i < dirList.size(); ++i)
			{
				File testDir = dirList.get(i);
				if(!isDirInList(testDir, finalList))
					finalList.add(testDir);
			}
			
			return finalList;
		}
		//-

		return dirList;	
	}

	public static List<String> getAppDirsCanonicalPaths(Context context)
	{
		List<String> dirList = new ArrayList<String>();
		try {
			if(android.os.Build.VERSION.SDK_INT >= 21)
			{
				File[] externalMedia = context.getExternalMediaDirs();
				for (int i = 0; i < externalMedia.length; ++i)
				{
					if (externalMedia[i] == null || externalMedia[i].getAbsolutePath() == null)
						continue;
					String trimmedPath = externalMedia[i].getCanonicalPath();
					dirList.add(trimmedPath);
					dirList.add(trimmedPath.replace("/media/", "/data/"));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return dirList.size() > 0 ? dirList : null;
	}

	private static boolean isDirInList(File dir, List<File> dirList) 
	{
		try {
			String dirHash = getDirHash(dir);
			for (int i = 0; i < dirList.size(); ++i)
			{
				File testDirB = dirList.get(i);
				if(dirHash.equals(getDirHash(testDirB))) {
					return true;	
				}
			}
		} catch (Exception e) {
			// swallow
		}
		return false;
	}
	
	private static String getDirHash(File dir) 
	{
		File[] files = dir.listFiles();
		if(files == null) return dir.getAbsolutePath();

		StringBuffer hashString = new StringBuffer();
		
		List<String> names = new ArrayList<String>();
		for(int i = 0; i < files.length; ++i) {
			File currentFile = files[i];
			names.add(currentFile.getName() + currentFile.lastModified());	
		}
		
		Collections.sort(names);
		
		for(int i = 0; i < files.length; ++i) 
		{
			hashString.append(names.get(i));	
		}

		return hashString.toString().trim();	
	}
	
	private static List<String> getStorageDirectories()
	{
		final Pattern DIR_SEPORATOR = Pattern.compile("/");
		
		// Final set of paths
	    final Set<String> rv = new HashSet<String>();
	    // Primary physical SD-CARD (not emulated)
	    final String rawExternalStorage = System.getenv("EXTERNAL_STORAGE");
	    // All Secondary SD-CARDs (all exclude primary) separated by ":"
	    final String rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE");
	    // Primary emulated SD-CARD
	    final String rawEmulatedStorageTarget = System.getenv("EMULATED_STORAGE_TARGET");
	    if(TextUtils.isEmpty(rawEmulatedStorageTarget))
	    {
	        // Device has physical external storage; use plain paths.
	        if(TextUtils.isEmpty(rawExternalStorage))
	        {
	            // EXTERNAL_STORAGE undefined; falling back to default.
	            rv.add("/storage/sdcard0");
	        }
	        else
	        {
	            rv.add(rawExternalStorage);
	        }
	    }
	    else
	    {
	        // Device has emulated storage; external storage paths should have
	        // userId burned into them.
	        final String rawUserId;
	        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
	        {
	            rawUserId = "";
	        }
	        else
	        {
	            final String path = Environment.getExternalStorageDirectory().getAbsolutePath();
	            final String[] folders = DIR_SEPORATOR.split(path);
	            final String lastFolder = folders[folders.length - 1];
	            boolean isDigit = false;
	            try
	            {
	                Integer.valueOf(lastFolder);
	                isDigit = true;
	            }
	            catch(NumberFormatException ignored)
	            {
	            }
	            rawUserId = isDigit ? lastFolder : "";
	        }
	        // /storage/emulated/0[1,2,...]
	        if(TextUtils.isEmpty(rawUserId))
	        {
	            rv.add(rawEmulatedStorageTarget);
	        }
	        else
	        {
	            rv.add(rawEmulatedStorageTarget + File.separator + rawUserId);
	        }
	    }
	    // Add all secondary storages
	    if(!TextUtils.isEmpty(rawSecondaryStoragesStr))
	    {
	        // All Secondary SD-CARDs splited into array
	        final String[] rawSecondaryStorages = rawSecondaryStoragesStr.split(File.pathSeparator);
	        Collections.addAll(rv, rawSecondaryStorages);
	    }
	    return new ArrayList<String>(rv);
	}
	
	public static boolean isWriteExceptionDir(CryptFileWrapper dir, Context context)
	{
		boolean specialDir = false;
		String path = dir.getAbsolutePath();
		String packageName = context.getPackageName();
		
		path = path.replaceAll(File.separator + "media" + File.separator, File.separator + "data" + File.separator);
		
		if(path.contains("Android" + File.separator + "data" + File.separator + packageName))
			specialDir = true;
		
		return specialDir && dir.getWritePermissionLevelForDir() > 1;
	}
	
	public static boolean makeBasicAppDirs(Context context)
	{
		File[] dirs = null;
		try {
			if(android.os.Build.VERSION.SDK_INT >= 19)
				dirs = context.getExternalFilesDirs(null);
			if(android.os.Build.VERSION.SDK_INT >= 21)
				dirs = context.getExternalMediaDirs();
		} catch (Throwable e) {
			// swallow
		}

		if(android.os.Build.VERSION.SDK_INT < 23)
			return true;

		boolean ok = false;

		if(dirs != null)
		{
			CryptFileWrapper testFile;
			try {
				testFile = new CryptFileWrapper(new CryptFile(dirs[0].getParentFile().getParentFile().getParentFile()));
				if(testFile.listFiles() != null)
					ok = true;
			} catch (Exception e){
					// N/A
			}
		}

		return ok;
	}

	public static boolean writeTestFile(CryptFileWrapper dir)
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
	
	private static List<String> getExternalMounts(Context context) 
	{
		   List<String> results = new ArrayList();

		    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //Method 1 for KitKat & above
		        File[] externalDirs = context.getExternalFilesDirs(null);

		        for (File file : externalDirs) {
		            String path = file.getPath().split("/Android")[0];

		            boolean addPath = false;

		            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
		                addPath = Environment.isExternalStorageRemovable(file);
		            }
		            else{
		                addPath = Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(file));
		            }

		            if(addPath){
		                results.add(path);
		            }
		        }
		    }

		        if(results.isEmpty()) { //Method 2 for all versions
		            String output = "";
		            try {
		                final Process process = new ProcessBuilder().command("mount | grep /dev/block/vold")
		                .redirectErrorStream(true).start();
		                process.waitFor();
		                final InputStream is = process.getInputStream();
		                final byte[] buffer = new byte[1024];
		                while (is.read(buffer) != -1) {
		                    output = output + new String(buffer);
		                }
		                is.close();
		            } catch (final Exception e) {
		                //e.printStackTrace();
		            }
		            if(!output.trim().isEmpty()) {
		                String devicePoints[] = output.split("\n");
		                for(String voldPoint: devicePoints) {
		                    results.add(voldPoint.split(" ")[2]);
		                }
		            }
		        }

		        if(Build.VERSION.SDK_INT >= 23) {
		            for (int i = 0; i < results.size(); i++) {
		                if (!results.get(i).toLowerCase().matches(".*[0-9a-f]{4}[-][0-9a-f]{4}")) {
		                    results.remove(i--);
		                }
		            }
		        } else {
		            for (int i = 0; i < results.size(); i++) {
		                if (!results.get(i).toLowerCase().contains("ext") && !results.get(i).toLowerCase().contains("sdcard")) {
		                    results.remove(i--);
		                }
		            }
		        }
	    return results;
	}
	
	public static String getImportExportPath(SettingDataHolder sdh)
	{
		String importExportPath = sdh.getItem("SC_Common", "SI_ImportExportPath");		
		return importExportPath;
	}
	
	public static File getImportExportDir(SettingDataHolder settingDataHolder)
	{       		
		String importExportPath = getImportExportPath(settingDataHolder);
		File importExportDir = null;
		
    	try {
    		File tempDir = new File(importExportPath);
    		if(!tempDir.exists()) tempDir.mkdir();
    		if(tempDir.exists() && tempDir.canRead()) importExportDir = tempDir;
    		new CryptFileWrapper(new CryptFile(importExportDir)).createDirectory(settingDataHolder.getDBHelper().getContext().getResources().getString(R.string.filesSubdir));
			new CryptFileWrapper(new CryptFile(importExportDir)).createDirectory(settingDataHolder.getDBHelper().getContext().getResources().getString(R.string.textsSubdir));
			new CryptFileWrapper(new CryptFile(importExportDir)).createDirectory(settingDataHolder.getDBHelper().getContext().getResources().getString(R.string.steganogramsSubDir));
		} catch (Exception e) {
			//swallow
		}
		return importExportDir;
	}

	public static boolean isImportExportDirWritable(SettingDataHolder sdh)
	{
		try {
			CryptFileWrapper testDir = new CryptFileWrapper(new CryptFile(Helpers.getImportExportDir(sdh)));
			if(testDir.getWritePermissionLevelForDir() > 1) return true;
		} catch (Exception e) {
			//swallow
		}
		return false;
	}
	
	public static boolean isSubDirectoryInList(List<CryptFileWrapper> baseDirs, CryptFileWrapper childCFW) 
	{
		boolean subDir = false;
		
		for(int i = 0; i < baseDirs.size(); ++i)
		{
			subDir = isSubDirectory(baseDirs.get(i), childCFW);
			if(subDir) break;
		}
		return subDir;
	}
	
	public static boolean isSubDirectory(CryptFileWrapper baseCFW, CryptFileWrapper childCFW) 
	{
		  if(baseCFW.getMode() != CryptFileWrapper.MODE_FILE || childCFW.getMode() != CryptFileWrapper.MODE_FILE)
			  return false;
		  
		  File base = baseCFW.getFile();
		  File child = childCFW.getFile();
		  
		  return isSubDirectory(base, child);
	}
	
	public static boolean isSubDirectory(File base, File child) 
	{
		  try {
			  if(!base.isDirectory())
				  return false;
			  
			  base = base.getCanonicalFile();
			  child = child.getCanonicalFile();

			  File parentFile = child;
			  while (parentFile != null) 
			  {
				  if(base.getAbsolutePath().length() > parentFile.getAbsolutePath().length())
					  break;
				  
				  if (base.equals(parentFile)) {
					  return true;
				  }
				  parentFile = parentFile.getParentFile();
			  }
		  } catch (Exception e) {
				// swallow
		  }

		  return false;
	}
	
	public static byte[] xorit(byte[] text, byte[] passPhrase)
	{		
		if (passPhrase.length == 0) passPhrase = "x".getBytes();
		byte[] outputBuffer = new byte[text.length];
		int counter = 0;
		for (int i = 0; i < text.length; ++i)
		{
			byte a = text[i];
			byte b = passPhrase[counter];
			outputBuffer[i] = (byte)(a ^ b);	
			++counter;
			if (counter == passPhrase.length) counter = 0;
		}		
		return outputBuffer;
	}
	
	public static byte[] concat(byte[]... args) 
	{
		int fulllength = 0;
		for (byte[] arrItem : args) 
		{
			fulllength += arrItem.length;
        }
		byte[] retArray = new byte[fulllength];
		int start = 0;
		for (byte[] arrItem : args) 
		{
			System.arraycopy(arrItem, 0, retArray, start, arrItem.length);
			start += arrItem.length;
		}
		return retArray;
	}
	
	public static byte[] getSubarray(byte[] array, int offset, int length) 
	{
		byte[] result = new byte[length];
		System.arraycopy(array, offset, result, 0, length);
		return result;
	}

	public static char[] concatAndWipe(char[] a, char[] b)
	{
		char result[] = new char[a.length + b.length];
		System.arraycopy(a, 0, result, 0, a.length);
		System.arraycopy(b, 0, result, a.length, b.length);
		Arrays.fill(a, '\u0000');
		Arrays.fill(b, '\u0000');
		return result;
	}

	public static String removeExt(String fileName, String extension)
    {
    	String name = fileName;
    	if (fileName.endsWith("." + extension))
    		name = name.substring(0, name.lastIndexOf('.')); 		
    	return name;
    }
	
	public static String getFirstDirFromFilepath (String filepath)
    {
    	String[] temp = filepath.split(File.separator);
    	if(temp[0].equals("") && temp.length > 1) return temp[1];
    	return temp[0];
    }
	
	public static String getFirstDirFromFilepathWithLFS(String filepath) //leading file separator (/...)
    {
		if(regexGetCountOf(filepath, File.separator) == 1) return filepath;
		String[] temp = filepath.split(File.separator);
    	if(temp[0].equals("") && temp.length > 1) return File.separator + temp[1];
    	return File.separator + temp[0];
    }
	
	public static String[] listToStringArray (List<String> strings)
    {
		String[] sList = new String[strings.size()];
		for(int i = 0; i < strings.size(); ++i)
		sList[i] = strings.get(i);
		return sList;
    }
	
	public static String[] fileListToNameStringArray (List<File> files)
    {
		String[] sList = new String[files.size()];
		for(int i = 0; i < files.size(); ++i)
		sList[i] = files.get(i).getName();
		return sList;
    }

	public static byte[] longToBytes(long l)
	{
		byte[] result = new byte[Long.BYTES];
		for (int i = Long.BYTES - 1; i >= 0; i--) {
			result[i] = (byte)(l & 0xFF);
			l >>= Byte.SIZE;
		}
		return result;
	}

	public static long[] getDirectorySizeWrapped(CryptFileWrapper directory) 
	{
		long[] output = new long[5];
		Arrays.fill(output, -1);
		long totalFolders = 0, totalFiles = 0;
		long foldersize = 0; long maxFileSize = 0;

		CryptFileWrapper[] filelist = directory.listFiles();
		for (int i = 0; i < filelist.length; i++) 
		{
			if (filelist[i].isDirectory()) 
			{
				long[] tempOutput = getDirectorySizeWrapped(filelist[i]);
				if(tempOutput[0] == -1) return output;
				foldersize += tempOutput[0];
				totalFiles += tempOutput[1];
				totalFolders += tempOutput[2];
				totalFolders++;
			} else {
				totalFiles++;
				foldersize += filelist[i].length();
				if(filelist[i].length() > maxFileSize) maxFileSize = filelist[i].length();
			}
		}
		output[0] = foldersize;
		output[1] = totalFiles;
		output[2] = totalFolders;
		output[3] = totalFolders + totalFiles;
		output[4] = maxFileSize;
		return output;
	}
	
	public static long[] getDirectorySizeWithInterruptionCheckWrapped(CryptFileWrapper directory) throws InterruptedException 
	{
		return getDirectorySizeWithInterruptionCheckWrapped(directory, null, null);
	}
	
	public static long[] getDirectoriesSizeWithInterruptionCheckWrapped(List<CryptFileWrapper> directories, ProgressMessage hm, ProgressBarToken progressBarToken) throws InterruptedException 
	{
		long[] allFilesOutput = new long[5];
		Arrays.fill(allFilesOutput, 0);
		
		for(int i = 0; i < directories.size(); ++i)
		{
			CryptFileWrapper tempFile = directories.get(i);
			if(tempFile.isFile())
			{
				allFilesOutput[0] += tempFile.length();
				++allFilesOutput[1];
				++allFilesOutput[3];
				hm.setProgressAbs(hm.getProgressAbs() + 1);
			} 
			else
			{
				long[] tempOutput = getDirectorySizeWithInterruptionCheckWrapped(tempFile, hm, progressBarToken);
				tempFile.setCachedDirectoryStats(tempOutput);
				allFilesOutput[0] += tempOutput[0];
				allFilesOutput[1] += tempOutput[1];
				allFilesOutput[2] += tempOutput[2];
				allFilesOutput[3] += tempOutput[3];
				if(tempOutput[4] > allFilesOutput[4]) allFilesOutput[4] = tempOutput[4];
			}
		}
		
		return allFilesOutput;
	}
	
	public static long[] getDirectorySizeWithInterruptionCheckWrapped(CryptFileWrapper directory, ProgressMessage hm, ProgressBarToken progressBarToken) throws InterruptedException 
	{
		long[] output = new long[5];
		Arrays.fill(output, -1);
		long totalFolders = 0, totalFiles = 0;
		long foldersize = 0; long maxFileSize = 0;
			
		CryptFileWrapper[] filelist = directory.listFiles();
		if(filelist == null) throw new InterruptedException("DirectorySize: FileList is NULL");
		if(progressBarToken != null) progressBarToken.setNotificationTextFromResource("common_initialization_text", "...");
		for (int i = 0; i < filelist.length; i++) 
		{
			if (filelist[i].isDirectory()) 
			{
				long[] tempOutput = getDirectorySizeWithInterruptionCheckWrapped(filelist[i], hm, progressBarToken);
				if(tempOutput[0] == -1) return output;
				foldersize += tempOutput[0];
				totalFiles += tempOutput[1];
				totalFolders += tempOutput[2];
				totalFolders++;
			} else {
				totalFiles++;
				foldersize += filelist[i].length();
				if(filelist[i].length() > maxFileSize) maxFileSize = filelist[i].length();
				
				if(progressBarToken != null)
				{
					Handler progressHandler = progressBarToken.getProgressHandler();
					hm.setProgressAbs(hm.getProgressAbs() + 1);
					if((directory.getMode() == CryptFileWrapper.MODE_DOCUMENT_FILE && hm.getProgressAbs() % 7 == 0) || hm.getProgressAbs() % 23 == 0)
						progressHandler.sendMessage(Message.obtain(progressHandler, -999, hm));
				}		
			}
			if (Thread.interrupted() || (progressBarToken != null && progressBarToken.isInterrupted()))
			{
				if(progressBarToken != null)
					throw new InterruptedException("Directory Check: " + progressBarToken.getStringResourceByName("common_canceledByUser"));
				else
					throw new InterruptedException("Directory Check:");
			}
		}
		output[0] = foldersize;
		output[1] = totalFiles;
		output[2] = totalFolders;
		output[3] = totalFolders + totalFiles;
		output[4] = maxFileSize;
		return output;
	}
	
	public static boolean deleteDirectory(File directory) 
	{
		if (directory == null)
			return false;
		if (!directory.exists())
			return true;
		if (!directory.isDirectory())
			return false;

		String[] list = directory.list();

		if (list != null) 
		{
			for (int i = 0; i < list.length; i++) 
			{
				File entry = new File(directory, list[i]);

				if (entry.isDirectory())
				{
					if (!deleteDirectory(entry))
						return false;
				}
				else
				{
					if (!entry.delete())
						return false;
				}
			}
		}
		return directory.delete();
	}
	
	public static boolean deleteDirectoriesWrapped(List<CryptFileWrapper> directories)
	{
		for(int i = 0; i < directories.size(); ++i)
		{
			CryptFileWrapper entry = directories.get(i);
			if(entry.isFile()) {
				if (!entry.delete()) return false;
			}
			if (!deleteDirectoryWrapped(entry)) return false;
		}
		return true;
	}
	
	public static boolean deleteDirectoryWrapped(CryptFileWrapper directory)
	{
		if (directory == null)
			return false;
		if (!directory.exists())
			return true;
		if (!directory.isDirectory())
			return false;

		CryptFileWrapper[] list = directory.listFiles();

		if (list != null) 
		{
			for (int i = 0; i < list.length; i++) 
			{
				CryptFileWrapper entry = list[i];

				if (entry.isDirectory())
				{
					if (!deleteDirectoryWrapped(entry))
						return false;
				}
				else
				{
					if (!entry.delete())
						return false;
				}
			}
		}
		return directory.delete();
	}
	
	
    public static DirectoryStats wipeFilesOrDirectoriesWrapped(List<CryptFileWrapper> files, ProgressBarToken progressBarToken) throws Exception
    {
    	DirectoryStats finalDS = getDirectoryStats();
    	
    	int numberOfFiles = files.size();
    	
    	Handler progressHandler = progressBarToken.getProgressHandler();
    	ProgressMessage hm = new ProgressMessage();
    	hm.setFullSizeB(numberOfFiles);
    	if(numberOfFiles > 1) progressHandler.sendMessage(Message.obtain(progressHandler, -1110, hm));
    	
    	for(int i = 0; i < numberOfFiles; ++i)
    	{
    		String wipingText = files.get(i).getMode() == CryptFileWrapper.MODE_FILE ? progressBarToken.getStringResourceByName("common_wiping_text") : progressBarToken.getStringResourceByName("common_deleting_text");
    		if(numberOfFiles > 1) wipingText += " (" + (i + 1) + "/" + numberOfFiles + ")";		
    		wipingText += ": <b>" + files.get(i).getName() + "</b><br/>";   		
    		progressHandler.sendMessage(Message.obtain(progressHandler, -1101, wipingText));
			progressBarToken.setNotificationTitle(progressBarToken.getStringResourceByName("common_app_fileEncryptor_name") + " (" + (i + 1) + "/" + numberOfFiles + ")");
    		
    		if(numberOfFiles > 1)
    		{
    			hm.setProgressAbsB(i);
    			progressHandler.sendMessage(Message.obtain(progressHandler, -1111, hm));
    		}
    		
    		DirectoryStats ds = wipeFileOrDirectoryWrapped(files.get(i), progressBarToken);
    		
    		finalDS.allFiles += ds.allFiles;
    		finalDS.allFolders += ds.allFolders;
    		finalDS.okFiles += ds.okFiles;
    		finalDS.okFolders += ds.okFolders;
    		if(i + 1 != numberOfFiles) progressHandler.sendMessage(Message.obtain(progressHandler, -1101, "<br/>"));
    	}
    	if(numberOfFiles > 1)
    	{
	    	hm.setProgressAbsB(numberOfFiles);
	    	progressHandler.sendMessage(Message.obtain(progressHandler, -1111, hm));
    	}
    	return finalDS;
    }
	
    public static DirectoryStats wipeFileOrDirectoryWrapped(CryptFileWrapper file, ProgressBarToken progressBarToken) throws Exception 
    {        
    	return wipeFileOrDirectoryWrapped(file, progressBarToken, false);
    }
	
	public static DirectoryStats wipeFileOrDirectoryWrapped(CryptFileWrapper file, ProgressBarToken progressBarToken, boolean tempFilesWiping) throws Exception 
	{        		
		Handler progressHandler = progressBarToken.getProgressHandler();
		// temp files wiping / normal wiping
		if(tempFilesWiping) {
			progressHandler.sendMessage(Message.obtain(progressHandler, -1202));
			progressBarToken.setNotificationTextFromResource("fe_wipingTempFiles", null);
		}
		else {
			progressHandler.sendMessage(Message.obtain(progressHandler, -1201));
			if(file.getMode() == CryptFileWrapper.MODE_FILE)
				progressBarToken.setNotificationTextFromResource("common_wiping_text", null);
			else
				progressBarToken.setNotificationTextFromResource("common_deleting_text", null);
		}
        progressHandler.sendMessage(Message.obtain(progressHandler, 0));
		progressBarToken.setNotificationProgress(0);
        ProgressMessage hm = new ProgressMessage();
        hm.setProgressAbs(0);
        DirectoryStats ds = getDirectoryStats();
    	
    	if(file.isDirectory())
    	{   		
    		ds.allFolders++;
    		long[] dirParametres = file.getCachedDirectoryStats() != null ? file.getCachedDirectoryStats() : getDirectorySizeWrapped(file);

			if(dirParametres[4] > 2147483647)
				throw new IllegalStateException(progressBarToken.getStringResourceByName("fe_message_wipe_folder_2GBlimit").replaceAll("<1>", getFormatedFileSize(hm.getFullSize())));

    		if(file.getMode() == CryptFileWrapper.MODE_FILE)
    		{
    			hm.setFullSize(dirParametres[0]);
    			file.renameTo(file.getName() + ".wipe");
    		}
    		else
    		{
    			hm.setFullSize(dirParametres[3]);
    		}

    		boolean ok = wipeDirectoryWrapped(file, progressBarToken, hm, ds);
    		if(ok) ds.okFolders++;
    	}
    	else if(file.isFile())
    	{
    		ds.allFiles++;
    		boolean ok = false;
    		if(file.getMode() == CryptFileWrapper.MODE_FILE)
    		{
	    		hm.setFullSize(file.length());
	    		if(hm.getFullSize() > 2147483647) 
	    			throw new IllegalStateException(progressBarToken.getStringResourceByName("fe_message_wipe_file_2GBlimit").replaceAll("<1>", getFormatedFileSize(hm.getFullSize())));
	    		ok = wipeFile(file.getFile(), progressBarToken, hm);
    		}
    		else
    		{
    			ok = file.delete(); // delete only if DocumentFile
    		}
    				
    		if(ok) ds.okFiles++;
    	}
    	if(!tempFilesWiping)
    	{
    		progressHandler.sendMessage(Message.obtain(progressHandler, -1211, ds)); // send DirectoryStats;
    		progressHandler.sendMessage(Message.obtain(progressHandler, -1101, ds.getReport(progressBarToken) + "<br/>")); // Append Verbose Text
    	}
    	return ds;
	}
    
    private static boolean wipeDirectoryWrapped(CryptFileWrapper directory, ProgressBarToken progressBarToken, ProgressMessage hm, DirectoryStats ds) throws IOException, InterruptedException
    {
		CryptFileWrapper[] list = directory.listFiles();
		Handler progressHandler = progressBarToken.getProgressHandler();

		if (list != null) 
		{
			for (int i = 0; i < list.length; i++) 
			{
				CryptFileWrapper entry = list[i];

				if (entry.isDirectory())
				{
					ds.allFolders++;
					if (wipeDirectoryWrapped(entry, progressBarToken, hm, ds)) ds.okFolders++;
				}
				else
				{
					ds.allFiles++;
					if(entry.getMode() == CryptFileWrapper.MODE_FILE)
					{
						if (wipeFile(entry.getFile(), progressBarToken, hm)) ds.okFiles++;
					}
					else
					{
						if (entry.delete()) 
						{
							ds.okFiles++;
							
							hm.setProgressAbs(hm.getProgressAbs() + 1);
							progressHandler.sendMessage(Message.obtain(progressHandler, -1100, hm));
							if(!hm.isRelSameAsLast()) progressBarToken.setNotificationProgress(hm.getProgressRel());
						}
					}
				}
				if (progressBarToken.isInterrupted())
				{
					progressBarToken.setInterrupt(false);
					throw new InterruptedException("Canceled by User.");
				}
			}
		}
		
		hm.setProgressAbs(hm.getProgressAbs() + 1);
		progressHandler.sendMessage(Message.obtain(progressHandler, -1100, hm));
		if(!hm.isRelSameAsLast()) progressBarToken.setNotificationProgress(hm.getProgressRel());
		return directory.delete();
    }
    
    private static boolean wipeFile(File file, ProgressBarToken progressBarToken, ProgressMessage hm) throws IOException
    {
    	Handler progressHandler = progressBarToken.getProgressHandler();
    	//SSElog.d("Wiping", "File: " + file.getName() + " " + file.length());
    	final int BUFFER_SIZE = 262144;
    	final long FILE_SIZE = file.length();
    	RandomAccessFile rwFile = null;
    	FileChannel rwChannel = null;
    	boolean wiped = false;
    	
    	byte[] nullBytes = new byte[BUFFER_SIZE];
    	try {  
    		if(!file.canWrite()) return false;
    		rwFile = new RandomAccessFile(file, "rw");
    		rwChannel = rwFile.getChannel();
    		
    		for(long i = 0; i * BUFFER_SIZE < FILE_SIZE; ++i)
    		{
    			long bytesLeft = FILE_SIZE - (i * BUFFER_SIZE);
    			int currentBufferSize = bytesLeft > BUFFER_SIZE ? BUFFER_SIZE : (int)bytesLeft;
    			MappedByteBuffer buffer = rwChannel.map(FileChannel.MapMode.READ_WRITE, i * BUFFER_SIZE, currentBufferSize);     
    			buffer.clear();
    			if(currentBufferSize != BUFFER_SIZE) nullBytes = new byte[currentBufferSize];
    			buffer.put(nullBytes);  
    			//byte[] randomBytes = new byte[currentBufferSize];     
    			//new Random().nextBytes(randomBytes);     
    			//buffer.put(randomBytes);     
    			buffer.force();

				hm.setProgressAbs(hm.getProgressAbs() + currentBufferSize);
				progressHandler.sendMessage(Message.obtain(progressHandler, -1100, hm));
				if(!hm.isRelSameAsLast()) progressBarToken.setNotificationProgress(hm.getProgressRel());
    		}
    		wiped = true;
    		
    		
    		
    		return wiped;
    	} catch (IOException ioe){  
    		throw new IOException("Wiping: " + ioe.getLocalizedMessage()); 		
    	} finally {  
    		if(rwChannel != null) rwChannel.close();
    		try{rwFile.getFD().sync();rwFile.close();}catch(Throwable e){}; 
    		if(wiped) {
    			File renamedFile = null;
    			while(true) {
    				renamedFile = new File(file.getParent(), Encryptor.getRandomString(32, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789") + ".w");
    				if(!renamedFile.exists()) break;
    			}
    			file.renameTo(renamedFile);
    			renamedFile.delete();
    		}
    	}  
    }
	
	public static String getFormatedFileSize(long fileSize) 
	{
		NumberFormat formatter = NumberFormat.getInstance();
		formatter.setMaximumFractionDigits(2);
		formatter.setMinimumFractionDigits(2);
		double fileSizeD = fileSize;
		if(fileSizeD < 1024) return ((long)fileSizeD + " B");
		fileSizeD = fileSizeD / 1024;
		if(fileSizeD < 1024) return (formatter.format(fileSizeD) + " kB");
		fileSizeD = fileSizeD / 1024;
		if(fileSizeD < 1024) return (formatter.format(fileSizeD) + " MB");
		fileSizeD = fileSizeD / 1024;
		if(fileSizeD < 1024) return (formatter.format(fileSizeD) + " GB");
		fileSizeD = fileSizeD / 1024;
		if(fileSizeD < 1024) return (formatter.format(fileSizeD) + " TB");
		fileSizeD = fileSizeD / 1024;
		if(fileSizeD < 1024) return (formatter.format(fileSizeD) + " PB");
		return (formatter.format(fileSizeD / 1024) + " EB");	
	}
		
	public static String getFormatedDateCustom(long time, String pattern) 
	{
		if(pattern == null) pattern = DATE_FORMAT;
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		return sdf.format(time);
	}
	
	public static String getFormatedDate(long time, Locale locale) 
	{
		if(locale == null) locale = Locale.getDefault();
		DateFormat formatter = DateFormat.getDateTimeInstance(
	            DateFormat.SHORT, 
	            DateFormat.SHORT, 
	            locale);
		return formatter.format(new Date(time));
	}
	
	public static String getFormatedTime(long time, Locale locale) 
	{
		if(locale == null) locale = Locale.getDefault();
		DateFormat formatter = DateFormat.getTimeInstance(
	            DateFormat.MEDIUM, 
	            locale);
		return formatter.format(new Date(time));
	}
	
	public static String replaceLastDot(String text, String replacement) 
	{
		return text.replaceAll(REGEX_REPLACEALL_LASTDOT, replacement);
	}
	
	public static int regexGetCountOf(byte[] input, String regex) throws UnsupportedEncodingException 
	{   
		return regexGetCountOf(new String(input, "UTF8"), regex);
	}
	
	public static int regexGetCountOf(String input, String regex) 
	{            	
		int count = 0;
		Pattern p = Pattern.compile(regex);   
		Matcher m = p.matcher(input);
		while (m.find()) ++count;
		return count;
	}

	public static int regexGetCountOf(char[] input, String regex)
	{
		int count = 0;
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(CharBuffer.wrap(input));
		while (m.find()) ++count;
		return count;
	}

	public static int indexOf(Pattern pattern, CharSequence s)
	{
		Matcher matcher = pattern.matcher(s);
		return matcher.find() ? matcher.start() : -1;
	}
	
	public static String convertToUnixFileSeparator(String path)
	{
		path = path.replaceAll(Pattern.quote(File.separator), UNIX_FILE_SEPARATOR);
		return path;
	}
	
	public static String convertToCurrentFileSeparator(String path)
	{
		path = path.replaceAll(Pattern.quote(UNIX_FILE_SEPARATOR), Matcher.quoteReplacement(File.separator));
		path = path.replaceAll(Pattern.quote(WINDOWS_FILE_SEPARATOR), Matcher.quoteReplacement(File.separator));
		return path;
	}
	
	public Set<Integer> getOnlyNavigationKeySet()
	{
		Set<Integer> keySet = new HashSet<Integer>();
		keySet.add(android.view.KeyEvent.KEYCODE_ENTER);
		return keySet;
	}
	
	public static String getFileExt(File file)
	{
		if(file == null) return null;
		return file.getName().substring(file.getName().lastIndexOf(".") + 1);
	}
	
	public static String getFileExtWrapped(CryptFileWrapper file)
	{
		if(file == null) return null;
		return file.getName().substring(file.getName().lastIndexOf(".") + 1);
	}
	
	public static FilenameFilter getOnlyExtFilenameFilter(String extension)
	{
		Helpers h = new Helpers();
		return h.getOnlyExtFF(extension);
	}
	
	private FilenameFilter getOnlyExtFF(String extension)
	{
		OnlyExt oe = new OnlyExt(extension);
		return oe;
	}

	private class OnlyExt implements FilenameFilter 
	{ 
		String ext;	
		public OnlyExt(String ext) 
		{ 
			this.ext = "." + ext; 
		}
		
		public boolean accept(File dir, String name) 
		{ 
			return name.toLowerCase().endsWith(ext);
		} 
	}
	
	public static CryptFileWrapper getCFWFromChooserIntent(Context context, Intent resultData, boolean verifyLength)
	{
		CryptFileWrapper chosenFile = null;
		final List<Uri> uris = Helpers.extractUriFromIntent(resultData);
		
		if(uris.size() > 0) 
		{
			if(android.os.Build.VERSION.SDK_INT >= 19)
			{
				try {
					DocumentFile dFile = DocumentFile.fromSingleUri(context, uris.get(0));
					if(dFile.exists() && (!verifyLength || dFile.length() > 0)) {
						chosenFile = new CryptFileWrapper(dFile, context);
					}
				} catch (Exception e) {
					// swallow
				}
			}

			if(chosenFile == null)
			{
				try {
					String selectedFilePath = Helpers.getRealPathFromUriExtended(context, uris.get(0));
					if (selectedFilePath != null) {
						CryptFile testFile = new CryptFile(selectedFilePath);
						if (testFile.exists() && testFile.isFile() && (!verifyLength || testFile.length() > 0)) {
							chosenFile = new CryptFileWrapper(testFile);
						}
					}
				} catch (Exception e) {
					// swallow
				}
			}
		}
		
		return chosenFile;
	}
		
	public static DirectoryStats getDirectoryStats()
	{
		Helpers h = new Helpers();
		return h.getDirectoryStatsInner();
	}
	
	private DirectoryStats getDirectoryStatsInner()
	{
		DirectoryStats ds =  new DirectoryStats();
		return ds;
	}
	
	public class DirectoryStats 
	{ 
		public int allFolders = 0, allFiles = 0;
		public int okFolders = 0, okFiles = 0;
		
		public String getReport(ProgressBarToken progressBarToken)
		{
			StringBuffer report = new StringBuffer();
			report.append(progressBarToken.getStringResourceByName("fe_report_wipedFiles")
					.replaceAll("<1>", "" + this.okFiles).replaceAll("<2>", "" + this.allFiles) + "<br/>");
			report.append(progressBarToken.getStringResourceByName("fe_report_wipedFolders")
					.replaceAll("<1>", "" + this.okFolders).replaceAll("<2>", "" + this.allFolders));
			return report.toString();
		}
	}
	
	public static void saveStringToFile(File file, String text) throws IOException
	{
		try {
			OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file), "UTF8");
			out.write(text);
			out.flush();
			out.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} 
	}

	public static void saveStringToFile(CryptFileWrapper file, String text) throws IOException
	{
		try {
			OutputStreamWriter out = new OutputStreamWriter(file.getOutputStream(), "UTF8");
			out.write(text);
			out.flush();
			out.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public static String loadStringFromFile(File file) throws IOException
	{
		StringBuilder text = new StringBuilder();
		
		try {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));

			String line = bufferedReader.readLine();
			if(line != null) line = line.replaceAll("\\p{C}", "");
			while(line != null){
				text.append(line.trim());
				text.append("\n");
				line = bufferedReader.readLine();
			}
			bufferedReader.close();
               
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } 
        return text.toString().trim();
	}
	
	public static String loadStringFromFile(CryptFileWrapper file) throws IOException
	{
		StringBuilder text = new StringBuilder();
		
		try {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF8"));

			String line = bufferedReader.readLine();
			if(line != null) line = line.replaceAll("\\p{C}", "");
			while(line != null){
				text.append(line.trim());
				text.append("\n");
				line = bufferedReader.readLine();
			}
			bufferedReader.close();
               
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } 
        return text.toString().trim();
	}
	
    public static void setLayoutOrientationA(LinearLayout mainWrapper, ViewGroup topWrapper, ViewGroup bottomWrapper, Context context)
    {    	
    	int orientation = context.getResources().getConfiguration().orientation;
    	if(orientation == Configuration.ORIENTATION_PORTRAIT)
    	{   	
    		mainWrapper.setOrientation(LinearLayout.VERTICAL);
    		
    		LinearLayout.LayoutParams topParams = 
    			new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0.0f);
    		topWrapper.setLayoutParams(topParams);
    		
    		LinearLayout.LayoutParams bottomParams = 
    			new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0.0f);
    		bottomParams.setMargins(0, 0, 0, 0);
    		bottomWrapper.setLayoutParams(bottomParams);  		
    	}
    	if(orientation == Configuration.ORIENTATION_LANDSCAPE)
    	{
    		mainWrapper.setOrientation(LinearLayout.HORIZONTAL);
    		
    		LinearLayout.LayoutParams topParams = 
    			new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
    		topWrapper.setLayoutParams(topParams);
    		
    		LinearLayout.LayoutParams bottomParams = 
    			new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
    		bottomParams.setMargins(((CryptActivity)context).dpToPx(5), 0, 0, 0);
    		bottomWrapper.setLayoutParams(bottomParams);
    	}
    }

    public static void blinkView(final View view, final int number)
	{
		if(view != null)
		{
			final Handler blinkHandler = new Handler();
			final Runnable blinker = new Runnable() {
				private int i = 0;
				@Override
				public void run()
				{
					try {
						if (++i % 2 == 0) {
							view.getBackground().setColorFilter(ContextCompat.getColor(view.getContext(), R.color.red_full), PorterDuff.Mode.MULTIPLY);
						} else {
							view.getBackground().clearColorFilter();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

					if(i < (number * 2 + 1)) {
						if(i > 1)
							blinkHandler.postDelayed(this, 350);
						else
							blinkHandler.postDelayed(this, 0);
					}
				}
			};
			blinkHandler.postDelayed(blinker, 200);
		}
	}
    
	/** Used in onPause methods (lock on sleep) */
	public static boolean isScreenOn(Activity activity)
	{
		PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);

		boolean screenOn;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
			screenOn = pm.isInteractive();
		} else {
			screenOn = pm.isScreenOn();
		}

		return screenOn;
	}

	public static Activity getActivityFromContext(Context context)
	{
		while (!(context instanceof Activity)) {
			if (!(context instanceof ContextWrapper)) {
				context = null;
			}
			ContextWrapper contextWrapper = (ContextWrapper) context;
			if (contextWrapper == null) {
				return null;
			}
			context = contextWrapper.getBaseContext();
			if (context == null) {
				return null;
			}
		}
		return (Activity) context;
	}
       
    public static String byteArrayToHexString(byte[] bytes) {
    	char[] hexArray = "0123456789ABCDEF".toCharArray();
    	char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
    
    public static String replaceLast(String string, String toReplace, String replacement) {
        int pos = string.lastIndexOf(toReplace);
        if (pos > -1) {
            return string.substring(0, pos)
                 + replacement
                 + string.substring(pos + toReplace.length(), string.length());
        } else {
            return string;
        }
    }
    
    public static String capitalizeFirstLetter(String original) {
        if(original.length() == 0)
            return original;
        return original.substring(0, 1).toUpperCase() + original.substring(1);
    }

	public static String capitalizeAllFirstLetters(String string) {
		char[] chars = string.toLowerCase().toCharArray();
		boolean found = false;
		for (int i = 0; i < chars.length; i++) {
			if (!found && Character.isLetter(chars[i])) {
				chars[i] = Character.toUpperCase(chars[i]);
				found = true;
			} else if (Character.isWhitespace(chars[i]) || chars[i]=='.' || chars[i]=='\'') { // You can add other chars here
				found = false;
			}
		}
		return String.valueOf(chars);
	}
    
    public static int[] getNumberOfEncAndUnenc(Map<String, CryptFileWrapper> filesMap)
    {
    	int[] results = new int[2];
    	List<CryptFileWrapper> filelist = new ArrayList<CryptFileWrapper>(filesMap.values());
    	
		for(int i = 0; i < filelist.size(); ++i)
		{
			CryptFileWrapper tempFile = filelist.get(i);
			if(tempFile.isEncrypted()) ++results[0];
			else ++results[1];
		}
		return results;    	
    }
    
    public static int[] getNumberOfDirsAndFiles(List<CryptFileWrapper> filelist)
    {
    	int[] results = new int[3];
    	
		for(int i = 0; i < filelist.size(); ++i)
		{
			CryptFileWrapper tempFile = filelist.get(i);
			if(tempFile.isDirectory()) ++results[0];
			else if(tempFile.isFile()) ++results[1];
			else ++results[2];
		}
		return results;    	
    }
    
    public static InputFilter getFileNameInputFilter()
    {
        InputFilter filter = new InputFilter()
    	{
    	    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) 
    	    { 
    	    	if (source.length() < 1) return null;
    	    	char last = source.charAt(source.length() - 1);
    	    	String reservedChars = "?:\"*|/\\<>";
            	if(reservedChars.indexOf(last) > -1) return source.subSequence(0, source.length() - 1);
            	return null;
    	    }  
    	};
    	return filter;
    }
    
    public static long getMaxFreeMemory()
    {
    	Runtime runtime = Runtime.getRuntime();
    	return (runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory()));
    }
    
    public static long getUsedMemInMB()
    {
    	Runtime runtime = Runtime.getRuntime();
    	return (runtime.totalMemory() - runtime.freeMemory()) / 1048576L;
    }
    
    public static Long getMaxHeapSizeInMB()
    {
    	Runtime runtime = Runtime.getRuntime();
    	return runtime.maxMemory() / 1048576L;
    }
    
	public static Bitmap toGrayscale(Bitmap bmpOriginal)
	{        
		int width, height;
		height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();    

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }
	
	public static char getCharFromChosenCharset(String charSet, byte deriveFromValue)
	{
		int byteValue = deriveFromValue + 128;
		int charIndex = (int)Math.floor(((double)byteValue / 255.001) * charSet.length());
		if(charIndex == charSet.length()) --charIndex;
		return charSet.charAt(charIndex);
	}
	
	/** Concat Arrays */
	public static <T> T[] concatAll(T[] first, T[]... rest) 
	{
		int totalLength = first.length;
		for (T[] array : rest) {
			totalLength += array.length;
		}
		T[] result = Arrays.copyOf(first, totalLength);
		int offset = first.length;
		for (T[] array : rest) {
			System.arraycopy(array, 0, result, offset, array.length);
			offset += array.length;
		}
		return result;
	}
	
	public static byte[] getByteArrayCopy(byte[] data)
	{
		byte[] dataCopy = new byte[data.length];
		System.arraycopy(data, 0, dataCopy, 0, data.length);
		return dataCopy;
	}

	public static boolean isEqualTimeConstant(byte[] digesta, byte[] digestb)
	{
		if (digesta == digestb) return true;
		if (digesta == null || digestb == null) {
			return false;
		}
		if (digesta.length != digestb.length) {
			return false;
		}

		int result = 0;
		for (int i = 0; i < digesta.length; i++) {
			result |= digesta[i] ^ digestb[i];
		}
		return result == 0;
	}

	public static char[] trim(char[] input)
	{
		int len = input.length;
		int st = 0;
		char[] val = input;

		while ((st < len) && (val[st] <= ' ')) {
			st++;
		}
		while ((st < len) && (val[len - 1] <= ' ')) {
			len--;
		}

		if((st > 0) || (len < input.length))
		{
			char[] output = Arrays.copyOfRange(input, st, len);
			Arrays.fill(input, '\u0000');
			return output;

		}
		else return input;
	}

	public static char[] intToChars(int input)
	{
		if(input < 0) throw new IllegalArgumentException("Cannot be < 0");
		if(input == 0) return (new char[]{'0'});
		int numOfDigits = getNumberOfDigits(input);
		char[] output = new char[numOfDigits];
		for(int i = numOfDigits - 1; i > -1; i--, input /= 10)
		{
			output[i] = (char)((input % 10) +  '0');
		}
		input = 0;
		return output;
	}

	public static int getNumberOfDigits(int input)
	{
		if(input < 0) throw new IllegalArgumentException("Cannot be < 0");
		if(input == 0) return 1;
		int length = 0;
		long temp = 1;
		while (temp <= input) {
			length++;
			temp *= 10;
		}
		input = 0;
		return length;
	}

	public static char[] toCharArray(final CharSequence source) {
		final int len = source.length();
		if (len == 0) {
			return new char[0];
		}
		if (source instanceof String) {
			return ((String) source).toCharArray();
		}
		final char[] array = new char[len];
		for (int i = 0; i < len; i++) {
			array[i] = source.charAt(i);
		}
		return array;
	}

	public static char[] toLowerCaseCharArray(final CharSequence source) {
		final int len = source.length();
		if (len == 0) {
			return new char[0];
		}
		if (source instanceof String) {
			return ((String) source).toLowerCase(Locale.ROOT).toCharArray();
		}
		final char[] array = new char[len];
		for (int i = 0; i < len; i++) {
			array[i] = Character.toLowerCase(source.charAt(i));
		}
		return array;
	}

	public static byte[] toBytes(char[] chars, boolean defaultCharset)
	{
		CharBuffer charBuffer = CharBuffer.wrap(chars);
		ByteBuffer byteBuffer = defaultCharset ? Charset.defaultCharset().encode(charBuffer) : Charset.forName("UTF-8").encode(charBuffer);
		byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
				byteBuffer.position(), byteBuffer.limit());
		Arrays.fill(byteBuffer.array(), (byte) 0);
		return bytes;
	}

	public static char[] toChars(CharSequence s)
	{
		if(s == null) return null;

		char[] chars = new char[s.length()];
		for(int i = 0; i < s.length(); ++i) {
			chars[i] = s.charAt(i);
		}
		return chars;
	}

	public static int indexOf(char[] source, int sourceOffset, int sourceCount,
					   char[] target, int targetOffset, int targetCount,
					   int fromIndex, boolean sourceCaseSensitive) {
		if (fromIndex >= sourceCount) {
			return (targetCount == 0 ? sourceCount : -1);
		}
		if (fromIndex < 0) {
			fromIndex = 0;
		}
		if (targetCount == 0) {
			return fromIndex;
		}

		char first = target[targetOffset];
		int max = sourceOffset + (sourceCount - targetCount);

		for (int i = sourceOffset + fromIndex; i <= max; i++) {
			/* Look for first character. */
			if(sourceCaseSensitive) {
				if (source[i] != first) {
					while (++i <= max && source[i] != first) ;
				}
			}
			else {
				if (Character.toLowerCase(source[i]) != first) {
					while (++i <= max && Character.toLowerCase(source[i]) != first) ;
				}
			}

			/* Found first character, now look at the rest of v2 */
			if (i <= max) {
				int j = i + 1;
				int end = j + targetCount - 1;
				if(sourceCaseSensitive) for (int k = targetOffset + 1; j < end && source[j] == target[k]; j++, k++);
				else for (int k = targetOffset + 1; j < end && Character.toLowerCase(source[j]) == target[k]; j++, k++);

				if (j == end) {
					/* Found whole string. */
					return i - sourceOffset;
				}
			}
		}
		return -1;
	}
	
	public static String loadStringFromInputStream(InputStream is) throws IOException
	{
		StringBuilder text = new StringBuilder();
		
		try {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF8"));

			String line = bufferedReader.readLine();
			while(line != null){
				text.append(line.trim());
				text.append("\n");
				line = bufferedReader.readLine();
			}      
			bufferedReader.close();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } 
        return text.toString();
	}

	public static void runClipboardCleaner(final Context context, final Handler afterHandler)
	{
		final SimpleWaitDialog waitDialog = new SimpleWaitDialog(context);
		waitDialog.setTitle(context.getResources().getString(R.string.ou_clipboardCleaner).toLowerCase());
		final Handler dialogHandler = new Handler()
		{
			public void handleMessage(Message msg)
			{
				if (msg.what == 1) waitDialog.show();
				else {
					if (waitDialog != null && waitDialog.isShowing()) {
						try {
							waitDialog.cancel();
						} catch (RuntimeException ex) {
							//N/A
						}
					}
				}
			}
		};

		final ClipboardManager clipBoard = (ClipboardManager) context.getSystemService(context.CLIPBOARD_SERVICE);

		Thread ccThread = new Thread (new Runnable()
		{
			public void run()
			{
				PowerManager.WakeLock wakeLock;
				PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
				wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SSE:CLEAN_CLIPBOARD");

				wakeLock.acquire();
				dialogHandler.sendMessage(Message.obtain(dialogHandler, 1));

				for (int i = 0; i < 60; ++i)
				{
					try {
						clipBoard.setText(Integer.toString(i));
						Thread.sleep(120);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				clipBoard.setText(null);

				dialogHandler.sendMessage(Message.obtain(dialogHandler, 0));
				wakeLock.release();
				afterHandler.sendMessage(Message.obtain(afterHandler, 1));
			}
		});
		ccThread.setPriority(Thread.MIN_PRIORITY);
		ccThread.start();
	}

	public static String getRealPathFromURI(Context context, Uri contentUri) 
	{
        Cursor cursor = context.getContentResolver().query(contentUri, null, null, null, null);

        int idx;
        if(contentUri.getPath().startsWith("/external/image") || contentUri.getPath().startsWith("/internal/image")) {
            idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        }
        else if(contentUri.getPath().startsWith("/external/video") || contentUri.getPath().startsWith("/internal/video")) {
            idx = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA);
        }
        else if(contentUri.getPath().startsWith("/external/audio") || contentUri.getPath().startsWith("/internal/audio")) {
            idx = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);
        }
        else{
            return contentUri.getPath();
        }
        if(cursor != null && cursor.moveToFirst()) {
            return cursor.getString(idx);
        }
        return null;
    }
	
	@SuppressLint("NewApi")
	public static String getRealPathFromUriExtended(final Context context, final Uri uri)
	{
		if (uri == null) {
			return null;
		}

		final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

		if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
			if (isExternalStorageDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				if ("primary".equalsIgnoreCase(type)) {
					return Environment.getExternalStorageDirectory() + "/"
							+ split[1];
				}
			}

			else if (isDownloadsDocument(uri)) {

				final String id = DocumentsContract.getDocumentId(uri);
				final Uri contentUri = ContentUris.withAppendedId(
						Uri.parse("content://downloads/public_downloads"),
						Long.valueOf(id));

				return getDataColumn(context, contentUri, null, null);
			}

			else if (isMediaDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				Uri contentUri = null;
				if ("image".equals(type)) {
					contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				} else if ("video".equals(type)) {
					contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
				} else if ("audio".equals(type)) {
					contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				}

				final String selection = "_id=?";
				final String[] selectionArgs = new String[] { split[1] };

				return getDataColumn(context, contentUri, selection,
						selectionArgs);
			}
		}

		else if ("content".equalsIgnoreCase(uri.getScheme())) {
			String path = getDataColumn(context, uri, null, null);
			if (path != null) {
				File file = new File(path);
				if (!file.canRead()) {
					return null;
				}
			}
			return path;
		}

		else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}

		return null;
	}

	public static String getDataColumn(Context context, Uri uri,
			String selection, String[] selectionArgs) {

		Cursor cursor = null;
		final String column = "_data";
		final String[] projection = { column };

		try {
			cursor = context.getContentResolver().query(uri, projection,
					selection, selectionArgs, null);
			if (cursor != null && cursor.moveToFirst()) {
				final int column_index = cursor.getColumnIndexOrThrow(column);
				return cursor.getString(column_index);
			}
		} catch (Exception e) {
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return null;
	}
	
	public static boolean isExternalStorageDocument(Uri uri) {
		return "com.android.externalstorage.documents".equals(uri.getAuthority());
	}


	public static boolean isDownloadsDocument(Uri uri) {
		return "com.android.providers.downloads.documents".equals(uri.getAuthority());
	}


	public static boolean isMediaDocument(Uri uri) {
		return "com.android.providers.media.documents".equals(uri.getAuthority());
	}
	
	public static List<Uri> extractUriFromIntent(final Intent intent) 
	{
		List<Uri> uris = new ArrayList();
		if (intent == null) {
			return uris;
		}
		Uri uri = intent.getData();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && uri == null) {
			final ClipData clipData = intent.getClipData();
			if (clipData != null) {
				for (int i = 0; i < clipData.getItemCount(); ++i) {
					uris.add(clipData.getItemAt(i).getUri());
				}
			}
		} else {
			uris.add(uri);
		}
		return uris;
	}

	public static String storEmulShorten(String path)
	{
		String emulString = "/storage/emulated/";
		String internalStorage = emulString + "0/";
		if(path.startsWith(internalStorage))
			return path.replaceFirst(Pattern.quote(internalStorage), Matcher.quoteReplacement("[IntStor]/"));
		else if(path.startsWith(emulString))
			return path.replaceFirst(Pattern.quote(emulString), Matcher.quoteReplacement("[StorEm]/"));
		else return path;
	}

	public static void trimChildMargins(ViewGroup vg)
	{
		final int childCount = vg.getChildCount();
		for (int i = 0; i < childCount; i++) {
			final View child = vg.getChildAt(i);

			if (child instanceof ViewGroup) {
				trimChildMargins((ViewGroup) child);
			}

			final ViewGroup.LayoutParams lp = child.getLayoutParams();
			if (lp instanceof ViewGroup.MarginLayoutParams) {
				((ViewGroup.MarginLayoutParams) lp).leftMargin = 0;
			}
			child.setPadding(0, 0, 0, 0);
		}
	}

	public static VaultV2 migrateVault(Vault origin)
	{
		VaultV2 newVault = VaultV2.getInstance();

		for(int i = 0; i < origin.getFolderCount(); ++i)
		{
			VaultFolder vf = origin.getFolderByIndex(i);
			VaultFolderV2 vf2 = new VaultFolderV2();

			vf2.setFolderName(vf.getFolderName().toCharArray());
			vf2.setFolderComment(vf.getFolderComment().toCharArray());
			vf2.setColorCode(vf.getColorCode());
			vf2.setDateModified(new Date(vf.getDateModified()));
			vf2.setAttributes(vf.getAttributes());
			for(int j = 0; j < vf.getItemCount(); ++j)
			{
				VaultItem vi = vf.getItemByIndex(j);
				VaultItemV2 vi2 = new VaultItemV2();

				vi2.setItemName(vi.getItemName().toCharArray());
				vi2.setItemComment(vi.getItemComment().toCharArray());
				vi2.setItemPassword(vi.getItemPassword().toCharArray());
				vi2.setColorCode(vi.getColorCode());
				vi2.setDateModified(new Date(vi.getDateModified()));
				if(vi.isExtendedItem()) {
					vi2.enableExtendedItem();
					vi2.setItemAccount(vi.getItemAccount().toCharArray());
					vi2.setItemURL(vi.getItemUrl().toCharArray());
				}

				if(vi.isExtendedItem())
				{
					List<String[]> cel = vi.getCustomElements();
					List<StringSentinel[]> cel2 = new ArrayList<>();
					for (int k = 0; k < cel.size(); ++k )
					{
						String[] ce = (String[])cel.get(k);
						StringSentinel[] ce2 = new StringSentinel[2];
						ce2[0] = new StringSentinel(ce[0].toCharArray(), VaultV2.SS_GROUP_ID);
						ce2[1] = new StringSentinel(ce[1].toCharArray(), VaultV2.SS_GROUP_ID);
						cel2.add(ce2);
					}
					vi2.addOrReplaceCustomElements(cel2);
				}
				vf2.addItem(vi2);
			}
			newVault.addFolder(vf2);
		}
		return newVault;
	}
	
	public static List<Integer> mergeVaults(VaultV2 baseVault, VaultV2 addVault)
	{
		int newFolders = 0;
		int newItems = 0;
		int replacedItems = 0;
		
		List<VaultFolderV2> folderList = new ArrayList<VaultFolderV2>();
		for(int i = 0; i < baseVault.getFolderCount(); ++i) {
			folderList.add(baseVault.getFolderByIndex(i));
		}
		
		for(int i = 0; i < addVault.getFolderCount(); ++i) 
		{
			boolean isNewFolder = true;
			VaultFolderV2 newFolder = addVault.getFolderByIndex(i);
			
			for(int j = 0; j < folderList.size(); ++j)
			{
				VaultFolderV2 testFolder = folderList.get(j);
				if(testFolder == null) continue;

				if(newFolder.getFolderNameSS().equals(testFolder.getFolderNameSS()) && testFolder.getColorCode() == newFolder.getColorCode())
				{
					isNewFolder = false;
					
					List<VaultItemV2> itemList = new ArrayList<VaultItemV2>();
					for(int k = 0; k < testFolder.getItemCount(); ++k) {
						itemList.add(testFolder.getItemByIndex(k));
					}
					
					for(int k = 0; k < newFolder.getItemCount(); ++k)
					{
						boolean isNewItem = true;
						VaultItemV2 newItem = newFolder.getItemByIndex(k);
						
						for(int l = 0; l < itemList.size(); ++l)
						{
							VaultItemV2 testItem = itemList.get(l);
							if(testItem == null) continue;
							
							if(newItem.getItemNameSS().equals(testItem.getItemNameSS()) && newItem.getColorCode() == testItem.getColorCode())
							{
								isNewItem = false;
								
								if(newItem.getDateModified() > testItem.getDateModified()) { // replace values
									testItem.addOrReplaceItemMainMap(newItem.getItemMainMap());
									testItem.addOrReplaceCustomElements(newItem.getCustomElements());
									testItem.setDateModified(new Date(newItem.getDateModified()));
									++replacedItems;
								}
								
								itemList.set(l, null);
								break;
							}	
						}
						if(isNewItem) { // add item
							testFolder.addItem(newItem);
							++newItems;
						}
					}
					
					folderList.set(j, null);
					break;
				}
				testFolder.notifyItemDataSetChanged();
			}
			if(isNewFolder) { // add whole folder
				baseVault.addFolder(newFolder);
				newItems += newFolder.getItemCount();
				++newFolders;
			}		
		}
		
		if(newFolders > 0) baseVault.notifyFolderDataSetChanged();
		
		List<Integer> report = new ArrayList<Integer>();
		report.add(newFolders);
		report.add(newItems);
		report.add(replacedItems);
		
		return report;
	}
	
	public static List<Integer> getImageDimension(File imgFile) throws IOException 
	{
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;

		//Returns null, sizes are in the options variable
		BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
		int width = options.outWidth;
		int height = options.outHeight;
		
		ArrayList<Integer> dimension = new ArrayList<Integer>();
		dimension.add(width);
		dimension.add(height);
		
		return dimension;
	}
	
	public static List<Integer> getImageDimension(CryptFileWrapper imgFile) throws IOException 
	{
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;

		//Returns null, sizes are in the options variable
		BitmapFactory.decodeStream(imgFile.getInputStream(), null, options);
		int width = options.outWidth;
		int height = options.outHeight;
		
		ArrayList<Integer> dimension = new ArrayList<Integer>();
		dimension.add(width);
		dimension.add(height);
		
		return dimension;
	}
	
    public static Bitmap getDownscaledBitmap(CryptFileWrapper cryptFileWrapper, int reqWidth, int reqHeight) throws Exception 
    {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(cryptFileWrapper.getInputStream(), null, options);

        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int heightRatio = Math.round((float)height / (float)reqHeight);
            final int widthRatio = Math.round((float)width / (float)reqWidth);

            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = inSampleSize;

        Bitmap decodedBitmap = BitmapFactory.decodeStream(cryptFileWrapper.getInputStream(), null, o2);
        
        if(decodedBitmap == null) throw new IllegalStateException();
        
        return Bitmap.createScaledBitmap(decodedBitmap, reqWidth, reqHeight, true);
    }
	
	public static RawImage loadToRawImage(CryptFileWrapper sourceFile, Double imageScale) throws Exception
	{
		Bitmap image = null;
		
		if(imageScale != null && imageScale != 1.0) // resize
		{	
			List<Integer> imgSize = getImageDimension(sourceFile);
			
			int newWidth = (int)Math.round(imgSize.get(0) * imageScale);
			int newHeight = (int)Math.round(imgSize.get(1) * imageScale);
			
			int inSampleSize = 1;
			long freeMemory = Helpers.getMaxFreeMemory();
			long neededMemoryFull = imgSize.get(0) * imgSize.get(1) * 8;
			while(true) 
			{
				long neededMemory = neededMemoryFull / (inSampleSize * inSampleSize);
				if(freeMemory > neededMemory) break;			
				inSampleSize *= 2;
			}			
			
			if(inSampleSize == 1)
				image = BitmapFactory.decodeStream(sourceFile.getInputStream());
			else 
			{				
				if(imgSize.get(0) / inSampleSize < newWidth)
					inSampleSize = (int)Math.round(1 / imageScale);
				BitmapFactory.Options bo = new BitmapFactory.Options();
		        bo.inSampleSize = inSampleSize;			
				image = BitmapFactory.decodeStream(sourceFile.getInputStream(), null, bo);
			}
			
			while(true)
			{
				int partialWidth = (int)Math.round((double)image.getWidth() / 1.5);
				int partialHeight = (int)Math.round((double)image.getHeight() / 1.5);
				
				if(partialWidth + partialHeight > newWidth + newHeight) {
					image = Bitmap.createScaledBitmap(image, partialWidth, partialHeight, true);
				}
				else {
					image = Bitmap.createScaledBitmap(image, newWidth, newHeight, true);
					break;
				}			
			}
		}
		else image = BitmapFactory.decodeStream(sourceFile.getInputStream());
		
		int width = image.getWidth();
		int height = image.getHeight();
		int pixels[] = new int[width * height];
		
		image.getPixels(pixels, 0, width, 0, 0, width, height);
		image.recycle();
		
		return new RawImage(pixels, width, height);
	}
	
	public static List<String> getFileCommentsList(List<String> fileNamesList, String dirPath, Locale locale, Integer sizeLimit) 
	{
		if(sizeLimit != null) {
			if(sizeLimit < 0) sizeLimit = 1500;
			if(fileNamesList.size() > sizeLimit) return null;
		}
		
		List<String> fileComments = new ArrayList<String>();
		for (int i = 0; i < fileNamesList.size(); ++i) 
		{
		    File tempFile = new File(dirPath + File.separator + fileNamesList.get(i));
		    fileComments.add(i, Helpers.getFormatedDate(tempFile.lastModified(), locale)
		    		+ " (" + Helpers.getFormatedFileSize(tempFile.length()) + ")");
		}
		
		return fileComments;
	}
	
	public static List<String> getMessageCommentsList(List<Long> length, List<Long> timestamp, CryptActivity cryptActivity)
	{
		Locale locale = cryptActivity.getResources().getConfiguration().locale;

		List<String> messageComments = new ArrayList<String>();
		for (int i = 0; i < length.size(); ++i) 
		{
			long lengthTemp = length.get(i);
			if(lengthTemp < 0) {
				messageComments.add(null);
				continue;
			}
			messageComments.add(i, Helpers.getFormatedDate(timestamp.get(i), locale)
		    		+ " (" +  cryptActivity.getStringResource("common_text_length").toLowerCase() + ": " + lengthTemp + ")");
		}
		
		return messageComments;
	}
	
	public static String getShortenedStackTrace(Throwable e) 
	{
		 return "<small>" + Helpers.getShortenedStackTrace(e, 2) + "</small>";
	}
	
	public static String getShortenedStackTrace(Throwable e, int maxLines) 
	{
	    StringWriter writer = new StringWriter();
	    e.printStackTrace(new PrintWriter(writer));
	    String[] lines = writer.toString().split("\n");
	    StringBuilder sb = new StringBuilder();
	    for (int i = 0; i < Math.min(lines.length, maxLines); i++) {
	        sb.append(lines[i]).append("\n");
	    }
	    return sb.toString();
	}	
	
	public static String insertTextPeriodically(String text, String insert, int period)
	{
		if(period < 1) return text;
		StringBuilder builder = new StringBuilder(text.length() + insert.length() * (text.length()/period)+1);

		int index = 0;
		String prefix = "";
		while (index < text.length())
		{
			builder.append(prefix);
			prefix = insert;
			builder.append(text.substring(index, 
					Math.min(index + period, text.length())));
			index += period;
		}
		return builder.toString();
	}

	public static boolean compareDeepElements(List a, List b)
	{
		if(a.size() == 0 && b.size() == 0) return true;
		boolean result = false;
		if(a.size() != b.size()) return false;
		for(int i = 0; i < a.size(); ++i)
		{
			StringSentinel[] ae = (StringSentinel[])a.get(i);
			StringSentinel[] be = (StringSentinel[])b.get(i);
			if(!ae[0].equals(be[0])) break;
			if(!ae[1].equals(be[1])) break;
			if(i + 1 == a.size()) result = true;
		}

		return result;
	}

	public static void checkAllAppDirectoriesAndShowError(SettingDataHolder sdh)
	{
		Context context = sdh.getDBHelper().getContext();
		boolean exist = existAllAppDirectories(sdh);

		if(!exist) {
			String dirList =
					"../" + context.getResources().getString(R.string.filesSubdir) + "<br/>" +
							"../" + context.getResources().getString(R.string.textsSubdir) + "<br/>" +
									"../" + context.getResources().getString(R.string.steganogramsSubDir);
			ComponentProvider.getShowMessageDialog(context,
					context.getResources().getString(R.string.common_warning_text).toUpperCase(),
					context.getResources().getString(R.string.common_cannotCreateAppDirectories).replaceAll("<1>", dirList).replaceAll("<2>", Helpers.getImportExportPath(sdh)),
					ComponentProvider.DRAWABLE_ICON_CANCEL).show();
		}
	}

	public static boolean existAllAppDirectories(SettingDataHolder sdh)
	{
		Context context = sdh.getDBHelper().getContext();
		try {
			CryptFileWrapper impExpDirWrapper = new CryptFileWrapper(new CryptFile(Helpers.getImportExportDir(sdh)));
			if(!impExpDirWrapper.existsChild(context.getResources().getString(R.string.filesSubdir))) return false;
			if(!impExpDirWrapper.existsChild(context.getResources().getString(R.string.textsSubdir))) return false;
			if(!impExpDirWrapper.existsChild(context.getResources().getString(R.string.steganogramsSubDir))) return false;

		} catch (Exception e) {
			return false;
		}

		return true;
	}

	public static int getNumberOfCores()
	{
		if(Build.VERSION.SDK_INT >= 17) {
			return Runtime.getRuntime().availableProcessors();
		}
		else {
			return getNumCoresOldPhones();
		}
	}

	private static int getNumCoresOldPhones()
	{
		class CpuFilter implements FileFilter {
			@Override
			public boolean accept(File pathname) {
				if(Pattern.matches("cpu[0-9]+", pathname.getName())) {
					return true;
				}
				return false;
			}
		}

		try {
			File dir = new File("/sys/devices/system/cpu/");
			File[] files = dir.listFiles(new CpuFilter());
			return files.length;
		} catch(Exception e) {
			return 1;
		}
	}

	public static boolean is64bitSystem()
	{
		if (Build.VERSION.SDK_INT >= 21) {
			try {if(Build.SUPPORTED_64_BIT_ABIS.length > 0) return true;} catch (Exception e) {}
		}
		return false;
	}

	public static boolean isAndroid_11P()
	{
		return android.os.Build.VERSION.SDK_INT > 29;
	}
	
	public static boolean isAndroid11PorHflavor()
	{
		return android.os.Build.VERSION.SDK_INT > 29 || StaticApp.VERSION_FLAVOR.equals("H");
	}
}
