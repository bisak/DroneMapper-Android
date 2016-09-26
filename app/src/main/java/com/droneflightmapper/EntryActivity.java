package com.droneflightmapper;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDescriptor;
import com.drew.metadata.exif.GpsDirectory;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

import dji.sdk.base.DJIBaseProduct;
import dji.sdk.products.DJIAircraft;

public class EntryActivity extends Activity implements View.OnClickListener {

    private static final String TAG = EntryActivity.class.getName();
    private static final int PICTURE_WIDTH = 500;
    private static final int GALLERY_INTENT = 2;
    private TextView mTextConnectionStatus;
    private TextView mTextProduct;
    private Button mBtnOpen;
    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            refreshSDKRelativeUI();
        }
    };
    private ProgressBar mImageProgressBar;
    private ProgressBar mThumbnailProgressBar;
    private Button mOpenGalleryButton;
    private Button mSelectImageButton;
    private TextView mThumbnailText;
    private TextView mImageText;
    private StorageReference mStorage;
    private DatabaseReference mDatabase;
    private double latitude;
    private double longitude;
    private double altitude;
    private String fileName;
    private String mainUrl;
    private String dbRecordName;
    private String thumbnailUrl;
    private com.droneflightmapper.PictureData pictureData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the
        // SDK work well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        setContentView(R.layout.activity_connection);

        initUI();

        mStorage = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Register the broadcast receiver for receiving the device connection's changes.
        IntentFilter filter = new IntentFilter();
        filter.addAction(DroneFlightMapperApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);
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
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
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

        mTextConnectionStatus = (TextView) findViewById(R.id.text_connection_status);
        mTextProduct = (TextView) findViewById(R.id.text_product_info);
        mBtnOpen = (Button) findViewById(R.id.btn_open);
        mBtnOpen.setOnClickListener(this);
        mBtnOpen.setEnabled(false);

        mSelectImageButton = (Button) findViewById(R.id.image_upload_button);
        mOpenGalleryButton = (Button) findViewById(R.id.gallery_open_button);
        mSelectImageButton.setOnClickListener(this);
        mOpenGalleryButton.setOnClickListener(this);
        mImageProgressBar = (ProgressBar) findViewById(R.id.image_upload_progressbar);
        mThumbnailProgressBar = (ProgressBar) findViewById(R.id.thumbnail_upload_progressbar);
        mThumbnailText = (TextView) findViewById(R.id.thumbnail_upload_text);
        mImageText = (TextView) findViewById(R.id.image_upload_text);

    }

    private void refreshSDKRelativeUI() {
        DJIBaseProduct mProduct = DroneFlightMapperApplication.getProductInstance();

        if (null != mProduct && mProduct.isConnected()) {
            Log.v(TAG, "refreshSDK: True");
            mBtnOpen.setEnabled(true);

            String str = mProduct instanceof DJIAircraft ? "DJIAircraft" : "DJIHandHeld";
            mTextConnectionStatus.setText("Status: " + str + " connected");

            if (null != mProduct.getModel()) {
                mTextProduct.setText("" + mProduct.getModel().getDisplayName());
            } else {
                mTextProduct.setText(R.string.product_information);
            }

        } else {
            Log.v(TAG, "refreshSDK: False");
            mBtnOpen.setEnabled(false);

            mTextProduct.setText(R.string.product_information);
            mTextConnectionStatus.setText(R.string.connection_loose);
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

            String filePath = getRealPathFromURI(image);
            File file = new File(filePath);
            byte[] thumbnail = makeThumbnail(filePath, PICTURE_WIDTH);
            getMetadata(file);
            fileName = getFileNameFromURI(image);
            dbRecordName = fileName + "-" + UUID.randomUUID().toString();
            uploadThumbnail(thumbnail);
            uploadImage(image);
        }

    }

    private void uploadThumbnail(byte[] thumbnail) {
        UploadTask thumbnailUpload = mStorage.child("/thumbnails").child(dbRecordName).putBytes(thumbnail);
        thumbnailUpload.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(EntryActivity.this, "Thumbnail Upload Failed!", Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                thumbnailUrl = taskSnapshot.getDownloadUrl().toString();
                Toast.makeText(EntryActivity.this, "Thumbnail Upload Done!", Toast.LENGTH_SHORT).show();
                mThumbnailProgressBar.setVisibility(View.INVISIBLE);
                mThumbnailText.setVisibility(View.INVISIBLE);

                /*Start upload of entire image here or first upload the entire image
                and from that snapshot data extract the entire image and make thumbnail from it
                which thumbnail is going to be uploaded. Do that if it is possible to get the image from the taskShapshot.*/

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

    private void uploadImage(Uri image) {
        UploadTask imageUpload = mStorage.child("/images").child(dbRecordName).putFile(image);
        imageUpload.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(EntryActivity.this, "Image Upload Done!", Toast.LENGTH_SHORT).show();
                mainUrl = taskSnapshot.getDownloadUrl().toString();
                pictureData = new com.droneflightmapper.PictureData(longitude, latitude, altitude, fileName, mainUrl, thumbnailUrl);
                mDatabase.child("/images").child(dbRecordName).setValue(pictureData);
                mImageProgressBar.setVisibility(View.INVISIBLE);
                mImageText.setVisibility(View.INVISIBLE);
                mSelectImageButton.setEnabled(true);;
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                mImageProgressBar.setVisibility(View.VISIBLE);
                mImageText.setVisibility(View.VISIBLE);
                mSelectImageButton.setEnabled(false);;
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                mImageProgressBar.setProgress((int) progress);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(EntryActivity.this, "Image Upload Failed!", Toast.LENGTH_SHORT).show();
                mSelectImageButton.setEnabled(true);;
            }
        });
    }

    private void getMetadata(File file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            GpsDescriptor gpsDescriptor = new GpsDescriptor(gpsDirectory);
            GeoLocation geoLocation = gpsDirectory.getGeoLocation();
            latitude = geoLocation.getLatitude();
            longitude = geoLocation.getLongitude();
            altitude = Double.parseDouble(gpsDescriptor.getGpsAltitudeDescription().split(" ")[0]);
        } catch (ImageProcessingException | IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] makeThumbnail(String filePath, int width) {
        int height = width * 9 / 16;
        Bitmap initialBitmap = BitmapFactory.decodeFile(filePath);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(initialBitmap, width, height, true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
    }

    private String getFileNameFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(EntryActivity.this, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String[] parts = cursor.getString(column_index).split("/");
        String name = parts[parts.length - 1].split("\\.")[0];
        cursor.close();
        return name;
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(EntryActivity.this, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String filePath = cursor.getString(column_index);
        cursor.close();
        return filePath;
    }

}
