package com.SMARTLIFE.curator;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String taskTitle = intent.getStringExtra("task");
        if (taskTitle == null) taskTitle = "Task Reminder";
        
        boolean isOneDayBefore = intent.getBooleanExtra("isOneDayBefore", false);

        String displayTitle = isOneDayBefore ? "Upcoming Task Tomorrow" : "Task Reminder";
        String displayText = isOneDayBefore ? "Don't forget: " + taskTitle : taskTitle;

        // Ensure notification channel exists (redundant but safe)
        createNotificationChannel(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "task_reminder")
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(displayTitle)
                .setContentText(displayText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            int notificationId = (int) System.currentTimeMillis();
            notificationManager.notify(notificationId, builder.build());
            
            // Log to Firestore so it shows up in the Notification Activity
            saveNotificationToFirestore(displayTitle, displayText);
            
        } catch (SecurityException e) {
            Log.e("ReminderReceiver", "Notification permission denied", e);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("task_reminder", "Curator", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void saveNotificationToFirestore(String title, String message) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Map<String, Object> notification = new HashMap<>();
            notification.put("title", title);
            notification.put("message", message);
            notification.put("timestamp", System.currentTimeMillis());
            notification.put("read", false);

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .collection("notifications")
                    .add(notification);
        }
    }
}
