package Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.example.blog.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.soundcloud.android.crop.Crop;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AddPostActivity extends AppCompatActivity {

    private ImageButton mPostImage;
    private EditText mPostTitle;
    private EditText mPostDesc;
    private Button mSubmitButton;
    private StorageReference mStorage;
    private DatabaseReference mPostDatabase;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private ProgressDialog mProgress;
    private Uri mImageUri;
    private static final int GALLERY_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        mProgress = new ProgressDialog(this);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mStorage = FirebaseStorage.getInstance().getReference(); // Getting storage link.

        mPostDatabase = FirebaseDatabase.getInstance().getReference().child("MBlog");

        mPostImage = findViewById(R.id.imageButton);
        mPostTitle = findViewById(R.id.postTitleEt);
        mPostDesc = findViewById(R.id.descriptionEt);
        mSubmitButton = findViewById(R.id.submitPost);

        mPostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
//                galleryIntent.setType("image/*");
//                startActivityForResult(galleryIntent, GALLERY_CODE);
                Crop.pickImage(AddPostActivity.this);
            }
        });

        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Posting to our database
                startPosting();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == GALLERY_CODE && resultCode == RESULT_OK) {
//            mImageUri = data.getData();
//            mPostImage.setImageURI(mImageUri);
//        }
        if (resultCode == RESULT_OK) {
            if (requestCode == Crop.REQUEST_PICK) {
                mImageUri = data.getData();
                Uri destination_uri = Uri.fromFile(new File(getCacheDir(), "cropped"));

                Crop.of(mImageUri, destination_uri).withAspect(1,1).start(this);
                mPostImage.setImageURI(Crop.getOutput(data));
            }
            else if (requestCode == Crop.REQUEST_CROP){
                if (resultCode == RESULT_OK) {
                    mPostImage.setImageURI(Crop.getOutput(data));
                }
                else if (resultCode == Crop.RESULT_ERROR) {

                }
            }
        }
    }

    private void startPosting() {

        mProgress.setMessage("Posting to blog...");
        mProgress.show();

        final String titleVal = mPostTitle.getText().toString().trim();
        final String descVal = mPostDesc.getText().toString().trim();

        if (!TextUtils.isEmpty(titleVal) && !TextUtils.isEmpty(descVal)
                && mImageUri != null) {
            //start the uploading...
            //mImageUri.getLastPathSegment() -> /image/myphoto.jpeg"

            final StorageReference filepath = mStorage.child("MBlog_images").
                    child(mImageUri.getLastPathSegment());
            filepath.putFile(mImageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()) {
                        filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String downloadUrl = uri.toString();
                                DatabaseReference newPost = mPostDatabase.push();

                                Map<String, String> dataToSave = new HashMap<>();
                                dataToSave.put("title", titleVal);
                                dataToSave.put("desc", descVal);
                                dataToSave.put("image", downloadUrl.toString());
                                dataToSave.put("timestamp", String.valueOf(System.currentTimeMillis()));
                                dataToSave.put("userid", mUser.getUid());
                                dataToSave.put("username", mUser.getEmail());

                                newPost.setValue(dataToSave);

                                mProgress.dismiss();

                                startActivity(new Intent(AddPostActivity.this, PostListActivity.class));
                                finish();
                            }
                        });
                    }
                }
            });
        }
    }
}


