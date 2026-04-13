package com.SMARTLIFE.curator;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

public class SignInActivity extends AppCompatActivity {

    private static final String TAG = "SignInActivity";
    private EditText etName, etEmail, etPassword;
    private Button btnSignIn;
    private TextView btnBack;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mAuth = FirebaseAuth.getInstance();

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnSignIn = findViewById(R.id.btn_sign_in);
        btnBack = findViewById(R.id.btn_back);

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters long");
            etPassword.requestFocus();
            return;
        }

        // Show a "Processing..." message
        Toast.makeText(this, "Registering...", Toast.LENGTH_SHORT).show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        Toast.makeText(SignInActivity.this, "Welcome " + name + "! Registration Successful.", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(SignInActivity.this, MainActivity2.class));
                        finish();
                    } else {
                        // Get the specific exception message
                        String error;
                        if (task.getException() != null) {
                            error = task.getException().getMessage();
                            Log.e(TAG, "Registration Error: ", task.getException());
                        } else {
                            error = "Unknown authentication error";
                        }
                        
                        // Show the REAL error message to the user
                        Toast.makeText(SignInActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
