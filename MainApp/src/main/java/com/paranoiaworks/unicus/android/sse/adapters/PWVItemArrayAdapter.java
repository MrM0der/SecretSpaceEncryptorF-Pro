package com.paranoiaworks.unicus.android.sse.adapters;

import android.app.Activity;
import android.content.ClipData;
import android.graphics.Bitmap;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.paranoiaworks.unicus.android.sse.R;
import com.paranoiaworks.unicus.android.sse.components.TextViewMod;
import com.paranoiaworks.unicus.android.sse.dao.VaultFolderV2;
import com.paranoiaworks.unicus.android.sse.dao.VaultItemV2;
import com.paranoiaworks.unicus.android.sse.misc.StringSentinel;
import com.paranoiaworks.unicus.android.sse.utils.ColorHelper;
import com.paranoiaworks.unicus.android.sse.utils.Encryptor;
import com.paranoiaworks.unicus.android.sse.utils.Helpers;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ext.com.andraskindler.quickscroll.Scrollable;

/**
 * Provides "Password Item Row" appearance (for the Items section of Password Vault)
 * 
 * @author Paranoia Works
 * @version 1.2.2
 */
public class PWVItemArrayAdapter extends BaseAdapter implements Scrollable, Filterable {
	private final Activity context;
	private final List<VaultItemV2> items;
	private List<VaultItemV2> filteredItems;
	private float customFontSizeMultiplier = 1.0F;
	private boolean dataHasChanged = false;
	
	public PWVItemArrayAdapter(Activity context, List<VaultItemV2> items)
	{
		this.context = context;
		this.items = items;
		this.filteredItems = items;
	}
	
    public int getCount() {
        return filteredItems.size();
    }
    
    public Object getItem(int position) {
    	return filteredItems.get(position);
    }
    
    public long getItemId(int position) {
        return position;
    }
	
	static class ViewHolder {
		public ImageView itemIcon;
		public TextViewMod itemName;
		public TextViewMod itemAccount;
		public TextViewMod itemPassword;
		public TextViewMod itemDate;
		public RelativeLayout passDateLine;
		public RelativeLayout mainLayout;
		public ViewGroup namePaswordDelimeter;
		public double originalTextSize = -1;
		public int originalColor;
		public int originalPasswordColor;
		public RelativeLayout.LayoutParams originalPassDateLineParams;
		public Drawable originalIconBg;
		
		public boolean lastRenderSpecial = false;
	}
	
	public void setFontSizeMultiplier(float multiplier)
	{
		customFontSizeMultiplier = multiplier;
	}

