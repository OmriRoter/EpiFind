package com.example.epifind;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.epifind.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private AppCompatImageView main_IMG_image;
    private TextView main_LBL_name;
    private TextView main_LBL_email;
    private TextView main_LBL_phone;
    private TextView main_LBL_imageLink;
    private TextView main_LBL_uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();
        initViews();
    }

    private void initViews() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        Glide
                .with(this)
                .load(user.getPhotoUrl())
                .centerCrop()
                .placeholder(R.drawable.epifinlogo)
                .into(main_IMG_image);
        main_LBL_name.setText(user.getDisplayName());
        main_LBL_email.setText(user.getEmail());
        main_LBL_phone.setText(user.getPhoneNumber());
        main_LBL_imageLink.setText(user.getPhotoUrl().toString());
        main_LBL_uid.setText(user.getUid());
    }

    private void findViews() {
        main_IMG_image = findViewById(R.id.main_IMG_image);
        main_LBL_name = findViewById(R.id.main_LBL_name);
        main_LBL_email = findViewById(R.id.main_LBL_email);
        main_LBL_phone = findViewById(R.id.main_LBL_phone);
        main_LBL_imageLink = findViewById(R.id.main_LBL_imageLink);
        main_LBL_uid = findViewById(R.id.main_LBL_uid);
    }
}