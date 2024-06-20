package com.maxvision.jmdns;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * user: zjj
 * date: 2024/6/19
 * desc: http请求类
 */
public class HttpRequestQueueManager {
    private static final String TAG = "zjj_mdns";
    private static final int MAX_RETRY_COUNT = 3;
    private static final int REQUEST_INTERVAL_MS = 1000;  // 请求间隔时间，单位为毫秒

    private final BlockingQueue<RequestWrapper> requestQueue;
    private final OkHttpClient httpClient;
    private final AtomicInteger successCount;
    private final AtomicInteger failureCount;
    private Thread processingThread;
    private volatile boolean isProcessing;  // 用于指示是否正在处理请求

    public HttpRequestQueueManager() {
        requestQueue = new LinkedBlockingQueue<>();
        httpClient = new OkHttpClient();
        successCount = new AtomicInteger(0);
        failureCount = new AtomicInteger(0);
        startProcessing();
    }

    public synchronized void addRequest(String url, HttpResponseCallback callback) {
        requestQueue.add(new RequestWrapper(url, callback, 0));
        Log.d(TAG, "Added request to queue: " + url);
        if (!isProcessing) {
            startProcessing();
        }
    }

    private synchronized void startProcessing() {
        isProcessing = true;
        processingThread = new Thread(() -> {
            while (isProcessing) {
                try {
                    RequestWrapper requestWrapper = requestQueue.take();
                    executeHttpRequest(requestWrapper);
//                    Thread.sleep(REQUEST_INTERVAL_MS);  // 增加请求间隔
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    isProcessing = false;
                }
            }
        });
        processingThread.start();
    }

    private void executeHttpRequest(RequestWrapper requestWrapper) {
        long time = System.currentTimeMillis();
        RequestBody formBody = new FormBody.Builder()
                .add("requestId", String.valueOf(time))
                .add("timestamp", String.valueOf(time))
                .build();
        Request request = new Request.Builder()
                .url(requestWrapper.url)
                .post(formBody)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            successCount.incrementAndGet();
            Log.d(TAG, "Request succeeded: " + requestWrapper.url );
            if (requestWrapper.callback != null && response.body() != null) {
                requestWrapper.callback.onResponse(response.body().string());
            }
        } catch (Exception e) {
            Log.e(TAG, "Request failed: " + requestWrapper.url, e);
            if (requestWrapper.retryCount < MAX_RETRY_COUNT) {
                requestWrapper.retryCount++;
                Log.d(TAG, "Retrying request: " + requestWrapper.url + " (Retry count: " + requestWrapper.retryCount + ")");
                requestQueue.add(requestWrapper);  // Re-add request to queue for retry
            } else {
                failureCount.incrementAndGet();
                if (requestWrapper.callback != null) {
                    requestWrapper.callback.onFailure(e);
                }
            }
        }
        Log.d(TAG, "Total success: " + successCount.get() + ", Total failure: " + failureCount.get());
    }

    public synchronized void stop() {
        isProcessing = false;
        if (processingThread != null) {
            processingThread.interrupt();
            successCount.set(0);
            failureCount.set(0);
        }
    }

    private static class RequestWrapper {
        String url;
        HttpResponseCallback callback;
        int retryCount;

        RequestWrapper(String url, HttpResponseCallback callback, int retryCount) {
            this.url = url;
            this.callback = callback;
            this.retryCount = retryCount;
        }
    }

    public interface HttpResponseCallback {
        void onResponse(String response);
        void onFailure(Exception e);
    }

}
