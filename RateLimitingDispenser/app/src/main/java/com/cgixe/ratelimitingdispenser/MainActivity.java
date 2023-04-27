package com.cgixe.ratelimitingdispenser;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import org.chromium.net.CronetEngine;
import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";
    private EditText txtIp;
    private EditText txtPin;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnConnect = findViewById(R.id.btnConnect);
        txtIp = findViewById(R.id.txtIp);
        txtPin = findViewById(R.id.txtPin);
        rootView = findViewById(R.id.root);

        btnConnect.setOnClickListener(view -> {
            String ip = txtIp.getText().toString();
            Pattern pattern = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
            Matcher matcher = pattern.matcher(ip);
            if (matcher.find()) {
                Log.i(TAG, "Valid ip entered: " + ip);
                String pin = txtPin.getText().toString();
                if (pin.length() == 0) {
                    Log.i(TAG, "Empty pin entered!");
                    //Toast.makeText(getApplicationContext(), "Please enter the dispenser's PIN!", Toast.LENGTH_LONG).show();
                    Snackbar.make(rootView, "Please enter the dispenser's PIN!", Snackbar.LENGTH_LONG).show();
                } else {
                    checkIp(ip, pin);
                }
            } else {
                Log.i(TAG, "Invalid ip entered: " + ip);
                //Toast.makeText(getApplicationContext(), "Please enter a valid IP address!", Toast.LENGTH_LONG).show();
                Snackbar.make(rootView, "Please enter a valid IP address!", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void checkIp(String ip, String pin) {
        Intent intent = new Intent(MainActivity.this, AdminActivity.class);
        intent.putExtra("ip", "10.137.0.32");
        intent.putExtra("pin", txtPin.getText().toString());
        MainActivity.this.startActivity(intent);
    }
}