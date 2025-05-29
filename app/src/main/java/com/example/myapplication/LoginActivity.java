package com.example.myapplication;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin, btnToRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ 判断是否已登录，直接进入主界面
        SharedPreferences sp = getSharedPreferences("user_info", MODE_PRIVATE);
        if (sp.getBoolean("isLogin", false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etLoginUsername);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnToRegister = findViewById(R.id.btnToRegister);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString();
            String password = etPassword.getText().toString();

            // 从文件中读取账号密码
            try {
                FileInputStream fis = openFileInput("user.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
                String line;
                boolean match = false;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts[0].equals(username) && parts[1].equals(password)) {
                        match = true;
                        break;
                    }
                }
                reader.close();

                if (match) {
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putBoolean("isLogin", true);
                    editor.putString("username", username);
                    editor.apply();

                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(this, "账号或密码错误", Toast.LENGTH_SHORT).show();
                }

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "读取文件失败", Toast.LENGTH_SHORT).show();
            }
        });

        btnToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }
}
