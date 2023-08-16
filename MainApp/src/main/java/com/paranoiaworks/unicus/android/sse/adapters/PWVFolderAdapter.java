package com.paranoiaworks.unicus.android.sse.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery.LayoutParams;
import android.widget.GridView;
import android.widget.ImageView;

import com.paranoiaworks.unicus.android.sse.R;
import com.paranoiaworks.unicus.android.sse.components.TextViewMod;

import com.paranoiaworks.unicus.android.sse.dao.VaultFolderV2;
import com.paranoiaworks.unicus.android.sse.dao.VaultV2;
import com.paranoiaworks.unicus.android.sse.utils.ColorHelper;

/**
 * Provides "Text under Folder icon" appearance for Vault object
 * 
 * @author Paranoia Works
 * @version 1.1.0
 */
public class PWVFolderAdapter extends BaseAdapter {
    private Context mContext;
    private VaultV2 passwordVault;
    private VaultFolderV2 searchResultsFolder;
    
    public PWVFolderAdapter(Context c, VaultV2 passwordVault) {
        mContext = c;
        this.passwordVault = passwordVault;
    }

    public int getCount() {
        return passwordVault.getFolderCount() + (searchResultsFolder != null ? 1 : 0);
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        TextViewMod textView;
        View layout;
        int colorCode;
        if (convertView == null)
        {       	
        	layout = ((Activity)mContext).getLayoutInflater().inflate(R.layout.lc_icon, null);
        	imageView = (ImageView) layout.findViewById(R.id.iconImagePW);
        	textView = (TextViewMod) layout.findViewById(R.id.iconTextPW);
        	layout.setLayoutParams(new GridView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        } else {
            layout = (View)convertView;
            imageView = (ImageView) layout.findViewById(R.id.iconImagePW);
            textView = (TextViewMod) layout.findViewById(R.id.iconTextPW);
        }
    	
        boolean searchResults = position >= passwordVault.getFolderCount();

        VaultFolderV2 actualFolder = !searchResults ? passwordVault.getFolderByIndex(position) : searchResultsFolder;
        
    	textView.setCharArrayAndWipe(actualFolder.getFolderName());
    	textView.setTag(actualFolder.getFolderSecurityHash());
           	
    	colorCode = actualFolder.getColorCode();
    	if(!searchResults) imageView.setImageResource(ColorHelper.getColorBean(colorCode).folderIconRId);
        else imageView.setImageResource(R.drawable.folder_search);

        return layout;
    }

    public boolean hasSearchResultFolder()
    {
        return this.searchResultsFolder != null;
    }

    public VaultFolderV2 getSearchResultFolder()
    {
        return this.searchResultsFolder;
    }

    public void setSearchResultFolder(VaultFolderV2 searchResultFolder)
    {
        this.searchResultsFolder = searchResultFolder;
    }
}