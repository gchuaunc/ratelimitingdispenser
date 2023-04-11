package com.cgixe.ratelimitingdispenser;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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

public class AdminActivity extends AppCompatActivity implements UrlRequestListener {

    private TextView ipTextView;
    private TextView curCandiesTextView;
    private EditText setChargeEditText;
    private View rootView;
    private String ip;
    private final String TAG = "AdminActivity";
    private CronetEngine cronetEngine;
    private String pin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        Intent intent = getIntent();
        ip = intent.getStringExtra("ip");
        pin = intent.getStringExtra("pin");

        ipTextView = findViewById(R.id.txtTitleIp);
        ipTextView.setText("Rate Limiting Dispenser at " + ip);

        curCandiesTextView = findViewById(R.id.txtCurCandies);

        Button dispenseBtn = findViewById(R.id.btnDispense);
        dispenseBtn.setOnClickListener(view -> {
            Log.i(TAG, "Force dispensing candy");
            forceDispense();
        });

        setChargeEditText = findViewById(R.id.editSetCharge);
        rootView = findViewById(R.id.root);
        Button setBtn = findViewById(R.id.btnSet);

        setBtn.setOnClickListener(view -> {
            try {
                int newCharge = Integer.parseInt(setChargeEditText.getText().toString());
                newCharge = Math.max(0, newCharge);
                newCharge = Math.min(3, newCharge);
                Log.i(TAG,"Setting charge to " + newCharge);
                setCharge(newCharge);
            } catch (NumberFormatException ex) {
                Log.e(TAG, "Invalid new charge set: " + setChargeEditText.getText().toString());
                //Toast.makeText(getApplicationContext(), "Please enter a valid charge (0-3)!", Toast.LENGTH_LONG).show();
                Snackbar.make(rootView, "Please enter a valid charge (0-3)!", Snackbar.LENGTH_LONG).show();
            }
        });

        updateCandies();
    }

    private void forceDispense() {
        if (cronetEngine == null) {
            CronetEngine.Builder myBuilder = new CronetEngine.Builder(this);
            cronetEngine = myBuilder.build();
        }
        Executor executor = Executors.newSingleThreadExecutor();
        UrlRequest.Builder requestBuilder = cronetEngine.newUrlRequestBuilder(
                "http://" + ip + "/" + pin + "?disp", new UrlRequestCallback(this), executor);
        UrlRequest request = requestBuilder.build();
        request.start();
    }

    private void setCharge(int amount) {
        if (cronetEngine == null) {
            CronetEngine.Builder myBuilder = new CronetEngine.Builder(this);
            cronetEngine = myBuilder.build();
        }
        Executor executor = Executors.newSingleThreadExecutor();
        UrlRequest.Builder requestBuilder = cronetEngine.newUrlRequestBuilder(
                "http://" + ip + "/" + pin + "?set=" + amount, new UrlRequestCallback(this), executor);
        UrlRequest request = requestBuilder.build();
        request.start();
    }

    private void updateCandies() {
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
        String path = requestUri.getPath();
        String query = requestUri.getQuery();
        //Log.i(TAG, "Request is complete path=" + path + ", query=" + query);
        try {
            JSONObject obj = new JSONObject(responseText);
            if (path.equals("/" + pin)) {
                if (query == null) {
                    // GET /pin
                    int candies = obj.getInt("status");
                    curCandiesTextView.setText(candies + "/3 Candies");
                    Log.i(TAG, "Updating candy display to " + candies);
                } else if (query.equals("disp")) {
                    // GET /pin?disp
                    String res = obj.getString("status");
                    if (res.equals("success")) {
                        Log.i(TAG, "Dispensed successfully");
                        Snackbar.make(rootView, "Successfully dispensed!", Snackbar.LENGTH_LONG).show();
                    } else {
                        Log.w(TAG, "Did not dispense successfully: " + res);
                        Snackbar.make(rootView, "Failed to dispense", Snackbar.LENGTH_LONG).show();
                    }
                    updateCandies();
                } else if (query.contains("set=")) {
                    // GET /pin?set=[val]
                    String res = obj.getString("status");
                    if (res.equals("success")) {
                        Log.i(TAG, "Successfully set charge");
                        Snackbar.make(rootView, "The charge was updated successfully!", Snackbar.LENGTH_LONG).show();
                    } else {
                        Log.w(TAG, "Did not set charge successfully: " + res);
                        Snackbar.make(rootView, "Failed to update charge", Snackbar.LENGTH_LONG).show();
                    }
                    updateCandies();
                }
            }
        } catch (JSONException ex) {
            Log.e(TAG, "Error parsing JSON from response: " + responseText);
            Snackbar.make(rootView, "Failed to parse JSON, this may not be the dispenser's address.", Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void requestFailed(CronetException exception) {
        Log.e(TAG, "HTTP request failed: " + exception.getMessage());
        Snackbar.make(rootView, "Failed to connect to dispenser: " + exception.getMessage(), Snackbar.LENGTH_LONG).show();
    }
}