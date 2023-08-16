package com.paranoiaworks.unicus.android.sse.components;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

/**
 * Full size (no scrollbar) ListView
 * 
 * @author Paranoia Works
 * @version 1.0.0
 */

public class ListViewNS extends ListView {

	public ListViewNS(Context context) {
		super(context);
	}

	public ListViewNS(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public ListViewNS(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,
				MeasureSpec.AT_MOST);
		super.onMeasure(widthMeasureSpec, expandSpec);
	}
}
