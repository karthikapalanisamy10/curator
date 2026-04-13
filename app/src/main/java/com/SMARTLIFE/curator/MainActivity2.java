package com.SMARTLIFE.curator;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.security.ProviderInstaller;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity2 extends AppCompatActivity {

    private static final int VOICE_REQUEST_CODE = 101;
    private static final int NOTIFICATION_PERMISSION_CODE = 102;
    private Button btnMakePlan;
    private ImageView navSchedule, navProfile, ivNotifications, btnVoiceInput;
    private EditText etTaskTitle, etTaskWork, etStartDate, etEndDate, etCity, etTaskTime;
    private LinearLayout tasksContainer;
    private TextView tvWelcomeTitle, tvWeatherInfo;

    private FirebaseFirestore db;
    private DatabaseReference mDatabase;
    private String userId;
    private Calendar calendar = Calendar.getInstance();
    private Calendar alarmCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // Update security provider to fix handshake issues
        try {
            ProviderInstaller.installIfNeeded(this);
        } catch (Exception e) {
            Log.e("SECURITY", "ProviderInstaller failed: " + e.getMessage());
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        userId = user.getUid();
        db = FirebaseFirestore.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("users").child(userId).child("details");

        createNotificationChannel();
        checkAndRequestNotificationPermission();

        btnMakePlan = findViewById(R.id.btn_make_plan);
        navSchedule = findViewById(R.id.nav_schedule);
        navProfile = findViewById(R.id.nav_profile);
        ivNotifications = findViewById(R.id.iv_notifications);
        btnVoiceInput = findViewById(R.id.btn_voice_input);
        etTaskTitle = findViewById(R.id.et_task_title);
        etTaskWork = findViewById(R.id.et_task_work);
        etStartDate = findViewById(R.id.et_start_date);
        etEndDate = findViewById(R.id.et_end_date);
        etCity = findViewById(R.id.et_city);
        etTaskTime = findViewById(R.id.et_task_time);
        tasksContainer = findViewById(R.id.tasks_container);
        tvWelcomeTitle = findViewById(R.id.tv_welcome_title);
        tvWeatherInfo = findViewById(R.id.tv_weather_info);

        // Fetch name from Personal Details in Realtime Database
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String fullName = snapshot.child("fullName").getValue(String.class);
                    if (fullName != null && !fullName.isEmpty()) {
                        tvWelcomeTitle.setText("Hello, " + fullName + "!");
                    } else {
                        setDefaultWelcome(user);
                    }
                } else {
                    setDefaultWelcome(user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setDefaultWelcome(user);
            }
        });

        // Set default dates
        String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime());
        etStartDate.setText(today);
        etEndDate.setText(today);

        // Pickers
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));
        etTaskTime.setOnClickListener(v -> showTimePicker());

        loadTasks();
        
        fetchWeather("Chennai");

        btnMakePlan.setOnClickListener(v -> saveTask());
        btnVoiceInput.setOnClickListener(v -> startVoiceRecognition());
        
        ivNotifications.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
        navSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScheduleActivity3.class);
            intent.putExtra("DESTINATION", "Chennai");
            startActivity(intent);
        });
        navProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity3.class)));
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void setDefaultWelcome(FirebaseUser user) {
        String name = user.getDisplayName();
        if (name == null || name.isEmpty()) {
            if (user.getEmail() != null) {
                name = user.getEmail().split("@")[0];
            } else {
                name = "User";
            }
        }
        tvWelcomeTitle.setText("Hello, " + name + "!");
    }

    private void showDatePicker(EditText editText) {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            
            alarmCalendar.set(Calendar.YEAR, year);
            alarmCalendar.set(Calendar.MONTH, month);
            alarmCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            
            String format = "dd/MM/yyyy";
            SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
            editText.setText(sdf.format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            alarmCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            alarmCalendar.set(Calendar.MINUTE, minute);
            alarmCalendar.set(Calendar.SECOND, 0);
            
            String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            etTaskTime.setText(timeStr);
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void saveTask() {
        String title = etTaskTitle.getText().toString().trim();
        String work = etTaskWork.getText().toString().trim();
        String startDateStr = etStartDate.getText().toString().trim();
        String endDateStr = etEndDate.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String timeStr = etTaskTime.getText().toString().trim();

        if (title.isEmpty() || work.isEmpty()) {
            Toast.makeText(this, "Please fill title and work", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (city.isEmpty()) city = "Chennai";
        if (timeStr.isEmpty()) {
            Calendar now = Calendar.getInstance();
            timeStr = String.format(Locale.getDefault(), "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        }

        String taskId = db.collection("users").document(userId).collection("tasks").document().getId();
        long timestamp = System.currentTimeMillis();

        Task task = new Task(taskId, title, work, startDateStr, endDateStr, timeStr, city, true, timestamp);
        
        db.collection("users").document(userId).collection("tasks").document(taskId)
            .set(task)
            .addOnSuccessListener(aVoid -> {
                // Send immediate notification that plan is created
                sendImmediateNotification(task);
                
                // Schedule primary alarm (3 hours before if possible)
                scheduleNotification(task, 3, 0, "Upcoming Task Reminder");
                // Schedule second alarm (5 minutes before)
                scheduleNotification(task, 0, 5, "Task starting in 5 minutes!");
                
                etTaskTitle.setText("");
                etTaskWork.setText("");
                etCity.setText("");
                etTaskTime.setText("");
                fetchWeather(task.location);
                Toast.makeText(this, "Plan Created! Notifications scheduled.", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void sendImmediateNotification(Task task) {
        String title = "Plan Created!";
        String message = "Task: " + task.title + " has been added to your schedule.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "task_reminder")
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        try {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
            // Also save to in-app notification page
            saveNotificationToFirestore(title, message);
        } catch (SecurityException e) {
            Log.e("NOTIF", "Permission missing", e);
        }
    }

    private void saveNotificationToFirestore(String title, String message) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", title);
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);

        db.collection("users")
                .document(userId)
                .collection("notifications")
                .add(notification);
    }

    private void scheduleNotification(Task task, int hoursBefore, int minutesBefore, String reminderType) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Calendar taskTime = Calendar.getInstance();
            taskTime.setTime(sdf.parse(task.startDate + " " + task.time));

            Calendar notifyTime = (Calendar) taskTime.clone();
            if (hoursBefore > 0) notifyTime.add(Calendar.HOUR_OF_DAY, -hoursBefore);
            if (minutesBefore > 0) notifyTime.add(Calendar.MINUTE, -minutesBefore);

            if (notifyTime.getTimeInMillis() <= System.currentTimeMillis()) {
                return; 
            }

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, ReminderReceiver.class);
            intent.putExtra("task", reminderType + ": " + task.title + " - " + task.work);
            
            int uniqueId = (int) (task.timestamp + (hoursBefore * 60) + minutesBefore);
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, uniqueId, intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notifyTime.getTimeInMillis(), pendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, notifyTime.getTimeInMillis(), pendingIntent);
                }
            }
        } catch (Exception e) {
            Log.e("ALARM", "Error scheduling alarm: " + e.getMessage());
        }
    }

    private void loadTasks() {
        db.collection("users").document(userId).collection("tasks")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) return;
                tasksContainer.removeAllViews();
                if (value != null && !value.isEmpty()) {
                    boolean first = true;
                    for (QueryDocumentSnapshot document : value) {
                        Task task = document.toObject(Task.class);
                        if (task != null && !task.completed) {
                            if (first) {
                                fetchWeather(task.location);
                                first = false;
                            }
                            addTaskToUI(task);
                        }
                    }
                }
            });
    }

    private void addTaskToUI(Task task) {
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.VERTICAL);
        itemLayout.setPadding(32, 24, 32, 24);
        itemLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryPurple));
        
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, 16);
        itemLayout.setLayoutParams(layoutParams);

        itemLayout.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScheduleActivity3.class);
            intent.putExtra("DESTINATION", task.location);
            startActivity(intent);
        });

        itemLayout.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Delete Plan")
                .setMessage("Delete this plan?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("users").document(userId).collection("tasks").document(task.id).delete()
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Plan Deleted", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
            return true;
        });

        TextView titleView = new TextView(this);
        titleView.setText(task.title + " (" + task.location + ")");
        titleView.setTextSize(18);
        titleView.setTextColor(Color.BLACK);
        titleView.setTypeface(null, Typeface.BOLD);

        TextView workView = new TextView(this);
        workView.setText(task.work + " at " + task.time);
        workView.setTextSize(14);
        workView.setTextColor(Color.BLACK);

        TextView dateView = new TextView(this);
        dateView.setText("Schedule: " + task.startDate + " to " + task.endDate);
        dateView.setTextSize(12);
        dateView.setTextColor(ContextCompat.getColor(this, R.color.darkPurple));

        itemLayout.addView(titleView);
        itemLayout.addView(workView);
        dateView.setPadding(0, 8, 0, 0);
        itemLayout.addView(dateView);

        tasksContainer.addView(itemLayout);
    }

    private void fetchWeather(String city) {
        new WeatherTask(city).execute();
    }

    private class WeatherTask extends AsyncTask<Void, Void, String> {
        private String city;
        public WeatherTask(String city) { this.city = city; }

        @Override
        protected String doInBackground(Void... params) {
            HttpsURLConnection conn = null;
            try {
                Geocoder geocoder = new Geocoder(MainActivity2.this, Locale.US);
                List<Address> addresses = geocoder.getFromLocationName(city, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    double lat = addresses.get(0).getLatitude();
                    double lon = addresses.get(0).getLongitude();
                    
                    String urlString = String.format(Locale.US, 
                        "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current_weather=true", 
                        lat, lon);
                    
                    URL url = new URL(urlString);
                    conn = (HttpsURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(20000);
                    conn.setReadTimeout(20000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                    conn.setRequestProperty("Connection", "close");

                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                        br.close();
                        return sb.toString();
                    }
                }
            } catch (Exception e) { 
                Log.e("WEATHER_HOME", "Error: " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                try {
                    JSONObject json = new JSONObject(result);
                    JSONObject current = json.getJSONObject("current_weather");
                    double temp = current.getDouble("temperature");
                    int code = current.getInt("weathercode");
                    tvWeatherInfo.setText("Weather in " + city + ": " + temp + "°C (" + getWeatherDescription(code) + ")");
                } catch (Exception ignored) {}
            } else {
                tvWeatherInfo.setText("Weather unavailable for " + city);
            }
        }
    }

    private String getWeatherDescription(int code) {
        if (code == 0) return "Clear sky";
        if (code >= 1 && code <= 3) return "Partly cloudy";
        if (code == 45 || code == 48) return "Foggy";
        if (code >= 51 && code <= 55) return "Drizzle";
        if (code >= 61 && code <= 65) return "Rainy";
        if (code >= 71 && code <= 75) return "Snowy";
        if (code >= 80 && code <= 82) return "Rain showers";
        if (code >= 95) return "Thunderstorm";
        return "Overcast";
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        try {
            startActivityForResult(intent, VOICE_REQUEST_CODE);
        } catch (Exception a) {
            Toast.makeText(this, "Voice input not supported", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) etTaskWork.setText(result.get(0));
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("task_reminder", "Curator", NotificationManager.IMPORTANCE_HIGH);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please enable notifications to receive reminders", Toast.LENGTH_LONG).show();
            }
        }
    }
}
