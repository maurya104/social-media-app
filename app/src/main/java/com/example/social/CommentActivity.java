package com.example.social;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.social.Adapter.CommentAdapter;
import com.example.social.Model.Comment;
import com.example.social.Model.Notification;
import com.example.social.Model.Post;
import com.example.social.Model.User;
import com.example.social.databinding.ActivityCommentBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;

public class CommentActivity extends AppCompatActivity {

    ActivityCommentBinding binding;
    Intent intent;
    String postId;
    String postedBy;
    FirebaseAuth auth;
    FirebaseDatabase database;
    ArrayList<Comment> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCommentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();

        setSupportActionBar(binding.toolbar2);
        CommentActivity.this.setTitle("Comments");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // getting data from another activity using intent
        intent = getIntent();
        postId = intent.getStringExtra("postId");
        postedBy = intent.getStringExtra("postedBy");

        database.getReference()
                .child("posts")
                .child(postId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Post post = snapshot.getValue(Post.class);
                Picasso.get()
                        .load(post.getPostImage())
                        .placeholder(R.drawable.placeholder)
                        .into(binding.postImage);

                binding.description.setText(post.getPostDescription());
                binding.like.setText(post.getPostLike() + "");
                binding.comment.setText(post.getCommentCount() + "");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        database.getReference()
                .child("Users")
                .child(postedBy).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                Picasso.get()
                        .load(user.getProfile())
                        .placeholder(R.drawable.placeholder)
                        .into(binding.profileImage);
                binding.name.setText(user.getName());

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        binding.commentPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Comment comment = new Comment();
                comment.setCommentBody(binding.commentET.getText().toString());
                comment.setCommentedAt(new Date().getTime());
                comment.setCommentedBy(FirebaseAuth.getInstance().getUid());

                database.getReference()
                        .child("posts")
                        .child(postId)
                        .child("comments")
                        .push()
                        .setValue(comment).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {

                        database.getReference()
                                .child("posts")
                                .child(postId)
                                .child("commentCount").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                int commentCount = 0;
                                if (snapshot.exists())
                                {
                                    commentCount = snapshot.getValue(Integer.class);
                                }
                                database.getReference()
                                        .child("posts")
                                        .child(postId)
                                        .child("commentCount")
                                        .setValue(commentCount + 1).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        binding.commentET.setText("");
                                        Toast.makeText(CommentActivity.this, "Commented", Toast.LENGTH_SHORT).show();


                                        Notification notification = new Notification();
                                        notification.setNotificationBy(FirebaseAuth.getInstance().getUid());
                                        notification.setNotificationAt(new Date().getTime());
                                        notification.setPostId(postId);
                                        notification.setPostedBy(postedBy);
                                        notification.setType("comment");

                                        FirebaseDatabase.getInstance().getReference()
                                                .child("notification")
                                                .child(postedBy)
                                                .push()
                                                .setValue(notification);
                                    }
                                });
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
                    }
                });
            }
        });

        CommentAdapter adapter = new CommentAdapter(this, list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.commentRv.setLayoutManager(layoutManager);
        binding.commentRv.setAdapter(adapter);

        database.getReference()
                .child("posts")
                .child(postId)
                .child("comments").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren())
                {
                    Comment comment = dataSnapshot.getValue(Comment.class);
                    list.add(comment);
                }
                adapter.notifyDataSetChanged();

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }
}