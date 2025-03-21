package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.text.BreakIterator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

//// MainActivity.java
//public class MainActivity extends AppCompatActivity {
//
//    private MediaPlayer mediaPlayer;
//    private boolean isPlaying = false;
//    private TextView tvStatus;
//    private TextView tvCurrentFile;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        // 初始化UI组件
//        Button btnPlay = findViewById(R.id.btn_play);
//        Button btnPause = findViewById(R.id.btn_pause);
//        Button btnStop = findViewById(R.id.btn_stop);
//        tvStatus = findViewById(R.id.tv_status);
//        tvCurrentFile = findViewById(R.id.tv_current_file);
//
//        // 初始化MediaPlayer
//        mediaPlayer = new MediaPlayer();
//
//        // 播放按钮点击事件
//        btnPlay.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (!isPlaying) {
//                    try {
//                        AssetFileDescriptor fileDescriptor = getResources().openRawResourceFd(R.raw.test_audio);
//                        mediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(),
//                                fileDescriptor.getStartOffset(),
//                                fileDescriptor.getLength());
//                        fileDescriptor.close();
//
//                        mediaPlayer.prepare();
//                        mediaPlayer.start();
//
//                        isPlaying = true;
//                        tvStatus.setText("播放中 ▶");
//                        tvCurrentFile.setText("当前播放：测试音频.mp3");
//
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                        tvStatus.setText("播放出错！");
//                    }
//                }
//            }
//        });
//
//        // 暂停按钮点击事件
//        btnPause.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (isPlaying) {
//                    mediaPlayer.pause();
//                    isPlaying = false;
//                    tvStatus.setText("已暂停 ⏸");
//                }
//            }
//        });
//
//        // 停止按钮点击事件
//        btnStop.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (isPlaying) {
//                    mediaPlayer.stop();
//                    mediaPlayer.reset();
//                    isPlaying = false;
//                    tvStatus.setText("已停止 ⏹");
//                    tvCurrentFile.setText("未选择文件");
//                }
//            }
//        });
//    }
//    public void Click(View v){
//        System.out.println("dianwo Click");
//    }
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (mediaPlayer != null) {
//            mediaPlayer.release();
//            mediaPlayer = null;
//        }
//    }
//}
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private List<Song> songList;
    private SeekBar seekBar;
    private TextView progress = null;
    private Thread playerThread;
    private boolean isSeeking = false;
    private Handler handler = new Handler();
    private volatile boolean isPlaying = false;
    private Map<Integer,String> songMap = new HashMap<>();
    private PlaylistManager playlistManager;
    private SongAdapter songAdapter;
    private int currentPlayingIndex = -1;
    private static final int PICK_AUDIO_REQUEST = 1;
    private Button prevButton, nextButton;




    private static final SimpleDateFormat timeFormat =
            new SimpleDateFormat("mm:ss", Locale.getDefault());

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
//按钮，变量初始化

//        recyclerView = findViewById(R.id.recycler_view);
//        adapter = new SongAdapter(songList);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        recyclerView.setAdapter(adapter);


        songList = new ArrayList<>();
        seekBar = findViewById(R.id.seekBar);
        progress = findViewById(R.id.tv_current_file);
        TextView tvStatus = findViewById(R.id.tv_status);
        prevButton = findViewById(R.id.btn_prev);
        nextButton = findViewById(R.id.btn_next);
        mediaPlayer = MediaPlayer.create(this, R.raw.test_audio);
        seekBar.setMax(mediaPlayer.getDuration());
        TextView currentSong = findViewById(R.id.tv_current_song_name);
        playlistManager = new PlaylistManager();
        songList = new ArrayList<>();
        songAdapter = new SongAdapter(songList,position -> playSong(position));
        RecyclerView songRecyclerView = findViewById(R.id.song_recycler_view);
        songRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        songRecyclerView.setAdapter(songAdapter);
//        songList = new ArrayList<>();
//
//        注册文件选择器
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),result -> {
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

            if (isPlaying && playerThread != null) {
                mediaPlayer.pause();
                isPlaying = false;
                tvStatus.setText("已暂停");
                playerThread.interrupt();
                handler.removeCallbacks(updateProgressRunnable);

            }
        });
//终止按钮
        findViewById(R.id.btn_stop).setOnClickListener(v -> stopPlayback());
        //添加歌曲
        findViewById(R.id.btn_add_song).setOnClickListener((v -> addSong()));
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

    }

