package com.dronemapper;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.TypefaceProvider;
import com.beardedhen.androidbootstrap.api.defaults.DefaultBootstrapBrand;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.battery.DJIBatteryState;
import dji.common.camera.CameraSystemState;
import dji.common.camera.DJICameraSettingsDef;
import dji.common.error.DJIError;
import dji.common.flightcontroller.DJIFlightControllerCurrentState;
import dji.common.product.Model;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.battery.DJIBattery;
import dji.sdk.camera.DJICamera;
import dji.sdk.camera.DJICamera.CameraReceivedVideoDataCallback;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.DJICompass;
import dji.sdk.flightcontroller.DJIFlightController;
import dji.sdk.flightcontroller.DJIFlightControllerDelegate;
import dji.sdk.products.DJIAircraft;

import static com.dronemapper.R.id.timer;


public class MainActivity extends Activity implements SurfaceTextureListener, OnClickListener {

    private static final long DATA_SEND_MILLISECONDS = 1500;
    private static final String TAG = MainActivity.class.getName();
    protected DJICamera.CameraReceivedVideoDataCallback mReceivedVideoDataCallBack = null;

    protected DJICodecManager mCodecManager = null;
    private DJIFlightController mFlightController;
    private DJICompass mCompass;
    protected TextureView mVideoSurface = null;
    private BootstrapButton mCaptureBtn;
    private BootstrapButton mRecordBtn;
    private BootstrapButton mTrackBtn;
    private TextView mRecordingTime;
    private TextView mHeightDisplay;
    private TextView mDistanceDisplay;
    private TextView mHorizontalSpeedDisplay;
    private TextView mVerticalSpeedDisplay;
    private TextView mBatteryDisplay;
    private DroneLocation droneLocation;
    private DatabaseReference mDatabase;
    private DJICamera mCamera;
    private DJIBaseProduct mProduct;
    private Timer sendTimer;
    private String secondaryChild;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DecimalFormat df = new DecimalFormat("#0.0");
    private double velocityX = 0;
    private double velocityY = 0;
    private double velocityZ = 0;
    private double mHomeLatitude = 181;
    private double mHomeLongitude = 181;
    private boolean isSenderTaskRunning = false;
    private boolean isHomePointSet = false;
    private boolean isRecordButtonToggled = false;
    private boolean isTrackButtonToggled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        TypefaceProvider.registerDefaultIconSets();
        setContentView(R.layout.activity_main);
        initUI();

