package com.dronemapper.util;

import android.content.Context;
import android.media.MediaActionSound;
import android.os.Environment;
import android.os.Handler;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;


public class Helper {

    public Helper() {
    }

    public static void startErrorLog() {
        try {
            File folder = new File(Environment.getExternalStorageDirectory() + File.separator + "DroneMapper");
            boolean success = true;
            if (!folder.exists()) {
                success = folder.mkdirs();
            }
            if (success) {
                File fdelete = new File(Environment.getExternalStorageDirectory() + File.separator + "DroneMapper" + File.separator + "DroneMapperLog.txt");
                if (fdelete.exists()) {
                    fdelete.delete();
                }
                Runtime.getRuntime().exec("logcat -f " + Environment.getExternalStorageDirectory() + File.separator + "DroneMapper" + File.separator + "DroneMapperLog.txt");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void playMediaSound(int soundId) {
        MediaActionSound sound = new MediaActionSound();
        sound.play(soundId);
    }

    public static void showToast(final Context context, final String msg) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
