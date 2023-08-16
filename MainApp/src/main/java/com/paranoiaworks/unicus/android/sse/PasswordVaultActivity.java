package com.paranoiaworks.unicus.android.sse;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.text.method.ScrollingMovementMethod;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.paranoiaworks.android.sse.interfaces.Lockable;
import com.paranoiaworks.unicus.android.sse.adapters.BasicListAdapter;
import com.paranoiaworks.unicus.android.sse.components.ImageToast;
import com.paranoiaworks.unicus.android.sse.components.PWVNewEditFolderDialog;
import com.paranoiaworks.unicus.android.sse.components.PWVSearchDialog;
import com.paranoiaworks.unicus.android.sse.components.PasswordDialog;
import com.paranoiaworks.unicus.android.sse.components.PasswordGeneratorDialog;
import com.paranoiaworks.unicus.android.sse.components.ScreenLockDialog;
import com.paranoiaworks.unicus.android.sse.components.SecureEditText;
import com.paranoiaworks.unicus.android.sse.components.SelectionDialog;
import com.paranoiaworks.unicus.android.sse.components.SimpleHTMLDialog;
import com.paranoiaworks.unicus.android.sse.components.SimpleWaitDialog;
import com.paranoiaworks.unicus.android.sse.components.TextViewMod;
import com.paranoiaworks.unicus.android.sse.adapters.ColorListAdapter;
import com.paranoiaworks.unicus.android.sse.adapters.CustomElementsAdapter;
import com.paranoiaworks.unicus.android.sse.adapters.PWVFolderAdapter;
import com.paranoiaworks.unicus.android.sse.adapters.PWVItemArrayAdapter;
import com.paranoiaworks.unicus.android.sse.dao.ActivityMessage;
import com.paranoiaworks.unicus.android.sse.dao.PasswordAttributes;
import com.paranoiaworks.unicus.android.sse.dao.Vault;
import com.paranoiaworks.unicus.android.sse.dao.VaultFolderV2;
import com.paranoiaworks.unicus.android.sse.dao.VaultItemV2;
import com.paranoiaworks.unicus.android.sse.dao.VaultV2;
import com.paranoiaworks.unicus.android.sse.misc.CryptFile;
import com.paranoiaworks.unicus.android.sse.misc.CryptFileWrapper;
import com.paranoiaworks.unicus.android.sse.misc.ExtendedEntropyProvider;
import com.paranoiaworks.unicus.android.sse.misc.KeyboardAppearanceDetector;
import com.paranoiaworks.unicus.android.sse.misc.KeyboardAppearanceDetector.KeyboardVisibilityChangedListener;
import com.paranoiaworks.unicus.android.sse.misc.StringSentinel;
import com.paranoiaworks.unicus.android.sse.utils.ColorHelper;
import com.paranoiaworks.unicus.android.sse.utils.ComponentProvider;
import com.paranoiaworks.unicus.android.sse.utils.Encryptor;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;
import com.paranoiaworks.unicus.android.sse.utils.KEMCipherProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.zip.DataFormatException;

import ext.com.andraskindler.quickscroll.QuickScroll;

/**
 * Password Vault activity class
 *
 * @author Paranoia Works
 * @version 2.0.4
 */
