package Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.example.blog.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.soundcloud.android.crop.Crop;
import com.soundcloud.android.crop.CropImageView;

import java.io.File;

public class CreateAccountActivity extends AppCompatActivity {

    private EditText firstName;
    private EditText lastName;
    private EditText email;
    private EditText password;
    private Button createAccountBtn;
    private ImageButton profilePic;
    private ProgressDialog mProgressDialog;
    private DatabaseReference mDatabaseReference;
    private FirebaseDatabase mDatabase;
    private StorageReference mFirebaseStorage;
    private FirebaseAuth mAuth;
    private Uri resultUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        mDatabase = FirebaseDatabase.getInstance();
        mDatabaseReference = mDatabase.getReference().child("MUsers");

        mAuth = FirebaseAuth.getInstance();

        mFirebaseStorage = FirebaseStorage.getInstance().getReference().child("MBlog_Profile_Pics");

        mProgressDialog = new ProgressDialog(this);

        firstName  =  findViewById(R.id.firstNameAct);
        lastName   =  findViewById(R.id.lastNameAct);
        email      =  findViewById(R.id.emailAct);
        password   =  findViewById(R.id.passwordAct);
        profilePic =  findViewById(R.id.profilePic);
        createAccountBtn =  findViewById(R.id.createAccoutAct);

        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Crop.pickImage(CreateAccountActivity.this);
            }
        });

        createAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewAccount();
            }
        });
    }

    private void createNewAccount() {
        final String name  = firstName.getText().toString().trim();
        final String lname = lastName.getText().toString().trim();
        String em  = email.getText().toString().trim();
        String pwd = password.getText().toString().trim();

        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(lname)
                && !TextUtils.isEmpty(em) && !TextUtils.isEmpty(pwd)) {

            mProgressDialog.setMessage("Creating Account...");
            mProgressDialog.show();

            mAuth.createUserWithEmailAndPassword(em, pwd)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                @Override
                public void onSuccess(AuthResult authResult) {

                    if (authResult != null) {

                        StorageReference imagePath = mFirebaseStorage.child(resultUri.getLastPathSegment());

                        imagePath.putFile(resultUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                String userid = mAuth.getCurrentUser().getUid();

                                DatabaseReference currenUserDb = mDatabaseReference.child(userid);
                                currenUserDb.child("firstname").setValue(name);
                                currenUserDb.child("lastname").setValue(lname);
                                currenUserDb.child("image").setValue(resultUri.toString());

                                mProgressDialog.dismiss();

                                //send users to postList
                                Intent intent = new Intent(CreateAccountActivity.this, PostListActivity.class );
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);

                            }
                        });
                    }
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == Crop.REQUEST_PICK) {
                resultUri = data.getData();
                Uri destination_uri = Uri.fromFile(new File(getCacheDir(), "cropped"));

                Crop.of(resultUri, destination_uri).withAspect(3,2).start(this);
                profilePic.setImageURI(Crop.getOutput(data));
            }
            else if (requestCode == Crop.REQUEST_CROP){
                if (resultCode == RESULT_OK) {
                    profilePic.setImageURI(Crop.getOutput(data));
                }
                else if (resultCode == Crop.RESULT_ERROR) {

                }
            }
        }
    }
}
