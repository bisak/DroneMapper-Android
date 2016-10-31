package com.droneflightmapper;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.w3c.dom.Text;

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

import static com.droneflightmapper.R.id.distance_display;
import static com.droneflightmapper.R.id.timer;


public class MainActivity extends Activity implements SurfaceTextureListener, OnClickListener {

    private static final long DATA_SEND_MILLISECONDS = 1500;
    private static final String TAG = MainActivity.class.getName();
    protected DJICamera.CameraReceivedVideoDataCallback mReceivedVideoDataCallBack = null;

    // Codec for video live view
    protected DJICodecManager mCodecManager = null;
    private DJIFlightController mFlightController;
    private DJICompass mCompass;
    protected TextureView mVideoSurface = null;
    private Button mCaptureBtn, mDebugOneBtn;
    private ToggleButton mRecordToggleBtn;
    private ToggleButton mDebugThreeToggleBtn;
    private ToggleButton mBlinkToggleBtn;
    private TextView mRecordingTime;
    private TextView mDebugMsgOne;
    private TextView mDebugMsgTwo;
    private TextView mDebugMsgThree;
    private TextView mDebugMsgFour;
    private TextView mDebugMsgFive;
    private TextView mDebugMsgSix;
    private TextView mDebugMsgMotors;
    private TextView mBatteryPercentage;
    private TextView mHeightDisplay;
    private TextView mDistanceDisplay;
    private TextView mHorizontalSpeedDisplay;
    private TextView mVerticalSpeedDisplay;
    private TextView mBatteryDisplay;
    private DroneLocation droneLocation;
    private DatabaseReference mDatabase;
    private DJICameraSettingsDef.CameraMode currentMode;
    private DJICamera mCamera;
    private DJIBaseProduct mProduct;
    private boolean areMotorsRunning = false;
    private boolean isSenderTaskRunning = false;
    private boolean isDebugThreeToggleBtnChecked = false;
    private Timer sender;
    private Timer blinker;
    private String secondaryChild;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    DecimalFormat df = new DecimalFormat("#0.0");
    double velocityX = 0;
    double velocityY = 0;
    double velocityZ = 0;
    double mHomeLatitude = 181;
    double mHomeLongitude = 181;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        initUI();
        mCamera = DroneFlightMapperApplication.getCameraInstance();
        mProduct = DroneFlightMapperApplication.getProductInstance();
        mFlightController = ((DJIAircraft) DroneFlightMapperApplication.getProductInstance()).getFlightController();
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