//    private MediaPlayer mediaPlayer = new MediaPlayer();

    private void playSong(int position) {
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

            songAdapter.setPlayingPosition(position); // 更新 UI
            tv_status.setText("正在播放：" + song.getName());

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "播放失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }



    private void playPrevious() {
        if (currentPlayingIndex > 0) {
            playSong(currentPlayingIndex - 1);
        }
    }

    private void playNext() {
        if (currentPlayingIndex < songList.size() - 1) {
            playSong(currentPlayingIndex + 1);
        }
    }
//    private static final int REQUEST_CODE_PICK_SONG = 101;
    //添加歌曲
    private void addSong() {
//        Song newSong = new Song("dacapo",0,"/sdcard/remusic");
//        playlistManager.addSong(newSong);
//        songList.add(newSong);
//        songAdapter.updateData(songList);
//        Toast.makeText(MainActivity.this,"歌曲已添加",Toast.LENGTH_SHORT);
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
            }
        }
    }

    private int getTimeDuration(Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, uri);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return durationStr != null ? Integer.parseInt(durationStr) / 1000 : 0;
        }catch (Exception e){
            e.printStackTrace();
            return 0;
        }finally {
            try {
                retriever.release();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 转换为秒
    }
//    public String getRealPathFromURI(Uri uri) {
//        String filePath = null;
//        if (uri.getScheme().equals("content")) {
//            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
//            if (cursor != null) {
//                int index = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
//                if (index != -1) {
//                    cursor.moveToFirst();
//                    filePath = cursor.getString(index);
//                }
//                cursor.close();
//            }
//        } else if (uri.getScheme().equals("file")) {
//            filePath = uri.getPath();
//        }
//        return filePath;
//    }
public String getRealPathFromURI(Uri uri) {
    String filePath = null;
    if (uri.getScheme().equals("content")) {
        Cursor cursor = getContentResolver().query(uri, new String[]{MediaStore.Audio.Media.DATA}, null, null, null);
        if (cursor != null) {
            int columnIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            if (columnIndex != -1 && cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex);
            }
            cursor.close();
        }
    }
    return filePath == null ? uri.toString() : filePath; // 确保 filePath 不为空
}

    //处理音频
    private void handleSelectedAudio(Uri audioUri) {
        //getname
        String filename = getFileName(audioUri);
        Toast.makeText(this,"选择： " + filename, Toast.LENGTH_SHORT);

        Song newSong = new Song(filename, 0,"/storage/emulated/0/Music/song.mp3"); // 这里 0 代表时间长度，可扩展
        songList.add(newSong);
        adapter.notifyItemInserted(songList.size() - 1);
    }
    private String getFileName(Uri uri) {
        String displayName = "";
        try (Cursor cursor = getContentResolver().query(
                uri,null,null,null,null
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
    private void startSeekBarUpdate() {
        handler.removeCallbacks(updateSeekBar);
        handler.post(updateSeekBar);
    }

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }
}
//    private Song song = new Song("five hundred miles.mp3",101);
//
//    private class SongPlayer implements Runnable{
//        @Override
//            public void run() {
//                String songName = song.getName();
//                for(int i = 0;i < song.getTimeDuration();i++){
//                    System.out.println(i+"************");
//                    progress.setText("歌名："+ songName +"   播放进度："+i+"%");
//                    try {
//                        Thread.sleep(100);
//                    }catch (Exception ex){
//                        ex.printStackTrace();
//
//                    }
//                }
//        }
//
//    }

//    @SuppressLint("SetTextI18n")
//    private void updateProgressText(int milliseconds) {
//        // 确保 songMap 包含该 Key
//        if (!songMap.containsKey(R.raw.test_audio)) {
////            Log.e("DEBUG", "songMap 中未找到 R.raw.dacapo 对应的歌曲名！");
//            return;
//        }
////        handler.post(() -> {
////            if (progress != null) {
////                progress.setText(" 播放进度: " + timeFormat.format(milliseconds));
////            } else {
////                Log.e("DEBUG", "TextView 为空，无法更新进度！");
////            }
//        }

//    public void onClick(View v)
//
//    public void Click(View v) {
////        System.out.println("dianwo dian wo");
//        Random rd = new Random();
//        int i = rd.nextInt(100);
//        String s = i + "";
//        TextView tv = findViewById(R.id.btn_play);
//        tv.setText(s);
//    }
//
//}