public class PasswordVaultActivity extends CryptActivity implements Lockable {

private int encryptAlgorithmCode;
private boolean askOnLeave;
private boolean disableAutoLinks;
private int lockOnPause = -1;
private boolean showBottomMenu;
private boolean sldVeto = false;
private int screenLockedPosition = -1;
private VaultV2 vault = null;
private PWVFolderAdapter iconAdapter = null;
private ViewAnimator layoutSwitcher;
private SpinnerAdapter itemColorSpinnerAdapter = new ColorListAdapter(this, ColorHelper.getColorList(), ColorListAdapter.ICONSET_ITEMS);
private SpinnerAdapter kemAlgorithmSpinnerAdapter = new BasicListAdapter(this, KEMCipherProvider.getAvailableAlgorithms());
private Dialog waitDialog;
private ScreenLockDialog sld;
private PasswordDialog changePasswordDialog;
private SelectionDialog moreDialog;
private Toast commonToast;
private KeyboardAppearanceDetector ked;
private float fontSizeMultiplier = 1.0F;
private StringSentinel dummyHolder;

// Start Layout
private LinearLayout layoutStartButtons;
private Button toMainPageButton;
private Button helpMeButton;

// Folders Layout
private List<VaultItemV2> currentItems = new ArrayList<VaultItemV2>();
private LinearLayout foldersBottomMenu;
private Button foldersMoreButton;
private Button foldersSearchButton;
private Button foldersNewFolderButton;
private Button foldersHelpButton;
private Button showMenuButton;

// Items Layout
private LinearLayout itemsBottomMenu;
private VaultFolderV2 currentFolder;
private PWVItemArrayAdapter itemsArrayAdapter;
private ListView itemsListView;
private QuickScroll itemsQuickscroll;
private Button showMenuItemsButton;
private Button itemsHelpButton;
private Button itemsNewItemButton;
private SearchView searchItemView;

// Item Detail Layout
private VaultItemV2 currentItem;
private SecureEditText itemNameEditText;
private SecureEditText itemPasswordEditText;
private SecureEditText itemCommentEditText;
private TextViewMod itemCommentTextView;
private SecureEditText itemAccountEditText;
private SecureEditText itemUrlEditText;
private SecureEditText kemPrivateKey;
private SecureEditText kemPublicKey;
private SecureEditText kemSharedSecret;
private SecureEditText kemSharedSecretEncapsulated;
private SecureEditText kemSharedSecretExtracted;
private Spinner itemColorSpinner;
private Spinner kemAlgorithmSpinner;
private TextView itemDeleteButton;
private TextView itemEditSaveButton;
private TextView itemMoveToButton;
private Button passwordGeneratorButton;
private Button convertToExtendedButton;
private Button nameToClipboardButton;
private Button passwordShowHideButton;
private Button kemPrivateKeyShowHideButton;
private Button kemSecretShowHideButton;
private Button kemSecretExtractedShowHideButton;
private Button kemPrivateKeyToClipboardButton;
private Button kemPublicKeyToClipboardButton;
private Button kemSharedSecretToClipboardButton;
private Button kemSharedSecretEncapsulatedToClipboardButton;
private Button kemSharedSecretExtractedToClipboardButton;
private Button kemPrivateKeyFromClipboardButton;
private Button kemPublicKeyFromClipboardButton;
private Button kemSharedSecretFromClipboardButton;
private Button kemSharedSecretEncapsulatedFromClipboardButton;
private Button kemSharedSecretExtractedFromClipboardButton;
private Button kemPrivateKeyCleanButton;
private Button kemPublicKeyCleanButton;
private Button kemSharedSecretCleanButton;
private Button kemSharedSecretEncapsulatedCleanButton;
private Button kemSharedSecretExtractedCleanButton;
private Button passwordToClipboardButton;
private Button accountToClipboardButton;
private Button urlToClipboardButton;
private Button noteToClipboardButton;
private Button switchTopBottomButton;
private Button openUrlButton;
private Button passwordToTextEncryptorButton;
private Button passwordToFileEncryptorButton;
private Button addNewCustomElementButton;
private TextView kemGenerateNewKeyPairButton;
private TextView kemGenerateNewSecretButton;
private TextView kemExtractSharedSecretButton;
private TextView noteCharCounter;
private ViewGroup topLeftContainer;
private ViewGroup bottomRightContainer;
private ViewGroup commentWrapContainer;
private ViewGroup commentExtendedContainer;
private ViewGroup convertToExtendedContainer;
private ViewGroup kemPrivateKeyEditOnlyContainer;
private ViewGroup kemPublicKeyEditOnlyContainer;
private ViewGroup kemSharedSecretEditOnlyContainer;
private ViewGroup kemSharedSecretEncapsulatedEditOnlyContainer;
private ViewGroup kemSharedSecretExtractedEditOnlyContainer;
private View bottomButtonLine;
private View bottomDelimiter;
private View passwordContainer;
private View accountContainer;
private View kemContainer;
private View urlContainer;
private ListView customElementsView;
private CustomElementsAdapter customElementsAdapter;


public static final String PWV_DBPREFIX = "PASSWORD_VAULT";
public static final String PWV_EXTRA_PASSWORD = "EXTRA_PASSWORD";
public static final String PWV_EXTRA_LOCKSCREEN_KEY = "LOCKSCREEN_KEY";
public static final String PWV_EXTRA_LOCKSCREEN_ON = "LOCKSCREEN_ON";
public static final String PWV_EXPORT_EXT = "pwv";
private static final int PWV_FORMAT_VERSION = 4;
private static final int REQUEST_CODE_SEND_PASSWORD = 101;

public static final int PWV_MESSAGE_FOLDER_NEW = -1101;
public static final int PWV_MESSAGE_FOLDER_SAVE = -1102;
public static final int PWV_MESSAGE_FOLDER_DELETE = -1103;
public static final int PWV_MESSAGE_FOLDER_DELETE_CONFIRM = -1104;
public static final int PWV_MESSAGE_ITEM_DELETE_CONFIRM = -1105;
public static final int PWV_MESSAGE_ITEM_MOVETOFOLDER = -1106;
public static final int PWV_MESSAGE_ITEM_SAVE_CONFIRM = -1107;
public static final int PWV_MESSAGE_ITEM_NOTE_COPY = -1108;
public static final int PWV_MESSAGE_ITEM_CREATE = -1109;
public static final int PWV_MESSAGE_MOREDIALOG = -1201;
public static final int PWV_MESSAGE_MOREDIALOG_IMPORT = -1202;
public static final int PWV_MESSAGE_MOREDIALOG_IMPORT_CONFIRM = -1203;
public static final int PWV_MESSAGE_MOREDIALOG_RESET_CONFIRM = -1204;
public static final int PWV_MESSAGE_MOREDIALOG_IMPORT_CONFIRM_XML = -1205;
public static final int PWV_MESSAGE_MOREDIALOG_IMPORT_CONFIRM_XML_PASSWORD = -1206;
public static final int PWV_MESSAGE_MOREDIALOG_MERGE = -1207;
public static final int PWV_MESSAGE_MOREDIALOG_MERGE_CONFIRM = -1208;
public static final int PWV_MESSAGE_SEARCHDIALOG_SEARCH = -1209;
public static final int PWV_MESSAGE_PWGDIALOG_SET = -1301;
public static final int PWV_MESSAGE_PWGDIALOG_SET_CONFIRM = -1302;
public static final int PWV_MESSAGE_SCREENLOCK_UNLOCK = -1401;
public static final int PWV_UNIVERSALHANDLER_SHOW_WAITDIALOG = -1501;
public static final int PWV_UNIVERSALHANDLER_HIDE_WAITDIALOG = -1502;
public static final int PWV_UNIVERSALHANDLER_SHOW_ERRORDIALOG = -1503;
public static final int PWV_UNIVERSALHANDLER_MERGE_FINALIZE = -1504;
public static final int PWV_UNIVERSALHANDLER_SEARCH_FINISH = -1505;
public static final int PWV_UNIVERSALHANDLER_KEM_SETKEYS = -1506;
public static final int PWV_UNIVERSALHANDLER_KEM_SETSECRETS = -1507;
public static final int PWV_UNIVERSALHANDLER_KEM_SETEXTRACTEDSECRET = -1508;

private static final int PWV_LAYOUT_START = 0;
private static final int PWV_LAYOUT_FOLDERS = 1;
private static final int PWV_LAYOUT_ITEMS = 2;
private static final int PWV_LAYOUT_ITEMDETAIL = 3;


@Override
public void onCreate(Bundle savedInstanceState) {
	this.setTheme(R.style.ThemeAltB);
	super.onCreate(savedInstanceState);
	this.setContentView(R.layout.la_passwordvault);
	this.setTitle(getResources().getString(R.string.common_app_passwordVault_name));
	encryptAlgorithmCode = settingDataHolder.getItemAsInt("SC_PasswordVault", "SI_Algorithm");
	askOnLeave = settingDataHolder.getItemAsBoolean("SC_Common", "SI_AskIfReturnToMainPage");
	lockOnPause = settingDataHolder.getItemAsInt("SC_PasswordVault", "SI_LockScreenTime");
	showBottomMenu = settingDataHolder.getItemAsBoolean("SC_PasswordVault", "SI_ShowMenu");
	disableAutoLinks = settingDataHolder.getItemAsBoolean("SC_PasswordVault", "SI_DisableAutoLinks");

	layoutSwitcher = (ViewAnimator) findViewById(R.id.vaultLayoutSwitcher);
	initLayoutStart();

	commonToast = new ImageToast("***", ImageToast.TOAST_IMAGE_CANCEL, this);
}

/**
 * Handle Message
 */
void processMessage() {
	ActivityMessage am = getMessage();
	if (am == null) return;

	int messageCode = am.getMessageCode();
	String mainMessage = am.getMainMessage();
	switch (messageCode) {
		case CryptActivity.COMMON_MESSAGE_SET_ENCRYPTOR:
			if (mainMessage.equals("merge")) {
				Encryptor tempEncryptor = (Encryptor) ((List) am.getAttachement()).get(1);
				List<Object> vaultParam = (List<Object>) am.getAttachement2();

				VaultV2 tempVault = null;

				try {
					tempVault = convertToLatestVaultVersion(deserializeVault((byte[]) vaultParam.get(0), (Integer) vaultParam.get(1), tempEncryptor));
				} catch (Exception e) {
					// swallow
				}

				if (tempVault != null) {
					mergeVaults(tempVault);
				} else {
					ComponentProvider.getShowMessageDialog(this,
							getResources().getString(R.string.pwv_mergeVaults),
							getResources().getString(R.string.me_decryptError),
							ComponentProvider.DRAWABLE_ICON_INFO_RED).show();
				}

				this.resetMessage();
				return;
			}

			wipeEncryptor();
			this.passwordAttributes = (PasswordAttributes) ((List) am.getAttachement()).get(0);
			this.encryptor = (Encryptor) ((List) am.getAttachement()).get(1);

			if (mainMessage.equals("enter")) {
				dummyHolder = StringSentinel.init(VaultV2.SS_GROUP_ID);
				try {
					int[] outputFlags = new int[1];
					vault = loadVaultfromDB(outputFlags);
					try {
						if (outputFlags[0] == 1) saveVaultToDBAsync();
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (DataFormatException e) { // corrupted data probably
					ComponentProvider.getShowMessageDialog(this, null, e.getLocalizedMessage(), ComponentProvider.DRAWABLE_ICON_CANCEL).show();
					encryptor = null;
					this.resetMessage();
					return;
				} catch (Exception e) {
					getStartScreenPasswordDialog().show();
					Toast tt = new ImageToast(
							this.getResources().getString(R.string.pwv_failedOnEnter),
							ImageToast.TOAST_IMAGE_CANCEL, this);
					tt.show();
					encryptor = null;
					this.resetMessage();
					return;
				}

				if (vault == null) {
					vault = getVaultOnFirstRun(null);
					vault.notifyFolderDataSetChanged();
					try {
						saveVaultToDBAsync();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				initLayoutFolders();
				initLayoutItems();
				initLayoutItemDetail();

				this.resetMessage();
				layoutSwitcher.showNext();
			}

			if (mainMessage.equals("change") && vault != null && encryptor != null) {
				try {
					saveVaultToDB();
				} catch (Exception e) {
					Toast tt = new ImageToast(e.getMessage(), ImageToast.TOAST_IMAGE_CANCEL, this);
					tt.show();
					e.printStackTrace();
				}

				changePasswordDialog = null;
				Toast tt = new ImageToast(
						this.getResources().getString(R.string.passwordDialog_passwordChanged),
						ImageToast.TOAST_IMAGE_OK,
						this);
				tt.show();
			}

			if (mainMessage.startsWith("xmlimport") && vault != null && encryptor != null) {
				try {
					dbHelper.deleteBlobData(PWV_DBPREFIX);
					saveVaultToDB();

					vault.notifyFolderDataSetChanged();
					initLayoutFolders();
					initLayoutItems();
					initLayoutItemDetail();

					layoutSwitcher.setDisplayedChild(PWV_LAYOUT_FOLDERS);

					ComponentProvider.getImageToast(this.getResources().getString(R.string.pwv_moreDialog_importVault_Loaded)
							.replaceAll("<1>", Matcher.quoteReplacement(am.getMainMessage().substring(am.getMainMessage().indexOf(":") + 1))), ImageToast.TOAST_IMAGE_OK, this).show();

				} catch (Exception e) {
					Toast tt = new ImageToast(e.getMessage(), ImageToast.TOAST_IMAGE_CANCEL, this);
					tt.setDuration(Toast.LENGTH_LONG);
					tt.show();
					e.printStackTrace();
				}
				this.resetMessage();
			}

			//if(waitDialog != null) waitDialog.cancel();
			//waitDialog = new SimpleWaitDialog(this);
			break;

		case PWV_MESSAGE_FOLDER_NEW:
			vault.addFolder((VaultFolderV2) am.getAttachement());
			try {
				saveVaultToDB();
			} catch (Exception e) {
				e.printStackTrace();
				showErrorDialog(e);
			}
			vault.notifyFolderDataSetChanged();
			iconAdapter.notifyDataSetChanged();

			this.resetMessage();
			break;

		case PWV_MESSAGE_FOLDER_SAVE:
			try {
				saveVaultToDB();
			} catch (Exception e) {
				e.printStackTrace();
				showErrorDialog(e);
			}
			vault.notifyFolderDataSetChanged();
			iconAdapter.notifyDataSetChanged();
			this.resetMessage();
			break;

		case PWV_MESSAGE_FOLDER_DELETE:
			ComponentProvider.getBaseQuestionDialog(
					this,
					getResources().getString(R.string.common_delete_text) + " " + getResources().getString(R.string.common_folder_text),
					getResources().getString(R.string.common_question_delete)
							.replaceAll("<1>", Matcher.quoteReplacement(String.valueOf(vault.getFolderByIndex((Integer) am.getAttachement()).getFolderName()))),
					(Integer) am.getAttachement() + ":" + (String) am.getMainMessage(), PWV_MESSAGE_FOLDER_DELETE_CONFIRM).show();
			this.resetMessage();
			break;

		case PWV_MESSAGE_FOLDER_DELETE_CONFIRM:
			if (am.getAttachement().equals(new Integer(1))) {
				String[] mm = am.getMainMessage().split(":");
				vault.removeFolderWithIndex(Integer.parseInt(mm[0]), mm[1]);
				iconAdapter.setSearchResultFolder(null);
				try {
					saveVaultToDB();
				} catch (Exception e) {
					e.printStackTrace();
					showErrorDialog(e);
				}
				vault.notifyFolderDataSetChanged();
				iconAdapter.notifyDataSetChanged();
				ComponentProvider.getImageToast(
						this.getResources().getString(R.string.common_question_delete_confirm),
						ImageToast.TOAST_IMAGE_OK, this).show();
			}
			this.resetMessage();
			break;

		case PWV_MESSAGE_ITEM_DELETE_CONFIRM:
			if (am.getAttachement().equals(new Integer(1))) {
				String[] mm = am.getMainMessage().split(":"); // item index, item security hash
				if (!currentFolder.isSearchResult()) {
					currentFolder.removeItemWithIndex(Integer.parseInt(mm[0]), mm[1]);
					if (iconAdapter.hasSearchResultFolder()) {
						VaultFolderV2 srFolder = iconAdapter.getSearchResultFolder();
						Integer itemPosition = srFolder.getItemPosition(mm[1]);
						if (itemPosition != null)
							srFolder.removeItemWithIndex(itemPosition, mm[1]);
					}
				} else {
					int index = Integer.parseInt(mm[0]);
					VaultItemV2 searchedItem = currentFolder.getItemByIndex(index);
					VaultFolderV2 itemParentFolder = searchedItem.getParentFolder();
					itemParentFolder.removeItemWithIndex(itemParentFolder.getItemPosition(mm[1]), mm[1]);
					currentFolder.removeItemWithIndex(Integer.parseInt(mm[0]), mm[1]);
					searchedItem.setParentFolder(null);
				}
				try {
					saveVaultToDB();
				} catch (Exception e) {
					e.printStackTrace();
					showErrorDialog(e);
				}
				resetItemsList();
				makeLayoutItemDetailReadOnly();
				currentItem = null;
				layoutSwitcher.showPrevious();
				itemDeleteButton.setEnabled(true);
				itemMoveToButton.setEnabled(true);
				itemEditSaveButton.setEnabled(true);
			}
			this.resetMessage();
			break;

		case PWV_MESSAGE_ITEM_SAVE_CONFIRM:
			if (am.getAttachement().equals(new Integer(1))) {
				String mode = am.getMainMessage();
				handleItemSave(mode);
			} else {
				leaveItemDetailLayout();
			}
			this.resetMessage();
			break;

		case PWV_MESSAGE_ITEM_MOVETOFOLDER:
			String[] mm = am.getMainMessage().split(":"); // destination folder index : destination folder hash : item index : item security hash
			VaultItemV2 itemToMove = currentFolder.getItemByIndex(Integer.parseInt(mm[2]));
			VaultFolderV2 destinationFolder = vault.getFolderByIndex(Integer.parseInt(mm[0]));
			destinationFolder.addItem(itemToMove);
			if (!currentFolder.isSearchResult()) {
				currentFolder.removeItemWithIndex(Integer.parseInt(mm[2]), mm[3]);
			} else {
				int index = Integer.parseInt(mm[2]);
				VaultItemV2 searchedItem = currentFolder.getItemByIndex(index);
				VaultFolderV2 itemParentFolder = searchedItem.getParentFolder();
				itemParentFolder.removeItemWithIndex(itemParentFolder.getItemPosition(mm[3]), mm[3]);
			}
			if (itemToMove.getParentFolder() != null)
				itemToMove.setParentFolder(destinationFolder);
			try {
				saveVaultToDB();
			} catch (Exception e) {
				e.printStackTrace();
				showErrorDialog(e);
			}
			resetItemsList();
			makeLayoutItemDetailReadOnly();
			currentItem = null;
			layoutSwitcher.showPrevious();
			itemDeleteButton.setEnabled(true);
			itemMoveToButton.setEnabled(true);
			itemEditSaveButton.setEnabled(true);

			ComponentProvider.getShowMessageDialog(this,
							null,
							getResources().getString(R.string.pwv_itemMoveToFolderReport).replaceAll("<1>", Matcher.quoteReplacement(String.valueOf(itemToMove.getItemName()))).replaceAll("<2>", Matcher.quoteReplacement(String.valueOf(destinationFolder.getFolderName()))),
							ComponentProvider.DRAWABLE_ICON_OK)
					.show();

			this.resetMessage();
			break;

		case PWV_MESSAGE_SEARCHDIALOG_SEARCH:
			if (am.getAttachement2() instanceof Object[]) {
				Object[] searchFor = (Object[]) am.getAttachement2();
				search((char[]) searchFor[0], (int) searchFor[1]);
			}

			this.resetMessage();
			break;

		case PWV_MESSAGE_MOREDIALOG:
			if (am.getMainMessage().equals("pwv_moreDialog_search")) {
				getSearchDialog().show();
			} else if (am.getMainMessage().equals("pwv_moreDialog_changePassword")) {
				changePasswordDialog = new PasswordDialog(this, PasswordDialog.PD_MODE_CHANGE_PASSWORD, Encryptor.PURPOSE_PASSWORD_VAULT);
				changePasswordDialog.setEncryptAlgorithmCode(encryptAlgorithmCode);
				changePasswordDialog.setParentMessage("change");
				changePasswordDialog.setCurrentDecryptSpec(encryptor.getKeyHash(), encryptor.getDecryptAlgorithmCode());
				changePasswordDialog.setWaitDialog(waitDialog, false);
				changePasswordDialog.show();
			} else if (am.getMainMessage().equals("pwv_moreDialog_importVault") || am.getMainMessage().equals("pwv_moreDialog_mergeVaults")) {
				File importExportDir = Helpers.getImportExportDir(settingDataHolder);
				if (importExportDir == null) {
					Dialog showMessageDialog = ComponentProvider.getShowMessageDialog(this,
							getResources().getString(R.string.pwv_moreDialog_importVault),
							getResources().getString(R.string.pwv_moreDialog_importExportVault_Invalid)
									.replaceAll("<1>", Matcher.quoteReplacement(Helpers.getImportExportPath(settingDataHolder))),
							ComponentProvider.DRAWABLE_ICON_CANCEL);
					showMessageDialog.show();
					return;
				}

				List<String> fileListPWV = Arrays.asList(importExportDir.list(
						Helpers.getOnlyExtFilenameFilter(PasswordVaultActivity.PWV_EXPORT_EXT)));
				List<String> fileListXML = Arrays.asList(importExportDir.list(
						Helpers.getOnlyExtFilenameFilter("xml")));

				List<String> fileList = new ArrayList<String>();
				Collections.sort(fileListPWV);
				Collections.sort(fileListXML);
				fileList.addAll(fileListPWV);
				fileList.addAll(fileListXML);

				List<String> fileComments = Helpers.getFileCommentsList(
						fileList,
						importExportDir.getAbsolutePath(),
						getResources().getConfiguration().locale, -1);

				if (!(fileList.size() > 0)) {
					Dialog showMessageDialog = ComponentProvider.getShowMessageDialog(this,
							getResources().getString(R.string.pwv_moreDialog_importVault),
							getResources().getString(R.string.pwv_moreDialog_importVault_NoFilesToImport)
									.replaceAll("<1>", Matcher.quoteReplacement(Helpers.getImportExportPath(settingDataHolder))),
							ComponentProvider.DRAWABLE_ICON_CANCEL);
					showMessageDialog.show();
					return;
				}


				SelectionDialog fileListDialog = null;
				if (am.getMainMessage().equals("pwv_moreDialog_mergeVaults")) {
					fileListDialog = new SelectionDialog(this,
							fileList,
							fileComments, null, null,
							getResources().getString(R.string.pwv_mergeVaults));
					fileListDialog.setMessageCode(PWV_MESSAGE_MOREDIALOG_MERGE);
				} else // import
				{
					fileListDialog = new SelectionDialog(this,
							fileList,
							fileComments, null, null,
							getResources().getString(R.string.pwv_moreDialog_importVault_dialogTitle));
					fileListDialog.setMessageCode(PWV_MESSAGE_MOREDIALOG_IMPORT);
				}

				if (fileListDialog != null) fileListDialog.show();
			} else if (am.getMainMessage().equals("pwv_moreDialog_exportVault") || am.getMainMessage().equals("pwv_moreDialog_exportVaultXML")) {
				File importExportDir = Helpers.getImportExportDir(settingDataHolder);
				if (importExportDir == null) {
					Dialog showMessageDialog = ComponentProvider.getShowMessageDialog(this,
							getResources().getString(R.string.pwv_moreDialog_exportVault),
							getResources().getString(R.string.pwv_moreDialog_importExportVault_Invalid)
									.replaceAll("<1>", Matcher.quoteReplacement(Helpers.getImportExportPath(settingDataHolder))),
							ComponentProvider.DRAWABLE_ICON_CANCEL);
					showMessageDialog.show();
					return;
				}
				if (!importExportDir.canWrite()) {
					Toast t = new ImageToast(
							"Export failed. Import dir <b>" + Helpers.getImportExportPath(settingDataHolder) + "</b> is read only.",
							ImageToast.TOAST_IMAGE_CANCEL, this);
					t.show();
					return;
				}

				try {
					if (vault != null) saveVaultToDB();
				} catch (Exception e) {
					e.printStackTrace();
					showErrorDialog(e);
				}

				Dialog setVaultNameDialog = ComponentProvider.getVaultSetNameDialog(this, am.getMainMessage().equals("pwv_moreDialog_exportVaultXML") ? vault : null);
				setVaultNameDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
				setVaultNameDialog.show();
			} else if (am.getMainMessage().equals("pwv_moreDialog_resetVault")) {
				AlertDialog cad = ComponentProvider.getCriticalQuestionDialog(this,
						getResources().getString(R.string.pwv_moreDialog_resetVault),
						getResources().getString(
								R.string.pwv_moreDialog_resetVault_ResetCriticalQuestion),
						null,
						PWV_MESSAGE_MOREDIALOG_RESET_CONFIRM);
				cad.show();
				cad.getButton(Dialog.BUTTON_NEGATIVE).setEnabled(false);
			} else if (am.getMainMessage().equals("pwv_moreDialog_enterPassword")) {
				getStartScreenPasswordDialog().show();
			}
			this.resetMessage();
			break;

		case PWV_MESSAGE_MOREDIALOG_IMPORT:
			String fileName = (String) am.getMainMessage();
			String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
			int importAction = ext.equalsIgnoreCase(PWV_EXPORT_EXT) ? PWV_MESSAGE_MOREDIALOG_IMPORT_CONFIRM : PWV_MESSAGE_MOREDIALOG_IMPORT_CONFIRM_XML;

			if (dbHelper.getBlobData(PWV_DBPREFIX) != null) {
				AlertDialog cad = ComponentProvider.getCriticalQuestionDialog(this,
						getResources().getString(R.string.common_importVault_text),
						getResources().getString(R.string.pwv_moreDialog_importVault_ImportCriticalQuestion)
								.replaceAll("<1>", Matcher.quoteReplacement(fileName)), fileName, importAction);
				cad.show();
				cad.getButton(Dialog.BUTTON_NEGATIVE).setEnabled(false);
			} else {
				Dialog cad = ComponentProvider.getBaseQuestionDialog(this,
						getResources().getString(R.string.common_importVault_text),
						getResources().getString(R.string.pwv_moreDialog_importVault_ImportQuestion)
								.replaceAll("<1>", Matcher.quoteReplacement(fileName)), fileName, importAction);
				cad.show();
			}
			this.resetMessage();
			break;

		case PWV_MESSAGE_MOREDIALOG_MERGE:
			ComponentProvider.getBaseQuestionDialog(this,
					getResources().getString(R.string.pwv_mergeVaults),
					getResources().getString(R.string.pwv_mergeVaultsInfo),
					(String) am.getMainMessage(),
					PWV_MESSAGE_MOREDIALOG_MERGE_CONFIRM,
					null,
					true).show();
			;

			this.resetMessage();
			break;


		case PWV_MESSAGE_MOREDIALOG_IMPORT_CONFIRM:
			if (am.getAttachement().equals(new Integer(1))) {
				importOrMergePWV(am.getMainMessage(), false);
			}
			this.resetMessage();
			break;

		case PWV_MESSAGE_MOREDIALOG_IMPORT_CONFIRM_XML:
			if (am.getAttachement().equals(new Integer(1))) {
				importOrMergeXML(am.getMainMessage(), false);
			}
			this.resetMessage();
			break;

		case PWV_MESSAGE_MOREDIALOG_MERGE_CONFIRM:
			if (am.getAttachement().equals(new Integer(1))) {
				String mergeFileName = am.getMainMessage();

				if (mergeFileName.toLowerCase().endsWith(PWV_EXPORT_EXT)) {
					importOrMergePWV(am.getMainMessage(), true);
				} else {
					importOrMergeXML(am.getMainMessage(), true);
				}
			}
			this.resetMessage();
			break;

		case PWV_MESSAGE_MOREDIALOG_RESET_CONFIRM:
			if (am.getAttachement().equals(new Integer(1))) {
				dbHelper.deleteBlobData(PWV_DBPREFIX);

				setResult(RESTART_PASSWORDVAULTACTIVITY);
				this.finish();
			}
			this.resetMessage();
			break;

		case PWV_MESSAGE_PWGDIALOG_SET:
			if (itemPasswordEditText.getText().toString().trim().equals("")) {
				itemPasswordEditText.setText("");
				char[] tempPw = (char[]) am.getAttachement2();
				itemPasswordEditText.append(CharBuffer.wrap(tempPw));
				Arrays.fill(tempPw, '\u0000');
			} else {
				ComponentProvider.getBaseQuestionDialog(
						this,
						this.getResources().getString(R.string.passwordGeneratorDialog_passwordGenerator_text),
						this.getResources().getString(R.string.passwordGeneratorDialog_replacePasswordQuestion),
						am.getMainMessage(),
						PWV_MESSAGE_PWGDIALOG_SET_CONFIRM,
						am.getAttachement2()).show();
			}

			this.resetMessage();
			break;

		case PWV_MESSAGE_PWGDIALOG_SET_CONFIRM:
			if (am.getAttachement().equals(new Integer(1))) {
				itemPasswordEditText.setText("");
				char[] tempPw = (char[]) am.getAttachement2();
				itemPasswordEditText.append(CharBuffer.wrap(tempPw));
				Arrays.fill(tempPw, '\u0000');
			}
			this.resetMessage();
			break;

		case PWV_MESSAGE_ITEM_NOTE_COPY: {
			setToSystemClipboard((String) am.getAttachement());
			new ImageToast(getResources().getString(R.string.common_textCopiedToSystemClipboard), ImageToast.TOAST_IMAGE_OK, this).show();
		}
		this.resetMessage();
		break;

		case PWV_MESSAGE_ITEM_CREATE: {
			String ica = (String) am.getAttachement();
			int position = (int) am.getAttachement2();
			if (ica.equals("B")) createNewItem(position, false, false);
			else if (ica.equals("E")) createNewItem(position, true, false);
			else if (ica.equals("K")) createNewItem(position, false, true);
		}
		this.resetMessage();
		break;

		case COMMON_MESSAGE_CONFIRM_EXIT:
			if (am.getAttachement() == null || am.getAttachement().equals(new Integer(1))) {
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
 * Create "Login to Password Vault Layout"
 */
private void initLayoutStart() {
	PasswordDialog startDialog = getStartScreenPasswordDialog();
	startDialog.show();

	toMainPageButton = (Button) findViewById(R.id.PWVS_toMainPage);
	helpMeButton = (Button) findViewById(R.id.PWVS_helpMe);
	layoutStartButtons = (LinearLayout) findViewById(R.id.PWVS_buttons);

	this.toMainPageButton.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			if (detectDoubleClick()) return;

			finish();
		}
	});

	// Help me get in! button
	this.helpMeButton.setOnClickListener(new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (detectDoubleClick()) return;

			String commentPrefix = android.os.Build.VERSION.SDK_INT > 27 ? "\uD83D\uDCC2 " : "(dir: ";
			String commentPostfix = android.os.Build.VERSION.SDK_INT > 27 ? "" : ")";
			String dirComment = commentPrefix + Helpers.storEmulShorten(Helpers.getImportExportPath(settingDataHolder)) + commentPostfix;
			boolean existsVault = dbHelper.getBlobData(PWV_DBPREFIX) != null;
			List<String> itemList = new ArrayList<String>();
			List<String> commentsList = new ArrayList<String>();
			List<Object> keyList = new ArrayList<Object>();
			itemList.add(getResources().getString(R.string.pwv_moreDialog_enterPassword));
			commentsList.add(null);
			keyList.add("pwv_moreDialog_enterPassword");
			if (existsVault) {
				itemList.add(getResources().getString(R.string.pwv_moreDialog_resetVault));
				commentsList.add(null);
				keyList.add("pwv_moreDialog_resetVault");
			}
			itemList.add(getResources().getString(R.string.pwv_moreDialog_importVault));
			commentsList.add(dirComment);
			keyList.add("pwv_moreDialog_importVault");
			if (existsVault) {
				itemList.add(getResources().getString(R.string.pwv_moreDialog_exportVault));
				commentsList.add(dirComment);
				keyList.add("pwv_moreDialog_exportVault");
			}

			moreDialog = new SelectionDialog(v,
					itemList,
					commentsList,
					null,
					keyList,
					getResources().getString(R.string.pwv_start_helpMe));
			moreDialog.setMessageCode(PWV_MESSAGE_MOREDIALOG);

			if (moreDialog != null) moreDialog.show();
		}
	});
}


/**
 * Create Password Folders Layout
 */
private void initLayoutFolders() {
	iconAdapter = new PWVFolderAdapter(this, vault);
	foldersBottomMenu = (LinearLayout) findViewById(R.id.PWVL_Folders_buttons);
	showMenuButton = (Button) findViewById(R.id.PWVL_Folders_showMenuButton);
	if (showBottomMenu) showBottomMenu();
	foldersMoreButton = (Button) findViewById(R.id.PWVL_Folders_moreButton);
	foldersSearchButton = (Button) findViewById(R.id.PWVL_Folders_search);
	foldersNewFolderButton = (Button) findViewById(R.id.PWVL_Folders_newFolderButton);
	foldersHelpButton = (Button) findViewById(R.id.PWVL_helpButton);
	GridView gridview = (GridView) findViewById(R.id.PWVL_Folders_gridview);
	gridview.setAdapter(iconAdapter);


	gridview.setOnItemClickListener(new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
			if (position < vault.getFolderCount()) {
				currentFolder = vault.getFolderByIndex(position);
			} else if (iconAdapter.hasSearchResultFolder()) {
				currentFolder = iconAdapter.getSearchResultFolder();
			}
			resetItemsList();
			layoutSwitcher.showNext();
		}
	});

	gridview.setOnItemLongClickListener(new OnItemLongClickListener() {
		public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
			if (position >= vault.getFolderCount()) return true;
			TextView tw = (TextView) v.findViewById(R.id.iconTextPW);
			PWVNewEditFolderDialog nefd = new PWVNewEditFolderDialog(
					v,
					vault,
					position,
					PWVNewEditFolderDialog.PWVFD_MODE_SHOW_FOLDER);
			nefd.setOriginalHash((String) tw.getTag());
			nefd.show();

			return true;
		}
	});

