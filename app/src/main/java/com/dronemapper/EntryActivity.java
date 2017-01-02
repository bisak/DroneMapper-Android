package com.dronemapper;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.TypefaceProvider;
import com.beardedhen.androidbootstrap.api.defaults.DefaultBootstrapBrand;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

import dji.sdk.base.DJIBaseProduct;

import static com.dronemapper.util.EntryActivityHelper.compressImage;
import static com.dronemapper.util.EntryActivityHelper.getPictureMetadata;
import static com.dronemapper.util.EntryActivityHelper.getRealPathFromURI;
import static com.dronemapper.util.Helper.showToast;


public class EntryActivity extends Activity implements View.OnClickListener {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private static final String TAG = EntryActivity.class.getName();
    private static final int THUMBNAIL_WIDTH = 500;
    private static final int IMAGE_COMPRESSED_WIDTH = 1500;

    private static final int GALLERY_INTENT = 2;
    private TextView mTextConnectionStatus;
    private TextView mTextProduct;
    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshSDKRelativeUI();
        }
    };
    private ProgressBar mImageProgressBar;
    private ProgressBar mThumbnailProgressBar;
    private BootstrapButton mBtnOpen;
    private BootstrapButton mOpenGalleryButton;
    private BootstrapButton mSelectImageButton;
    private BootstrapButton mOpenLoginActivity;
    private TextView mThumbnailText;
    private TextView mImageText;
    private StorageReference mStorage;
    private DatabaseReference mDatabase;
    private String mainUrl;
    private String thumbnailUrl;
    private PictureData pictureData;
    public static String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initUI();

        mStorage = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        IntentFilter filter = new IntentFilter();
        filter.addAction(DroneFlightMapperApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        handleUserStatus();

    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    public void onReturn(View view) {
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private void initUI() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        TypefaceProvider.registerDefaultIconSets();
        setContentView(R.layout.activity_connection);


        mTextConnectionStatus = (TextView) findViewById(R.id.text_connection_status);
        mTextProduct = (TextView) findViewById(R.id.text_product_info);
        mBtnOpen = (BootstrapButton) findViewById(R.id.btn_open);
        mBtnOpen.setOnClickListener(this);
        mBtnOpen.setEnabled(false);
        mBtnOpen.setBootstrapBrand(DefaultBootstrapBrand.REGULAR);
        mSelectImageButton = (BootstrapButton) findViewById(R.id.image_upload_button);
        mOpenGalleryButton = (BootstrapButton) findViewById(R.id.gallery_open_button);
        mOpenLoginActivity = (BootstrapButton) findViewById(R.id.login_activity_open);
        mSelectImageButton.setOnClickListener(this);
        mOpenGalleryButton.setOnClickListener(this);
        mOpenLoginActivity.setOnClickListener(this);
        mImageProgressBar = (ProgressBar) findViewById(R.id.image_upload_progressbar);
        mThumbnailProgressBar = (ProgressBar) findViewById(R.id.thumbnail_upload_progressbar);
        mThumbnailText = (TextView) findViewById(R.id.thumbnail_upload_text);
        mImageText = (TextView) findViewById(R.id.image_upload_text);
    }

    private void refreshSDKRelativeUI() {
        DJIBaseProduct mProduct = DroneFlightMapperApplication.getProductInstance();

        if (null != mProduct && mProduct.isConnected()) {
            Log.v(TAG, "refreshSDK: True");
            mTextConnectionStatus.setText("Drone connected");
            if (null != mProduct.getModel()) {
                mTextProduct.setText("" + mProduct.getModel().getDisplayName());
                mBtnOpen.setBootstrapBrand(DefaultBootstrapBrand.SUCCESS);
                mBtnOpen.setEnabled(true);
            }
        } else {
            Log.v(TAG, "refreshSDK: False");
            mBtnOpen.setEnabled(false);
            mBtnOpen.setBootstrapBrand(DefaultBootstrapBrand.REGULAR);
            mTextProduct.setText("");
            mTextConnectionStatus.setText("No Drone Connected");
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btn_open: {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                break;
            }

            case R.id.image_upload_button: {
                imageSelectButtonActions();
                break;
            }

            case R.id.gallery_open_button: {
                Intent openGalleryIntent = new Intent(EntryActivity.this, ImagesGalleryActivity.class);
                startActivity(openGalleryIntent);
                break;
            }

            case R.id.login_activity_open: {
                if (mAuth.getCurrentUser() != null) {
                    mAuth.signOut();
                } else {
                    Intent openLoginIntent = new Intent(EntryActivity.this, LoginActivity.class);
                    startActivity(openLoginIntent);
                    break;
                }
            }
            default:
                break;
        }
    }

    private void imageSelectButtonActions() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/jpeg");
        startActivityForResult(intent, GALLERY_INTENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_INTENT && resultCode == RESULT_OK) {
            Uri image = data.getData();
            String filePath = getRealPathFromURI(image, EntryActivity.this);
            File file = new File(filePath);
            pictureData = getPictureMetadata(file);
            if (pictureData.getMaker().equals("DJI")) {
                pictureData.setMaker(null);
                uploadImages(filePath, pictureData);
            } else {
                showToast(getApplicationContext(), "Only DJI Drone pictures allowed.");
            }
        }
    }

    private void uploadImages(String filePath, PictureData metadata) {
        String dbRecordName = mDatabase.push().getKey();
        byte[] thumbnail = compressImage(filePath, THUMBNAIL_WIDTH);
        byte[] compressedImage = compressImage(filePath, IMAGE_COMPRESSED_WIDTH);
        uploadThumbnail(thumbnail, compressedImage, dbRecordName);
    }

    private void uploadThumbnail(byte[] thumbnail, final byte[] compressedImage, final String dbRecordName) {
        UploadTask thumbnailUpload = mStorage.child("/thumbnails/" + userId).child(dbRecordName).putBytes(thumbnail);
        thumbnailUpload.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                showToast(getApplicationContext(), "Thumbnail Upload Failed.");
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                thumbnailUrl = taskSnapshot.getDownloadUrl().toString();
                pictureData.setThumbnailUrl(thumbnailUrl);
                uploadImage(compressedImage, dbRecordName);
                showToast(getApplicationContext(), "Thumbnail Uploaded.");
                mThumbnailProgressBar.setVisibility(View.INVISIBLE);
                mThumbnailText.setVisibility(View.INVISIBLE);
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                mThumbnailProgressBar.setVisibility(View.VISIBLE);
                mThumbnailText.setVisibility(View.VISIBLE);
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                mThumbnailProgressBar.setProgress((int) progress);
            }
        });
    }

    private void uploadImage(byte[] image, final String dbRecordName) {
        UploadTask imageUpload = mStorage.child("/images/" + userId).child(dbRecordName).putBytes(image);
        imageUpload.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                showToast(getApplicationContext(), "Image Uploaded.");
                mainUrl = taskSnapshot.getDownloadUrl().toString();
                pictureData.setUrl(mainUrl);
                setMetadata(dbRecordName);
                mImageProgressBar.setVisibility(View.INVISIBLE);
                mImageText.setVisibility(View.INVISIBLE);
                mSelectImageButton.setEnabled(true);
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                mImageProgressBar.setVisibility(View.VISIBLE);
                mImageText.setVisibility(View.VISIBLE);
                mSelectImageButton.setEnabled(false);
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                mImageProgressBar.setProgress((int) progress);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                showToast(getApplicationContext(), "Image Upload Failed.");
                mSelectImageButton.setEnabled(true);
            }
        });
    }

    private void setMetadata(final String dbRecordName) {
        mDatabase.child("/images/" + userId).child(dbRecordName).setValue(pictureData);
    }

    private void handleUserStatus() {
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    userId = user.getUid();
                    mOpenLoginActivity.setText("Log Out");
                    mSelectImageButton.setBootstrapBrand(DefaultBootstrapBrand.SUCCESS);
                    mOpenGalleryButton.setBootstrapBrand(DefaultBootstrapBrand.SUCCESS);
                    mSelectImageButton.setEnabled(true);
                    mOpenGalleryButton.setEnabled(true);

                } else {
                    userId = "0";
                    mOpenGalleryButton.setBootstrapBrand(DefaultBootstrapBrand.REGULAR);
                    mSelectImageButton.setBootstrapBrand(DefaultBootstrapBrand.REGULAR);
                    mOpenLoginActivity.setText("Log In");
                    mSelectImageButton.setEnabled(false);
                    mOpenGalleryButton.setEnabled(false);
                }
            }
        };
    }

}