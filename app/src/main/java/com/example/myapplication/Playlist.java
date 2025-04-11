// --- START OF FILE Playlist.java ---
package com.example.myapplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single playlist containing a name and a list of songs.
 */
public class Playlist {
    private String name;
    private List<Song> songs;
    // Optional: Store last played index specifically for this playlist
    private int lastPlayingIndex = -1;

    // Constructor for creating a new playlist
    public Playlist(String name) {
        this.name = name;
        this.songs = new ArrayList<>();
    }

    // Constructor for loading from JSON
    public Playlist(String name, List<Song> songs, int lastPlayingIndex) {
        this.name = name;
        this.songs = songs != null ? songs : new ArrayList<>();
        this.lastPlayingIndex = lastPlayingIndex;
    }

    // --- Getters ---
    public String getName() {
        return name;
    }

    public List<Song> getSongs() {
        return songs;
    }

    public int getLastPlayingIndex() {
        return lastPlayingIndex;
    }

    // --- Setters ---
    public void setName(String name) {
        this.name = name;
    }

    public void setLastPlayingIndex(int lastPlayingIndex) {
        this.lastPlayingIndex = lastPlayingIndex;
    }


    // --- Methods for managing songs within this playlist ---
    public void addSong(Song song) {
        if (song != null) {
            this.songs.add(song);
        }
    }

    public void removeSong(int position) {
        if (position >= 0 && position < songs.size()) {
            songs.remove(position);
        }
    }

    public int getSongCount() {
        return songs.size();
    }

    // --- Methods for JSON serialization/deserialization (example) ---

    /**
     * Creates a JSONObject representation of this Playlist.
     * Note: Requires external handling of URI permissions during saving/loading.
     * @return JSONObject representing the playlist.
     * @throws JSONException If JSON creation fails.
     */
    public JSONObject toJson() throws JSONException {
        JSONObject playlistJson = new JSONObject();
        playlistJson.put("name", this.name);
        playlistJson.put("lastPlayingIndex", this.lastPlayingIndex); // Save last index per playlist

        JSONArray songsJsonArray = new JSONArray();
        for (Song song : this.songs) {
            JSONObject songJson = new JSONObject();
            songJson.put("title", song.getName() != null ? song.getName() : "");
            songJson.put("time", song.getTimeDuration());
            songJson.put("path", song.getFilePath() != null ? song.getFilePath() : "");
            songsJsonArray.put(songJson);
        }
        playlistJson.put("songs", songsJsonArray);

        return playlistJson;
    }

    /**
     * Creates a Playlist object from a JSONObject.
     * Note: Requires external handling of URI permissions after loading.
     * @param jsonObject The JSONObject containing playlist data.
     * @return A new Playlist object.
     * @throws JSONException If JSON parsing fails.
     */
    public static Playlist fromJson(JSONObject jsonObject) throws JSONException {
        String name = jsonObject.getString("name");
        int lastIndex = jsonObject.optInt("lastPlayingIndex", -1); // Load last index
        JSONArray songsJsonArray = jsonObject.getJSONArray("songs");
        List<Song> loadedSongs = new ArrayList<>();

        for (int i = 0; i < songsJsonArray.length(); i++) {
            JSONObject songJson = songsJsonArray.getJSONObject(i);
            // Create Song object - URI permission needs to be handled in MainActivity load
            Song song = new Song(
                    songJson.getString("title"),
                    songJson.getInt("time"),
                    songJson.getString("path")
            );
            loadedSongs.add(song);
        }
        return new Playlist(name, loadedSongs, lastIndex);
    }
}
// --- END OF FILE Playlist.java ---