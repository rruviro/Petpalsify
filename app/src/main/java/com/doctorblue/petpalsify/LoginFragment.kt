package com.doctorblue.petpalsify

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.doctorblue.petpalsify.databinding.FragmentLoginBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private lateinit var database: DatabaseReference
    private lateinit var loadingDialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        database = FirebaseDatabase.getInstance().reference.child("users")

        setupLoadingDialog()

        binding.login.setOnClickListener {
            val number = binding.username.text.toString()
            val password = binding.password.text.toString()

            if (number.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter all fields", Toast.LENGTH_SHORT).show()
            } else {
                showLoadingDialog()
                loginUser(number, password)
            }
        }

        binding.signup.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
        }

        return binding.root
    }

    private fun setupLoadingDialog() {
        val builder = AlertDialog.Builder(requireContext(), R.style.TransparentDialog)
        builder.setView(R.layout.loading_dialog)
        builder.setCancelable(false)
        loadingDialog = builder.create()
    }

    private fun showLoadingDialog() {
        loadingDialog.show()
    }

    private fun hideLoadingDialog() {
        if (loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }

    private fun loginUser(number: String, password: String) {
        database.orderByChild("phoneNumber").equalTo(number)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    hideLoadingDialog()
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val storedPassword = userSnapshot.child("password").value.toString()
                            if (storedPassword == password) {
                                val userId = userSnapshot.key.toString() // Get userId
                                saveUserIdToPreferences(userId)
                                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                                return
                            }
                        }
                        Toast.makeText(requireContext(), "Incorrect password", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    hideLoadingDialog()
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun saveUserIdToPreferences(userId: String) {
        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("userId", userId)
        editor.apply()
    }
}
