package com.doctorblue.petpalsify

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.doctorblue.petpalsify.Adapter.InquireAdapter
import com.doctorblue.petpalsify.Model.Conversation
import com.doctorblue.petpalsify.Model.Message
import com.doctorblue.petpalsify.Model.PersonalInfo
import com.doctorblue.petpalsify.databinding.FragmentChatsBinding
import com.google.firebase.database.FirebaseDatabase

class ChatsFragment : Fragment() {

    private lateinit var binding: FragmentChatsBinding
    private var conversationList = mutableListOf<Conversation>()
    private lateinit var inquireAdapter: InquireAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChatsBinding.inflate(inflater, container, false)

        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", "No ID Found").toString()

        binding.bottomNavigation.homebtn.setOnClickListener {
            findNavController().navigate(R.id.action_chatsFragment_to_homeFragment)
        }

        binding.bottomNavigation.petsbtn.setOnClickListener {
            findNavController().navigate(R.id.action_chatsFragment_to_myPostFragment)
        }

        binding.bottomNavigation.profilebtn.setOnClickListener {
            findNavController().navigate(R.id.action_chatsFragment_to_profileFragment)
        }

        // Set up RecyclerView with a click listener
        inquireAdapter = InquireAdapter(conversationList, requireContext()) { recipientId ->
            // Navigate to the conversation fragment with recipientId as argument
            val bundle = Bundle().apply {
                putString("userPetId", recipientId)
            }
            findNavController().navigate(R.id.action_chatsFragment_to_convoFragment, bundle)
        }

        binding.eachRecycleview.layoutManager = LinearLayoutManager(requireContext())
        binding.eachRecycleview.adapter = inquireAdapter

        // Fetch conversations for the user
        fetchConversations(userId)

        return binding.root
    }

    private fun fetchConversations(userId: String) {
        val database = FirebaseDatabase.getInstance().reference
        val loadingDialog = showLoadingDialog()
        // Fetch the conversations for the user
        database.child("conversations").child(userId).get()
            .addOnSuccessListener { snapshot ->
                binding.filter.visibility = View.GONE
                loadingDialog.dismiss()
                if (snapshot.exists()) {
                    conversationList.clear()
                    // Loop through each conversation (recipient)
                    for (recipientSnapshot in snapshot.children) {
                        val recipientUserId = recipientSnapshot.key
                        val messages = mutableListOf<Message>()

                        // Fetch messages in the conversation
                        recipientSnapshot.child("messages").child("messagesInfo").children.forEach { messageSnapshot ->
                            val message = messageSnapshot.getValue(Message::class.java)
                            message?.let { messages.add(it) }
                        }

                        // After fetching messages, fetch user info for the recipient (toId)
                        if (messages.isNotEmpty() && recipientUserId != null) {
                            fetchUserInfo(recipientUserId) { userInfo ->
                                // Create a conversation object with userInfo after fetching the user data
                                val conversation = Conversation(
                                    conversationId = recipientUserId, // Use recipientUserId as conversationId
                                    messages = messages,               // List of messages
                                    userInfo = userInfo                // Fetched user info (PersonalInfo)
                                )
                                conversationList.add(conversation)

                                // Notify adapter after each conversation is added
                                inquireAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                } else {
                    binding.filter.visibility = View.GONE
                    loadingDialog.dismiss()
                    Toast.makeText(requireContext(), "No conversations found", Toast.LENGTH_SHORT)
                }
            }
            .addOnFailureListener { exception ->
                binding.filter.visibility = View.GONE
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "Error fetching data: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchUserInfo(userId: String, onComplete: (PersonalInfo?) -> Unit) {
        val database = FirebaseDatabase.getInstance().reference

        // Fetch user info from Firebase
        database.child("users").child(userId).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val userInfo = snapshot.getValue(PersonalInfo::class.java)
                    onComplete(userInfo)  // Return the fetched user info
                } else {
                    onComplete(null)  // Return null if no user info found
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error fetching user info: ${exception.message}", Toast.LENGTH_SHORT).show()
                onComplete(null)  // Return null on failure
            }
    }

    private fun showLoadingDialog(): androidx.appcompat.app.AlertDialog {
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.TransparentDialog)
        builder.setView(R.layout.loading_dialog)
        builder.setCancelable(false)
        binding.filter.visibility = View.VISIBLE
        return builder.create().apply { show() }
    }

}