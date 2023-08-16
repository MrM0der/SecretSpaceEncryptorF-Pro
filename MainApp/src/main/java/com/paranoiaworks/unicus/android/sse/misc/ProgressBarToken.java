package com.paranoiaworks.unicus.android.sse.misc;

import android.app.Dialog;
import android.app.NotificationManager;
import android.content.res.Resources;
import android.os.Handler;

import com.paranoiaworks.unicus.android.sse.components.DualProgressDialog;

import java.util.List;

import androidx.core.app.NotificationCompat;

/**
 * Helper object for communication between ProgressBar and executor Thread
 * 
 * @author Paranoia Works
 * @version 1.0.7
 */
public class ProgressBarToken {
	
	private DualProgressDialog dialog;
	private Dialog cancelDialog;
	private Handler progressHandler;
	private int increment;
	
	private boolean encryptAllToOneFile = false;
	private String customFileName = null;
	private CryptFileWrapper customOutputDirectoryEncrypted = null;
	private CryptFileWrapper customOutputDirectoryDecrypted = null;
	private List<CryptFileWrapper> includedFiles = null;
	private int numberOfFile = -1;
	private boolean customInterruption = false;
	private Resources resources;
	private String packageName;
	private RenderPhase renderPhase = null;
	private NotificationManager notificationManager;
	private NotificationCompat.Builder notificationBuilder;
	private int notificationId = -1;

	public ProgressBarToken(RenderPhase renderPhase) {
		this.renderPhase = renderPhase;
	}

	public boolean getEncryptAllToOneFile()
	{
		return encryptAllToOneFile;
	}
	
	public String getCustomFileName()
	{
		return customFileName;
	}
	
	public CryptFileWrapper getCustomOutputDirectoryEncrypted() {
		return customOutputDirectoryEncrypted;
	}

	public CryptFileWrapper getCustomOutputDirectoryDecrypted() {
		return customOutputDirectoryDecrypted;
	}

	public List<CryptFileWrapper> getIncludedFiles()
	{
		return includedFiles;
	}

	public int getNumberOfFiles()
	{
		return numberOfFile;
	}
	
	public void setEncryptAllToOneFile(boolean toOneFile)
	{
		this.encryptAllToOneFile = toOneFile;
	}
	
	public void setCustomFileName(String fileName)
	{
		this.customFileName = fileName;
	}
	
	public void setCustomOutputDirectoryEncrypted(
			CryptFileWrapper customOutputDirectoryEncrypted) {
		this.customOutputDirectoryEncrypted = customOutputDirectoryEncrypted;
	}

	public void setCustomOutputDirectoryDecrypted(
			CryptFileWrapper customOutputDirectoryDecrypted) {
		this.customOutputDirectoryDecrypted = customOutputDirectoryDecrypted;
	}

	public void setIncludedFiles(List<CryptFileWrapper> files)
	{
		this.includedFiles = files;
	}
	
	public void setNumberOfFiles(int number)
	{
		this.numberOfFile = number;
	}
	
	public DualProgressDialog getDialog() {
		return dialog;
	}
	
	public void setDialog(DualProgressDialog dialog) {
		this.dialog = dialog;
	}	

	public Dialog getCancelDialog() {
		return cancelDialog;
	}

	public void setCancelDialog(Dialog cancelDialog) {
		this.cancelDialog = cancelDialog;
	}

	public int getIncrement() {
		return increment;
	}
	
	public void setIncrement(int increment) {
		this.increment = increment;
	}
	
	public Handler getProgressHandler() {
		return progressHandler;
	}
	
	public void setProgressHandler(Handler progressHandler) {
		this.progressHandler = progressHandler;
	}

	public void setInterrupt(boolean active) {
		customInterruption = active;
	}

	public Resources getResources()
	{
		return this.resources;
	}

	public void setResources(Resources resources, String packageName) {
		this.resources = resources;
		this.packageName = packageName;
	}

	public void setNotificationObjects(NotificationManager notificationManager, NotificationCompat.Builder notificationBuilder, int notificationId)
	{
		this.notificationManager = notificationManager;
		this.notificationBuilder = notificationBuilder;
		this.notificationId = notificationId;
	}

	public synchronized void setNotificationProgress(int progress)
	{
		if(notificationManager != null && notificationBuilder != null)
		{
			try {
				if(progress < 0)
					notificationBuilder.setProgress(100, 100, true);
				else
					notificationBuilder.setProgress(100, progress, false);
				notificationManager.notify(notificationId, notificationBuilder.build());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized void setNotificationTextFromResource(String resourceName, String additionalText)
	{
		String text = null;
		try {
			text = getStringResourceByName(resourceName);
			if(additionalText != null) text += " " + additionalText;
			setNotificationText(text);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if(text != null) setNotificationText(text);
	}

	public synchronized void setNotificationText(String text)
	{
		if(notificationManager != null && notificationBuilder != null)
		{
			try {
				notificationBuilder.setContentText(text);
				notificationManager.notify(notificationId, notificationBuilder.build());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized void setNotificationTitle(String text)
	{
		if(notificationManager != null && notificationBuilder != null)
		{
			try {
				notificationBuilder.setContentTitle(text);
				notificationManager.notify(notificationId, notificationBuilder.build());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public boolean isInterrupted() {
		return customInterruption;
	}

	public void setRenderPhase(int renderPhase) {
		this.renderPhase.setRenderPhase(renderPhase);
	}

	public int getRenderPhase(){
		return renderPhase.getRenderPhase();
	}

	public String getStringResourceByName(String name)
	{
		if(resources == null) return name;
		String resText = null;
		int resID = resources.getIdentifier(name, "string", packageName);
		if(resID == 0) resText = name;
		else resText = resources.getString(resID);

		return resText;
	}
}
