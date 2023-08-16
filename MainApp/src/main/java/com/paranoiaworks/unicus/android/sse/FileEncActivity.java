package com.paranoiaworks.unicus.android.sse;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.paranoiaworks.android.sse.interfaces.Lockable;
import com.paranoiaworks.unicus.android.sse.adapters.FileEncArrayAdapter;
import com.paranoiaworks.unicus.android.sse.components.CustomAlertDialog;
import com.paranoiaworks.unicus.android.sse.components.DualProgressDialog;
import com.paranoiaworks.unicus.android.sse.components.EncDecChoiceDialog;
import com.paranoiaworks.unicus.android.sse.components.HtmlAlertDialog;
import com.paranoiaworks.unicus.android.sse.components.ImageToast;
import com.paranoiaworks.unicus.android.sse.components.PasswordDialog;
import com.paranoiaworks.unicus.android.sse.components.ScreenLockDialog;
import com.paranoiaworks.unicus.android.sse.components.SelectionDialog;
import com.paranoiaworks.unicus.android.sse.components.SimpleHTMLDialog;
import com.paranoiaworks.unicus.android.sse.components.SimpleWaitDialog;
import com.paranoiaworks.unicus.android.sse.dao.ActivityMessage;
import com.paranoiaworks.unicus.android.sse.dao.PasswordAttributes;
import com.paranoiaworks.unicus.android.sse.misc.CryptFile;
import com.paranoiaworks.unicus.android.sse.misc.CryptFileWrapper;
import com.paranoiaworks.unicus.android.sse.misc.ProgressBarToken;
import com.paranoiaworks.unicus.android.sse.misc.ProgressMessage;
import com.paranoiaworks.unicus.android.sse.misc.RenderPhase;
import com.paranoiaworks.unicus.android.sse.services.FileEncryptionService;
import com.paranoiaworks.unicus.android.sse.utils.ComponentProvider;
import com.paranoiaworks.unicus.android.sse.utils.Encryptor;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;

import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import ext.com.andraskindler.quickscroll.QuickScroll;
import ext.com.nononsenseapps.filepicker.FilePickerActivity;

/**
 * File Encryptor activity class
 *
 * @author Paranoia Works
 * @version 1.1.13
 */
public class FileEncActivity extends CryptActivity implements Lockable {

    private int encryptAlgorithmCode;
    private boolean askOnLeave;
    private int lockOnPause = -1;
    private boolean nativeCodeDisabled;
    private boolean replaceEncWarning;
    private boolean showRoot = false;
    private boolean compress = false;
    private boolean sessionPasswordActive = false;
    private boolean startFromFileSystem = false;
    private boolean sldVeto = false;
    private static boolean pbLock = false;
    private static boolean fileUpdateLock = false;
    private Map<String, Integer> scrollPositionMap = new HashMap<String, Integer>();
    private ProgressBarToken progressBarToken;
    private ListView filesListView;
    private PasswordDialog passwordDialog;
    private List<String> tips = new ArrayList<String>();
    private String predefinedScreenLockKey;
    private Context context;
    private long timeCheck = 0;
    private boolean readOnlyDirAlertDisplayed = false;
    private List<String> uninstallWarningPathsList;


    // Files Related
    private List<CryptFileWrapper> availableVolumesList;
    private CryptFileWrapper currentDir;
    private TreeMap<String, CryptFileWrapper> selectedItemsMap = new TreeMap<String, CryptFileWrapper>();
    private FileEncArrayAdapter fileArrayAdapter;
    private List<CryptFileWrapper> currentFiles = new ArrayList<CryptFileWrapper>();
    private List<CryptFileWrapper> currentFilesTemp = new ArrayList<CryptFileWrapper>();
    private OutputParametersBean outputParameters;
    private CryptFileWrapper safOutputOnlyDir;
    private File primaryMediaDir = null;
    private boolean multiSelection = false;
    private int encFileDestination = 0;
    private int decFileDestination = 0;
    private int thumbnailSizeCode = 0;

    private ViewGroup rootLayout;
    private TextView topTextView;
    private TextView bottomTextView;
    private Button startEncDecButton;
    private Button helpButton;
    private Button toMainPageButton;
    private Button compressButton;
    private Button sessionPasswordButton;
    private Button moreButton;
    private Button homeButton;
    private Button safButton;
    private Button selectDirButton;
    private Dialog waitDialog;
    private ScreenLockDialog sld;
    private QuickScroll quickscroll;
    private RenderPhase renderPhase = new RenderPhase();


    private Thread dirSizeThread;
    private Thread encDecThread;
    private Thread volumeSizeThread;
    private Thread wipeThread;

    private static final int FILE_DESTINATION_ASSOURCE = 0;
    private static final int FILE_DESTINATION_CUSTOM = 1;
    private static final int FILE_DESTINATION_ASK = 2;

    private static final int REQUEST_CODE_SAF = 100;
    private static final int REQUEST_CODE_DIRCHOOSER_ENCRYPTION = 101;
    private static final int REQUEST_CODE_DIRCHOOSER_DECRYPTION = 102;
    private static final int REQUEST_CODE_SAF_OUTPUT_ONLY_ENCRYPTION = 103;
    private static final int REQUEST_CODE_SAF_OUTPUT_ONLY_DECRYPTION = 104;

    private static final int FEA_MESSAGE_DIALOG_FILEACTION = -3101;
    private static final int FEA_MESSAGE_DIALOG_FILEACTION_DELETE_CONFIRM = -3102;
    private static final int FEA_MESSAGE_DIALOG_FILEACTION_RENAME_CONFIRM = -3103;
    private static final int FEA_MESSAGE_DIALOG_FILEACTION_WIPE_CONFIRM = -3104;
    private static final int FEA_MESSAGE_RENDER_CANCEL_CONFIRM = -3105;
    private static final int FEA_MESSAGE_DIALOG_HOMEACTION = -3106;
    private static final int FEA_MESSAGE_DIALOG_HOMEACTION_HOMESET_CONFIRM = -3107;
    private static final int FEA_MESSAGE_DIALOG_SELECT_DIRLIST = -3108;
    private static final int FEA_MESSAGE_DIALOG_SAF_FIRSTRUN = -3109;
    private static final int FEA_MESSAGE_READONLY_DIALOG_AFTER = -3110;
    private static final int FEA_MESSAGE_AFTERENCRYPT_REPORT = -3111;
    private static final int FEA_MESSAGE_AFTERENCRYPT_DELETE_ASK = -3112;
    private static final int FEA_MESSAGE_DIALOG_FILEACTION_CREATE_FOLDER_CONFIRM = -3113;

    public static final int FEA_MESSAGE_DIALOG_ENCDECCHOICE = -3120;

    public static final int FEA_MESSAGE_OUTPUTDIR_SELECTION = -3130;
    public static final int FEA_MESSAGE_OUTPUTDIR_FILEREPLACE_CONFIRM = -3131;

    public static final int FEA_PROGRESSHANDLER_SET_MAINMESSAGE = -3201;
    public static final int FEA_PROGRESSHANDLER_SET_INPUTFILEPATH = -3202;
    public static final int FEA_PROGRESSHANDLER_SET_OUTPUTFILEPATH = -3203;
    public static final int FEA_PROGRESSHANDLER_SET_ERRORMESSAGE = -3204;
    public static final int FEA_PROGRESSHANDLER_SET_DIRFILENUMBER = -3205;

