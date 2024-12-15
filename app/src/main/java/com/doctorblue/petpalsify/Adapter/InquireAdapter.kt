package com.doctorblue.petpalsify.Adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.doctorblue.petpalsify.Model.Conversation
import com.doctorblue.petpalsify.R
import com.google.android.material.imageview.ShapeableImageView

class InquireAdapter(
    private val conversationInfo: MutableList<Conversation>,
    private val context: Context,
    private val onConversationClick: (String) -> Unit // Pass the recipient ID on click
) : RecyclerView.Adapter<InquireAdapter.InquireViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InquireViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.person_chat, parent, false)
        return InquireViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: InquireViewHolder, position: Int) {
        val conversation = conversationInfo[position]
        holder.bind(conversation)

        // Set item click listener
        holder.itemView.setOnClickListener {
            onConversationClick(conversation.conversationId.toString()) // Use recipientId for the Toast
        }
    }

    override fun getItemCount(): Int = conversationInfo.size

    inner class InquireViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userName: TextView = itemView.findViewById(R.id.userName)
        private val currentChat: TextView = itemView.findViewById(R.id.currentChat)
        private val time: TextView = itemView.findViewById(R.id.time)
        private val profileImage: ShapeableImageView = itemView.findViewById(R.id.profileImage)

        fun bind(conversation: Conversation) {
            // Check if userInfo is available, and concatenate firstname and lastname
            val fullName = conversation.userInfo?.let {
                "${it.firstname} ${it.lastname}"
            } ?: "Unknown" // Default to "Unknown" if userInfo is null

            userName.text = fullName
            currentChat.text = conversation.messages.lastOrNull()?.text ?: "No message"

            // Get timestamp for the last message and format it
            val timestamp = conversation.messages.lastOrNull()?.timestamp ?: System.currentTimeMillis()
            time.text = formatTimestamp(timestamp)

            // Check if petImage is not null and load it
            conversation.userInfo?.profileImage?.let { imageBase64 ->
                // Decode the Base64 string into a Bitmap
                try {
                    val decodedString: ByteArray = Base64.decode(imageBase64, Base64.DEFAULT)  // Corrected here
                    val decodedBitmap: Bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

                    // Load the decoded Bitmap into the ImageView using Glide
                    Glide.with(context)
                        .load(decodedBitmap)  // Use Bitmap for local image
                        .into(profileImage)
                } catch (e: Exception) {
                    // Optionally, log or show a Toast
                }
            }

        }

        // Update the format to display the timestamp like "THU 10:10 PM"
        private fun formatTimestamp(timestamp: Long): String {
            val date = java.util.Date(timestamp)
            val sdf = java.text.SimpleDateFormat("EEE hh:mm a", java.util.Locale.getDefault()) // EEE is day abbreviation (e.g., "Thu")
            return sdf.format(date)
        }
    }

}
