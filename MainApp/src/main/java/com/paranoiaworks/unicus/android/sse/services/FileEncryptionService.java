package com.paranoiaworks.unicus.android.sse.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;

import com.paranoiaworks.unicus.android.sse.CryptActivity;
import com.paranoiaworks.unicus.android.sse.FileEncActivity;
import com.paranoiaworks.unicus.android.sse.R;
import com.paranoiaworks.unicus.android.sse.dao.SettingDataHolder;
import com.paranoiaworks.unicus.android.sse.misc.CryptFileWrapper;
import com.paranoiaworks.unicus.android.sse.misc.EncryptorException;
import com.paranoiaworks.unicus.android.sse.misc.ExtendedInterruptedException;
import com.paranoiaworks.unicus.android.sse.misc.ProgressBarToken;
import com.paranoiaworks.unicus.android.sse.misc.ProgressMessage;
import com.paranoiaworks.unicus.android.sse.utils.Encryptor;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;
import com.paranoiaworks.unicus.android.sse.utils.Helpers.DirectoryStats;

import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.zip.DataFormatException;

import androidx.core.app.NotificationCompat;

/**
 * File Encryptor tasks service
 *
 * @author Paranoia Works
 * @version 1.0.3
 */
public class FileEncryptionService extends IntentService {

    public static final int PURPOSE_ENCDEC = 1; // Encrypt/Decrypt
    public static final int PURPOSE_WIPE = 101;
    public static final int PURPOSE_DELETE = 102;
    public static final String FES_NOTFINISHED_TOKEN = "com.paranoiaworks.unicus.android.sse.FES_NOTFINISHED_TOKEN";

    public static final String NAME = "com.paranoiaworks.unicus.android.sse.FILE_ENC_SERVICE";
    public static final String VERIFICATION_TOKEN = "com.paranoiaworks.unicus.android.sse.FILE_ENC_SERVICE_VERIFICATION_TOKEN";
    public static final String PURPOSE_KEY = "PURPOSE";

    private static final String CHANNEL_ID = "com.paranoiaworks.unicus.android.sse.FILE_ENC_SERVICE_CHANNEL";
    private static final int NOTIFICATION_ID = 1;

    private static boolean running = false;
    private static boolean wipeEncryptorAfterDone = false;
    private static ProgressBarToken progressBarTokenStatic;

    private ProgressBarToken progressBarToken;
    private Handler progressHandler;
    private Handler universalHandler;
    private Encryptor encryptor;
    private SettingDataHolder settingDataHolder;
    private List<CryptFileWrapper> selectedItems;
    private CryptFileWrapper currentDir;
    private List<CryptFileWrapper> currentFilesTemp;
    private boolean compress;
    private boolean showRoot;
    private SharedPreferences preferences;

