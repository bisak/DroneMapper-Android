package com.droneflightmapper;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class ImagesGalleryActivity extends AppCompatActivity {

    ArrayList<String> imageUrls = new ArrayList<String>();
    private DatabaseReference mDatabase;
    private ListView imagesDisplayListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_images_gallery);
        getSupportActionBar().setTitle("DroneFlightMapper Gallery");

        imagesDisplayListView = (ListView) findViewById(R.id.listView);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("/images/").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                String url = dataSnapshot.child("thumbnailUrl").getValue().toString();
                imageUrls.add(url);

                ImageListAdapter adapter = new ImageListAdapter(ImagesGalleryActivity.this, imageUrls);
                imagesDisplayListView.setAdapter(adapter);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
