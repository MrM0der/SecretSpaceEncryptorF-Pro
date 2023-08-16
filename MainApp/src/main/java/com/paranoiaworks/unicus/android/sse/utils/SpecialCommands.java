package com.paranoiaworks.unicus.android.sse.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.paranoiaworks.unicus.android.sse.CryptActivity;
import com.paranoiaworks.unicus.android.sse.R;
import com.paranoiaworks.unicus.android.sse.StaticApp;
import com.paranoiaworks.unicus.android.sse.dao.SettingDataHolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 * Various tools for working with "Special Commands"
 *
 * @author Paranoia Works
 * @version 1.0.2
 */

public class SpecialCommands {

    public static Dialog getSpecialCommandDialog(final Context c)
    {
        final Dialog dialog = new Dialog(c);
        final SettingDataHolder sdh = ((CryptActivity)Helpers.getActivityFromContext(c)).getSettingDataHolder();
        final DBHelper dbHelper = sdh.getDBHelper();

        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.lc_settext_dialog);

        final TextView titleView = (TextView)dialog.findViewById(R.id.STD_Title);
        titleView.setText(c.getResources().getString(R.string.SI_SpecialCommands));
        final EditText enteredText = (android.widget.EditText)dialog.findViewById(R.id.enteredText);

        // Confirm Button
        Button confirmButton = (Button)dialog.findViewById(R.id.okButton);
        confirmButton.setText(c.getResources().getString(R.string.common_confirm_text));
        confirmButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String command = enteredText.getText().toString().replaceAll("\\s+", " ").trim().toLowerCase();
                String[] commandParsed = command.split(" ");
                if(command.isEmpty()) return;

                try {
                    if(command.equals("import settings") && StaticApp.VERSION_FLAVOR.startsWith("F"))
                    {
                        try {
                            File importFile = new File(Helpers.getImportExportPath(sdh) + File.separator + "settings.exp");
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();

                            FileInputStream fis = new FileInputStream(importFile);
                            byte[] buffer = new byte[10240];
                            int noOfBytes = 0;

                            while ((noOfBytes = fis.read(buffer)) != -1) {
                                bos.write(buffer, 0, noOfBytes);
                            }
                            fis.close();

                            byte[] sdhRaw = bos.toByteArray();
                            SettingDataHolder newSdh = (SettingDataHolder)Encryptor.decompressObjectLZMA(sdhRaw);
                            sdh.replaceInstance(newSdh);

                            showStandardOkDialogAndCancel(c, dialog);

                        } catch (Exception e) {
                            throw  new IllegalArgumentException();
                        }
                    }
                    else if(command.equals("export settings"))
                    {
                        try {
                            sdh.save();
                            StringBuffer dbhs = new StringBuffer();
                            ArrayList otherData = new ArrayList();
                            byte[] sdhRaw = dbHelper.getBlobData(SettingDataHolder.SDH_DBPREFIX, dbhs, otherData);

                            FileOutputStream out = new FileOutputStream(new File(Helpers.getImportExportPath(sdh) + File.separator + "settings.exp"));
                            out.write(sdhRaw);
                            out.flush();
                            out.close();

                            showStandardOkDialogAndCancel(c, dialog);

                        } catch (Exception e) {
                            throw  new IllegalArgumentException();
                        }
                    }
                    else if(commandParsed.length > 1)
                    {
                        if(commandParsed[0].equals("enable"))
                        {
                            if(commandParsed[1].equals("nc"))
                            {
                                sdh.addOrReplaceItem("SC_Common", "SI_NativeCodeDisable", "false");
                                sdh.save();

                                showStandardOkDialogAndCancel(c, dialog);
                            }
                            else if(commandParsed[1].equals("debuginfo"))
                            {
                                sdh.addOrReplacePersistentDataObject("SpecialCommand:DebugInfo", true);
                                sdh.save();

                                showStandardOkDialogAndCancel(c, dialog);
                            }
                            else throw new IllegalArgumentException();
                        }
                        else if(commandParsed[0].equals("disable"))
                        {
                            if(commandParsed[1].equals("nc"))
                            {
                                sdh.addOrReplaceItem("SC_Common", "SI_NativeCodeDisable", "true");
                                sdh.save();

                                showStandardOkDialogAndCancel(c, dialog);
                            }
                            else if(commandParsed[1].equals("debuginfo"))
                            {
                                sdh.addOrReplacePersistentDataObject("SpecialCommand:DebugInfo", false);
                                sdh.save();

                                showStandardOkDialogAndCancel(c, dialog);
                            }
                            else throw new IllegalArgumentException();
                        }
                        else throw new IllegalArgumentException();
                    }
                    else throw new IllegalArgumentException();

                } catch (IllegalArgumentException e) {
                    showDialog(c.getResources().getString(R.string.common_error_text) + ": unknown command", c, ComponentProvider.DRAWABLE_ICON_CANCEL);
                }
            }
        });

        // Cancel Button
        Button cancelButton = (Button)dialog.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dialog.cancel();
            }
        });

        enteredText.requestFocus();

        return dialog;
    }

    private static void showStandardOkDialogAndCancel(Context c, Dialog d)
    {
        showDialog(c.getResources().getString(R.string.SI_SpecialCommands) + ": "
                + c.getResources().getString(R.string.common_ok_text) , c, ComponentProvider.DRAWABLE_ICON_OK);
        d.cancel();
    }

    private static void showDialog(String message, Context c, int iconCode)
    {
        ComponentProvider.getShowMessageDialog(c,
                c.getResources().getString(R.string.common_message_text),
                message,
                iconCode).show();
    }
}
