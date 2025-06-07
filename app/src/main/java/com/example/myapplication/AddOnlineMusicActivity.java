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
import java.util.List;

import android.os.Handler;
import android.os.Looper;

public class AddOnlineMusicActivity extends Activity {
    private EditText etKeyword;
    private Button btnSearch, btnAdd;
    private TextView tvResult;

    private String songName, songer, songPath;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private RecyclerView rvOnlineSongs;
    private OnlineSongAdapter adapter;
    private List<Song> onlineSongs = new ArrayList<>();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_online_music);

        etKeyword = findViewById(R.id.et_keyword);
        btnSearch = findViewById(R.id.btn_search);
        btnAdd = findViewById(R.id.btn_add_online_song);
        tvResult = findViewById(R.id.tv_result);
        rvOnlineSongs = findViewById(R.id.rv_online_songs);
        rvOnlineSongs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OnlineSongAdapter(onlineSongs);
        rvOnlineSongs.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> {
            String kw = etKeyword.getText().toString().trim();
            if (kw.isEmpty()) {
                Toast.makeText(this, "请输入关键词", Toast.LENGTH_SHORT).show();
                return;
            }
            searchOnlineMusic(kw);
        });

        btnAdd.setOnClickListener(v -> {
            List<Song> selected = adapter.getSelectedSongs();
            if (selected.isEmpty()) {
                Toast.makeText(this, "请选择要添加的歌曲", Toast.LENGTH_SHORT).show();
                return;
            }
            // 只传递选中的歌曲列表回去
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
//搜索在线歌曲
    private void searchOnlineMusic(String kw) {
        btnAdd.setEnabled(false);
        tvResult.setText("正在搜索...");
        onlineSongs.clear();
        adapter.notifyDataSetChanged();
        new Thread(() -> {
        //     try {
        //         String urlStr = "http://192.168.183.1:8888/houduan/jiekou?kw=" + URLEncoder.encode(kw, "UTF-8");
        //         URL url = new URL(urlStr);
        //         HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        //         conn.setConnectTimeout(3000);
        //         conn.setRequestMethod("GET");
        //         BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        //         StringBuilder sb = new StringBuilder();
        //         String line;
        //         while ((line = reader.readLine()) != null)
        //             sb.append(line);
        //         reader.close();
        //         //解析json数据
        //         String result = sb.toString().trim();
        //         if (result.startsWith("[")) {
        //             // 返回的是JSONArray
        //             JSONArray arr = new JSONArray(result);
        //             if (arr.length() > 0) {
        //                 JSONObject obj = arr.getJSONObject(0); // 取第一个
        //                 songName = obj.optString("songname");
        //                 songer = obj.optString("songer");
        //                 songPath = obj.optString("songpath");
        //                 mainHandler.post(() -> {
        //                     tvResult.setText("歌名: " + songName + "\n歌手: " + songer + "\n地址: " + songPath);
        //                     btnAdd.setEnabled(true);
        //                 });
        //             } else {
        //                 mainHandler.post(() -> {
        //                     tvResult.setText("未搜索到结果");
        //                     btnAdd.setEnabled(false);
        //                 });
        //             }
        //         } else {
        //             // 返回的是JSONObject
        //             JSONObject obj = new JSONObject(result);
        //             songName = obj.optString("songname");
        //             songer = obj.optString("songer");
        //             songPath = obj.optString("songpath");
        //             mainHandler.post(() -> {
        //                 tvResult.setText("歌名: " + songName + "\n歌手: " + songer + "\n地址: " + songPath);
        //                 btnAdd.setEnabled(true);
        //             });
        //         }
        //     } catch (Exception e) {
        //         mainHandler.post(() -> {
        //             tvResult.setText("搜索失败: " + e.getMessage());
        //             btnAdd.setEnabled(false);
        //         });
        //     }
        // }).start();
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
                JSONArray arr = result.startsWith("[") ? new JSONArray(result) : new JSONArray("[" + result + "]");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String name = obj.optString("songname");
                    String singer = obj.optString("songer");
                    String path = obj.optString("songpath");
                    onlineSongs.add(new Song(name, 0, path));
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