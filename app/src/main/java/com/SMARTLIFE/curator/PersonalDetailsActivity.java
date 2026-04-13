package com.SMARTLIFE.curator;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public class PersonalDetailsActivity extends AppCompatActivity {
    private EditText etName, etPhone, etAddress;
    private DatabaseReference mDatabase;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_details);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        userId = user.getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference("users").child(userId).child("details");

        etName = findViewById(R.id.et_full_name);
        etPhone = findViewById(R.id.et_phone);
        etAddress = findViewById(R.id.et_address);
        Button btnSave = findViewById(R.id.btn_save_details);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Load existing details
        loadPersonalDetails();

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String address = etAddress.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Name is required");
                return;
            }

            Map<String, Object> details = new HashMap<>();
            details.put("fullName", name);
            details.put("phone", phone);
            details.put("address", address);
            
            mDatabase.setValue(details).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Details Saved Successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void loadPersonalDetails() {
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("fullName").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String address = snapshot.child("address").getValue(String.class);

                    if (name != null) etName.setText(name);
                    if (phone != null) etPhone.setText(phone);
                    if (address != null) etAddress.setText(address);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PersonalDetails", "Error loading data: " + error.getMessage());
            }
        });
    }
}
