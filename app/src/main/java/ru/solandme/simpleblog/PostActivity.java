package ru.solandme.simpleblog;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PostActivity extends AppCompatActivity {

    private ImageButton selectImage;
    private EditText postTitle;
    private EditText postDesc;
    private TextView locationName;
    private double latitude;
    private double longitude;
    private Uri imageUri = null;

    private StorageReference storage;
    private DatabaseReference databaseReference;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseRefUser;
    private int mWidth = 0;
    private int mHeight = 0;

    private ProgressDialog progress;

    private static final int GALLERY_REQUEST = 1;
    public static final int PLACE_PICKER_REQUEST = 2;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        storage = FirebaseStorage.getInstance().getReference();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Blog");
        databaseRefUser = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUser.getUid());

        selectImage = (ImageButton) findViewById(R.id.imageSelect);
        selectImage.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        mWidth = selectImage.getMeasuredWidth();
        mHeight = selectImage.getMeasuredHeight();
        
        
        postTitle = (EditText) findViewById(R.id.titleField);
        postDesc = (EditText) findViewById(R.id.descField);
        locationName = (TextView) findViewById(R.id.locationName);

        progress = new ProgressDialog(this);

        selectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GALLERY_REQUEST);
            }
        });

    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        
        getMenuInflater().inflate(R.menu.post_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        
        if (item.getItemId() == R.id.action_post) {
    
            startPosting();
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void startPosting() {
        progress.setMessage("Posting to Blog ...");

        final String title_val = postTitle.getText().toString().trim();
        final String desc_val = postDesc.getText().toString().trim();

        if (!TextUtils.isEmpty(title_val) && !TextUtils.isEmpty(desc_val) && imageUri != null) {

            progress.show();

            StorageReference filePath = storage.child("Blog_Images").child(imageUri.getLastPathSegment());
    
            Bitmap scaledBM;
            ByteArrayInputStream bs;
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),imageUri);
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                View view = findViewById(R.id.imageSelect);
                int frameWidth = view.getWidth();
                int expectedHeight = (height * frameWidth) / width;
                scaledBM = Bitmap.createScaledBitmap(bitmap, frameWidth, expectedHeight, true);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                scaledBM.compress(Bitmap.CompressFormat.JPEG, 50 /*ignored for PNG*/, bos);
                byte[] bitmapdata = bos.toByteArray();
                bs = new ByteArrayInputStream(bitmapdata);//이렇게 비트맵으로 다운스케일을 해서 용량을 줄임으로
                //업로드 다운로드 속도가 개선되었다.
    
                filePath.putStream(bs).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        final Uri downloadUrl = taskSnapshot.getDownloadUrl();
            
                        final DatabaseReference newPostRef = databaseReference.push();
            
                        databaseRefUser.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                newPostRef.child("title").setValue(title_val);
                                newPostRef.child("description").setValue(desc_val);
                                if (latitude != 0 && longitude != 0) {
                                    newPostRef.child("latitude").setValue(latitude);
                                    newPostRef.child("longitude").setValue(longitude);
                                }
                                newPostRef.child("imageURL").setValue(downloadUrl.toString());
                                newPostRef.child("uid").setValue(currentUser.getUid());
                                newPostRef.child("username").setValue(dataSnapshot.child("name")
                                        .getValue()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            startActivity(new Intent(PostActivity.this, MainActivity.class));
                                            finish();
                                        }
                                    }
                                });
                            }
                
                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                    
                            }
                        });
                        progress.dismiss();
                    }
                });
            } catch(IOException e) {
                System.out.println(e);
            }

        }
    }
    
    public void getLocation(View view) {
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        try {
            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
        
            if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK) {
                imageUri = data.getData();
                Picasso.with(getBaseContext()).load(imageUri).resize(mWidth, mHeight).centerInside().into(selectImage);
            }
    
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(data, this);
                locationName.setText(place.getName());
                LatLng latLng = place.getLatLng();
                latitude = latLng.latitude;
                longitude = latLng.longitude;
            }
        }
    }
}
