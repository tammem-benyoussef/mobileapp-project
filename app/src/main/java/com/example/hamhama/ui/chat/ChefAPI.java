package com.example.hamhama.ui.chat;

import android.text.TextUtils;
import android.util.Log;

import com.example.hamhama.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChefAPI {
    private static final String TAG = "ChefAPI";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS)
        .build();

    public interface ApiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public static void askChef(List<ChatMessage> conversation, ApiCallback callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("stream", false);
            payload.put("model", "llama-3.3-70b-versatile");

            JSONArray messages = new JSONArray();

            if (conversation != null) {
                for (ChatMessage message : conversation) {
                    if (message == null || TextUtils.isEmpty(message.getText())) {
                        continue;
                    }

                    messages.put(new JSONObject()
                        .put("role", message.isUserMessage() ? "user" : "assistant")
                        .put("content", message.getText()));
                }
            }

            payload.put("messages", messages);

            RequestBody body = RequestBody.create(payload.toString(), JSON_MEDIA_TYPE);
            Request request = new Request.Builder()
                .url(resolveChatUrl())
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

            CLIENT.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Chat request failed", e);
                    callback.onError("Connection error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (Response safeResponse = response) {
                        String result = safeResponse.body() != null ? safeResponse.body().string() : "";

                        if (!safeResponse.isSuccessful()) {
                            callback.onError(buildHttpError(safeResponse.code(), result));
                            return;
                        }

                        callback.onSuccess(extractReply(result));
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse chat response", e);
                        callback.onError("Parse error: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to start chat request", e);
            callback.onError("Error: " + e.getMessage());
        }
    }

    public static void askChef(String question, ApiCallback callback) {
        askChef(java.util.Collections.singletonList(new ChatMessage(question, true)), callback);
    }

    private static String resolveChatUrl() {
        String baseUrl = BuildConfig.CHATBOT_BASE_URL;
        if (TextUtils.isEmpty(baseUrl)) {
            return "http://10.197.93.154:8000/chat";
        }

        if (baseUrl.endsWith("/")) {
            return baseUrl + "chat";
        }

        return baseUrl + "/chat";
    }

    private static String extractReply(String result) {
        if (TextUtils.isEmpty(result)) {
            return "No response received";
        }

        try {
            JSONObject json = new JSONObject(result);

            String directReply = firstNonEmpty(
                json.optString("reply", null),
                json.optString("answer", null),
                json.optString("response", null),
                json.optString("message", null),
                json.optString("text", null)
            );
            if (!TextUtils.isEmpty(directReply)) {
                return directReply;
            }

            JSONObject data = json.optJSONObject("data");
            if (data != null) {
                String nestedReply = firstNonEmpty(
                    data.optString("reply", null),
                    data.optString("answer", null),
                    data.optString("response", null),
                    data.optString("message", null),
                    data.optString("text", null)
                );
                if (!TextUtils.isEmpty(nestedReply)) {
                    return nestedReply;
                }
            }

            JSONArray choices = json.optJSONArray("choices");
            if (choices != null && choices.length() > 0) {
                JSONObject choice = choices.optJSONObject(0);
                if (choice != null) {
                    JSONObject message = choice.optJSONObject("message");
                    if (message != null) {
                        String content = message.optString("content", null);
                        if (!TextUtils.isEmpty(content)) {
                            return content;
                        }
                    }

                    String text = choice.optString("text", null);
                    if (!TextUtils.isEmpty(text)) {
                        return text;
                    }
                }
            }
        } catch (Exception ignored) {
            // Fall back to raw text below.
        }

        return result;
    }

    private static String buildHttpError(int statusCode, String body) {
        if (TextUtils.isEmpty(body)) {
            return "HTTP " + statusCode;
        }

        String parsedBody = extractReply(body);
        if (TextUtils.isEmpty(parsedBody) || parsedBody.equals(body)) {
            return "HTTP " + statusCode + ": " + body;
        }

        return "HTTP " + statusCode + ": " + parsedBody;
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }

        return null;
    }
}