    public FileEncryptionService() {
        super(NAME);
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        if(running) return;
        running = true;

        NotificationCompat.Builder notificationBuilder = null;
        NotificationManager notificationManager = null;
        try {
            notificationBuilder = null;
            notificationManager = null;

            notificationBuilder = new NotificationCompat.Builder(this, createChannel());

            notificationBuilder.setAutoCancel(true)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.notification_fes)
                    .setContentTitle(getResources().getString(R.string.common_app_fileEncryptor_name))
                    .setContentText("")
                    .setProgress(100, 0, false);

            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            //NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            startForeground(NOTIFICATION_ID, notificationBuilder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            selectedItems = new ArrayList<CryptFileWrapper>();
            Bundle extras = intent.getExtras();
            preferences = getSharedPreferences(NAME, MODE_PRIVATE);

            byte[] verificationToken = intent.getExtras().getByteArray(VERIFICATION_TOKEN);
            int purpose = intent.getExtras().getInt(PURPOSE_KEY);
            Map<String, Object> intentMap = (Map<String, Object>)CryptActivity.getTemporaryObject(verificationToken);

            if(intentMap == null) throw new Error("Can't pass IntentMap.");
            CryptActivity.removeTemporaryObject();

            encryptor = (Encryptor) intentMap.get("VAR_Enryptor");
            progressBarToken = (ProgressBarToken) intentMap.get("VAR_ProgressBarToken");
            universalHandler = (Handler) intentMap.get("VAR_UniversalHandler");
            if(intentMap.get("VAR_SelectedItems") != null)
                selectedItems.addAll((List<CryptFileWrapper>)intentMap.get("VAR_SelectedItems"));
            currentDir = (CryptFileWrapper) intentMap.get("VAR_CurrentDir");
            currentFilesTemp = (List<CryptFileWrapper>) intentMap.get("VAR_CurrentFilesTemp");
            showRoot = (boolean) intentMap.get("VAR_ShowRoot");
            settingDataHolder = (SettingDataHolder)intentMap.get("VAR_SettingDataHolder");

            Boolean fWipeEncryptorAfterDone = (Boolean)intentMap.get("FLAG_WipeEncryptorAfterDone");
            if(fWipeEncryptorAfterDone == null) fWipeEncryptorAfterDone = false;
            wipeEncryptorAfterDone = fWipeEncryptorAfterDone;

            if(selectedItems.size() < 1) throw new IllegalStateException("SelectedItems is NULL");
            if(progressBarToken == null) throw new IllegalStateException("ProgressBarToken is NULL");

            progressBarTokenStatic = progressBarToken;
            progressBarToken.setNotificationObjects(notificationManager, notificationBuilder, NOTIFICATION_ID);
            progressBarToken.setResources(getResources(), getPackageName());

            preferences.edit().putBoolean(FES_NOTFINISHED_TOKEN, true).commit();


            if(purpose == PURPOSE_ENCDEC) {
                compress = (boolean) intentMap.get("VAR_Compress");
                progressHandler = progressBarToken.getProgressHandler();
                startEncDec();
            }
            else if(purpose == PURPOSE_WIPE && encryptor == null) {
                wipe();
            }
            else if(purpose == PURPOSE_DELETE && encryptor == null) {
                progressBarToken.setNotificationProgress(-1);
                delete();
            }

        } catch(Exception e) {
            if(progressBarToken != null) {
                progressBarToken.getDialog().cancel();
                progressBarToken.getCancelDialog().cancel();
            }
            if(universalHandler != null) {
                universalHandler.sendMessage(Message.obtain(universalHandler, FileEncActivity.FEA_UNIVERSALHANDLER_HIDE_WAITDIALOG));
                universalHandler.sendMessage(Message.obtain(universalHandler, FileEncActivity.FEA_UNIVERSALHANDLER_SHOW_ERROR_DIALOG, Helpers.getShortenedStackTrace(e)));
            }
            e.printStackTrace();
        }
        finally {
            if(preferences != null) preferences.edit().putBoolean(FES_NOTFINISHED_TOKEN, false).commit();
            if(progressBarToken != null) progressBarToken.setResources(null, null);
            if(wipeEncryptorAfterDone && encryptor != null) encryptor.wipeMasterKeys();
            wipeEncryptorAfterDone = false;
            progressBarToken = null;
            progressBarTokenStatic = null;
            CryptActivity.removeTemporaryObject();
            running = false;
            stopForeground(true);
        }
    }

    public static boolean isRunning()
    {
        return running;
    }

    public static void setWipeEncryptorAfterDone()
    {
        wipeEncryptorAfterDone = true;
    }

    public static void terminateTasks()
    {
        progressBarTokenStatic.setInterrupt(true);
    }

    /** Encryption/Decryption Service */
    private synchronized void startEncDec()
    {
        PowerManager.WakeLock wakeLock;
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.paranoiaworks.unicus.android.sse:FE");
        wakeLock.acquire();

        progressBarToken.setNotificationText(getResources().getString(R.string.common_initialization_text) + "...");

        final List<CryptFileWrapper> filelist = selectedItems;

        if(!progressBarToken.getEncryptAllToOneFile()) // not to one file
        {
            List<CryptFileWrapper> encList = new ArrayList<CryptFileWrapper>();
            List<CryptFileWrapper> unencList = new ArrayList<CryptFileWrapper>();
            List<CryptFileWrapper> finalFileList = new ArrayList<CryptFileWrapper>();

            for(int i = 0; i < filelist.size(); ++i)
            {
                CryptFileWrapper tempFile = filelist.get(i);
                if(tempFile.isEncrypted()) encList.add(tempFile);
                else unencList.add(tempFile);
            }

            finalFileList.addAll(encList);
            finalFileList.addAll(unencList);

            progressBarToken.setNumberOfFiles(finalFileList.size());
            ProgressMessage hm = new ProgressMessage();
            hm.setFullSizeB(finalFileList.size());
            progressHandler.sendMessage(Message.obtain(progressHandler, -1110, hm));

            boolean interrupted = false;
            for(int i = 0; i < finalFileList.size(); ++i)
            {
                progressBarToken.setNotificationProgress(0);
                boolean ok = true;
                interrupted = false;
                if(i > 0) progressHandler.sendMessage(Message.obtain(progressHandler, -1101, "<br/>"));
                progressHandler.sendMessage(Message.obtain(progressHandler, -1101,
                        getResources().getString(R.string.common_precessing_text) + " (" + (i + 1) + "/"
                                + finalFileList.size() + "): <b>" + finalFileList.get(i).getName() + "</b><br/>"));
                hm.setProgressAbsB(i);
                progressHandler.sendMessage(Message.obtain(progressHandler, -1111, hm));
                if(finalFileList.size() > 1)
                    progressBarToken.setNotificationTitle(getResources().getString(R.string.common_app_fileEncryptor_name) + " (" + (i + 1) + "/" + finalFileList.size() + ")");

                try
                {
                    doEncDec(finalFileList.get(i), null);
                    if(Thread.interrupted() || progressBarToken.isInterrupted()) throw new InterruptedException(getResources().getString(R.string.common_canceledByUser));
                }
                catch (DataFormatException e)
                {
                    String message = e.getMessage();
                    try {
                        String[] messageParams = message.split("::");
                        if(messageParams.length > 1)
                            message = getResources().getString(Integer.parseInt(messageParams[0])).replaceAll("<1>", Matcher.quoteReplacement(messageParams[1]));
                        else
                            message = getResources().getString(Integer.parseInt(message));
                    } catch(Exception ie){};
                    sendPBTMessage(-401, message);
                    if(selectedItems.size() > 1) progressHandler.sendMessage(Message.obtain(progressHandler, -1113, false));
                    ok = false;
                } catch (EncryptorException e) {
                    String message = e.getMessage();
                    sendPBTMessage(-401, message);
                    if(selectedItems.size() > 1) progressHandler.sendMessage(Message.obtain(progressHandler, -1113, false));
                    ok = false;
                } catch (InterruptedException e) {
                    sendPBTMessage(-401, e.getMessage());
                    ok = false;
                    interrupted = true;
                    break;
                } catch (NoSuchAlgorithmException e) {
                    sendPBTMessage(-401, getResources().getString(R.string.common_unknownAlgorithm_text));
                    ok = false;
                } catch (Exception e) {
                    if(e.getMessage() != null && e.getMessage().equals("canceled"))
                        sendPBTMessage(-400, getResources().getString(R.string.common_canceledByUser));
                    else
                        sendPBTMessage(-400, Helpers.getShortenedStackTrace(e));
                    e.printStackTrace();
                    ok = false;
                }
            }
            hm.setProgressAbsB(finalFileList.size());
            if(!interrupted && !progressBarToken.isInterrupted()) progressHandler.sendMessage(Message.obtain(progressHandler, -1111, hm));
            progressHandler.sendMessage(Message.obtain(progressHandler, -998));
        }
        else // all to one file
        {
            try
            {
                doEncDec(null, filelist);
            }
            catch (DataFormatException e)
            {
                String message = e.getMessage();
                try {
                    String[] messageParams = message.split("::");
                    if(messageParams.length > 1)
                        message = getResources().getString(Integer.parseInt(messageParams[0])).replaceAll("<1>", Matcher.quoteReplacement(messageParams[1]));
                    else
                        message = getResources().getString(Integer.parseInt(message));
                }catch(Exception ie){};
                sendPBTMessage(-401, message);
            } catch (EncryptorException e) {
                String message = e.getMessage();
                sendPBTMessage(-401, message);
            } catch (InterruptedException e) {
                sendPBTMessage(-401, e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                sendPBTMessage(-401, getResources().getString(R.string.common_unknownAlgorithm_text));
            } catch (Exception e) {
                if(e.getMessage().equals("canceled"))
                    sendPBTMessage(-400, getResources().getString(R.string.common_canceledByUser));
                else
                    sendPBTMessage(-400, Helpers.getShortenedStackTrace(e));
                e.printStackTrace();
            }
        }

        StringBuffer outputPathsReport = new StringBuffer();
        if(progressBarToken.getCustomOutputDirectoryEncrypted() != null)
        {
            outputPathsReport.append("<br/>");
            outputPathsReport.append(getResources().getString(R.string.fe_report_outputFilePath));
            outputPathsReport.append(" (" + getResources().getString(R.string.common_encryption_text).toLowerCase() + "):<br/>");
            outputPathsReport.append(progressBarToken.getCustomOutputDirectoryEncrypted().getUniqueIdentifier());
        }
        if(progressBarToken.getCustomOutputDirectoryDecrypted() != null)
        {
            if(outputPathsReport.length() != 0) outputPathsReport.append("<br/>");
            outputPathsReport.append("<br/>");
            outputPathsReport.append(getResources().getString(R.string.fe_report_outputFilePath));
            outputPathsReport.append(" (" + getResources().getString(R.string.common_decryption_text).toLowerCase() + "):<br/>");
            outputPathsReport.append(progressBarToken.getCustomOutputDirectoryDecrypted().getUniqueIdentifier());
        }
        String outputPathsReportS = outputPathsReport.toString().trim();
        if(outputPathsReportS.length() > 0) progressHandler.sendMessage(Message.obtain(progressHandler, -1101, outputPathsReportS));

        updateCurrentFilesPhase2(currentDir, currentFilesTemp, showRoot);
        progressBarToken.getProgressHandler().sendMessage(Message.obtain(progressBarToken.getProgressHandler(), -200));
        progressBarToken.getProgressHandler().sendMessage(Message.obtain(progressBarToken.getProgressHandler(), -100));
        wakeLock.release();
    }

    /** Encrypt/Decrypt selected Folder/File */
    private synchronized void doEncDec(CryptFileWrapper finalFile, List<CryptFileWrapper> fileList) throws Exception
    {
        CryptFileWrapper inputFile = finalFile;
        if(inputFile == null)
        {
            progressBarToken.setIncludedFiles(fileList);
        }

        if (inputFile != null && !inputFile.exists())
        {
            throw new FileNotFoundException();
        }

        this.sendPBTMessage(FileEncActivity.FEA_PROGRESSHANDLER_SET_INPUTFILEPATH, inputFile);
        if(fileList != null) this.sendPBTMessage(FileEncActivity.FEA_PROGRESSHANDLER_SET_DIRFILENUMBER, Helpers.getNumberOfDirsAndFiles(fileList));

        if(inputFile != null && inputFile.isEncrypted()) //decryption
        {
            try {
                this.sendPBTMessage(FileEncActivity.FEA_PROGRESSHANDLER_SET_MAINMESSAGE, "decrypting");
                long start = System.currentTimeMillis();

                encryptor.unzipAndDecryptFile(inputFile, progressBarToken);
                if(selectedItems.size() > 1) progressHandler.sendMessage(Message.obtain(progressHandler, -1113, true));

                long time = (System.currentTimeMillis() - start);
                //SSElog.d("Dec Time: " + time + " : " + !nativeCodeDisabled + " : " + inputFile.getName() + " : " + inputFile.length());

                boolean deleteAfterDecryption = settingDataHolder.getItemAsBoolean("SC_FileEnc", "SI_DeleteAfterDecryption");
                if(deleteAfterDecryption)
                {
                    progressHandler.sendMessage(Message.obtain(progressHandler, -1101, getResources().getString(R.string.common_deleting_text)
                            + ": " + inputFile.getName() + "<br/>"));

                    boolean fileDeleted = inputFile.delete();
                    if(!fileDeleted)
                    {
                        universalHandler.sendMessage(Message.obtain(universalHandler, FileEncActivity.FEA_UNIVERSALHANDLER_SHOW_ERROR_TOAST,
                                getResources().getString(R.string.common_fileFailedToDelete).replaceAll("<1>", Matcher.quoteReplacement(inputFile.getName()))));
                        progressHandler.sendMessage(Message.obtain(progressHandler, -1212));
                    }
                }

            } catch (InterruptedException e) {

                ExtendedInterruptedException eie = (ExtendedInterruptedException)e;
                String message = eie.getMessage();

                if(progressBarToken.getRenderPhase() == 4) // wipe uncompleted file
                {
                    if(eie != null && eie.getAttachement() != null) Helpers.wipeFileOrDirectoryWrapped((CryptFileWrapper)eie.getAttachement(), progressBarToken, true);
                }
                throw new InterruptedException(message);
            }
        }
        else // encryption
        {
            try {
                this.sendPBTMessage(FileEncActivity.FEA_PROGRESSHANDLER_SET_MAINMESSAGE, "encrypting");
                long start = System.currentTimeMillis();

                encryptor.zipAndEncryptFile(inputFile, compress, progressBarToken);
                if(selectedItems.size() > 1 && !progressBarToken.getEncryptAllToOneFile()) progressHandler.sendMessage(Message.obtain(progressHandler, -1113, true));

                long time = (System.currentTimeMillis() - start);
                //SSElog.d("Enc Time: " + time + " : " + !nativeCodeDisabled + " : " + inputFile.getName() + " : " + inputFile.length());

                if(settingDataHolder.getItemAsBoolean("SC_FileEnc", "SI_WipeSourceFiles"))
                {
                    if(inputFile != null)
                    {
                        List<CryptFileWrapper> oneFileList = new ArrayList<CryptFileWrapper>();
                        oneFileList.add(inputFile);
                        Helpers.wipeFilesOrDirectoriesWrapped(oneFileList, progressBarToken);
                    }
                    else // all to one file
                    {
                        progressHandler.sendMessage(Message.obtain(progressHandler, -997, false));
                        DirectoryStats ds = Helpers.wipeFilesOrDirectoriesWrapped(progressBarToken.getIncludedFiles(), progressBarToken);
                        progressHandler.sendMessage(Message.obtain(progressHandler, -1211, ds));
                    }
                }

            } catch (Exception e) {

                if(progressBarToken.getRenderPhase() == 2)
                {
                    CryptFileWrapper tf = null;
                    String inputFileName = (inputFile != null) ? inputFile.getName() : progressBarToken.getCustomFileName();
                    CryptFileWrapper targetDir = progressBarToken.getCustomOutputDirectoryEncrypted();
                    if(targetDir == null) targetDir = currentDir;
                    tf = targetDir.findFile(inputFileName + "." + Encryptor.ENC_FILE_EXTENSION + "." + Encryptor.ENC_FILE_UNFINISHED_EXTENSION);
                    if(tf != null) tf.delete();
                }
                throw e;
            }
        }
    }

    /** Wiping Service */
    private synchronized void wipe()
    {
        PowerManager.WakeLock wakeLock;
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.paranoiaworks.unicus.android.sse:FE_WIPE");

        wakeLock.acquire();
        try {
            if(selectedItems.size() == 1)
                Helpers.wipeFileOrDirectoryWrapped(selectedItems.get(0), progressBarToken);
            else
                Helpers.wipeFilesOrDirectoriesWrapped(selectedItems, progressBarToken);
        } catch (InterruptedException e) {
            sendPBTMessage(-401, e.getMessage());
        } catch (Exception e) {
            sendPBTMessage(-400, e.getLocalizedMessage());
            e.printStackTrace();
        } finally {
            updateCurrentFilesPhase2(currentDir, currentFilesTemp, showRoot);
            progressBarToken.getProgressHandler().sendMessage(Message.obtain(progressBarToken.getProgressHandler(), -100));
            wakeLock.release();
        }
    }

    /** Deleting Service */
    private synchronized void delete()
    {
        progressBarToken.setNotificationText(getResources().getString(R.string.common_deleting_text));

        PowerManager.WakeLock wakeLock;
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.paranoiaworks.unicus.android.sse:FE_DELETE");

        wakeLock.acquire();
        try {
            Helpers.deleteDirectoriesWrapped(selectedItems);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            updateCurrentFilesPhase2(currentDir, currentFilesTemp, showRoot);
            universalHandler.sendMessage(Message.obtain(universalHandler, FileEncActivity.FEA_UNIVERSALHANDLER_REFRESH_FILELIST_P13));
            wakeLock.release();
        }
    }

    public static synchronized void updateCurrentFilesPhase2(CryptFileWrapper currentDir, List<CryptFileWrapper> currentFilesTemp, boolean showRoot)
    {
        currentFilesTemp.clear();

        if (currentDir.getParentFile() != null  && (currentDir.getParentFile().getUniqueIdentifier().length() > 1 || showRoot)) // restrict ROOT
        {
            CryptFileWrapper backDir = currentDir.getParentFile();
            backDir.setBackDir(true);
            if(backDir.listFiles() != null) currentFilesTemp.add(backDir);
        }

        {
            CryptFileWrapper[] tempList = currentDir.listFiles(true);
            if (tempList != null)
            {
                for (int j = 0; j < tempList.length; ++j)
                {
                    currentFilesTemp.add(tempList[j]);
                }
            }
        }

        Collections.sort(currentFilesTemp);
    }

    private synchronized String createChannel()
    {
        if(Build.VERSION.SDK_INT < 26) return "";

        NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        String id = CHANNEL_ID;
        NotificationChannel mChannel = new NotificationChannel(id, id, NotificationManager.IMPORTANCE_LOW);

        mChannel.enableLights(true);
        mChannel.setLightColor(Color.BLUE);
        mChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        mChannel.enableVibration(false);
        if (mNotificationManager != null) {
            mNotificationManager.createNotificationChannel(mChannel);
        } else {
            stopSelf();
        }
        return id;
    }

    /** ShortCut for progressBarToken...sendMessage...  */
    private void sendPBTMessage(int message, Object attachement)
    {
        progressBarToken.getProgressHandler().sendMessage(Message.obtain(progressBarToken.getProgressHandler(),
                message,
                attachement
        ));
    }
}