        try {
            Runtime.getRuntime().exec("logcat -f" + " /sdcard/DroneFlightMapperLog.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCamera = DroneFlightMapperApplication.getCameraInstance();
        mProduct = DroneFlightMapperApplication.getProductInstance();
        mFlightController = ((DJIAircraft) mProduct).getFlightController();
        mCompass = mFlightController.getCompass();
        droneLocation = new DroneLocation();

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();


        // The callback for receiving the raw H264 video data for mCamera live view
        mReceivedVideoDataCallBack = new CameraReceivedVideoDataCallback() {

            @Override
            public void onResult(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    // Send the raw H264 video data to codec manager for decoding
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                } else {
                    Log.e(TAG, "mCodecManager is null");
                }
            }
        };
        if (mCamera != null) {
            mCamera.setDJICameraUpdatedSystemStateCallback(new DJICamera.CameraUpdatedSystemStateCallback() {
                @Override
                public void onResult(CameraSystemState cameraSystemState) {
                    if (null != cameraSystemState) {

                        int recordTime = cameraSystemState.getCurrentVideoRecordingTimeInSeconds();
                        int minutes = (recordTime % 3600) / 60;
                        int seconds = recordTime % 60;

                        final String timeString = String.format("%02d:%02d", minutes, seconds);
                        final boolean isVideoRecording = cameraSystemState.isRecording();

                        MainActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                mRecordingTime.setText(timeString);
                                if (isVideoRecording) {
                                    mRecordingTime.setVisibility(View.VISIBLE);
                                    mRecordBtn.setBootstrapBrand(DefaultBootstrapBrand.SUCCESS);
                                } else {
                                    mRecordingTime.setVisibility(View.INVISIBLE);
                                    mRecordBtn.setBootstrapBrand(DefaultBootstrapBrand.SECONDARY);
                                }
                            }
                        });
                    }
                }
            });

        }

        DroneFlightMapperApplication.getAircraftInstance().getBattery().setBatteryStateUpdateCallback(new DJIBattery.DJIBatteryStateUpdateCallback() {
            @Override
            public void onResult(DJIBatteryState djiBatteryState) {
                final int percentage = djiBatteryState.getBatteryEnergyRemainingPercent();
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBatteryDisplay.setText("Batt: " + String.valueOf(percentage) + "%"); //TODO check text displays not to move when changing values
                    }
                });
            }
        });

        mFlightController.setUpdateSystemStateCallback(new DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback() {
            @Override
            public void onResult(DJIFlightControllerCurrentState state) {
                droneLocation.setLatitude(state.getAircraftLocation().getLatitude());
                droneLocation.setLongitude(state.getAircraftLocation().getLongitude());
                droneLocation.setAltitude(Double.valueOf(df.format(state.getAircraftLocation().getAltitude())));
                velocityX = (double) state.getVelocityX();
                velocityY = (double) state.getVelocityY();
                velocityZ = (double) state.getVelocityZ();
                if (velocityZ != 0) {
                    velocityZ = -velocityZ;
                }
                double speed = Math.sqrt((velocityX * velocityX) + (velocityY * velocityY));
                droneLocation.setSpeed(Double.valueOf(df.format(speed)));
                droneLocation.setTime(new Date().getTime() / 1000);
                droneLocation.setHeading(mCompass.getHeading());
                if (state.isHomePointSet()) {
                    isHomePointSet = true;
                    mHomeLatitude = state.getHomeLocation().getLatitude();
                    mHomeLongitude = state.getHomeLocation().getLongitude();
                    float distanceFromhome = distFrom((float) droneLocation.getLatitude(), (float) droneLocation.getLongitude(), (float) mHomeLatitude, (float) mHomeLongitude);
                    droneLocation.setDistance(Float.valueOf(df.format(distanceFromhome)));
                } else {
                    isHomePointSet = false;
                }

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isHomePointSet) {
                            mDistanceDisplay.setTextColor(Color.GREEN);
                        } else {
                            mDistanceDisplay.setTextColor(Color.RED);
                        }
                        mHorizontalSpeedDisplay.setText("H.S: " + String.valueOf(droneLocation.getSpeed()) + "m/s");
                        mVerticalSpeedDisplay.setText("V.S: " + String.valueOf(df.format(velocityZ)) + "m/s");
                        mHeightDisplay.setText("H: " + String.valueOf(droneLocation.getAltitude() + "m"));
                        mDistanceDisplay.setText("D: " + String.valueOf(droneLocation.getDistance() + "m"));
                    }
                });
            }
        });

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    mTrackBtn.setEnabled(true);
                } else {
                    showToast("Log in in order to track flights!");
                    mTrackBtn.setEnabled(false);
                }
            }
        };

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
                e.printStackTrace();
                if (isSenderTaskRunning) {
                    mDatabase.child("/realtime-flights/").child(secondaryChild).removeValue();
                    sendTimer.cancel();
                    sendTimer.purge();
                    mTrackBtn.setBootstrapBrand(DefaultBootstrapBrand.SECONDARY);
                    isSenderTaskRunning = false;
                }

                System.exit(1);
            }
        });
    }

    protected void onProductChange() {
        initPreviewer();
    }

    @Override
    public void onResume() {
        super.onResume();
        initPreviewer();
        onProductChange();
    }

    @Override
    public void onPause() {
        uninitPreviewer();
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    public void onReturn(View view) {
        this.finish();
    }

    @Override
    protected void onDestroy() {
        uninitPreviewer();
        super.onDestroy();
        if (isSenderTaskRunning) {
            mDatabase.child("/realtime-flights/").child(secondaryChild).removeValue();
            sendTimer.cancel();
            sendTimer.purge();
            mTrackBtn.setBootstrapBrand(DefaultBootstrapBrand.SECONDARY);
            isSenderTaskRunning = false;
        }
        try {
            File file = new File("/sdcard/DroneFlightMapperLog.txt");
            file.delete();
        } catch (Exception e) {
        }

    }

    private void initUI() {
        mVideoSurface = (TextureView) findViewById(R.id.video_previewer_surface);
        mRecordingTime = (TextView) findViewById(timer);
        mHeightDisplay = (TextView) findViewById(R.id.height_display);
        mDistanceDisplay = (TextView) findViewById(R.id.distance_display);
        mHorizontalSpeedDisplay = (TextView) findViewById(R.id.horizontal_speed_display);
        mVerticalSpeedDisplay = (TextView) findViewById(R.id.vertical_speed_display);
        mBatteryDisplay = (TextView) findViewById(R.id.battery_display);
        mCaptureBtn = (BootstrapButton) findViewById(R.id.btn_capture);
        mRecordBtn = (BootstrapButton) findViewById(R.id.btn_record);
        mTrackBtn = (BootstrapButton) findViewById(R.id.track_btn);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        mCaptureBtn.setOnClickListener(this);
        mRecordBtn.setOnClickListener(this);
        mTrackBtn.setOnClickListener(this);
        mRecordingTime.setVisibility(View.INVISIBLE);


    }

    private void initPreviewer() {


        if (mProduct == null || !mProduct.isConnected()) {
            showToast(getString(R.string.disconnected));
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if (!mProduct.getModel().equals(Model.UnknownAircraft)) {
                DJICamera camera = mProduct.getCamera();
                if (camera != null) {
                    // Set the callback
                    camera.setDJICameraReceivedVideoDataCallback(mReceivedVideoDataCallBack);
                }
            }
        }
    }

    private void uninitPreviewer() {
        DJICamera camera = DroneFlightMapperApplication.getCameraInstance();
        if (camera != null) {
            // Reset the callback
            DroneFlightMapperApplication.getCameraInstance().setDJICameraReceivedVideoDataCallback(null);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG, "onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }

        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_capture: {
                if (mCamera != null) {
                    mCamera.getCameraMode(new DJICommonCallbacks.DJICompletionCallbackWith<DJICameraSettingsDef.CameraMode>() {
                        @Override
                        public void onSuccess(DJICameraSettingsDef.CameraMode currentCameraMode) {
                            if (currentCameraMode != DJICameraSettingsDef.CameraMode.ShootPhoto) {
                                switchCameraMode(DJICameraSettingsDef.CameraMode.ShootPhoto);
                            }
                            captureAction();
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            showToast("Error getting mCamera mode");
                        }
                    });
                }
                break;
            }

            case R.id.track_btn: {
                isTrackButtonToggled = !isTrackButtonToggled;
                if (isTrackButtonToggled) {
                    sendTimer = new Timer();
                    sendTimer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            if (!isSenderTaskRunning) {
                                secondaryChild = mDatabase.push().getKey();
                            }
                            mDatabase.child("/realtime-flights/").child(secondaryChild).push().setValue(droneLocation);
                            mDatabase.child("/saved-flights/").child(secondaryChild).push().setValue(droneLocation);
                            isSenderTaskRunning = true;
                        }

                    }, 0, DATA_SEND_MILLISECONDS);
                    mTrackBtn.setBootstrapBrand(DefaultBootstrapBrand.SUCCESS);
                } else {
                    if (isSenderTaskRunning) {
                        mDatabase.child("/realtime-flights/").child(secondaryChild).removeValue();
                        sendTimer.cancel();
                        sendTimer.purge();
                        mTrackBtn.setBootstrapBrand(DefaultBootstrapBrand.SECONDARY);
                        isSenderTaskRunning = false;
                    }
                }
                break;
            }

            case R.id.btn_record: {
                isRecordButtonToggled = !isRecordButtonToggled;
                if (isRecordButtonToggled) {
                    if (mCamera != null) {
                        mCamera.getCameraMode(new DJICommonCallbacks.DJICompletionCallbackWith<DJICameraSettingsDef.CameraMode>() {
                            @Override
                            public void onSuccess(DJICameraSettingsDef.CameraMode currentCameraMode) {
                                if (currentCameraMode != DJICameraSettingsDef.CameraMode.RecordVideo) {
                                    switchCameraMode(DJICameraSettingsDef.CameraMode.RecordVideo);
                                }
                                startRecord();
                            }

                            @Override
                            public void onFailure(DJIError djiError) {
                                showToast("Error getting camera mode");
                            }
                        });
                    }
                } else {
                    stopRecord();
                }
            }

            default:
                break;
        }
    }

    private void switchCameraMode(DJICameraSettingsDef.CameraMode cameraMode) {
        if (mCamera != null) {
            mCamera.setCameraMode(cameraMode, new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        //showToast("Switch Camera Mode Succeeded");
                    } else {
                        showToast(error.getDescription());
                    }
                }
            });
        }
    }

    // Method for taking photo
    private void captureAction() {

        DJICameraSettingsDef.CameraMode cameraMode = DJICameraSettingsDef.CameraMode.ShootPhoto;

        if (mCamera != null) {

            DJICameraSettingsDef.CameraShootPhotoMode photoMode = DJICameraSettingsDef.CameraShootPhotoMode.Single; // Set the mCamera capture mode as Single mode
            mCamera.startShootPhoto(photoMode, new DJICommonCallbacks.DJICompletionCallback() {

                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        showToast("Sucessfully took photo.");
                    }
                }

            }); // Execute the startShootPhoto API
        }
    }

    // Method for starting recording
    private void startRecord() {

        DJICameraSettingsDef.CameraMode cameraMode = DJICameraSettingsDef.CameraMode.RecordVideo;
        if (mCamera != null) {
            mCamera.startRecordVideo(new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        showToast("Started recording video.");
                    }
                }
            }); // Execute the startRecordVideo API
        }
    }

    // Method for stopping recording
    private void stopRecord() {

        if (mCamera != null) {
            mCamera.stopRecordVideo(new DJICommonCallbacks.DJICompletionCallback() {

                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        showToast("Stopped recording video.");
                    }
                }
            }); // Execute the stopRecordVideo API
        }
    }


    public static float distFrom(float lat1, float lng1, float lat2, float lng2) {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        float dist = (float) (earthRadius * c);

        return dist;
    }
}