	this.foldersMoreButton.setOnClickListener(new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (detectDoubleClick()) return;

			String commentPrefix = android.os.Build.VERSION.SDK_INT > 27 ? "\uD83D\uDCC2 " : "(dir: ";
			String commentPostfix = android.os.Build.VERSION.SDK_INT > 27 ? "" : ")";
			String dirComment = commentPrefix + Helpers.storEmulShorten(Helpers.getImportExportPath(settingDataHolder)) + commentPostfix;
			List<String> itemList = new ArrayList<String>();
			List<Object> keyList = new ArrayList<Object>();
			List<String> commentsList = new ArrayList<String>();
			List<Integer> icon = new ArrayList<Integer>();
				/*
				itemList.add(getResources().getString(R.string.common_search_Text));
				commentsList.add(null);
				keyList.add("pwv_moreDialog_search");
				icon.add(R.drawable.item_search);
				*/
			itemList.add(getResources().getString(R.string.pwv_moreDialog_changePassword));
			commentsList.add(null);
			keyList.add("pwv_moreDialog_changePassword");
			icon.add(null);
			itemList.add(getResources().getString(R.string.pwv_moreDialog_importVault));
			commentsList.add(dirComment);
			keyList.add("pwv_moreDialog_importVault");
			icon.add(null);
			itemList.add(getResources().getString(R.string.pwv_mergeVaults));
			commentsList.add(dirComment);
			keyList.add("pwv_moreDialog_mergeVaults");
			icon.add(null);
			itemList.add(getResources().getString(R.string.pwv_moreDialog_exportVault));
			commentsList.add(dirComment);
			keyList.add("pwv_moreDialog_exportVault");
			icon.add(null);
			itemList.add(getResources().getString(R.string.pwv_moreDialog_exportVaultXML));
			commentsList.add(dirComment);
			keyList.add("pwv_moreDialog_exportVaultXML");
			icon.add(null);

			moreDialog = new SelectionDialog(v,
					itemList,
					commentsList,
					icon,
					keyList,
					getResources().getString(R.string.me_moreDialog_Title));
			moreDialog.setMessageCode(PWV_MESSAGE_MOREDIALOG);

			if (moreDialog != null) moreDialog.show();
		}
	});

	this.foldersNewFolderButton.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			if (detectDoubleClick()) return;

			if (isButtonsLockActivated()) return;
			activateButtonsLock();

			PWVNewEditFolderDialog nefd = new PWVNewEditFolderDialog(v, vault, null, PWVNewEditFolderDialog.PWVFD_MODE_NEW_FOLDER);
			nefd.setTitle(getResources().getString(R.string.pwv_newFolder_text));

			nefd.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialogInterface) {
					deactivateButtonsLock();
				}
			});

			nefd.show();
		}
	});

	this.foldersSearchButton.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			if (detectDoubleClick()) return;

			getSearchDialog().show();
		}
	});

	this.foldersHelpButton.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			if (detectDoubleClick()) return;

			SimpleHTMLDialog simpleHTMLDialog = new SimpleHTMLDialog(v);
			simpleHTMLDialog.addValue("API_VERSION", Integer.toString(android.os.Build.VERSION.SDK_INT));
			simpleHTMLDialog.loadURL(getResources().getString(R.string.helpLink_PasswordVault));
			simpleHTMLDialog.show();
		}
	});

	this.showMenuButton.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			showBottomMenu();
			showBottomMenuItems();
		}
	});
}


/**
 * Create Password Items Layout
 */
private void initLayoutItems() {
	itemsListView = (ListView) findViewById(R.id.PWVL_Items_listView);
	itemsBottomMenu = (LinearLayout) findViewById(R.id.PWVL_Items_buttons);
	showMenuItemsButton = (Button) findViewById(R.id.PWVL_Items_showMenuButton);
	itemsNewItemButton = (Button) findViewById(R.id.PWVL_Items_newItemButton);
	itemsHelpButton = (Button) findViewById(R.id.PWVL_Items_helpButton);
	searchItemView = (SearchView) findViewById(R.id.PWVL_Items_searchView);
	Helpers.trimChildMargins(searchItemView);

	itemsArrayAdapter = (new PWVItemArrayAdapter(this, currentItems));
	try {
		fontSizeMultiplier = Integer.parseInt(settingDataHolder.getItemValueName("SC_PasswordVault", "SI_PasswordListFontSize").split("::")[0]) / 100.0F;
	} catch (Exception e) {
		e.printStackTrace();
	}
	((PWVItemArrayAdapter) itemsArrayAdapter).setFontSizeMultiplier(fontSizeMultiplier);
	itemsListView.setAdapter(itemsArrayAdapter);

	if (Build.VERSION.SDK_INT >= 12) {
		itemsQuickscroll = (QuickScroll) findViewById(R.id.PWVL_quickscroll);
		itemsQuickscroll.init(QuickScroll.TYPE_INDICATOR_WITH_HANDLE, itemsListView, itemsArrayAdapter, QuickScroll.STYLE_HOLO);
		itemsQuickscroll.setFixedSize(1);
		itemsQuickscroll.setMinAllVsVisibleRatio(5);
		itemsQuickscroll.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 42);
	}

	itemsListView.setOnItemClickListener(new OnItemClickListener() {
		// click on item
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			searchItemView.clearFocus();
			VaultItemV2 tvi = (VaultItemV2) itemsListView.getAdapter().getItem(position);

			if (tvi.isSpecial()) {
				if (tvi.getSpecialCode() == VaultItemV2.SPEC_GOBACKITEM) {
					layoutSwitcher.showPrevious();
					searchItemView.setQuery("", false);
				} else if (tvi.getSpecialCode() == VaultItemV2.SPEC_NEWITEM) {
					showCreateNewItemDialog(currentFolder.getItemCount());
				} else if (tvi.getSpecialCode() == VaultItemV2.SPEC_SEARCH) {
					layoutSwitcher.showPrevious();
					getSearchDialog().show();
				}
			} else {
				tvi.setSelected(!tvi.isSelected());
				itemsArrayAdapter.notifyDataSetChanged();
			}
		}
	});

	itemsListView.setOnItemLongClickListener(new OnItemLongClickListener() {
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			searchItemView.clearFocus();
			VaultItemV2 tvi = (VaultItemV2) itemsListView.getAdapter().getItem(position);
			String itemHash = (String) view.findViewById(R.id.PWVI_itemName).getTag(); // hash

			if (tvi.isSpecial()) {
				if (tvi.getSpecialCode() == VaultItemV2.SPEC_GOBACKITEM) {
					layoutSwitcher.showPrevious();
					searchItemView.setQuery("", false);
				} else if (tvi.getSpecialCode() == VaultItemV2.SPEC_NEWITEM) {
					showCreateNewItemDialog(currentFolder.getItemCount());
				} else if (tvi.getSpecialCode() == VaultItemV2.SPEC_SEARCH) {
					layoutSwitcher.showPrevious();
					searchItemView.setQuery("", false);
					getSearchDialog().show();
				}
			} else {
				currentItem = tvi;
				if (!itemHash.equals(currentItem.getItemSecurityHash())) return false;

				int realPosition = currentFolder.getIndexByReference(tvi);
				List tagMessage = new ArrayList();
				tagMessage.add(currentItem.getItemSecurityHash());
				tagMessage.add(realPosition);
				itemNameEditText.setTag(tagMessage); // hash + position
				prepareLayoutItemDetailForShow(currentItem.isExtendedItem(), currentItem.isKemItem());
				layoutSwitcher.showNext();
				solveViewAnimatorBug();
			}
			return true;
		}
	});

	this.itemsHelpButton.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			if (detectDoubleClick()) return;

			searchItemView.clearFocus();
			SimpleHTMLDialog simpleHTMLDialog = new SimpleHTMLDialog(v);
			simpleHTMLDialog.addValue("API_VERSION", Integer.toString(Build.VERSION.SDK_INT));
			simpleHTMLDialog.loadURL(getResources().getString(R.string.helpLink_PasswordVault));
			simpleHTMLDialog.show();
		}
	});

	this.itemsNewItemButton.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			if (detectDoubleClick()) return;

			searchItemView.clearFocus();
			showCreateNewItemDialog(currentFolder.getItemCount());
		}
	});

	this.showMenuItemsButton.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			showBottomMenu();
			showBottomMenuItems();
		}
	});

	searchItemView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
		@Override
		public boolean onQueryTextSubmit(String s) {
			return false;
		}

		@Override
		public boolean onQueryTextChange(String s) {
			itemsArrayAdapter.getFilter().filter(s);
			return false;
		}
	});
}


/**
 * Create Item Detail Layout
 */
