package com.droneflightmapper;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.AbsListView;
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
    private int mLastFirstVisibleItem;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_images_gallery);
        getSupportActionBar().setTitle("DroneFlightMapper Gallery");

        imagesDisplayListView = (ListView) findViewById(R.id.gallery_list_view);
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

        imagesDisplayListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                /**
                 * Hide actionbar when scroll down
                 */
                if (mLastFirstVisibleItem < firstVisibleItem)
                    if (getSupportActionBar().isShowing())
                        getSupportActionBar().hide();

                if (mLastFirstVisibleItem > firstVisibleItem)
                    if (!getSupportActionBar().isShowing())
                        getSupportActionBar().show();
                mLastFirstVisibleItem = firstVisibleItem;
            }
        });
    }
}
