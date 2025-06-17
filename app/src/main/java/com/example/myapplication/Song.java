package com.example.myapplication;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Song {

    private String name;
    private int timeDuration;
    private String filePath;
    private String songid;
    // Song.java
    private int platcount; // 播放次数
    private String singer; // 歌手
    public String getSinger() {
        return singer;
    }
    public int getPlatcount() {
        return platcount;
    }

    public void setPlatcount(int platcount) {
        this.platcount = platcount;
    }

    public String getSongid() {
        return songid;
    }

    public void setSongid(String songid) {
        this.songid = songid;
    }
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
