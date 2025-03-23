package com.example.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "MusicPrefs"; // SharedPreferences 名称
    private static final String KEY_PLAYLIST = "playlist"; // 存储播放列表的 key
    private static final String KEY_CURRENT_POSITION = "current_position"; // 存储播放进度
    private static final String KEY_CURRENT_SONG = "current_song"; // 存储当前歌曲 index
    private static final int PICK_AUDIO_REQUEST = 1;
    private enum PlayMode {LOOP, SHUFFLE, SINGLE}
    private static final SimpleDateFormat timeFormat =
            new SimpleDateFormat("mm:ss", Locale.getDefault());
    private PlayMode currentPlayMode = PlayMode.LOOP;
    private List<Integer> shuffleOrder = new ArrayList<>();
    private int shuffleIndex = 0;
    private MediaPlayer mediaPlayer;
    private ActivityResultLauncher<Intent> filePickerLauncher;
//    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private List<Song> songList;
    private SeekBar seekBar;
    private TextView progress = null;
    private Thread playerThread;
    private boolean isSeeking = false;
    private Handler handler = new Handler();
    private final Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && !isSeeking) {
                int currentPosition = mediaPlayer.getCurrentPosition();
                seekBar.setProgress(currentPosition);
            }
            handler.postDelayed(this, 500);
        }
    };
    private volatile boolean isPlaying = false;
    private Map<Integer, String> songMap = new HashMap<>();
    private SongAdapter songAdapter;
    private int currentPlayingIndex = -1;
    private Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPlaying && !isSeeking) {
                int currentPosition = mediaPlayer.getCurrentPosition();
                seekBar.setProgress(currentPosition);
                updateProgressText(currentPosition, mediaPlayer.getDuration());
                handler.postDelayed(this, 500); // 每 500 毫秒更新一次
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songList = new ArrayList<>();
        updatePlayModeText();
        seekBar = findViewById(R.id.seekBar);
        progress = findViewById(R.id.tv_current_file);
        TextView tvStatus = findViewById(R.id.tv_status);
        ImageButton prevButton = findViewById(R.id.btn_prev);
        ImageButton nextButton = findViewById(R.id.btn_next);
        mediaPlayer = MediaPlayer.create(this, R.raw.test_audio);
        seekBar.setMax(mediaPlayer.getDuration());
        TextView currentSong = findViewById(R.id.tv_current_song_name);
        songList = new ArrayList<>();
        songAdapter = new SongAdapter(songList, position -> playSong(position));
        RecyclerView songRecyclerView = findViewById(R.id.song_recycler_view);
        songRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        songRecyclerView.setAdapter(songAdapter);
//        ImageButton btnMore = findViewById(R.id.btn_more);
//        songList = new ArrayList<>();
//
        //加载上一次播放列表和播放进度
        loadPlaylist();
//        注册文件选择器
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri audioUri = result.getData().getData();

                        getContentResolver().takePersistableUriPermission(
                                audioUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                        handleSelectedAudio(audioUri);
                    }
                });
//建立hash映射检索歌名
        songMap.put(R.raw.test_audio, "顶上的风景");
        Log.d("DEBUG", "songMap 初始化完毕: " + songMap.toString());
        progress = findViewById(R.id.tv_current_file);

//meidaplayer初始化
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.test_audio);
            if (mediaPlayer != null) {
                seekBar.setMax(mediaPlayer.getDuration());
                progress.setText("准备就绪");
            }
        } catch (Exception e) {
            Log.e("MediaPlayer", "初始化失败", e);
        }

        TextView finalCurrentSong = currentSong;

        //启动按钮
        findViewById(R.id.btn_play).setOnClickListener(v -> {
            if (mediaPlayer == null) return;


            String currentSongName = songMap.get(R.raw.test_audio);
            finalCurrentSong.setText(currentSongName);
            if (playerThread != null && playerThread.isAlive()) {
                tvStatus.setText("已暂停");
                playerThread.interrupt();
                mediaPlayer.pause();
            }

            // 优化播放控制逻辑
            if (!mediaPlayer.isPlaying()) {
                tvStatus.setText("播放中...");
                mediaPlayer.start();
                isPlaying = true;
                playerThread = new Thread(this::updatePlayProgress);
                playerThread.start();
                handler.post(updateProgressRunnable);
            }
        });


