package com.example.notescanai;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.io.Serializable;

@IgnoreExtraProperties
public class Note implements Serializable {
    private String id;
    private String title;
    private String text;
    private long timestamp;
    private boolean pinned;

    // Required empty constructor for Firebase
    public Note() {

        this.pinned = false;
        // Default constructor required for Firebase
    }

    public Note(String id, String text, long timestamp) {
        this.id = id;
        this.text = text;
        this.timestamp = timestamp;
        this.pinned = false;
    }

    public String getId() {
        return id;
    }
    public String getTitle() { return title; }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    // Returns a shortened preview of the text
    @Exclude
    public String getPreview() {
        return text;
    }

}