    public static final int FEA_UNIVERSALHANDLER_SHOW_WAITDIALOG = -3301;
    public static final int FEA_UNIVERSALHANDLER_HIDE_WAITDIALOG = -3302;
    public static final int FEA_UNIVERSALHANDLER_REFRESH_FILELIST_P13 = -3303;
    public static final int FEA_UNIVERSALHANDLER_SHOW_DIRSIZE = -3304;
    public static final int FEA_UNIVERSALHANDLER_SHOW_VOLUMESIZE = -3305;
    public static final int FEA_UNIVERSALHANDLER_SELECT_DIR = -3306;
    public static final int FEA_UNIVERSALHANDLER_SHOW_ERROR_TOAST = -3307;
    public static final int FEA_UNIVERSALHANDLER_SHOW_OK_TOAST = -3308;
    public static final int FEA_UNIVERSALHANDLER_SHOW_ERROR_DIALOG = -3309;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.la_fileenc);
        encryptAlgorithmCode = settingDataHolder.getItemAsInt("SC_FileEnc", "SI_Algorithm");
        multiSelection = settingDataHolder.getItemAsBoolean("SC_FileEnc", "SI_MultiSelection");
        askOnLeave = settingDataHolder.getItemAsBoolean("SC_Common", "SI_AskIfReturnToMainPage");
        lockOnPause = settingDataHolder.getItemAsInt("SC_FileEnc", "SI_LockScreenTimeFE");
        nativeCodeDisabled = settingDataHolder.getItemAsBoolean("SC_Common", "SI_NativeCodeDisable");
        replaceEncWarning = settingDataHolder.getItemAsBoolean("SC_FileEnc", "SI_EncFileExistWarning");
        encFileDestination = settingDataHolder.getItemAsInt("SC_FileEnc", "SI_EncFileDestination");
        decFileDestination = settingDataHolder.getItemAsInt("SC_FileEnc", "SI_DecFileDestination");
        thumbnailSizeCode = settingDataHolder.getItemAsInt("SC_FileEnc", "SI_ThumbnailSize");
        context = this;

        if ((new HtmlAlertDialog(this, getResources().getString(R.string.alerts_dir) + "Alert_UninstallWarning.html", "")).getWillBeShown())
            uninstallWarningPathsList = Helpers.getAppDirsCanonicalPaths(this);

        //Intent - External password, External File Path
        final android.content.Intent intent = getIntent();
        List<CryptFileWrapper> externalFiles = null;
        if (intent != null) {
            char[] predefinedPassword = Helpers.toChars(intent.getCharSequenceExtra(PasswordVaultActivity.PWV_EXTRA_PASSWORD));
            if (predefinedPassword != null) {
                String screenLockKey = intent.getStringExtra(PasswordVaultActivity.PWV_EXTRA_LOCKSCREEN_KEY);

                lockOnPause = intent.getIntExtra(PasswordVaultActivity.PWV_EXTRA_LOCKSCREEN_ON, -1);
                if (lockOnPause > -1) predefinedScreenLockKey = screenLockKey;
                sldVeto = true;

                encryptor = null;
                showPasswordDialogSetMode(predefinedPassword);
            } else {
                externalFiles = getExternalFilesFromIntent(intent, this);

                sessionPasswordActive = false;
                encryptor = null;
                if (sessionPasswordButton != null)
                    sessionPasswordButton.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.key_inact), null, null, null);

                if (externalFiles != null) {
                    startFromFileSystem = true;
                }
            }
        }

        //GUI parameters
        topTextView = (TextView) findViewById(R.id.FE_topTextView);
        bottomTextView = (TextView) findViewById(R.id.FE_bottomTextView);
        filesListView = (ListView) findViewById(R.id.FE_list);
        rootLayout = (ViewGroup) findViewById(R.id.FE_rootLayout);
        setTitle(getResources().getString(R.string.common_app_fileEncryptor_name));


        // Available directories
        availableVolumesList = Helpers.getExtDirectoriesWrapped(getApplicationContext(), false);

        // Button - Start Encryption/Decryption of file
        startEncDecButton = (Button) findViewById(R.id.FE_startbtn);
        startEncDecButton.setEnabled(false);
        startEncDecButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (detectDoubleClick()) return;

                if (FileEncryptionService.isRunning()) {
                    terminateRunningTasks();
                } else {
                    startEncDecDispatcher();
                }
            }
        });

        // More Button
        moreButton = (Button) findViewById(R.id.FE_moreButton);
        moreButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (detectDoubleClick()) return;

                showFileActionDialog();
            }
        });

        // Compress Button
        compressButton = (Button) findViewById(R.id.FE_compressButton);
        compressButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                compress = !compress;
                Drawable img = null;
                if (compress) img = getResources().getDrawable(R.drawable.compress_act);
                else img = getResources().getDrawable(R.drawable.compress_inact);
                compressButton.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null);
                String text = compress ? getResources().getString(R.string.fe_compression_on) : getResources().getString(R.string.fe_compression_off);
                ImageToast it = new ImageToast(text, ImageToast.TOAST_IMAGE_INFO, FileEncActivity.this);
                it.setDuration(Toast.LENGTH_SHORT);
                it.show();
            }
        });

        // Set Session Password Button
        sessionPasswordButton = (Button) findViewById(R.id.FE_setSessionPasswordButton);
        sessionPasswordButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (detectDoubleClick()) return;

                if (sessionPasswordActive) {
                    sessionPasswordActive = false;
                    if (encryptor != null && !pbLock) {
                        wipeEncryptor();
                    }
                    sessionPasswordButton.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.key_inact), null, null, null);
                } else {
                    showPasswordDialogSetMode(null);
                }
            }
        });

        // To Main Menu Button
        toMainPageButton = (Button) findViewById(R.id.FE_toMainPageButton);
        toMainPageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (detectDoubleClick()) return;

                if (askOnLeave && !startFromFileSystem) {
                    showExitDialog();
                } else setMessage(new ActivityMessage(COMMON_MESSAGE_CONFIRM_EXIT, null));
            }
        });

        // Help Button
        this.helpButton = (Button) this.findViewById(R.id.FE_helpButton);
        this.helpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (detectDoubleClick()) return;

                SimpleHTMLDialog simpleHTMLDialog = new SimpleHTMLDialog(v); //Help dialog
                simpleHTMLDialog.addValue("API_VERSION", Integer.toString(android.os.Build.VERSION.SDK_INT));
                simpleHTMLDialog.loadURL(getResources().getString(R.string.helpLink_FileEncryptor));
                simpleHTMLDialog.show();
            }
        });


        //+ Create Top Buttons Line (shortcuts to available volumes)
        LinearLayout rl = (LinearLayout) findViewById(R.id.FE_topLinearLayout);


        Button listButton = (Button) getLayoutInflater().inflate(R.layout.lc_smallbutton_list_template, null);
        //listButton.setText("List");
        listButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                List<File> paths = Helpers.getExtDirectories(context, true);

                List<String> title = new ArrayList<String>();
                List<String> comment = new ArrayList<String>();
                List<Integer> icon = new ArrayList<Integer>();
                List<Object> tag = new ArrayList<Object>();

                if (paths.size() > 0) {
                    boolean okft = false;
                    try {
                        CryptFileWrapper testFile = new CryptFileWrapper(new CryptFile(paths.get(1)));
                        if (testFile.listFiles() != null && testFile.listFiles().length > 0)
                            okft = true;
                    } catch (Exception e) {
                        // N/A
                    }

                    if (okft) {
                        for (int i = 1; i < paths.size(); ++i) {
                            title.add(i == 1 ? Helpers.capitalizeAllFirstLetters(getResources().getString(R.string.common_internal_storage)) : File.separator + paths.get(i).getName());
                            comment.add(paths.get(i).getAbsolutePath());
                            icon.add(R.drawable.sdcard);
                            tag.add(paths.get(i).getAbsolutePath());
                        }
                    }
                }

                if (android.os.Build.VERSION.SDK_INT >= 21) // Android 5 and above
                {
                    File[] externalMedia = getExternalMediaDirs();
                    if (externalMedia != null) {
                        for (int i = 0; i < externalMedia.length; ++i) {
                            if (externalMedia[i] == null || externalMedia[i].getAbsolutePath() == null)
                                continue;
                            if (i == 0)
                                title.add("Int. Media Dir");
                            else if (i == 1)
                                title.add("Ext. Media Dir");
                            else
                                title.add("Media Dir " + (i + 1));
                            comment.add(externalMedia[i].getAbsolutePath());
                            icon.add(R.drawable.diskdir);
                            tag.add(externalMedia[i].getAbsolutePath());
                        }
                    }
                }

                if (paths.size() > 2 && !Helpers.isAndroid_11P()) {
                    CryptFileWrapper apprwdir = new CryptFileWrapper(new CryptFile(paths.get(2) + "/Android/data/com.paranoiaworks.unicus.android.sse"));
                    if (!apprwdir.exists()) apprwdir.getFile().mkdir();
                    if (apprwdir.exists() && apprwdir.getWritePermissionLevelForDir() > 0) {
                        title.add("Ext. SDCARD Data Dir");
                        comment.add(apprwdir.getAbsolutePath());
                        icon.add(R.drawable.diskdir);
                        tag.add(apprwdir.getAbsolutePath());
                    }
                }

                if (Helpers.isAndroid_11P()) {
                    File[] externalFile = getExternalFilesDirs(null);
                    if (externalFile != null) {
                        for (int i = 0; i < externalFile.length; ++i) {
                            if (externalFile[i] == null || externalFile[i].getParentFile() == null || externalFile[i].getParentFile().getAbsolutePath() == null)
                                continue;
                            if (i == 0)
                                title.add("Int. Data Dir");
                            else if (i == 1)
                                title.add("Ext. Data Dir");
                            else
                                title.add("Data Dir " + (i + 1));
                            comment.add(externalFile[i].getParentFile().getAbsolutePath());
                            icon.add(R.drawable.diskdir);
                            tag.add(externalFile[i].getParentFile().getAbsolutePath());
                        }
                    }
                }

                if (!Helpers.isAndroid_11P()) {
                    String storagePath = "/storage";
                    if (new File(storagePath).exists()) {
                        title.add("Storage Dir");
                        comment.add(storagePath);
                        icon.add(R.drawable.diskdir);
                        tag.add(storagePath);
                    } else {
                        storagePath = "/mnt";
                        if (new File(storagePath).exists()) {
                            title.add(storagePath);
                            comment.add(storagePath);
                            icon.add(R.drawable.diskdir);
                            tag.add(storagePath);
                        }
                    }
                }

                try {
                    File root = new File(File.separator);
                    if (root.exists() && root.isDirectory() && root.list().length > 0) {
                        title.add("ROOT");
                        comment.add(File.separator);
                        icon.add(R.drawable.diskdir);
                        tag.add(File.separator);
                    }
                } catch (Exception e) {
                    // swallow
                }

                SelectionDialog selectDirDialog = new SelectionDialog(v, title, comment, icon, tag);
                selectDirDialog.setMessageCode(FEA_MESSAGE_DIALOG_SELECT_DIRLIST);
                selectDirDialog.show();
            }
        });
        rl.addView((View) listButton);
        rl.addView((FrameLayout) getLayoutInflater().inflate(R.layout.lct_delimiter_thin_invisible, null));


        // Internal Storage Button
        if (availableVolumesList.size() > 1 && !Helpers.isAndroid11PorHflavor()) {
            Button isButton = (Button) getLayoutInflater().inflate(R.layout.lc_smallbutton_template, null);
            isButton.setText("Int.SD");
            isButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (detectDoubleClick()) return;

                    showRoot = false;
                    currentDir = new CryptFileWrapper(availableVolumesList.get(1).getAbsolutePath(), CryptFileWrapper.MODE_FILE, context);
                    updateCurrentFiles();
                    fileArrayAdapter.notifyDataSetChanged();
                    filesListView.setSelectionAfterHeaderView();
                }
            });
            rl.addView((View) isButton);
            rl.addView((FrameLayout) getLayoutInflater().inflate(R.layout.lct_delimiter_thin_invisible, null));
        }

        // Storage Access Framework Button
        if (android.os.Build.VERSION.SDK_INT >= 21 && !Helpers.isAndroid11PorHflavor()) // Android 5 and above
        {
            safButton = (Button) getLayoutInflater().inflate(R.layout.lc_smallbutton_template, null);
            safButton.setText("SAF");
            safButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (detectDoubleClick()) return;

                    HtmlAlertDialog safFirstRunDialog = new HtmlAlertDialog(v, getResources().getString(R.string.alerts_dir) + "Alert_SAF_FirstStart.html", "SAF Mode");
                    if (safFirstRunDialog.getWillBeShown()) {
                        safFirstRunDialog.setMessageCode(FEA_MESSAGE_DIALOG_SAF_FIRSTRUN);
                        safFirstRunDialog.show();
                    } else {
                        ((CryptActivity) context).setMessage(new ActivityMessage(FEA_MESSAGE_DIALOG_SAF_FIRSTRUN, null, null));
                    }
                }
            });
            rl.addView((View) safButton);
        }

        //Select dir button
        if (Helpers.isAndroid11PorHflavor()) {
            selectDirButton = (Button) getLayoutInflater().inflate(R.layout.lc_smallbutton_selectdir_template, null);
            selectDirButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (detectDoubleClick()) return;

                    HtmlAlertDialog safFirstRunDialog = new HtmlAlertDialog(v, getResources().getString(R.string.alerts_dir) + "Alert_SAF_FirstStart_11.html", "SAF Mode");
                    if (safFirstRunDialog.getWillBeShown()) {
                        safFirstRunDialog.setMessageCode(FEA_MESSAGE_DIALOG_SAF_FIRSTRUN);
                        safFirstRunDialog.show();
                    } else {
                        ((CryptActivity) context).setMessage(new ActivityMessage(FEA_MESSAGE_DIALOG_SAF_FIRSTRUN, null, null));
                    }
                }
            });
            rl.addView((View) selectDirButton);
        }

        //- Create Top Buttons Line (shortcuts to available volumes)


        // Home Button
        homeButton = (Button) findViewById(R.id.FE_homeButton);
        homeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (detectDoubleClick()) return;

                List<String> title = new ArrayList<String>();
                List<String> comment = new ArrayList<String>();
                List<Integer> icon = new ArrayList<Integer>();
                List<Object> tag = new ArrayList<Object>();
                List<Integer> itemType = new ArrayList<Integer>();

                title.add(getResources().getString(R.string.fe_favouriteDialog_homeDir));
                tag.add("fe_homeactionDialog_goHome");
                icon.add(R.drawable.home);
                comment.add(getHomeDir("FAVOURITES_HOMEDIR") != null ? getHomeDir("FAVOURITES_HOMEDIR").getUniqueIdentifier() : null);
                itemType.add(SelectionDialog.ITEMTYPE_NORMAL);
                if (getHomeDir("FAVOURITES_LASTSAF") != null) {
                    title.add(getResources().getString(R.string.fe_favouriteDialog_goToLastSAF));
                    tag.add("fe_homeactionDialog_goToLastSAF");
                    comment.add(getHomeDir("FAVOURITES_LASTSAF").getUniqueIdentifier());
                    icon.add(R.drawable.diskdir);
                    itemType.add(SelectionDialog.ITEMTYPE_NORMAL);
                }
                if (encFileDestination != FILE_DESTINATION_ASSOURCE) {
                    title.add(getResources().getString(R.string.fe_favouriteDialog_encFileDir));
                    icon.add(R.drawable.diskdir);
                    tag.add("fe_homeactionDialog_goToEncFilesDir");
                    CryptFileWrapper dir = getEncFilesDir();
                    if (dir != null) {
                        comment.add(dir.getUniqueIdentifier());
                        itemType.add(SelectionDialog.ITEMTYPE_NORMAL);
                    } else {
                        comment.add("(" + getResources().getString(R.string.common_unavailable_text) + ")");
                        itemType.add(SelectionDialog.ITEMTYPE_INACTIVE);
                    }
                }
                if (decFileDestination != FILE_DESTINATION_ASSOURCE) {
                    title.add(getResources().getString(R.string.fe_favouriteDialog_decFileDir));
                    icon.add(R.drawable.diskdir);
                    tag.add("fe_homeactionDialog_goToDecFilesDir");
                    CryptFileWrapper dir = getDecFilesDir();
                    if (dir != null) {
                        comment.add(dir.getUniqueIdentifier());
                        itemType.add(SelectionDialog.ITEMTYPE_NORMAL);
                    } else {
                        comment.add("(" + getResources().getString(R.string.common_unavailable_text) + ")");
                        itemType.add(SelectionDialog.ITEMTYPE_INACTIVE);
                    }
                }
                if (currentDir.getMode() == CryptFileWrapper.MODE_FILE) {
                    title.add(getResources().getString(R.string.fe_favouriteDialog_setAs));
                    tag.add("fe_homeactionDialog_setAsDialog");
                    icon.add(R.drawable.add_new);
                    comment.add(null);
                    itemType.add(SelectionDialog.ITEMTYPE_NORMAL);
                }

                SelectionDialog selectDirDialog = new SelectionDialog(FileEncActivity.this, title, comment, icon, tag, getResources().getString(R.string.fe_favouriteDialog_title), true);
                selectDirDialog.setMessageCode(FEA_MESSAGE_DIALOG_HOMEACTION);
                selectDirDialog.setCommentEllipsize(TruncateAt.START);
                selectDirDialog.setItemTypes(itemType);
                selectDirDialog.deferredInit();
                selectDirDialog.show();
            }
        });

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            File[] mdirs = getExternalMediaDirs();
            if (mdirs != null && mdirs.length > 0)
                primaryMediaDir = mdirs[0];
        }

        if (externalFiles != null) {
            if (externalFiles.get(0).isFile())
                currentDir = externalFiles.get(0).getParentFile();
            else if (externalFiles.get(0).isDirectory())
                currentDir = externalFiles.get(0);
        } else if (getHomeDir("FAVOURITES_HOMEDIR") != null)
            currentDir = getHomeDir("FAVOURITES_HOMEDIR");
        else if (availableVolumesList.size() > 1 && availableVolumesList.get(1) != null && availableVolumesList.get(1).isDirectory() && availableVolumesList.get(1).getWritePermissionLevelForDir() > 1 && availableVolumesList.get(1).listFiles() != null)
            currentDir = availableVolumesList.get(1); // second dir in the list use as start dir
        else
            currentDir = Helpers.isAndroid11PorHflavor() && primaryMediaDir != null ? new CryptFileWrapper(new CryptFile(primaryMediaDir)) : availableVolumesList.get(0); //ROOT
        updateCurrentFiles();

        //+ Create File List View
        {
            fileArrayAdapter = (new FileEncArrayAdapter(this, currentFiles));
            fileArrayAdapter.setSelectHandler(universalHandler);
            RelativeLayout emptyView = (RelativeLayout) findViewById(R.id.FE_list_empty);
            ((TextView) emptyView.getChildAt(0)).setText(getResources().getString(R.string.fe_emptyDir_text));
            filesListView.setEmptyView(emptyView);
            filesListView.setAdapter(fileArrayAdapter);

            if (android.os.Build.VERSION.SDK_INT >= 12) {
                quickscroll = (QuickScroll) findViewById(R.id.FE_quickscroll);
                quickscroll.init(QuickScroll.TYPE_INDICATOR_WITH_HANDLE, filesListView, fileArrayAdapter, QuickScroll.STYLE_HOLO);
                quickscroll.setFixedSize(1);
                quickscroll.setMinAllVsVisibleRatio(5);
                quickscroll.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 42);
            }

            //click on item (file)
            filesListView.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final CryptFileWrapper clickedFile = currentFiles.get(position);

                    // if Directory
                    if (clickedFile.isDirectory()) {
                        if (detectDoubleClick(false)) return;
                        if (handleFileUpdateLock()) return;

                        scrollPositionMap.put(currentDir.getUniqueIdentifier(), filesListView.getFirstVisiblePosition());
                        currentDir = clickedFile;
                        checkCurrentDirectoryForWarnings();

                        final Handler ufh = new Handler() {
                            public void handleMessage(Message msg) {
                                if (msg.what == 1) {
                                    updateCurrentFilesPhase3();
                                    if (waitDialog != null) waitDialog.cancel();
                                    fileArrayAdapter.notifyDataSetChanged();
                                    if (clickedFile.isBackDir())
                                        setHistoricScrollPosition(clickedFile);
                                    else filesListView.setSelectionAfterHeaderView();
                                }
                            }
                        };

                        updateCurrentFiles(ufh);
                    }
                    // if File
                    else if (clickedFile.isFile()) {
                        selectFile(clickedFile);
                    }
                }
            });
            filesListView.setOnItemLongClickListener(new OnItemLongClickListener() {
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    final CryptFileWrapper clickedFile = currentFiles.get(position);
                    //CryptFileWrapper parentFile = clickedFile.getParentFile();

                    if (clickedFile.isBackDir()) return false;

                    // Directory
                    if (clickedFile.isDirectory()) {
                        selectDir(clickedFile);
                    }
                    // File
                    else if (clickedFile.isFile()) {
                        List<String> itemList = new ArrayList<String>();
                        List<String> commentList = new ArrayList<String>();
                        List<Object> keyList = new ArrayList<Object>();
                        List<Integer> itemType = new ArrayList<Integer>();

                        if (FileEncryptionService.isRunning()) {
                            itemList.add(getResources().getString(R.string.fes_terminateRunningTasks));
                            commentList.add(null);
                            keyList.add("fes_terminateRunningTasks");
                            itemType.add(SelectionDialog.ITEMTYPE_HIGHLIGHTED);
                        }

                        if (currentDir.getWritePermissionLevelForDir() > 0) {
                            itemList.add(getResources().getString(R.string.fe_fileactionDialog_renameFile));
                            commentList.add(null);
                            keyList.add("fe_fileactionDialog_renameFile");
                            itemType.add(SelectionDialog.ITEMTYPE_NORMAL);

                            itemList.add(getResources().getString(R.string.fe_fileactionDialog_deleteFile));
                            keyList.add("fe_fileactionDialog_deleteFile");
                            if (FileEncryptionService.isRunning()) {
                                itemType.add(SelectionDialog.ITEMTYPE_INACTIVE);
                                commentList.add("(" + getResources().getString(R.string.fes_terminateRunningTasks).toLowerCase() + ")");
                            } else {
                                itemType.add(SelectionDialog.ITEMTYPE_NORMAL);
                                commentList.add(null);
                            }

                            if (currentDir.getMode() == CryptFileWrapper.MODE_FILE) {
                                itemList.add(getResources().getString(R.string.fe_fileactionDialog_wipeFile));
                                keyList.add("fe_fileactionDialog_wipeFile");
                                if (FileEncryptionService.isRunning()) {
                                    itemType.add(SelectionDialog.ITEMTYPE_INACTIVE);
                                    commentList.add("(" + getResources().getString(R.string.fes_terminateRunningTasks).toLowerCase() + ")");
                                } else {
                                    itemType.add(SelectionDialog.ITEMTYPE_NORMAL);
                                    commentList.add("(" + getResources().getString(R.string.common_useHelpForDetails) + ")");
                                }
                            }
                        }
                        if (!clickedFile.isEncrypted()) {
                            itemList.add(getResources().getString(R.string.fe_fileactionDialog_openFile));
                            commentList.add(null);
                            keyList.add("fe_fileactionDialog_openFile");
                            itemType.add(SelectionDialog.ITEMTYPE_NORMAL);
                        }
                        itemList.add(getResources().getString(R.string.fe_fileactionDialog_sendFile));
                        commentList.add(null);
                        keyList.add("fe_fileactionDialog_sendFile");
                        itemType.add(SelectionDialog.ITEMTYPE_NORMAL);

                        SelectionDialog fileActionDialog = new SelectionDialog(FileEncActivity.this,
                                itemList,
                                commentList, null,
                                keyList,
                                getResources().getString(R.string.fe_file_dialogTitle),
                                true);
                        fileActionDialog.setAttachment(clickedFile);
                        fileActionDialog.setItemTypes(itemType);
                        fileActionDialog.deferredInit();
                        fileActionDialog.setMessageCode(FEA_MESSAGE_DIALOG_FILEACTION);

                        if (fileActionDialog != null) fileActionDialog.show();
                    }

                    return true;
                }
            });
        }
        //- Create File List View

        // External File Path
        if (externalFiles != null && externalFiles.get(0).isFile()) {
            int externalFileIndex = getFileIndex(externalFiles.get(0));
            if (externalFileIndex < 0) return;

            if (!multiSelection || externalFiles.size() == 1) {
                externalFiles.set(0, currentFiles.get(externalFileIndex));
                setSelectedItem(externalFiles.get(0));
                fileArrayAdapter.notifyDataSetChanged();
                filesListView.setSelectionFromTop(externalFileIndex, 0);

                moreButton.setEnabled(true);
                startEncDecButton.setEnabled(true);
                if (externalFiles.get(0).isEncrypted())
                    startEncDecButton.setText(getResources().getString(R.string.fe_goButtonDecFile));
                else
                    startEncDecButton.setText(getResources().getString(R.string.fe_goButtonEncFile));
                outputParameters = new OutputParametersBean();
                if (!FileEncryptionService.isRunning()) startEncDecSingleFileDispatcher();
            } else {
                selectAll(externalFiles);
                filesListView.setSelectionFromTop(externalFileIndex, 0);

                outputParameters = new OutputParametersBean();
                if (!verifyMultiSelectionShowDialog()) return;

                if (selectedItemsMap.size() > 1 && !FileEncryptionService.isRunning()) {
                    EncDecChoiceDialog encDecChoiceDialog = new EncDecChoiceDialog(this, selectedItemsMap);
                    if (encFileDestination == FILE_DESTINATION_CUSTOM && getEncFilesDir() != null)
                        encDecChoiceDialog.setCustumEncFileDestination(getEncFilesDir());
                    if (encFileDestination == FILE_DESTINATION_ASK)
                        encDecChoiceDialog.setSuppressRewriteWarning(true);
                    encDecChoiceDialog.show();
                }
            }
        }
    }


    /**
     * Handle Message
     */
    void processMessage() {
        ActivityMessage am = getMessage();
        if (am == null) return;

        int messageCode = am.getMessageCode();
        //SSElog.d("Activity Message: ", ""  + messageCode);
        switch (messageCode) {
            case CryptActivity.COMMON_MESSAGE_SET_ENCRYPTOR:
                this.passwordAttributes = (PasswordAttributes) ((List) am.getAttachement()).get(0);
                this.encryptor = (Encryptor) ((List) am.getAttachement()).get(1);
                if (!nativeCodeDisabled) this.encryptor.enableNativeCodeEngine();
                String parrentMessage = am.getMainMessage();
                this.resetMessage();

                if (parrentMessage != null && parrentMessage.equals("SessionPassword")) // For Session Password
                {
                    sessionPasswordActive = true;
                    sessionPasswordButton.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.key_act), null, null, null);
                } else // Direct Encryption/Decryption
                {
                    try {
                        startEncDec();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (InvalidKeySpecException e) {
                        e.printStackTrace();
                    }
                }
                break;

            case FileEncActivity.FEA_MESSAGE_DIALOG_FILEACTION: {
                Object attachment = am.getAttachement2();
                CryptFileWrapper singleFile = null;
                List<CryptFileWrapper> fileList = null;
                if (attachment instanceof CryptFileWrapper) {
                    singleFile = (CryptFileWrapper) attachment;
                    fileList = new ArrayList<CryptFileWrapper>();
                    fileList.add(singleFile);
                } else if (attachment != null) {
                    fileList = (List) attachment;
                    if (fileList.size() == 1) singleFile = fileList.get(0);
                }

                if (am.getMainMessage().equals("fe_fileactionDialog_renameFile")) {
                    Dialog fileSetNameDialog = ComponentProvider.getFileSetNameDialog(this, singleFile, FEA_MESSAGE_DIALOG_FILEACTION_RENAME_CONFIRM);
                    fileSetNameDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    fileSetNameDialog.show();
                } else if (am.getMainMessage().equals("fe_fileactionDialog_deleteFile")) {
                    String title = "";
                    String replacer = "";

                    if (singleFile != null) {
                        if (singleFile.isFile())
                            title = getResources().getString(R.string.fe_deleteFile_dialogTitle);
                        else if (singleFile.isDirectory())
                            title = getResources().getString(R.string.fe_deleteFolder_dialogTitle);
                        replacer = singleFile.getName();
                    } else {
                        title = getResources().getString(R.string.fe_fileactionDialog_deleteFiles);
                        replacer = fileList.size() + " " + getResources().getString(R.string.fe_fileaction_itemsFragment);
                    }

                    ComponentProvider.getBaseQuestionDialog(this,
                                    title,
                                    getResources().getString(R.string.common_question_delete).replaceAll("<1>", Matcher.quoteReplacement(replacer)),
                                    "",
                                    FileEncActivity.FEA_MESSAGE_DIALOG_FILEACTION_DELETE_CONFIRM,
                                    fileList)
                            .show();
                } else if (am.getMainMessage().equals("fe_fileactionDialog_wipeFile")) {
                    String title = "";
                    String replacer = "";

                    if (singleFile != null) {
                        if (singleFile.isFile())
                            title = getResources().getString(R.string.fe_wipeFile_dialogTitle);
                        else if (singleFile.isDirectory())
                            title = getResources().getString(R.string.fe_wipeFolder_dialogTitle);
                        replacer = singleFile.getName();
                    } else {
                        title = getResources().getString(R.string.fe_fileactionDialog_wipeFiles);
                        replacer = fileList.size() + " " + getResources().getString(R.string.fe_fileaction_itemsFragment);
                    }

                    ComponentProvider.getBaseQuestionDialog(this,
                                    title,
                                    getResources().getString(R.string.common_question_wipe).replaceAll("<1>", Matcher.quoteReplacement(replacer)),
                                    "",
                                    FileEncActivity.FEA_MESSAGE_DIALOG_FILEACTION_WIPE_CONFIRM,
                                    fileList)
                            .show();
                } else if (am.getMainMessage().equals("fe_fileactionDialog_openFile")) {
                    try {
                        MimeTypeMap mime = MimeTypeMap.getSingleton();
                        String ext = Helpers.getFileExtWrapped(singleFile).toLowerCase();
                        String type = mime.getMimeTypeFromExtension(ext);
                        if (type == null) type = "*/*";

                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);

                        Uri contentUri = null;
                        if (android.os.Build.VERSION.SDK_INT <= 23 || singleFile.getMode() == CryptFileWrapper.MODE_DOCUMENT_FILE)
                            contentUri = singleFile.getUri();
                        else
                            contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileProvider", singleFile.getFile());

                        intent.setDataAndType(contentUri, type);

                        if (singleFile.getMode() == CryptFileWrapper.MODE_DOCUMENT_FILE || android.os.Build.VERSION.SDK_INT > 23)
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        startActivity(Intent.createChooser(intent, getResources().getString(R.string.fe_fileactionDialog_openFile)));
                    } catch (Exception e) {
                        e.printStackTrace();
                        ComponentProvider.getImageToast(getResources().getString(R.string.fe_cannotPerformThisAction),
                                ImageToast.TOAST_IMAGE_CANCEL, this).show();
                    }
                } else if (am.getMainMessage().equals("fe_fileactionDialog_sendFile")) {
                    try {
                        final String archiveExt = "zip"; // as archive behavior
                        Intent intent = null;
                        MimeTypeMap mime = MimeTypeMap.getSingleton();
                        if (singleFile != null) {
                            String ext = Helpers.getFileExtWrapped(singleFile).toLowerCase();
                            if (ext.equalsIgnoreCase(CryptFile.ENC_FILE_EXTENSION) || ext.equalsIgnoreCase(PasswordVaultActivity.PWV_EXPORT_EXT))
                                ext = archiveExt;
                            String type = mime.getMimeTypeFromExtension(ext);
                            if (type == null) type = "*/*";

                            intent = new Intent(Intent.ACTION_SEND);

                            Uri contentUri = null;
                            if (android.os.Build.VERSION.SDK_INT <= 23 || singleFile.getMode() == CryptFileWrapper.MODE_DOCUMENT_FILE)
                                contentUri = singleFile.getUri();
                            else
                                contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileProvider", singleFile.getFile());

                            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                            intent.setType(type);
                        } else {
                            String type = mime.getMimeTypeFromExtension(archiveExt);
                            if (type == null) type = "*/*";

                            ArrayList<Uri> uriList = new ArrayList<Uri>(fileList.size());

                            if (android.os.Build.VERSION.SDK_INT <= 23 || (fileList.size() > 0 && fileList.get(0).getMode() == CryptFileWrapper.MODE_DOCUMENT_FILE))
                                for (CryptFileWrapper item : fileList) uriList.add(item.getUri());
                            else
                                for (CryptFileWrapper item : fileList)
                                    uriList.add(FileProvider.getUriForFile(this, getPackageName() + ".fileProvider", item.getFile()));

                            intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList);
                            intent.setType(type);
                        }

                        startActivity(Intent.createChooser(intent, getResources().getString(R.string.fe_fileactionDialog_sendFile)));

                    } catch (Exception e) {
                        ComponentProvider.getImageToast(getResources().getString(R.string.fe_cannotPerformThisAction),
                                ImageToast.TOAST_IMAGE_CANCEL, this).show();
                    }
                } else if (am.getMainMessage().equals("fe_fileactionDialog_selectAll")) {
                    selectAll();
                } else if (am.getMainMessage().equals("fe_fileactionDialog_deselectAll")) {
                    deselectAll();
                } else if (am.getMainMessage().equals("fe_fileactionDialog_createFolder")) {
                    Dialog createFolderDialog = ComponentProvider.getCreateFolderDialog(this, currentDir, FEA_MESSAGE_DIALOG_FILEACTION_CREATE_FOLDER_CONFIRM);
                    createFolderDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    createFolderDialog.show();
                } else if (am.getMainMessage().equals("fes_terminateRunningTasks")) {
                    terminateRunningTasks();
                }
            }
            this.resetMessage();
            break;

            case FileEncActivity.FEA_MESSAGE_DIALOG_FILEACTION_RENAME_CONFIRM: {
                deselectAll();
            }
            this.resetMessage();
            break;

            case FileEncActivity.FEA_MESSAGE_DIALOG_FILEACTION_CREATE_FOLDER_CONFIRM: {
                final Handler ufh = new Handler() {
                    public void handleMessage(Message msg) {
                        if (msg.what == 1) {
                            updateCurrentFilesPhase3();
                            if (waitDialog != null) waitDialog.cancel();
                            fileArrayAdapter.notifyDataSetChanged();
                            setHistoricScrollPosition(currentDir);
                        }
                    }
                };

                updateCurrentFiles(ufh);
            }
            this.resetMessage();
            break;

            case FileEncActivity.FEA_MESSAGE_DIALOG_FILEACTION_DELETE_CONFIRM:
                if (am.getAttachement().equals(new Integer(1))) {
                    final List<CryptFileWrapper> fileList = (List) am.getAttachement2();
                    CryptFileWrapper singleFile = null;
                    if (fileList.size() == 1) singleFile = fileList.get(0);

                    if (singleFile != null && singleFile.isFile()) {
                        if (singleFile.delete()) {
                            final Handler ufh = new Handler() {
                                public void handleMessage(Message msg) {
                                    if (msg.what == 1) {
                                        updateCurrentFilesPhase3();
                                        if (waitDialog != null) waitDialog.cancel();
                                        fileArrayAdapter.notifyDataSetChanged();
                                        ComponentProvider.getImageToast(FileEncActivity.this.getResources().getString(R.string.fe_fileDeleted),
                                                        ImageToast.TOAST_IMAGE_OK, FileEncActivity.this)
                                                .show();
                                    }
                                }
                            };

                            updateCurrentFiles(ufh);
                        } else {
                            ComponentProvider.getImageToast(this.getResources().getString(R.string.fe_fileNotDeleted),
                                            ImageToast.TOAST_IMAGE_CANCEL, this)
                                    .show();
                        }
                    } else {
                        waitDialog = new SimpleWaitDialog(this);
                        waitDialog.setTitle(getResources().getString(R.string.common_deleting_text) + "...");
                        startWipeOrDelete(fileList, false);
                    }
                }
                this.resetMessage();
                break;

            case FileEncActivity.FEA_MESSAGE_DIALOG_FILEACTION_WIPE_CONFIRM:
                if (am.getAttachement().equals(new Integer(1))) {
                    final List<CryptFileWrapper> fileList = (List) am.getAttachement2();
                    startWipeOrDelete(fileList, true);
                }
                this.resetMessage();
                break;

            case FileEncActivity.FEA_MESSAGE_AFTERENCRYPT_REPORT:
                FinalMessageBean fmb = (FinalMessageBean) (am.getAttachement());
                String reportTitle = getResources().getString(R.string.fe_report_title);
                StringBuffer report = new StringBuffer();
                Dialog showMessageDialog = null;

                CryptFileWrapper iFile = fmb.inputFileForFMB;
                CryptFileWrapper oFile = fmb.outputFileForFMB;

                if (!fmb.errorMessage.equals("")) {
                    boolean mayBeKitKatLimit = (fmb.errorMessage.toLowerCase().indexOf("ext") > -1 || fmb.errorMessage.toLowerCase().indexOf("usb") > -1)
                            && fmb.errorMessage.toLowerCase().indexOf("denied") > -1; // KitKat readonly extSD issue
                    showMessageDialog = ComponentProvider.getShowMessageDialog(this,
                            reportTitle,
                            fmb.errorMessage + (mayBeKitKatLimit ? getResources().getString(R.string.fe_kitkatIssue) : ""),
                            ComponentProvider.DRAWABLE_ICON_CANCEL
                    );
                    showMessageDialog.show();
                    this.resetMessage();
                    return;
                }

                if (fmb.mainMessage.equals("encrypting")) {
                    reportTitle = getResources().getString(R.string.fe_report_enc_title);
                    if (iFile != null) {
                        if (iFile.isDirectory())
                            report.append(getResources().getString(R.string.fe_report_inputFolder).replaceAll("<1>", Matcher.quoteReplacement(iFile.getOriginalName())));
                        else
                            report.append(getResources().getString(R.string.fe_report_inputFile).replaceAll("<1>", Matcher.quoteReplacement(iFile.getOriginalName())));
                    } else {
                        report.append(getResources().getString(R.string.fe_report_inputFolder) + "<b>" + fmb.filesFromAllToOneFile + "+" + fmb.foldersFromAllToOneFile + " (" + getResources().getString(R.string.fe_fileactionDialog_universalTitle).replaceAll("/", "+") + ")</b>");
                    }
                    report.append("<br/>");
                    String oFileName = oFile.getName();
                    if (oFileName.endsWith("." + Encryptor.ENC_FILE_UNFINISHED_EXTENSION))
                        oFileName = oFileName.substring(0, oFileName.length() - 4);
                    report.append(getResources().getString(R.string.fe_report_outputFile).replaceAll("<1>", Matcher.quoteReplacement(oFileName)));

                    if (outputParameters.outputDirectoryEncrypted != null)
                        report.append("<br/><br/><small>" + getResources().getString(R.string.fe_report_outputFilePath) + ":<br/>" + outputParameters.outputDirectoryEncrypted.getUniqueIdentifier() + "</small>");
                } else if (fmb.mainMessage.equals("decrypting")) {
                    reportTitle = getResources().getString(R.string.fe_report_dec_title);
                    report.append(getResources().getString(R.string.fe_report_inputFile).replaceAll("<1>", Matcher.quoteReplacement(iFile.getName())));
                    report.append("<br/>");

                    if (oFile.isDirectory())
                        report.append(getResources().getString(R.string.fe_report_outputFolder).replaceAll("<1>", Matcher.quoteReplacement(oFile.getName())));
                    else
                        report.append(getResources().getString(R.string.fe_report_outputFile).replaceAll("<1>", Matcher.quoteReplacement(oFile.getName())));

                    if (outputParameters.outputDirectoryDecrypted != null)
                        report.append("<br/><br/><small>" + getResources().getString(R.string.fe_report_outputFilePath) + ":<br/>" + outputParameters.outputDirectoryDecrypted.getUniqueIdentifier() + "</small>");
                }

                // wiping report
                if (fmb.files > 0 || fmb.folders > 0) {
                    if (fmb.mainMessage.equals("encrypting")) report.append("<br/><br/>");
                    if (currentDir.getMode() == CryptFileWrapper.MODE_FILE)
                        report.append(getResources().getString(R.string.fe_report_wiped) + "<br/>");
                    else
                        report.append(getResources().getString(R.string.fe_report_deleted) + "<br/>");
                    report.append(getResources().getString(R.string.fe_report_wipedFiles)
                            .replaceAll("<1>", "" + fmb.deletedFiles).replaceAll("<2>", "" + fmb.files) + "<br/>");
                    report.append(getResources().getString(R.string.fe_report_wipedFolders)
                            .replaceAll("<1>", "" + fmb.deletedFolders).replaceAll("<2>", "" + fmb.folders));
                } else if (fmb.deletedFolders + fmb.deletedFiles == 0) {
                    StringBuffer deleteYesNo = new StringBuffer();
                    deleteYesNo.append("<br/><br/><small>");
                    deleteYesNo.append(getResources().getString(R.string.fe_sourceFilesSDeleted));
                    deleteYesNo.append(": <0></small>");

                    if (fmb.mainMessage.equals("encrypting")) {
                        report.append(deleteYesNo.toString().replaceAll("<0>", "<b><font color='#FFFF00'>" + getResources().getString(R.string.common_no_text).toUpperCase()
                                + "</font></b> (" + getResources().getString(R.string.common_disabled_text).toLowerCase() + ")"));
                    } else if (fmb.mainMessage.equals("decrypting")) {
                        if (fmb.singleDeleteFailed)
                            report.append(deleteYesNo.toString().replaceAll("<0>", "<b><font color='#FF0000'>" + getResources().getString(R.string.common_no_text).toUpperCase() + "</font></b>"));
                        else if (settingDataHolder.getItemAsBoolean("SC_FileEnc", "SI_DeleteAfterDecryption"))
                            report.append(deleteYesNo.toString().replaceAll("<0>", "<b>" + getResources().getString(R.string.common_yes_text).toUpperCase() + "</b>"));
                        else
                            report.append(deleteYesNo.toString().replaceAll("<0>", "<b>" + getResources().getString(R.string.common_no_text).toUpperCase()
                                    + "</b> (" + getResources().getString(R.string.common_disabled_text).toLowerCase() + ")"));
                    }
                }

                boolean afterDelete = false; // afterDelete not used
                if (afterDelete) {
                    showMessageDialog = ComponentProvider.getShowMessageDialog(this,
                            reportTitle,
                            report.toString(),
                            ComponentProvider.DRAWABLE_ICON_OK,
                            fmb.inputFileForFMB.getUniqueIdentifier(),
                            FEA_MESSAGE_AFTERENCRYPT_DELETE_ASK
                    );
                } else {
                    int iconCode = !fmb.mainMessage.equals("decrypting") && !fmb.mainMessage.equals("encrypting")
                            ? ComponentProvider.DRAWABLE_ICON_INFO_BLUE : ComponentProvider.DRAWABLE_ICON_OK;
                    showMessageDialog = ComponentProvider.getShowMessageDialog(this,
                            reportTitle,
                            report.toString(),
                            iconCode
                    );
                }
                showMessageDialog.show();

                this.resetMessage();
                break;

            case FileEncActivity.FEA_MESSAGE_AFTERENCRYPT_DELETE_ASK: {
                CryptFileWrapper tFile = new CryptFileWrapper(am.getMainMessage(), currentDir.getMode(), context);
                String title = "";
                if (tFile.isFile())
                    title = getResources().getString(R.string.fe_deleteFile_dialogTitle);
                else if (tFile.isDirectory())
                    title = getResources().getString(R.string.fe_deleteFolder_dialogTitle);

                ComponentProvider.getBaseQuestionDialog(this,
                                title,
                                getResources().getString(R.string.common_question_delete).replaceAll("<1>", Matcher.quoteReplacement(tFile.getName())),
                                am.getMainMessage(),
                                FileEncActivity.FEA_MESSAGE_DIALOG_FILEACTION_DELETE_CONFIRM)
                        .show();
            }
            this.resetMessage();
            break;

            case FileEncActivity.FEA_MESSAGE_RENDER_CANCEL_CONFIRM:
                if (am.getAttachement().equals(new Integer(1))) {
                    ((CustomAlertDialog) progressBarToken.getCancelDialog()).setCustomFlag(false);
                    if (renderPhase.getRenderPhase() == 11) // temp files wiping
                    {
                        Toast toast = ComponentProvider.getImageToast(this.getResources().getString(R.string.fe_message_wipe_interrupted_tempfiles),
                                ImageToast.TOAST_IMAGE_INFO, this);
                        toast.setDuration(Toast.LENGTH_LONG);
                        toast.show();
                        return;
                    }

                    if (encDecThread != null) encDecThread.interrupt();
                    //if(wipeThread != null) progressBarToken.setInterrupt(true);
                    progressBarToken.setInterrupt(true);

                    if (renderPhase.getRenderPhase() == 10) // stand alone wiping
                    {
                        Toast toast = ComponentProvider.getImageToast(this.getResources().getString(R.string.fe_message_wipe_interrupted_aftercurrent),
                                ImageToast.TOAST_IMAGE_INFO, this);
                        toast.setDuration(Toast.LENGTH_LONG);
                        toast.show();
                    }
                } else {
                    progressBarToken.getDialog().getWindow().setGravity(Gravity.CENTER);
                }
                this.resetMessage();
                break;

            case FileEncActivity.FEA_MESSAGE_DIALOG_HOMEACTION: {
                if (am.getMainMessage().equals("fe_homeactionDialog_setHome")) {
        			/*
        			ComponentProvider.getBaseQuestionDialog(this,
        					getResources().getString(R.string.fe_favouriteDialog_setHome_confirmTitle),
            				getResources().getString(R.string.fe_favouriteDialog_setHome_confirmQuestion).replaceAll("<1>", currentDir.getUniqueIdentifier()),
            				am.getMainMessage(),
            				FileEncActivity.FEA_MESSAGE_DIALOG_HOMEACTION_HOMESET_CONFIRM,
            				currentDir).show();
            		*/
                    this.setMessage(new ActivityMessage(
                            FileEncActivity.FEA_MESSAGE_DIALOG_HOMEACTION_HOMESET_CONFIRM,
                            am.getMainMessage(),
                            new Integer(1),
                            currentDir));
                } else if (am.getMainMessage().equals("fe_homeactionDialog_goHome")) {
                    if (getHomeDir("FAVOURITES_HOMEDIR") != null)
                        currentDir = getHomeDir("FAVOURITES_HOMEDIR");
                    else if (availableVolumesList.size() > 1 && availableVolumesList.get(1) != null && availableVolumesList.get(1).listFiles() != null)
                        currentDir = availableVolumesList.get(1);
                    else
                        currentDir = Helpers.isAndroid_11P() && primaryMediaDir != null ? new CryptFileWrapper(new CryptFile(primaryMediaDir)) : availableVolumesList.get(0);
                    checkCurrentDirectoryForWarnings();

                    updateCurrentFiles();
                    fileArrayAdapter.notifyDataSetChanged();
                    filesListView.setSelectionAfterHeaderView();
                } else if (am.getMainMessage().equals("fe_homeactionDialog_goToLastSAF")) {
                    if (getHomeDir("FAVOURITES_LASTSAF") != null) {
                        if (handleFileUpdateLock()) return;

                        currentDir = getHomeDir("FAVOURITES_LASTSAF");

                        final Handler ufh = new Handler() {
                            public void handleMessage(Message msg) {
                                if (msg.what == 1) {
                                    updateCurrentFilesPhase3();
                                    if (waitDialog != null) waitDialog.cancel();
                                    fileArrayAdapter.notifyDataSetChanged();
                                    filesListView.setSelectionAfterHeaderView();
                                }
                            }
                        };
                        updateCurrentFiles(ufh);
                    }
                } else if (am.getMainMessage().equals("fe_homeactionDialog_setAsDialog")) {
                    List<String> titleList = new ArrayList<String>();
                    List<Object> tagList = new ArrayList<Object>();

                    titleList.add(getResources().getString(R.string.fe_favouriteDialog_homeDir));
                    tagList.add("fe_homeactionDialog_setHome");

                    if (encFileDestination != FILE_DESTINATION_ASSOURCE) {
                        titleList.add(getResources().getString(R.string.fe_favouriteDialog_encFileDir));
                        tagList.add("fe_homeactionDialog_setEncFilesDir");
                    }

                    if (decFileDestination != FILE_DESTINATION_ASSOURCE) {
                        titleList.add(getResources().getString(R.string.fe_favouriteDialog_decFileDir));
                        tagList.add("fe_homeactionDialog_setDecFilesDir");
                    }

                    SelectionDialog selectDirDialog = new SelectionDialog(
                            this,
                            titleList,
                            null,
                            null,
                            tagList,
                            getResources().getString(R.string.fe_favouriteDialog_setAs));
                    selectDirDialog.setMessageCode(FEA_MESSAGE_DIALOG_HOMEACTION);
                    selectDirDialog.show();

                } else if (am.getMainMessage().equals("fe_homeactionDialog_setEncFilesDir")) {
                    if ((currentDir.canWrite() && currentDir.getWritePermissionLevelForDir() > 1) || Helpers.isWriteExceptionDir(currentDir, this)) {
                        settingDataHolder.addOrReplacePersistentDataObject("FAVOURITES_ENCFILES", currentDir);
                        settingDataHolder.save();

                        ComponentProvider.getImageToast(
                                getResources().getString(R.string.fe_favouriteDialog_encFileDir) + ": <br/><small>" + currentDir.getUniqueIdentifier() + "</small>",
                                ImageToast.TOAST_IMAGE_INFO, this).show();
                    } else {
                        ComponentProvider.getShowMessageDialog(this,
                                getResources().getString(R.string.fe_readOnlyWarningTitle),
                                getResources().getString(R.string.fe_directoryReadOnly),
                                ComponentProvider.DRAWABLE_ICON_CANCEL).show();
                    }

                } else if (am.getMainMessage().equals("fe_homeactionDialog_setDecFilesDir")) {
                    if ((currentDir.canWrite() && currentDir.getWritePermissionLevelForDir() > 1) || Helpers.isWriteExceptionDir(currentDir, this)) {
                        settingDataHolder.addOrReplacePersistentDataObject("FAVOURITES_DECFILES", currentDir);
                        settingDataHolder.save();

                        ComponentProvider.getImageToast(
                                getResources().getString(R.string.fe_favouriteDialog_decFileDir) + ": <br/><small>" + currentDir.getUniqueIdentifier() + "</small>",
                                ImageToast.TOAST_IMAGE_INFO, this).show();
                    } else {
                        ComponentProvider.getShowMessageDialog(this,
                                getResources().getString(R.string.fe_readOnlyWarningTitle),
                                getResources().getString(R.string.fe_directoryReadOnly),
                                ComponentProvider.DRAWABLE_ICON_CANCEL).show();
                    }
                } else if (am.getMainMessage().equals("fe_homeactionDialog_goToEncFilesDir")) {
                    if (getEncFilesDir() != null) {
                        currentDir = getEncFilesDir();
                        checkCurrentDirectoryForWarnings();
                        updateCurrentFiles();
                        fileArrayAdapter.notifyDataSetChanged();
                        filesListView.setSelectionAfterHeaderView();
                    }
                } else if (am.getMainMessage().equals("fe_homeactionDialog_goToDecFilesDir")) {
                    if (getDecFilesDir() != null) {
                        currentDir = getDecFilesDir();
                        checkCurrentDirectoryForWarnings();
                        updateCurrentFiles();
                        fileArrayAdapter.notifyDataSetChanged();
                        filesListView.setSelectionAfterHeaderView();
                    }
                }
            }
            this.resetMessage();
            break;

            case FileEncActivity.FEA_MESSAGE_DIALOG_HOMEACTION_HOMESET_CONFIRM:
                if (am.getAttachement() == null || am.getAttachement().equals(new Integer(1))) {
                    settingDataHolder.addOrReplacePersistentDataObject("FAVOURITES_HOMEDIR", currentDir);
                    settingDataHolder.save();

                    ComponentProvider.getImageToast(
                            getResources().getString(R.string.fe_favouriteDialog_homeDir) + ": <br/><small>" + currentDir.getUniqueIdentifier() + "</small>",
                            ImageToast.TOAST_IMAGE_INFO, this).show();
                }
                this.resetMessage();
                break;

            case FileEncActivity.FEA_MESSAGE_DIALOG_SELECT_DIRLIST: {
                String path = (String) am.getAttachement();
                currentDir = new CryptFileWrapper(path, CryptFileWrapper.MODE_FILE, context);
                showRoot = path.length() == 1 ? true : false;

                checkCurrentDirectoryForWarnings();
                updateCurrentFiles();
                fileArrayAdapter.notifyDataSetChanged();
                filesListView.setSelectionAfterHeaderView();
            }
            this.resetMessage();
            break;

            case FileEncActivity.FEA_MESSAGE_DIALOG_ENCDECCHOICE: {
                boolean allToOneFile = (Boolean) am.getAttachement();
                if (allToOneFile) // encrypt all to one file
                {
                    outputParameters.allToOneFile = true;
                    outputParameters.outputFileName = (String) am.getAttachement2();

                    if (encFileDestination == FILE_DESTINATION_CUSTOM && getEncFilesDir() != null) {
                        outputParameters.outputDirectoryEncrypted = getEncFilesDir();
                        startEncDecPasswordDispatcher();
                    } else if (encFileDestination == FILE_DESTINATION_ASK) {
                        showChooseOutputDirectoryDialog(true);
                    } else if (encFileDestination == FILE_DESTINATION_ASSOURCE && isCurrentDirReadOnly()) {
                        showChooseOutputDirectoryDialog(true);
                    } else startEncDecPasswordDispatcher();
                } else {
                    if (isOnlyEncryptedFilesSelection()) // multi files decryption
                    {
                        if (decFileDestination == FILE_DESTINATION_CUSTOM && getDecFilesDir() != null) {
                            outputParameters.outputDirectoryDecrypted = getDecFilesDir();
                            startEncDecPasswordDispatcher();
                        } else if (decFileDestination == FILE_DESTINATION_ASK) {
                            showChooseOutputDirectoryDialog(false);
                        } else if (decFileDestination == FILE_DESTINATION_ASSOURCE && isCurrentDirReadOnly()) {
                            showChooseOutputDirectoryDialog(false);
                        } else startEncDecPasswordDispatcher();
                    } else if (isOnlyUnencryptedFilesSelection()) // multi files encryption
                    {
                        if (encFileDestination == FILE_DESTINATION_CUSTOM && getEncFilesDir() != null) {
                            outputParameters.outputDirectoryEncrypted = getEncFilesDir();
                            startEncDecPasswordDispatcher();
                        } else if (encFileDestination == FILE_DESTINATION_ASK) {
                            showChooseOutputDirectoryDialog(true);
                        } else if (encFileDestination == FILE_DESTINATION_ASSOURCE && isCurrentDirReadOnly()) {
                            showChooseOutputDirectoryDialog(true);
                        } else startEncDecPasswordDispatcher();
                    } else // multi files encryption/decryption
                    {
                        if (encFileDestination == FILE_DESTINATION_ASK && decFileDestination == FILE_DESTINATION_ASK) {
                            outputParameters.encAndDecNeeded = true;
                            showChooseOutputDirectoryDialog(true);
                        } else if ((encFileDestination == FILE_DESTINATION_ASSOURCE || decFileDestination == FILE_DESTINATION_ASSOURCE) && isCurrentDirReadOnly()) {
                            outputParameters.encAndDecNeeded = true;
                            showChooseOutputDirectoryDialog(true);
                        } else {
                            if (encFileDestination == FILE_DESTINATION_CUSTOM && getEncFilesDir() != null) {
                                outputParameters.outputDirectoryEncrypted = getEncFilesDir();
                            }
                            if (decFileDestination == FILE_DESTINATION_CUSTOM && getDecFilesDir() != null) {
                                outputParameters.outputDirectoryDecrypted = getDecFilesDir();
                            }
                            if (encFileDestination == FILE_DESTINATION_ASK) {
                                showChooseOutputDirectoryDialog(true);
                            } else if (decFileDestination == FILE_DESTINATION_ASK) {
                                showChooseOutputDirectoryDialog(false);
                            } else startEncDecPasswordDispatcher();
                        }
                    }
                }
            }
            this.resetMessage();
            break;

            case FileEncActivity.FEA_MESSAGE_OUTPUTDIR_SELECTION: {
                boolean encryption = (Boolean) am.getAttachement2();

                if (am.getMainMessage().equals("fe_outputDir_asSource")) {
                    // do nothing
                } else if (am.getMainMessage().equals("fe_outputDir_encFilesDir")) {
                    outputParameters.outputDirectoryEncrypted = getEncFilesDir();
                } else if (am.getMainMessage().equals("fe_outputDir_decFilesDir")) {
                    outputParameters.outputDirectoryDecrypted = getDecFilesDir();
                } else if (am.getMainMessage().equals("fe_outputDir_selectDir")) {
                    Intent intent = new Intent(context, FilePickerActivity.class);
                    intent.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
                    intent.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);
                    intent.putExtra(FilePickerActivity.EXTRA_START_PATH, currentDir.getAbsolutePath());

                    startActivityForResult(intent, encryption ? REQUEST_CODE_DIRCHOOSER_ENCRYPTION : REQUEST_CODE_DIRCHOOSER_DECRYPTION);
                    this.resetMessage();
                    return;
                } else if (am.getMainMessage().equals("fe_outputDir_selectDirSAF")) {
                    showSAFOutputDirPicker(encryption);
                    this.resetMessage();
                    return;
                } else if (am.getMainMessage().equals("fe_outputDir_custom")) {
                    CryptFileWrapper customDir = new CryptFileWrapper(new CryptFile((File) am.getAttachement()));

                    if (!customDir.canWrite() || customDir.getWritePermissionLevelForDir() < 2) {
                        ComponentProvider.getShowMessageDialog(this,
                                getResources().getString(R.string.fe_readOnlyWarningTitle),
                                getResources().getString(R.string.fe_directoryReadOnly),
                                ComponentProvider.DRAWABLE_ICON_CANCEL).show();
                        return;
                    } else {
                        if (encryption) outputParameters.outputDirectoryEncrypted = customDir;
                        else outputParameters.outputDirectoryDecrypted = customDir;
                    }
                }

                if (encryption && outputParameters.encAndDecNeeded)
                    showChooseOutputDirectoryDialog(false);
                else startEncDecPasswordDispatcher();
            }
            this.resetMessage();
            break;

            case FileEncActivity.FEA_MESSAGE_OUTPUTDIR_FILEREPLACE_CONFIRM: {
                if (am.getAttachement().equals(new Integer(1))) {
                    outputParameters.replaceEncFilesChecked = true;
                    startEncDecPasswordDispatcher();
                }
            }
            this.resetMessage();
            break;

            case FileEncActivity.FEA_MESSAGE_DIALOG_SAF_FIRSTRUN: {
                Intent intent;

                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
                    intent = sm.getPrimaryStorageVolume().createOpenDocumentTreeIntent();
                } else {
                    intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                }

                if (Helpers.isAndroid_11P()) {
                    String startDir = Uri.encode("Android/data");
                    Uri uri = intent.getParcelableExtra("android.provider.extra.INITIAL_URI");
                    String scheme = uri.toString();
                    scheme = scheme.replace("/root/", "/document/");
                    scheme += "%3A" + startDir;
                    uri = Uri.parse(scheme);
                    intent.putExtra("android.provider.extra.INITIAL_URI", uri);
                }

                startActivityForResult(intent, REQUEST_CODE_SAF);
            }
            this.resetMessage();
            break;

            case FileEncActivity.FEA_MESSAGE_READONLY_DIALOG_AFTER: {
                final Button blinkingButton = Helpers.isAndroid_11P() ? selectDirButton : safButton;
                Helpers.blinkView(blinkingButton, 3);
            }
            this.resetMessage();
            break;

            case COMMON_MESSAGE_CONFIRM_EXIT:
                if (am.getAttachement() == null || am.getAttachement().equals(new Integer(1))) {
                    String mm = am.getMainMessage();
                    if (mm != null) {
                        setResult(Integer.parseInt(mm));
                    }
                    finish();
                }
                break;

            case EXIT_CASCADE: {
                setResult(EXIT_CASCADE);
                finish();
            }
            break;

            default:
                break;
        }
    }

    /**
     * Start Encryption/Decryption  dispatcher
     */
    public void startEncDecDispatcher() {
        outputParameters = new OutputParametersBean();
        if (!verifyMultiSelectionShowDialog()) return;

        if (selectedItemsMap.size() > 1) {
            EncDecChoiceDialog encDecChoiceDialog = new EncDecChoiceDialog(this, selectedItemsMap);
            if (encFileDestination == FILE_DESTINATION_CUSTOM && getEncFilesDir() != null)
                encDecChoiceDialog.setCustumEncFileDestination(getEncFilesDir());
            else if (encFileDestination == FILE_DESTINATION_ASSOURCE && isCurrentDirReadOnly())
                encDecChoiceDialog.setSuppressRewriteWarning(true);
            if (encFileDestination == FILE_DESTINATION_ASK)
                encDecChoiceDialog.setSuppressRewriteWarning(true);

            encDecChoiceDialog.show();
        } else {
            startEncDecSingleFileDispatcher();
        }
    }

    /**
     * Encryption/Decryption Single File Selected Dispatcher
     **/
    public void startEncDecSingleFileDispatcher() {
        CryptFileWrapper singlefile = selectedItemsMap.get(selectedItemsMap.firstKey());

        if (singlefile.isEncrypted()) // single file decryption
        {
            if (decFileDestination == FILE_DESTINATION_CUSTOM && getDecFilesDir() != null) {
                outputParameters.outputDirectoryDecrypted = getDecFilesDir();
            } else if (decFileDestination == FILE_DESTINATION_ASK) {
                showChooseOutputDirectoryDialog(false);
                return;
            } else if (isCurrentDirReadOnly()) {
                showChooseOutputDirectoryDialog(false);
                return;
            }
        } else // single file/dir encryption
        {
            if (encFileDestination == FILE_DESTINATION_CUSTOM && getEncFilesDir() != null) {
                outputParameters.outputDirectoryEncrypted = getEncFilesDir();
            } else if (encFileDestination == FILE_DESTINATION_ASK) {
                showChooseOutputDirectoryDialog(true);
                return;
            } else if (isCurrentDirReadOnly()) {
                showChooseOutputDirectoryDialog(true);
                return;
            }
        }
        startEncDecPasswordDispatcher();
    }

    /**
     * Encryption/Decryption Password Dialog Dispatcher
     **/
    public void startEncDecPasswordDispatcher() {
        final StringBuffer existingEncFilesTemp = new StringBuffer();

        if (replaceEncWarning && !outputParameters.replaceEncFilesChecked) {
            CryptFileWrapper outputDir = outputParameters.outputDirectoryEncrypted;
            if (outputDir == null) outputDir = currentDir;

            if (outputDir.getMode() == CryptFileWrapper.MODE_DOCUMENT_FILE) {
                if (handleFileUpdateLock()) return;

                waitDialog = new SimpleWaitDialog(this);
                waitDialog.show();

                final CryptFileWrapper tempOutputDir = outputDir;
                final Handler rwh = new Handler() {
                    public void handleMessage(Message msg) {
                        if (msg.what == 1) {
                            if (waitDialog != null) waitDialog.cancel();
                            fileUpdateLock = false;

                            if (existingEncFilesTemp.length() == 0)
                                startEncDecPasswordDispatcher(null);
                            else
                                startEncDecPasswordDispatcher(existingEncFilesTemp.toString());
                        }
                    }
                };

                Thread rwt = new Thread(new Runnable() {
                    public void run() {
                        PowerManager.WakeLock wakeLock;
                        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sse:checkReplaceEncFile");

                        wakeLock.acquire();
                        String existingEncFiles = getReplaceEncFilesWarning(tempOutputDir);
                        if (existingEncFiles != null) existingEncFilesTemp.append(existingEncFiles);
                        wakeLock.release();
                        rwh.sendMessage(Message.obtain(rwh, 1));
                    }
                });
                rwt.start();
            } else {
                String existingEncFiles = getReplaceEncFilesWarning(outputDir);
                startEncDecPasswordDispatcher(existingEncFiles);
            }
        } else startEncDecPasswordDispatcher(null);
    }

    /**
     * Encryption/Decryption Password Dialog Dispatcher
     **/
    public void startEncDecPasswordDispatcher(String existingEncFiles) {
        if (existingEncFiles != null) {
            ComponentProvider.getBaseQuestionDialog(this,
                            getResources().getString(R.string.common_replacefile_text),
                            getResources().getString(R.string.fe_encFilesAlreadyExistsWarning) + "<br/>" + existingEncFiles,
                            "",
                            FileEncActivity.FEA_MESSAGE_OUTPUTDIR_FILEREPLACE_CONFIRM,
                            null,
                            true)
                    .show();
        } else {
            if (checkOutputNotSubdirOfInput()) {
                if (!sessionPasswordActive) showPasswordDialog();
                else {
                    try {
                        prepareProgressBarToken();
                        startEncDec();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (InvalidKeySpecException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Encryption/Decryption implementation
     */
    public void startEncDec() throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (pbLock) return;
        if (FileEncryptionService.isRunning()) return;
        if (selectedItemsMap.size() < 1) return;
        pbLock = true;

        currentFilesTemp.clear();

        Map<String, Object> intentMap = new HashMap<String, Object>();
        intentMap.put("VAR_Enryptor", encryptor);
        intentMap.put("VAR_ProgressBarToken", progressBarToken);
        intentMap.put("VAR_UniversalHandler", universalHandler);
        intentMap.put("VAR_SelectedItems", new ArrayList<CryptFileWrapper>(selectedItemsMap.values()));
        intentMap.put("VAR_CurrentDir", currentDir);
        intentMap.put("VAR_CurrentFilesTemp", currentFilesTemp);
        intentMap.put("VAR_Compress", compress);
        intentMap.put("VAR_ShowRoot", showRoot);
        intentMap.put("VAR_SettingDataHolder", settingDataHolder);

        if (!sessionPasswordActive) encryptor = null;
        intentMap.put("FLAG_WipeEncryptorAfterDone", !sessionPasswordActive);

        byte[] verificationToken = Helpers.concat("FES_".getBytes(), Encryptor.getRandomBA(64));

        Intent startService = new Intent(context, FileEncryptionService.class);
        startService.putExtra(FileEncryptionService.PURPOSE_KEY, FileEncryptionService.PURPOSE_ENCDEC);
        startService.putExtra(FileEncryptionService.VERIFICATION_TOKEN, verificationToken);
        CryptActivity.setTemporaryObject(intentMap, verificationToken);
        if (Build.VERSION.SDK_INT >= 26)
            context.startForegroundService(startService);
        else
            context.startService(startService);
    }

    /**
     * Start Wiping or Deleting
     */
    public void startWipeOrDelete(List<CryptFileWrapper> filesToWipe, boolean wipe) {
        if (pbLock) return;
        if (FileEncryptionService.isRunning()) return;
        pbLock = true;
        if (dirSizeThread != null) dirSizeThread.interrupt();

        progressBarToken = new ProgressBarToken(renderPhase);
        initProgressBar();
        progressBarToken.setProgressHandler(progressHandler);
        if (wipe) {
            progressBarToken.getDialog().show();
        } else {
            if (waitDialog != null) waitDialog.show();
        }

        currentFilesTemp.clear();

        Map<String, Object> intentMap = new HashMap<String, Object>();
        intentMap.put("VAR_ProgressBarToken", progressBarToken);
        intentMap.put("VAR_UniversalHandler", universalHandler);
        intentMap.put("VAR_SelectedItems", filesToWipe);
        intentMap.put("VAR_CurrentDir", currentDir);
        intentMap.put("VAR_CurrentFilesTemp", currentFilesTemp);
        intentMap.put("VAR_ShowRoot", showRoot);
        intentMap.put("VAR_SettingDataHolder", settingDataHolder);

        int purpose = wipe ? FileEncryptionService.PURPOSE_WIPE : FileEncryptionService.PURPOSE_DELETE;

        byte[] verificationToken = Helpers.concat("FES_".getBytes(), Encryptor.getRandomBA(64));

        Intent startService = new Intent(context, FileEncryptionService.class);
        startService.putExtra(FileEncryptionService.PURPOSE_KEY, purpose);
        startService.putExtra(FileEncryptionService.VERIFICATION_TOKEN, verificationToken);
        CryptActivity.setTemporaryObject(intentMap, verificationToken);
        if (Build.VERSION.SDK_INT >= 26)
            context.startForegroundService(startService);
        else
            context.startService(startService);
    }

    /**
     * Initialize ENC/DEC ProgressBar
     */
    private void initProgressBar() {
        final Dialog cancelDialog = ComponentProvider.getBaseQuestionDialog(
                this,
                getResources().getString(R.string.fe_question_cancel_title),
                getResources().getString(R.string.fe_question_cancel_question)
                        .replaceAll("<1>", ""), "X", FileEncActivity.FEA_MESSAGE_RENDER_CANCEL_CONFIRM);

        final DualProgressDialog pd = new DualProgressDialog(this);
        pd.setCancelable(false);
        if (selectedItemsMap.size() > 1 && !progressBarToken.getEncryptAllToOneFile()) {
            pd.setFullScreen(true);
            pd.hideProgressBarB(false);
            pd.hideVerboseView(false);
        }
        pd.setMessage("");
        pd.setProgress(0);
        pd.setMax(100);

        progressBarToken.setDialog(pd);
        progressBarToken.setCancelDialog(cancelDialog);
        progressBarToken.setIncrement(1);

        pd.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (!pd.isFinished()) {
                        pd.getWindow().setGravity(Gravity.TOP);
                        cancelDialog.getWindow().setGravity(Gravity.BOTTOM);
                        cancelDialog.show();
                        return true;
                    }
                }
                return true;
            }
        });
    }


    /**
     * Update File List and other "current files related" variables
     */
    private void updateCurrentFiles() {
        updateCurrentFiles(null);
    }

    /**
     * Update File List and other "current files related" variables
     */
    private void updateCurrentFiles(final Handler phase3Handler) {
        timeCheck = System.currentTimeMillis();

        checkReadOnlyDirAlert();

        updateCurrentFilesPhase1();

        if (phase3Handler != null && currentDir.getMode() == CryptFileWrapper.MODE_DOCUMENT_FILE) {
            waitDialog = new SimpleWaitDialog(this);
            waitDialog.show();

            Thread updateFiles = new Thread(new Runnable() {
                public void run() {
                    PowerManager.WakeLock wakeLock;
                    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "sse:updateFilesLock");

                    wakeLock.acquire();
                    FileEncryptionService.updateCurrentFilesPhase2(currentDir, currentFilesTemp, showRoot);
                    wakeLock.release();
                    phase3Handler.sendMessage(Message.obtain(phase3Handler, 1));
                }
            });
            updateFiles.start();
        } else {
            FileEncryptionService.updateCurrentFilesPhase2(currentDir, currentFilesTemp, showRoot);
            if (phase3Handler == null) updateCurrentFilesPhase3();
            else phase3Handler.sendMessage(Message.obtain(phase3Handler, 1));
        }
    }

    private void updateCurrentFilesPhase1() {
        if (!multiSelection && isCurrentDirReadOnly() && !FileEncryptionService.isRunning())
            moreButton.setEnabled(false);
        else moreButton.setEnabled(true);
        startEncDecButton.setEnabled(false);
        startEncDecButton.setText(getResources().getString(R.string.fe_goButton));
        if (fileArrayAdapter != null) ((FileEncArrayAdapter) fileArrayAdapter).clearDirSizeMap();
        if (dirSizeThread != null) dirSizeThread.interrupt();
    }

    private void updateCurrentFilesPhase3() {
        currentFiles.clear();
        selectedItemsMap.clear();

        currentFiles.addAll(currentFilesTemp);

        if (currentDir.getMode() == CryptFileWrapper.MODE_DOCUMENT_FILE) {
            topTextView.setBackgroundResource(R.drawable.d_textview_d);
            bottomTextView.setBackgroundResource(R.drawable.d_textview_d);
        } else {
            topTextView.setBackgroundResource(R.drawable.d_textview_a);
            bottomTextView.setBackgroundResource(R.drawable.d_textview_a);
        }

        topTextView.setText(getResources().getString(R.string.fe_currentDir_text) + " " + currentDir.getUniqueIdentifier());

        bottomTextView.setEllipsize(TruncateAt.END);
        if (currentFiles != null && currentFiles.size() > 0 && currentFiles.get(0) != null && currentFiles.get(0).isBackDir()) // is Top Dir then first tip?
            bottomTextView.setText(getTip(1)); // 0 for random
        else bottomTextView.setText(getTip(1));

        fileUpdateLock = false;

        // TODO
        //showInforDialog("" + (System.currentTimeMillis() - timeCheck));


        // Volume Size
        if (currentDir.getMode() == CryptFileWrapper.MODE_FILE) // File Mode Only
        {
            final String absulutePath = currentDir.getAbsolutePath();
            if (!Helpers.getFirstDirFromFilepathWithLFS(absulutePath).equals((String) getTitleRightTag()))
                setTitleRight("");
            if (volumeSizeThread != null) return;
            volumeSizeThread = new Thread(new Runnable() {
                public void run() {
                    String titleRight = "";
                    String titleRightTag = Helpers.getFirstDirFromFilepathWithLFS(absulutePath);
                    List message = new ArrayList();
                    try {
                        StatFs stat = new StatFs(absulutePath);
                        long blockSize = (long) stat.getBlockSize();
                        long sdAvailSize = (long) stat.getAvailableBlocks() * blockSize;
                        long sdSize = (long) stat.getBlockCount() * blockSize;
                        if (sdSize < 1) throw new Exception();
                        titleRight = (Helpers.getFormatedFileSize(sdAvailSize) + File.separator + Helpers.getFormatedFileSize(sdSize));
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }
                    message.add(titleRight);
                    message.add(titleRightTag);
                    universalHandler.sendMessage(Message.obtain(universalHandler, FEA_UNIVERSALHANDLER_SHOW_VOLUMESIZE, message));
                }
            });
            volumeSizeThread.setPriority(Thread.MIN_PRIORITY);
            volumeSizeThread.start();
        } else setTitleRight("");
    }

    /**
     * Display SAF help when current dir is read-only
     */
    private void checkReadOnlyDirAlert() {
        if (android.os.Build.VERSION.SDK_INT >= 21 && currentDir != null && currentDir.getMode() == CryptFileWrapper.MODE_FILE && !readOnlyDirAlertDisplayed && isCurrentDirReadOnly() && currentDir.countFilesInDir() > 0) {
            readOnlyDirAlertDisplayed = true;
            try {
                HtmlAlertDialog safHelpDialog = new HtmlAlertDialog(this, getResources().getString(R.string.alerts_dir) +
                        "Alert_SAF_Ad.html", getResources().getString(R.string.fe_readOnlyWarningTitle));
                safHelpDialog.setMessageCode(FEA_MESSAGE_READONLY_DIALOG_AFTER);
                safHelpDialog.addValue("API_VERSION", Integer.toString(android.os.Build.VERSION.SDK_INT));
                safHelpDialog.addValue("FILE_DESTINATIONS", encFileDestination + "" + decFileDestination);
                safHelpDialog.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Restore "historic" ListView position using scrollPositionMap
     */
    private void setHistoricScrollPosition(CryptFileWrapper file) {
        if (file.isBackDir()) {
            Integer index = scrollPositionMap.get(file.getUniqueIdentifier());
            if (index != null) filesListView.setSelectionFromTop(index, 0);
            else filesListView.setSelectionAfterHeaderView();
        } else filesListView.setSelectionAfterHeaderView();
    }

    /**
     * Get File position in parent directory
     */
    private int getFileIndex(CryptFileWrapper file) {
        int fileIndex = -1;
        for (int i = 0; i < currentFiles.size(); ++i) {
            if (currentFiles.get(i).getAbsolutePath().equals(file.getAbsolutePath())) {
                fileIndex = i;
                break;
            }
        }
        return fileIndex;
    }

    /**
     * Set this file as the Selected One (or Opposite if selected and Multiselection enabled)
     */
    private void setSelectedItem(CryptFileWrapper file) {
        if (multiSelection) {
            file.setSelected(!file.isSelected());
            if (file.isSelected()) {
                selectedItemsMap.put(file.getName(), file);
            } else {
                selectedItemsMap.remove(file.getName());
            }

            if (selectedItemsMap.size() > 1) {
                bottomTextView.setText(getResources().getString(R.string.common_selectedXItems).replaceAll("<1>", Integer.toString(selectedItemsMap.size())));
            } else if (selectedItemsMap.size() == 1) {
                bottomTextView.setEllipsize(TruncateAt.START);
                bottomTextView.setText(getResources().getString(R.string.fe_selected_text) + " " + selectedItemsMap.firstKey());
            } else {
                bottomTextView.setEllipsize(TruncateAt.END);
                bottomTextView.setText(getTip(1));
                if (!multiSelection && isCurrentDirReadOnly() && !FileEncryptionService.isRunning())
                    moreButton.setEnabled(false);
                startEncDecButton.setEnabled(false);
                startEncDecButton.setText(getResources().getString(R.string.fe_goButton));
            }
        } else {
            if (selectedItemsMap.size() > 1)
                throw new IllegalStateException("SelectedItemsMap.Size > 1 !!!");
            if (selectedItemsMap.size() > 0)
                selectedItemsMap.get(selectedItemsMap.firstKey()).setSelected(false);
            selectedItemsMap.clear();
            file.setSelected(true);
            selectedItemsMap.put(file.getName(), file);

            bottomTextView.setEllipsize(TruncateAt.START);
            bottomTextView.setText(getResources().getString(R.string.fe_selected_text) + " " + selectedItemsMap.firstKey());
        }
    }

    /**
     * ShortCut for progressBarToken...sendMessage...
     */
    public void sendPBTMessage(int message, Object attachement) {
        progressBarToken.getProgressHandler().sendMessage(Message.obtain(progressBarToken.getProgressHandler(),
                message,
                attachement
        ));
    }

    /**
     * Get Tip Text (fe_tip_X) - code == 0 for random
     */
    private String getTip(int tipCode) {
        if (tips.size() == 0) {
            int tipCounter = 0;
            while (true) {
                ++tipCounter;
                String resourceName = "fe_tip_" + tipCounter;
                String tempTip = getStringResource(resourceName);
                if (!tempTip.equals(resourceName)) tips.add(tempTip);
                else break;
            }
        }

        String tip = "NULL";
        int tipIndex;

        if (tipCode > 0) tipIndex = tipCode - 1;
        else {
            Random rand = new Random(System.currentTimeMillis());
            tipIndex = rand.nextInt(tips.size());
        }
        try {
            tip = tips.get(tipIndex);
        } catch (Exception e) {
        }

        return tip;
    }

    /**
     * Lock Screen
     */
    private void showScreenLockDialog() {
        if (predefinedScreenLockKey == null)
            sld = new ScreenLockDialog(this, encryptor.getKeyHash());
        else
            sld = new ScreenLockDialog(this, predefinedScreenLockKey);
        if (pbLock) sld.leaveButtonEnabled(false);
        sld.show();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (sld != null && sld.getActiveFlag()) return;

        if (lockOnPause > -1 && (sessionPasswordActive || predefinedScreenLockKey != null)) {
            setScreenLockTime(lockOnPause);
            doOnLock();
            if (!Helpers.isScreenOn(this)) showScreenLockDialog();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sldVeto) {
            sldVeto = false;
            return;
        }

        if (checkScreenAutoUnlock()) {
            doOnUnlock();
            return;
        }

        if (sld != null && sld.getActiveFlag()) return;

        if (lockOnPause > -1 && (sessionPasswordActive || predefinedScreenLockKey != null)) {
            doOnLock();
            showScreenLockDialog();
        }
    }

    //+ Lockable
    public void doOnLock() {
        rootLayout.setVisibility(ViewGroup.GONE);
    }

    public void doOnUnlock() {
        rootLayout.setVisibility(ViewGroup.VISIBLE);
        try {
            if (sld != null) sld.cancel();
            sld = null;
        } catch (Exception e) {
        }
        ;
    }
//- Lockable

    /**
     * Back Button - if current dir has a parent show the parent - else go to the main menu
     */
    @Override
    public void onBackPressed() {
        CryptFileWrapper cf = null;
        if (currentFiles != null && currentFiles.size() > 0) cf = currentFiles.get(0);
        if (cf != null && cf.isBackDir()) {
            if (handleFileUpdateLock()) return;

            currentDir = cf;

            final Handler ufh = new Handler() {
                public void handleMessage(Message msg) {
                    if (msg.what == 1) {
                        updateCurrentFilesPhase3();
                        if (waitDialog != null) waitDialog.cancel();
                        fileArrayAdapter.notifyDataSetChanged();
                        setHistoricScrollPosition(currentDir);
                    }
                }
            };

            updateCurrentFiles(ufh);
        } else {
            if (askOnLeave && !startFromFileSystem) {
                showExitDialog();
            } else setMessage(new ActivityMessage(COMMON_MESSAGE_CONFIRM_EXIT, null));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        if (dirSizeThread != null) dirSizeThread.interrupt();
        try {
            Dialog tempDialog = null;
            if (progressBarToken != null) progressBarToken.getDialog();
            if (tempDialog != null && tempDialog.isShowing()) {
                tempDialog.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (FileEncryptionService.isRunning()) {
            FileEncryptionService.setWipeEncryptorAfterDone();
        } else if (sessionPasswordActive && encryptor != null && !pbLock) {
            wipeEncryptor();
        }

        super.onDestroy();
    }

    /**
     * Handle Menu Button, ignore Search Button, else as default
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            showFileActionDialog();
            return true;
        } else return super.onKeyDown(keyCode, event);
    }

    @SuppressLint("NewApi")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == REQUEST_CODE_SAF) {
            if (resultCode == RESULT_OK) {
                if (handleFileUpdateLock()) return;

                Uri treeUri = resultData.getData();
                DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
                try {
                    getContentResolver().takePersistableUriPermission(
                            treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                currentDir = new CryptFileWrapper(pickedDir, context);

                settingDataHolder.addOrReplacePersistentDataObject("FAVOURITES_LASTSAF", currentDir);
                settingDataHolder.save();

                final Handler ufh = new Handler() {
                    public void handleMessage(Message msg) {
                        if (msg.what == 1) {
                            updateCurrentFilesPhase3();
                            if (waitDialog != null) waitDialog.cancel();
                            fileArrayAdapter.notifyDataSetChanged();
                            filesListView.setSelectionAfterHeaderView();
                            sldVeto = true;
                        }
                    }
                };
                updateCurrentFiles(ufh);
            } else sldVeto = true;
        } else if (requestCode == REQUEST_CODE_SAF_OUTPUT_ONLY_ENCRYPTION || requestCode == REQUEST_CODE_SAF_OUTPUT_ONLY_DECRYPTION) {
            if (resultCode == RESULT_OK) {
                Uri treeUri = resultData.getData();
                DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
                try {
                    getContentResolver().takePersistableUriPermission(
                            treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                CryptFileWrapper pdw = new CryptFileWrapper(pickedDir, context);

                if (requestCode == REQUEST_CODE_SAF_OUTPUT_ONLY_ENCRYPTION)
                    outputParameters.outputDirectoryEncrypted = pdw;
                else
                    outputParameters.outputDirectoryDecrypted = pdw;

                if (requestCode == REQUEST_CODE_SAF_OUTPUT_ONLY_ENCRYPTION && outputParameters.encAndDecNeeded)
                    showChooseOutputDirectoryDialog(false);
                else startEncDecPasswordDispatcher();
            }
        } else if (requestCode == REQUEST_CODE_DIRCHOOSER_ENCRYPTION || requestCode == REQUEST_CODE_DIRCHOOSER_DECRYPTION) {
            if (resultCode == RESULT_OK || resultCode == RESULT_CANCELED) sldVeto = true;
            if (resultCode == RESULT_OK) {
                File selectedDir = new File(resultData.getData().getPath());
                if (selectedDir == null || !selectedDir.exists()) {
                    ComponentProvider.getImageToast(
                            getResources().getString(R.string.fe_directoryCannotBeSelected),
                            ComponentProvider.DRAWABLE_ICON_CANCEL,
                            this).show();
                    return;
                }

                this.setMessage(new ActivityMessage(
                        FileEncActivity.FEA_MESSAGE_OUTPUTDIR_SELECTION,
                        "fe_outputDir_custom",
                        selectedDir,
                        requestCode == REQUEST_CODE_DIRCHOOSER_ENCRYPTION));
            }
        }
    }

    public static List<CryptFileWrapper> getExternalFilesFromIntent(Intent intent, Context context) {
        List<CryptFileWrapper> externalFiles = null;
        List<android.net.Uri> dataList = null;
        android.net.Uri data = intent.getData();
        if (data == null) {
            Object rawData = null;
            Bundle bundle = intent.getExtras();
            if (bundle != null) rawData = bundle.get(Intent.EXTRA_STREAM);
            if (rawData instanceof android.net.Uri)
                data = (android.net.Uri) rawData;
            else if (rawData instanceof List)
                dataList = (List) rawData;
        }
        if (data != null) {
            dataList = new ArrayList<android.net.Uri>();
            dataList.add(data);
        }

        if (dataList != null) {
            externalFiles = new ArrayList<CryptFileWrapper>();

            String directoryPath = null;
            for (int i = 0; i < dataList.size(); ++i) {
                CryptFileWrapper tempFile = null;
                try {
                    if (!dataList.get(i).toString().startsWith("content:"))
                        tempFile = new CryptFileWrapper(dataList.get(i).getPath(), CryptFileWrapper.MODE_FILE, context);
                    else
                        tempFile = new CryptFileWrapper(Helpers.getRealPathFromUriExtended(context, dataList.get(i)), CryptFileWrapper.MODE_FILE, context);

                    if (i == 0)
                        directoryPath = tempFile.getParentFile().getAbsolutePath();
                    else if (!directoryPath.equals(tempFile.getParentFile().getAbsolutePath())) {
                        ImageToast toast = new ImageToast(context.getResources().getString(R.string.fe_filesNotFromOneDirectory), ImageToast.TOAST_IMAGE_INFO_RED, (Activity) context);
                        toast.setDuration(Toast.LENGTH_LONG);
                        toast.show();

                        externalFiles = null;
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (tempFile != null && tempFile.exists() && tempFile.isFile()) {
                    externalFiles.add(tempFile);
                } else if (dataList.get(i).toString().contains("com.dropbox.android.FileCache")) {
                    List<File> paths = Helpers.getExtDirectories(context, true);
                    CryptFile cfile = null;
                    if (paths != null && paths.size() > 1) {
                        String dropPath = null;
                        try {
                            dropPath = paths.get(1).getCanonicalPath() + "/Android/data/com.dropbox.android/files/";
                            cfile = new CryptFile(dropPath);
                            if (cfile.exists())
                                dropPath = cfile.listFiles()[0].getCanonicalPath() + "/scratch/";
                            cfile = new CryptFile(dropPath);
                        } catch (Exception e) {
                            //N/A
                        }

                        if (cfile != null && cfile.exists())
                            externalFiles.add(new CryptFileWrapper(cfile));
                        else {
                            ImageToast toast = new ImageToast(context.getResources().getString(R.string.common_incorrectFile_text), ImageToast.TOAST_IMAGE_INFO_RED, (Activity) context);
                            toast.setDuration(Toast.LENGTH_LONG);
                            toast.show();

                            externalFiles = null;
                            break;
                        }
                    }
                } else {
                    ImageToast toast = new ImageToast(context.getResources().getString(R.string.common_incorrectFile_text), ImageToast.TOAST_IMAGE_INFO_RED, (Activity) context);
                    toast.setDuration(Toast.LENGTH_LONG);
                    toast.show();

                    externalFiles = null;
                    break;
                }
            }

            if (externalFiles == null || externalFiles.size() < 1) externalFiles = null;
        }

        return externalFiles;
    }

    // Show Password dialog
    private void showPasswordDialog() {
        if (isButtonsLockActivated()) return;
        activateButtonsLock();

        if (dirSizeThread != null) dirSizeThread.interrupt();

        CryptFileWrapper singlefile = null;
        if (selectedItemsMap.size() < 2)
            singlefile = selectedItemsMap.get(selectedItemsMap.firstKey());
        progressBarToken = new ProgressBarToken(renderPhase);
        progressBarToken.setEncryptAllToOneFile(outputParameters.allToOneFile);
        progressBarToken.setCustomFileName(outputParameters.outputFileName);
        progressBarToken.setCustomOutputDirectoryEncrypted(outputParameters.outputDirectoryEncrypted);
        progressBarToken.setCustomOutputDirectoryDecrypted(outputParameters.outputDirectoryDecrypted);

        if (singlefile != null) {
            if (singlefile.isEncrypted())
                passwordDialog = new PasswordDialog(this, PasswordDialog.PD_MODE_ENTER_PASSWORD, Encryptor.PURPOSE_FILE_ENCRYPTION);
            else
                passwordDialog = new PasswordDialog(this, PasswordDialog.PD_MODE_SET_PASSWORD, Encryptor.PURPOSE_FILE_ENCRYPTION);
        } else {
            if (outputParameters.allToOneFile)
                passwordDialog = new PasswordDialog(this, PasswordDialog.PD_MODE_SET_PASSWORD, Encryptor.PURPOSE_FILE_ENCRYPTION);
            else {
                int[] encdec = Helpers.getNumberOfEncAndUnenc(selectedItemsMap);
                if (encdec[1] > 0)
                    passwordDialog = new PasswordDialog(this, PasswordDialog.PD_MODE_SET_PASSWORD, Encryptor.PURPOSE_FILE_ENCRYPTION);
                else
                    passwordDialog = new PasswordDialog(this, PasswordDialog.PD_MODE_ENTER_PASSWORD, Encryptor.PURPOSE_FILE_ENCRYPTION);
            }
        }
        initProgressBar();
        progressBarToken.setProgressHandler(progressHandler);
        passwordDialog.setEncryptAlgorithmCode(encryptAlgorithmCode);
        passwordDialog.setWaitDialog(progressBarToken, false);

        passwordDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                deactivateButtonsLock();
            }
        });

        if (passwordDialog.getDialogMode() == PasswordDialog.PD_MODE_ENTER_PASSWORD)
            passwordDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        passwordDialog.show();

    }

    // Show Password dialog (Set Password Mode - for Session Password)
    private void showPasswordDialogSetMode(char[] predefinedPassword) {
        if (isButtonsLockActivated()) return;
        activateButtonsLock();

        if (dirSizeThread != null) dirSizeThread.interrupt();

        if (predefinedPassword == null)
            passwordDialog = new PasswordDialog(this, PasswordDialog.PD_MODE_SET_PASSWORD, Encryptor.PURPOSE_FILE_ENCRYPTION);
        else {
            passwordDialog = new PasswordDialog(this, PasswordDialog.PD_MODE_SET_PASSWORD, predefinedPassword, Encryptor.PURPOSE_FILE_ENCRYPTION);
            predefinedPassword = null;
        }
        passwordDialog.setEncryptAlgorithmCode(encryptAlgorithmCode);
        passwordDialog.setCustomTitle(getResources().getString(R.string.passwordDialog_title_set_session));
        passwordDialog.setParentMessage("SessionPassword");

        passwordDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                deactivateButtonsLock();
            }
        });

        passwordDialog.show();
    }

    // Show dialog for Enc/Dec output directory selection
    private void showChooseOutputDirectoryDialog(boolean encryption) {
        List<String> title = new ArrayList<String>();
        List<String> comment = new ArrayList<String>();
        List<Object> tag = new ArrayList<Object>();
        List<Integer> itemType = new ArrayList<Integer>();


        String dialogTitle = getResources().getString(R.string.fe_select_destination);
        if (outputParameters.encAndDecNeeded)
            dialogTitle += " (" + (encryption ? getResources().getString(R.string.common_encryption_text) : getResources().getString(R.string.common_decryption_text)) + ")";

        title.add(getResources().getString(R.string.SIV_SameAsSource));
        tag.add("fe_outputDir_asSource");
        if (isCurrentDirReadOnly()) {
            itemType.add(SelectionDialog.ITEMTYPE_INACTIVE_RED_COMMENT);
            comment.add("(" + getResources().getString(R.string.fe_directoryReadOnly).toLowerCase().replace(".", "") + ")");
        } else {
            itemType.add(SelectionDialog.ITEMTYPE_NORMAL);
            comment.add(null);
        }

        CryptFileWrapper predefinedDir = null;
        if (encryption) {
            predefinedDir = getEncFilesDir();
            title.add(getResources().getString(R.string.fe_favouriteDialog_encFileDir));
            tag.add("fe_outputDir_encFilesDir");
        } else {
            predefinedDir = getDecFilesDir();
            title.add(getResources().getString(R.string.fe_favouriteDialog_decFileDir));
            tag.add("fe_outputDir_decFilesDir");
        }
        if (predefinedDir != null) {
            comment.add(predefinedDir.getUniqueIdentifier());
            itemType.add(SelectionDialog.ITEMTYPE_NORMAL);
        } else {
            comment.add("(" + getResources().getString(R.string.common_unavailable_text) + ")");
            itemType.add(SelectionDialog.ITEMTYPE_INACTIVE);
        }

        if (currentDir.getMode() == CryptFileWrapper.MODE_FILE && (android.os.Build.VERSION.SDK_INT >= 12)) {
            title.add(getResources().getString(R.string.fe_select_destination));
            tag.add("fe_outputDir_selectDir");
            comment.add(null);
            itemType.add(SelectionDialog.ITEMTYPE_NORMAL);
        }

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            if (currentDir.getMode() == CryptFileWrapper.MODE_FILE) {
                title.add(getResources().getString(R.string.fe_select_destination) + " (SAF)");
                comment.add(getResources().getString(R.string.fe_safUseOnlyIfNecessary));
            } else {
                title.add(getResources().getString(R.string.fe_select_destination));
                comment.add(null);
            }
            tag.add("fe_outputDir_selectDirSAF");
            if (isCurrentDirReadOnly()) itemType.add(SelectionDialog.ITEMTYPE_HIGHLIGHTED);
            else itemType.add(SelectionDialog.ITEMTYPE_NORMAL);
        }

        SelectionDialog selectDirDialog = new SelectionDialog(
                this,
                title,
                comment,
                null,
                tag,
                dialogTitle,
                true);
        selectDirDialog.setMessageCode(FEA_MESSAGE_OUTPUTDIR_SELECTION);
        selectDirDialog.setCommentEllipsize(TruncateAt.START);
        selectDirDialog.setItemTypes(itemType);
        selectDirDialog.setAttachment(encryption);
        selectDirDialog.deferredInit();
        selectDirDialog.show();
    }

    // Check output directory is not part of input directory
    private boolean checkOutputNotSubdirOfInput() {
        if (dirSizeThread != null) dirSizeThread.interrupt();

        if (outputParameters.outputDirectoryEncrypted != null && encFileDestination != FILE_DESTINATION_ASSOURCE) {
            if (Helpers.isSubDirectoryInList(new ArrayList<CryptFileWrapper>(selectedItemsMap.values()), outputParameters.outputDirectoryEncrypted)) {
                showErrorDialog(getResources().getString(R.string.fe_warning_outputSubdirOfInput));
                return false;
            }
        }
        return true;
    }

    private void prepareProgressBarToken() {
        progressBarToken = new ProgressBarToken(renderPhase);
        progressBarToken.setEncryptAllToOneFile(outputParameters.allToOneFile);
        progressBarToken.setCustomFileName(outputParameters.outputFileName);
        progressBarToken.setCustomOutputDirectoryEncrypted(outputParameters.outputDirectoryEncrypted);
        progressBarToken.setCustomOutputDirectoryDecrypted(outputParameters.outputDirectoryDecrypted);

        initProgressBar();
        progressBarToken.setProgressHandler(progressHandler);
        progressBarToken.getDialog().show();
    }

    private boolean selectDir(final CryptFileWrapper clickedFile) {
        CryptFileWrapper parentFile = clickedFile.getParentFile();
        if (parentFile == null) return false;
        if (clickedFile.listFiles() == null) {
            ComponentProvider.getShowMessageDialog(this,
                            null,
                            getResources().getString(R.string.fe_directoryCannotBeSelected),
                            ComponentProvider.DRAWABLE_ICON_INFO_RED)
                    .show();
            return true;
        }

        int beforeSelect = selectedItemsMap.size();
        setSelectedItem(clickedFile);
        fileArrayAdapter.notifyDataSetChanged();

        // Read only check
        if (selectedItemsMap.size() > 0) {
            moreButton.setEnabled(true);
            startEncDecButton.setEnabled(true);
            if (selectedItemsMap.size() > 1)
                startEncDecButton.setText(getResources().getString(R.string.fe_goButton));
            else {
                CryptFileWrapper singlefile = selectedItemsMap.get(selectedItemsMap.firstKey());
                if (singlefile.isDirectory())
                    startEncDecButton.setText(getResources().getString(R.string.fe_goButtonEncDir));
                else if (singlefile.isEncrypted())
                    startEncDecButton.setText(getResources().getString(R.string.fe_goButtonDecFile));
                else
                    startEncDecButton.setText(getResources().getString(R.string.fe_goButtonEncFile));
            }
        } else if (selectedItemsMap.size() >= beforeSelect) {
            ComponentProvider.getImageToast("<b>" + getResources().getString(R.string.common_warning_text)
                    + ":</b><br/>" + getResources().getString(R.string.fe_parentDirectoryReadOnly), ComponentProvider.DRAWABLE_ICON_CANCEL, this).show();
            showFileReadOnlyDialog();
        }

        if (selectedItemsMap.size() >= beforeSelect) {
            final String dirPath = clickedFile.getUniqueIdentifier();
            ((FileEncArrayAdapter) fileArrayAdapter).removeDirSize(dirPath);
            if (dirSizeThread != null) dirSizeThread.interrupt();
            dirSizeThread = new Thread(new Runnable() {
                public void run() {
                    long[] dirSize = new long[3];
                    try {
                        dirSize = Helpers.getDirectorySizeWithInterruptionCheckWrapped(clickedFile);
                    } catch (InterruptedException e) {
                        dirSize[0] = -1;
                        dirSize[1] = -1;  // number of files
                        dirSize[2] = -1; // number of dirs
                    }
                    List message = new ArrayList();
                    message.add(dirPath);
                    message.add(dirSize);

                    universalHandler.sendMessage(Message.obtain(universalHandler, FEA_UNIVERSALHANDLER_SHOW_DIRSIZE, message));
                }
            });
            dirSizeThread.setPriority(Thread.MIN_PRIORITY);
            dirSizeThread.start();
        }

        return true;
    }

    private void selectFile(CryptFileWrapper clickedFile) {
        CryptFileWrapper parentFile = clickedFile.getParentFile();
        if (parentFile == null) return;

        int beforeSelect = selectedItemsMap.size();
        setSelectedItem(clickedFile);
        fileArrayAdapter.notifyDataSetChanged();

        if (selectedItemsMap.size() > 0) {
            moreButton.setEnabled(true);
            startEncDecButton.setEnabled(true);
            if (selectedItemsMap.size() > 1)
                startEncDecButton.setText(getResources().getString(R.string.fe_goButton));
            else {
                CryptFileWrapper singlefile = selectedItemsMap.get(selectedItemsMap.firstKey());
                if (singlefile.isDirectory())
                    startEncDecButton.setText(getResources().getString(R.string.fe_goButtonEncDir));
                else if (singlefile.isEncrypted())
                    startEncDecButton.setText(getResources().getString(R.string.fe_goButtonDecFile));
                else
                    startEncDecButton.setText(getResources().getString(R.string.fe_goButtonEncFile));
            }
        } else if (selectedItemsMap.size() >= beforeSelect) {
            ComponentProvider.getImageToast("<b>" + getResources().getString(R.string.common_warning_text)
                    + ":</b><br/>" + getResources().getString(R.string.fe_parentDirectoryReadOnly), ComponentProvider.DRAWABLE_ICON_CANCEL, this).show();
            showFileReadOnlyDialog();
        }
    }

    // Select All Files/Dirs
    private void selectAll() {
        selectAll(null);
    }

    // Select All Files/Dirs in List
    private void selectAll(List<CryptFileWrapper> selectList) {
        Set<String> fileNamesSet = null;
        if (selectList != null) {
            fileNamesSet = new HashSet<String>();
            for (int i = 0; i < selectList.size(); ++i) {
                fileNamesSet.add(selectList.get(i).getName());
            }
        }

        for (int i = 0; i < currentFiles.size(); ++i) {
            CryptFileWrapper tempFile = currentFiles.get(i);

            if ((tempFile.isFile() || tempFile.isDirectory()) && !tempFile.isBackDir() && (fileNamesSet == null || fileNamesSet.contains(tempFile.getName()))) {
                tempFile.setSelected(true);
                selectedItemsMap.put(tempFile.getName(), tempFile);
            }
        }
        fileArrayAdapter.notifyDataSetChanged();
        if (selectedItemsMap.size() > 1) {
            startEncDecButton.setEnabled(true);
            startEncDecButton.setText(getResources().getString(R.string.fe_goButton));
            bottomTextView.setText(getResources().getString(R.string.common_selectedXItems).replaceAll("<1>", Integer.toString(selectedItemsMap.size())));
        } else if (selectedItemsMap.size() == 1) {
            bottomTextView.setEllipsize(TruncateAt.START);
            bottomTextView.setText(getResources().getString(R.string.fe_selected_text) + " " + selectedItemsMap.firstKey());
        }
    }

    // Deselect All Files/Dirs
    private void deselectAll() {
        selectedItemsMap.clear();

        for (int i = 0; i < currentFiles.size(); ++i) {
            CryptFileWrapper tempFile = currentFiles.get(i);

            if ((tempFile.isFile() || tempFile.isDirectory()) && !tempFile.isBackDir()) {
                tempFile.setSelected(false);
            }
        }

        updateCurrentFilesPhase1();
        fileArrayAdapter.notifyDataSetChanged();
        bottomTextView.setEllipsize(TruncateAt.END);
        bottomTextView.setText(getTip(1));
    }

    // Any special action for this directory?
    private void checkCurrentDirectoryForWarnings() {
        try {
            if (uninstallWarningPathsList != null) {
                String testPath = currentDir.getFile().getCanonicalPath();
                for (int i = 0; i < uninstallWarningPathsList.size(); ++i) {
                    if (testPath.startsWith(uninstallWarningPathsList.get(i))) {
                        HtmlAlertDialog uninstallWarning = new HtmlAlertDialog(this, getResources().getString(R.string.alerts_dir) + "Alert_UninstallWarning.html", getResources().getString(R.string.common_message_text));
                        uninstallWarning.show();
                        uninstallWarningPathsList = null;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Show FileAction (More) dialog
    private void showFileActionDialog() {
        if (dirSizeThread != null) dirSizeThread.interrupt();
        if (handleFileUpdateLock()) return;
        if (!verifyMultiSelectionShowDialog()) {
            fileUpdateLock = false;
            return;
        }

        String renameText = "";
        String deleteText = "";
        String wipeText = "";
        String titleText = getResources().getString(R.string.fe_fileactionDialog_universalTitle);

        List<String> itemList = new ArrayList<String>();
        List<String> commentList = new ArrayList<String>();
        List<Object> keyList = new ArrayList<Object>();
        List<Integer> itemType = new ArrayList<Integer>();

        if (FileEncryptionService.isRunning()) {
            itemList.add(getResources().getString(R.string.fes_terminateRunningTasks));
            commentList.add(null);
            keyList.add("fes_terminateRunningTasks");
            itemType.add(SelectionDialog.ITEMTYPE_HIGHLIGHTED);
        }

        if (multiSelection) {
            if (!isSelectedAll()) {
                itemList.add(getResources().getString(R.string.fe_fileactionDialog_selectAll));
                commentList.add(null);
                keyList.add("fe_fileactionDialog_selectAll");
                itemType.add(SelectionDialog.ITEMTYPE_NORMAL);
            }
            if (selectedItemsMap.size() > 0) {
                itemList.add(getResources().getString(R.string.fe_fileactionDialog_deselectAll));
                commentList.add(null);
                keyList.add("fe_fileactionDialog_deselectAll");
                itemType.add(SelectionDialog.ITEMTYPE_NORMAL);
            }
        }

        List fileList = null;

        if (selectedItemsMap.size() > 0) {
            CryptFileWrapper firstItem = selectedItemsMap.get(selectedItemsMap.firstKey());
            fileList = new ArrayList<CryptFileWrapper>(selectedItemsMap.values());

            if (selectedItemsMap.size() < 2) {
                renameText = firstItem.isFile() ? getResources().getString(R.string.fe_fileactionDialog_renameFile)
                        : getResources().getString(R.string.fe_fileactionDialog_renameFolder);
                deleteText = firstItem.isFile() ? getResources().getString(R.string.fe_fileactionDialog_deleteFile)
                        : getResources().getString(R.string.fe_fileactionDialog_deleteFolder);
                wipeText = firstItem.isFile() ? getResources().getString(R.string.fe_fileactionDialog_wipeFile)
                        : getResources().getString(R.string.fe_fileactionDialog_wipeFolder);
            } else {
                deleteText = getResources().getString(R.string.fe_fileactionDialog_deleteFiles);
                wipeText = getResources().getString(R.string.fe_fileactionDialog_wipeFiles);
            }

            if (currentDir.getWritePermissionLevelForDir() > 0) {
                if (selectedItemsMap.size() < 2) {
                    itemList.add(renameText);
                    commentList.add(null);
                    keyList.add("fe_fileactionDialog_renameFile");
                    itemType.add(SelectionDialog.ITEMTYPE_NORMAL);
                }
                itemList.add(deleteText);
                keyList.add("fe_fileactionDialog_deleteFile");
                if (FileEncryptionService.isRunning()) {
                    itemType.add(SelectionDialog.ITEMTYPE_INACTIVE);
                    commentList.add("(" + getResources().getString(R.string.fes_terminateRunningTasks).toLowerCase() + ")");
                } else {
                    itemType.add(SelectionDialog.ITEMTYPE_NORMAL);
                    commentList.add(null);
                }
                if (currentDir.getMode() == CryptFileWrapper.MODE_FILE) {
                    itemList.add(wipeText);

                    keyList.add("fe_fileactionDialog_wipeFile");
                    if (FileEncryptionService.isRunning()) {
                        itemType.add(SelectionDialog.ITEMTYPE_INACTIVE);
                        commentList.add("(" + getResources().getString(R.string.fes_terminateRunningTasks).toLowerCase() + ")");
                    } else {
                        itemType.add(SelectionDialog.ITEMTYPE_NORMAL);
                        commentList.add("(" + getResources().getString(R.string.common_useHelpForDetails) + ")");
                    }
                }
            }
            if (selectedItemsMap.size() < 2 && firstItem.isFile() && !firstItem.isEncrypted()) {
                itemList.add(getResources().getString(R.string.fe_fileactionDialog_openFile));
                commentList.add(null);
                keyList.add("fe_fileactionDialog_openFile");
                itemType.add(SelectionDialog.ITEMTYPE_NORMAL);
            }
            if (isOnlyFilesSelection()) {
                itemList.add(getResources().getString(R.string.fe_fileactionDialog_sendFile));
                commentList.add(null);
                keyList.add("fe_fileactionDialog_sendFile");
                itemType.add(SelectionDialog.ITEMTYPE_NORMAL);
            }
        }

        if (currentDir.getWritePermissionLevelForDir() > 0) {
            itemList.add(getResources().getString(R.string.common_text_createFolder));
            commentList.add(null);
            keyList.add("fe_fileactionDialog_createFolder");
            itemType.add(SelectionDialog.ITEMTYPE_NORMAL);
        }

        if (keyList.size() > 0) {
            SelectionDialog fileActionDialog = new SelectionDialog(this,
                    itemList,
                    commentList, null,
                    keyList,
                    titleText,
                    true);
            fileActionDialog.setItemTypes(itemType);
            fileActionDialog.deferredInit();
            fileActionDialog.setAttachment(fileList);
            fileActionDialog.setMessageCode(FEA_MESSAGE_DIALOG_FILEACTION);

            if (fileActionDialog != null) fileActionDialog.show();
        } else {
            new ImageToast(getResources().getString(R.string.common_noActionsAvailable_text), ImageToast.TOAST_IMAGE_INFO_RED, this).show();
        }

        fileUpdateLock = false;
    }

    // Are "all possible files" selected?
    private boolean isSelectedAll() {
        int selectable = 0;
        for (int i = 0; i < currentFiles.size(); ++i) {
            CryptFileWrapper tempFile = currentFiles.get(i);
            if ((tempFile.isFile() || tempFile.isDirectory()) && !tempFile.isBackDir())
                ++selectable;
        }
        return (selectedItemsMap.size() == selectable);
    }

    // Are all selected "files" files?
    private boolean isOnlyFilesSelection() {
        List<CryptFileWrapper> selectedFiles = new ArrayList<CryptFileWrapper>(selectedItemsMap.values());
        for (int i = 0; i < selectedFiles.size(); ++i) {
            CryptFileWrapper tempFile = selectedFiles.get(i);
            if (!tempFile.isFile()) return false;
        }
        return true;
    }

    // Are all selected "files" encrypted?
    private boolean isOnlyEncryptedFilesSelection() {
        List<CryptFileWrapper> selectedFiles = new ArrayList<CryptFileWrapper>(selectedItemsMap.values());
        for (int i = 0; i < selectedFiles.size(); ++i) {
            CryptFileWrapper tempFile = selectedFiles.get(i);
            if (!tempFile.isEncrypted()) return false;
        }
        return true;
    }

    // Are all selected "files" unencrypted?
    private boolean isOnlyUnencryptedFilesSelection() {
        List<CryptFileWrapper> selectedFiles = new ArrayList<CryptFileWrapper>(selectedItemsMap.values());
        for (int i = 0; i < selectedFiles.size(); ++i) {
            CryptFileWrapper tempFile = selectedFiles.get(i);
            if (tempFile.isEncrypted()) return false;
        }
        return true;
    }

    // Verify selectedItemsMap against currentFiles + Show Warning
    private boolean verifyMultiSelectionShowDialog() {
        boolean ok = verifyMultiSelection();
        if (!ok) {
            Dialog showMessageDialog = ComponentProvider.getShowMessageDialog(this,
                    "Warning",
                    "Selection Inconsistent",
                    ComponentProvider.DRAWABLE_ICON_CANCEL);
            showMessageDialog.show();
        }
        return ok;
    }

    // Verify selectedItemsMap against currentFiles
    private boolean verifyMultiSelection() {
        List<String> fileNames = new ArrayList<String>();

        for (int i = 0; i < currentFiles.size(); ++i) {
            CryptFileWrapper tempFile = currentFiles.get(i);
            if (tempFile.isSelected()) fileNames.add(tempFile.getName());
        }

        if (selectedItemsMap.size() != fileNames.size()) return false;

        for (int i = 0; i < fileNames.size(); ++i) {
            if (!selectedItemsMap.containsKey(fileNames.get(i))) return false;
        }

        return true;
    }

    // Get user defined Home Directory
    private CryptFileWrapper getHomeDir(String favouritesString) {
        CryptFileWrapper homeDirTemp = null;
        try {
            homeDirTemp = (CryptFileWrapper) settingDataHolder.getPersistentDataObject(favouritesString);
            if (homeDirTemp.getMode() == CryptFileWrapper.MODE_DOCUMENT_FILE)
                homeDirTemp = homeDirTemp.tryToCreateCFWfromSerializationString(context);
        } catch (Exception e) {
            homeDirTemp = null;
        }
        if (homeDirTemp != null && !homeDirTemp.exists()) homeDirTemp = null;
        return homeDirTemp;
    }

    // Get user defined Encrypted File Directory
    private CryptFileWrapper getEncFilesDir() {
        CryptFileWrapper dir = (CryptFileWrapper) settingDataHolder.getPersistentDataObject("FAVOURITES_ENCFILES");
        if (dir != null && dir.exists() && (dir.canWrite() || Helpers.isWriteExceptionDir(dir, this)) && dir.getWritePermissionLevelForDir() > 1)
            return dir;  // direct file access
        return null;
    }

    // Get user defined Decrypted File Directory
    private CryptFileWrapper getDecFilesDir() {
        CryptFileWrapper dir = (CryptFileWrapper) settingDataHolder.getPersistentDataObject("FAVOURITES_DECFILES");
        if (dir != null && dir.exists() && (dir.canWrite() || Helpers.isWriteExceptionDir(dir, this)) && dir.getWritePermissionLevelForDir() > 1)
            return dir;  // direct file access
        return null;
    }

    // Read Only Parent Directory Warning
    private void showFileReadOnlyDialog() {
        if (android.os.Build.VERSION.SDK_INT < 19) return;

        String message = getResources().getString(R.string.fe_parentDirectoryReadOnly);

        if (android.os.Build.VERSION.SDK_INT == 19 || android.os.Build.VERSION.SDK_INT == 20) // KitKat
            message += getResources().getString(R.string.fe_kitkatIssue);
        if (android.os.Build.VERSION.SDK_INT >= 21) { // Android 5 or above
            message += getResources().getString(R.string.fe_tryToUseSAF);
        }

        Dialog showMessageDialog = ComponentProvider.getShowMessageDialog(this,
                getResources().getString(R.string.fe_readOnlyWarningTitle),
                message,
                ComponentProvider.DRAWABLE_ICON_CANCEL,
                "safblink",
                FEA_MESSAGE_READONLY_DIALOG_AFTER
        );
        showMessageDialog.show();
    }

    // Get text list of the .enc files that will be replaced
    private String getReplaceEncFilesWarning(CryptFileWrapper outputDir) {
        StringBuilder fileListString = new StringBuilder();

        if (outputParameters.allToOneFile) {
            String tempName = outputParameters.outputFileName + "." + Encryptor.ENC_FILE_EXTENSION;
            if (outputDir.existsChild(tempName)) fileListString.append(tempName + "<br/>");
        } else {
            List<String> selectedFileNames = new ArrayList<String>(selectedItemsMap.keySet());
            for (int i = 0; i < selectedFileNames.size(); ++i) {
                CryptFileWrapper selectedFileTemp = selectedItemsMap.get(selectedFileNames.get(i));
                if (selectedFileTemp != null && selectedFileTemp.isEncrypted()) continue;
                String tempName = selectedFileNames.get(i) + "." + Encryptor.ENC_FILE_EXTENSION;
                if (outputDir.existsChild(tempName)) fileListString.append(tempName + "<br/>");
            }
        }

        String output = fileListString.toString().trim();
        if (output.length() == 0) return null;
        else return "<small>" + output + "</small>";
    }

    private boolean isCurrentDirReadOnly() {
        return !(currentDir.getWritePermissionLevelForDir() > 0);
    }

    private void showSAFOutputDirPicker(boolean forEncryption) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        int request = forEncryption ? REQUEST_CODE_SAF_OUTPUT_ONLY_ENCRYPTION : REQUEST_CODE_SAF_OUTPUT_ONLY_DECRYPTION;
        startActivityForResult(intent, request);
    }

    private synchronized boolean handleFileUpdateLock() {
        if (fileUpdateLock) return true;
        else {
            fileUpdateLock = true;
            return false;
        }
    }

    // Return current thumbnail size
    public int getThumbnailSizeCode() {
        return thumbnailSizeCode;
    }

    private void showExitDialog() {
        ComponentProvider.getBaseQuestionDialog(this,
                getResources().getString(R.string.common_returnToMainMenuTitle),
                getResources().getString(R.string.common_question_leave).replaceAll("<1>", getResources().getString(R.string.common_app_fileEncryptor_name)),
                null,
                COMMON_MESSAGE_CONFIRM_EXIT
        ).show();
    }

    private void terminateRunningTasks() {
        Handler terminationHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    waitDialog = new SimpleWaitDialog(FileEncActivity.this);
                    new Thread(new Runnable() {
                        public void run() {
                            PowerManager.WakeLock wakeLock;
                            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SSE:FE_SERVICE_TERMINATION");

                            wakeLock.acquire();
                            universalHandler.sendMessage(Message.obtain(universalHandler, FEA_UNIVERSALHANDLER_SHOW_WAITDIALOG));
                            try {
                                while (FileEncryptionService.isRunning()) {
                                    FileEncryptionService.terminateTasks();
                                    Thread.sleep(1000);
                                }
                                universalHandler.sendMessage(Message.obtain(universalHandler, FileEncActivity.FEA_UNIVERSALHANDLER_SHOW_OK_TOAST,
                                        getResources().getString(R.string.fes_runningTasksTerminated).toLowerCase()));

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            FileEncryptionService.updateCurrentFilesPhase2(currentDir, currentFilesTemp, showRoot);
                            universalHandler.sendMessage(Message.obtain(universalHandler, FEA_UNIVERSALHANDLER_REFRESH_FILELIST_P13));
                            wakeLock.release();
                        }
                    }).start();
                }
            }
        };
        ComponentProvider.getBaseQuestionDialog(
                this,
                getResources().getString(R.string.fe_report_enc_title),
                getResources().getString(R.string.fes_alreadyRunningQuestion),
                terminationHandler,
                false, true).show();
    }

    // Handler for the background ENC/DEC progress updating
    Handler progressHandler = new Handler() {
        FinalMessageBean finalMessageBean = new FinalMessageBean();

        public synchronized void handleMessage(Message msg) {
            try {
                handleMessageExecution(msg);
            } catch (android.view.WindowManager.BadTokenException e) {
                e.printStackTrace();
            }
        }

        private void handleMessageExecution(Message msg) {
            if (msg.what == -1100) {
                int progressRel = ((ProgressMessage) msg.obj).getProgressRel();
                //int secondaryProgressRel = ((ProgressMessage)msg.obj).getSecondaryProgressRel();
                progressBarToken.getDialog().setProgress(progressRel);
                return;
            }
            if (msg.what == -1101) { // Append Verbose Text
                String verbose = (String) msg.obj;
                progressBarToken.getDialog().appendText(verbose);
                return;
            }
            if (msg.what == -1102) { // Append Verbose Text Red
                String verbose = (String) msg.obj;
                progressBarToken.getDialog().appendTextRed(verbose);
                return;
            }
            if (msg.what == -1110) { // Set progress B Max
                int maxB = (int) ((ProgressMessage) msg.obj).getFullSizeB();
                progressBarToken.getDialog().setMaxB(maxB);
                return;
            }
            if (msg.what == -1111) { // Set progress B
                int progressAbs = (int) ((ProgressMessage) msg.obj).getProgressAbsB();
                progressBarToken.getDialog().setProgressB(progressAbs);
                return;
            }
            if (msg.what == -1011) { // compressing + encrypting (one pass - since 1.3)
                String algComment = (String) msg.obj;
                String message = getResources().getString(R.string.common_encrypting_text)
                        + " (<small>" + algComment + "</small>)";
                progressBarToken.getDialog().setMessage(Html.fromHtml(message));
                progressBarToken.getDialog().appendText(message + ": ");
                renderPhase.setRenderPhase(2);
                return;
            }
            if (msg.what == -1112) { // report CRC Error
                String verbose = getResources().getString(R.string.fe_integrity2_error) + " <b>" + (String) msg.obj + "</b><br/>";
                progressBarToken.getDialog().appendTextRed(verbose);
                return;
            }
            if (msg.what == -1113) { // verbose finish report (OK + output file name)
                boolean ok = (Boolean) msg.obj;
                FinalMessageBean fmb = finalMessageBean.clone();
                if (ok && fmb.errorMessage.equals(""))
                    progressBarToken.getDialog().appendText("<b>OK</b><br/>");
                CryptFileWrapper oFile = fmb.outputFileForFMB;
                if (oFile != null) {
                    String outputFileNameText = (fmb.outputFileForFMB.isDirectory()) ? getResources().getString(R.string.fe_report_outputFolder) : getResources().getString(R.string.fe_report_outputFile);
                    outputFileNameText = outputFileNameText.replaceAll("<1>", Matcher.quoteReplacement(fmb.outputFileForFMB.getName())) + "<br/>";
                    progressBarToken.getDialog().appendText(outputFileNameText);
                }
                finalMessageBean.reset();
                return;
            }
            if (msg.what == -1201) { // wiping only (Helpers.wipeFileOrDirectory(...))
                if (currentDir.getMode() == CryptFileWrapper.MODE_FILE)
                    progressBarToken.getDialog().setMessage(getResources().getString(R.string.common_wiping_text));
                else
                    progressBarToken.getDialog().setMessage(getResources().getString(R.string.common_deleting_text));
                renderPhase.setRenderPhase(10);
                return;
            }
            if (msg.what == -1202) { // temp files wiping
                if (currentDir.getMode() == CryptFileWrapper.MODE_FILE)
                    progressBarToken.getDialog().setMessage(getResources().getString(R.string.fe_wipingTempFiles));
                else
                    progressBarToken.getDialog().setMessage(getResources().getString(R.string.common_deleting_text));
                renderPhase.setRenderPhase(11);
                return;
            }
            if (msg.what == -1211) { // directory stats from delete/wipe procedures
                Helpers.DirectoryStats ds = null;
                if (msg.obj != null) ds = ((Helpers.DirectoryStats) msg.obj);
                finalMessageBean.files = ds.allFiles;
                finalMessageBean.folders = ds.allFolders;
                finalMessageBean.deletedFiles = ds.okFiles;
                finalMessageBean.deletedFolders = ds.okFolders;
                return;
            }
            if (msg.what == -1212) { // source file not deleted
                finalMessageBean.singleDeleteFailed = true;
                progressBarToken.getDialog().appendText("<b><font color='#FF0000'>" + getResources().getString(R.string.common_fileFailedToDelete) + "</font></b><br/>");
                return;
            }
            if (msg.what == 0) { // reset progress
                progressBarToken.getDialog().setProgress(0);
                return;
            }
            if (msg.what == -997) { // show/hide progress B
                progressBarToken.getDialog().hideProgressBarB((Boolean) msg.obj);
                return;
            }
            if (msg.what == -998) { // set progress A to 100
                progressBarToken.getDialog().setProgress(100);
                return;
            }
            if (msg.what == -999) { // checking directory
                long filesNumber = ((ProgressMessage) msg.obj).getProgressAbs();
                progressBarToken.getDialog().setMessage(getResources().getString(R.string.common_checkingdir_text).replaceAll("<1>", Long.toString(filesNumber)));
                renderPhase.setRenderPhase(2);
                return;
            }
            if (msg.what == -1000) { // generatingKey
                progressBarToken.getDialog().setMessage(getResources().getString(R.string.common_generatingKey_text));
                renderPhase.setRenderPhase(2);
                return;
            }
            if (msg.what == -1001) { // compressing
                progressBarToken.getDialog().setMessage(getResources().getString(R.string.common_compressing_text));
                renderPhase.setRenderPhase(1);
                return;
            }
            if (msg.what == -1002) { // encrypting
                progressBarToken.getDialog().setMessage(getResources().getString(R.string.common_encrypting_text)
                        + "  (" + encryptor.getEncryptAlgorithmShortComment() + ")");
                renderPhase.setRenderPhase(2);
                return;
            }
            if (msg.what == -1003) { // decompressing
                progressBarToken.getDialog().setMessage(getResources().getString(R.string.common_decompressing_text));
                renderPhase.setRenderPhase(3);
                return;
            }
            if (msg.what == -1004) { // decrypting
                String algComment = (String) msg.obj;
                String message = getResources().getString(R.string.common_decrypting_text)
                        + "  (<small>" + algComment + "</small>)";
                progressBarToken.getDialog().setMessage(Html.fromHtml(message));
                progressBarToken.getDialog().appendText(message + ": ");
                renderPhase.setRenderPhase(4);
                return;
            }

            if (msg.what == -400) { // handle unexpected errors
                String error = "error";
                if (msg.obj != null) error = ((String) msg.obj);
                finalMessageBean.errorMessage = error;
                progressBarToken.getDialog().appendTextRed("<br/>" + error + "<br/>");
                return;
            }
            if (msg.what == -401) { // handle interruption and other "expected" events
                String error = "interruption";
                if (msg.obj != null) error = ((String) msg.obj);
                finalMessageBean.errorMessage = error;
                progressBarToken.getDialog().appendTextRed("<br/>" + error + "<br/>");
                return;
            }
            if (msg.what == -200) {
                //not used
                return;
            }
            if (msg.what == -100) // finalize ENC/DEC/WIPE
            {
                int selectedFiles = selectedItemsMap.size();
                progressBarToken.getCancelDialog().cancel();
                updateCurrentFilesPhase1();
                updateCurrentFilesPhase3();
                fileArrayAdapter.notifyDataSetChanged();
                if (selectedFiles < 2 || progressBarToken.getEncryptAllToOneFile()) {
                    try {
                        progressBarToken.getDialog().cancel();
                    } catch (Exception e) {
                    }
                } else progressBarToken.getDialog().setEnabledButton(true);
                pbLock = false;
                renderPhase.setRenderPhase(0);
                encDecThread = null;
                wipeThread = null;
                if (selectedFiles < 2 || progressBarToken.getEncryptAllToOneFile())
                    setMessage(new ActivityMessage(FEA_MESSAGE_AFTERENCRYPT_REPORT, null, finalMessageBean.clone()));
                finalMessageBean.reset();

                return;
            }

            //+ Create "After ENC/DEC Report"
            if (msg.what == FEA_PROGRESSHANDLER_SET_MAINMESSAGE) {
                finalMessageBean.mainMessage = ((String) msg.obj);
                return;
            }
            if (msg.what == FEA_PROGRESSHANDLER_SET_INPUTFILEPATH) {
                finalMessageBean.inputFileForFMB = ((CryptFileWrapper) msg.obj);
                return;
            }
            if (msg.what == FEA_PROGRESSHANDLER_SET_OUTPUTFILEPATH) {
                finalMessageBean.outputFileForFMB = ((CryptFileWrapper) msg.obj);
                return;
            }
            if (msg.what == FEA_PROGRESSHANDLER_SET_ERRORMESSAGE) {
                finalMessageBean.errorMessage = ((String) msg.obj);
                return;
            }
            if (msg.what == FEA_PROGRESSHANDLER_SET_DIRFILENUMBER) {
                int[] numbers = ((int[]) msg.obj);
                finalMessageBean.foldersFromAllToOneFile = (numbers[0]);
                finalMessageBean.filesFromAllToOneFile = (numbers[1]);
                return;
            }
            //- Create "After Encrypt Report"
        }
    };


    // Handler for miscellaneous background activities
    Handler universalHandler = new Handler() {
        public void handleMessage(Message msg) {
            try {
                handleMessageExecution(msg);
            } catch (android.view.WindowManager.BadTokenException e) {
                e.printStackTrace();
            }
        }

        private void handleMessageExecution(Message msg) {
            if (msg.what == FEA_UNIVERSALHANDLER_SELECT_DIR) {
                selectDir((CryptFileWrapper) msg.obj);
                return;
            }
            if (msg.what == FEA_UNIVERSALHANDLER_SHOW_WAITDIALOG) {
                if (waitDialog != null) waitDialog.show();
                return;
            }
            if (msg.what == FEA_UNIVERSALHANDLER_HIDE_WAITDIALOG) {
                if (waitDialog != null) waitDialog.cancel();
                return;
            }
            if (msg.what == FEA_UNIVERSALHANDLER_REFRESH_FILELIST_P13) {
                updateCurrentFilesPhase1();
                updateCurrentFilesPhase3();
                if (waitDialog != null) waitDialog.cancel();
                fileArrayAdapter.notifyDataSetChanged();
                pbLock = false;
                return;
            }
            if (msg.what == FEA_UNIVERSALHANDLER_SHOW_DIRSIZE) {
                List message = (List) msg.obj;
                String path = (String) message.get(0);
                long[] dirParam = (long[]) message.get(1);
                ((FileEncArrayAdapter) fileArrayAdapter).setDirSize(path, dirParam);
                fileArrayAdapter.notifyDataSetChanged();
                return;
            }
            if (msg.what == FEA_UNIVERSALHANDLER_SHOW_VOLUMESIZE) {
                List message = (List) msg.obj;
                String titleRight = (String) message.get(0);
                String titleRightTag = (String) message.get(1);
                setTitleRight(titleRight);
                setTitleRightTag(titleRightTag);
                volumeSizeThread = null;
                return;
            }
            if (msg.what == FEA_UNIVERSALHANDLER_SHOW_ERROR_TOAST) {
                ComponentProvider.getImageToast(
                        (String) msg.obj,
                        ComponentProvider.DRAWABLE_ICON_CANCEL,
                        FileEncActivity.this).show();
                return;
            }
            if (msg.what == FEA_UNIVERSALHANDLER_SHOW_OK_TOAST) {
                ComponentProvider.getImageToast(
                        (String) msg.obj,
                        ComponentProvider.DRAWABLE_ICON_OK,
                        FileEncActivity.this).show();
                return;
            }
            if (msg.what == FEA_UNIVERSALHANDLER_SHOW_ERROR_DIALOG) {
                showErrorDialog((String) msg.obj);
                return;
            }
        }
    };

    /**
     * Output File Parameters
     **/
    public class OutputParametersBean {
        public boolean allToOneFile = false;
        public boolean encAndDecNeeded = false; // for both: outputDirectoryEncrypted and outputDirectoryDecrypted will be the "choose dialog" shown
        public boolean replaceEncFilesChecked = false;
        public String outputFileName = null;
        public CryptFileWrapper outputDirectoryEncrypted = null;
        public CryptFileWrapper outputDirectoryDecrypted = null;
    }

    /**
     * Keeps "After ENC/DEC" Report data
     */
    public static class FinalMessageBean implements Cloneable {
        private String mainMessage;
        private String secondaryMessage;
        private CryptFileWrapper inputFileForFMB;
        private CryptFileWrapper outputFileForFMB;
        private String errorMessage;
        public int files;
        public int deletedFiles;
        public int folders;
        public int deletedFolders;
        public int filesFromAllToOneFile;
        public int foldersFromAllToOneFile;
        public boolean singleDeleteFailed;

        {
            reset();
        }

        public void reset() {
            mainMessage = "";
            secondaryMessage = "";
            inputFileForFMB = null;
            outputFileForFMB = null;
            errorMessage = "";
            files = 0;
            deletedFiles = 0;
            folders = 0;
            deletedFolders = 0;
            filesFromAllToOneFile = 0;
            foldersFromAllToOneFile = 0;
            singleDeleteFailed = false;
        }

        @Override
        public FinalMessageBean clone() {
            try {
                final FinalMessageBean result = (FinalMessageBean) super.clone();
                result.mainMessage = new String(mainMessage);
                result.secondaryMessage = new String(secondaryMessage);
                result.inputFileForFMB = inputFileForFMB;
                result.outputFileForFMB = outputFileForFMB;
                result.errorMessage = new String(errorMessage);
                result.files = files;
                result.deletedFiles = deletedFiles;
                result.folders = folders;
                result.deletedFolders = deletedFolders;
                result.filesFromAllToOneFile = filesFromAllToOneFile;
                result.foldersFromAllToOneFile = foldersFromAllToOneFile;
                result.singleDeleteFailed = singleDeleteFailed;
                return result;
            } catch (final CloneNotSupportedException ex) {
                throw new AssertionError();
            }
        }
    }
}
