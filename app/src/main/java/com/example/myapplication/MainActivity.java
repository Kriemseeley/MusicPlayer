package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import java.net.URLConnection;

import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.appbar.MaterialToolbar;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.jaudiotagger.tag.reference.PictureTypes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
//import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import android.content.DialogInterface; // For AlertDialog
import android.view.View; // For View visibility
import android.widget.AdapterView; // For Spinner
import android.widget.ArrayAdapter;
import android.content.DialogInterface; // For AlertDialog
import android.view.View; // For View visibility
import android.widget.AdapterView; // For Spinner
import android.widget.ArrayAdapter; // For Spinner Adapter
import android.widget.EditText; // For Dialog input
import android.widget.ImageButton; // If using ImageButton for add/delete playlist
import android.widget.Spinner; // For Playlist selection
import androidx.appcompat.app.AlertDialog; // For Dialogs
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import android.util.DisplayMetrics;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import android.widget.Toolbar;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.datatype.Artwork;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderEffectBlur;
import eightbitlab.com.blurview.RenderScriptBlur;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_AUDIO_REQUEST = 1;
    private static final int PICK_BACKGROUND_REQUEST = 2;
    private static final int RECORD_AUDIO_PERMISSION_REQUEST = 3;
    // public static final int VIEW_TYPE_EMPTY = 0;

    private enum PlayMode {
        LOOP, SHUFFLE, SINGLE
    }

    private AudioVisualizerView visualizerView;
    private AudioVisualizerManager visualizerManager;
    private CoverFlipAnimator flipAnimator;
    private ViewGroup coverContainer;

    private ImageView coverArtImageView; // Add reference for the ImageView
    private ExecutorService backgroundExecutor;
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("mm:ss", Locale.getDefault());
    private PlayMode currentPlayMode = PlayMode.LOOP;
    private List<Integer> shuffleOrder = new ArrayList<>();
    private int shuffleIndex = 0;
    private int stoppedAtIndex = -1;
    private static final String PLAYLISTS_FILENAME = "playlists_data.json"; // New filename for all playlists
    private static final String PREFS_NAME = "PlayerPrefs";
    private static final String KEY_LAST_ACTIVE_PLAYLIST_INDEX = "lastActivePlaylistIndex";

    // --- 新增背景管理相关变量 ---
    private BackgroundManager backgroundManager;
    private ConstraintLayout mainContentLayout;
    private ConstraintLayout playbackControlsLayout;
    private ActivityResultLauncher<Intent> backgroundPickerLauncher;

    // --- Replace songList with playlist management ---
    // private List<Song> songList; // REMOVE THIS or comment out
    private List<Playlist> allPlaylists; // Holds all user playlists
    private int currentPlaylistIndex = 0; // Index of the currently active playlist in allPlaylists

    // --- UI Elements for Playlist Management ---
    private Spinner playlistSpinner;
    private BlurBackgroundView blurBackgroundView;
    private ArrayAdapter<String> playlistSpinnerAdapter;
    private MaterialButton btnAddPlaylist; // Add Button ID in XML
    private MaterialButton btnDeletePlaylist; // Add Button ID in XML
    private MaterialButton prevButton; // Make sure you have these as member variables
    private MaterialButton nextButton;
    private MediaPlayer mediaPlayer;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    // private RecyclerView recyclerView;
    private SongAdapter adapter;
    // private List<Song> songList;
    private SeekBar seekBar;
    private TextView progress = null;
    // private Thread playerThread;
    private boolean shouldAutoPlay = false;
    private volatile boolean isPreparing = false;
    private boolean isSeeking = false;
    private Handler handler = new Handler();
    private volatile boolean isPlaying = false;
    // private Map<Integer, String> songMap = new HashMap<>();
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

    // BlurView 相关变量
    // private BlurView blurViewTop;
    private BlurView blurViewPlayMode;
    private BlurView blurViewProgress;
    private BlurView blurViewControls;
    private BlurView blurViewPlaylist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 移除过渡动画相关代码
        setContentView(R.layout.activity_main);
        blurBackgroundView = findViewById(R.id.blur_background_view);
        // --- 初始化背景管理器 ---

        // --- 注册背景图片选择器 ---
        backgroundPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            // 保存并应用背景
                            if (backgroundManager.saveBackgroundFromUri(imageUri)) {
                                // applyBlurEffectToLayouts();
                                Toast.makeText(this, "背景已更新", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "背景更新失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

        // songList = new ArrayList<>();

        allPlaylists = new ArrayList<>();
        backgroundExecutor = Executors.newSingleThreadExecutor();
        // --- Find Playlist UI Elements ---
        playlistSpinner = findViewById(R.id.spinner_playlists); // Add this ID to activity_main.xml
        btnAddPlaylist = findViewById(R.id.btn_add_playlist); // Add this ID to activity_main.xml
        btnDeletePlaylist = findViewById(R.id.btn_delete_playlist); // Add this ID to activity_main.xml
        updatePlayModeText();
        coverArtImageView = findViewById(R.id.iv_cover_art);
        seekBar = findViewById(R.id.seekBar);
        progress = findViewById(R.id.tv_current_file);
        TextView tvStatus = findViewById(R.id.tv_status);
        mediaPlayer = MediaPlayer.create(this, R.raw.test_audio);
        seekBar.setMax(mediaPlayer.getDuration());
        progress = findViewById(R.id.tv_current_file);
        TextView currentSong = findViewById(R.id.tv_current_song_name);
        RecyclerView songRecyclerView = findViewById(R.id.song_recycler_view);
        prevButton = findViewById(R.id.btn_prev); // Assign member variable
        nextButton = findViewById(R.id.btn_next); // Assign member variable

        // --- 获取需要应用毛玻璃效果的布局 ---
        mainContentLayout = findViewById(R.id.main_content_layout);
        playbackControlsLayout = findViewById(R.id.playback_controls);

        // --- 应用毛玻璃效果 ---
        // applyBlurEffectToLayouts();

        // --- Add a check in case the RecyclerView ID is wrong or missing ---
        if (songRecyclerView == null) {
            Log.e("MainActivity", "FATAL: RecyclerView R.id.song_recycler_view not found in layout!");
            Toast.makeText(this, "布局错误: 找不到歌曲列表视图", Toast.LENGTH_LONG).show();
            // Optional: finish the activity if this is critical
            finish();
            return; // Exit onCreate early to prevent further NullPointerExceptions
        }
        songRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        songRecyclerView.setAdapter(songAdapter);
        playlistSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        playlistSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        playlistSpinner.setAdapter(playlistSpinnerAdapter);

        playlistSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Switch to the selected playlist if it's different from the current one
                if (position != currentPlaylistIndex) {
                    switchPlaylist(position);
                }
                // Enable/disable delete button based on selection (e.g., don't delete last
                // playlist)
                btnDeletePlaylist.setEnabled(allPlaylists.size() > 1);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // --- Setup Playlist Buttons ---
        btnAddPlaylist.setOnClickListener(v -> {
            animateButton(v);
            showCreatePlaylistDialog();
        });
        // Button btnLogout = findViewById(R.id.btnLogout);
        // btnLogout = findViewById(R.id.btnLogout);
        // btnLogout.setOnClickListener(v -> {
        // SharedPreferences.Editor editor = getSharedPreferences("user_info",
        // MODE_PRIVATE).edit();
        // editor.putBoolean("isLogin", false);
        // editor.apply();
        //
        // Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        // intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
        // Intent.FLAG_ACTIVITY_NEW_TASK);
        // startActivity(intent);
        // finish();
        // });
        MaterialToolbar toolbar = findViewById(R.id.toolbar); // ID 改成你 XML 里的 toolbar ID
        setSupportActionBar(toolbar);
        btnDeletePlaylist.setOnClickListener(v -> showDeletePlaylistConfirmation());
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.reset();
        } catch (Exception e) {
            Log.e("MediaPlayer", "初始化失败", e);
        }
        songAdapter = new SongAdapter(new ArrayList<>(), position -> {
            if (getCurrentPlaylist() != null && !getCurrentPlaylist().getSongs().isEmpty()) {
                playSong(position); // Play song from the *current* playlist
            } else if (position == -1 && songAdapter.getItemViewType(0) == SongAdapter.VIEW_TYPE_EMPTY) {
                // Handle click on "Empty" placeholder - maybe trigger addSong?
                addSong();
            }
        });
        songRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        songRecyclerView.setAdapter(songAdapter);
        loadAllPlaylists(); // New loading method
        // 注册文件选择器
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri audioUri = result.getData().getData();

                        getContentResolver().takePersistableUriPermission(
                                audioUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        handleSelectedAudio(audioUri);
                    }
                });

        // 建立hash映射检索歌名
        // songMap.put(R.raw.test_audio, "顶上的风景");
        // Log.d("DEBUG", "songMap 初始化完毕: " + songMap.toString());

        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, // 允许上下拖动
                0 // 禁用滑动删除
        ) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder,
                    @NonNull RecyclerView.ViewHolder target) {
                // 获取拖动的位置
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                Playlist currentPlaylist = getCurrentPlaylist();
                if (currentPlaylist == null || currentPlaylist.getSongs().isEmpty())
                    return false;

                List<Song> currentSongs = currentPlaylist.getSongs();

                // Check bounds
                if (fromPosition < 0 || fromPosition >= currentSongs.size() ||
                        toPosition < 0 || toPosition >= currentSongs.size()) {
                    return false;
                }
                // 更新播放索引
                if (currentPlayingIndex == fromPosition) {
                    currentPlayingIndex = toPosition;
                } else if (fromPosition < currentPlayingIndex && toPosition >= currentPlayingIndex) {
                    currentPlayingIndex--;
                } else if (fromPosition > currentPlayingIndex && toPosition <= currentPlayingIndex) {
                    currentPlayingIndex++;
                }

                // 移动歌曲并刷新列表
                // Move song within the *current* playlist's list
                Collections.swap(currentSongs, fromPosition, toPosition);
                songAdapter.notifyItemMoved(fromPosition, toPosition); // Notify adapter

                // Save all playlists because the order changed in one of them
                saveAllPlaylists();
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // 不需要处理滑动
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(songRecyclerView);

        // 设置封面翻转动画
        setupCoverFlipAnimation();

        // 检查录音权限（在可视化器初始化之后）
        checkRecordAudioPermission();

        // 添加在线歌曲 新添加
        // findViewById(R.id.btn_add_online_music).setOnClickListener(v -> {
        // animateButton(v);
        // Intent intent = new Intent(MainActivity.this, AddOnlineMusicActivity.class);
        // startActivityForResult(intent, 2001); // 2001为自定义请求码
        // });

        // 启动按钮
        findViewById(R.id.btn_play).setOnClickListener(v -> {
            animateButton(v);
            Playlist currentPlaylist = getCurrentPlaylist();

            if (currentPlaylist == null || currentPlaylist.getSongs().isEmpty()) {
                Toast.makeText(this, "当前歌单为空，请添加歌曲", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isPreparing) {
                Log.d("PlayButton", "Ignoring Play click: MediaPlayer is preparing.");
                Toast.makeText(this, "正在准备...", Toast.LENGTH_SHORT).show();
                return;
            }
            shouldAutoPlay = true;
            if (mediaPlayer != null && !isPlaying && mediaPlayer.getCurrentPosition() > 0
                    && currentPlayingIndex != -1) {
                Log.d("PlayButton", "Resuming paused playback at index: " + currentPlayingIndex);
                try {
                    mediaPlayer.start();
                    isPlaying = true;
                    isPreparing = false; // Not preparing anymore
                    tvStatus.setText("正在播放");
                    handler.post(updateProgressRunnable);

                    // 可视化器状态已通过生命周期自动管理，无需手动更新

                    // Ensure highlight is correct
                    if (songAdapter != null)
                        songAdapter.setPlayingPosition(currentPlayingIndex);
                } catch (IllegalStateException e) {
                    Log.e("PlayButton", "Error resuming playback", e);
                    // Might need to reset and play from beginning or stop
                    stopPlayback();
                    Toast.makeText(this, "恢复播放失败", Toast.LENGTH_SHORT).show();
                }
            }
            // Case 2: Play the song that was explicitly stopped
            else if (stoppedAtIndex != -1) {
                Log.d("PlayButton", "Playing previously stopped song at index: " + stoppedAtIndex);
                int indexToPlay = stoppedAtIndex;
                stoppedAtIndex = -1; // Consume the stopped state
                playSong(indexToPlay);
            }
            // Case 3: Nothing playing, nothing stopped - play the first song
            else if (currentPlayingIndex == -1) {
                Log.d("PlayButton", "Nothing playing or stopped, starting from index 0.");
                stoppedAtIndex = -1; // Ensure stopped state is clear
                playSong(0);
            }
            // Case 4: (Less likely but safety check) Already playing or unexpected state
            else if (isPlaying) {
                Log.d("PlayButton", "Already playing, doing nothing.");
                // Optional: maybe pause here? Current logic assumes Play=Start/Resume
            } else {
                // Should ideally not reach here if states are managed well, maybe play current?
                Log.w("PlayButton", "Unexpected state. CurrentIndex: " + currentPlayingIndex + ", stoppedAtIndex: "
                        + stoppedAtIndex + ". Attempting to play current index.");
                stoppedAtIndex = -1;
            }
        });

        // 暂停按钮
        findViewById(R.id.btn_pause).setOnClickListener(v -> {
            animateButton(v);
            // ... (Keep existing pause logic, it should work fine) ...
            shouldAutoPlay = false;
            synchronized (this) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    isPlaying = false;
                    tvStatus.setText("已暂停");
                    handler.removeCallbacks(updateProgressRunnable);

                    // 可视化器状态已通过生命周期自动管理，无需手动更新

                    // No need to handle playerThread here if using the handler approach
                }
            }
        });
        // 终止按钮
        findViewById(R.id.btn_stop).setOnClickListener(v -> {
            animateButton(v);
            stopPlayback();
        });
        // 添加歌曲
        findViewById(R.id.btn_add_song).setOnClickListener((v -> {
            animateButton(v);
            addSong();
        }));
        // 删除歌曲
        findViewById(R.id.btn_delete_song).setOnClickListener(v -> {
            animateButton(v);
            removeSelectedSong();
        });
        // 上一首
        prevButton.setOnClickListener(v -> {
            animateButton(v);
            playPrevious();
        });
        nextButton.setOnClickListener(v -> {
            animateButton(v);
            playNext();
        });

        // 测试封面翻转按钮
        findViewById(R.id.btn_test_flip).setOnClickListener(v -> {
            animateButton(v);
            if (flipAnimator != null) {
                if (flipAnimator.isShowingFront()) {
                    Log.d("TestFlip", "Flipping to back (visualizer)");
                    flipAnimator.flipToBack();
                } else {
                    Log.d("TestFlip", "Flipping to front (cover)");
                    flipAnimator.flipToFront();
                }
            } else {
                Log.w("TestFlip", "flipAnimator is null");
            }
        });
        // 进度条
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                    // updatePlayProgress();
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
        // menu button
        findViewById(R.id.btn_switch_mode).setOnClickListener(v -> {
            animateButton(v);
            switchPlayMode();
        });
        setDefaultCoverArt();

        // 初始化BlurKit
        // BlurKit.init(this);

        // 初始化所有 BlurView
        // blurViewTop = findViewById(R.id.blur_view_top);
        blurViewPlayMode = findViewById(R.id.blur_view_play_mode);
        blurViewProgress = findViewById(R.id.blur_view_progress);
        blurViewControls = findViewById(R.id.blur_view_controls);
        blurViewPlaylist = findViewById(R.id.blur_view_playlist);

        setupAllBlurViews();

        backgroundManager = new BackgroundManager(this);

        // 检查录音权限（音频可视化需要）
        checkRecordAudioPermission();
    }

    private void checkRecordAudioPermission() {
        Log.d("Permission", "Checking RECORD_AUDIO permission...");

        // 检查权限状态
        int permissionStatus = checkSelfPermission(Manifest.permission.RECORD_AUDIO);
        Log.d("Permission",
                "Permission status: " + permissionStatus + " (GRANTED=" + PackageManager.PERMISSION_GRANTED + ")");

        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            Log.d("Permission", "RECORD_AUDIO permission not granted, requesting...");

            // 检查是否应该显示权限说明
            if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                Log.d("Permission", "Should show permission rationale");
                Toast.makeText(this, "需要录音权限来显示音频可视化效果", Toast.LENGTH_LONG).show();
            }

            // 显示权限请求对话框
            requestPermissions(new String[] { Manifest.permission.RECORD_AUDIO }, RECORD_AUDIO_PERMISSION_REQUEST);
        } else {
            Log.d("Permission", "RECORD_AUDIO permission already granted");
            Toast.makeText(this, "录音权限已授予，音频可视化功能可用", Toast.LENGTH_SHORT).show();

            // 权限已授予，如果当前正在播放音乐，立即启动可视化器
            if (mediaPlayer != null && mediaPlayer.isPlaying() && visualizerManager != null) {
                Log.d("Permission", "Initializing visualizer with existing permission");
                if (visualizerManager.initialize(mediaPlayer)) {
                    visualizerManager.start();
                    if (visualizerView != null) {
                        visualizerView.startVisualization();
                    }
                    Log.d("Permission", "Visualizer started successfully with existing permission");
                }
            }
        }
    }

    private void setupCoverFlipAnimation() {
        // 1. 获取容器
        coverContainer = findViewById(R.id.cover_container);

        // 2. 获取封面卡片View的引用
        View coverCard = findViewById(R.id.cover_card);

        // 3. 创建波形可视化视图
        visualizerView = new AudioVisualizerView(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        visualizerView.setLayoutParams(params);
        coverContainer.addView(visualizerView);
        visualizerView.setVisibility(View.GONE);

        // 4. 创建翻转动画管理器，使用 coverCard 作为正面
        flipAnimator = new CoverFlipAnimator(coverContainer, coverCard, visualizerView);

        // 5. 创建可视化管理器
        visualizerManager = new AudioVisualizerManager();

        // 6. 设置音频数据回调
        visualizerManager.setAudioDataCallback(new AudioVisualizerManager.AudioDataCallback() {
            @Override
            public void onAudioDataUpdate(@NonNull byte[] waveform, @Nullable byte[] fft, int samplingRate) {
                Log.d("MainActivity", "Audio data received - waveform length: " + waveform.length +
                        ", samplingRate: " + samplingRate + ", visualizerView: "
                        + (visualizerView != null ? "available" : "null"));

                if (visualizerView != null) {
                    visualizerView.updateAudioData(waveform);
                    Log.d("MainActivity", "Audio data passed to visualizerView");
                } else {
                    Log.w("MainActivity", "VisualizerView is null, cannot update audio data");
                }
            }
        });

        // 7. 设置状态变化回调
        visualizerManager.setStateChangeCallback(new AudioVisualizerManager.StateChangeCallback() {
            @Override
            public void onStateChanged(boolean isEnabled, @Nullable String error) {
                if (error != null) {
                    Log.e("Visualizer", "Visualizer error: " + error);
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onPerformanceUpdate(int droppedFrames, float avgLatency) {
                if (droppedFrames > 10) {
                    Log.w("Visualizer", "Performance warning: " + droppedFrames + " dropped frames");
                }
            }
        });

        // 8. 添加生命周期观察
        getLifecycle().addObserver(visualizerManager);

        // 9. 设置封面点击事件
        if (coverCard != null) {
            coverCard.setOnClickListener(v -> {
                if (flipAnimator != null) {
                    flipAnimator.flip();
                }
            });
        }
    }

    private void setupAllBlurViews() {
        float radius = 20f;
        View decorView = getWindow().getDecorView();
        ViewGroup rootView = decorView.findViewById(android.R.id.content);
        Drawable windowBackground = decorView.getBackground();

        // 设置所有 BlurView
        // setupBlurView(rootView, windowBackground, radius);
        setupBlurView(blurViewPlayMode, rootView, windowBackground, radius);
        setupBlurView(blurViewProgress, rootView, windowBackground, radius);
        setupBlurView(blurViewControls, rootView, windowBackground, radius);
        setupBlurView(blurViewPlaylist, rootView, windowBackground, radius);
    }

    private void setupBlurView(BlurView blurView, ViewGroup rootView, Drawable windowBackground, float radius) {
        blurView.setupWith(rootView, new RenderScriptBlur(this))
                .setFrameClearDrawable(windowBackground)
                .setBlurRadius(radius)
                .setBlurAutoUpdate(true);
    }

    /**
     * 应用毛玻璃效果到指定布局
     */
    // private void applyBlurEffectToLayouts() {
    // if (backgroundManager != null) {
    // // 优化模糊参数
    // float blurRadius = 20.0f; // 降低模糊半径以提高性能
    // int overlayColor = Color.argb(40, 255, 255, 255); // 降低叠加层透明度
    //
    // // 应用毛玻璃效果到各个布局
    // backgroundManager.applyBlurredBackground(mainContentLayout, blurRadius,
    // overlayColor);
    // backgroundManager.applyBlurredBackground(playbackControlsLayout, blurRadius,
    // overlayColor);
    //
    // // 设置背景模糊视图
    // View blurBackground = findViewById(R.id.blur_background);
    // if (blurBackground != null) {
    // blurBackground.setBackground(BlurKit.getInstance().blur(blurBackground, (int)
    // blurRadius));
    // blurBackground.getBackground().setAlpha(255 - Color.alpha(overlayColor));
    // }
    // }
    // }

    /**
     * 打开背景选择器
     */
    private void openBackgroundPicker() {
        // 检查并请求权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13及以上使用新的媒体权限
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] { Manifest.permission.READ_MEDIA_IMAGES }, PICK_BACKGROUND_REQUEST);
                return;
            }
        } else {
            // Android 13以下使用存储权限
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, PICK_BACKGROUND_REQUEST);
                return;
            }
        }

        // 创建文件选择器意图
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                "image/jpeg",
                "image/png",
                "image/webp"
        });
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        backgroundPickerLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PICK_BACKGROUND_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，重新打开选择器
                openBackgroundPicker();
            } else {
                Toast.makeText(this, "需要存储权限才能选择背景图片", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permission", "RECORD_AUDIO permission granted");
                Toast.makeText(this, "音频可视化功能已启用", Toast.LENGTH_SHORT).show();

                // 如果当前正在播放音乐，立即初始化可视化器
                if (mediaPlayer != null && mediaPlayer.isPlaying() && visualizerManager != null) {
                    Log.d("Permission", "Initializing visualizer after permission granted");
                    if (visualizerManager.initialize(mediaPlayer)) {
                        visualizerManager.start();
                        if (visualizerView != null) {
                            visualizerView.startVisualization();
                        }
                        Log.d("Permission", "Visualizer started successfully after permission granted");
                    } else {
                        Log.w("Permission", "Visualizer initialization failed after permission granted");
                    }
                }
            } else {
                Log.w("Permission", "RECORD_AUDIO permission denied");
                Toast.makeText(this, "需要录音权限才能显示音频可视化效果", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("MenuDebug", "菜单点击: " + item.getItemId());
        int id = item.getItemId();
        if (id == R.id.action_add_online_music) {
            // 跳转到添加在线音乐页面
            Intent addIntent = new Intent(this, AddOnlineMusicActivity.class);
            startActivity(addIntent);
            return true;
        } else if (id == R.id.btnLogout) {
            SharedPreferences.Editor editor = getSharedPreferences("user_info", MODE_PRIVATE).edit();
            editor.clear();
            editor.apply();
            // 跳转到登录页并清空返回栈
            Intent logoutIntent = new Intent(this, LoginActivity.class);
            logoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(logoutIntent);
            finishAffinity(); // 彻底清理Activity栈
            return true;
        } else if (id == R.id.action_change_background) {
            // 打开背景选择器
            openBackgroundPicker();
            return true;
        } else if (id == R.id.action_reset_background) {
            // 重置为默认背景
            backgroundManager.clearCustomBackground();
            // applyBlurEffectToLayouts();
            Toast.makeText(this, "已恢复默认背景", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // private void notifyPlayOnlineSong(String songid) {
    // OkHttpClient client = new OkHttpClient();
    // HttpUrl url = HttpUrl.parse("http://192.168.183.1:8888/houduan/play")
    // .newBuilder()
    // .addQueryParameter("songid", songid)
    // .build();
    // Request request = new Request.Builder().url(url).get().build();
    // client.newCall(request).enqueue(new Callback() {
    // @Override
    // public void onFailure(Call call, IOException e) {
    // }
    //
    // @Override
    // public void onResponse(Call call, Response response) throws IOException {
    // }
    // });
    // }

    private Playlist getCurrentPlaylist() {
        if (allPlaylists != null && currentPlaylistIndex >= 0 && currentPlaylistIndex < allPlaylists.size()) {
            return allPlaylists.get(currentPlaylistIndex);
        }
        return null; // Or return a default empty playlist if preferred
    }

    private void copyUriToTempFile(Uri uri, File tempFile) throws IOException {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = getContentResolver().openInputStream(uri);
            outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e("CoverArt", "关闭输入流失败", e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e("CoverArt", "关闭输出流失败", e);
                }
            }
        }
    }

    private List<Song> getCurrentSongs() {
        Playlist current = getCurrentPlaylist();
        return (current != null) ? current.getSongs() : new ArrayList<>(); // Return empty list if no playlist
    }

    // --- Playlist Management Methods ---

    /**
     * Switches the active playlist, updates the UI, and saves the state.
     * 
     * @param newPlaylistIndex The index in allPlaylists to switch to.
     */
    // --- Location: MainActivity.java ---
    private void switchPlaylist(int newPlaylistIndex) {
        Log.d("PlaylistSwitch", "--- Entering switchPlaylist ---"); // Log entry
        Log.d("PlaylistSwitch", "Requested index: " + newPlaylistIndex + ", Current index: " + currentPlaylistIndex);
        Log.d("PlaylistSwitch", "Total playlists: " + (allPlaylists != null ? allPlaylists.size() : "null"));

        // --- Check 1: Basic validation ---
        if (allPlaylists == null || newPlaylistIndex < 0 || newPlaylistIndex >= allPlaylists.size()) {
            Log.e("PlaylistSwitch", "Validation failed: Invalid index or allPlaylists is null.");
            // Ensure spinner selection reflects reality if index was invalid
            if (playlistSpinner != null && currentPlaylistIndex >= 0
                    && currentPlaylistIndex < playlistSpinner.getCount()
                    && playlistSpinner.getSelectedItemPosition() != currentPlaylistIndex) {
                Log.w("PlaylistSwitch", "Resetting spinner selection to current index: " + currentPlaylistIndex);
                playlistSpinner.setSelection(currentPlaylistIndex);
            }
            return; // Exit if invalid
        }

        // --- Check 2: Already selected? ---
        if (newPlaylistIndex == currentPlaylistIndex) {
            Log.d("PlaylistSwitch", "Validation passed but index is same as current. No switch needed.");
            // Optional: Refresh view anyway? Or just exit?
            // refreshCurrentPlaylistView(); // Uncomment if you want refresh even on same
            // selection
            return;
        }

        Log.d("PlaylistSwitch", "Proceeding with switch logic...");

        stopPlayback(); // Stop playback before switching

        // Update the current index
        currentPlaylistIndex = newPlaylistIndex;
        Log.d("PlaylistSwitch", "Updated currentPlaylistIndex to: " + currentPlaylistIndex);

        // Update the RecyclerView using the helper method
        Log.d("PlaylistSwitch", "Refreshing adapter view for newly selected playlist.");
        refreshCurrentPlaylistView(); // Use the helper method

        // Reset playback state for the new playlist
        stoppedAtIndex = -1; // Reset stopped state
        // currentPlayingIndex was reset by stopPlayback()
        // Optionally load last played index for this playlist if implemented
        // Playlist newPlaylist = getCurrentPlaylist();
        // if (newPlaylist != null) { currentPlayingIndex =
        // newPlaylist.getLastPlayingIndex(); }

        // Clear current song display (or update if restoring state)
        TextView currentSongTextView = findViewById(R.id.tv_current_song_name);
        currentSongTextView.setText("未选择歌曲"); // Reset text
        TextView tvStatus = findViewById(R.id.tv_status);
        tvStatus.setText("播放列表已切换");
        updateProgressText(0, 0);
        seekBar.setProgress(0);
        seekBar.setMax(100);

        // Update the spinner selection visually (ensure it doesn't trigger listener
        // again infinitely)
        // The listener check should prevent infinite loop, but be cautious
        if (playlistSpinner != null && playlistSpinner.getSelectedItemPosition() != currentPlaylistIndex) {
            Log.d("PlaylistSwitch", "Setting spinner selection visually to: " + currentPlaylistIndex);
            // Set selection without triggering listener if possible, though standard
            // setSelection usually triggers it once.
            // The check inside the listener (position != currentPlaylistIndex) is the
            // primary guard.
            playlistSpinner.setSelection(currentPlaylistIndex);
        }

        // Enable/disable delete button
        if (btnDeletePlaylist != null) {
            btnDeletePlaylist.setEnabled(allPlaylists.size() > 1);
            Log.d("PlaylistSwitch", "Delete button enabled: " + (allPlaylists.size() > 1));
        }

        // Save the index of the newly active playlist
        saveLastActivePlaylistIndex();
        Log.d("PlaylistSwitch", "Saved new active playlist index.");

        Log.d("PlaylistSwitch", "--- Exiting switchPlaylist ---"); // Log exit
    }

    /**
     * Shows a dialog to enter the name for a new playlist.
     */
    private void showCreatePlaylistDialog() {
        // animateButton(v);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("创建新歌单");

        // Set up the input
        final EditText input = new EditText(this);
        input.setHint("歌单名称");
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("创建", (dialog, which) -> {
            String playlistName = input.getText().toString().trim();
            if (!playlistName.isEmpty()) {
                // Check for duplicate names
                boolean exists = false;
                for (Playlist p : allPlaylists) {
                    if (p.getName().equalsIgnoreCase(playlistName)) {
                        exists = true;
                        break;
                    }
                }
                if (exists) {
                    Toast.makeText(this, "歌单名称已存在", Toast.LENGTH_SHORT).show();
                } else {
                    createPlaylist(playlistName);
                }
            } else {
                Toast.makeText(this, "歌单名称不能为空", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    /**
     * Creates a new playlist, adds it to the list, updates UI, and saves.
     * 
     * @param name The name of the new playlist.
     */
    private void createPlaylist(String name) {
        Playlist newPlaylist = new Playlist(name);
        allPlaylists.add(newPlaylist);

        // Update Spinner
        playlistSpinnerAdapter.add(name);
        playlistSpinnerAdapter.notifyDataSetChanged();

        // Optionally switch to the new playlist immediately
        playlistSpinner.setSelection(allPlaylists.size() - 1); // Select the newly added one
        // switchPlaylist(allPlaylists.size() - 1); // This will be triggered by
        // setSelection listener

        // Save all playlists
        saveAllPlaylists();
        Toast.makeText(this, "歌单 '" + name + "' 已创建", Toast.LENGTH_SHORT).show();

        // Update delete button state after adding
        btnDeletePlaylist.setEnabled(allPlaylists.size() > 1);
    }

    /**
     * Shows a confirmation dialog before deleting the currently selected playlist.
     */
    // --- Location: MainActivity.java ---
    private void showDeletePlaylistConfirmation() {
        Log.d("DeletePlaylist", "--- Entering showDeletePlaylistConfirmation ---");
        Playlist playlistToDelete = getCurrentPlaylist();

        // --- Validation ---
        if (playlistToDelete == null) {
            Log.e("DeletePlaylist", "Cannot show confirmation: getCurrentPlaylist() returned null.");
            Toast.makeText(this, "错误：无法获取当前歌单", Toast.LENGTH_SHORT).show();
            return;
        }
        if (allPlaylists == null || allPlaylists.size() <= 1) {
            Log.w("DeletePlaylist", "Cannot show confirmation: Only one playlist exists or list is null.");
            Toast.makeText(this, "无法删除最后一个歌单", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("DeletePlaylist", "Showing confirmation for playlist: " + playlistToDelete.getName() + " at index "
                + currentPlaylistIndex);

        new AlertDialog.Builder(this)
                .setTitle("删除歌单")
                .setMessage("确定要删除歌单 '" + playlistToDelete.getName() + "' 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    Log.d("DeletePlaylist",
                            "Delete confirmed by user. Calling deletePlaylist with index: " + currentPlaylistIndex);
                    // Pass the index that was current when dialog was shown
                    deletePlaylist(currentPlaylistIndex);
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    Log.d("DeletePlaylist", "Delete cancelled by user.");
                })
                .show();
        Log.d("DeletePlaylist", "Confirmation dialog shown.");
    }

    private void deletePlaylist(int indexToDelete) {
        Log.d("DeletePlaylist", "--- Entering deletePlaylist ---");
        Log.d("DeletePlaylist", "Attempting to delete index: " + indexToDelete);
        Log.d("DeletePlaylist",
                "Total playlists BEFORE delete: " + (allPlaylists != null ? allPlaylists.size() : "null"));
        Log.d("DeletePlaylist", "Current active index BEFORE delete: " + currentPlaylistIndex);

        // --- Validation ---
        if (allPlaylists == null || indexToDelete < 0 || indexToDelete >= allPlaylists.size()
                || allPlaylists.size() <= 1) {
            Log.e("DeletePlaylist", "Validation failed: Invalid index or cannot delete last playlist.");
            // Optionally show toast again here
            return;
        }

        Playlist deletedPlaylist = allPlaylists.get(indexToDelete); // Get ref before removing
        Log.d("DeletePlaylist", "Playlist to delete: " + deletedPlaylist.getName());

        // Stop playback if the deleted playlist was active
        if (indexToDelete == currentPlaylistIndex) {
            Log.d("DeletePlaylist", "Deleted playlist was active. Stopping playback.");
            stopPlayback();
        }

        // --- Step 1: Remove from the data source ---
        allPlaylists.remove(indexToDelete);
        Log.d("DeletePlaylist", "Removed playlist from allPlaylists. New size: " + allPlaylists.size());

        // --- Step 2: Update the Spinner Adapter ---
        if (playlistSpinnerAdapter != null) {
            // Remove by object or name if possible, otherwise requires recreating the
            // adapter's list
            // Removing by name is safer if object instances might differ
            playlistSpinnerAdapter.remove(deletedPlaylist.getName());
            // Let the adapter know its underlying data count changed
            playlistSpinnerAdapter.notifyDataSetChanged();
            Log.d("DeletePlaylist", "Removed '" + deletedPlaylist.getName() + "' from spinner adapter. Adapter count: "
                    + playlistSpinnerAdapter.getCount());
        } else {
            Log.e("DeletePlaylist", "playlistSpinnerAdapter is null! Cannot update spinner.");
        }

        // --- Step 3: Determine and set the new active playlist index ---
        int newActiveIndex;
        if (allPlaylists.isEmpty()) {
            Log.w("DeletePlaylist", "Playlist became empty after deletion!");
            newActiveIndex = -1; // Or handle creation of a default one?
            // For now, set to -1 and let subsequent logic handle it.
            currentPlaylistIndex = -1; // Explicitly set current index
        } else {
            // Logic to select the next active index (e.g., previous one, or 0)
            if (indexToDelete < currentPlaylistIndex) {
                newActiveIndex = currentPlaylistIndex - 1;
            } else { // Deleted at or after the current index
                newActiveIndex = Math.max(0, indexToDelete - 1); // Select previous or 0
                if (newActiveIndex >= allPlaylists.size()) { // Safety check if deleting last item
                    newActiveIndex = Math.max(0, allPlaylists.size() - 1);
                }
            }
            Log.d("DeletePlaylist", "Calculated new active index: " + newActiveIndex);
            // Set the new current index BEFORE calling switchPlaylist or setting spinner
            currentPlaylistIndex = newActiveIndex;
        }

        // --- Step 4: Update UI (Spinner Selection and Song List) ---
        if (currentPlaylistIndex != -1) { // Only if there's a valid playlist left
            Log.d("DeletePlaylist", "Setting spinner selection to new active index: " + currentPlaylistIndex);
            // Set spinner selection visually
            if (playlistSpinner != null) {
                // Use post to ensure adapter update has settled? Maybe not needed.
                playlistSpinner.setSelection(currentPlaylistIndex);
            }
            // Refresh the song list view for the NEW active playlist
            Log.d("DeletePlaylist", "Refreshing song list view for the new active playlist.");
            refreshCurrentPlaylistView();
        } else {
            // Handle the case where the last playlist was deleted
            Log.w("DeletePlaylist", "Last playlist deleted. Clearing song list view.");
            refreshCurrentPlaylistView(); // Will show empty list
            // Optionally create a default playlist here
            // createPlaylist("默认歌单"); // This would trigger saves etc.
        }

        // Update delete button state
        if (btnDeletePlaylist != null) {
            btnDeletePlaylist.setEnabled(allPlaylists != null && allPlaylists.size() > 1);
            Log.d("DeletePlaylist", "Delete button enabled: " + btnDeletePlaylist.isEnabled());
        }

        // --- Step 5: Save the changes ---
        saveAllPlaylists();
        Toast.makeText(this, "歌单 '" + deletedPlaylist.getName() + "' 已删除", Toast.LENGTH_SHORT).show();

        Log.d("DeletePlaylist", "--- Exiting deletePlaylist ---");
    }

    /**
     * Updates the playlist spinner UI based on the current allPlaylists list.
     */
    private void updatePlaylistSpinner() {
        playlistSpinnerAdapter.clear();
        List<String> playlistNames = new ArrayList<>();
        for (Playlist p : allPlaylists) {
            playlistNames.add(p.getName());
        }
        playlistSpinnerAdapter.addAll(playlistNames);
        playlistSpinnerAdapter.notifyDataSetChanged();

        // Ensure the spinner selection matches the currentPlaylistIndex
        if (currentPlaylistIndex >= 0 && currentPlaylistIndex < allPlaylists.size()) {
            playlistSpinner.setSelection(currentPlaylistIndex);
        } else if (!allPlaylists.isEmpty()) {
            // If index is invalid, reset to 0
            currentPlaylistIndex = 0;
            playlistSpinner.setSelection(0);
        }
        // Update delete button state
        btnDeletePlaylist.setEnabled(allPlaylists.size() > 1);
    }

    private void playSong(int position) {
        Playlist currentPlaylist = getCurrentPlaylist();
        if (currentPlaylist == null || position < 0 || position >= currentPlaylist.getSongCount()) {
            Toast.makeText(this, "无效的播放位置", Toast.LENGTH_SHORT).show();
            Log.e("Playback", "Invalid position or playlist null. Pos: " + position + ", Playlist: "
                    + (currentPlaylist == null ? "null" : currentPlaylist.getName()));
            return;
        }

        Song song = currentPlaylist.getSongs().get(position);
        Log.d("Playback", "Attempting to play: " + song.getName() + " at index " + position + " in playlist "
                + currentPlaylist.getName());
        setDefaultCoverArt();
        backgroundExecutor.submit(() -> extractAndDisplayCoverArt(song));
        setNavigationButtonsEnabled(false);
        isPreparing = true;
        synchronized (this) {
            handler.removeCallbacks(updateProgressRunnable);
            stoppedAtIndex = -1;
            Log.d("Playback", "Removed progress update callbacks.");
            try {
                // Safely release old player
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                    // Reset needed before setting new source if player exists
                    try {
                        mediaPlayer.reset();
                    } catch (IllegalStateException e) {
                        Log.w("MediaPlayer", "Reset failed, creating new instance.", e);
                        mediaPlayer.release();
                        mediaPlayer = null; // Force creation below
                    }
                }
                // Ensure mediaPlayer instance exists
                if (mediaPlayer == null) {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                }

                Uri uri = Uri.parse(song.getFilePath());
                // Check URI access before setting data source
                try {
                    getContentResolver().openFileDescriptor(uri, "r").close(); // Check read access
                    Log.d("Playback", "URI access verified for: " + uri.toString());
                } catch (Exception e) {
                    Log.e("Playback", "URI permission likely lost for: " + uri.toString(), e);
                    Toast.makeText(this, "无法访问文件，请重新添加: " + song.getName(), Toast.LENGTH_LONG).show();
                    // Optional: Remove the song automatically? Or just fail playback?
                    // removeSong(position); // Be careful with indices if you do this
                    return; // Stop playback attempt
                }

                mediaPlayer.setDataSource(this, uri); // Set data source
                mediaPlayer.prepareAsync(); // Prepare asynchronously

                mediaPlayer.setOnPreparedListener(mp -> {
                    Log.d("Playback", "MediaPlayer prepared for: " + song.getName());

                    isPreparing = false; // 标记准备已完成
                    stoppedAtIndex = -1;
                    seekBar.setMax(mp.getDuration());
                    mp.start(); // Start playback
                    isPlaying = true;
                    currentPlayingIndex = position; // Update the index *within the current playlist*
                    songAdapter.setPlayingPosition(position); // Highlight in adapter
                    handler.post(updateProgressRunnable); // Start progress updates

                    // 重新启用导航按钮
                    setNavigationButtonsEnabled(true);

                    if (songAdapter != null) {
                        // Use setPlayingPosition which handles notifying previous and current items
                        songAdapter.setPlayingPosition(currentPlayingIndex);
                        Log.d("Playback", "Adapter highlight set for index: " + currentPlayingIndex);
                    }
                    scrollToPlayingItem(currentPlayingIndex);

                    // 设置可视化器（如果已初始化）
                    if (visualizerManager != null) {
                        // 检查录音权限
                        if (checkSelfPermission(
                                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            Log.d("Playback", "Initializing visualizer for MediaPlayer, audioSessionId: "
                                    + mediaPlayer.getAudioSessionId());

                            // 延迟一下再初始化，确保MediaPlayer完全准备好
                            handler.postDelayed(() -> {
                                Log.d("Playback", "Delayed visualizer initialization");
                                if (visualizerManager.initialize(mediaPlayer)) {
                                    visualizerManager.start();
                                    if (visualizerView != null) {
                                        visualizerView.startVisualization();
                                    }
                                    Log.d("Playback", "Visualizer started successfully");

                                    // 启动备用音频数据生成器
                                    startBackupAudioDataGenerator();
                                } else {
                                    Log.w("Playback", "Visualizer initialization failed");
                                }
                            }, 500); // 延迟500ms
                        } else {
                            Log.w("Playback", "RECORD_AUDIO permission not granted, visualizer not available");
                            Toast.makeText(this, "需要录音权限才能显示音频可视化", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w("Playback", "VisualizerManager is null");
                    }

                    // 触发封面翻转动画
                    if (flipAnimator != null) {
                        flipAnimator.startFlipAnimation();
                    }

                    // Update UI status
                    TextView tvStatus = findViewById(R.id.tv_status);
                    TextView currentSongTextView = findViewById(R.id.tv_current_song_name);
                    tvStatus.setText("正在播放");
                    currentSongTextView.setText(song.getName());
                    Log.d("UI Update", "Displaying song: " + song.getName());

                    mediaPlayer.setOnCompletionListener(mpCompleted -> {
                        Log.d("Playback", "Song completed: " + song.getName());
                        isPlaying = false; // Mark as not playing
                        handler.removeCallbacks(updateProgressRunnable); // Stop progress updates
                        currentPlayingIndex = position; // Keep index for SINGLE mode check

                        if (currentPlayMode == PlayMode.SINGLE) {
                            Log.d("Playback", "Single mode loop.");
                            // Re-prepare and play the same song
                            playSong(currentPlayingIndex);
                        } else {
                            Log.d("Playback", "Moving to next song.");
                            // Need to ensure playNext calculates based on current playlist size
                            playNext();
                        }
                    });

                    mediaPlayer.setOnErrorListener((mpOnError, what, extra) -> {
                        Log.e("Playback",
                                "MediaPlayer Error: what=" + what + ", extra=" + extra + " for " + song.getName());
                        Toast.makeText(MainActivity.this, "播放错误: " + song.getName(), Toast.LENGTH_SHORT).show();
                        isPreparing = false; // Preparation failed
                        setNavigationButtonsEnabled(true);
                        stopPlayback(); // Reset on error
                        setDefaultCoverArt();
                        return true; // Indicate error was handled
                    });

                });

                // Update last played index for the playlist (do this when starting playback)
                // currentPlaylist.setLastPlayingIndex(position); // Save for persistence

            } catch (IOException | IllegalStateException | SecurityException e) {
                Log.e("Playback", "Playback initialization failed for " + song.getName(), e);
                stoppedAtIndex = -1;
                isPreparing = false; // Initialization failed
                setNavigationButtonsEnabled(true);
                Toast.makeText(this, "播放失败：" + song.getName() + " - " + e.getMessage(), Toast.LENGTH_SHORT).show();
                // Reset state if failed
                stopPlayback();
                setDefaultCoverArt();
            }
        }
    }

    private void scrollToPlayingItem(final int position) {
        // Ensure position is valid
        if (position < 0) {
            Log.w("Scroll", "scrollToPlayingItem called with invalid position: " + position);
            return;
        }

        final RecyclerView songRecyclerView = findViewById(R.id.song_recycler_view); // Make final for use in post()
        final RecyclerView.LayoutManager layoutManager = songRecyclerView.getLayoutManager(); // Make final

        if (songRecyclerView != null && layoutManager instanceof LinearLayoutManager) { // Check if it's
                                                                                        // LinearLayoutManager
                                                                                        // Use post to ensure scrolling
                                                                                        // happens after layout
                                                                                        // calculations might have
                                                                                        // settled
            songRecyclerView.post(() -> {
                Log.d("Scroll", "Attempting to smooth scroll to position: " + position);

                // Create a custom SmoothScroller that snaps to the top
                RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(songRecyclerView.getContext()) {
                    @Override
                    protected int getVerticalSnapPreference() {
                        // This is the key: Align the *top* of the target item
                        // with the *top* of the RecyclerView parent.
                        return LinearSmoothScroller.SNAP_TO_START;
                    }

                    // Optional: Adjust the scroll speed if desired
                    // Increase the value returned for slower scrolling, decrease for faster.
                    // The default value is 25f / displayMetrics.densityDpi.
                    /*
                     * @Override
                     * protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                     * // Example: Make scroll take roughly 100ms per inch
                     * // return 100f / displayMetrics.densityDpi;
                     * // Or make it slightly slower than default:
                     * return super.calculateSpeedPerPixel(displayMetrics) * 1.5f; // 50% slower
                     * }
                     */
                };

                // Set the target position for the scroller
                smoothScroller.setTargetPosition(position);

                // Start the smooth scroll action using the LayoutManager
                Log.d("Scroll", "Starting smooth scroll with SNAP_TO_START for position: " + position);
                layoutManager.startSmoothScroll(smoothScroller);
            });
        } else {
            Log.e("Scroll", "Cannot scroll: RecyclerView, LayoutManager is null, or not a LinearLayoutManager.");
        }
    } // End of scrollToPlayingItem

    /**
     * Helper method to enable or disable the Previous and Next buttons.
     * Ensures this runs on the UI thread.
     * 
     * @param enabled true to enable, false to disable.
     */
    private void setNavigationButtonsEnabled(final boolean enabled) {
        // Run on UI thread as this modifies UI elements
        runOnUiThread(() -> {
            if (prevButton != null) {
                prevButton.setEnabled(enabled);
                // Optional: change visual appearance when disabled (e.g., alpha)
                prevButton.setAlpha(enabled ? 1.0f : 0.5f);
            }
            if (nextButton != null) {
                nextButton.setEnabled(enabled);
                nextButton.setAlpha(enabled ? 1.0f : 0.5f);
            }
            Log.d("UIUpdate", "Navigation buttons enabled: " + enabled);
        });
    }

    private void prepareMediaPlayer(int position) {
        Playlist currentPlaylist = getCurrentPlaylist();
        if (currentPlaylist == null || position < 0 || position >= currentPlaylist.getSongCount())
            return;

        Song song = currentPlaylist.getSongs().get(position);
        Uri uri = Uri.parse(song.getFilePath());

        try {
            // Release existing player first
            if (mediaPlayer != null) {
                // Check if playing before releasing
                if (mediaPlayer.isPlaying())
                    mediaPlayer.stop();
                mediaPlayer.reset(); // Use reset instead of release if reusing the object
            } else {
                mediaPlayer = new MediaPlayer(); // Create if null
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }

            // Verify URI access
            getContentResolver().openFileDescriptor(uri, "r").close();

            mediaPlayer.setDataSource(this, uri);
            mediaPlayer.prepareAsync(); // Prepare async

            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d("Prepare", "Prepared: " + song.getName());
                seekBar.setMax(mp.getDuration());
                updateProgressText(0, mp.getDuration()); // Update text for prepared song

                // Restore saved position *for this song* if needed (more complex, requires
                // saving per song)
                // SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                // int savedPosition = prefs.getInt("lastPosition_" + song.getFilePath(), 0); //
                // Example key
                // mp.seekTo(savedPosition);

                if (shouldAutoPlay) { // Check flag before starting
                    mp.start();
                    isPlaying = true;
                    currentPlayingIndex = position;
                    songAdapter.setPlayingPosition(position);
                    handler.post(updateProgressRunnable);
                    // Update UI
                    TextView tvStatus = findViewById(R.id.tv_status);
                    TextView currentSongTextView = findViewById(R.id.tv_current_song_name);
                    tvStatus.setText("正在播放");
                    currentSongTextView.setText(song.getName());
                } else {
                    // Just update UI to show it's ready
                    TextView currentSongTextView = findViewById(R.id.tv_current_song_name);
                    currentSongTextView.setText(song.getName() + " (准备就绪)");
                }

                // Set completion listener here as well
                mp.setOnCompletionListener(mpCompleted -> {
                    isPlaying = false;
                    handler.removeCallbacks(updateProgressRunnable);
                    if (currentPlayMode == PlayMode.SINGLE) {
                        playSong(currentPlayingIndex);
                    } else {
                        playNext();
                    }
                });

                mp.setOnErrorListener((mpOnError, what, extra) -> {
                    Log.e("Playback", "MediaPlayer Error during prepare/play: what=" + what + ", extra=" + extra);
                    Toast.makeText(MainActivity.this, "播放错误: " + song.getName(), Toast.LENGTH_SHORT).show();
                    stopPlayback();
                    return true;
                });
            });

        } catch (IOException | IllegalStateException | SecurityException e) {
            Log.e("Prepare", "Prepare failed for " + song.getName(), e);
            Toast.makeText(this, "无法准备文件: " + song.getName(), Toast.LENGTH_SHORT).show();
            stopPlayback();
        }
    }

    // delete the song we chosen
    private void removeSelectedSong() {

        Playlist currentPlaylist = getCurrentPlaylist();
        if (currentPlaylist != null && currentPlayingIndex >= 0
                && currentPlayingIndex < currentPlaylist.getSongCount()) {
            removeSong(currentPlayingIndex); // Call the modified removeSong
        } else {
            Toast.makeText(this, "没有选中歌曲或歌单为空", Toast.LENGTH_SHORT).show();
        }
    }

    // Modify removeSong to remove from the *current* playlist
    // --- Location: MainActivity.java ---
    // --- Location: MainActivity.java ---
    @SuppressLint("NotifyDataSetChanged")
    private void removeSong(int position) {
        Playlist currentPlaylist = getCurrentPlaylist();
        if (currentPlaylist == null || position < 0 || position >= currentPlaylist.getSongCount()) {
            Log.w("RemoveSong", "Invalid position or playlist null. Pos: " + position);
            return;
        }

        synchronized (this) {
            Song removedSong = currentPlaylist.getSongs().get(position);
            Log.d("RemoveSong", "Removing song: " + removedSong.getName() + " at index " + position + " from playlist "
                    + currentPlaylist.getName());
            Log.d("RemoveSong", "Playlist size BEFORE remove: " + currentPlaylist.getSongCount());

            boolean isCurrentPlayingSong = (position == currentPlayingIndex);

            // Stop playback if needed (BEFORE modifying list)
            if (isCurrentPlayingSong && mediaPlayer != null && (isPlaying || isPreparing)) {
                Log.d("RemoveSong", "Stopping playback as current song is removed.");
                stopPlayback();
            }

            // --- Step 1: Remove the song from the actual data source (Playlist) ---
            currentPlaylist.removeSong(position);
            Log.d("RemoveSong", "Playlist size AFTER remove: " + currentPlaylist.getSongCount());

            // --- Step 2: Update the Adapter's internal list to match ---
            // This is the crucial step that was missing or incorrect before.
            if (songAdapter != null) {
                Log.d("RemoveSong", "Updating adapter's internal list BEFORE notifying.");
                // Use the method that refreshes the adapter's internal list
                // and calls notifyDataSetChanged internally.
                songAdapter.updateSongList(currentPlaylist.getSongs());
                Log.d("RemoveSong", "Adapter list updated. New adapter count: " + songAdapter.getItemCount());
            } else {
                Log.e("RemoveSong", "songAdapter is null, cannot update its list!");
            }

            // --- Step 3: Adjust the playing index if needed ---
            // (This needs to happen AFTER the list is modified but BEFORE saving state
            // potentially)
            if (isCurrentPlayingSong) {
                // stopPlayback already set currentPlayingIndex = -1
                Log.d("RemoveSong", "Current playing index remains -1 after stop.");
            } else if (position < currentPlayingIndex) {
                currentPlayingIndex--;
                Log.d("RemoveSong", "Adjusted current playing index to: " + currentPlayingIndex);
                // Optional: If you want the highlight to immediately reflect the new index
                // after deletion
                // This might be needed if updateSongList doesn't handle restoring highlight
                // correctly
                if (songAdapter != null && currentPlayingIndex != -1) {
                    // We need to ensure this doesn't conflict with updateSongList clearing
                    // highlight
                    // Maybe call setPlayingPosition *after* updateSongList? Or handle in
                    // updateSongList?
                    // For now, let's rely on updateSongList clearing and subsequent play action to
                    // highlight.
                }
            }

            // --- Step 4: Save the updated playlists ---
            saveAllPlaylists();
            Toast.makeText(this, "歌曲 '" + removedSong.getName() + "' 已删除", Toast.LENGTH_SHORT).show();

            // Logging after save
            Log.d("RemoveSong", "Playlist size after save: " + currentPlaylist.getSongCount());
        }
    }

    // --- Location: MainActivity.java ---
    private void playPrevious() {
        // animateButton(v);
        Playlist currentPlaylist = getCurrentPlaylist();
        if (currentPlaylist == null || currentPlaylist.getSongCount() == 0) {
            Toast.makeText(this, "歌单为空", Toast.LENGTH_SHORT).show();
            return;
        }

        int songCount = currentPlaylist.getSongCount();
        int prevPosition = -1; // Initialize

        // --- Check play mode FIRST ---
        if (currentPlayMode == PlayMode.SINGLE) {
            // In single loop, "Previous" just restarts the current song
            Log.d("PlayPrevious", "Single mode: Restarting current song index " + currentPlayingIndex);
            prevPosition = currentPlayingIndex; // Target the current index
            // Ensure currentPlayingIndex is valid, otherwise fallback
            if (prevPosition < 0 || prevPosition >= songCount) {
                Log.w("PlayPrevious", "Single mode: currentPlayingIndex is invalid (" + currentPlayingIndex
                        + "). Falling back to 0.");
                prevPosition = 0; // Fallback to first song if index is somehow invalid
            }

        } else if (currentPlayMode == PlayMode.SHUFFLE) {
            // Handle Shuffle mode (existing logic)
            if (shuffleOrder.isEmpty() || shuffleOrder.size() != songCount)
                generateShuffleOrder();
            if (!shuffleOrder.isEmpty()) { // Check if shuffle order exists
                // Go back in shuffle order, handle wrap around carefully
                shuffleIndex = (shuffleIndex - 1);
                if (shuffleIndex < 0) {
                    shuffleIndex = shuffleOrder.size() - 1; // Wrap to end
                }
                prevPosition = shuffleOrder.get(shuffleIndex);
                Log.d("PlayPrevious", "Shuffle mode: Calculated previous index " + prevPosition
                        + " from shuffle order (shuffleIndex=" + shuffleIndex + ")");
            } else {
                Log.e("PlayPrevious", "Shuffle mode: Shuffle order is empty. Falling back to 0.");
                prevPosition = 0; // Fallback if shuffle order is empty
            }

        } else { // LOOP Mode (or default)
            // Handle Loop mode (existing logic)
            if (currentPlayingIndex <= 0) {
                prevPosition = songCount - 1; // Wrap to end
            } else {
                prevPosition = currentPlayingIndex - 1;
            }
            Log.d("PlayPrevious", "Loop mode: Calculated previous index " + prevPosition);
        }

        // --- Final Validation and Play ---
        if (prevPosition >= 0 && prevPosition < songCount) {
            Log.d("PlayPrevious", "Final target index: " + prevPosition + ". Calling playSong.");
            playSong(prevPosition); // playSong will handle re-enabling buttons
        } else {
            Log.e("PlayPrevious",
                    "Failed to determine a valid previous position. Final calculated index: " + prevPosition);
            // Optionally play the first song as a fallback if calculation fails
            if (songCount > 0) {
                Log.w("PlayPrevious", "Fallback: Playing first song (index 0).");
                playSong(0);
            }
        }
    }

    private void playNext() {
        // animateButton(v);
        Playlist currentPlaylist = getCurrentPlaylist();
        if (currentPlaylist == null || currentPlaylist.getSongs().isEmpty()) {
            Toast.makeText(this, "歌单为空", Toast.LENGTH_SHORT).show();
            stopPlayback(); // Stop if list becomes empty somehow
            return;
        }

        int songCount = currentPlaylist.getSongCount();
        if (songCount == 0)
            return; // Should be caught above, but double check

        int nextPosition = -1;

        // According to play mode
        switch (currentPlayMode) {
            case SINGLE:
                // In the OnCompletionListener, SINGLE mode already restarts the current song.
                // If called manually (e.g., button press), play the *same* song again.
                nextPosition = currentPlayingIndex;
                break;
            case SHUFFLE:
                if (shuffleOrder.isEmpty() || shuffleOrder.size() != songCount) {
                    Log.d("PlayNext", "Generating shuffle order for size: " + songCount);
                    generateShuffleOrder(); // Regenerate if needed (playlist changed size?)
                }
                // Increment shuffle index, wrapping around if necessary
                shuffleIndex = (shuffleIndex + 1);
                if (shuffleIndex >= shuffleOrder.size()) {
                    Log.d("PlayNext", "Shuffle loop completed, reshuffling.");
                    Collections.shuffle(shuffleOrder); // Reshuffle when order completes
                    shuffleIndex = 0;
                }
                if (!shuffleOrder.isEmpty()) { // Check if shuffle order is valid
                    nextPosition = shuffleOrder.get(shuffleIndex);
                } else {
                    Log.e("PlayNext", "Shuffle order empty after generation!");
                    nextPosition = 0; // Fallback to first song
                }

                break;
            case LOOP:
            default:
                // Loop back to the beginning from the last song
                nextPosition = (currentPlayingIndex + 1) % songCount;
                break;
        }

        // Validate final position before playing
        if (nextPosition >= 0 && nextPosition < songCount) {
            Log.d("PlayNext", "Playing next song at index: " + nextPosition);
            playSong(nextPosition);
        } else {
            Log.e("PlayNext", "Calculated invalid next position: " + nextPosition + " (current index: "
                    + currentPlayingIndex + ", mode: " + currentPlayMode + ")");
            // Fallback: Play the first song if calculation somehow failed
            if (songCount > 0)
                playSong(0);
        }
    }

    private int getTimeDuration(Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, uri);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return (durationStr != null) ? Integer.parseInt(durationStr) : 0; // 直接返回毫秒值
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

    private void saveAllPlaylists() {
        if (allPlaylists == null) {
            Log.e("SavePlaylist", "allPlaylists is null, cannot save.");
            return;
        }

        JSONArray playlistsJsonArray = new JSONArray();
        for (Playlist playlist : allPlaylists) {
            // Update last playing index for the currently active playlist before saving
            if (playlist == getCurrentPlaylist()) {
                playlist.setLastPlayingIndex(currentPlayingIndex);
            }
            try {
                // We need to ensure permissions are obtainable later, but don't need to call
                // takePersistableUriPermission here during save.
                // Loading logic will handle re-acquiring permissions.
                playlistsJsonArray.put(playlist.toJson());
            } catch (JSONException e) {
                Log.e("SavePlaylist", "Failed to convert playlist '" + playlist.getName() + "' to JSON", e);
            }
        }

        JSONObject rootObject = new JSONObject();
        try {
            // Embed the array within a root object if needed, or just save the array
            // rootObject.put("playlists", playlistsJsonArray); // Optional structure
            // Save the array directly for simplicity matching Playlist.fromJson expecting
            // an object
            File file = new File(getExternalFilesDir(null), PLAYLISTS_FILENAME);
            try (FileWriter writer = new FileWriter(file)) {
                // writer.write(rootObject.toString(2)); // Use rootObject if you structured it
                // that way
                writer.write(playlistsJsonArray.toString(2)); // Save array directly (pretty print)
                Log.d("SavePlaylist", "All playlists saved successfully to " + file.getAbsolutePath());
            } catch (IOException e) {
                Log.e("SavePlaylist", "Failed to write playlists file", e);
            }

        } catch (JSONException e) {
            Log.e("SavePlaylist", "Failed to construct final JSON structure", e);
        }

        // Also save the index of the last active playlist separately
        saveLastActivePlaylistIndex();
    }

    /**
     * Loads all playlists from the JSON file.
     */
    private void loadAllPlaylists() {
        allPlaylists.clear(); // Clear existing list before loading
        File file = new File(getExternalFilesDir(null), PLAYLISTS_FILENAME);

        if (!file.exists()) {
            Log.d("LoadPlaylist", "Playlists file not found. Creating default playlist.");
            // Create a default playlist if the file doesn't exist
            allPlaylists.add(new Playlist("默认歌单")); // Default playlist name
            currentPlaylistIndex = 0;
            saveAllPlaylists(); // Save the default one immediately
        } else {
            StringBuilder jsonString = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonString.append(line);
                }
            } catch (IOException e) {
                Log.e("LoadPlaylist", "Failed to read playlists file", e);
                // Fallback: Create default if reading fails
                if (allPlaylists.isEmpty()) {
                    allPlaylists.add(new Playlist("默认歌单"));
                    currentPlaylistIndex = 0;
                }
            }

            try {
                // Assuming the file directly contains the JSON array
                JSONArray playlistsJsonArray = new JSONArray(jsonString.toString());
                for (int i = 0; i < playlistsJsonArray.length(); i++) {
                    JSONObject playlistJson = playlistsJsonArray.getJSONObject(i);
                    Playlist loadedPlaylist = Playlist.fromJson(playlistJson); // Use static factory method

                    // --- Crucial: Re-acquire permissions for all songs in the loaded playlist ---
                    boolean playlistValid = true;
                    List<Song> songsToRemove = new ArrayList<>(); // To avoid concurrent modification
                    for (Song song : loadedPlaylist.getSongs()) {
                        Uri uri = Uri.parse(song.getFilePath());
                        try {
                            // Check if we still have permission
                            getContentResolver().takePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            Log.d("LoadPlaylist", "Permission retained/re-acquired for: " + uri);
                        } catch (SecurityException e) {
                            Log.e("LoadPlaylist", "Permission lost for URI: " + uri + " in playlist "
                                    + loadedPlaylist.getName() + ". Removing song.");
                            Toast.makeText(this,
                                    "无法访问歌曲 '" + song.getName() + "'，已从歌单 '" + loadedPlaylist.getName() + "' 移除",
                                    Toast.LENGTH_LONG).show();
                            songsToRemove.add(song); // Mark for removal
                            // playlistValid = false; // Decide if one bad song invalidates the playlist
                        }
                    }
                    // Remove songs for which permission was lost
                    if (!songsToRemove.isEmpty()) {
                        loadedPlaylist.getSongs().removeAll(songsToRemove);
                    }

                    // Add the loaded (and potentially cleaned) playlist
                    if (playlistValid) { // Only add if deemed valid (e.g., if you didn't set playlistValid=false)
                        allPlaylists.add(loadedPlaylist);
                    } else {
                        Log.w("LoadPlaylist",
                                "Playlist '" + loadedPlaylist.getName() + "' skipped due to missing permissions.");
                        // Optionally inform user playlist couldn't be fully loaded
                    }
                }

                // If after loading, the list is somehow empty (e.g., file was '[]' or all
                // permissions failed)
                if (allPlaylists.isEmpty()) {
                    Log.w("LoadPlaylist", "No valid playlists loaded. Creating default.");
                    allPlaylists.add(new Playlist("默认歌单"));
                    currentPlaylistIndex = 0;
                    saveAllPlaylists(); // Save the new default
                }

            } catch (JSONException e) {
                Log.e("LoadPlaylist", "Failed to parse playlists JSON", e);
                // Fallback: Create default if parsing fails
                if (allPlaylists.isEmpty()) {
                    allPlaylists.add(new Playlist("默认歌单"));
                    currentPlaylistIndex = 0;
                }
            }
        }

        // --- Restore the last active playlist index ---
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int lastIndex = prefs.getInt(KEY_LAST_ACTIVE_PLAYLIST_INDEX, 0);
        // Validate the loaded index against the actual number of playlists
        if (lastIndex >= 0 && lastIndex < allPlaylists.size()) {
            currentPlaylistIndex = lastIndex;
        } else {
            currentPlaylistIndex = 0; // Default to first playlist if index is invalid
        }
        Log.d("LoadPlaylist", "Set active playlist index to: " + currentPlaylistIndex);

        // --- Update UI based on loaded data ---
        updatePlaylistSpinner(); // Populate the spinner
        // Update the song list in the adapter for the initially active playlist
        Playlist activePlaylist = getCurrentPlaylist();
        songAdapter.updateSongList(activePlaylist != null ? activePlaylist.getSongs() : new ArrayList<>());

        // --- Restore Playback State (Optional - Restore for the active playlist) ---
        if (activePlaylist != null) {
            int lastSongIndex = activePlaylist.getLastPlayingIndex(); // Use index saved per-playlist
            SharedPreferences playerStatePrefs = getSharedPreferences("PlayerState", MODE_PRIVATE); // Separate prefs
                                                                                                    // for state
            int lastPositionMs = playerStatePrefs.getInt("lastPosition_" + activePlaylist.getName(), 0); // Key includes
                                                                                                         // playlist
                                                                                                         // name
            boolean wasPlaying = playerStatePrefs.getBoolean("wasPlaying_" + activePlaylist.getName(), false);

            Log.d("LoadPlaylist", "Restoring state for playlist '" + activePlaylist.getName() + "': lastSongIndex="
                    + lastSongIndex + ", lastPositionMs=" + lastPositionMs + ", wasPlaying=" + wasPlaying);

            if (lastSongIndex >= 0 && lastSongIndex < activePlaylist.getSongCount()) {
                currentPlayingIndex = lastSongIndex; // Set the index
                // Prepare the media player for this song, but don't auto-play yet
                prepareMediaPlayer(currentPlayingIndex);

                // Seek to the saved position *after* preparation is complete (in
                // OnPreparedListener)
                // We need to store this desired seek position temporarily
                final int seekToPos = lastPositionMs;
                mediaPlayer.setOnPreparedListener(mp -> {
                    Log.d("RestoreState", "MediaPlayer prepared, seeking to " + seekToPos);
                    mp.seekTo(seekToPos);
                    seekBar.setMax(mp.getDuration());
                    updateProgressText(seekToPos, mp.getDuration()); // Update UI with restored position

                    // Now decide if we should start playing based on saved state
                    if (wasPlaying) {
                        shouldAutoPlay = true; // Set flag for prepareMediaPlayer or start manually
                        mp.start();
                        isPlaying = true;
                        songAdapter.setPlayingPosition(currentPlayingIndex);
                        handler.post(updateProgressRunnable);
                        TextView tvStatus = findViewById(R.id.tv_status);
                        tvStatus.setText("正在播放");
                    } else {
                        shouldAutoPlay = false;
                        isPlaying = false;
                        TextView tvStatus = findViewById(R.id.tv_status);
                        tvStatus.setText("已暂停"); // Or "准备就绪"
                        // Ensure adapter highlights the loaded song even if not playing
                        songAdapter.setPlayingPosition(currentPlayingIndex);
                    }

                    // Re-attach completion listener
                    mp.setOnCompletionListener(mpCompleted -> {
                        isPlaying = false;
                        handler.removeCallbacks(updateProgressRunnable);
                        if (currentPlayMode == PlayMode.SINGLE)
                            playSong(currentPlayingIndex);
                        else
                            playNext();
                    });
                    mp.setOnErrorListener((mpOnError, what, extra) -> {
                        /* ... error handling ... */ return true;
                    });
                });

            } else {
                Log.d("LoadPlaylist", "No valid last song index to restore for this playlist.");
                currentPlayingIndex = -1; // Ensure index is reset if no song to restore
            }
        } else {
            Log.w("LoadPlaylist", "Active playlist is null after loading, cannot restore state.");
        }

        // Load play mode
        String savedMode = prefs.getString("playMode", PlayMode.LOOP.name());
        try {
            currentPlayMode = PlayMode.valueOf(savedMode);
        } catch (IllegalArgumentException e) {
            currentPlayMode = PlayMode.LOOP; // Default if saved value is invalid
        }
        updatePlayModeText(); // Update UI for play mode

    }

    /**
     * Saves the index of the currently active playlist to SharedPreferences.
     */
    private void saveLastActivePlaylistIndex() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_LAST_ACTIVE_PLAYLIST_INDEX, currentPlaylistIndex);
        // Save play mode as well
        editor.putString("playMode", currentPlayMode.name());
        editor.apply();
        Log.d("SaveState",
                "Saved last active playlist index: " + currentPlaylistIndex + " and mode: " + currentPlayMode.name());
    }

    /**
     * Saves the current playback state (song index, position, playing status) for
     * the active playlist.
     */
    private void savePlaybackState() {
        Playlist activePlaylist = getCurrentPlaylist();
        if (activePlaylist == null || mediaPlayer == null)
            return;

        SharedPreferences playerStatePrefs = getSharedPreferences("PlayerState", MODE_PRIVATE); // Separate prefs for
                                                                                                // state
        SharedPreferences.Editor editor = playerStatePrefs.edit();

        String playlistKeyPrefix = activePlaylist.getName(); // Use playlist name in key

        if (isPlaying || (mediaPlayer.getCurrentPosition() > 0 && currentPlayingIndex != -1)) {
            // Save state only if something is loaded/playing
            editor.putInt("lastSongIndex_" + playlistKeyPrefix, currentPlayingIndex);
            editor.putInt("lastPosition_" + playlistKeyPrefix, mediaPlayer.getCurrentPosition());
            editor.putBoolean("wasPlaying_" + playlistKeyPrefix, isPlaying); // Save if it was actively playing
            Log.d("SaveState", "Saving state for '" + playlistKeyPrefix + "': index=" + currentPlayingIndex + ", pos="
                    + mediaPlayer.getCurrentPosition() + ", playing=" + isPlaying);
        } else {
            // Clear state if playback is fully stopped/reset
            editor.remove("lastSongIndex_" + playlistKeyPrefix);
            editor.remove("lastPosition_" + playlistKeyPrefix);
            editor.remove("wasPlaying_" + playlistKeyPrefix);
            Log.d("SaveState", "Clearing saved state for '" + playlistKeyPrefix + "'");
        }

        editor.apply();
    }

    private void switchPlayMode() {
        Playlist currentPlaylist = getCurrentPlaylist();
        int songCount = (currentPlaylist != null) ? currentPlaylist.getSongCount() : 0;

        if (currentPlayMode == PlayMode.LOOP) {
            currentPlayMode = PlayMode.SHUFFLE;
            // Generate shuffle order based on the *current* playlist size
            generateShuffleOrder(); // This now uses getCurrentSongs() size
            Log.d("PlayMode", "Switched to SHUFFLE. Order size: " + (shuffleOrder != null ? shuffleOrder.size() : 0));
        } else if (currentPlayMode == PlayMode.SHUFFLE) {
            currentPlayMode = PlayMode.SINGLE;
            Log.d("PlayMode", "Switched to SINGLE.");
        } else { // Was SINGLE
            currentPlayMode = PlayMode.LOOP;
            Log.d("PlayMode", "Switched to LOOP.");
            // Shuffle order is not needed for LOOP, can clear if desired
            // shuffleOrder.clear();
        }
        updatePlayModeText();
        // Save the new mode immediately
        saveLastActivePlaylistIndex(); // This method now saves mode too
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

    // 转地址编码
    private void generateShuffleOrder() {
        shuffleOrder = new ArrayList<>();
        List<Song> currentSongs = getCurrentSongs(); // Get songs from active playlist
        int songCount = currentSongs.size();
        if (songCount > 0) {
            for (int i = 0; i < songCount; i++) {
                shuffleOrder.add(i);
            }
            Collections.shuffle(shuffleOrder);
            Log.d("Shuffle", "Generated shuffle order for " + songCount + " songs.");
        } else {
            Log.d("Shuffle", "Cannot generate shuffle order, current playlist is empty.");
        }
        shuffleIndex = 0; // Reset index when order is generated/regenerated
    }

    private void addSong() {
        // animateButton(v);
        Playlist currentPlaylist = getCurrentPlaylist();
        if (currentPlaylist == null) {
            Toast.makeText(this, "请先创建或选择一个歌单", Toast.LENGTH_SHORT).show();
            return; // Don't allow adding if no playlist is selected/exists
        }
        Log.d("AddSong", "Opening file picker to add to playlist: " + currentPlaylist.getName());

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, PICK_AUDIO_REQUEST); // Use the constant
        // saveAllPlaylists(); // Save is called in onActivityResult *after* adding
    }

    private void refreshCurrentPlaylistView() {
        Playlist currentPlaylist = getCurrentPlaylist();
        if (songAdapter != null) {
            Log.d("AdapterRefresh", "Refreshing adapter view for playlist: "
                    + (currentPlaylist != null ? currentPlaylist.getName() : "null"));
            // Use the adapter's method to update its internal list and notify
            songAdapter.updateSongList(currentPlaylist != null ? currentPlaylist.getSongs() : new ArrayList<>());
        } else {
            Log.e("AdapterRefresh", "Cannot refresh view, songAdapter is null!");
        }
    }
    // --- Location: MainActivity.java ---

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Playlist playlistBeingAddedTo = getCurrentPlaylist(); // Get the target playlist

        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK && data != null) {
            if (playlistBeingAddedTo == null) {
                Log.e("ActivityResult", "No active playlist selected when receiving files.");
                Toast.makeText(this, "错误：没有选中的歌单来添加歌曲", Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d("ActivityResult", "Attempting to add songs to playlist: " + playlistBeingAddedTo.getName());

            int addedCount = 0;
            boolean changesMade = false; // Flag to track if any song was successfully added

            // --- Process selected files (multi or single) ---
            if (data.getClipData() != null) { // Multi-select
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    if (addSongToList(uri, playlistBeingAddedTo)) { // Add to data model
                        addedCount++;
                        changesMade = true;
                    }
                }
            } else if (data.getData() != null) { // Single-select
                Uri uri = data.getData();
                if (addSongToList(uri, playlistBeingAddedTo)) { // Add to data model
                    addedCount++;
                    changesMade = true;
                }
            }

            // --- Update UI and Save AFTER processing all files ---
            if (changesMade) {
                Log.d("ActivityResult", "Successfully processed " + addedCount + " songs.");
                // Save the updated playlist data
                saveAllPlaylists();
                Toast.makeText(this, "已添加 " + addedCount + " 首歌曲到 '" + playlistBeingAddedTo.getName() + "'",
                        Toast.LENGTH_SHORT).show();

                // --- Refresh the adapter view IF the changes were made to the currently active
                // playlist ---
                if (playlistBeingAddedTo == getCurrentPlaylist()) { // Check if the target playlist is still the active
                                                                    // one
                    Log.d("ActivityResult", "Refreshing adapter view after adding songs.");
                    refreshCurrentPlaylistView(); // Call the helper method
                } else {
                    Log.d("ActivityResult", "Songs added to an inactive playlist. UI not refreshed immediately.");
                }
            } else {
                Log.d("ActivityResult", "No songs were successfully added.");
            }
        }
        if (requestCode == 2001 && resultCode == RESULT_OK && data != null) {
            ArrayList<String> names = data.getStringArrayListExtra("song_names");
            ArrayList<String> paths = data.getStringArrayListExtra("song_paths");
            ArrayList<Integer> durations = data.getIntegerArrayListExtra("song_durations");

            Playlist currentPlaylist = getCurrentPlaylist();
            if (currentPlaylist != null && names != null && paths != null && durations != null) {
                int addCount = 0;
                for (int i = 0; i < names.size(); i++) {
                    String name = names.get(i);
                    String path = paths.get(i);
                    int duration = durations.get(i);
                    boolean exists = false;
                    for (Song s : currentPlaylist.getSongs()) {
                        if (s.getFilePath().equals(path)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        Song song = new Song(name, duration, path);
                        currentPlaylist.addSong(song);
                        addCount++;
                    }
                }
                if (addCount > 0) {
                    saveAllPlaylists();
                    refreshCurrentPlaylistView();
                    Toast.makeText(this, "已添加" + addCount + "首在线歌曲", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "所选歌曲均已存在", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }// End of onActivityResult

    // --- Modify addSongToList to ONLY update the data model ---
    // --- Remove the notifyDataSetChanged() call from here ---
    private boolean addSongToList(Uri uri, Playlist targetPlaylist) {
        if (uri == null || targetPlaylist == null)
            return false;

        // Check for duplicates
        String newFilePath = uri.toString();
        for (Song existingSong : targetPlaylist.getSongs()) {
            if (existingSong.getFilePath().equals(newFilePath)) {
                Toast.makeText(this, getFileName(uri) + " 已存在于歌单 '" + targetPlaylist.getName() + "'",
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        try {
            // Get permission
            getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Get info and create Song
            String fileName = getFileName(uri);
            int duration = getTimeDuration(uri);
            Song newSong = new Song(fileName, duration, uri.toString());

            // --- Add to the playlist's data list ---
            targetPlaylist.addSong(newSong);
            Log.d("AddSongToList", "Data added: '" + fileName + "' to playlist '" + targetPlaylist.getName() + "'");

            // --- DO NOT NOTIFY ADAPTER HERE ---
            // Notification will happen in onActivityResult after all songs are processed.
            /*
             * if (targetPlaylist == getCurrentPlaylist()) {
             * // songAdapter.notifyItemInserted(targetPlaylist.getSongCount() - 1);
             * songAdapter.notifyDataSetChanged(); // REMOVE THIS LINE
             * Log.d("AddSongToList", "Added '" + fileName + "' to current adapter view.");
             * } else {
             * Log.d("AddSongToList", "Added '" + fileName + "' to inactive playlist '" +
             * targetPlaylist.getName() + "'. Adapter not updated immediately.");
             * }
             */
            return true; // Indicate success

        } catch (SecurityException e) {
            Log.e("URI Permission", "Could not take permission for: " + uri + " (" + getFileName(uri) + ")", e);
            Toast.makeText(this, "权限不足，无法添加: " + getFileName(uri), Toast.LENGTH_SHORT).show();
            return false;
        } catch (Exception e) {
            Log.e("AddSongToList", "Failed to get info or add song for URI: " + uri + " (" + getFileName(uri) + ")", e);
            Toast.makeText(this, "添加歌曲失败: " + getFileName(uri), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void handleSelectedAudio(Uri audioUri) {
        Log.w("HandleSelectedAudio", "This method might be redundant. Called for URI: " + audioUri);
        Playlist currentPlaylist = getCurrentPlaylist();
        if (currentPlaylist == null) {
            Toast.makeText(this, "请先选择一个歌单", Toast.LENGTH_SHORT).show();
            return;
        }
        if (addSongToList(audioUri, currentPlaylist)) {
            saveAllPlaylists(); // Save if added successfully
            Toast.makeText(this, "选择: " + getFileName(audioUri) + " 已添加到 " + currentPlaylist.getName(),
                    Toast.LENGTH_SHORT).show();
        }
        // String filename = getFileName(audioUri);
        // Toast.makeText(this, "选择： " + filename, Toast.LENGTH_SHORT);
        // Song newSong = new Song(filename, getTimeDuration(audioUri),
        // audioUri.toString());
        // getCurrentPlaylist().addSong(newSong); // Add to current
        // songAdapter.notifyItemInserted(getCurrentPlaylist().getSongCount() - 1); //
        // Update adapter
        // saveAllPlaylists(); // Save changes
    }

    private String getFileName(Uri uri) {
        String displayName = "";
        try (Cursor cursor = getContentResolver().query(
                uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                displayName = cursor.getString(
                        cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
            }
        }
        return displayName;
    }

    private void stopPlayback() {
        isPlaying = false;
        if (adapter != null) {
            adapter.setPlayingPosition(-1);
        }
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
        }
        handler.removeCallbacks(updateProgressRunnable);
        seekBar.setProgress(0);
        updateProgressText(0, 0);
        findViewById(R.id.btn_pause).setBackgroundResource(R.drawable.ic_play_song);

        // 停止可视化器
        if (visualizerManager != null) {
            visualizerManager.stop();
        }
        if (visualizerView != null) {
            visualizerView.stopVisualization();
        }

        // 设置默认封面
        setDefaultCoverArt();
    }

    private void setDefaultCoverArt() {
        runOnUiThread(() -> {
            if (coverArtImageView != null) {
                coverArtImageView.setImageResource(R.drawable.default_cover_placeholder);
            }
            if (blurBackgroundView != null) {
                blurBackgroundView.setCover(null);
            }
        });
    }

    private void extractAndDisplayCoverArt(Song song) {
        if (backgroundExecutor == null || backgroundExecutor.isShutdown()) {
            backgroundExecutor = Executors.newSingleThreadExecutor();
        }
        backgroundExecutor.execute(() -> {
            Bitmap coverArt = null;
            try {
                Uri uri = Uri.parse(song.getFilePath());
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(this, uri);
                byte[] art = retriever.getEmbeddedPicture();
                if (art != null) {
                    coverArt = BitmapFactory.decodeByteArray(art, 0, art.length);
                }
                retriever.release();
            } catch (Exception e) {
                Log.e("CoverArt", "Error extracting cover art", e);
            }

            final Bitmap finalCoverArt = coverArt;
            runOnUiThread(() -> {
                if (finalCoverArt != null) {
                    coverArtImageView.setImageBitmap(finalCoverArt);
                    blurBackgroundView.setCover(finalCoverArt);
                } else {
                    setDefaultCoverArt();
                }
            });
        });
    }

    private boolean isValidAudioExtension(String extension) {
        switch (extension) {
            case ".mp3":
            case ".flac":
            case ".ogg":
            case ".m4a":
            case ".mp4": // Can contain audio tags
            case ".wav": // Tag support might be limited
            case ".aif":
            case ".aiff":
            case ".dsf": // DSD files
                return true;
            default:
                return false;
        }
    }

    /**
     * Gets the display name of a file from a content URI.
     * 
     * @param uri The content URI.
     * @return The display name, or null if not found.
     */
    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        if (uri != null && "content".equals(uri.getScheme())) {
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e("FileNameUtil", "Error getting filename from URI: " + uri, e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        // Fallback if not a content URI or query failed - try using path segments
        if (fileName == null && uri != null) {
            fileName = uri.getLastPathSegment();
            // Basic sanitization if needed (e.g., remove query params, though less likely
            // for file URIs)
        }
        return fileName;
    }
    // 设置默认封面图片的方法，需要确保该方法在类中存在

    @Override
    protected void onPause() {
        // animateButton(v);
        super.onPause();
        // Save state when the activity is paused (e.g., user switches apps)
        if (mediaPlayer != null) { // Check if player exists
            savePlaybackState();
        }
    }

    @Override
    protected void onStop() {
        // animateButton(v);
        super.onStop();
        // Consider if you want to save state here too. onPause is usually sufficient.
        // savePlaybackState();
    }

    private void updateProgressText(int current, int duration) {
        progress.setText("播放进度: " + timeFormat.format(current) + " / " + timeFormat.format(duration));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("Lifecycle", "onDestroy called.");
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            Log.d("Lifecycle", "Shutting down background executor.");
            backgroundExecutor.shutdown();
        }
        // Save final state before destroying
        saveAllPlaylists(); // Save the playlist structure
        savePlaybackState(); // Save the playback position/status
        // 可视化器会通过生命周期自动清理，这里不需要手动调用
        // Release MediaPlayer resources
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release(); // Release resources completely
            mediaPlayer = null;
            Log.d("Lifecycle", "MediaPlayer released.");
        }
        // Remove handler callbacks
        handler.removeCallbacks(updateProgressRunnable);
    }

    private void animateButton(View v) {
        v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(75)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(75).start()).start();
    }

    /**
     * 启动备用音频数据生成器
     * 当Visualizer不工作时，基于MediaPlayer播放位置生成模拟音频数据
     */
    private void startBackupAudioDataGenerator() {
        Log.d("BackupAudio", "Starting backup audio data generator");

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying() && visualizerView != null) {
                    try {
                        // 基于当前播放位置生成动态音频数据
                        int currentPosition = mediaPlayer.getCurrentPosition();
                        int duration = mediaPlayer.getDuration();

                        // 创建基于播放进度的动态数据
                        byte[] audioData = generateAudioDataFromPosition(currentPosition, duration);

                        Log.d("BackupAudio", "Generated audio data for position: " + currentPosition + "/" + duration);
                        visualizerView.updateAudioData(audioData);

                    } catch (Exception e) {
                        Log.e("BackupAudio", "Error generating backup audio data", e);
                    }

                    // 继续生成数据，150ms间隔以获得适中的更新速度
                    handler.postDelayed(this, 150);
                } else {
                    Log.d("BackupAudio", "Stopping backup audio data generator");
                }
            }
        }, 150);
    }

    /**
     * 基于播放位置生成音频数据
     */
    private byte[] generateAudioDataFromPosition(int currentPosition, int duration) {
        byte[] data = new byte[1024];

        // 基于播放位置创建变化的波形
        double timeRatio = (double) currentPosition / duration;
        long timeMs = System.currentTimeMillis();

        for (int i = 0; i < data.length; i++) {
            // 组合多个频率创建复杂波形，但变化更缓慢
            double freq1 = Math.sin(timeMs * 0.003 + i * 0.1) * 0.4; // 低频基础，更慢
            double freq2 = Math.sin(timeMs * 0.008 + i * 0.3) * 0.3; // 中频，更慢
            double freq3 = Math.sin(timeMs * 0.015 + i * 0.8) * 0.2; // 高频，稍微慢一点
            double freq4 = Math.sin(timeRatio * Math.PI * 2 + i * 0.2) * 0.1; // 基于播放进度的慢变化

            double amplitude = freq1 + freq2 + freq3 + freq4;
            data[i] = (byte) (amplitude * 127);
        }

        return data;
    }

    /**
     * 测试可视化器数据流
     */
    private void testVisualizerDataFlow() {
        Log.d("VisualizerTest", "Testing visualizer data flow");

        // 创建测试音频数据
        byte[] testData = new byte[1024];
        for (int i = 0; i < testData.length; i++) {
            // 生成正弦波测试数据
            double angle = 2.0 * Math.PI * i / 64; // 频率
            testData[i] = (byte) (Math.sin(angle) * 127);
        }

        // 直接调用音频数据回调来测试数据流
        if (visualizerView != null) {
            Log.d("VisualizerTest", "Sending test data to visualizer view");
            visualizerView.updateAudioData(testData);
        } else {
            Log.w("VisualizerTest", "VisualizerView is null");
        }

        // 每100ms发送一次测试数据，模拟快速音频变化
        handler.postDelayed(() -> {
            if (mediaPlayer != null && mediaPlayer.isPlaying() && visualizerView != null) {
                // 生成更动态的测试数据，模拟音乐节拍
                byte[] rhythmData = new byte[1024];
                long time = System.currentTimeMillis();
                double beatFreq = Math.sin(time * 0.01) * 0.5 + 0.5; // 慢节拍
                double highFreq = Math.sin(time * 0.05) * 0.3 + 0.3; // 快节拍

                for (int i = 0; i < rhythmData.length; i++) {
                    double wave = Math.sin(i * 0.1) * beatFreq + Math.sin(i * 0.3) * highFreq;
                    rhythmData[i] = (byte) (wave * 127);
                }
                Log.d("VisualizerTest", "Sending rhythm test data");
                visualizerView.updateAudioData(rhythmData);

                // 继续测试
                testVisualizerDataFlow();
            }
        }, 100); // 100ms间隔，更快的更新
    }
}