private void initLayoutItemDetail() {
	setLayoutItemDetailOrientation();

	ked = new KeyboardAppearanceDetector(this);
	ked.setKeyboardVisibilityChangedListener(new KeyboardVisibilityChangedListener() {

		@Override
		public void onKeyboardVisibilityChanged(boolean isKeyboardVisible) {
			handleKeyboardAppear(isKeyboardVisible);
		}
	});
	if (!this.isTablet()) ked.startDetection();

	itemNameEditText = (SecureEditText) findViewById(R.id.PWVD_name);
	itemPasswordEditText = (SecureEditText) findViewById(R.id.PWVD_password);
	itemAccountEditText = (SecureEditText) findViewById(R.id.PWVD_account);
	itemUrlEditText = (SecureEditText) findViewById(R.id.PWVD_url);
	itemCommentEditText = (SecureEditText) findViewById(R.id.PWVD_comment);
	kemPrivateKey = (SecureEditText) findViewById(R.id.PWVD_kemPrivateKey);
	kemPublicKey = (SecureEditText) findViewById(R.id.PWVD_kemPublicKey);
	kemSharedSecret = (SecureEditText) findViewById(R.id.PWVD_kemSharedSecret);
	kemSharedSecretEncapsulated = (SecureEditText) findViewById(R.id.PWVD_kemSharedSecretEncapsulated);
	kemSharedSecretExtracted = (SecureEditText) findViewById(R.id.PWVD_kemSharedSecretExtracted);
	itemCommentTextView = (TextViewMod) findViewById(R.id.PWVD_comment_readonly);
	itemColorSpinner = (Spinner) findViewById(R.id.PWVD_colorCombo);
	kemAlgorithmSpinner = (Spinner) findViewById(R.id.PWVD_kemAlgorithmSpinner);
	itemDeleteButton = (TextView) findViewById(R.id.PWVD_buttonDelete);
	itemEditSaveButton = (TextView) findViewById(R.id.PWVD_buttonEditSave);
	itemMoveToButton = (TextView) findViewById(R.id.PWVD_buttonMoveTo);
	convertToExtendedButton = (Button) findViewById(R.id.PWVD_convertToExtendedButton);
	passwordGeneratorButton = (Button) findViewById(R.id.PWVD_passwordGeneratorButton);
	nameToClipboardButton = (Button) findViewById(R.id.PWVD_nameToClipboardButton);
	kemPrivateKeyToClipboardButton = (Button) findViewById(R.id.PWVD_kemPrivateKeyToClipboardButton);
	kemPublicKeyToClipboardButton = (Button) findViewById(R.id.PWVD_kemPublicKeyToClipboardButton);
	kemSharedSecretToClipboardButton = (Button) findViewById(R.id.PWVD_kemSharedSecretToClipboardButton);
	kemSharedSecretEncapsulatedToClipboardButton = (Button) findViewById(R.id.PWVD_kemSharedSecretEncapsulatedToClipboardButton);
	kemSharedSecretExtractedToClipboardButton = (Button) findViewById(R.id.PWVD_kemSharedSecretExtractedToClipboardButton);
	kemPrivateKeyFromClipboardButton = (Button) findViewById(R.id.PWVD_kemPrivateKeyFromClipboardButton);
	kemPublicKeyFromClipboardButton = (Button) findViewById(R.id.PWVD_kemPublicKeyFromClipboardButton);
	kemSharedSecretFromClipboardButton = (Button) findViewById(R.id.PWVD_kemSharedSecretFromClipboardButton);
	kemSharedSecretEncapsulatedFromClipboardButton = (Button) findViewById(R.id.PWVD_kemSharedSecretEncapsulatedFromClipboardButton);
	kemSharedSecretExtractedFromClipboardButton = (Button) findViewById(R.id.PWVD_kemSharedSecretExtractedFromClipboardButton);
	kemPrivateKeyCleanButton = (Button) findViewById(R.id.PWVD_kemPrivateKeyCleanButton);
	kemPublicKeyCleanButton = (Button) findViewById(R.id.PWVD_kemPublicKeyCleanButton);
	kemSharedSecretCleanButton = (Button) findViewById(R.id.PWVD_kemSharedSecretCleanButton);
	kemSharedSecretEncapsulatedCleanButton = (Button) findViewById(R.id.PWVD_kemSharedSecretEncapsulatedCleanButton);
	kemSharedSecretExtractedCleanButton = (Button) findViewById(R.id.PWVD_kemSharedSecretExtractedCleanButton);
	passwordToClipboardButton = (Button) findViewById(R.id.PWVD_passwordToClipboardButton);
	passwordShowHideButton = (Button) findViewById(R.id.PWVD_passwordShowHide);
	kemPrivateKeyShowHideButton = (Button) findViewById(R.id.PWVD_kemPrivateKeyShowHide);
	kemSecretShowHideButton = (Button) findViewById(R.id.PWVD_kemSharedSecretShowHide);
	kemSecretExtractedShowHideButton = (Button) findViewById(R.id.PWVD_kemSharedSecretExtractedShowHide);
	accountToClipboardButton = (Button) findViewById(R.id.PWVD_accountToClipboardButton);
	urlToClipboardButton = (Button) findViewById(R.id.PWVD_urlToClipboardButton);
	openUrlButton = (Button) findViewById(R.id.PWVD_openUrlButton);
	passwordToTextEncryptorButton = (Button) findViewById(R.id.PWVD_passwordToTextEncryptorButton);
	passwordToFileEncryptorButton = (Button) findViewById(R.id.PWVD_passwordToFileEncryptorButton);
	noteToClipboardButton = (Button) findViewById(R.id.PWVD_noteToClipboardButton);
	addNewCustomElementButton = (Button) findViewById(R.id.PWVD_addNewElementButton);
	kemGenerateNewKeyPairButton = (TextView) findViewById(R.id.PWVD_kemGenerateNewKeyPairButton);
	kemGenerateNewSecretButton = (TextView) findViewById(R.id.PWVD_kemGenerateNewSecretButton);
	kemExtractSharedSecretButton = (TextView) findViewById(R.id.PWVD_kemExtractSharedSecretButton);
	noteCharCounter = (TextView) findViewById(R.id.PWVD_noteCharCounter);
	switchTopBottomButton = (Button) findViewById(R.id.PWVD_switchTopBottomButton);
	topLeftContainer = findViewById(R.id.PWVD_mainTopLeft);
	bottomRightContainer = findViewById(R.id.PWVD_mainBottomRight);
	commentWrapContainer = findViewById(R.id.PWVD_commentWrap);
	commentExtendedContainer = findViewById(R.id.PWVD_commentExtendedContainer);
	convertToExtendedContainer = (ViewGroup) findViewById(R.id.PWVD_convertToExtendedWrapper);
	kemPrivateKeyEditOnlyContainer = (ViewGroup) findViewById(R.id.PWVD_kemPrivateKeyEditOnlyContainer);
	kemPublicKeyEditOnlyContainer = (ViewGroup) findViewById(R.id.PWVD_kemPublicKeyEditOnlyContainer);
	kemSharedSecretEditOnlyContainer = (ViewGroup) findViewById(R.id.PWVD_kemSharedSecretEditOnlyContainer);
	kemSharedSecretEncapsulatedEditOnlyContainer = (ViewGroup) findViewById(R.id.PWVD_kemSharedSecretEncapsulatedEditOnlyContainer);
	kemSharedSecretExtractedEditOnlyContainer = (ViewGroup) findViewById(R.id.PWVD_kemSharedSecretExtractedEditOnlyContainer);
	bottomButtonLine = findViewById(R.id.PWVD_buttonLine);
	bottomDelimiter = findViewById(R.id.PWVD_bottomDelimiter);
	passwordContainer = (View) findViewById(R.id.PWVD_passwordContainer);
	accountContainer = (View) findViewById(R.id.PWVD_accountContainer);
	kemContainer = (View) findViewById(R.id.PWVD_kemContainer);
	urlContainer = (View) findViewById(R.id.PWVD_urlContainer);
	customElementsView = (ListView) findViewById(R.id.PWVD_customElements_LV);

	itemNameEditText.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) (itemNameEditText.getTextSize() * fontSizeMultiplier));
	itemPasswordEditText.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) (itemPasswordEditText.getTextSize() * fontSizeMultiplier));
	itemAccountEditText.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) (itemAccountEditText.getTextSize() * fontSizeMultiplier));
	itemUrlEditText.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) (itemUrlEditText.getTextSize() * fontSizeMultiplier));
	itemCommentEditText.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) (itemCommentEditText.getTextSize() * fontSizeMultiplier));
	itemCommentTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) (itemCommentTextView.getTextSize() * fontSizeMultiplier));

	itemNameEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
	itemPasswordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
	kemPrivateKey.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
	kemPublicKey.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
	kemSharedSecret.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
	kemSharedSecretEncapsulated.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
	kemSharedSecretExtracted.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
	itemEditSaveButton.setTag("edit");
	itemColorSpinner.setAdapter(itemColorSpinnerAdapter);
	kemAlgorithmSpinner.setAdapter(kemAlgorithmSpinnerAdapter);

	makeEditableEditText(itemCommentEditText);

	this.itemEditSaveButton.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			String mode = (String) v.getTag();
			handleItemSave(mode);
		}
	});

	this.itemDeleteButton.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			if (detectDoubleClick()) return;

			if (isButtonsLockActivated()) return;
			activateButtonsLock();

			List tagMessage = (List) itemNameEditText.getTag();

			Dialog deleteDialog = ComponentProvider.getBaseQuestionDialog(v, getResources().getString(R.string.common_delete_text) + " " + getResources().getString(R.string.common_item_text),
					getResources().getString(R.string.common_question_delete)
							.replaceAll("<1>", Matcher.quoteReplacement(String.valueOf(currentFolder.getItemByIndex((Integer) tagMessage.get(1)).getItemName()))),
					(Integer) tagMessage.get(1) + ":" + (String) tagMessage.get(0), PWV_MESSAGE_ITEM_DELETE_CONFIRM);

			deleteDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialogInterface) {
					deactivateButtonsLock();
				}
			});

			deleteDialog.show();
		}
	});

	this.itemMoveToButton.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			if (detectDoubleClick()) return;

			if (isButtonsLockActivated()) return;
			activateButtonsLock();

			List tagMessage = (List) itemNameEditText.getTag();
			List<String> itemList = new ArrayList<String>();
			List<Integer> iconList = new ArrayList<Integer>();
			List<Object> keyList = new ArrayList<Object>();

			for (int i = 0; i < vault.getFolderCount(); ++i) {
				VaultFolderV2 tempFolder = vault.getFolderByIndex(i);
				if (currentFolder.isSearchResult() && currentItem.getParentFolder() == tempFolder)
					continue;
				if (tempFolder == currentFolder) continue;
				itemList.add(String.valueOf(tempFolder.getFolderName()));
				keyList.add(Integer.toString(i) + ":" +
						tempFolder.getFolderSecurityHash() + ":" +
						(Integer) tagMessage.get(1) + ":" +
						(String) tagMessage.get(0));
				iconList.add(ColorHelper.getColorBean(tempFolder.getColorCode()).folderIconRId);
			}

			SelectionDialog moveToFolderDialog = new SelectionDialog(
					v,
					itemList,
					null,
					iconList,
					keyList,
					getResources().getString(R.string.common_moveToFolder_text));
			moveToFolderDialog.setMessageCode(PWV_MESSAGE_ITEM_MOVETOFOLDER);
			moveToFolderDialog.show();

			moveToFolderDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialogInterface) {
					deactivateButtonsLock();
				}
			});

			moveToFolderDialog.show();
		}
	});

	this.kemGenerateNewKeyPairButton.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			if (detectDoubleClick()) return;

			kemGenerateNewKeyPair(KEMCipherProvider.getAlgorithmNameByIndex(kemAlgorithmSpinner.getSelectedItemPosition()));
		}
	});

	this.kemGenerateNewSecretButton.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			if (detectDoubleClick()) return;

			kemGenerateNewSecret(KEMCipherProvider.getAlgorithmNameByIndex(kemAlgorithmSpinner.getSelectedItemPosition()), kemPublicKey.toCharArray());
		}
	});

	this.kemExtractSharedSecretButton.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			if (detectDoubleClick()) return;

			kemExtractSharedSecret(KEMCipherProvider.getAlgorithmNameByIndex(kemAlgorithmSpinner.getSelectedItemPosition()),
					kemPrivateKey.toCharArray(), kemSharedSecretEncapsulated.toCharArray());
		}
	});

	this.convertToExtendedButton.setTag("");
	this.convertToExtendedButton.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			Handler dialogHandler = new Handler() {
				public void handleMessage(Message msg) {
					if (msg.what == 1) {
						convertToExtendedContainer.setVisibility(ViewGroup.GONE);
						addNewCustomElementButton.setVisibility(View.VISIBLE);
						switchTopBottomButton.setVisibility(View.GONE);
						prepareLayoutItemDetailForShow(true, false, true);
					}
				}
			};
			ComponentProvider.getBaseQuestionDialog(
					v.getContext(),
					getResources().getString(R.string.common_continue_text),
					getResources().getString(R.string.pwv_newItemConversionQuestion)
							.replaceAll("<1>", Matcher.quoteReplacement(getResources().getString(R.string.pwv_newItemBasic)))
							.replaceAll("<2>", Matcher.quoteReplacement(getResources().getString(R.string.pwv_newItemExtended))),
					dialogHandler,
					false).show();
		}
	});

	this.passwordGeneratorButton.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			if (detectDoubleClick()) return;

			new PasswordGeneratorDialog(v, PWV_MESSAGE_PWGDIALOG_SET).show();
		}
	});

	this.openUrlButton.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			try {
				String url = itemUrlEditText.getText().toString().trim();
				if (url.length() > 0 && !URLUtil.isValidUrl(url))
					url = "https://" + url;

				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				startActivity(browserIntent);
			} catch (Exception e) {
				ComponentProvider.getImageToast(v.getResources().getString(R.string.fe_cannotPerformThisAction),
						ImageToast.TOAST_IMAGE_CANCEL, v).show();
			}
		}
	});

	this.passwordToClipboardButton.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			char[] text = itemPasswordEditText.getPassword();
			text = Helpers.trim(text);

			if (text.length == 0) {
				ComponentProvider.getImageToastKO(getResources().getString(R.string.common_noTextToCopy), v).show();
				return;
			}

			setToSystemClipboard(CharBuffer.wrap(text));

			ComponentProvider.getShowMessageDialog(v,
					getResources().getString(R.string.common_copyToClipboard_text),
					getResources().getString(R.string.common_passwordCopiedToClipboard_text) + "<br/><br/>" + getResources().getString(R.string.common_copyToClipboardWarning),
					ComponentProvider.DRAWABLE_ICON_INFO_BLUE).show();
		}
	});

	this.passwordToTextEncryptorButton.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			if (detectDoubleClick()) return;

			char[] text = itemPasswordEditText.getPassword();
			text = Helpers.trim(text);

			if (text.length == 0) {
				ComponentProvider.getImageToastKO(getResources().getString(R.string.common_noTextToCopy), v).show();
				return;
			}

			Intent intent = new Intent(PasswordVaultActivity.this, MessageEncActivity.class);
			intent.putExtra(PasswordVaultActivity.PWV_EXTRA_PASSWORD, CharBuffer.wrap(text));
			intent.putExtra(PasswordVaultActivity.PWV_EXTRA_LOCKSCREEN_KEY, encryptor.getKeyHash());
			intent.putExtra(PasswordVaultActivity.PWV_EXTRA_LOCKSCREEN_ON, lockOnPause);
			startActivityForResult(intent, REQUEST_CODE_SEND_PASSWORD);
		}
	});

	this.passwordToFileEncryptorButton.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			if (detectDoubleClick()) return;

			char[] text = itemPasswordEditText.getPassword();
			text = Helpers.trim(text);

			if (text.length == 0) {
				ComponentProvider.getImageToastKO(getResources().getString(R.string.common_noTextToCopy), v).show();
				return;
			}

			Intent intent = new Intent(PasswordVaultActivity.this, FileEncActivity.class);
			intent.putExtra(PasswordVaultActivity.PWV_EXTRA_PASSWORD, CharBuffer.wrap(text));
			intent.putExtra(PasswordVaultActivity.PWV_EXTRA_LOCKSCREEN_KEY, encryptor.getKeyHash());
			intent.putExtra(PasswordVaultActivity.PWV_EXTRA_LOCKSCREEN_ON, lockOnPause);
			startActivityForResult(intent, REQUEST_CODE_SEND_PASSWORD);
		}
	});

	this.noteToClipboardButton.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			String text = itemCommentEditText.getText().toString().trim();
			handleCommentCopy(text);
		}
	});

	this.switchTopBottomButton.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			if (topLeftContainer.getVisibility() == View.VISIBLE) {
				bottomRightContainer.setVisibility(View.VISIBLE);
				topLeftContainer.setVisibility(View.GONE);
			} else {
				if (ked.isOpen()) bottomRightContainer.setVisibility(View.GONE);
				else bottomRightContainer.setVisibility(View.VISIBLE);
				topLeftContainer.setVisibility(View.VISIBLE);
			}
			moveCursorToEnd();
		}
	});

	this.addNewCustomElementButton.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			StringSentinel[] tempSS = new StringSentinel[2];
			tempSS[0] = new StringSentinel(VaultV2.SS_GROUP_ID);
			tempSS[1] = new StringSentinel(VaultV2.SS_GROUP_ID);
			customElementsAdapter.getDataSet().add(tempSS);
			customElementsAdapter.setMode(CustomElementsAdapter.MODE_EDIT);
			customElementsAdapter.notifyDataSetChanged();
		}
	});

	// Comment Character Counter
	itemCommentEditText.addTextChangedListener(new TextWatcher() {
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		public void afterTextChanged(Editable s) {
			noteCharCounter.setText(" (" + Integer.toString(itemCommentEditText.getText().length()) + "/" + Integer.toString(VaultV2.COMMENT_MAXCHARS) + ")");
		}
	});

	installShowHideButtonListener(passwordShowHideButton, itemPasswordEditText);
	installShowHideButtonListener(kemPrivateKeyShowHideButton, kemPrivateKey);
	installShowHideButtonListener(kemSecretShowHideButton, kemSharedSecret);
	installShowHideButtonListener(kemSecretExtractedShowHideButton, kemSharedSecretExtracted);

	installCopyToClipboardButtonListener(nameToClipboardButton, itemNameEditText);
	installCopyToClipboardButtonListener(accountToClipboardButton, itemAccountEditText);
	installCopyToClipboardButtonListener(urlToClipboardButton, itemUrlEditText);
	installCopyToClipboardButtonListener(kemPrivateKeyToClipboardButton, kemPrivateKey);
	installCopyToClipboardButtonListener(kemPublicKeyToClipboardButton, kemPublicKey);
	installCopyToClipboardButtonListener(kemSharedSecretToClipboardButton, kemSharedSecret);
	installCopyToClipboardButtonListener(kemSharedSecretEncapsulatedToClipboardButton, kemSharedSecretEncapsulated);
	installCopyToClipboardButtonListener(kemSharedSecretExtractedToClipboardButton, kemSharedSecretExtracted);

	installPasteFromClipboardButtonListener(kemPrivateKeyFromClipboardButton, kemPrivateKey);
	installPasteFromClipboardButtonListener(kemPublicKeyFromClipboardButton, kemPublicKey);
	installPasteFromClipboardButtonListener(kemSharedSecretFromClipboardButton, kemSharedSecret);
	installPasteFromClipboardButtonListener(kemSharedSecretEncapsulatedFromClipboardButton, kemSharedSecretEncapsulated);
	installPasteFromClipboardButtonListener(kemSharedSecretExtractedFromClipboardButton, kemSharedSecretExtracted);

	installCleanButtonListener(kemPrivateKeyCleanButton, kemPrivateKey);
	installCleanButtonListener(kemPublicKeyCleanButton, kemPublicKey);
	installCleanButtonListener(kemSharedSecretCleanButton, kemSharedSecret);
	installCleanButtonListener(kemSharedSecretEncapsulatedCleanButton, kemSharedSecretEncapsulated);
	installCleanButtonListener(kemSharedSecretExtractedCleanButton, kemSharedSecretExtracted);

	makeLayoutItemDetailReadOnly();
}

