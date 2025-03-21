package com.example.myapplication;
import java.util.ArrayList;
import java.util.List;
public class PlaylistManager {
    private List<Song> playlist;

    public PlaylistManager() {
        playlist = new ArrayList<>();
    }

    public void addSong(Song song) {
        playlist.add(song);
    }
    public void removeSong(int position) {
        if (position >= 0 && position < playlist.size()) {
            playlist.remove(position);
        }
    }
    public List<Song> getPlayList() {
        return playlist;
    }
}
