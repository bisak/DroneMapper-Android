package com.dronemapper.util;


import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.dronemapper.PictureData;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EntryActivityHelper {

    public EntryActivityHelper() {
    }

    public static PictureData getPictureMetadata(File file) {
        PictureData pd = new PictureData();
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            ExifIFD0Directory exifDirectory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            ExifSubIFDDirectory exifSubDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

            if (gpsDirectory != null && exifDirectory != null && exifSubDirectory != null) {
                DateFormat dateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()); //TODO test with getDefault locale.
                Date date = new Date();
                GeoLocation geoLocation = gpsDirectory.getGeoLocation();

                double latitude = geoLocation.getLatitude();
                double longitude = geoLocation.getLongitude();
                double altitude = Double.parseDouble(gpsDirectory.getDescription(GpsDirectory.TAG_ALTITUDE).split(" ")[0]);
                String maker = exifDirectory.getDescription(ExifIFD0Directory.TAG_MAKE);
                String model = exifDirectory.getDescription(ExifIFD0Directory.TAG_MODEL);
                String dateTaken = exifDirectory.getDescription(ExifIFD0Directory.TAG_DATETIME);
                String width = exifSubDirectory.getDescription(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH).split(" ")[0];
                String height = exifSubDirectory.getDescription(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT).split(" ")[0];
                String dateUploaded = dateFormat.format(date);
                String name = exifDirectory.getDescription(ExifIFD0Directory.TAG_IMAGE_DESCRIPTION);
                int slashIndex = name.lastIndexOf("\\");
                name = name.substring(slashIndex + 1);
                name = name.split("\\.")[0];

                pd.setMaker(maker);
                pd.setLat(latitude);
                pd.setLongt(longitude);
                pd.setAlt(altitude);
                pd.setCameraModel(model);
                pd.setDateTaken(dateTaken);
                pd.setResolution(width + "x" + height);
                pd.setDateUploaded(dateUploaded);
                pd.setName(name);
            } else {
                pd.setMaker("");
            }
        } catch (ImageProcessingException | IOException e) {
            e.printStackTrace();
        }
        return pd;
    }

    public static byte[] compressImage(String filePath, int width) {
        int height = width * 9 / 16;
        Bitmap initialBitmap = BitmapFactory.decodeFile(filePath);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(initialBitmap, width, height, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, baos);
        return baos.toByteArray();
    }

    public static String getRealPathFromURI(Uri contentUri, Context context) {
        String[] proj = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(context, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String filePath = cursor.getString(column_index);
        cursor.close();
        return filePath;
    }

}
