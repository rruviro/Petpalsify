package com.doctorblue.petpalsify.Adapter

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.doctorblue.petpalsify.Model.Conversation
import com.doctorblue.petpalsify.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversationAdapter(
    private var conversationList: List<Conversation>,  // List of Conversation objects
    private val currentUserId: String,
) : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val conversation = conversationList[position]

        // Set up to show all messages in the conversation
        holder.messagesLayout.removeAllViews()  // Clear any previous messages (optional if needed)

        for (message in conversation.messages) {
            val messageView = LayoutInflater.from(holder.itemView.context).inflate(R.layout.item_message, null)

            // Set message text
            val messageText = messageView.findViewById<TextView>(R.id.messageText)
            messageText.text = message.text

            // Set message time
            val timeText = messageView.findViewById<TextView>(R.id.timeText)
            timeText.text = SimpleDateFormat("EEE hh:mm a", Locale.getDefault()).format(
                Date(message.timestamp)
            )

            // Check if the message is sent by the current user
            val messageLayout = messageView.findViewById<LinearLayout>(R.id.message_layout)
            if (message.fromId == currentUserId) {
                // Message is sent by the current user
                messageLayout.gravity = Gravity.END  // Align message to the right
                messageText.setTextColor(Color.WHITE)
                messageText.setBackgroundResource(R.drawable.message_background_my) // Set background for my messages
            } else {
                // Message is from another user
                messageLayout.gravity = Gravity.START  // Align message to the left
                messageText.setTextColor(Color.BLACK)
                messageText.setBackgroundResource(R.drawable.message_background_other) // Set background for other messages
            }

            // Add the message view to the layout
            holder.messagesLayout.addView(messageView)
        }
    }

    override fun getItemCount(): Int {
        return conversationList.size
    }

    class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messagesLayout: LinearLayout = itemView.findViewById(R.id.messagesLayout)  // Container for multiple messages
    }

}