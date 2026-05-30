package com.example.hamhama.ui.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hamhama.R;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ProgressBar loadingIndicator;
    private ChatMessageAdapter messageAdapter;
    private List<ChatMessage> messages;
    private static final String KEY_SCROLL_POSITION = "scroll_position";
    private static final String KEY_MESSAGES = "messages";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initViews();
        setupRecyclerView();
        setupSendButton();

        // Restore messages from saved state if rotation occurred
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_MESSAGES)) {
            messages = (List<ChatMessage>) savedInstanceState.getSerializable(KEY_MESSAGES);
            messageAdapter.notifyDataSetChanged();
            // Restore scroll position
            int scrollPosition = savedInstanceState.getInt(KEY_SCROLL_POSITION, 0);
            if (scrollPosition > 0) {
                chatRecyclerView.post(() -> chatRecyclerView.scrollToPosition(scrollPosition));
            }
        }
    }

    private void initViews() {
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
        loadingIndicator = findViewById(R.id.loading_indicator);
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setTitle("Chat");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setupRecyclerView() {
        messages = new ArrayList<>();
        messageAdapter = new ChatMessageAdapter(messages);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(messageAdapter);
    }

    private void setupSendButton() {
        sendButton.setOnClickListener(v -> sendMessage());
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        
        if (TextUtils.isEmpty(message)) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        sendButton.setEnabled(false);

        // Add user message to chat
        messages.add(new ChatMessage(message, true));
        messageAdapter.notifyItemInserted(messages.size() - 1);
        chatRecyclerView.scrollToPosition(messages.size() - 1);
        
        messageInput.setText("");
        loadingIndicator.setVisibility(android.view.View.VISIBLE);

        ChefAPI.askChef(new ArrayList<>(messages), new ChefAPI.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    loadingIndicator.setVisibility(android.view.View.GONE);
                    sendButton.setEnabled(true);
                    messages.add(new ChatMessage(response, false));
                    messageAdapter.notifyItemInserted(messages.size() - 1);
                    chatRecyclerView.scrollToPosition(messages.size() - 1);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    loadingIndicator.setVisibility(android.view.View.GONE);
                    sendButton.setEnabled(true);
                    messages.add(new ChatMessage("Error: " + error, false));
                    messageAdapter.notifyItemInserted(messages.size() - 1);
                    chatRecyclerView.scrollToPosition(messages.size() - 1);
                });
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save current scroll position
        if (chatRecyclerView != null && chatRecyclerView.getLayoutManager() != null) {
            int scrollPosition = ((LinearLayoutManager) chatRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
            outState.putInt(KEY_SCROLL_POSITION, scrollPosition);
        }
        // Save messages list
        if (messages != null) {
            outState.putSerializable(KEY_MESSAGES, (ArrayList<ChatMessage>) messages);
        }
    }
}
