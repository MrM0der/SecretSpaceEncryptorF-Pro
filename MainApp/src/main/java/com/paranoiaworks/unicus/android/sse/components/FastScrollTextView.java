package com.paranoiaworks.unicus.android.sse.components;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import ext.com.mixiaoxiao.fastscroll.FastScrollDelegate.FastScrollable;
import ext.com.mixiaoxiao.fastscroll.FastScrollDelegate;

/**
 * Implementation of https://github.com/Mixiaoxiao/FastScroll-Everywhere FastScrollWebView
 *
 * @author Paranoia Works based on Mixiaoxiao
 * @version 1.0.1
 */
@SuppressLint("AppCompatCustomView")
public class FastScrollTextView extends TextViewMod implements FastScrollable {

	private FastScrollDelegate mFastScrollDelegate;

	// ===========================================================
	// Constructors
	// ===========================================================

	public FastScrollTextView(Context context) {
		super(context);
		createFastScrollDelegate(context);
	}

	public FastScrollTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		createFastScrollDelegate(context);
	}

	public FastScrollTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		createFastScrollDelegate(context);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public FastScrollTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		createFastScrollDelegate(context);
	}

	// ===========================================================
	// createFastScrollDelegate
	// ===========================================================

	private void createFastScrollDelegate(Context context) {
		mFastScrollDelegate = new FastScrollDelegate.Builder(this).build();
	}

	// ===========================================================
	// Delegate
	// ===========================================================

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mFastScrollDelegate.onTouchEvent(event)) {
			return true;
		}
		try {
			return super.onTouchEvent(event);
		} catch (Exception e) {
			return true;
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		mFastScrollDelegate.onAttachedToWindow();
	}

	@Override
	protected void onVisibilityChanged(View changedView, int visibility) {
		super.onVisibilityChanged(changedView, visibility);
		if (mFastScrollDelegate != null) {
			mFastScrollDelegate.onVisibilityChanged(changedView, visibility);
		}
	}

	@Override
	protected void onWindowVisibilityChanged(int visibility) {
		super.onWindowVisibilityChanged(visibility);
		mFastScrollDelegate.onWindowVisibilityChanged(visibility);
	}

	@Override
	protected boolean awakenScrollBars() {
		return mFastScrollDelegate.awakenScrollBars();
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		mFastScrollDelegate.dispatchDrawOver(canvas);
	}

	// ===========================================================
	// FastScrollable IMPL, ViewInternalSuperMethods
	// ===========================================================

	@Override
	public void superOnTouchEvent(MotionEvent event) {
		super.onTouchEvent(event);
	}

	@Override
	public int superComputeVerticalScrollExtent() {
		return super.computeVerticalScrollExtent();
	}

	@Override
	public int superComputeVerticalScrollOffset() {
		return super.computeVerticalScrollOffset();
	}

	@Override
	public int superComputeVerticalScrollRange() {
		return super.computeVerticalScrollRange();
	}

	@Override
	public View getFastScrollableView() {
		return this;
	}

	/**
	 * @deprecated use {@link #getFastScrollDelegate()} instead
	 */
	public FastScrollDelegate getDelegate() {
		return getFastScrollDelegate();
	}

	@Override
	public FastScrollDelegate getFastScrollDelegate() {
		return mFastScrollDelegate;
	}

	@Override
	public void setNewFastScrollDelegate(FastScrollDelegate newDelegate) {
		if (newDelegate == null) {
			throw new IllegalArgumentException("setNewFastScrollDelegate must NOT be NULL.");
		}
		mFastScrollDelegate.onDetachedFromWindow();
		mFastScrollDelegate = newDelegate;
		newDelegate.onAttachedToWindow();
	}
}
