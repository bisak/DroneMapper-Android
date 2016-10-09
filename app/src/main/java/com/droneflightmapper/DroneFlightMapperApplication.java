package com.droneflightmapper;

import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseComponent.DJIComponentListener;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIBaseProduct.DJIBaseProductListener;
import dji.sdk.base.DJIBaseProduct.DJIComponentKey;
import dji.sdk.camera.DJICamera;
import dji.sdk.products.DJIAircraft;
import dji.sdk.products.DJIHandHeld;
import dji.sdk.sdkmanager.DJISDKManager;

public class DroneFlightMapperApplication extends Application {

    public static final String FLAG_CONNECTION_CHANGE = "fpv_tutorial_connection_change";

    private static DJIBaseProduct mProduct;

    private Handler mHandler;
    private Runnable updateRunnable = new Runnable() {

        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            sendBroadcast(intent);
        }
    };
    private DJIComponentListener mDJIComponentListener = new DJIComponentListener() {

        @Override
        public void onComponentConnectivityChanged(boolean isConnected) {
            notifyStatusChange();
        }

    };
    private DJIBaseProductListener mDJIBaseProductListener = new DJIBaseProductListener() {

        @Override
        public void onComponentChange(DJIComponentKey key, DJIBaseComponent oldComponent, DJIBaseComponent newComponent) {

            if (newComponent != null) {
                newComponent.setDJIComponentListener(mDJIComponentListener);
            }
            notifyStatusChange();
        }

        @Override
        public void onProductConnectivityChanged(boolean isConnected) {

            notifyStatusChange();
        }

    };
    /**
     * When starting SDK services, an instance of interface DJISDKManager.DJISDKManagerCallback will be used to listen to
     * the SDK Registration result and the product changing.
     */
    private DJISDKManager.DJISDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.DJISDKManagerCallback() {

        //Listens to the SDK registration result
        @Override
        public void onGetRegisteredResult(final DJIError error) {

            if (error == DJISDKError.REGISTRATION_SUCCESS) {

                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), error.getDescription(), Toast.LENGTH_SHORT).show();
                    }
                });

                DJISDKManager.getInstance().startConnectionToProduct();

            } else {

                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), error.getDescription(), Toast.LENGTH_LONG).show();
                    }
                });

            }
            Log.e("TAG", error.toString());
        }

        //Listens to the connected product changing, including two parts, component changing or product connection changing.
        @Override
        public void onProductChanged(DJIBaseProduct oldProduct, DJIBaseProduct newProduct) {

            mProduct = newProduct;
            if (mProduct != null) {
                mProduct.setDJIBaseProductListener(mDJIBaseProductListener);
            }

            notifyStatusChange();
        }
    };

    /**
     * This function is used to get the instance of DJIBaseProduct.
     * If no product is connected, it returns null.
     */
    public static synchronized DJIBaseProduct getProductInstance() {
        if (null == mProduct) {
            mProduct = DJISDKManager.getInstance().getDJIProduct();
        }
        return mProduct;
    }

    public static boolean isAircraftConnected() {
        return getProductInstance() != null && getProductInstance() instanceof DJIAircraft;
    }


    public static synchronized DJIAircraft getAircraftInstance() {
        if (!isAircraftConnected()) return null;
        return (DJIAircraft) getProductInstance();
    }

    public static synchronized DJICamera getCameraInstance() {

        if (getProductInstance() == null) return null;

        DJICamera camera = null;

        if (getProductInstance() instanceof DJIAircraft) {
            camera = getProductInstance().getCamera();

        } else if (getProductInstance() instanceof DJIHandHeld) {
            camera = getProductInstance().getCamera();
        }

        return camera;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(Looper.getMainLooper());
        //This is used to start SDK services and initiate SDK.
        DJISDKManager.getInstance().initSDKManager(this, mDJISDKManagerCallback);
    }

    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }

}
