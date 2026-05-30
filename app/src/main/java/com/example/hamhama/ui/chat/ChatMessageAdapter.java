package com.example.hamhama.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hamhama.R;

import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    private List<ChatMessage> messages;

    public ChatMessageAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageText;
        private View messageBubble;
        private LinearLayout messageContainer;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            messageBubble = itemView.findViewById(R.id.message_bubble);
            messageContainer = itemView.findViewById(R.id.message_container);
        }

        void bind(ChatMessage message) {
            messageText.setText(message.getText());
            
            LinearLayout.LayoutParams params = 
                (LinearLayout.LayoutParams) messageContainer.getLayoutParams();
            
            if (message.isUserMessage()) {
                // User message - light blue, right aligned
                messageBubble.setBackgroundResource(R.drawable.bubble_user);
                messageText.setTextColor(itemView.getContext().getColor(R.color.black));
                params.gravity = android.view.Gravity.END;
            } else {
                // Bot message - orange, left aligned
                messageBubble.setBackgroundResource(R.drawable.bubble_bot);
                messageText.setTextColor(itemView.getContext().getColor(R.color.black));
                params.gravity = android.view.Gravity.START;
            }
            
            messageContainer.setLayoutParams(params);
        }
    }
}