private void installCleanButtonListener(Button button, EditText editText)
{
	button.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			editText.setText("");
		}
	});
}

private void installCopyToClipboardButtonListener(Button button, EditText editText)
{
	button.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			handleBasicCopy(editText.getText().toString().trim());
		}
	});
}

private void installPasteFromClipboardButtonListener(Button button, EditText editText)
{
	button.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			ClipboardManager clipMan = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			String text = null;
			try {
				text = clipMan.getText().toString().trim();
			} catch (Exception e) {
				// Empty clipboard (Android 3.0+)
			}
			if (text == null) text = "";
			editText.setText(text);
		}
	});
}

private void installShowHideButtonListener(Button button, EditText editText)
{
	button.setOnClickListener(new OnClickListener() {
		@Override
		public synchronized void onClick(View v) {
			handleShowHide(button, editText, null);
		}
	});
}

/**
 * Serialize, Compress and Encrypt given Vault Object
 */
private byte[] serializeVault(VaultV2 passwordVault, int version) throws Exception {
	byte[] serializedVault;
	String crcZipCompress = "";

	List<String> crc = new ArrayList<String>();
	byte[] output = null;

	serializedVault = Encryptor.compressObjectLZMA(passwordVault);
	Encryptor.decompressObjectLZMA(serializedVault); // compression verification
	output = encryptor.encryptEAXWithAlgCodeAndKdf(serializedVault);
	Arrays.fill(serializedVault, (byte) 0);

	return output;
}


/**
 * Decrypt, Decompress and Deserialize given serialized Vault Object
 */
private Object deserializeVault(byte[] serializedVault, int version, Encryptor customEncryptor) throws Exception {
	Object unzipedVault = null;
	;

	if (version < 2) {
		String crcZipDecompress = "";
		String crcZipFromFile = "";

		List<String> crcf = new ArrayList<String>();
		byte[] decrypted = (customEncryptor == null) ? encryptor.decryptWithCRC(serializedVault, crcf) : customEncryptor.decryptWithCRC(serializedVault, crcf);
		crcZipFromFile = crcf.get(0);

		List<String> crcd = new ArrayList<String>();
		unzipedVault = Encryptor.unzipObject(decrypted, crcd);
		crcZipDecompress = crcd.get(0);

		if (!crcZipFromFile.equals(crcZipDecompress))
			((Vault) unzipedVault).setIntegrityCheckFailed(true);
	} else if (version == 2) {
		byte[] decrypted = (customEncryptor == null) ? encryptor.decryptEAXWithAlgCode(serializedVault) : customEncryptor.decryptEAXWithAlgCode(serializedVault);

		try {
			unzipedVault = Encryptor.decompressObjectLZMA(decrypted);
		} catch (Exception e) {
			throw new DataFormatException(e.getLocalizedMessage());
		}
	} else if (version == 3) {
		byte[] decrypted = (customEncryptor == null) ? encryptor.decryptEAXWithAlgCodeAndKdfV3(serializedVault) : customEncryptor.decryptEAXWithAlgCodeAndKdfV3(serializedVault);

		try {
			unzipedVault = Encryptor.decompressObjectLZMA(decrypted);
		} catch (Exception e) {
			throw new DataFormatException(e.getLocalizedMessage());
		}
	} else if (version == 4) {
		byte[] decrypted = (customEncryptor == null) ? encryptor.decryptEAXWithAlgCodeAndKdfV4(serializedVault) : customEncryptor.decryptEAXWithAlgCodeAndKdfV4(serializedVault);

		try {
			unzipedVault = Encryptor.decompressObjectLZMA(decrypted);
		} catch (Exception e) {
			throw new DataFormatException(e.getLocalizedMessage());
		}
		Arrays.fill(decrypted, (byte) 0);
	} else {
		throw new DataFormatException(getResources().getString(R.string.common_invalid_format_version).replaceAll("<1>", "" + version));
	}

	return unzipedVault;
}


/**
 * Load Vault Object from Application Database
 */
private synchronized VaultV2 loadVaultfromDB(int[] outputFlags) throws Exception {
	byte[] dbVault;
	StringBuffer dbhs = new StringBuffer();

	ArrayList otherData = new ArrayList();
	dbVault = dbHelper.getBlobData(PWV_DBPREFIX, dbhs, otherData);
	if (dbVault == null) return null;

	int pwvVersion = ((Integer) otherData.get(0)).intValue();

	Object desVault = deserializeVault(dbVault, pwvVersion, null);
	try {
		if (!(desVault instanceof VaultV2)) {
			exportVaultToFile("PWV-MigrationToV4_" + Helpers.getFormatedDateCustom(System.currentTimeMillis(), "yyyy-MM-dd—HH-mm-ss"), null, false, true, true);
			outputFlags[0] = 1;
		}
	} catch (Exception e) {
		e.printStackTrace();
	}
	VaultV2 tempVault = convertToLatestVaultVersion(desVault);
	tempVault.setStampHashFromDB(dbhs.toString());
	tempVault.notifyFolderDataSetChanged();

	return tempVault;
}


/**
 * Search Items
 */
private synchronized void search(char[] searchFor, int searchIn) {
	waitDialog = new SimpleWaitDialog(this);
	waitDialog.show();
	new Thread(new Runnable() {
		public void run() {
			PowerManager.WakeLock wakeLock;
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SSE:PWV_SEARCH");

			wakeLock.acquire();

			//+ Process
			try {
				VaultFolderV2 searchResults = new VaultFolderV2();

				for (int i = 0; i < vault.getFolderCount(); ++i) {
					VaultFolderV2 searchedFolder = vault.getFolderByIndex(i);
					for (int j = 0; j < searchedFolder.getItemCount(); ++j) {
						VaultItemV2 searchedItem = searchedFolder.getItemByIndex(j);
						int searchIndex = searchedItem.searchIndexOf(searchFor, searchIn);

						if (searchIndex > -1) {
							searchedItem.setParentFolder(searchedFolder);
							searchResults.addItem(searchedItem);
						}
					}
				}

				if (searchResults.getItemCount() > 0) {
					iconAdapter.setSearchResultFolder(searchResults);
					searchResults.setFolderName(getResources().getString(R.string.pwv_search_latestSearchResults).toCharArray());
					searchResults.setIsSearchResult(true);
					universalHandler.sendMessage(Message.obtain(universalHandler, PWV_UNIVERSALHANDLER_SEARCH_FINISH));
				} else {
					universalHandler.sendMessage(Message.obtain(universalHandler, PWV_UNIVERSALHANDLER_SHOW_ERRORDIALOG,
							getResources().getString(R.string.pwv_search_noResults)));
				}

			} catch (Exception e) {
				e.printStackTrace();
				universalHandler.sendMessage(Message.obtain(universalHandler, PWV_UNIVERSALHANDLER_SHOW_ERRORDIALOG,
						Helpers.getShortenedStackTrace(e, 1)));
			}
			//- Process

			universalHandler.sendMessage(Message.obtain(universalHandler, PWV_UNIVERSALHANDLER_HIDE_WAITDIALOG));
			wakeLock.release();
		}
	}).start();
}

/**
 * Save Vault Object to Application Database
 */
private synchronized void saveVaultToDBAsync() throws Exception {
	waitDialog = new SimpleWaitDialog(this);
	waitDialog.show();
	new Thread(new Runnable() {
		public void run() {
			PowerManager.WakeLock wakeLock;
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SSE:PWV_SAVE_VAULT");

			wakeLock.acquire();

			//+ Process
			try {
				saveVaultToDB();
			} catch (Exception e) {
				e.printStackTrace();
				universalHandler.sendMessage(Message.obtain(universalHandler, PWV_UNIVERSALHANDLER_SHOW_ERRORDIALOG,
						Helpers.getShortenedStackTrace(e, 1)));
			}
			//- Process

			universalHandler.sendMessage(Message.obtain(universalHandler, PWV_UNIVERSALHANDLER_HIDE_WAITDIALOG));
			wakeLock.release();
		}
	}).start();
}

/**
 * Save Vault Object to Application Database
 */
private synchronized void saveVaultToDB() throws Exception {
	String oldStampHash = vault.getCurrentStampHash();
	String newStampHash = vault.generateNewStampHash();
	String dbStampHash = null;
	byte[] serializedVault = serializeVault(vault, PWV_FORMAT_VERSION);

	StringBuffer dbhs = new StringBuffer();
	byte[] blobData = dbHelper.getBlobData(PWV_DBPREFIX, dbhs, null);

	if (oldStampHash == null && blobData != null) // important - don't save FirstRun vault if exist db version
		throw new IllegalStateException(
				"DB inconsistent: current object cannot be saved.");

	if (!dbhs.toString().equals("")) dbStampHash = dbhs.toString();

	if (!(oldStampHash == null && dbStampHash == null) && !oldStampHash.equals(dbStampHash))
		throw new IllegalStateException(
				"DB invalid HashStamp: current object cannot be saved.");

	dbHelper.insertUpdateBlobData(PWV_DBPREFIX, serializedVault, newStampHash, PWV_FORMAT_VERSION);

	// Auto Backup to File
	String autoBck = settingDataHolder.getItem("SC_PasswordVault", "SI_AutoBackup");
	if (autoBck.equals(SettingsActivity.PATH_DISABLED)) return;
	CryptFile bckDir = new CryptFile(autoBck);

	if (!bckDir.exists() || (new CryptFileWrapper(bckDir)).getWritePermissionLevelForDir() < 2) {
		ComponentProvider.getShowMessageDialog(this, null, getResources().getString(R.string.pwv_autoBackupDir_Invalid), ComponentProvider.DRAWABLE_ICON_CANCEL).show();
		return;
	}

	try {
		exportVaultToFile(getResources().getString(R.string.pwvAutoBackupFileName), bckDir, false, true, true);
	} catch (Exception e) {
		e.printStackTrace();
		ComponentProvider.getShowMessageDialog(this, null, e.getLocalizedMessage(), ComponentProvider.DRAWABLE_ICON_CANCEL).show();
	}
}

/**
 * Export Vault to a File
 */
public void exportVaultToFile(String fileName, File exportDir, boolean xmlExport, boolean replaceExisting, boolean silentExport) throws Exception {
	if (exportDir == null) exportDir = Helpers.getImportExportDir(settingDataHolder);

	fileName += ".";
	if (!xmlExport) fileName += PasswordVaultActivity.PWV_EXPORT_EXT; //pwv file
	else fileName += "xml"; //xml file
	File exportFile = new File(exportDir.getAbsolutePath() + File.separator + fileName);

	if (exportFile.exists() && !replaceExisting) {
		new ImageToast(getResources().getString(R.string.common_fileNameAlreadyExists_text).replaceAll("<1>", Matcher.quoteReplacement(fileName)), ImageToast.TOAST_IMAGE_CANCEL, this).show();
		return;
	}

	if (!xmlExport) //pwv file
	{
		byte[] dbVault;
		StringBuffer dbhs = new StringBuffer();
		ArrayList otherData = new ArrayList();
		dbVault = dbHelper.getBlobData(PasswordVaultActivity.PWV_DBPREFIX, dbhs, otherData);
		int pwvVersion = ((Integer) otherData.get(0)).intValue();

		if (pwvVersion < 2) {
			byte[] hash = Encryptor.getShortHash(dbVault);

			FileOutputStream out = new FileOutputStream(exportFile);
			out.write(hash);
			out.write(dbVault);
			out.flush();
			out.close();
		} else if (pwvVersion == 2) {
			FileOutputStream out = new FileOutputStream(exportFile);
			out.write("PWV".getBytes());
			out.write((byte) pwvVersion);
			out.write(dbVault);
			out.write(Encryptor.getSHA256Hash(dbVault));
			out.flush();
			out.close();
		} else if (pwvVersion == 3 || pwvVersion == 4) {
			FileOutputStream out = new FileOutputStream(exportFile);
			out.write("PWV".getBytes("UTF-8"));
			out.write((byte) pwvVersion);
			out.write(dbVault);
			out.write(Encryptor.getSHA3Hash(dbVault, 256));
			out.flush();
			out.close();
		} else throw new IllegalArgumentException("Unknown PWV Version: " + pwvVersion);
	} else Helpers.saveStringToFile(exportFile, vault.asXML()); //xml file

	if (!silentExport) {
		ComponentProvider.getShowMessageDialog(this, null, getResources().getString(R.string.pwv_moreDialog_exportVault_Saved)
				.replaceAll("<1>", Matcher.quoteReplacement(fileName)).replaceAll("<2>", Matcher.quoteReplacement(exportDir.getAbsolutePath())), null).show();
	}
}

/**
 * Simple input validation for KEM items
 */
private void simpleInputValidationKEM(String errorFragmentName, char[] data) {
	if (data.length == 0)
		throw new IllegalArgumentException(getResources().getString(R.string.common_missingInput_text) + ": " + getStringResource(errorFragmentName));
	if (data.length % 4 != 0)
		throw new IllegalArgumentException(getResources().getString(R.string.common_incorrectBase64) + ":<br/> " + getStringResource(errorFragmentName));
}

/**
 * Generate New KEM Key Pair
 */
private void kemGenerateNewKeyPair(String algorithm) {
	waitDialog = new SimpleWaitDialog(this);
	waitDialog.show();
	new Thread(new Runnable() {
		public void run() {
			PowerManager.WakeLock wakeLock;
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SSE:PWV_NewKeyPair");

			wakeLock.acquire();

			//+ Process
			try {
				KEMCipherProvider kcp = new KEMCipherProvider(algorithm, true);
				kcp.setContext(PasswordVaultActivity.this);
				char[][] keys = kcp.generateKeyPairEncoded();
				universalHandler.sendMessage(Message.obtain(universalHandler, PWV_UNIVERSALHANDLER_KEM_SETKEYS, keys));

			} catch (Exception e) {
				e.printStackTrace();
				universalHandler.sendMessage(Message.obtain(universalHandler, PWV_UNIVERSALHANDLER_SHOW_ERRORDIALOG,
						Helpers.getShortenedStackTrace(e, 1)));
			}
			//- Process

			universalHandler.sendMessage(Message.obtain(universalHandler, PWV_UNIVERSALHANDLER_HIDE_WAITDIALOG));
			wakeLock.release();
		}
	}).start();
}

/**
 * Generate New KEM Secret
 */
private void kemGenerateNewSecret(String algorithm, char[] publicKey) {
	waitDialog = new SimpleWaitDialog(this);
	waitDialog.show();
	new Thread(new Runnable() {
		public void run() {
			PowerManager.WakeLock wakeLock;
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SSE:PWV_NewSecret");

			wakeLock.acquire();

			//+ Process
			try {
				simpleInputValidationKEM("pwv_pqc_publicKey", publicKey);

				KEMCipherProvider kcp = new KEMCipherProvider(algorithm, true);
				kcp.setContext(PasswordVaultActivity.this);
				char[][] secrets = kcp.generateKeySecretsEncoded(publicKey);
				universalHandler.sendMessage(Message.obtain(universalHandler, PWV_UNIVERSALHANDLER_KEM_SETSECRETS, secrets));

			} catch (Exception e) {
				e.printStackTrace();
				String message = e.getLocalizedMessage();
				universalHandler.sendMessage(Message.obtain(universalHandler, PWV_UNIVERSALHANDLER_SHOW_ERRORDIALOG, message));
			}
			//- Process

			universalHandler.sendMessage(Message.obtain(universalHandler, PWV_UNIVERSALHANDLER_HIDE_WAITDIALOG));
			wakeLock.release();
		}
	}).start();
}

/**
 * Extract KEM Secret
 */
private void kemExtractSharedSecret(String algorithm, char[] privateKey, char[] secretEncapsulated) {
	waitDialog = new SimpleWaitDialog(this);
	waitDialog.show();
	new Thread(new Runnable() {
		public void run() {
			PowerManager.WakeLock wakeLock;
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SSE:PWV_ExtractSecret");

			wakeLock.acquire();

			//+ Process
			try {
				simpleInputValidationKEM("pwv_pqc_privateKey", privateKey);
				simpleInputValidationKEM("pwv_pqc_sharedSecretEncapsulated", secretEncapsulated);

				KEMCipherProvider kcp = new KEMCipherProvider(algorithm);
				kcp.setContext(PasswordVaultActivity.this);
				char[][] secret = kcp.extractSecretEncoded(privateKey, secretEncapsulated);
				universalHandler.sendMessage(Message.obtain(universalHandler, PWV_UNIVERSALHANDLER_KEM_SETEXTRACTEDSECRET, secret));

			} catch (Exception e) {
				e.printStackTrace();
				String message = e.getLocalizedMessage();
				universalHandler.sendMessage(Message.obtain(universalHandler, PWV_UNIVERSALHANDLER_SHOW_ERRORDIALOG, message));
			}
			//- Process

			universalHandler.sendMessage(Message.obtain(universalHandler, PWV_UNIVERSALHANDLER_HIDE_WAITDIALOG));
			wakeLock.release();
		}
	}).start();
}

