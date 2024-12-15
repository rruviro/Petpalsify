package com.doctorblue.petpalsify

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.doctorblue.petpalsify.Model.PersonalInfo
import com.doctorblue.petpalsify.databinding.FragmentSignUpBinding
import com.google.firebase.database.*

class SignUpFragment : Fragment() {

    private lateinit var binding: FragmentSignUpBinding
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentSignUpBinding.inflate(inflater, container, false)

        // Firebase Database reference
        database = FirebaseDatabase.getInstance().reference.child("users")

        // Navigate to login fragment when login button is clicked
        binding.login.setOnClickListener {
            findNavController().navigate(R.id.action_signUpFragment_to_loginFragment)
        }

        // Sign-up functionality when signup button is clicked
        binding.signupbtn.setOnClickListener {
            val firstname = binding.firstname.text.toString()
            val lastname = binding.lastname.text.toString()
            val phoneNumber = binding.phone.text.toString()
            val password = binding.password.text.toString()
            val confirmPassword = binding.confirmPassword.text.toString()

            if (firstname.isNotEmpty() && lastname.isNotEmpty()
                && phoneNumber.isNotEmpty() && password.isNotEmpty()
                && confirmPassword.isNotEmpty()
            ) {
                if (!isValidPhoneNumber(phoneNumber)) {
                    Toast.makeText(requireContext(), "Invalid phone number format!", Toast.LENGTH_SHORT).show()
                } else if (confirmPassword != password) {
                    Toast.makeText(requireContext(), "Password and confirm password do not match!", Toast.LENGTH_SHORT).show()
                } else {
                    checkPhoneNumberExists(firstname, lastname, phoneNumber, password)
                }
            } else {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        // Match numbers starting with 09 (11 digits total) or +63 (12 digits total)
        val regex = Regex("^09\\d{9}\$|^\\+63\\d{9}\$")
        return regex.matches(phoneNumber)
    }

    // Check if phone number already exists in Firebase Realtime Database
    private fun checkPhoneNumberExists(
        firstname: String,
        lastname: String,
        phoneNumber: String,
        password: String
    ) {
        database.orderByChild("phoneNumber").equalTo(phoneNumber)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Toast.makeText(requireContext(), "Phone number already exists!", Toast.LENGTH_SHORT).show()
                    } else {
                        saveUserData(firstname, lastname, phoneNumber, password)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error checking phone number: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Save user data to Firebase Realtime Database
    private fun saveUserData(
        firstname: String,
        lastname: String,
        phoneNumber: String,
        password: String
    ) {
        val userId = database.push().key // Generate a unique user ID
        val user = PersonalInfo(
            firstname = firstname,
            lastname = lastname,
            phoneNumber = phoneNumber,
            password = password
        )

        userId?.let {
            database.child(it).setValue(user).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "User data saved successfully!", Toast.LENGTH_SHORT).show()
                    // Navigate to login fragment
                    findNavController().navigate(R.id.action_signUpFragment_to_loginFragment)
                } else {
                    Toast.makeText(requireContext(), "Error saving data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}