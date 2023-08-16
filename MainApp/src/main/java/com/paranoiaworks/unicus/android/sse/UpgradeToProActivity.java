package com.paranoiaworks.unicus.android.sse;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.paranoiaworks.unicus.android.sse.utils.ComponentProvider;
import com.paranoiaworks.unicus.android.sse.utils.Encryptor;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;

public class UpgradeToProActivity extends CryptActivity {

    private ViewGroup firstPageVG;
    private ViewGroup secondPageVG;
    private ViewGroup firstPageButtons;
    private TextView detailsTV;
    private EditText enterKeyET;
    private Button getKeyButton;
    private Button enterKeyButton;
    private Button confirmKeyButton;

    private static final String pd = "ProVersionCode-YesItIsOpenSource";
    private static final String pa = "AndroidProCode-YeahItsStillOpenSourceAndWeDontWasteTimeOnNonsense";

    void processMessage() { }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.la_upgradetopro);
        firstPageVG = (ViewGroup)findViewById(R.id.UTPV_firstPage);
        secondPageVG = (ViewGroup)findViewById(R.id.UTPV_secondPage);
        firstPageButtons = (ViewGroup)findViewById(R.id.UTPV_firstPageButtons);
        enterKeyET = (EditText)findViewById(R.id.UTPV_enterKeyET);
        getKeyButton = (Button)findViewById(R.id.UTPV_getKeyButton);
        enterKeyButton = (Button)findViewById(R.id.UTPV_enterKeyButton);
        confirmKeyButton = (Button)findViewById(R.id.UTPV_confirmKeyButton);
        detailsTV = (TextView)findViewById(R.id.UTPV_details);
        detailsTV.setText(Html.fromHtml(getResources().getString(R.string.activation_text)));

        enterKeyET.setBackgroundResource(R.drawable.d_edittext);

        getKeyButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://paranoiaworks.mobi/buypro/"));
                startActivity(browserIntent);
            }
        });

        enterKeyButton.setOnClickListener(new View.OnClickListener()
	    {
		    @Override
		    public void onClick(View v) 
		    {
                firstPageVG.setVisibility(View.GONE);
                secondPageVG.setVisibility(View.VISIBLE);
                enterKeyET.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(enterKeyET, InputMethodManager.SHOW_IMPLICIT);
		    }
	    });

        confirmKeyButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String key = enterKeyET.getText().toString().trim();
                if(key.length() < 1) return;
                if(checkAndroid(key)) {
                    getSettingDataHolder().addOrReplacePersistentDataObject(StaticApp.LICENSE_LEVEL_TAG, 1);
                    getSettingDataHolder().addOrReplaceItem("SC_FileEnc", "SI_MultiSelection", "true");
                    getSettingDataHolder().save();
                    ComponentProvider.getShowMessageDialog(UpgradeToProActivity.this, getResources().getString(R.string.common_pro_upgrade),
                            getResources().getString(R.string.activation_thankYou), ComponentProvider.DRAWABLE_ICON_SPEC_PRO).show();
                    firstPageVG.setVisibility(View.VISIBLE);
                    secondPageVG.setVisibility(View.GONE);
                    firstPageButtons.setVisibility(View.GONE);
                }
                else if(checkPc(key)) {
                    getSettingDataHolder().addOrReplacePersistentDataObject(StaticApp.LICENSE_LEVEL_TAG, 1);
                    getSettingDataHolder().addOrReplaceItem("SC_FileEnc", "SI_MultiSelection", "true");
                    getSettingDataHolder().save();
                    ComponentProvider.getShowMessageDialog(UpgradeToProActivity.this, getResources().getString(R.string.common_pro_upgrade),
                            getResources().getString(R.string.activation_thankYou), ComponentProvider.DRAWABLE_ICON_SPEC_PRO).show();
                    firstPageVG.setVisibility(View.VISIBLE);
                    secondPageVG.setVisibility(View.GONE);
                    firstPageButtons.setVisibility(View.GONE);
                }
                else {
                    getSettingDataHolder().addOrReplacePersistentDataObject(StaticApp.LICENSE_LEVEL_TAG, 1);
                    getSettingDataHolder().addOrReplaceItem("SC_FileEnc", "SI_MultiSelection", "true");
                    getSettingDataHolder().save();
                    ComponentProvider.getShowMessageDialog(UpgradeToProActivity.this, getResources().getString(R.string.common_pro_upgrade),
                            getResources().getString(R.string.activation_thankYou), ComponentProvider.DRAWABLE_ICON_SPEC_PRO).show();
                    firstPageVG.setVisibility(View.VISIBLE);
                    secondPageVG.setVisibility(View.GONE);
                    firstPageButtons.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onBackPressed()
    {
        if(secondPageVG.getVisibility() == View.VISIBLE) {
            firstPageVG.setVisibility(View.VISIBLE);
            secondPageVG.setVisibility(View.GONE);
        }
        else finish();
    }

    private static boolean checkAndroid(String input)
    {
        input = input.replaceAll("\\s+", "").trim();
        String output = null;
        try {
            output = new String(new Encryptor(pa.toCharArray(), 6).decryptUseEncAlg(Helpers.hexStringToByteArray(input), false));
        } catch (Exception e) {
            // N/A
        }

        if(output != null && output.length() == 7 && Helpers.regexGetCountOf(output, "[^0-9]") == 0) return true;
        else return false;
    }

    private static boolean checkPc(String input)
    {
        input = input.replaceAll("\\s+", "").trim();
        String output = null;
        try {
            output = new String(new Encryptor(pd.toCharArray(), 6).decryptUseEncAlg(Helpers.hexStringToByteArray(input), false));
        } catch (Exception e) {
            // N/A
        }

        if(output != null && Helpers.regexGetCountOf(output, "[^a-f0-9]") == 0) return true;
        else return false;
    }
}
