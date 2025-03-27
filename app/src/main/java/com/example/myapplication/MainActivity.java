package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
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
    private boolean shouldAutoPlay = false;
    private boolean isSeeking = false;
    private Handler handler = new Handler();
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
        MaterialButton prevButton = findViewById(R.id.btn_prev);
        MaterialButton nextButton = findViewById(R.id.btn_next);
        mediaPlayer = MediaPlayer.create(this, R.raw.test_audio);
        seekBar.setMax(mediaPlayer.getDuration());
        TextView currentSong = findViewById(R.id.tv_current_song_name);
        songList = new ArrayList<>();
        songAdapter = new SongAdapter(songList, position -> playSong(position));
        RecyclerView songRecyclerView = findViewById(R.id.song_recycler_view);
        songRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        songRecyclerView.setAdapter(songAdapter);

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
        //meidaplayer初始化
        try {
            mediaPlayer = new MediaPlayer(); // 空初始化
            seekBar = findViewById(R.id.seekBar);
            progress = findViewById(R.id.tv_current_file);
        } catch (Exception e) {
            Log.e("MediaPlayer", "初始化失败", e);
        }
//建立hash映射检索歌名
//        songMap.put(R.raw.test_audio, "顶上的风景");
//        Log.d("DEBUG", "songMap 初始化完毕: " + songMap.toString());
        progress = findViewById(R.id.tv_current_file);


        TextView finalCurrentSong = currentSong;

        //启动按钮
        findViewById(R.id.btn_play).setOnClickListener(v -> {
                    shouldAutoPlay = true;
                    if (mediaPlayer == null) return;

                    // 在播放按钮点击事件中：
                    playerThread = new Thread(this::updatePlayProgress);
                    playerThread.start(); // 确保在同步块内执行

                    // 如果当前没有播放，自动播放第一首
                    if (currentPlayingIndex == -1) {
                        playSong(0);
                    } else {
                        if (!mediaPlayer.isPlaying()) {
                            mediaPlayer.start();
                            isPlaying = true;
                            handler.post(updateProgressRunnable);
                        }
                    }
                });

//            String currentSongName = songMap.get(R.raw.test_audio);
//            finalCurrentSong.setText(currentSongName);
//            if (playerThread != null && playerThread.isAlive()) {
////                tvStatus.setText("已暂停");
//                playerThread.interrupt();
//                mediaPlayer.pause();
//            }
//
//            // 优化播放控制逻辑
//            if (!mediaPlayer.isPlaying()) {
//                tvStatus.setText("播放中...");
//                mediaPlayer.start();
//                isPlaying = true;
//                playerThread = new Thread(this::updatePlayProgress);
//                playerThread.start();
//                handler.post(updateProgressRunnable);

