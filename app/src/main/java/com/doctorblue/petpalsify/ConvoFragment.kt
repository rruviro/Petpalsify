package com.doctorblue.petpalsify

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.doctorblue.petpalsify.databinding.FragmentConvoBinding
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.LinearLayoutManager
import com.doctorblue.petpalsify.Adapter.ConversationAdapter
import com.doctorblue.petpalsify.Model.Conversation
import com.doctorblue.petpalsify.Model.Message
import com.doctorblue.petpalsify.Model.PersonalInfo
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.database.FirebaseDatabase

class ConvoFragment : Fragment() {

    private lateinit var binding: FragmentConvoBinding
    private var defaultConversationHeight: Int = 0
    private var lastKeyboardState: Boolean = false
    private var isAnimating = false

    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var conversationList: MutableList<Conversation> // Define as a class-level variable

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentConvoBinding.inflate(inflater, container, false)

        val userPetId = requireArguments().getString("userPetId").toString()
        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", "No ID Found").toString()
        hideSystemUI()

        binding.back.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.message.setOnClickListener {
            val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(binding.message, InputMethodManager.SHOW_IMPLICIT)
        }

        binding.info.setOnClickListener {
            personalInfoDialog(userPetId, userId)
        }

        binding.conversation.viewTreeObserver.addOnGlobalLayoutListener {
            if (defaultConversationHeight == 0) {
                defaultConversationHeight = binding.conversation.height
            }
        }

        binding.root.viewTreeObserver.addOnPreDrawListener {
            val rect = Rect()
            binding.root.getWindowVisibleDisplayFrame(rect)
            val screenHeight = binding.root.height
            val keypadHeight = screenHeight - rect.bottom

            val isKeyboardVisible = keypadHeight > 0
            if (isKeyboardVisible != lastKeyboardState) {
                updateConversationLayout(isKeyboardVisible, keypadHeight, screenHeight)
                lastKeyboardState = isKeyboardVisible
            }

            true
        }

