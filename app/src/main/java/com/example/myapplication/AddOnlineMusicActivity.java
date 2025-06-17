package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.os.Handler;
import android.os.Looper;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import android.view.View;
public class AddOnlineMusicActivity extends Activity {
    private EditText etKeyword;
    private Button btnSearch;
    private FloatingActionButton btnAdd;
    // private TextView tvResult;

    private List<Integer> hotList = new ArrayList<>();
    private String songName, songer, songPath;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private RecyclerView rvOnlineSongs;
    private OnlineSongAdapter adapter;
    private List<Song> onlineSongs = new ArrayList<>();
    private List<String> singerList = new ArrayList<>();

    private void animateButton(View v) {
        v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(75)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(75).start()).start();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_online_music);

        etKeyword = findViewById(R.id.et_keyword);
        btnSearch = findViewById(R.id.btn_search);
        btnAdd = findViewById(R.id.btn_add_online_song);
        // tvResult = findViewById(R.id.tv_result);
        rvOnlineSongs = findViewById(R.id.rv_online_songs);
        rvOnlineSongs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OnlineSongAdapter(onlineSongs, hotList);
        rvOnlineSongs.setAdapter(adapter);

        // btnSearch.setOnClickListener(v -> {
        // String kw = etKeyword.getText().toString().trim();
        // if (kw.isEmpty()) {
        // Toast.makeText(this, "请输入关键词", Toast.LENGTH_SHORT).show();
        // return;
        // }
        // searchOnlineMusic(kw);
        // });

        // btnAdd.setOnClickListener(v -> {
        // List<Song> selected = adapter.getSelectedSongs();
        // if (selected.isEmpty()) {
        // Toast.makeText(this, "请选择要添加的歌曲", Toast.LENGTH_SHORT).show();
        // return;
        // }
        // // 1. 先给每首歌热度+1
        // for (Song s : selected) {
        // // 用歌名+歌手名定位
        // increaseSongHot(s.getName(), s.getSinger());
        // }
        // new Handler().postDelayed(() -> {
        // String kw = etKeyword.getText().toString().trim();
        // if (!kw.isEmpty()) {
        // searchOnlineMusic(kw); // 重新拉取最新热度
        // }
        // }, 500);
        // // 2. 原有添加逻辑
        // Intent data = new Intent();
        // ArrayList<String> names = new ArrayList<>();
        // ArrayList<String> paths = new ArrayList<>();
        // ArrayList<Integer> durations = new ArrayList<>();
        // for (Song s : selected) {
        // names.add(s.getName());
        // paths.add(s.getFilePath());
        // durations.add(s.getTimeDuration());
        // }
        // data.putStringArrayListExtra("song_names", names);
        // data.putStringArrayListExtra("song_paths", paths);
        // data.putIntegerArrayListExtra("song_durations", durations);
        // setResult(RESULT_OK, data);
        // finish();
        // });
        btnSearch.setOnClickListener(v -> {
            animateButton(v);
            String kw = etKeyword.getText().toString().trim();
            if (kw.isEmpty()) {
                Toast.makeText(this, "请输入关键词", Toast.LENGTH_SHORT).show();
                return;
            }
            searchOnlineMusic(kw);
        });
        btnAdd.setOnClickListener(v -> {
            animateButton(v);
            List<Song> selected = adapter.getSelectedSongs();
            if (selected.isEmpty()) {
                Toast.makeText(this, "请选择要添加的歌曲", Toast.LENGTH_SHORT).show();
                return;
            }
            // 1. 先给每首歌热度+1
            for (Song s : selected) {
                // 用歌名+歌手名定位
                increaseSongHot(s.getName(), s.getSinger());
            }
            new Handler().postDelayed(() -> {
                String kw = etKeyword.getText().toString().trim();
                if (!kw.isEmpty()) {
                    searchOnlineMusic(kw); // 重新拉取最新热度
                }
            }, 500);
            // 2. 原有添加逻辑
            Intent data = new Intent();
            ArrayList<String> names = new ArrayList<>();
            ArrayList<String> paths = new ArrayList<>();
            ArrayList<Integer> durations = new ArrayList<>();
            for (Song s : selected) {
                names.add(s.getName());
                paths.add(s.getFilePath());
                durations.add(s.getTimeDuration());
            }
            data.putStringArrayListExtra("song_names", names);
            data.putStringArrayListExtra("song_paths", paths);
            data.putIntegerArrayListExtra("song_durations", durations);
            setResult(RESULT_OK, data);
            finish();
        });
    }

    private void increaseSongHot(String name, String singer) {
        OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("songname", name)
                .add("songer", singer)
                .build();
        Request request = new Request.Builder()
                .url("http://192.168.183.1:8888/houduan/addcount")
                .post(formBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
            }
        });
    }

    private void searchOnlineMusic(String kw) {
        btnAdd.setEnabled(false);
        onlineSongs.clear();
        hotList.clear();
        adapter.notifyDataSetChanged();
        singerList.clear();
        new Thread(() -> {
            try {
                String urlStr = "http://192.168.183.1:8888/houduan/jiekou?kw=" + URLEncoder.encode(kw, "UTF-8");
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    sb.append(line);
                reader.close();

                String result = sb.toString().trim();
                JSONArray arr = new JSONArray(result);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String name = obj.optString("songname");
                    int timeDuration = obj.optInt("timeduration", 0); // 或 songduration
                    String path = obj.optString("songpath");
                    int platcount = obj.optInt("platcount", 0);
                    String singer = obj.optString("songer", "");

                    Song song = new Song(name, timeDuration, path);
                    onlineSongs.add(song);
                    hotList.add(platcount);
                    singerList.add(singer);
                }
                mainHandler.post(() -> {
                    adapter.notifyDataSetChanged();
                    btnAdd.setEnabled(!onlineSongs.isEmpty());
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    Toast.makeText(this, "搜索失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnAdd.setEnabled(false);
                });
            }
        }).start();
    }
}