package com.example.myapplication;

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
}
