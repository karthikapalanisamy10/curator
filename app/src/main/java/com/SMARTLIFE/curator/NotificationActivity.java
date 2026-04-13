package com.SMARTLIFE.curator;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationActivity extends AppCompatActivity {

    private LinearLayout notificationsContainer;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        notificationsContainer = findViewById(R.id.notifications_container);
        db = FirebaseFirestore.getInstance();
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            loadNotifications();
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void loadNotifications() {
        db.collection("users")
                .document(userId)
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    notificationsContainer.removeAllViews();
                    if (value != null && !value.isEmpty()) {
                        for (QueryDocumentSnapshot doc : value) {
                            addNotificationToUI(
                                    doc.getId(),
                                    doc.getString("title"),
                                    doc.getString("message"),
                                    doc.getLong("timestamp")
                            );
                        }
                    } else {
                        addEmptyState();
                    }
                });
    }

    private void addNotificationToUI(String docId, String title, String message, Long timestamp) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(32, 24, 32, 24);
        item.setGravity(Gravity.CENTER_VERTICAL);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        item.setLayoutParams(params);
        item.setBackgroundColor(Color.parseColor("#F8F9FA"));

        // Content Container (Vertical)
        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(16);
        tvTitle.setTextColor(Color.BLACK);
        tvTitle.setTypeface(null, Typeface.BOLD);

        TextView tvMsg = new TextView(this);
        tvMsg.setText(message);
        tvMsg.setTextColor(Color.DKGRAY);

        TextView tvTime = new TextView(this);
        String timeStr = "";
        if (timestamp != null) {
            timeStr = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(new Date(timestamp));
        }
        tvTime.setText(timeStr);
        tvTime.setTextSize(10);
        tvTime.setGravity(Gravity.START);

        contentLayout.addView(tvTitle);
        contentLayout.addView(tvMsg);
        contentLayout.addView(tvTime);

        // Delete Button
        ImageView btnDelete = new ImageView(this);
        btnDelete.setImageResource(android.R.drawable.ic_menu_delete);
        btnDelete.setPadding(16, 16, 16, 16);
        btnDelete.setColorFilter(Color.RED);
        btnDelete.setOnClickListener(v -> {
            db.collection("users")
                    .document(userId)
                    .collection("notifications")
                    .document(docId)
                    .delete()
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show());
        });

        item.addView(contentLayout);
        item.addView(btnDelete);
        
        notificationsContainer.addView(item);
    }

    private void addEmptyState() {
        TextView tv = new TextView(this);
        tv.setText("No new notifications");
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, 100, 0, 0);
        notificationsContainer.addView(tv);
    }
}
