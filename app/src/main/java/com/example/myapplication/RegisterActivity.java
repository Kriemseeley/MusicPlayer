package com.example.myapplication;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.*;

import java.io.IOException;

public class RegisterActivity extends AppCompatActivity {
    private EditText etNewUsername, etNewPassword, etConfirmPassword;
    private Button btnRegister;

    private static final String REGISTER_URL = "http://192.168.183.1:8888/houduan/register";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, MainActivity.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
        } else {
            startActivity(intent);
        }
        setContentView(R.layout.activity_register);

        etNewUsername = findViewById(R.id.etRegisterUsername);
        etNewPassword = findViewById(R.id.etRegisterPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> {
            String username = etNewUsername.getText().toString().trim();
            String password = etNewPassword.getText().toString().trim();
            String confirm = etConfirmPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirm)) {
                Toast.makeText(this, "两次密码不一致", Toast.LENGTH_SHORT).show();
                return;
            }

            registerUser(username, password);
        });
    }

    private void registerUser(String username, String password) {
        OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build();
        Request request = new Request.Builder()
                .url(REGISTER_URL)
                .post(formBody)
                .build();

        // 打印调试信息
        String debugUrl = REGISTER_URL + "?username=" + username + "&password=" + password;
        System.out.println("Register Debug URL: " + debugUrl);
        // 或者用Log
        android.util.Log.d("RegisterDebug", "Register Debug URL: " + debugUrl);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "网络错误", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resp = response.body().string();
                runOnUiThread(() -> {
                    if (resp.contains("success")) {
                        Toast.makeText(RegisterActivity.this, "注册成功", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, "注册失败：" + resp, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}