package com.doctorblue.petpalsify.Model

data class Conversation(
    var conversationId: String? = null, // Conversation ID
    var messages: List<Message> = emptyList(),
    val userInfo: PersonalInfo? = null // Adding userInfo to store PersonalInfo
) {
    constructor() : this(null, emptyList())  // Default constructor for Firebase
}

data class Message(
    val fromId: String = "",  // Sender's user ID
    val toId: String = "",    // Recipient's user ID
    val text: String = "",    // Message text
    val timestamp: Long = 0L, // Time of message
    val told: Boolean = false, // To track if the message is seen
    val id: String? = null    // Unique ID for the message
)