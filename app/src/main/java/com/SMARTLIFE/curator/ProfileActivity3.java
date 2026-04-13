package com.SMARTLIFE.curator;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity3 extends AppCompatActivity {

    private ImageView navHome, navSchedule;
    private TextView btnLogout, btnBack, tvUserName, tvUserEmail;
    private TextView btnPersonalDetails;
    private SwitchCompat switchLocation;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LocationManager.PROVIDERS_CHANGED_ACTION.equals(intent.getAction())) {
                updateLocationSwitchState();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile3);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Configure Google Sign-In for logout
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        navHome = findViewById(R.id.nav_home);
        navSchedule = findViewById(R.id.nav_schedule);
        btnLogout = findViewById(R.id.btn_logout);
        btnBack = findViewById(R.id.btn_back);
        tvUserName = findViewById(R.id.tv_user_name);
        tvUserEmail = findViewById(R.id.tv_user_email);
        
        btnPersonalDetails = findViewById(R.id.btn_personal_details);
        switchLocation = findViewById(R.id.switch_location);

        updateLocationSwitchState();

        switchLocation.setOnClickListener(v -> {
            boolean isChecked = switchLocation.isChecked();
            if (isChecked) {
                if (!checkLocationPermission()) {
                    requestLocationPermission();
                } else if (!isLocationEnabled()) {
                    Toast.makeText(this, "Please turn on device location in settings", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            } else {
                Toast.makeText(this, "Turn off device location in settings to fully disable tracking", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        if (currentUser != null) {
            tvUserEmail.setText(currentUser.getEmail());
            
            // Set default name first
            String name = currentUser.getDisplayName();
            if (name == null || name.isEmpty()) {
                name = currentUser.getEmail().split("@")[0];
            }
            tvUserName.setText(name);

            // Fetch name from Personal Details in Realtime Database
            mDatabase = FirebaseDatabase.getInstance().getReference("users")
                    .child(currentUser.getUid()).child("details");
            
            mDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String fullName = snapshot.child("fullName").getValue(String.class);
                        if (fullName != null && !fullName.isEmpty()) {
                            tvUserName.setText(fullName);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("Profile", "Error fetching name: " + error.getMessage());
                }
            });
        }

        // Open Personal Details Page
        btnPersonalDetails.setOnClickListener(v -> 
            startActivity(new Intent(ProfileActivity3.this, PersonalDetailsActivity.class)));

        navHome.setOnClickListener(v -> startActivity(new Intent(this, MainActivity2.class)));
        navSchedule.setOnClickListener(v -> startActivity(new Intent(this, ScheduleActivity3.class)));
        btnBack.setOnClickListener(v -> finish());

        btnLogout.setOnClickListener(v -> {
            // Sign out from Firebase
            mAuth.signOut();
            
            // Sign out from Google to allow account selection next time
            mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                Toast.makeText(ProfileActivity3.this, "Logged Out Successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ProfileActivity3.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(locationReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(locationReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLocationSwitchState();
    }

    private void updateLocationSwitchState() {
        boolean isGpsOn = isLocationEnabled();
        boolean hasPermission = checkLocationPermission();
        switchLocation.setChecked(isGpsOn && hasPermission);
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateLocationSwitchState();
                if (!isLocationEnabled()) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            } else {
                switchLocation.setChecked(false);
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