//暂停按钮
        findViewById(R.id.btn_pause).setOnClickListener(v -> {
            shouldAutoPlay = false;

            synchronized (this) { // 添加同步块
                if (mediaPlayer == null) return;

                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    isPlaying = false;
                    tvStatus.setText("已暂停");

                    // 安全停止线程
                    if (playerThread != null && playerThread.isAlive()) {
                        playerThread.interrupt();
                        try {
                            playerThread.join(100); // 等待线程结束
                        } catch (InterruptedException e) {
                            Log.e("Thread", "线程中断异常", e);
                        }
                    }
                    playerThread = null; // 显式置空

                    handler.removeCallbacks(updateProgressRunnable);
                }
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
        if (position < 0 || position >= songList.size()) {
            Toast.makeText(this, "无效的播放位置", Toast.LENGTH_SHORT).show();
            return;
        }

        synchronized (this) {
            try {
                // 安全释放旧播放器
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                    mediaPlayer.release();
                    mediaPlayer = null;
                }

                Song song = songList.get(position);
                Uri uri = Uri.parse(song.getFilePath());

                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setDataSource(this, uri);
                mediaPlayer.prepareAsync();

                mediaPlayer.setOnPreparedListener(mp -> {
                    seekBar.setMax(mp.getDuration());
                    mp.start();
                    isPlaying = true;
                    currentPlayingIndex = position;
                    songAdapter.setPlayingPosition(position);
                    handler.post(updateProgressRunnable);

                    // 更新UI状态
                    TextView tvStatus = findViewById(R.id.tv_status);
                    TextView currentSong = findViewById(R.id.tv_current_song_name);
                    tvStatus.setText("正在播放：" + song.getName());
                    currentSong.setText(song.getName());
                });

            } catch (IOException e) {
                Log.e("Playback", "播放初始化失败", e);
                Toast.makeText(this, "播放失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void prepareMediaPlayer(int position) {
        if (position < 0 || position >= songList.size()) return;

        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }

            Song song = songList.get(position);
            Uri uri = Uri.parse(song.getFilePath());

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, uri);
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                seekBar.setMax(mp.getDuration());
                updateProgressText(0, mp.getDuration());
                if (shouldAutoPlay) {
                    mp.start();
                    isPlaying = true;
                    handler.post(updateProgressRunnable);
                    songAdapter.setPlayingPosition(position);
                }
                // 恢复播放进度（需在Song类中添加position字段）
                SharedPreferences prefs = getSharedPreferences("PlayerPrefs", MODE_PRIVATE);
                int savedPosition = prefs.getInt("lastPosition", 0);
                mp.seekTo(savedPosition);
            });

        } catch (IOException e) {
            Log.e("MediaPlayer", "初始化失败", e);
            Toast.makeText(this, "无法播放此文件", Toast.LENGTH_SHORT).show();
        }
    }

    //delete the song we chosen
    private void removeSelectedSong() {
        if (currentPlayingIndex >= 0 && currentPlayingIndex < songList.size()) {
            removeSong(currentPlayingIndex);
        }
    }

    //remove song
    @SuppressLint("NotifyDataSetChanged")
    private void removeSong(int position) {
        if (position < 0 || position >= songList.size()) return;

        synchronized (this) {
            // 处理当前播放歌曲被删除的情况
            boolean isCurrentSong = (position == currentPlayingIndex);
            boolean needStop = isCurrentSong && mediaPlayer != null;

            if (needStop) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                isPlaying = false;
                handler.removeCallbacks(updateProgressRunnable);
            }

            // 删除歌曲
            songList.remove(position);
            songAdapter.notifyItemRemoved(position);
            songAdapter.notifyDataSetChanged();

            // 修正播放索引
            if (isCurrentSong) {
                currentPlayingIndex = -1;
                if (!songList.isEmpty()) {
                    int newIndex = Math.min(position, songList.size() - 1);
                    playSong(newIndex); // 自动播放最近的有效歌曲
                }
            } else if (currentPlayingIndex > position) {
                currentPlayingIndex--; // 修正后续歌曲的索引
            }

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
//
//    private void savePlaylist() {
//        try {
//            SharedPreferences preferences = getSharedPreferences("PlaylistPrefs", MODE_PRIVATE);
//            SharedPreferences.Editor editor = preferences.edit();
//
//            JSONArray jsonArray = new JSONArray();
//            for (Song song : songList) {
//                JSONObject jsonObject = new JSONObject();
//                try {
//                    jsonObject.put("title", song.getName() != null ? song.getName() : "");
//                    jsonObject.put("time", song.getTimeDuration());
//                    jsonObject.put("path", song.getFilePath() != null ? song.getFilePath() : "");
//                    jsonArray.put(jsonObject);
//
//                    Log.d("Playlist", "✅ 已保存歌曲: " + song.getName());
//                } catch (JSONException e) {
//                    Log.e("Playlist", "❌ 保存歌曲失败: " + song.getName(), e);
//                }
//            }
//
//            // 添加整个 JSON 的异常捕获
//            String jsonString = jsonArray.toString();
//            editor.putString("playlist", jsonString);
//            Log.d("Playlist", "完整播放列表 JSON: " + jsonString);
//
//            editor.putInt("lastPlayingIndex", currentPlayingIndex);
//            editor.apply();
//            Log.d("Playlist", "✔ 播放列表保存成功");
//
//        } catch (Exception e) {
//            Log.e("Playlist", "‼ 保存播放列表时发生未知错误", e);
//        }
//    }

    //save playlist
    private void savePlaylist() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (Song song : songList) {
                Uri uri = Uri.parse(song.getFilePath());
                try {
                    getContentResolver().takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                } catch (SecurityException e) {
                    Log.e("URI Permission", "No permission for: " + uri);
                    continue;
                }
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("title", song.getName() != null ? song.getName() : "");
                jsonObject.put("time", song.getTimeDuration());
                jsonObject.put("path", song.getFilePath() != null ? song.getFilePath() : "");
                jsonArray.put(jsonObject);
            }

            JSONObject playlistObject = new JSONObject();
            playlistObject.put("playlist", jsonArray);
            playlistObject.put("lastPlayingIndex", currentPlayingIndex);
            playlistObject.put("playMode", currentPlayMode.name());//保存播放模式

            // 将 JSON 数据写入文件
            File file = new File(getExternalFilesDir(null), "playlist.json");
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(playlistObject.toString());
            }

            Log.d("Playlist", "播放列表保存成功");

        } catch (Exception e) {
            Log.e("Playlist", "保存播放列表失败", e);
            Toast.makeText(this, "播放列表保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    //laodplaylist
//    private void loadPlaylist() {
//        SharedPreferences preferences = getSharedPreferences("PlaylistPrefs", MODE_PRIVATE);
//        String jsonPlaylist = preferences.getString("playlist", null);
//        int lastPlayingIndex = preferences.getInt("lastPlayingIndex", -1);
//        boolean wasPlaying = preferences.getBoolean("wasPlaying", false);
//
//        Log.d("Playlist", "Loading playlist. JSON exists: " + (jsonPlaylist != null));
//        Log.d("Playlist", "Last index: " + lastPlayingIndex + ", Was playing: " + wasPlaying);
//
//        if (jsonPlaylist != null) {
//            songList.clear();
//            try {
//                JSONArray jsonArray = new JSONArray(jsonPlaylist);
//                for (int i = 0; i < jsonArray.length(); i++) {
//                    JSONObject jsonObject = jsonArray.getJSONObject(i);
//                    String title = jsonObject.getString("title");
//                    int time = jsonObject.getInt("time");
//                    String path = jsonObject.getString("path");
//
//                    Uri uri = Uri.parse(path);
//                    try {
//                        getContentResolver().takePersistableUriPermission(
//                                uri,
//                                Intent.FLAG_GRANT_READ_URI_PERMISSION
//                        );
//                    } catch (SecurityException e) {
//                        Log.e("URI Permission", "No permission for: " + uri);
//                        continue;
//                    }
//
//                    songList.add(new Song(title, time, path));
//                }
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//
//            handler.postDelayed(() -> {
//                if (lastPlayingIndex >= 0 && lastPlayingIndex < songList.size()) {
//                    prepareMediaPlayer(lastPlayingIndex); // 准备播放器但不立即播放
//                }
//            }, 1000);
//        }
//    }

    private void loadPlaylist() {
        try {
            File file = new File(getExternalFilesDir(null), "playlist.json");
            if (!file.exists()) {
                Log.d("Playlist", "⚠ 播放列表 JSON 文件不存在，跳过加载");
                return;
            }


            StringBuilder jsonString = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonString.append(line);
                }
            }

            JSONObject playlistObject = new JSONObject(jsonString.toString());
            JSONArray jsonArray = playlistObject.getJSONArray("playlist");
            songList.clear();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String path = jsonObject.getString("path");

                // 重新获取URI权限
                Uri uri = Uri.parse(path);
                try {
                    getContentResolver().takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                } catch (SecurityException e) {
                    Log.e("URI Permission", "No permission for: " + uri);
                    continue;
                }

                songList.add(new Song(
                        jsonObject.getString("title"),
                        jsonObject.getInt("time"),
                        path
                ));
            }

            // 恢复播放状态
            currentPlayingIndex = playlistObject.optInt("lastPlayingIndex", -1);
            String mode = playlistObject.optString("playMode", "LOOP");
            currentPlayMode = PlayMode.valueOf(mode);
            updatePlayModeText();

            // 延迟恢复播放
            new Handler().postDelayed(() -> {
                if (currentPlayingIndex != -1 && !songList.isEmpty()) {
                    prepareMediaPlayer(currentPlayingIndex);
                }
            }, 1000);

        } catch (Exception e) {
            Log.e("Playlist", "加载播放列表失败", e);
            Toast.makeText(this, "播放列表加载失败", Toast.LENGTH_SHORT).show();
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
    //转地址编码

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
        savePlaylist();

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

//    private void stopPlayback() {
//        shouldAutoPlay = false;
//        if (mediaPlayer != null) {
//            if (mediaPlayer.isPlaying()) {
//                mediaPlayer.stop(); // 停止播放
//                try {
//                    mediaPlayer.prepare();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            mediaPlayer.seekTo(0);
//            isPlaying = false;
//            handler.removeCallbacks(updateProgressRunnable);
//            updateProgressText(0, mediaPlayer.getDuration());
//            TextView tvStatus = findViewById(R.id.tv_status);
//            tvStatus.setText("已停止");
//            seekBar.setProgress(0);
//        }
//        TextView tvStatus = findViewById(R.id.tv_status);
//
//        isPlaying = false;
//        if (playerThread != null && playerThread.isAlive()) {
//            playerThread.interrupt();
//        }
//
//        handler.post(() -> {
//            progress.setText("播放进度: 00:00 / " + timeFormat.format(mediaPlayer.getDuration()));
//            tvStatus.setText("已停止");
//            seekBar.setProgress(0); // 重置进度条
//        });
//
//        Log.d("DEBUG", "播放已停止，进度重置");
//    }

    private void stopPlayback() {
        synchronized (this) {
            shouldAutoPlay = false;
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }

            currentPlayingIndex = -1; // 重置播放索引
            isPlaying = false;
            handler.removeCallbacks(updateProgressRunnable);
            updateProgressText(0, 0);

            // 安全停止线程
            if (playerThread != null) {
                playerThread.interrupt();
                playerThread = null;
            }

            handler.removeCallbacks(updateProgressRunnable);
            updateProgressText(0, 0);
        }
    }


    private void updatePlayProgress() {
        while (!Thread.currentThread().isInterrupted()) {
            synchronized (this) {
                if (mediaPlayer == null || !isPlaying) break;

                try {
                    final int current = mediaPlayer.getCurrentPosition();
                    final int duration = mediaPlayer.getDuration();

                    handler.post(() -> {
                        if (mediaPlayer != null && isPlaying) {
                            progress.setText("播放进度: " + timeFormat.format(current) +
                                    " / " + timeFormat.format(duration));
                        }
                    });

                    Thread.sleep(500);
                } catch (InterruptedException | IllegalStateException e) {
                    Thread.currentThread().interrupt(); // 正确标记中断状态
                    Log.d("Player", "播放线程正常终止");
                }
            }
        }
    }

    private void updateProgressText(int current, int duration) {
        progress.setText("播放进度: " + timeFormat.format(current) + " / " + timeFormat.format(duration));
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 保存当前播放进度
        if (mediaPlayer != null && isPlaying) {
            SharedPreferences.Editor editor = getSharedPreferences("PlayerPrefs", MODE_PRIVATE).edit();
            editor.putInt("lastPosition", mediaPlayer.getCurrentPosition());
            editor.apply();
        }

        savePlaylist();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }
}

