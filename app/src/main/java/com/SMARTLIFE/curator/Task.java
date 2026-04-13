package com.SMARTLIFE.curator;

public class Task {
    public String id;
    public String title;
    public String work;
    public String startDate;
    public String endDate;
    public String time;
    public String location;
    public long timestamp;
    public boolean completed;
    public boolean hasRoute;

    public Task() {
        // Default constructor required for calls to DataSnapshot.getValue(Task.class)
    }

    public Task(String id, String title, String work, String startDate, String endDate, String time, String location, boolean hasRoute, long timestamp) {
        this.id = id;
        this.title = title;
        this.work = work;
        this.startDate = startDate;
        this.endDate = endDate;
        this.time = time;
        this.location = location;
        this.hasRoute = hasRoute;
        this.timestamp = timestamp;
        this.completed = false;
    }
}