        binding.message.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                animateConversationHeight(defaultConversationHeight)
                true
            } else {
                false
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(context, "Back navigation is disabled", Toast.LENGTH_SHORT).show()
            }
        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the userPetId (Recipient user ID)
        val userPetId = requireArguments().getString("userPetId").toString()
        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val currentUserId = sharedPreferences.getString("userId", "") ?: ""

        // Initialize the conversation list and adapter
        conversationList = mutableListOf()
        conversationAdapter = ConversationAdapter(conversationList, currentUserId)

        // Set up RecyclerView
        binding.convoRecycleView.layoutManager = LinearLayoutManager(requireContext())
        binding.convoRecycleView.adapter = conversationAdapter

        // Fetch conversations from Firebase
        val recipientUserId = userPetId
        fetchConversations(currentUserId, recipientUserId)
        fetchUserInfo(
            userId = userPetId,
            callback = { _ ->
                // Use userInfo to update UI (e.g., show recipient's name or profile picture)
            }
        )
        // Set up send button click listener
        binding.send.setOnClickListener {
            val messageText = binding.message.text.toString().trim()
            if (messageText.isNotEmpty()) {

                val timestamp = System.currentTimeMillis()

                val message = Message(
                    fromId = currentUserId,
                    toId = recipientUserId,
                    text = messageText,
                    timestamp = timestamp,  // Use current timestamp
                    told = false
                )

                // Insert the message into Firebase
                insertMessage(message, currentUserId, recipientUserId)

                // Add the new message to the conversation list and notify the adapter
                val newConversation = Conversation(
                    conversationId = getUniqueConversationId(currentUserId, recipientUserId),
                    messages = listOf(message)
                )
                conversationList.add(newConversation)
                conversationAdapter.notifyItemInserted(conversationList.size - 1)

                // Clear the input field after sending
                binding.message.text.clear()
            }
        }

    }

    private fun insertMessage(message: Message, currentUserId: String, recipientUserId: String) {
        val database = FirebaseDatabase.getInstance().reference

        // Get the unique conversation ID between the two users
        val conversationId = getUniqueConversationId(currentUserId, recipientUserId)

        // Generate a unique message ID for this message
        val messageId = database.child("conversations")
            .child(currentUserId)
            .child(recipientUserId)
            .child("messages")
            .child("messagesInfo")
            .push()
            .key

        // Ensure messageId is not null before proceeding
        if (messageId != null) {
            // Prepare the message data
            val messageData = mapOf(
                "fromId" to currentUserId,
                "toId" to recipientUserId,
                "text" to message.text,
                "timestamp" to System.currentTimeMillis() // Adding a timestamp
            )

            // Store the message for the sender (currentUserId)
            database.child("conversations")
                .child(currentUserId)
                .child(recipientUserId)
                .child("messages")
                .child("messagesInfo")
                .child(messageId)
                .setValue(messageData)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Now store the same message for the recipient (recipientUserId)
                        database.child("conversations")
                            .child(recipientUserId)
                            .child(currentUserId)  // Store under recipient's conversation path
                            .child("messages")
                            .child("messagesInfo")
                            .child(messageId)
                            .setValue(messageData)
                            .addOnCompleteListener { recipientTask ->
                                if (recipientTask.isSuccessful) {
                                    Toast.makeText(requireContext(), "Message Sent", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(requireContext(), "Error saving message for recipient", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        Toast.makeText(requireContext(), "Error saving message", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            // If messageId is null, show an error message
            Toast.makeText(requireContext(), "Error generating message ID", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getUniqueConversationId(userId1: String, userId2: String): String {
        val sortedUserIds = listOf(userId1, userId2).sorted()
        return sortedUserIds.joinToString("-")
    }

    private fun fetchConversations(userId: String, recipientUserId: String) {
        val database = FirebaseDatabase.getInstance().reference
        val userConversationsRef = database.child("conversations").child(recipientUserId)

        // Get only conversations where userId is part of the conversation
        userConversationsRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val updatedConversations = mutableListOf<Conversation>()

                // Iterate through all conversation IDs
                for (conversationSnapshot in snapshot.children) {
                    val conversationId = conversationSnapshot.key ?: continue

                    // Ensure the conversationId contains the current userId (both user1_user2 and user2_user1 should be valid)
                    val participants = conversationId.split("_")
                    Log.d("ConversationFragment", "Conversation ID: $conversationId, Participants: $participants")

                    if (conversationId.contains(userId)) {
                        val messages = mutableListOf<Message>()
                        val messagesInfo = conversationSnapshot.child("messages").child("messagesInfo")

                        // Fetch all messages for this conversation
                        for (messageSnapshot in messagesInfo.children) {
                            val message = messageSnapshot.getValue(Message::class.java)
                            if (message != null) {
                                messages.add(message)
                            }
                        }

                        // Only add the conversation if there are messages
                        if (messages.isNotEmpty()) {
                            updatedConversations.add(Conversation(conversationId = conversationId, messages = messages))
                        }
                    }
                }

                // Update the conversation list and notify the adapter
                Log.d("ConversationFragment", "Conversations fetched: ${updatedConversations.size}")
                updateConversations(updatedConversations)
            }
        }.addOnFailureListener { exception ->
            Log.e("ConversationFragment", "Failed to fetch conversations", exception)
            Toast.makeText(requireContext(), "Failed to fetch conversations: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateConversations(newConversations: List<Conversation>) {
        // Clear existing data and add new conversations
        conversationList.clear()
        conversationList.addAll(newConversations)
        Log.d("ConversationFragment", "Updating conversation list, current size: ${conversationList.size}")

        // Notify adapter of the data change
        conversationAdapter.notifyDataSetChanged()
    }

    private fun hideSystemUI() {
        requireActivity().window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    private fun updateConversationLayout(isKeyboardVisible: Boolean, keypadHeight: Int, screenHeight: Int) {
        val targetHeight = if (isKeyboardVisible) {
            screenHeight - keypadHeight - 100
        } else {
            defaultConversationHeight
        }

        if (!isAnimating && binding.conversation.layoutParams.height != targetHeight) {
            animateConversationHeight(targetHeight)
        }
    }

    private fun animateConversationHeight(targetHeight: Int) {
        isAnimating = true

        val animator = ValueAnimator.ofInt(binding.conversation.height, targetHeight)
        animator.addUpdateListener { valueAnimator ->
            val animatedHeight = valueAnimator.animatedValue as Int
            val params = binding.conversation.layoutParams
            params.height = animatedHeight
            binding.conversation.layoutParams = params
        }

        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                isAnimating = false
            }
        })

        animator.duration = 300
        animator.start()
    }

    private fun hideKeyboard() {
        val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(binding.message.windowToken, 0)
    }

    private fun resetConversationLayoutHeight() {
        hideKeyboard()

        binding.root.postDelayed({
            animateConversationHeight(defaultConversationHeight)
        }, 200)
    }

    override fun onStart() {
        super.onStart()
        hideSystemUI()
    }

    override fun onStop() {
        super.onStop()
        resetConversationLayoutHeight()
    }

    private fun fetchUserInfo(userId: String, callback: (PersonalInfo?) -> Unit) {
        val database = FirebaseDatabase.getInstance().reference
        val userRef = database.child("users").child(userId) // Assuming users are stored under "users"

        userRef.get().addOnSuccessListener { snapshot ->
            val userInfo = snapshot.getValue(PersonalInfo::class.java)
            binding.userName.text = "${userInfo?.firstname} ${userInfo?.lastname}"
            callback(userInfo)
        }.addOnFailureListener { exception ->
            Log.e("ConversationFragment", "Failed to fetch user info for userId: $userId", exception)
            callback(null)
        }
    }

    private fun personalInfoDialog(userPetId: String, userId: String) {
        // Inflate the custom layout for the dialog
        val dialogView = layoutInflater.inflate(R.layout.person_info, null)

        // Create the dialog
        val dialog = AlertDialog.Builder(requireContext(), R.style.TransparentDialog)
            .setView(dialogView)
            .create()

        // Initialize Firebase reference
        val database = FirebaseDatabase.getInstance().reference
        val userRef = database.child("users").child(userPetId)

        // Access views inside dialogView
        val userNameTextView = dialogView.findViewById<TextView>(R.id.userName)
        val closeTextView = dialogView.findViewById<TextView>(R.id.close)
        val category = dialogView.findViewById<TextView>(R.id.category)

        // Fetch user data from Firebase
        userRef.get()
            .addOnSuccessListener { snapshot ->
                val userInfo = snapshot.getValue(PersonalInfo::class.java)
                userNameTextView.text = "${userInfo?.firstname.orEmpty()} ${userInfo?.lastname.orEmpty()}"

                // Handle profile image (if available)
                userInfo?.profileImage?.let { base64Image ->
                    // If profileImage is in Base64, decode it
                    val decodedString: ByteArray = Base64.decode(base64Image, Base64.DEFAULT)
                    val decodedBitmap: Bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                    dialog.findViewById<ShapeableImageView>(R.id.profileImage).setImageBitmap(decodedBitmap)
                }

                if (userInfo!!.category != null) {
                    category.text = userInfo.category
                } else {
                    category.text = "UNKNOWN"
                }

            }
            .addOnFailureListener { exception ->
                Log.e("ConvoFragment", "Failed to fetch user info for userId: $userPetId", exception)
                userNameTextView.text = "Error fetching user info"
            }

        // Set close button listener
        closeTextView.setOnClickListener {
            dialog.dismiss()
        }

        val databaseRefCount = FirebaseDatabase.getInstance().reference
            .child("pet-profile")
            .child(userId)

        // Fetch the petId count for the specific user
        databaseRefCount.get()
            .addOnSuccessListener { snapshot ->

                if (snapshot.exists()) {
                    val petCount = snapshot.childrenCount // Count the number of petId entries (child nodes)
                    Log.d("ProfileFragment", "Pet count for userId $userId: $petCount")
                    dialogView.findViewById<TextView>(R.id.postCount).setText("Post : $petCount")
                } else {
                    Log.e("ProfileFragment", "No pets found for userId: $userId")
                }
            }
            .addOnFailureListener {
                // Dismiss the loading dialog in case of failure
                Log.e("ProfileFragment", "Error fetching pet data: ${it.message}")
            }
        // Show the dialog
        dialog.show()
    }

}