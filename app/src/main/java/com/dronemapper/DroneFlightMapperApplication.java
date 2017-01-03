package com.dronemapper;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import dji.sdk.flightcontroller.DJIFlightController;
import dji.sdk.products.DJIAircraft;
import dji.sdk.sdkmanager.DJISDKManager;

import static com.dronemapper.util.Helper.showToast;


public class DroneFlightMapperApplication extends Application {

    public static final String FLAG_CONNECTION_CHANGE = "dronemapper_connection_change";

    private static DJIBaseProduct mProduct;
    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            initSDK();
        }
    };
    private Handler mHandler;

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

    private DJISDKManager.DJISDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.DJISDKManagerCallback() {
        //Listens to the SDK registration result
        @Override
        public void onGetRegisteredResult(final DJIError error) {
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct();
                showToast(getApplicationContext(), error.getDescription());
                Toast.makeText(getApplicationContext(), error.getDescription(), Toast.LENGTH_LONG).show();
            } else {
                showToast(getApplicationContext(), error.getDescription());
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
        }
        return camera;
    }

    public static synchronized DJIFlightController getFlightControllerInstance() {
        if (getAircraftInstance() == null) return null;
        DJIFlightController controller = null;
        if (getAircraftInstance() instanceof DJIAircraft) {
            controller = getAircraftInstance().getFlightController();
        }
        return controller;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter(SplashActivity.FLAG_PERMISSIONS_GRANTED);
        registerReceiver(mReceiver, filter);
    }

    public void initSDK() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mHandler = new Handler(Looper.getMainLooper());
                DJISDKManager.getInstance().initSDKManager(DroneFlightMapperApplication.this, mDJISDKManagerCallback);
            }
        }, 250);
    }

    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }

    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            sendBroadcast(intent);
        }
    };
}