                                /*
                                 * Update mRecordingTime TextView visibility and mRecordToggleBtn's check state
                                 */
                                if (isVideoRecording) {
                                    mRecordingTime.setVisibility(View.VISIBLE);
                                } else {
                                    mRecordingTime.setVisibility(View.INVISIBLE);
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
                        mBatteryDisplay.setText("Batt: " + String.valueOf(percentage) + "%");
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
                double speed = Math.sqrt((velocityX * velocityX) + (velocityY * velocityY));
                droneLocation.setSpeed(Double.valueOf(df.format(speed)));
                Date date = new Date();
                droneLocation.setTime(date.getTime() / 1000);
                droneLocation.setHeading(mCompass.getHeading());
                mHomeLatitude = state.getHomeLocation().getLatitude();
                mHomeLongitude = state.getHomeLocation().getLongitude();

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDebugMsgOne.setText(String.valueOf(droneLocation.getLatitude()));
                        mDebugMsgTwo.setText(String.valueOf(droneLocation.getLongitude()));
                        mDebugMsgThree.setText("Speed: " + String.valueOf(droneLocation.getSpeed()));
                        mDebugMsgFour.setText("HDG" + String.valueOf(droneLocation.getHeading()));
                        mDebugMsgFive.setText("DebugMsg.");
                        mDebugMsgSix.setText(String.valueOf(droneLocation.getAltitude()));

                        mHorizontalSpeedDisplay.setText("H.S: " + String.valueOf(droneLocation.getSpeed()) + "m/s");
                        mVerticalSpeedDisplay.setText("V.S: " + String.valueOf(df.format(-velocityZ)) + "m/s"); //TODO fix that!
                        mHeightDisplay.setText("H: " + String.valueOf(droneLocation.getAltitude() + "m"));
                        mDistanceDisplay.setText("D: " + String.valueOf(df.format(distFrom((float) droneLocation.getLatitude(), (float) droneLocation.getLongitude(), (float) mHomeLatitude, (float) mHomeLongitude))) + "m");

                    }
                });


                if (state.areMotorsOn()) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDebugMsgMotors.setText("Motors On");
                        }
                    });
                } else {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDebugMsgMotors.setText("Motors Off");
                        }
                    });
                }
            }
        });

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    mDebugThreeToggleBtn.setEnabled(true);
                } else {
                    mDebugThreeToggleBtn.setEnabled(false);
                }
            }
        };
    }

    protected void onProductChange() {
        initPreviewer();
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        initPreviewer();
        onProductChange();

        if (mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
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
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        uninitPreviewer();
        super.onDestroy();
    }

    private void initUI() {
        mVideoSurface = (TextureView) findViewById(R.id.video_previewer_surface);
        mDebugMsgOne = (TextView) findViewById(R.id.debug_msg_one);
        mDebugMsgTwo = (TextView) findViewById(R.id.debug_msg_two);
        mDebugMsgThree = (TextView) findViewById(R.id.debug_msg_three);
        mDebugMsgFour = (TextView) findViewById(R.id.debug_msg_four);
        mDebugMsgFive = (TextView) findViewById(R.id.debug_msg_five);
        mDebugMsgSix = (TextView) findViewById(R.id.debug_msg_six);
        mDebugMsgMotors = (TextView) findViewById(R.id.debug_msg_motors);
        mRecordingTime = (TextView) findViewById(timer);
        mBatteryPercentage = (TextView) findViewById(R.id.battery_percentage);
        mHeightDisplay = (TextView) findViewById(R.id.height_display);
        mDistanceDisplay = (TextView) findViewById(R.id.distance_display);
        mHorizontalSpeedDisplay = (TextView) findViewById(R.id.horizontal_speed_display);
        mVerticalSpeedDisplay = (TextView) findViewById(R.id.vertical_speed_display);
        mBatteryDisplay = (TextView) findViewById(R.id.battery_display);
        mCaptureBtn = (Button) findViewById(R.id.btn_capture);
        mRecordToggleBtn = (ToggleButton) findViewById(R.id.btn_record);
        mDebugOneBtn = (Button) findViewById(R.id.debug_one);
        mDebugThreeToggleBtn = (ToggleButton) findViewById(R.id.debug_three_toggle);
        mBlinkToggleBtn = (ToggleButton) findViewById(R.id.blink_toggle);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        mCaptureBtn.setOnClickListener(this);
        mRecordToggleBtn.setOnClickListener(this);
        mDebugOneBtn.setOnClickListener(this);

        mRecordingTime.setVisibility(View.INVISIBLE);

        mDebugThreeToggleBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    sendDataToDb(droneLocation);
                } else {
                    mDatabase.child("/realtime-flights/").child(secondaryChild).removeValue(); //TODO ADD THIS TO ONCLOSE , ONSTOP and other overrided methods
                    isSenderTaskRunning = false;
                    sender.cancel();
                    sender.purge();
                }
            }
        });

        mRecordToggleBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
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
        });

        mBlinkToggleBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    blinkLeds(750);
                } else {
                    blinker.cancel();
                    blinker.purge();
                    DroneFlightMapperApplication.getAircraftInstance().getFlightController().setLEDsEnabled(true, new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {

                        }
                    });
                }
            }
        });
    }

    private void sendDataToDb(final DroneLocation drloc) {
        sender = new Timer();
        sender.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isSenderTaskRunning) {
                    secondaryChild = mDatabase.push().getKey();
                }
                mDatabase.child("/realtime-flights/").child(secondaryChild).push().setValue(drloc);
                mDatabase.child("/saved-flights/").child(secondaryChild).push().setValue(drloc);
                isSenderTaskRunning = true;
            }

        }, 0, DATA_SEND_MILLISECONDS);
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
            case R.id.debug_one: {
                break;
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
                        showToast("Switch Camera Mode Succeeded");
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
                        showToast("take photo: success");
                    } else {
                        showToast(error.getDescription());
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
                        showToast("Record video: success");
                    } else {
                        showToast(error.getDescription());
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
                        showToast("Stop recording: success");
                    } else {
                        showToast(error.getDescription());
                    }
                }
            }); // Execute the stopRecordVideo API
        }
    }


    private void blinkLeds(int speed) {
        blinker = new Timer();
        blinker.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                DroneFlightMapperApplication.getAircraftInstance().getFlightController().getLEDsEnabled(new DJICommonCallbacks.DJICompletionCallbackWith<Boolean>() {
                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        if (aBoolean) {
                            DroneFlightMapperApplication.getAircraftInstance().getFlightController().setLEDsEnabled(false, new DJICommonCallbacks.DJICompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {

                                }
                            });
                        } else {
                            DroneFlightMapperApplication.getAircraftInstance().getFlightController().setLEDsEnabled(true, new DJICommonCallbacks.DJICompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {

                                }
                            });
                        }
                    }

                    @Override
                    public void onFailure(DJIError djiError) {

                    }
                });
            }
        }, 0, speed);

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

