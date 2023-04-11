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

public class MainActivity extends AppCompatActivity implements UrlRequestListener {

    private final String TAG = "MainActivity";
    private EditText txtIp;
    private EditText txtPin;
    private View rootView;
    private CronetEngine cronetEngine;

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
        if (cronetEngine == null) {
            CronetEngine.Builder myBuilder = new CronetEngine.Builder(this);
            cronetEngine = myBuilder.build();
        }
        Executor executor = Executors.newSingleThreadExecutor();
        UrlRequest.Builder requestBuilder = cronetEngine.newUrlRequestBuilder(
                "http://" + ip + "/" + pin, new UrlRequestCallback(this), executor);
        UrlRequest request = requestBuilder.build();
        request.start();
    }

    @Override
    public void requestComplete(URI requestUri, String responseText) {
        Log.i(TAG, "Response: " + responseText);
        try {
            JSONObject obj = new JSONObject(responseText);
            String status = obj.getString("status");
            Log.i(TAG, "Status is " + status);
            if (!status.equals("404")) {
                Intent intent = new Intent(MainActivity.this, AdminActivity.class);
                intent.putExtra("ip", requestUri.getHost());
                intent.putExtra("pin", txtPin.getText().toString());
                MainActivity.this.startActivity(intent);
            } else {
                Log.e(TAG, "Status was 404, aborting!");
                Snackbar.make(rootView, "Incorrect PIN for dispenser!", Snackbar.LENGTH_LONG).show();
            }
        } catch (JSONException ex) {
            Log.e(TAG, "Failed to parse JSON: " + responseText);
            Snackbar.make(rootView, "Failed to parse JSON, this may not be the dispenser's address.", Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void requestFailed(CronetException exception) {
        Log.i(TAG,"Request failed! " + exception.getMessage());
        Snackbar.make(rootView, "Failed to connect to dispenser: " + exception.getMessage(), Snackbar.LENGTH_LONG).show();
    }
}