/**
 * Set and Reset Item Detail Variables before show
 */
private void prepareLayoutItemDetailForShow(boolean extendedItem, boolean kemItem) {
	prepareLayoutItemDetailForShow(extendedItem, kemItem, false);
}

/**
 * Set and Reset Item Detail Variables before show
 */
private void prepareLayoutItemDetailForShow(boolean extendedItem, boolean kemItem, boolean conversionInProgress) {
	if (conversionInProgress) convertToExtendedButton.setTag("inProgress");
	else convertToExtendedButton.setTag("");
	convertToExtendedContainer.setTag("");

	setLayoutItemDetailOrientation();

	bottomRightContainer.removeAllViews();
	commentExtendedContainer.removeAllViews();
	itemColorSpinner.setVisibility(View.VISIBLE);

	if (extendedItem || conversionInProgress) {
		switchTopBottomButton.setVisibility(View.GONE);
		accountContainer.setVisibility(View.VISIBLE);
		urlContainer.setVisibility(View.VISIBLE);
		customElementsView.setVisibility(View.VISIBLE);
		kemContainer.setVisibility(View.GONE);

		customElementsAdapter = new CustomElementsAdapter(this, currentItem.getCustomElementsClone(), fontSizeMultiplier);
		customElementsView.setAdapter(customElementsAdapter);

		commentExtendedContainer.addView(commentWrapContainer);
		commentExtendedContainer.setVisibility(View.VISIBLE);
		bottomRightContainer.setVisibility(View.GONE);

		convertToExtendedContainer.setTag("hide");
	} else if (kemItem) {
		switchTopBottomButton.setVisibility(View.GONE);
		accountContainer.setVisibility(View.GONE);
		urlContainer.setVisibility(View.GONE);
		customElementsView.setVisibility(View.GONE);
		itemColorSpinner.setVisibility(View.GONE);
		kemContainer.setVisibility(View.VISIBLE);

		commentExtendedContainer.addView(commentWrapContainer);
		commentExtendedContainer.setVisibility(View.VISIBLE);
		bottomRightContainer.setVisibility(View.GONE);

		convertToExtendedContainer.setTag("hide");
	} else {
		if (isOrientationPortrait()) switchTopBottomButton.setVisibility(View.VISIBLE);
		else switchTopBottomButton.setVisibility(View.GONE);
		accountContainer.setVisibility(View.GONE);
		urlContainer.setVisibility(View.GONE);
		customElementsView.setVisibility(View.GONE);
		kemContainer.setVisibility(View.GONE);

		customElementsAdapter = new CustomElementsAdapter(this, new ArrayList<StringSentinel[]>(), fontSizeMultiplier);
		customElementsView.setAdapter(customElementsAdapter);

		bottomRightContainer.addView(commentWrapContainer);
		commentExtendedContainer.setVisibility(View.GONE);
		bottomRightContainer.setVisibility(View.VISIBLE);
	}

	if (!conversionInProgress) {
		itemNameEditText.setCharArrayAndWipe(currentItem.getItemName());
		itemPasswordEditText.setCharArrayAndWipe(currentItem.getItemPassword());
		itemAccountEditText.setCharArrayAndWipe(currentItem.getItemAccount());
		itemUrlEditText.setCharArrayAndWipe(currentItem.getItemUrl());
		if (currentItem.getItemKemAlgorithm().length > 0)
			kemAlgorithmSpinner.setSelection(KEMCipherProvider.getAlgorithmIndex(new String(currentItem.getItemKemAlgorithm())));
		kemPrivateKey.setCharArrayAndWipe(currentItem.getItemKemPrivateKey());
		kemPublicKey.setCharArrayAndWipe(currentItem.getItemKemPublicKey());
		kemSharedSecret.setCharArrayAndWipe(currentItem.getItemKemSharedSecret());
		kemSharedSecretEncapsulated.setCharArrayAndWipe(currentItem.getItemKemSharedSecretEncapsulated());
		kemSharedSecretExtracted.setCharArrayAndWipe(currentItem.getItemKemSharedSecretExtracted());
		itemCommentEditText.setCharArrayAndWipe(currentItem.getItemComment());
		itemCommentTextView.setCharArrayAndWipe(currentItem.getItemComment());
	}

	itemCommentTextView.setFocusableInTouchMode(false);
	itemCommentTextView.setFocusable(false);
	itemCommentTextView.setMovementMethod(new ScrollingMovementMethod());
	itemCommentTextView.setBackgroundResource(R.drawable.d_edittext_readonly);
	itemCommentTextView.setTextColor(Color.BLACK);
	itemCommentEditText.scrollTo(0, 0);
	itemCommentTextView.scrollTo(0, 0);
	topLeftContainer.scrollTo(0, 0);
	itemColorSpinner.setSelection(ColorHelper.getColorPosition(currentItem.getColorCode()));
	if (android.os.Build.VERSION.SDK_INT >= 19 && !disableAutoLinks)
		Linkify.addLinks(itemCommentTextView, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
	if ((itemPasswordEditText.getText().toString().trim().equals("")
			&& !itemNameEditText.getText().toString().trim().equals("")
			&& !currentItem.isExtendedItem() && !isItemConversionInProgress()))
		passwordContainer.setVisibility(View.GONE);
	else passwordContainer.setVisibility(View.VISIBLE);
}

/**
 * Prepare Item Detail Layout for View
 */
private void makeLayoutItemDetailReadOnly() {
	if (customElementsAdapter != null) {
		customElementsAdapter.setMode(CustomElementsAdapter.MODE_READ);
		customElementsAdapter.notifyDataSetChanged();
	}

	makeReadOnlyEditText(itemNameEditText);
	makeReadOnlyEditText(itemPasswordEditText);
	makeReadOnlyEditText(itemAccountEditText);
	makeReadOnlyEditText(itemUrlEditText);
	makeReadOnlyEditText(kemPrivateKey);
	makeReadOnlyEditText(kemPublicKey);
	makeReadOnlyEditText(kemSharedSecret);
	makeReadOnlyEditText(kemSharedSecretEncapsulated);
	makeReadOnlyEditText(kemSharedSecretExtracted);
	handleShowHide(passwordShowHideButton, itemPasswordEditText, false);
	handleShowHide(kemPrivateKeyShowHideButton, kemPrivateKey, false);
	handleShowHide(kemSecretShowHideButton, kemSharedSecret, false);
	handleShowHide(kemSecretExtractedShowHideButton, kemSharedSecretExtracted, false);
	//makeReadOnlyEditText(itemCommentEditText);
	itemCommentEditText.setVisibility(View.GONE);
	itemCommentTextView.setVisibility(View.VISIBLE);
	addNewCustomElementButton.setVisibility(View.GONE);
	convertToExtendedContainer.setVisibility(View.GONE);

	kemPrivateKeyEditOnlyContainer.setVisibility(View.GONE);
	kemPublicKeyEditOnlyContainer.setVisibility(View.GONE);
	kemSharedSecretEditOnlyContainer.setVisibility(View.GONE);
	kemSharedSecretEncapsulatedEditOnlyContainer.setVisibility(View.GONE);
	kemSharedSecretExtractedEditOnlyContainer.setVisibility(View.GONE);
	kemGenerateNewKeyPairButton.setVisibility(View.GONE);
	kemGenerateNewSecretButton.setVisibility(View.GONE);
	kemExtractSharedSecretButton.setVisibility(View.GONE);

	itemColorSpinner.setEnabled(false);
	kemAlgorithmSpinner.setEnabled(false);
	itemColorSpinner.setBackgroundResource(R.drawable.d_edittext_readonly);
	kemAlgorithmSpinner.setBackgroundResource(R.drawable.d_edittext_readonly);
	itemEditSaveButton.setText(getResources().getString(R.string.common_edit_text));
	itemEditSaveButton.setTag("edit");
	passwordGeneratorButton.setVisibility(Button.GONE);
	passwordShowHideButton.setVisibility(Button.VISIBLE);
	kemPrivateKeyShowHideButton.setVisibility(Button.VISIBLE);
	kemSecretShowHideButton.setVisibility(Button.VISIBLE);
	kemSecretExtractedShowHideButton.setVisibility(Button.VISIBLE);
	topLeftContainer.setVisibility(View.VISIBLE);
	bottomRightContainer.setVisibility(View.VISIBLE);
	bottomDelimiter.setVisibility(View.VISIBLE);
}


/**
 * Prepare Item Detail Layout for Edit
 */
private void makeLayoutItemDetailEditable() {
	if (customElementsAdapter != null) {
		customElementsAdapter.setMode(CustomElementsAdapter.MODE_EDIT);
		customElementsAdapter.notifyDataSetChanged();
	}

	makeEditableEditText(itemNameEditText);
	makeEditableEditText(itemPasswordEditText);
	makeEditableEditText(itemAccountEditText);
	makeEditableEditText(itemUrlEditText);
	makeEditableEditText(kemPrivateKey);
	makeEditableEditText(kemPublicKey);
	makeEditableEditText(kemSharedSecret);
	makeEditableEditText(kemSharedSecretEncapsulated);
	makeEditableEditText(kemSharedSecretExtracted);
	handleShowHide(passwordShowHideButton, itemPasswordEditText, true);
	handleShowHide(kemPrivateKeyShowHideButton, kemPrivateKey, true);
	handleShowHide(kemSecretShowHideButton, kemSharedSecret, true);
	handleShowHide(kemSecretExtractedShowHideButton, kemSharedSecretExtracted, true);
	itemCommentTextView.setVisibility(View.GONE);
	itemCommentEditText.setVisibility(View.VISIBLE);
	if (currentItem.isExtendedItem()) addNewCustomElementButton.setVisibility(View.VISIBLE);
	else if (currentItem.isKemItem()) {
		addNewCustomElementButton.setVisibility(View.GONE);
		switchTopBottomButton.setVisibility(View.GONE);
	} else {
		addNewCustomElementButton.setVisibility(View.GONE);
		if (isOrientationPortrait()) switchTopBottomButton.setVisibility(View.VISIBLE);
	}

	if (((String) convertToExtendedContainer.getTag()).equals("hide"))
		convertToExtendedContainer.setVisibility(View.GONE);
	else convertToExtendedContainer.setVisibility(View.VISIBLE);

	kemPrivateKeyEditOnlyContainer.setVisibility(View.VISIBLE);
	kemPublicKeyEditOnlyContainer.setVisibility(View.VISIBLE);
	kemSharedSecretEditOnlyContainer.setVisibility(View.VISIBLE);
	kemSharedSecretEncapsulatedEditOnlyContainer.setVisibility(View.VISIBLE);
	kemSharedSecretExtractedEditOnlyContainer.setVisibility(View.VISIBLE);
	kemGenerateNewKeyPairButton.setVisibility(View.VISIBLE);
	kemGenerateNewSecretButton.setVisibility(View.VISIBLE);
	kemExtractSharedSecretButton.setVisibility(View.VISIBLE);

	itemColorSpinner.setEnabled(true);
	kemAlgorithmSpinner.setEnabled(true);
	itemColorSpinner.setBackgroundResource(R.drawable.d_edittext);
	kemAlgorithmSpinner.setBackgroundResource(R.drawable.d_edittext);
	itemEditSaveButton.setText(getResources().getString(R.string.common_save_text));
	itemEditSaveButton.setTag("save");
	passwordGeneratorButton.setVisibility(Button.VISIBLE);
	passwordShowHideButton.setVisibility(Button.GONE);
	kemPrivateKeyShowHideButton.setVisibility(Button.GONE);
	kemSecretShowHideButton.setVisibility(Button.GONE);
	kemSecretExtractedShowHideButton.setVisibility(Button.GONE);
	moveCursorToEnd();
	//if(android.os.Build.VERSION.SDK_INT >= 19) itemCommentEditText.setText(itemCommentEditText.getText().toString());
	if (!currentItem.isKemItem()) passwordContainer.setVisibility(View.VISIBLE);
	else passwordContainer.setVisibility(View.GONE);
}


/**
 * Update Item List and other "current items related" variables
 */
private void resetItemsList() {
	currentItems.clear();
	VaultItemV2 tvi = VaultItemV2.getSpecial(VaultItemV2.SPEC_GOBACKITEM, currentFolder.getFolderName());
	tvi.setColorCode(currentFolder.getColorCode());
	currentFolder.notifyItemDataSetChanged();
	currentItems.add(tvi);
	currentItems.addAll(currentFolder.getItemList());
	if (!currentFolder.isSearchResult())
		currentItems.add(VaultItemV2.getSpecial(VaultItemV2.SPEC_NEWITEM));
	else currentItems.add(VaultItemV2.getSpecial(VaultItemV2.SPEC_SEARCH));

	itemsArrayAdapter.getFilter().filter(searchItemView.getQuery());
	itemsArrayAdapter.notifyDataSetChanged();
}


/**
 * Helper method for "makeLayoutItemDetailReadOnly" method
 */
private void makeReadOnlyEditText(EditText et) {
	//et.setEnabled(false);
	et.setFocusableInTouchMode(false);
	et.setFocusable(false);
	et.setClickable(false);
	et.setCursorVisible(false);
	et.setBackgroundResource(R.drawable.d_edittext_readonly);
	et.setTextColor(Color.BLACK);
}


/**
 * Helper method for "makeLayoutItemDetailEditable" method
 */
private void makeEditableEditText(EditText et) {
	//et.setEnabled(true);
	et.setFocusableInTouchMode(true);
	et.setFocusable(true);
	et.setClickable(true);
	et.setCursorVisible(true);
	et.setBackgroundResource(R.drawable.d_edittext);
}

/**
 * Move Cursor to End of Text - if Current Focus is EditText
 **/
private void moveCursorToEnd() {
	View view = getCurrentFocus();
	if (view != null && view instanceof EditText) {
		EditText et = (EditText) view;
		int textLength = et.getText().length();
		et.setSelection(textLength, textLength);
	}
}

/**
 * Solve differences between Portrait and Landscape orientation (Item Detail Layer)
 */
private void setLayoutItemDetailOrientation() {
	ViewGroup lMTL = (ViewGroup) this.findViewById(R.id.PWVD_mainTopLeft);
	ViewGroup lMBR = (ViewGroup) this.findViewById(R.id.PWVD_mainBottomRight);
	FrameLayout lC = (FrameLayout) this.findViewById(R.id.PWVD_centerer);

	if (ked != null) handleKeyboardAppear(ked.isOpen());

	int orientation = this.getResources().getConfiguration().orientation;
	if (orientation == Configuration.ORIENTATION_PORTRAIT || (currentItem != null && (currentItem.isExtendedItem() || currentItem.isKemItem())) || isItemConversionInProgress()) {
		{
			RelativeLayout.LayoutParams relativeParams =
					new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			relativeParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);

			lMTL.setLayoutParams(relativeParams);
		}
		{
			RelativeLayout.LayoutParams relativeParams =
					new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			relativeParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			relativeParams.addRule(RelativeLayout.BELOW, lC.getId());

			lMBR.setLayoutParams(relativeParams);
		}
		{
			RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(0, 0);
			relativeParams.addRule(RelativeLayout.BELOW, lMTL.getId());

			lC.setLayoutParams(relativeParams);
		}
	} else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
		{
			RelativeLayout.LayoutParams relativeParams =
					new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			relativeParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			relativeParams.addRule(RelativeLayout.LEFT_OF, lC.getId());

			lMTL.setLayoutParams(relativeParams);
		}
		{
			RelativeLayout.LayoutParams relativeParams =
					new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			relativeParams.addRule(RelativeLayout.RIGHT_OF, lC.getId());
			relativeParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

			lMBR.setLayoutParams(relativeParams);
		}
		{
			RelativeLayout.LayoutParams relativeParams = new RelativeLayout.LayoutParams(0, 0);
			relativeParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

			lC.setLayoutParams(relativeParams);
		}
	}
}

private PasswordDialog getStartScreenPasswordDialog() {
	PasswordDialog passwordDialog;
	byte[] testVault = dbHelper.getBlobData(PWV_DBPREFIX);
	if (testVault == null)
		passwordDialog = new PasswordDialog(this, PasswordDialog.PD_MODE_SET_PASSWORD, Encryptor.PURPOSE_PASSWORD_VAULT);
	else {
		passwordDialog = new PasswordDialog(this, PasswordDialog.PD_MODE_ENTER_PASSWORD, Encryptor.PURPOSE_PASSWORD_VAULT);
		passwordDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	}
	passwordDialog.setEncryptAlgorithmCode(encryptAlgorithmCode);
	passwordDialog.setParentMessage("enter");
	return passwordDialog;
}

