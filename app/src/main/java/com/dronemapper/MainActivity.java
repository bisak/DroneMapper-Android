package com.dronemapper;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.MediaActionSound;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.TypefaceProvider;
import com.beardedhen.androidbootstrap.api.defaults.DefaultBootstrapBrand;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Locale;
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
import dji.sdk.flightcontroller.DJIFlightController;
import dji.sdk.flightcontroller.DJIFlightControllerDelegate;

import static com.dronemapper.util.Helper.showToast;
import static com.dronemapper.util.MainActivityHelper.distBetweenCoords;
import static com.dronemapper.util.MainActivityHelper.getTimestamp;
import static com.dronemapper.util.MainActivityHelper.playMediaSound;


public class MainActivity extends Activity implements SurfaceTextureListener, OnClickListener {

    private static final long DATA_SEND_MILLISECONDS = 1500;
    private static final String TAG = MainActivity.class.getName();
    protected DJICamera.CameraReceivedVideoDataCallback mReceivedVideoDataCallBack = null;

    protected DJICodecManager mCodecManager = null;
    private DJIFlightController mFlightController;
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
    private DroneLocation droneLocation = new DroneLocation();
    private DatabaseReference mDatabase;
    private DJICamera mCamera;
    private DJIBaseProduct mProduct;
    private Timer sendTimer;
    private String secondaryChild;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private boolean isTrackingActive = false;
    private boolean isHomePointSet = false;
    private boolean isRecordingVideo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleCrashes();
        initUI();
        initComponents();
        handleCameraRecordingState();
        setLiveVideoCallback();
        setFCCallback();
        setBatteryCallback();
        handleUserStatus();
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
        stopTrackingAction();
    }

    private void initUI() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        TypefaceProvider.registerDefaultIconSets();
        setContentView(R.layout.activity_main);

        mVideoSurface = (TextureView) findViewById(R.id.video_previewer_surface);
        mRecordingTime = (TextView) findViewById(R.id.timer);
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
            showToast(getApplicationContext(), getString(R.string.disconnected));
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if (!mProduct.getModel().equals(Model.UnknownAircraft)) {
                DJICamera camera = DroneFlightMapperApplication.getCameraInstance();
                if (camera != null) {
                    // Set the callback
                    camera.setDJICameraReceivedVideoDataCallback(mReceivedVideoDataCallBack);
                }
            }
        }
    }

    private void uninitPreviewer() {
        if (mCamera != null) {
            // Reset the callback
            mCamera.setDJICameraReceivedVideoDataCallback(null);
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_capture: {
                capturePhoto();
                break;
            }

            case R.id.track_btn: {
                if (!isTrackingActive) {
                    startTrackingAction();
                } else {
                    stopTrackingAction();
                }
                break;

            }

            case R.id.btn_record: {
                if (!isRecordingVideo) {
                    captureVideo();
                } else {
                    stopVideoCapture();
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
                        playMediaSound(MediaActionSound.FOCUS_COMPLETE);
                    } else {
                        showToast(getApplicationContext(), error.getDescription());
                    }
                }
            });
        }
    }

    // Method for taking photo
    private void capturePhotoAction() {
        if (mCamera != null) {
            DJICameraSettingsDef.CameraShootPhotoMode photoMode = DJICameraSettingsDef.CameraShootPhotoMode.Single; // Set the mCamera capture mode as Single mode
            mCamera.startShootPhoto(photoMode, new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        playMediaSound(MediaActionSound.SHUTTER_CLICK);
                    }
                }
            });
        }
    }

    // Method for starting recording
    private void captureVideoAction() {
        if (mCamera != null) {
            mCamera.startRecordVideo(new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        playMediaSound(MediaActionSound.START_VIDEO_RECORDING);
                    } else {
                        showToast(getApplicationContext(), error.getDescription());
                    }
                }
            });
        }
    }

    // Method for stopping recording
    private void stopVideoCapture() {
        if (mCamera != null) {
            mCamera.stopRecordVideo(new DJICommonCallbacks.DJICompletionCallback() {

                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        playMediaSound(MediaActionSound.STOP_VIDEO_RECORDING);
                    } else {
                        showToast(getApplicationContext(), error.getDescription());
                    }
                }
            });
        }
    }

    private void capturePhoto() {
        if (mCamera != null) {
            mCamera.getCameraMode(new DJICommonCallbacks.DJICompletionCallbackWith<DJICameraSettingsDef.CameraMode>() {
                @Override
                public void onSuccess(DJICameraSettingsDef.CameraMode currentCameraMode) {
                    if (currentCameraMode != DJICameraSettingsDef.CameraMode.ShootPhoto) {
                        switchCameraMode(DJICameraSettingsDef.CameraMode.ShootPhoto);
                    }
                    capturePhotoAction();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    showToast(getApplicationContext(), "Error getting camera mode");
                }
            });
        }
    }

    private void captureVideo() {
        if (mCamera != null) {
            mCamera.getCameraMode(new DJICommonCallbacks.DJICompletionCallbackWith<DJICameraSettingsDef.CameraMode>() {
                @Override
                public void onSuccess(DJICameraSettingsDef.CameraMode currentCameraMode) {
                    if (currentCameraMode != DJICameraSettingsDef.CameraMode.RecordVideo) {
                        switchCameraMode(DJICameraSettingsDef.CameraMode.RecordVideo);
                    }
                    captureVideoAction();
                }

                @Override
                public void onFailure(DJIError djiError) {
                    showToast(getApplicationContext(), "Error getting camera mode");
                }
            });
        }
    }

    private void handleCameraRecordingState() {
        if (mCamera != null) {
            mCamera.setDJICameraUpdatedSystemStateCallback(new DJICamera.CameraUpdatedSystemStateCallback() {
                @Override
                public void onResult(CameraSystemState cameraSystemState) {
                    if (null != cameraSystemState) {
                        int recordTime = cameraSystemState.getCurrentVideoRecordingTimeInSeconds();
                        int minutes = (recordTime % 3600) / 60;
                        int seconds = recordTime % 60;
                        final String timeString = String.format("%02d:%02d", minutes, seconds);
                        isRecordingVideo = cameraSystemState.isRecording();
                        MainActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                mRecordingTime.setText(timeString);
                                if (isRecordingVideo) {
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
    }

    public void startTrackingAction() {
        if (!isTrackingActive) {
            sendTimer = new Timer();
            secondaryChild = mDatabase.push().getKey();
            sendTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    mDatabase.child("/realtime-flights/").child(secondaryChild).push().setValue(droneLocation);
                    mDatabase.child("/saved-flights/").child(secondaryChild).push().setValue(droneLocation);
                }

            }, 0, DATA_SEND_MILLISECONDS);
            mTrackBtn.setBootstrapBrand(DefaultBootstrapBrand.SUCCESS);
            isTrackingActive = true;
        }
    }

    public void stopTrackingAction() {
        if (isTrackingActive) {
            mDatabase.child("/realtime-flights/").child(secondaryChild).removeValue();
            sendTimer.cancel();
            sendTimer.purge();
            mTrackBtn.setBootstrapBrand(DefaultBootstrapBrand.SECONDARY);
            isTrackingActive = false;
        }
    }

    public void handleCrashes() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
                e.printStackTrace();
                stopTrackingAction();
                System.exit(1);
            }
        });
    }

    public void setBatteryCallback() {
        DJIBattery battery = DroneFlightMapperApplication.getAircraftInstance().getBattery();
        battery.setBatteryStateUpdateCallback(new DJIBattery.DJIBatteryStateUpdateCallback() {
            @Override
            public void onResult(DJIBatteryState djiBatteryState) {
                final int percentage = djiBatteryState.getBatteryEnergyRemainingPercent();
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBatteryDisplay.setText("Bat: " + String.valueOf(percentage) + "%");
                    }
                });
            }
        });
    }

    public void setLiveVideoCallback() {
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
    }

    public void handleUserStatus() {
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    mTrackBtn.setEnabled(true);
                } else {
                    showToast(getApplicationContext(), "Log in to track flights!");
                    mTrackBtn.setEnabled(false);
                }
            }
        };
    }

    public void setFCCallback() {
        mFlightController.setUpdateSystemStateCallback(new DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback() {
            @Override
            public void onResult(DJIFlightControllerCurrentState state) {
                handleFCData(state);
                displayFCData();
            }
        });
    }

    public void displayFCData() {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isHomePointSet) {
                    mDistanceDisplay.setTextColor(Color.GREEN);
                } else {
                    mDistanceDisplay.setTextColor(Color.RED);
                }

                mHorizontalSpeedDisplay.setText(String.format(Locale.getDefault(), "H.S: %1$.1f m/s", droneLocation.getHorizSpeed()));
                mVerticalSpeedDisplay.setText(String.format(Locale.getDefault(), "V.S: %1$.1f m/s", droneLocation.getVertSpeed()));
                mHeightDisplay.setText(String.format(Locale.getDefault(), "H: %1$.1f m", droneLocation.getAltitude()));
                mDistanceDisplay.setText(String.format(Locale.getDefault(), "D: %1$.1f m", droneLocation.getDistanceFromHome()));
            }
        });
    }

    public void handleFCData(DJIFlightControllerCurrentState state) {
        double velocityX = (double) state.getVelocityX();
        double velocityY = (double) state.getVelocityY();
        double velocityZ = (double) state.getVelocityZ();
        double vertSpeed = (velocityZ != 0) ? -velocityZ : velocityZ;
        double horizSpeed = Math.sqrt((velocityX * velocityX) + (velocityY * velocityY));
        double latitude = state.getAircraftLocation().getLatitude();
        double longitude = state.getAircraftLocation().getLongitude();
        double altitude = state.getAircraftLocation().getAltitude();
        int heading = state.getAircraftHeadDirection();
        long timestamp = getTimestamp();

        droneLocation.setLatitude(latitude);
        droneLocation.setLongitude(longitude);
        droneLocation.setAltitude(altitude);
        droneLocation.setVertSpeed(vertSpeed);
        droneLocation.setHorizSpeed(horizSpeed);
        droneLocation.setTimestamp(timestamp);
        droneLocation.setHeading(heading);

        if (state.isHomePointSet()) {
            isHomePointSet = true;
            double homePointLatitude = state.getHomeLocation().getLatitude();
            double homePointLongitude = state.getHomeLocation().getLongitude();
            double droneLatitude = droneLocation.getLatitude();
            double droneLongitude = droneLocation.getLongitude();
            double distanceFromHome = distBetweenCoords(droneLatitude, droneLongitude, homePointLatitude, homePointLongitude);
            droneLocation.setDistanceFromHome(distanceFromHome);
        } else {
            isHomePointSet = false;
        }
    }

    public void initComponents() {
        mCamera = DroneFlightMapperApplication.getCameraInstance();
        mProduct = DroneFlightMapperApplication.getProductInstance();
        mFlightController = DroneFlightMapperApplication.getFlightControllerInstance();

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
    }
}