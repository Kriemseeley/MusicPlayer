package com.example.myapplication;

public class SongID {
    private String name;
    private int resourceID;

    public SongID(String name, int resourceID) {
        this.name = name;
        this.resourceID = resourceID;
    }

    public int getResourceID() {
        return resourceID;
    }

    public String getName() {
        return name;
    }
}