private PWVSearchDialog getSearchDialog() {
	PWVSearchDialog sd = new PWVSearchDialog(PasswordVaultActivity.this, PWV_MESSAGE_SEARCHDIALOG_SEARCH);
	sd.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	return sd;
}

/**
 * Alter Item Detail Layout regarding Keyboard is/isn't visible
 */
private void handleKeyboardAppear(boolean isKeyboardVisible) {
	if (topLeftContainer == null || bottomRightContainer == null || switchTopBottomButton == null || bottomDelimiter == null)
		return;

	boolean orientationPortrait = this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

	if (layoutSwitcher.getDisplayedChild() == PWV_LAYOUT_ITEMDETAIL) {
		if (currentItem != null && (currentItem.isExtendedItem() || currentItem.isKemItem()) || isItemConversionInProgress())
			return;

		if (!orientationPortrait) {
			switchTopBottomButton.setVisibility(View.GONE);
			topLeftContainer.setVisibility(View.VISIBLE);
			bottomRightContainer.setVisibility(View.VISIBLE);
			bottomDelimiter.setVisibility(View.VISIBLE);
		} else {
			switchTopBottomButton.setVisibility(View.VISIBLE);

			View focView = getWindow().getCurrentFocus();
			if (focView == null) return;
			Integer id = focView.getId();

			if (id == R.id.PWVD_comment && isKeyboardVisible) {
				topLeftContainer.setVisibility(View.GONE);
				bottomDelimiter.setVisibility(View.GONE);
			} else if ((id == R.id.PWVD_name || id == R.id.PWVD_password) && isKeyboardVisible) {
				bottomRightContainer.setVisibility(View.GONE);
				bottomDelimiter.setVisibility(View.GONE);
			}
		}
	}
}

/**
 * Before PasswordVaultActivity Exit
 */
private void handleExit() {
	if (askOnLeave) {
		ComponentProvider.getBaseQuestionDialog(this,
				getResources().getString(R.string.common_returnToMainMenuTitle),
				getResources().getString(R.string.common_question_leave).replaceAll("<1>", getResources().getString(R.string.common_app_passwordVault_name)),
				null,
				COMMON_MESSAGE_CONFIRM_EXIT
		).show();
	} else setMessage(new ActivityMessage(COMMON_MESSAGE_CONFIRM_EXIT, null));
}

/**
 * Check, Add and Save Item
 */
private void handleItemSave(String mode) {
	if (itemNameEditText.getText().toString().trim().equals("")) {
		new ImageToast(getResources().getString(R.string.common_enterTheName_text), ImageToast.TOAST_IMAGE_CANCEL, this).show();
		return;
	}

	if (mode.equals("new")) {
		currentFolder.addItem(currentItem);
		itemDeleteButton.setEnabled(true);
		itemMoveToButton.setEnabled(true);
	}

	if (mode.equals("edit")) {
		makeLayoutItemDetailEditable();
		itemDeleteButton.setEnabled(false);
		itemMoveToButton.setEnabled(false);
		return;
	}

	itemDeleteButton.setEnabled(false);
	itemEditSaveButton.setEnabled(false);
	itemMoveToButton.setEnabled(false);

	List tagMessage = (List) itemNameEditText.getTag();
	String itemHash = (String) tagMessage.get(0);
	int position = (Integer) tagMessage.get(1);

	if (!((String) tagMessage.get(0)).equals(currentItem.getItemSecurityHash()))
		throw new IllegalStateException("hash doesn't match");

	currentItem.setItemName(Helpers.trim(itemNameEditText.toCharArray()));
	currentItem.setItemPassword(Helpers.trim(itemPasswordEditText.toCharArray()));
	currentItem.setItemComment(Helpers.trim(itemCommentEditText.toCharArray()));
	if (!currentItem.isKemItem())
		currentItem.setColorCode(ColorHelper.getColorList().get(itemColorSpinner.getSelectedItemPosition()).colorCode);
	currentItem.setDateModified();

	if (isItemConversionInProgress()) currentItem.enableExtendedItem();

	if (currentItem.isExtendedItem()) {
		currentItem.setItemAccount(Helpers.trim(itemAccountEditText.toCharArray()));
		currentItem.setItemURL(Helpers.trim(itemUrlEditText.toCharArray()));
		currentItem.addOrReplaceCustomElements(customElementsAdapter.getDataSet());
	}

	if (currentItem.isKemItem()) {
		currentItem.setItemKemAlgorithm(KEMCipherProvider.getAlgorithmNameByIndex(kemAlgorithmSpinner.getSelectedItemPosition()).toCharArray());
		currentItem.setItemKemPrivateKey(Helpers.trim(kemPrivateKey.toCharArray()));
		currentItem.setItemKemPublicKey(Helpers.trim(kemPublicKey.toCharArray()));
		currentItem.setItemKemSharedSecret(Helpers.trim(kemSharedSecret.toCharArray()));
		currentItem.setItemKemSharedSecretEncapsulated(Helpers.trim(kemSharedSecretEncapsulated.toCharArray()));
		currentItem.setItemKemSharedSecretExtracted(Helpers.trim(kemSharedSecretExtracted.toCharArray()));
		currentItem.setColorCode(Color.rgb(254, 254, 254));
	}

	try {
		saveVaultToDB();
	} catch (Exception e) {
		e.printStackTrace();
		showErrorDialog(e);
	}

	//Hide keyboard
	View view = this.getCurrentFocus();
	if (view != null) {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

	tagMessage.clear();
	tagMessage.add(currentItem.getItemSecurityHash());
	tagMessage.add(position);

	leaveItemDetailLayout();
}

/**
 * Import or Merge from XML file
 */
private void importOrMergeXML(String fileName, boolean merge) {
	File importFile = new File(Helpers.getImportExportPath(settingDataHolder) + File.separator + fileName);

	VaultV2 tempvault = null;

	try {
		if (!merge) {
			if (!StringSentinel.isInitialized(VaultV2.SS_GROUP_ID))
				dummyHolder = StringSentinel.init(VaultV2.SS_GROUP_ID);
		}
		tempvault = VaultV2.getInstance(Helpers.loadStringFromFile(importFile), this);
	} catch (Exception e) {
		ComponentProvider.getShowMessageDialog(this, this.getResources().getString(R.string.pwv_moreDialog_importVault),
				this.getResources().getString(R.string.pwv_moreDialog_importVault_NotValid)
						.replaceAll("<1>", Matcher.quoteReplacement(fileName)) + "<br/><br/>" + e.getLocalizedMessage(), ComponentProvider.DRAWABLE_ICON_CANCEL).show();
	}

	if (!merge) {
		if (tempvault != null) {
			vault = tempvault;
			PasswordDialog xmlPD = new PasswordDialog(this, PasswordDialog.PD_MODE_SET_PASSWORD, Encryptor.PURPOSE_PASSWORD_VAULT);
			xmlPD.setEncryptAlgorithmCode(encryptAlgorithmCode);
			xmlPD.setParentMessage("xmlimport:" + fileName);
			xmlPD.setBlockCancellation(true);
			xmlPD.show();
		}
	} else {
		if (tempvault != null) mergeVaults(tempvault);
	}
}

/**
 * Import or Merge from PWV file
 */
private void importOrMergePWV(String fileName, boolean merge) {
	File importFile = new File(Helpers.getImportExportPath(settingDataHolder) + File.separator + fileName);

	if (importFile.length() > 52428800) {
		ComponentProvider.getImageToast("Sorry - file <1> is too large to import.<br/> 50MB max."
				.replaceAll("<1>", Matcher.quoteReplacement(fileName)), ImageToast.TOAST_IMAGE_CANCEL, this).show();
		return;
	} else if (importFile.length() < 50) {
		ComponentProvider.getImageToast(("Incorrect file - file <1> is too small: " + importFile.length() + " bytes")
				.replaceAll("<1>", Matcher.quoteReplacement(fileName)), ImageToast.TOAST_IMAGE_CANCEL, this).show();
		return;
	}

	ByteArrayOutputStream bos = new ByteArrayOutputStream();

	try {
		FileInputStream fis = new FileInputStream(importFile);
		byte[] buffer = new byte[10240];
		int noOfBytes = 0;

		while ((noOfBytes = fis.read(buffer)) != -1) {
			bos.write(buffer, 0, noOfBytes);
		}
		fis.close();
	} catch (IOException e) {
		e.printStackTrace();
	}

	byte[] input = bos.toByteArray();
	bos = null;

	// Version detection
	int version = -1;
	// version 2+ ?
	if (input[0] == 'P' && input[1] == 'W' && input[2] == 'V') {
		int possibleVersion = input[3];

		byte[] currentHash = null;
		if (possibleVersion == 2)
			currentHash = Encryptor.getSHA256Hash(Helpers.getSubarray(input, 4, input.length - 36));
		else if (possibleVersion == 3 || possibleVersion == 4)
			currentHash = Encryptor.getSHA3Hash(Helpers.getSubarray(input, 4, input.length - 36), 256);
		byte[] storedHash = Helpers.getSubarray(input, input.length - 32, 32);

		if (currentHash != null && Arrays.equals(currentHash, storedHash))
			version = input[3];
	}

	byte[] vaultData = null;

	// version 1
	if (version < 0) {
		version = 1;
		int offset = 4;
		byte[] hash = Helpers.getSubarray(input, 0, offset);
		byte[] data = Helpers.getSubarray(input, offset, input.length - offset);

		if (!Arrays.equals(hash, Encryptor.getShortHash(data))) {
			ComponentProvider.getImageToast(this.getResources().getString(R.string.pwv_moreDialog_importVault_Corrupted)
					.replaceAll("<1>", Matcher.quoteReplacement(fileName)), ImageToast.TOAST_IMAGE_CANCEL, this).show();
			return;
		}

		if (!merge)
			dbHelper.insertUpdateBlobData(PWV_DBPREFIX, data, "IMPORTED", 1);
		else
			vaultData = data;
	} else // version 2+
	{
		byte[] data = Helpers.getSubarray(input, 4, input.length - 36);

		if (!merge)
			dbHelper.insertUpdateBlobData(PWV_DBPREFIX, data, "IMPORTED", version);
		else
			vaultData = data;
	}

	if (!merge) {
		ComponentProvider.getImageToast(this.getResources().getString(R.string.pwv_moreDialog_importVault_Loaded)
				.replaceAll("<1>", Matcher.quoteReplacement(fileName)), ImageToast.TOAST_IMAGE_OK, this).show();

		setResult(RESTART_PASSWORDVAULTACTIVITY);
		this.finish();
	} else {
		List<Object> attachment = new ArrayList<Object>();
		attachment.add(vaultData);
		attachment.add(version);

		PasswordDialog mergePD = new PasswordDialog(this, PasswordDialog.PD_MODE_ENTER_PASSWORD, Encryptor.PURPOSE_PASSWORD_VAULT);
		mergePD.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		mergePD.setParentMessage("merge");
		mergePD.setAttachment(attachment);
		mergePD.show();
	}
}

/**
 * Merge given vault with current vault
 */
private synchronized void mergeVaults(final VaultV2 mergeVault) {
	waitDialog = new SimpleWaitDialog(this);
	waitDialog.show();
	new Thread(new Runnable() {
		public void run() {
			PowerManager.WakeLock wakeLock;
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SSE:PWV_MERGE_VAULTS");

			wakeLock.acquire();

			//+ Process
			try {
				List<Integer> mergeReport = Helpers.mergeVaults(vault, mergeVault);
				saveVaultToDB();

				iconAdapter.setSearchResultFolder(null);
				universalHandler.sendMessage(Message.obtain(universalHandler, PWV_UNIVERSALHANDLER_MERGE_FINALIZE, mergeReport));

			} catch (Exception e) {
				e.printStackTrace();
				universalHandler.sendMessage(Message.obtain(universalHandler, PWV_UNIVERSALHANDLER_SHOW_ERRORDIALOG,
						Helpers.getShortenedStackTrace(e, 1)));
			}
			//- Process

			universalHandler.sendMessage(Message.obtain(universalHandler, PWV_UNIVERSALHANDLER_HIDE_WAITDIALOG));
			wakeLock.release();
		}
	}).start();
}

private synchronized void handleShowHide(Button button, EditText editText, Boolean show) {
	Drawable img = null;
	if(show == null) show = editText.getTransformationMethod() != null;

	if (!show) {
		editText.setTransformationMethod(new PasswordTransformationMethod());
		img = getResources().getDrawable(R.drawable.invisible_icon_cropped);
	}
	else {
		editText.setTransformationMethod(null);
		img = getResources().getDrawable(R.drawable.visible_icon_cropped);
	}
	button.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null);
}

/**
 * Leave Item Detail Layout
 */
private void leaveItemDetailLayout() {
	currentFolder.notifyItemDataSetChanged();
	makeLayoutItemDetailReadOnly();
	resetItemsList();
	layoutSwitcher.showPrevious();
	itemDeleteButton.setEnabled(true);
	itemMoveToButton.setEnabled(true);
	itemEditSaveButton.setEnabled(true);

	View view = this.getCurrentFocus();
	if (view != null) {
		InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
	}
}

private VaultV2 convertToLatestVaultVersion(Object vaultObject) {
	VaultV2 vault;
	if (vaultObject instanceof Vault) vault = Helpers.migrateVault((Vault) vaultObject);
	else vault = (VaultV2) vaultObject;
	return vault;
}

@Override
protected void onStart() {
	super.onStart();
}

/**
 * Lock Screen
 */
private void showScreenLockDialog() {
	sld = new ScreenLockDialog(this, encryptor.getKeyHash());
	sld.show();
}

@Override
protected void onPause() {
	super.onPause();

	if (lockOnPause > -1 && layoutSwitcher.getDisplayedChild() > 0) {
		setScreenLockTime(lockOnPause);
		screenLockedPosition = layoutSwitcher.getDisplayedChild();
		layoutStartButtons.setVisibility(LinearLayout.GONE);
		layoutSwitcher.setDisplayedChild(0);

		if (!Helpers.isScreenOn(this)) {
			showScreenLockDialog();
		}
	}
}

@Override
protected void onResume() {
	super.onResume();

	if (sldVeto) {
		doOnUnlock();
		sldVeto = false;
		return;
	}

	if (checkScreenAutoUnlock()) {
		doOnUnlock();
		return;
	}

	if (sld != null && sld.getActiveFlag()) return;

	if (screenLockedPosition > 0) {
		showScreenLockDialog();
	}
}

//+ Lockable
public void doOnLock() {
	// N/A
}

public void doOnUnlock() {
	if (screenLockedPosition > 0) layoutSwitcher.setDisplayedChild(screenLockedPosition);
	screenLockedPosition = -1;
	try {
		if (sld != null) sld.cancel();
		sld = null;
	} catch (Exception e) {
	}
	;
}
//- Lockable

/**
 * Back Button - navigate back in the Password Vault Layers
 * if Folders or Start Layer, return to Main Menu
 */