//暂停按钮
        findViewById(R.id.btn_pause).setOnClickListener(v -> {
            if (mediaPlayer == null) return;

            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                isPlaying = false;
                tvStatus.setText("已暂停");
//                playerThread.interrupt();
                handler.removeCallbacks(updateProgressRunnable);

            }
        });
//终止按钮
        findViewById(R.id.btn_stop).setOnClickListener(v -> stopPlayback());
        //添加歌曲
        findViewById(R.id.btn_add_song).setOnClickListener((v -> addSong()));
        //删除歌曲
        findViewById(R.id.btn_delete_song).setOnClickListener(v -> removeSelectedSong());
        //上一首
        prevButton.setOnClickListener(v -> playPrevious());
        nextButton.setOnClickListener(v -> playNext());
//进度条
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
//                    updatePlayProgress();
                    updateProgressText(progress, mediaPlayer.getDuration());
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeeking = true;
                handler.removeCallbacks(updateProgressRunnable);

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeeking = false;
                mediaPlayer.seekTo(seekBar.getProgress());
                if (isPlaying) {
                    handler.post(updateProgressRunnable);
                }

            }
        });
        //menu button
        findViewById(R.id.btn_switch_mode).setOnClickListener(v -> switchPlayMode());
    }

    private void playSong(int position) {
        if (position < 0 || position >= songList.size()) ;
        Song song = songList.get(position);
        Uri uri = Uri.parse(song.getFilePath()); // 直接解析 URI

        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(this, uri); // ✅ 使用 URI 代替文件路径
            mediaPlayer.prepare();
            mediaPlayer.start();

            TextView tv_status = findViewById(R.id.tv_status);
            TextView currentSong = findViewById(R.id.tv_current_song_name);
            //update current index of player
            currentPlayingIndex = position;

            songAdapter.setPlayingPosition(position); // 更新 UI
            tv_status.setText("正在播放：" + song.getName());
            currentSong.setText(song.getName());

            //使当前播放的歌曲滚动到播放列表的第一位置，值改变ui
            RecyclerView songRecyclerView = findViewById(R.id.song_recycler_view);
            ((LinearLayoutManager) songRecyclerView.getLayoutManager()).scrollToPositionWithOffset(position, 0);
            //when click the music in player will init the seekbar to update the progress
            seekBar.setMax(mediaPlayer.getDuration());
            seekBar.setProgress(0);
            handler.post(updateProgressRunnable);
            //when current song have done,the next will play automatically
            mediaPlayer.setOnCompletionListener(mp -> playNext());
            isPlaying = true;

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "播放失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //delete the song we chosen
    private void removeSelectedSong() {
        if (currentPlayingIndex >= 0 && currentPlayingIndex < songList.size()) {
            removeSong(currentPlayingIndex);
        }
    }
    //remove song
    private void removeSong(int position) {
        if (position >= 0 && position < songList.size()) {
            boolean wasPlaying = (position == currentPlayingIndex);

            // 删除歌曲
            songList.remove(position);
            songAdapter.notifyItemRemoved(position);
            songAdapter.notifyDataSetChanged();

            // 重新计算当前播放索引
            if (wasPlaying) {
                if (!songList.isEmpty()) {
                    int newIndex = (int) (Math.random() * songList.size()); // 随机选择新歌
                    playSong(newIndex);
                } else {
                    // 播放列表为空，停止播放
                    if (mediaPlayer != null) {
                        mediaPlayer.stop();
                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                    currentPlayingIndex = -1;
                    isPlaying = false;
                }
            }

            // 保存修改后的播放列表
            savePlaylist();
        }
    }

    private void playPrevious() {
        if (currentPlayingIndex > 0) {
            playSong(--currentPlayingIndex);
        }
    }


    private void playNext() {
        if (currentPlayMode == PlayMode.SINGLE) {
            playSong(currentPlayingIndex);
        } else if (currentPlayMode == PlayMode.SHUFFLE) {
            if (shuffleIndex >= shuffleOrder.size()) {
                Collections.shuffle(shuffleOrder);
                shuffleIndex = 0;
            }
            playSong(shuffleOrder.get(shuffleIndex++));
        } else {
            currentPlayingIndex = (currentPlayingIndex + 1) % songList.size();
            playSong(currentPlayingIndex);
        }
    }
    //save current playlist and song progress
//    private void savePlaylist() {
//        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = prefs.edit();
//
//        StringBuilder playlistData = new StringBuilder();
//        for  (Song song : songList) {
//            playlistData.append(song.getName()).append("|").append(song.getFilePath()).append("\n");
//        }
////        String playlistString = new Gson().toJson(songList);
//        editor.putString(KEY_PLAYLIST, playlistData.toString());
//        editor.putInt(KEY_CURRENT_SONG,mediaPlayer.getCurrentPosition());
//        if (mediaPlayer != null) {
//            editor.putInt(KEY_CURRENT_POSITION, mediaPlayer.getCurrentPosition());
//        }
//
//    }
    private void savePlaylist() {
        SharedPreferences preferences = getSharedPreferences("PlaylistPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        JSONArray jsonArray = new JSONArray();
        for (Song song : songList) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("title", song.getName());
                jsonObject.put("time", song.getTimeDuration());
                jsonObject.put("path", song.getFilePath()); // 存储歌曲路径
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        editor.putString("playlist", jsonArray.toString());
        editor.putInt("lastPlayingIndex", currentPlayingIndex);
        editor.apply();
    }

    //加载列表
//    private void loadPlaylist() {
//        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
//        String savedPlaylist = prefs.getString(KEY_PLAYLIST, "");
//        int savedPosition = prefs.getInt(KEY_CURRENT_POSITION, 0);
//        int savedSongIndex = prefs.getInt(KEY_CURRENT_SONG, 0);
//
//        if (!savedPlaylist.isEmpty()) {
//            songList.clear();
//            String[] songs = savedPlaylist.split("\n");
//            for (String songData : songs) {
//                String[] details = songData.split("\\|");
//                if (details.length == 2) {
//                    String name = details[0];
//                    String filePath = details[1];
//                    songList.add(new Song(name, 0, filePath));
//                }
//            }
//            songAdapter.notifyDataSetChanged();
//        }
//        if (savedSongIndex >= 0 && savedSongIndex < songList.size()) {
//            playSong(savedPosition);
//            mediaPlayer.seekTo(savedPosition);
//        }
//    }
    private void loadPlaylist() {
        SharedPreferences preferences = getSharedPreferences("PlaylistPrefs", MODE_PRIVATE);
        String jsonPlaylist = preferences.getString("playlist", null);
        int lastPlayingIndex = preferences.getInt("lastPlayingIndex", -1);

        if (jsonPlaylist != null) {
            songList.clear();
            try {
                JSONArray jsonArray = new JSONArray(jsonPlaylist);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String title = jsonObject.getString("title");
                    int time = jsonObject.getInt("time");
                    String path = jsonObject.getString("path");

                    songList.add(new Song(title,time,path));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (!songList.isEmpty() && lastPlayingIndex >= 0 && lastPlayingIndex < songList.size()) {
                playSong(lastPlayingIndex); // 继续播放上次的歌曲
            }
        }
    }

    //switch mode
    private void switchPlayMode() {
        if (currentPlayMode == PlayMode.LOOP) {
            currentPlayMode = PlayMode.SHUFFLE;
            shuffleOrder.clear();
            for (int i = 0; i < songList.size(); i++) {
                shuffleOrder.add(i);
            }
            Collections.shuffle(shuffleOrder);
            shuffleIndex = 0;
        } else if (currentPlayMode == PlayMode.SHUFFLE) {
            currentPlayMode = PlayMode.SINGLE;
        } else {
            currentPlayMode = PlayMode.LOOP;
        }
        updatePlayModeText();
    }

    private void updatePlayModeText() {
        String modeText;
        switch (currentPlayMode) {
            case SHUFFLE:
                modeText = "随机播放";
                break;
            case SINGLE:
                modeText = "单曲循环";
                break;
            default:
                modeText = "列表循环";
                break;
        }
        TextView tv_playMode = findViewById(R.id.tv_play_mode);
        tv_playMode.setText("播放模式: " + modeText);
    }

//    private void generateShuffleOrder() {
//        shuffleOrder = new ArrayList<>();
//        for (int i = 0; i < songList.size(); i++) {
//            shuffleOrder.add(i);
//        }
//        Collections.shuffle(shuffleOrder);
//        shuffleIndex = 0;
//    }

    //    private static final int REQUEST_CODE_PICK_SONG = 101;
    //添加歌曲
    private void addSong() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_AUDIO_REQUEST);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                String fileName = getFileName(uri);
                int duration = getTimeDuration(uri);

                songList.add(new Song(fileName, duration, uri.toString())); // 保存 URI
                songAdapter.notifyDataSetChanged();
                savePlaylist();
            }
        }
    }

    private int getTimeDuration(Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, uri);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return durationStr != null ? Integer.parseInt(durationStr) / 1000 : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    //处理音频
    private void handleSelectedAudio(Uri audioUri) {
        //getname
        String filename = getFileName(audioUri);
        Toast.makeText(this, "选择： " + filename, Toast.LENGTH_SHORT);

        Song newSong = new Song(filename, 0, "/storage/emulated/0/Music/song.mp3"); // 这里 0 代表时间长度，可扩展
        songList.add(newSong);
        adapter.notifyItemInserted(songList.size() - 1);
    }

    private String getFileName(Uri uri) {
        String displayName = "";
        try (Cursor cursor = getContentResolver().query(
                uri, null, null, null, null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                displayName = cursor.getString(
                        cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                );
            }
        }
        return displayName;
    }

    private void stopPlayback() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop(); // 停止播放
                try {
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mediaPlayer.seekTo(0);
            isPlaying = false;
            handler.removeCallbacks(updateProgressRunnable);
            updateProgressText(0, mediaPlayer.getDuration());
            TextView tvStatus = findViewById(R.id.tv_status);
            tvStatus.setText("已停止");
            seekBar.setProgress(0);
        }
        TextView tvStatus = findViewById(R.id.tv_status);

        isPlaying = false;
        if (playerThread != null && playerThread.isAlive()) {
            playerThread.interrupt();
        }

        handler.post(() -> {
            progress.setText("播放进度: 00:00 / " + timeFormat.format(mediaPlayer.getDuration()));
            tvStatus.setText("已停止");
            seekBar.setProgress(0); // 重置进度条
        });

        Log.d("DEBUG", "播放已停止，进度重置");
    }


    private void updatePlayProgress() {
        while (isPlaying && !Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(500);
                if (mediaPlayer != null) {
                    final int current = mediaPlayer.getCurrentPosition();
                    final int duration = mediaPlayer.getDuration();

                    handler.post(() -> {
                        progress.setText("播放进度: " + timeFormat.format(current) +
                                " / " + timeFormat.format(duration));
                    });
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void updateProgressText(int current, int duration) {
        progress.setText("播放进度: " + timeFormat.format(current) + " / " + timeFormat.format(duration));
    }

//    private void startSeekBarUpdate() {
//        handler.removeCallbacks(updateSeekBar);
//        handler.post(updateSeekBar);
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        savePlaylist();//退出时保存
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }


}


