package ru.solandme.simpleblog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class BlogSingleActivity extends AppCompatActivity {

    private DatabaseReference databaseRef;
    private FirebaseAuth auth;
    private String postKey = null;
    private ImageView singleImageSelect;
    private EditText singleTitleField;
    private EditText singleDescField;
    private Button singleRemoveButton;
    private MenuItem mEdit, mDone, mRemove, mMap;
    private String mPostId;
    private boolean mAfterEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blog_single);

        postKey = getIntent().getExtras().getString(MainActivity.POST_KEY);
        databaseRef = FirebaseDatabase.getInstance().getReference().child("Blog");
        auth = FirebaseAuth.getInstance();

        singleImageSelect = (ImageView) findViewById(R.id.singleImageSelect);
        singleTitleField = (EditText) findViewById(R.id.singleTitleField);
        singleDescField = (EditText) findViewById(R.id.singleDescField);
        singleRemoveButton = (Button) findViewById(R.id.singleRemoveButton);

        databaseRef.child(postKey).addValueEventListener(new ValueEventListener() {
            
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String imageURL = (String) dataSnapshot.child("imageURL").getValue();
                String postTitle = (String) dataSnapshot.child("title").getValue();
                String postDesc = (String) dataSnapshot.child("description").getValue();
                mPostId = (String) dataSnapshot.child("uid").getValue();

                singleTitleField.setText(postTitle);
                singleTitleField.setEnabled(false);
                singleDescField.setText(postDesc);
                singleDescField.setEnabled(false);
                singleImageSelect.setClickable(false);
    
//                singleImageSelect.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
//                int width = singleImageSelect.getMeasuredWidth();
//                int height = singleImageSelect.getMeasuredHeight();
//                Picasso.with(BlogSingleActivity.this).load(imageURL).resize(width, height).centerInside().into(singleImageSelect);
                if (!mAfterEdit) {
                    Glide.with(BlogSingleActivity.this).load(imageURL).into(singleImageSelect);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }
    public void removePost(){
        databaseRef.child(postKey).removeValue();
        finish();
    }
    
    public void editPost(){
        singleImageSelect.setClickable(true);
        singleTitleField.setEnabled(true);
        singleDescField.setEnabled(true);
    }
    
    public void postPost(){
        mAfterEdit = true;
        databaseRef.child(postKey).child("title").setValue(singleTitleField.getText().toString());
        databaseRef.child(postKey).child("description").setValue(singleDescField.getText().toString());
        singleImageSelect.setClickable(false);
        singleTitleField.setEnabled(false);
        singleDescField.setEnabled(false);
    }
    
    public void showMap(Uri geoLocation) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail_menu, menu);
        mEdit = menu.findItem(R.id.action_edit);
        mDone = menu.findItem(R.id.action_done);
        mRemove = menu.findItem(R.id.action_remove);
        mMap = menu.findItem(R.id.action_map);
        if (auth.getCurrentUser().getUid().equals(mPostId)) {
            mEdit.setVisible(true);
            mRemove.setVisible(true);
            mMap.setVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit:
                editPost();
                mEdit.setVisible(false);
                mDone.setVisible(true);
                return true;
            case R.id.action_done:
                postPost();
                mEdit.setVisible(true);
                mDone.setVisible(false);
                return true;
            case R.id.action_remove:
                removePost();
                return true;
            case R.id.action_map:
                showMap(Uri.parse("geo:0,0?q=37.792180,-122.412179(Treasure)"));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
