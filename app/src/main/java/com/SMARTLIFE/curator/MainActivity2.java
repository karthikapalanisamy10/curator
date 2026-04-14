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
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

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
        checkExactAlarmPermission();

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

        String today = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime());
        etStartDate.setText(today);
        etEndDate.setText(today);

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

    private void checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
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
            String format = "dd/MM/yyyy";
            SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
            editText.setText(sdf.format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
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
        String time = etTaskTime.getText().toString().trim();

        if (title.isEmpty() || work.isEmpty() || startDateStr.isEmpty() || endDateStr.isEmpty() || city.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> task = new HashMap<>();
        task.put("title", title);
        task.put("work", work);
        task.put("startDate", startDateStr);
        task.put("endDate", endDateStr);
        task.put("location", city);
        task.put("time", time);
        task.put("timestamp", System.currentTimeMillis());

        db.collection("users").document(userId).collection("tasks")
                .add(task)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Task Saved Successfully", Toast.LENGTH_SHORT).show();
                    etTaskTitle.setText("");
                    etTaskWork.setText("");
                    etTaskTime.setText("");
                    loadTasks();
                    scheduleReminder(title, work, startDateStr, time);
                    notifyTaskCreated(title, work);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error saving task", Toast.LENGTH_SHORT).show());
    }

    private void notifyTaskCreated(String title, String work) {
        String message = "Task \"" + title + "\" for " + work + " has been created successfully!";
        
        // 1. Mobile Display Bar Notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "reminder_channel";
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Task Created")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }

        // 2. Inside App Notification (Firestore)
        Map<String, Object> data = new HashMap<>();
        data.put("title", "Task Created");
        data.put("message", message);
        data.put("timestamp", System.currentTimeMillis());
        data.put("read", false);

        db.collection("users").document(userId).collection("notifications").add(data)
                .addOnFailureListener(e -> Log.e("NOTIFY", "Firestore save failed", e));
    }

    private void scheduleReminder(String title, String work, String date, String time) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Calendar scheduledTime = Calendar.getInstance();
            scheduledTime.setTime(sdf.parse(date + " " + time));
            
            long taskTime = scheduledTime.getTimeInMillis();
            long currentTime = System.currentTimeMillis();

            // 3 hours before
            long reminder3hrs = taskTime - (3 * 60 * 60 * 1000);
            // 5 minutes before
            long reminder5min = taskTime - (5 * 60 * 1000);

            if (reminder3hrs > currentTime) {
                setAlarm(reminder3hrs, title, "Reminder: 3 hours left for " + work);
            }

            if (reminder5min > currentTime) {
                setAlarm(reminder5min, title, "Reminder: 5 minutes left for " + work);
            }

            // Also notify at the exact time
            if (taskTime > currentTime) {
                setAlarm(taskTime, title, "It's time for " + work);
            }

        } catch (Exception e) {
            Log.e("ALARM", "Error scheduling alarm", e);
        }
    }

    private void setAlarm(long time, String title, String message) {
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("message", message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (int) time, // Use time as unique request code
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent);
            }
        }
    }

    private void loadTasks() {
        tasksContainer.removeAllViews();
        db.collection("users").document(userId).collection("tasks")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnCompleteListener(taskQuery -> {
                if (taskQuery.isSuccessful()) {
                    for (QueryDocumentSnapshot document : taskQuery.getResult()) {
                        Task task = document.toObject(Task.class);
                        task.id = document.getId();
                        addTaskToUI(task);
                    }
                }
            });
    }

    private void addTaskToUI(Task task) {
        LinearLayout outerLayout = new LinearLayout(this);
        outerLayout.setOrientation(LinearLayout.HORIZONTAL);
        outerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        outerLayout.setPadding(0, 0, 0, 16);

        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.VERTICAL);
        itemLayout.setPadding(32, 24, 32, 24);
        itemLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.rounded_box));
        
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        itemLayout.setLayoutParams(itemParams);

        itemLayout.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScheduleActivity3.class);
            intent.putExtra("DESTINATION", task.location);
            startActivity(intent);
        });

        // 🔥 LONG CLICK TO DELETE
        itemLayout.setOnLongClickListener(v -> {
            showDeleteDialog(task);
            return true;
        });

        TextView titleView = new TextView(this);
        titleView.setText(task.title + " (" + task.location + ")");
        titleView.setTextSize(18);
        titleView.setTextColor(Color.WHITE);
        titleView.setTypeface(null, Typeface.BOLD);

        TextView workView = new TextView(this);
        workView.setText(task.work + " at " + task.time);
        workView.setTextSize(14);
        workView.setTextColor(Color.WHITE);

        TextView dateView = new TextView(this);
        dateView.setText("Schedule: " + task.startDate + " to " + task.endDate);
        dateView.setTextSize(12);
        dateView.setTextColor(ContextCompat.getColor(this, R.color.lightPurple));

        itemLayout.addView(titleView);
        itemLayout.addView(workView);
        dateView.setPadding(0, 8, 0, 0);
        itemLayout.addView(dateView);

        outerLayout.addView(itemLayout);
        tasksContainer.addView(outerLayout);
    }

    private void showDeleteDialog(Task task) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete the task: \"" + task.title + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> deleteTask(task))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTask(Task task) {
        db.collection("users").document(userId).collection("tasks").document(task.id)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                    loadTasks();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete task", Toast.LENGTH_SHORT).show());
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
                    String urlString = String.format(Locale.US, "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current_weather=true", lat, lon);
                    URL url = new URL(urlString);
                    conn = (HttpsURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(20000);
                    conn.setReadTimeout(20000);
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
            NotificationChannel channel = new NotificationChannel("reminder_channel", "Reminders", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
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