	@Override
	public synchronized View getView(int position, View convertView, ViewGroup parent) {

		ViewHolder holder;
		View rowView = convertView;
		if (rowView == null) {
			LayoutInflater inflater = context.getLayoutInflater();
			rowView = inflater.inflate(R.layout.lc_passwordvault_item_listrow, null, true);
			holder = new ViewHolder();
			holder.itemName = (TextViewMod) rowView.findViewById(R.id.PWVI_itemName);
			holder.itemAccount = (TextViewMod) rowView.findViewById(R.id.PWVI_itemAccount);
			holder.itemPassword = (TextViewMod) rowView.findViewById(R.id.PWVI_itemPassword);
			holder.itemDate = (TextViewMod) rowView.findViewById(R.id.PWVI_itemDate);
			holder.itemIcon = (ImageView) rowView.findViewById(R.id.PWVI_itemIcon);
			holder.passDateLine = (RelativeLayout) rowView.findViewById(R.id.PWVI_passDateLine);
			holder.mainLayout = (RelativeLayout) rowView.findViewById(R.id.PWVI_RowLayout);
			holder.namePaswordDelimeter = (ViewGroup) rowView.findViewById(R.id.PWVI_namePasswordDelimiter);
			
			if (holder.originalTextSize < 0)
			{
				//float den = rowView.getResources().getDisplayMetrics().density;
				holder.originalTextSize = holder.itemName.getTextSize();
				holder.itemName.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float)(holder.originalTextSize * 1.32 * customFontSizeMultiplier));
				holder.itemAccount.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float)(holder.originalTextSize * 1.28 * customFontSizeMultiplier));
				holder.itemPassword.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float)(holder.originalTextSize * 1.28 * customFontSizeMultiplier));
				holder.originalColor = rowView.getResources().getColor(R.color.white_file);
				holder.originalPasswordColor = rowView.getResources().getColor(R.color.lightblue_file);
				holder.originalPassDateLineParams = (LayoutParams) holder.passDateLine.getLayoutParams();
				holder.originalIconBg = holder.itemIcon.getBackground();
				resetView(holder, rowView);
			}
			
			rowView.setTag(holder);
			
		} else {
			holder = (ViewHolder) rowView.getTag();
		}
			
		VaultItemV2 tempItem = filteredItems.get(position);
		holder.itemName.setTag(tempItem.getItemSecurityHash());
		if (holder.lastRenderSpecial) resetView(holder, rowView);
		
		if (!tempItem.isSpecial())
		{
			holder.itemName.setCharArrayAndWipe(tempItem.getItemName());
			if(filteredItems.get(filteredItems.size() - 1).getSpecialCode() == VaultItemV2.SPEC_SEARCH) {
				VaultFolderV2 parentFolder = tempItem.getParentFolder();
				if(parentFolder != null) {
					Drawable dr = context.getResources().getDrawable(ColorHelper.getColorBean(parentFolder.getColorCode()).folderIconRId);
					holder.itemName.setCompoundDrawablesWithIntrinsicBounds(null, null, getMiniatureForTextView(dr, holder.itemName), null);
				}
			}
			else holder.itemName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			if(tempItem.isExtendedItem() && Helpers.trim(tempItem.getItemAccount()).length > 0) {
				holder.itemAccount.setVisibility(View.VISIBLE);
				holder.namePaswordDelimeter.setVisibility(View.GONE);
				holder.itemAccount.setCharArrayAndWipe(tempItem.getItemAccount());
			}
			else {
				holder.itemAccount.setVisibility(View.GONE);
				holder.namePaswordDelimeter.setVisibility(View.VISIBLE);
			}
			
			char[] tempPassword = Helpers.trim(tempItem.getItemPassword());
			if(tempItem.isSelected())
			{				
				if(tempPassword.length == 0)
					tempPassword = tempItem.getItemComment();
				holder.mainLayout.setBackgroundResource(R.drawable.d_itemicon_bg);
				holder.itemPassword.setCharArrayAndWipe(tempPassword);
				holder.lastRenderSpecial = true;
				holder.itemIcon.setBackgroundResource(R.drawable.null_image);
				holder.itemDate.setText("");
			}		
			else
			{
				if(tempPassword.length == 0)
					holder.itemPassword.setCharArrayAndWipe(tempItem.getItemComment());
				else
					holder.itemPassword.setText("********");
				holder.itemDate.setText(Helpers.getFormatedDate(tempItem.getDateModified(), rowView.getResources().getConfiguration().locale));
			}
			if(!tempItem.isKemItem()) holder.itemIcon.setImageResource(ColorHelper.getColorBean(tempItem.getColorCode()).itemIconRId);
			else holder.itemIcon.setImageResource(R.drawable.password_item_kem);
		} else {
			int code = tempItem.getSpecialCode();
			holder.itemAccount.setVisibility(View.GONE);
			holder.namePaswordDelimeter.setVisibility(View.VISIBLE);
			holder.itemName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			switch (code) 
	        {
		        case VaultItemV2.SPEC_GOBACKITEM:
		        	Shader textShader = new LinearGradient(0, 0, 0, holder.itemName.getTextSize(), 
		        			new int[]{tempItem.getColorCode(), holder.originalColor},
		        			new float[]{0, 1}, TileMode.CLAMP);
		        	
		        	holder.itemName.setCharArrayAndWipe(tempItem.getItemName());
		        	holder.itemName.getPaint().setShader(textShader);
		        	holder.itemIcon.setImageResource(R.drawable.item_back);
		        	holder.itemIcon.setBackgroundResource(R.drawable.null_image);
		        	holder.itemPassword.setText("");
		        	holder.itemDate.setText("");
		        	holder.passDateLine.setLayoutParams(new LayoutParams(0, 0));
		        	rowView.setBackgroundResource(R.drawable.d_filerow_a);
					if(filteredItems.get(filteredItems.size() - 1).getSpecialCode() != VaultItemV2.SPEC_SEARCH) {
						Drawable dr = context.getResources().getDrawable(ColorHelper.getColorBean(tempItem.getColorCode()).folderIconRId);
						holder.itemName.setCompoundDrawablesWithIntrinsicBounds(null, null, getMiniatureForTextView(dr, holder.itemName), null);
					}
		        	break;
		        case VaultItemV2.SPEC_NEWITEM:
		        	holder.itemName.setText(rowView.getResources().getString(R.string.pwv_newItem_text));
		        	holder.itemIcon.setImageResource(R.drawable.add_new);
		        	holder.itemIcon.setBackgroundResource(R.drawable.null_image);
		        	holder.itemPassword.setText("");
		        	holder.passDateLine.setLayoutParams(new LayoutParams(0, 0));
		        	rowView.setBackgroundResource(R.drawable.d_filerow_a);
		        	break;
				case VaultItemV2.SPEC_SEARCH:
					holder.itemName.setText(rowView.getResources().getString(R.string.common_search_Text));
					holder.itemIcon.setImageResource(R.drawable.item_search);
					holder.itemIcon.setBackgroundResource(R.drawable.null_image);
					holder.itemPassword.setText("");
					holder.passDateLine.setLayoutParams(new LayoutParams(0, 0));
					rowView.setBackgroundResource(R.drawable.d_filerow_a);
					break;
	        }
			
			holder.lastRenderSpecial = true;
		}

		rowView.getBackground().setDither(true);
		return rowView;
	}
	
	private void resetView(ViewHolder holder, View rowView)
	{
		rowView.setBackgroundResource(R.drawable.d_filerow);
		holder.itemName.setTextColor(holder.originalColor);
		holder.itemName.getPaint().setShader(null);
		holder.itemPassword.setTextColor(holder.originalPasswordColor);
		holder.itemDate.setTextColor(holder.originalColor);
		holder.passDateLine.setLayoutParams(holder.originalPassDateLineParams);
		holder.itemIcon.setBackgroundDrawable(holder.originalIconBg);
		holder.mainLayout.setBackgroundResource(R.drawable.d_null_semitrans);
		holder.lastRenderSpecial = false;
	}
	
	@Override
	public void notifyDataSetChanged()
	{
		super.notifyDataSetChanged();
		dataHasChanged = true;
	}
	
	public String getIndicatorForPosition(int childposition, int groupposition) {
		VaultItemV2 tempItem = filteredItems.get(childposition);
		if(tempItem.isSpecial()) {
			if(tempItem.getSpecialCode() == VaultItemV2.SPEC_NEWITEM || tempItem.getSpecialCode() == VaultItemV2.SPEC_SEARCH) return "+:Y";
			else return "..:Y";
		}
		return Character.toUpperCase(tempItem.getItemName()[0]) + ":W";
	}

	public int getScrollPosition(int childposition, int groupposition) {
		return childposition;
	}
	
	public synchronized boolean hasDataChanged()
	{
		boolean temp = dataHasChanged;
		dataHasChanged = false;
		return temp;
	}

	public Filter getFilter() {
		return new ItemFilter();
	}

	private Drawable getMiniatureForTextView(Drawable original, TextView textView)
	{
		Bitmap bitmap = ((BitmapDrawable) original).getBitmap();
		int size = textView.getLineHeight();
		return new BitmapDrawable(context.getResources(), Bitmap.createScaledBitmap(bitmap, size, size, true));
	}

	public class ItemFilter extends Filter
	{
		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			filteredItems = (List<VaultItemV2>)results.values;
			notifyDataSetChanged();
		}

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();
			List<VaultItemV2> filteredList= new ArrayList<VaultItemV2>();
			if (constraint == null || constraint.length() == 0) {
				results.values = items;
				results.count = items.size();
			}
			else {
				for (int i = 0; i < items.size(); i++) {
					VaultItemV2 currentItem = items.get(i);
					StringSentinel name = currentItem.getItemNameSS();
					StringSentinel account = currentItem.getItemAccountSS();
					char[] constraintCA = Helpers.toLowerCaseCharArray(constraint);
					if (name.searchIndexOf(constraintCA) > -1 || (account != null && account.searchIndexOf(constraintCA) > -1) || currentItem.isSpecial()) {
						filteredList.add(currentItem);
					}
					Arrays.fill(constraintCA, '\u0000');
				}
				results.values = filteredList;
				results.count = filteredList.size();
			}
			return results;
		}
	}
}