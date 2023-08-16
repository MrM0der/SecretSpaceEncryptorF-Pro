package com.paranoiaworks.unicus.android.sse;

import android.app.Application;

/**
 * Basic Configuration
 * 
 * @author Paranoia Works
 * @version 1.1.2
 */
public class StaticApp extends Application {

    public static final String VERSION_FLAVOR = "F";
    public static final boolean SHOW_UPGRADE_FEATURES = true;
    public static final int SHOW_REVIEW_ABOUT = 0;
    public static final String LICENSE_LEVEL_TAG = "LICENSE_LEVEL";

    @Override
    public void onCreate() {
        super.onCreate();
    }
}

