package com.example.myapplication;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Song {

    private String name;
    private int timeDuration;
    private String filePath;



    public Song(String name, int timeDuration, String filePath) {
        this.name = name;
        this.timeDuration = timeDuration;
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }
    public String getName() {
        return name;
    }

    public int getTimeDuration() {
        return timeDuration;
    }
    public String getFormattedDuration() {
        SimpleDateFormat formatter = new SimpleDateFormat("mm:ss", Locale.getDefault());
        return formatter.format(new Date(timeDuration));
    }
}
