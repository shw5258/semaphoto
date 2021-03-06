package ru.solandme.simpleblog;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class MainActivity extends AppCompatActivity {
    
    public static final String POST_KEY = "postKey";
    private RecyclerView blogList;
    private DatabaseReference databaseRef;
    private DatabaseReference databaseRefUsers;
    private DatabaseReference databaseRefCurrentUser;
    private DatabaseReference databaseRefLike;
    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private Query queryCurrentUser;
    private String user_id;
    private LinearLayoutManager mLayoutManager;
    private Boolean processLike;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        auth = FirebaseAuth.getInstance();
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null) {
                    Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                    loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(loginIntent);
                } else{
                    user_id = auth.getCurrentUser().getUid();
                }
            }
        };

        databaseRef = FirebaseDatabase.getInstance().getReference().child("Blog");
        databaseRefUsers = FirebaseDatabase.getInstance().getReference().child("Users");
        databaseRefLike = FirebaseDatabase.getInstance().getReference().child("Likes");
    
        if (auth.getCurrentUser() != null) {
            String currentUserId = auth.getCurrentUser().getUid();
            databaseRefCurrentUser = FirebaseDatabase.getInstance().getReference().child("Blog");
            queryCurrentUser = databaseRefCurrentUser.orderByChild("uid").equalTo(currentUserId);
        }
    
        databaseRef.keepSynced(true);
        databaseRefUsers.keepSynced(true);
        databaseRefLike.keepSynced(true);

        blogList = (RecyclerView) findViewById(R.id.blogList);

        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setStackFromEnd(true);
//        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL);
//        layoutManager.setReverseLayout(true);
//        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
//        blogList.setHasFixedSize(true);
        blogList.setLayoutManager(mLayoutManager);
        
        auth.addAuthStateListener(authStateListener);
        checkUserExist();
        FirebaseRecyclerAdapter<Blog, BlogViewHolder> firebaseRecyclerAdapter =
                new FirebaseRecyclerAdapter<Blog, BlogViewHolder>(Blog.class, R.layout.blog_row,
                        BlogViewHolder.class, databaseRef) {
                
                    @Override
                    protected void populateViewHolder(BlogViewHolder viewHolder, Blog model, int position) {
                    
                        final String postKey = getRef(position).getKey();
                    
                        viewHolder.setTitle(model.getTitle());
                        viewHolder.setImage(viewHolder.view.getContext(), model.getImageURL());
                        viewHolder.setUsername(model.getUsername());
                        viewHolder.setLikeBtn(postKey);
                        viewHolder.view.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent singleBlogIntent = new Intent(MainActivity.this, BlogSingleActivity.class);
                                singleBlogIntent.putExtra(POST_KEY, postKey);
                                startActivity(singleBlogIntent);
                            }
                        });
                        viewHolder.likeBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                processLike = true;
                                databaseRefLike.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                    
                                        if (processLike) {
                                            if (dataSnapshot.child(postKey).hasChild(auth.getCurrentUser().getUid())) {
                                                databaseRefLike.child(postKey).child(auth.getCurrentUser().getUid()).removeValue();
                                                processLike = false;
                                            } else {
                                                databaseRefLike.child(postKey).child(auth.getCurrentUser().getUid()).setValue("RandomValue");
                                                processLike = false;
                                            }
                                        }
                                    }
                                
                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {}
                                });
                            }
                        });
                    }
                };
        blogList.setAdapter(firebaseRecyclerAdapter);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        auth.removeAuthStateListener(authStateListener);
    }
    
    //    @Override
//    protected void onPause() {
//        super.onPause();
//        positionIndex= mLayoutManager.findFirstVisibleItemPosition();
//        View startView = blogList.getChildAt(0);
//        topView = (startView == null) ? 0 : (startView.getTop() - blogList.getPaddingTop());
//        Log.d("MainActivity", "onPause called: " + positionIndex + " topview:" + topView);
//    }
//
//    @Override
//    protected void onResume() {
//        Log.d("MainActivity", "onResume called: " + positionIndex + " topview:" + topView);
//        super.onResume();
//        if (positionIndex!= -1) {
//        mLayoutManager.scrollToPositionWithOffset(4, 0);
//        }
//    }
    
    public static class BlogViewHolder extends RecyclerView.ViewHolder {

        View view;
        ImageButton likeBtn;
        DatabaseReference databaseRefLike;
        FirebaseAuth auth;

        public BlogViewHolder(View itemView) {
            super(itemView);
            view = itemView;
            likeBtn = (ImageButton) view.findViewById(R.id.likeBtn);
            databaseRefLike = FirebaseDatabase.getInstance().getReference().child("Likes");
            auth = FirebaseAuth.getInstance();

            databaseRefLike.keepSynced(true);
        }

        void setTitle(String title) {
            TextView postTitle = (TextView) view.findViewById(R.id.postTitle);
            postTitle.setText(title);
        }

        void setLikeBtn(final String postKey) {
            databaseRefLike.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.child(postKey).hasChild(auth.getCurrentUser().getUid())) {
                        likeBtn.setImageResource(R.drawable.ic_like);
                    } else {
                        likeBtn.setImageResource(R.drawable.ic_like_grey);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        void setImage(Context context, String imageUrl) {
            ImageView postImage = (ImageView) view.findViewById(R.id.postImage);
//            postImage.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
//            int width = postImage.getMeasuredWidth();
//            int height = postImage.getMeasuredHeight();
//            Picasso.with(context)
//                    .load(imageUrl)
//                    .placeholder(R.mipmap.add_btn)
//                    .resize(width, height)
//                    .centerInside()
//                    .into(postImage);
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.food_plate)
                    .centerCrop()
                    .into(postImage);
//            Log.d(MainActivity.class.getCanonicalName(), "postImageView Width: " + width + " Height: " + height);
        }
        //datachange

        void setUsername(String username) {
            TextView postUsername = (TextView) view.findViewById(R.id.postUsername);
            postUsername.setText(username);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_add) {
            startActivity(new Intent(MainActivity.this, PostActivity.class));
        }

        if (item.getItemId() == R.id.action_logout) {
            logout();
        }

        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        auth.signOut();
    }


    private void checkUserExist() {

        databaseRefUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (null != dataSnapshot && dataSnapshot.hasChild(user_id)
                        && (!dataSnapshot.child(user_id).hasChild("name") || !dataSnapshot.child(user_id).hasChild("image"))) {
                    Intent setupIntent = new Intent(MainActivity.this, SetupActivity.class);
                    setupIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(setupIntent);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
