package com.cgixe.ratelimitingdispenser;

import org.chromium.net.CronetException;

import java.net.URI;

public interface UrlRequestListener {
    /**
     * Called when the request has succeeded.
     * @param requestUri The URI of the request.
     * @param responseText A string containing the HTTP response text.
     */
    void requestComplete(URI requestUri, String responseText);

    /**
     * Called if the request failed.
     * @param exception The exception describing the failure.
     */
    void requestFailed(CronetException exception);
}
