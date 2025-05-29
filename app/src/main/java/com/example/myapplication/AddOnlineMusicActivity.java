package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import android.os.Handler;
import android.os.Looper;

public class AddOnlineMusicActivity extends Activity {
    private EditText etKeyword;
    private Button btnSearch, btnAdd;
    private TextView tvResult;

    private String songName, songer, songPath;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_online_music);

        etKeyword = findViewById(R.id.et_keyword);
        btnSearch = findViewById(R.id.btn_search);
        btnAdd = findViewById(R.id.btn_add_online_song);
        tvResult = findViewById(R.id.tv_result);

        btnSearch.setOnClickListener(v -> {
            String kw = etKeyword.getText().toString().trim();
            if (kw.isEmpty()) {
                Toast.makeText(this, "请输入关键词", Toast.LENGTH_SHORT).show();
                return;
            }
            searchOnlineMusic(kw);
        });

        btnAdd.setOnClickListener(v -> {
            if (songName != null && songPath != null) {
                // 通过Intent返回数据给MainActivity
                Intent data = new Intent();
                data.putExtra("song_name", songName);
                data.putExtra("song_path", songPath);
                data.putExtra("song_duration", 0); // 在线歌曲可不知时长，设为0或后续可扩展
                setResult(RESULT_OK, data);
                finish();
            }
        });
    }
//搜索在线歌曲
    private void searchOnlineMusic(String kw) {
        btnAdd.setEnabled(false);
        tvResult.setText("正在搜索...");
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
                //解析json数据
                String result = sb.toString().trim();
                if (result.startsWith("[")) {
                    // 返回的是JSONArray
                    JSONArray arr = new JSONArray(result);
                    if (arr.length() > 0) {
                        JSONObject obj = arr.getJSONObject(0); // 取第一个
                        songName = obj.optString("songname");
                        songer = obj.optString("songer");
                        songPath = obj.optString("songpath");
                        mainHandler.post(() -> {
                            tvResult.setText("歌名: " + songName + "\n歌手: " + songer + "\n地址: " + songPath);
                            btnAdd.setEnabled(true);
                        });
                    } else {
                        mainHandler.post(() -> {
                            tvResult.setText("未搜索到结果");
                            btnAdd.setEnabled(false);
                        });
                    }
                } else {
                    // 返回的是JSONObject
                    JSONObject obj = new JSONObject(result);
                    songName = obj.optString("songname");
                    songer = obj.optString("songer");
                    songPath = obj.optString("songpath");
                    mainHandler.post(() -> {
                        tvResult.setText("歌名: " + songName + "\n歌手: " + songer + "\n地址: " + songPath);
                        btnAdd.setEnabled(true);
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    tvResult.setText("搜索失败: " + e.getMessage());
                    btnAdd.setEnabled(false);
                });
            }
        }).start();
    }
}