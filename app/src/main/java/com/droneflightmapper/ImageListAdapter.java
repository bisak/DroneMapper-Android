package com.droneflightmapper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class ImageListAdapter extends ArrayAdapter {
    ArrayList<String> imageUrls = new ArrayList<>();
    private Context context;
    private LayoutInflater inflater;

    public ImageListAdapter(Context context, ArrayList<String> imageUrls) {
        super(context, R.layout.listview_item_image, imageUrls);
        this.context = context;
        for (String temp : imageUrls) {
            this.imageUrls.add(temp);
        }
        inflater = LayoutInflater.from(context);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (null == convertView) {
            convertView = inflater.inflate(R.layout.listview_item_image, parent, false);
        }

        Glide.with(context).load(imageUrls.get(position)).into((ImageView) convertView);

        return convertView;
    }
}