package com.example.peng;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChatFragment extends Fragment implements View.OnClickListener {
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference mDatabaseReference;
    private FloatingActionButton sendMessageFab;
    private EditText messageEditText;
    public static final String ANONYMOUS = "anonymous";
    private String mUsername;
    private RecyclerView messageRecyclerView;
    private static FirebaseRecyclerAdapter adapter;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private static final int RC_SIGN_IN = 123;
    private static final int RC_PHOTO_PICKER =  2;
    private ImageView sendImage;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;

    public ChatFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        {
            mUsername = ANONYMOUS;

            //Initialize references to views
            sendMessageFab = getView().findViewById(R.id.sendMessageFab);
            messageEditText = getView().findViewById(R.id.messageEditText);
            messageRecyclerView = getView().findViewById(R.id.messagesRecyclerView);
            sendImage = getView().findViewById(R.id.sendImageImageView);
            sendMessageFab.setOnClickListener(this);
            sendImage.setOnClickListener(this);

            //Initialize firebase references
            firebaseDatabase = FirebaseDatabase.getInstance();
            firebaseAuth = FirebaseAuth.getInstance();
            mDatabaseReference = firebaseDatabase.getReference().child("messages");
            firebaseStorage = FirebaseStorage.getInstance();
            storageReference = firebaseStorage.getReference().child("chat_photos");

            setHasOptionsMenu(true);

            //Set up recycler view
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
            messageRecyclerView.setLayoutManager(linearLayoutManager);
            messageRecyclerView.setHasFixedSize(true);
            messageRecyclerView.setAdapter(fetch());
            adapter.notifyDataSetChanged();

            //Implement authStateListener to check and confirm user login
            authStateListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        OnSignedInInitialize(firebaseUser.getDisplayName());

                    } else {
                        OnSignedOutCleanUp();
                        startActivityForResult(
                                AuthUI.getInstance()
                                        .createSignInIntentBuilder()
                                        .setIsSmartLockEnabled(false)
                                        .setAvailableProviders(Arrays.asList(
                                                new AuthUI.IdpConfig.GoogleBuilder().build(),
                                                new AuthUI.IdpConfig.EmailBuilder().build()))
                                        .build(),
                                RC_SIGN_IN);
                    }
                }
            };

        }
    }

    //OnClick method that assigns function to views
    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.sendMessageFab) {
            if (messageEditText.getText().equals("")) {
                return;
            }
            else {
                {
                    Message message = new Message(mUsername, messageEditText.getText().toString(), null);
                    mDatabaseReference.push().setValue(message);
                }
                messageEditText.setText("");
            }
        }
        else if (i == R.id.sendImageImageView){
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/jpeg");
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
        }
        }

    //Method to get values from the firebase database and then populate the recycler view. A FirebaseRecyclerAdapter is returned
    private FirebaseRecyclerAdapter fetch() {
        {
            Query query = FirebaseDatabase.getInstance().getReference().child("messages");
            FirebaseRecyclerOptions<Message> options = new FirebaseRecyclerOptions.Builder<Message>().setQuery(query, new SnapshotParser<Message>() {
                @NonNull
                @Override
                public Message parseSnapshot(@NonNull DataSnapshot snapshot) {
                    return new Message(snapshot.child("name").getValue() + "",
                            snapshot.child("text").getValue() + "",
                            snapshot.child("photoUrl").getValue() + "");
                }
            }).build();
            adapter = new FirebaseRecyclerAdapter<Message, ViewHolder>(options) {

                @NonNull
                @Override
                public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
                    return new ViewHolder(view);
                }

                @Override
                protected void onBindViewHolder(@NonNull ViewHolder viewHolder, int i, final Message o) {
                    viewHolder.nameTextView.setText(o.getName());
                    final Message message = getItem(i);
                    boolean isMessage = message.getPhotoUrl().equals("null");

                    if (isMessage) {
                        viewHolder.messageTextView.setVisibility(View.VISIBLE);
                        viewHolder.photoImageView.setVisibility(View.GONE);
                        viewHolder.messageTextView.setText(message.getText());
                    } else {

                        viewHolder.messageTextView.setVisibility(View.GONE);
                        viewHolder.photoImageView.setVisibility(View.VISIBLE);
                        viewHolder.nameTextView.setVisibility(View.VISIBLE);
                        Glide.with(viewHolder.photoImageView.getContext())
                                .load(message.getPhotoUrl())
                                .into(viewHolder.photoImageView);
                    }
                }
            };
        }
        return adapter;
    }
    @Override
    public void onStart() {
        super.onStart();
            adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
            adapter.stopListening();
    }

    //This method pushes images to the firebase database.
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
            } else if (resultCode == Activity.RESULT_CANCELED) {
                getActivity().finish();
            }
        } else if (requestCode == RC_PHOTO_PICKER && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            final StorageReference photoRef = storageReference.child(uri.getLastPathSegment());
            UploadTask uploadTask = photoRef.putFile(uri);
            Task<Uri> uriTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    // Continue with the task to get the download URL
                    return photoRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        String downloadURL = downloadUri.toString();
                        Toast.makeText(getContext(), downloadURL, Toast.LENGTH_LONG).show();
                        Message message = new Message(mUsername, null, downloadURL);
                        mDatabaseReference.push().setValue(message);
                    } else {
                        // Handle failures
                        // ...
                    }
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        firebaseAuth.removeAuthStateListener(authStateListener);
    }
    private void OnSignedInInitialize(String userName) {
        mUsername = userName;
        adapter.startListening();
    }
    private void OnSignedOutCleanUp() {
        mUsername = ANONYMOUS;
            adapter.stopListening();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(getContext());
                return true;
                default:
                    return super.onOptionsItemSelected(item);
        }
    }
}