@Override
public void onBackPressed() {
	switch (layoutSwitcher.getDisplayedChild()) {
		case PWV_LAYOUT_FOLDERS: {
			handleExit();
			break;
		}
		case PWV_LAYOUT_ITEMS: {
			layoutSwitcher.showPrevious();
			searchItemView.setQuery("", false);
			foldersSearchButton.requestFocus();
			break;
		}
		case PWV_LAYOUT_ITEMDETAIL: {
			boolean itemChanged =
					!(
							Arrays.equals(currentItem.getItemName(), Helpers.trim(itemNameEditText.toCharArray())) &&
									Arrays.equals(currentItem.getItemPassword(), Helpers.trim(itemPasswordEditText.toCharArray())) &&
									Arrays.equals(currentItem.getItemComment(), Helpers.trim(itemCommentEditText.toCharArray())) &&
									(currentItem.getColorCode() == ColorHelper.getColorList().get(itemColorSpinner.getSelectedItemPosition()).colorCode ||
											currentItem.getColorCode() == -1)
					);

			boolean extendedItemChange =
					currentItem.isExtendedItem() && !(
							Arrays.equals(currentItem.getItemAccount(), Helpers.trim(itemAccountEditText.toCharArray())) &&
									Arrays.equals(currentItem.getItemUrl(), Helpers.trim(itemUrlEditText.toCharArray())) &&
									Helpers.compareDeepElements(currentItem.getCustomElements(), customElementsAdapter.getDataSet())
					);

			boolean kemItemChange =
					currentItem.isKemItem() && !(
							Arrays.equals(currentItem.getItemKemAlgorithm(), KEMCipherProvider.getAlgorithmNameByIndex(kemAlgorithmSpinner.getSelectedItemPosition()).toCharArray()) &&
									Arrays.equals(currentItem.getItemKemPrivateKey(), Helpers.trim(kemPrivateKey.toCharArray())) &&
									Arrays.equals(currentItem.getItemKemPublicKey(), Helpers.trim(kemPublicKey.toCharArray())) &&
									Arrays.equals(currentItem.getItemKemSharedSecret(), Helpers.trim(kemSharedSecret.toCharArray())) &&
									Arrays.equals(currentItem.getItemKemSharedSecretEncapsulated(), Helpers.trim(kemSharedSecretEncapsulated.toCharArray())) &&
									Arrays.equals(currentItem.getItemKemSharedSecretExtracted(), Helpers.trim(kemSharedSecretExtracted.toCharArray()))
					);

			if (itemChanged || extendedItemChange || kemItemChange || isItemConversionInProgress()) {
				ComponentProvider.getBaseQuestionDialog(this,
						getResources().getString(R.string.common_save_text),
						getResources().getString(R.string.common_question_saveChanges),
						(String) itemEditSaveButton.getTag(),
						PWV_MESSAGE_ITEM_SAVE_CONFIRM
				).show();
			} else leaveItemDetailLayout();
			break;
		}
		case PWV_LAYOUT_START: {
			handleExit();
			break;
		}
		default:
			break;
	}
}

/**
 * Menu + Search Buttons
 */
@Override
public boolean onKeyDown(int keyCode, KeyEvent event) {
	if (keyCode == KeyEvent.KEYCODE_SEARCH) {
		return true;
	} else if (keyCode == KeyEvent.KEYCODE_MENU) {
		if (foldersBottomMenu != null) {
			if (foldersBottomMenu.getVisibility() == LinearLayout.GONE) showBottomMenu();
			else hideBottomMenu();
		}
		return true;
	} else return super.onKeyDown(keyCode, event);
}

@Override
public void onConfigurationChanged(Configuration c) {
	setLayoutItemDetailOrientation();

	super.onConfigurationChanged(c);
}

@Override
public void onWindowFocusChanged(boolean b) {
	if (this.encryptor == null) {
		layoutStartButtons.setVisibility(LinearLayout.VISIBLE);
	}
	super.onWindowFocusChanged(b);
}

@Override
public void onDestroy() {
	if (ked != null) ked.stopDetection();
	wipeEncryptor();
	StringSentinel.destroy(VaultV2.SS_GROUP_ID);
	super.onDestroy();
}

@Override
public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
	if (requestCode == REQUEST_CODE_SEND_PASSWORD) {
		if (resultCode != EXIT_CASCADE)
			sldVeto = true;
		else
			setMessage(new ActivityMessage(EXIT_CASCADE, null));
	}
}

Handler waitForSaveHandler = new Handler() {
	public void handleMessage(Message msg) {
		if (msg.what == -100) {
			if (waitDialog != null) waitDialog.cancel();
			return;
		}
		if (msg.what == -400) {
			if (waitDialog != null) waitDialog.cancel();
			Exception e = (Exception) msg.obj;
			commonToast.setText(e.getMessage());
			((ImageToast) commonToast).setImage(ImageToast.TOAST_IMAGE_CANCEL);
			commonToast.show();
			e.printStackTrace();
		}
	}
};

/**
 * Create default Vault Object on the first run
 */
private VaultV2 getVaultOnFirstRun(VaultV2 v) {
	VaultV2 vault;
	if (v == null) vault = VaultV2.getInstance();
	else vault = v;

	//Items
	VaultItemV2 v00 = new VaultItemV2();
	v00.setItemName(getResources().getString(R.string.pwv_data_item_00).toCharArray());
	v00.setItemPassword(getResources().getString(R.string.pwv_data_item_00_password).toCharArray());
	v00.setItemComment(getResources().getString(R.string.pwv_data_item_00_comment).toCharArray());
	v00.setDateModified();
	v00.setColorCode(Color.rgb(255, 0, 0));
	waitPlease(3);

	VaultItemV2 v01 = new VaultItemV2();
	v01.setItemName(getResources().getString(R.string.pwv_data_item_01).toCharArray());
	v01.setItemPassword(getResources().getString(R.string.pwv_data_item_01_password).toCharArray());
	v01.setItemComment(getResources().getString(R.string.pwv_data_item_00_comment).toCharArray());
	v01.setDateModified();
	v01.setColorCode(Color.rgb(0, 0, 255));

	VaultItemV2 v02 = new VaultItemV2();
	v02.setItemName(getResources().getString(R.string.pwv_data_item_02).toCharArray());
	v02.setItemPassword(getResources().getString(R.string.pwv_data_item_02_password).toCharArray());
	v02.setItemComment(getResources().getString(R.string.pwv_data_item_02_comment).toCharArray());
	v02.setDateModified();
	v02.setColorCode(Color.rgb(255, 255, 0));

	VaultItemV2 v03 = new VaultItemV2();
	v03.enableExtendedItem();
	v03.setItemName(("⚡ " + getResources().getString(R.string.pwv_newItemExtended)).toCharArray());
	v03.setItemPassword(getResources().getString(R.string.common_password_text).toLowerCase().toCharArray());
	v03.setDateModified();
	v03.setColorCode(Color.rgb(170, 0, 255));
	v03.setItemAccount(getResources().getString(R.string.pwv_data_extended_item_01_account).toCharArray());
	v03.setItemURL(getResources().getString(R.string.pwv_data_extended_item_01_url).toCharArray());
	v03.addCustomElement((Helpers.capitalizeAllFirstLetters(getResources().getString(R.string.pwv_newItemElementTitle)) + " A").toCharArray(),
			Helpers.capitalizeAllFirstLetters(getResources().getString(R.string.pwv_newItemElementValue)).toCharArray());
	v03.addCustomElement((Helpers.capitalizeAllFirstLetters(getResources().getString(R.string.pwv_newItemElementTitle)) + " B").toCharArray(),
			Helpers.capitalizeAllFirstLetters(getResources().getString(R.string.pwv_newItemElementValue)).toCharArray());

	//Folders
	VaultFolderV2 v0 = new VaultFolderV2();
	v0.setFolderName(getResources().getString(R.string.pwv_data_folder_00).toCharArray());
	v0.setColorCode(Color.rgb(255, 150, 0));
	waitPlease(3);
	vault.addFolder(v0);

	VaultFolderV2 v1 = new VaultFolderV2();
	v1.setFolderName(getResources().getString(R.string.pwv_data_folder_01).toCharArray());
	v1.setColorCode(Color.rgb(0, 121, 240));
	waitPlease(3);
	vault.addFolder(v1);

	VaultFolderV2 v2 = new VaultFolderV2();
	v2.setFolderName(getResources().getString(R.string.pwv_data_folder_02).toCharArray());
	v2.setColorCode(Color.rgb(255, 255, 2));
	v2.setAttribute(VaultFolderV2.VAULTFOLDER_ATTRIBUTE_POSITION, 2);
	vault.addFolder(v2);
	waitPlease(3);

	VaultFolderV2 v3 = new VaultFolderV2();
	v3.setFolderName(getResources().getString(R.string.pwv_data_folder_03).toCharArray());
	v3.setColorCode(Color.rgb(255, 255, 2));
	v3.setAttribute(VaultFolderV2.VAULTFOLDER_ATTRIBUTE_POSITION, 1);
	v3.addItem(v00);
	v3.addItem(v01);
	v3.addItem(v02);
	v3.addItem(v03);
	vault.addFolder(v3);

	return vault;
}

/**
 * Ugly hack solving Android ViewAnimator invalidation bug
 */
private void solveViewAnimatorBug() {
	if (true || android.os.Build.VERSION.SDK_INT < 26) {
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (passwordContainer.getVisibility() == View.GONE) {
					passwordContainer.setVisibility(View.VISIBLE);
					passwordContainer.setVisibility(View.GONE);
				} else {
					passwordContainer.setVisibility(View.GONE);
					passwordContainer.setVisibility(View.VISIBLE);
				}
			}
		}, 10);
	}
}

/**
 * Show Bottom Menu (Folders Layout)
 */
private void showBottomMenu() {
	showMenuButton.setVisibility(LinearLayout.GONE);
	foldersBottomMenu.setVisibility(LinearLayout.VISIBLE);
}

/**
 * Show Bottom Menu (Items Layout)
 */
private void showBottomMenuItems() {
	showMenuItemsButton.setVisibility(LinearLayout.GONE);
	itemsBottomMenu.setVisibility(LinearLayout.VISIBLE);
}

/**
 * Hide Bottom Menu (Folders Layout)
 */
private void hideBottomMenu() {
	foldersBottomMenu.setVisibility(LinearLayout.GONE);
	showMenuButton.setVisibility(LinearLayout.VISIBLE);
}

private void waitPlease(int ms) {
	try {
		Thread.sleep(ms);
	} catch (InterruptedException e) {
		e.printStackTrace();
	}
}

@SuppressWarnings("deprecation")
private void setToSystemClipboard(CharSequence text) {
	ClipboardManager ClipMan = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
	ClipMan.setText(text);
}

/**
 * Prepare new Item
 */
private void createNewItem(int position, boolean extendedItem, boolean kemItem) {
	currentItem = new VaultItemV2();
	if (extendedItem) currentItem.enableExtendedItem();
	if (kemItem) {
		currentItem.enableKemItem();
		currentItem.setItemKemAlgorithm(KEMCipherProvider.ALGORITHM_CRYSTALS_KYBER_1024.toCharArray());
		kemAlgorithmSpinner.setSelection(KEMCipherProvider.getAlgorithmIndex(new String(currentItem.getItemKemAlgorithm())));
	}
	itemDeleteButton.setEnabled(false);
	itemMoveToButton.setEnabled(false);
	List tagMessage = new ArrayList();
	tagMessage.add(currentItem.getItemSecurityHash());
	tagMessage.add(position);
	itemNameEditText.setTag(tagMessage); // hash + position
	prepareLayoutItemDetailForShow(extendedItem, kemItem);
	makeLayoutItemDetailEditable();
	itemEditSaveButton.setTag("new");
	layoutSwitcher.showNext();
	solveViewAnimatorBug();
}

private boolean isOrientationPortrait() {
	return this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
}

private void showCreateNewItemDialog(int position) {
	List<String> title = new ArrayList<String>();
	List<String> comment = new ArrayList<String>();
	List<Object> tag = new ArrayList<Object>();

	title.add(getResources().getString(R.string.pwv_newItemBasic));
	comment.add(getResources().getString(R.string.pwv_newItemBasicComment));
	tag.add("B");
	title.add(getResources().getString(R.string.pwv_newItemExtended));
	comment.add(getResources().getString(R.string.pwv_newItemExtendedComment));
	tag.add("E");
	title.add(getResources().getString(R.string.pwv_pqc_item));
	comment.add(getResources().getString(R.string.pwv_pqc_itemComment));
	tag.add("K");

	SelectionDialog createItemDialog = new SelectionDialog(this, title, comment, null, tag, getResources().getString(R.string.pwv_newItem_text));
	createItemDialog.setAttachment(position);
	createItemDialog.setMessageCode(PWV_MESSAGE_ITEM_CREATE);
	createItemDialog.show();
}

private boolean isItemConversionInProgress() {
	if (convertToExtendedButton != null && convertToExtendedButton.getTag() != null)
		return ((String) convertToExtendedButton.getTag()).equals("inProgress");
	else
		return false;
}

/**
 * Copy to system clipboard
 */
private void handleBasicCopy(String text) {
	text = text.trim();
	if (text.length() == 0) {
		ComponentProvider.getImageToastKO(getResources().getString(R.string.common_noTextToCopy), this).show();
		return;
	}

	setToSystemClipboard(text);
	ComponentProvider.getImageToastOK(getResources().getString(R.string.common_textCopiedToSystemClipboard), this).show();
}

/**
 * Copy to system clipboard with option to copy particular paragraphs
 */
private void handleCommentCopy(String text) {
	text = text.trim();

	if (text.length() == 0) {
		ComponentProvider.getImageToastKO(getResources().getString(R.string.common_noTextToCopy), this).show();
		return;
	}

	String[] notes = text.split("\n");
	ArrayList<String> noteList = new ArrayList<String>();

	for (int i = 0; i < notes.length; ++i) {
		String tempNote = notes[i].trim();
		if (tempNote.length() > 0) noteList.add(tempNote);
	}

	if (noteList.size() == 1) {
		setToSystemClipboard(noteList.get(0));
		ComponentProvider.getImageToastOK(getResources().getString(R.string.common_textCopiedToSystemClipboard), this).show();
		return;
	}

	List<String> title = new ArrayList<String>();
	List<String> comment = new ArrayList<String>();
	List<Integer> icon = new ArrayList<Integer>();
	List<Object> tag = new ArrayList<Object>();

	title.add(getResources().getString(R.string.common_copyall_text));
	comment.add("...");
	icon.add(R.drawable.clipboard);
	tag.add(text);

	for (int i = 0; i < noteList.size(); ++i) {
		title.add(getResources().getString(R.string.common_copyParagraph_text) + " " + (i + 1));
		comment.add(noteList.get(i));
		tag.add(noteList.get(i));
		icon.add(R.drawable.clipboard_num);
	}

	SelectionDialog selectDirDialog = new SelectionDialog(this, title, comment, icon, tag);
	selectDirDialog.setMessageCode(PWV_MESSAGE_ITEM_NOTE_COPY);
	selectDirDialog.show();
}

// Handler for miscellaneous background activities
Handler universalHandler = new Handler() {
	public void handleMessage(Message msg) {
		if (msg.what == PWV_UNIVERSALHANDLER_SHOW_WAITDIALOG) {
			if (waitDialog != null) waitDialog.show();
			return;
		}
		if (msg.what == PWV_UNIVERSALHANDLER_HIDE_WAITDIALOG) {
			if (waitDialog != null) waitDialog.cancel();
			return;
		}
		if (msg.what == PWV_UNIVERSALHANDLER_SHOW_ERRORDIALOG) {
			showErrorDialog((String) msg.obj);
			return;
		}
		if (msg.what == PWV_UNIVERSALHANDLER_SEARCH_FINISH) {
			iconAdapter.notifyDataSetChanged();
			currentFolder = iconAdapter.getSearchResultFolder();
			resetItemsList();
			layoutSwitcher.showNext();
			return;
		}
		if (msg.what == PWV_UNIVERSALHANDLER_MERGE_FINALIZE) {
			iconAdapter.notifyDataSetChanged();
			List<Integer> outputReportList = (List) msg.obj;
			int newFolders = outputReportList.get(0);
			int newItems = outputReportList.get(1);
			int replacedItems = outputReportList.get(2);

			int iconCode = (newFolders > 0 || newItems > 0 || replacedItems > 0) ? ComponentProvider.DRAWABLE_ICON_OK : ComponentProvider.DRAWABLE_ICON_INFO_BLUE;

			StringBuffer mergeReport = new StringBuffer();
			mergeReport.append(getResources().getString(R.string.common_newIFolders) + ": " + newFolders + "<br/>");
			mergeReport.append(getResources().getString(R.string.common_newItems) + ": " + newItems + "<br/>");
			mergeReport.append(getResources().getString(R.string.common_replacedItems) + ": " + replacedItems + "<br/>");

			ComponentProvider.getShowMessageDialog(PasswordVaultActivity.this,
					getResources().getString(R.string.pwv_mergeVaults),
					mergeReport.toString(),
					iconCode).show();

			return;
		}
		if (msg.what == PWV_UNIVERSALHANDLER_KEM_SETKEYS) {
			char[][] keys = (char[][]) msg.obj;
			kemPrivateKey.setCharArrayAndWipe(keys[0]);
			kemPublicKey.setCharArrayAndWipe(keys[1]);

			return;
		}
		if (msg.what == PWV_UNIVERSALHANDLER_KEM_SETSECRETS) {
			char[][] keys = (char[][]) msg.obj;
			kemSharedSecret.setCharArrayAndWipe(keys[0]);
			kemSharedSecretEncapsulated.setCharArrayAndWipe(keys[1]);

			return;
		}
		if (msg.what == PWV_UNIVERSALHANDLER_KEM_SETEXTRACTEDSECRET) {
			char[][] keys = (char[][]) msg.obj;
			kemSharedSecretExtracted.setCharArrayAndWipe(keys[0]);

			return;
		}
	}
};
}
