package com.doctorblue.petpalsify

import TwilioResponse
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.doctorblue.petpalsify.databinding.FragmentLoginBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import retrofit2.Call
import retrofit2.Response

class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private lateinit var database: DatabaseReference
    private lateinit var loadingDialog: AlertDialog
    private val retrofitClient = RetrofitClient.api
    private val twilioServiceSid = "VA690347f55399d285b83fcc33dc0864c2" // Twilio service SID

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        database = FirebaseDatabase.getInstance().reference.child("users")

        setupLoadingDialog()

        binding.login.setOnClickListener {
            val phoneNumber = binding.username.text.toString()
            val password = binding.password.text.toString()

            if (phoneNumber.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter all fields", Toast.LENGTH_SHORT).show()
            } else {
                showLoadingDialog()
                loginUser(phoneNumber, password)
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

    private fun loginUser(phoneNumber: String, password: String) {
        val formattedPhoneNumber = formatPhoneNumber(phoneNumber)  // Format the phone number

        database.orderByChild("phoneNumber").equalTo(phoneNumber)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    hideLoadingDialog()
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val storedPassword = userSnapshot.child("password").value.toString()
                            if (storedPassword == password) {
                                // Send OTP to the formatted phone number using Twilio
                                sendOTP(formattedPhoneNumber)
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

    // Function to format the phone number
    private fun formatPhoneNumber(phoneNumber: String): String {
        return if (phoneNumber.startsWith("09")) {
            "+63" + phoneNumber.substring(1)  // Replace '09' with '+63'
        } else {
            phoneNumber // Return the number as it is if it's already in the correct format
        }
    }

    private fun sendOTP(phoneNumber: String) {
        retrofitClient.sendOTP(twilioServiceSid, phoneNumber, "sms")
            .enqueue(object : retrofit2.Callback<TwilioResponse> {
                override fun onResponse(call: Call<TwilioResponse>, response: Response<TwilioResponse>) {
                    if (response.isSuccessful) {
                        // OTP sent, now prompt for verification
                        promptForOTP(phoneNumber)
                    } else {
                        Toast.makeText(requireContext(), "Failed to send OTP", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<TwilioResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun promptForOTP(phoneNumber: String) {
        val otpDialog = AlertDialog.Builder(requireContext())
            .setTitle("Enter OTP")
            .setView(R.layout.dialog_otp) // Ensure this layout exists
            .setPositiveButton("Verify") { dialog, _ ->
                // After the dialog is shown, retrieve the EditText for OTP
                val otpCode = (dialog as AlertDialog).findViewById<EditText>(R.id.otp_code)?.text.toString()

                if (otpCode.isNotEmpty()) {
                    verifyOTP(phoneNumber, otpCode)
                } else {
                    Toast.makeText(requireContext(), "Please enter the OTP", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss() // Close the dialog after handling the OTP
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss() // Dismiss the dialog if Cancel is clicked
            }
            .create()

        otpDialog.show() // Show the dialog
    }


    private fun verifyOTP(phoneNumber: String, otpCode: String) {
        retrofitClient.verifyOTP(twilioServiceSid, phoneNumber, otpCode)
            .enqueue(object : retrofit2.Callback<TwilioResponse> {
                override fun onResponse(call: Call<TwilioResponse>, response: Response<TwilioResponse>) {
                    if (response.isSuccessful) {
                        // OTP verified, proceed to home screen
                        val userId = "user_id_from_snapshot" // Retrieve userId after successful login
                        saveUserIdToPreferences(userId)
                        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                    } else {
                        Toast.makeText(requireContext(), "Invalid OTP", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<TwilioResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
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
