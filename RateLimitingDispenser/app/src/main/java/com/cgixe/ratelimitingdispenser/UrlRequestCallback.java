package com.cgixe.ratelimitingdispenser;

import android.util.Log;

import androidx.annotation.Nullable;

import org.chromium.net.CronetException;
import org.chromium.net.UrlRequest;
import org.chromium.net.UrlResponseInfo;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

public class UrlRequestCallback extends UrlRequest.Callback {
    private static final String TAG = "UrlRequestCallback";
    private StringBuilder responseText;
    private UrlRequestListener listener;

    public UrlRequestCallback(UrlRequestListener listener) {
        responseText = new StringBuilder();
        this.listener = listener;
    }

    @Override
    public void onRedirectReceived(UrlRequest request, UrlResponseInfo info, String newLocationUrl) {
        Log.i(TAG, "onRedirectReceived method called.");
        // You should call the request.followRedirect() method to continue
        // processing the request.
        request.followRedirect();
    }

    @Override
    public void onResponseStarted(UrlRequest request, UrlResponseInfo info) {
        Log.i(TAG, "onResponseStarted method called.");
        // You should call the request.read() method before the request can be
        // further processed. The following instruction provides a ByteBuffer object
        // with a capacity of 102400 bytes for the read() method. The same buffer
        // with data is passed to the onReadCompleted() method.
        if (info.getHttpStatusCode() == 200) {
            request.read(ByteBuffer.allocateDirect(102400));
        }
    }

    @Override
    public void onReadCompleted(UrlRequest request, UrlResponseInfo info, ByteBuffer byteBuffer) {
        Log.i(TAG, "onReadCompleted method called.");
        responseText.append(new String(byteBuffer.array()));
        // You should keep reading the request until there's no more data.
        byteBuffer.clear();
        request.read(byteBuffer);
    }

    @Override
    public void onSucceeded(UrlRequest request, UrlResponseInfo info) {
        Log.i(TAG, "onSucceeded method called.");
        try {
            URI uri = new URI(info.getUrl());
            String raw = responseText.toString();
            String resp = raw.substring(raw.indexOf('{'), raw.indexOf('}') + 1);
            listener.requestComplete(uri, resp);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error getting hostname from " + info.getUrl());
            listener.requestFailed(new CronetException("Error getting hostname from " + info.getUrl(), new Exception()) {
                @Override
                public String getMessage() {
                    return "Error getting hostname from " + info.getUrl();
                }
            });
        }
    }

    @Override
    public void onFailed(UrlRequest request, UrlResponseInfo info, CronetException error) {
        Log.e(TAG, "onFailed called");
        Log.e(TAG, error.getMessage());
        listener.requestFailed(error);
    }
}
