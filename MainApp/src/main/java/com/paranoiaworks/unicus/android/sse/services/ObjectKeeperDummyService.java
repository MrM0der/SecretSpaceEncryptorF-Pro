package com.paranoiaworks.unicus.android.sse.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.paranoiaworks.unicus.android.sse.utils.Helpers;

/**
 * Dummy service for temporary object storage (availability is not 100%, but better than compromising security);
 * When other methods are not safe (not proud of this, but they are not any "official" safe methods for passing objects and saving states in Android);
 * It is currently used to backup the state of the application during external file picking in the text encryptor;
 *
 * @author Paranoia Works
 * @version 1.0.1
 */

public class ObjectKeeperDummyService extends Service
{
    public static final int APP_ID_FE = 0;
    public static final int APP_ID_TE = 1;
    public static final int APP_ID_PWV = 2;

    private static Object[] temporaryObjectStorage = new Object[2];
    private static boolean isRunning = false;

    public ObjectKeeperDummyService() {
        super();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return  null;
    }

    @Override
    public void onCreate() {
        //System.out.println("ObjectKeeperDummyService: " + "Service was Created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;
        //System.out.println("ObjectKeeperDummyService: " + "Service Started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        //System.out.println("ObjectKeeperDummyService: " + "Service Stopped");
    }

    public static boolean isRunning() {
        return isRunning;
    }

    public synchronized static void setTemporaryObject(Object object, byte[] verificationTag, int appId)
    {
        Object[] appTempObject = new Object[2];

        appTempObject[0] = object;
        appTempObject[1] = verificationTag;

        temporaryObjectStorage[appId] = appTempObject;
    }

    public static Object getTemporaryObject(byte[] verificationTag, int appId)
    {
        try {
            Object[] appTempObject = (Object[])temporaryObjectStorage[appId];

            if(appTempObject != null && Helpers.isEqualTimeConstant(verificationTag, (byte[])appTempObject[1]))
                return appTempObject[0];
            else return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static void removeTemporaryObject(int appId)
    {
        temporaryObjectStorage[appId] = null;
    }
}
