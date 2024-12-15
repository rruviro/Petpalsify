package com.doctorblue.petpalsify

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.doctorblue.petpalsify.Model.PersonalInfo
import com.doctorblue.petpalsify.databinding.FragmentProfileBinding
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding

    private var selectedProfileImageUri: Uri? = null
    private var selectedCoverImageUri: Uri? = null

    // This will hold the type of image being selected (profile or cover)
    private var imageType: String? = null

    // Register the Activity Result API for image selection
    private val getImageResult = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            when (imageType) {
                "profileImage" -> {
                    selectedProfileImageUri = it
                    binding.profileImage.setImageURI(it) // Display the selected image
                    // Convert the image to Base64 and save it in Firebase
                    val base64Image = convertUriToBase64(it)
                    saveProfileImageToFirebase(base64Image)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentProfileBinding.inflate(inflater, container, false)

        // Navigation button setup
        binding.bottomNavigation.homebtn.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_homeFragment)
        }

        binding.bottomNavigation.petsbtn.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_myPostFragment)
        }

        binding.bottomNavigation.inboxbtn.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_chatsFragment)
        }

        binding.logout.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }

        // Fetch pet data and populate UI
        fetchPersonalData()

        binding.editProfile.setOnClickListener {
            showManageProfileDialog()
        }

        binding.profileImage.setOnClickListener {
            selectImageFromGallery("profileImage")
        }

        return binding.root
    }

    private fun fetchPersonalData() {

        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", "No ID Found").toString()
        val loadingDialog = showLoadingDialog()

        if (userId == "No ID Found") {
            Log.e("ProfileFragment", "User ID not found in SharedPreferences.")
            return
        }

        val databaseRef = FirebaseDatabase.getInstance().reference
            .child("users")
            .child(userId)

        // Fetch the user's personal information from Firebase
        databaseRef.get()
            .addOnSuccessListener { snapshot ->

                binding.filter.visibility = View.GONE
                loadingDialog.dismiss()

                if (snapshot.exists()) {
                    val personalInfo = snapshot.getValue(PersonalInfo::class.java)
                    personalInfo?.let { populateUI(it) } // Populate the UI with fetched data
                } else {
                    Log.e("ProfileFragment", "No data found for userId: $userId")
                }

            }
            .addOnFailureListener {
                // Dismiss the loading dialog in case of failure
                loadingDialog.dismiss()
                Log.e("ProfileFragment", "Error fetching data: ${it.message}")
            }

        val databaseRefCount = FirebaseDatabase.getInstance().reference
            .child("pet-profile")
            .child(userId)

        // Fetch the petId count for the specific user
        databaseRefCount.get()
            .addOnSuccessListener { snapshot ->
                binding.filter.visibility = View.GONE
                loadingDialog.dismiss()

                if (snapshot.exists()) {
                    val petCount = snapshot.childrenCount // Count the number of petId entries (child nodes)
                    Log.d("ProfileFragment", "Pet count for userId $userId: $petCount")
                    binding.totalPost.text = "$petCount"  // Update with the count value

                    // Fetch the user's personal information from Firebase
                    val userRef = FirebaseDatabase.getInstance().reference.child("users").child(userId)
                    userRef.get()
                        .addOnSuccessListener { userSnapshot ->
                            if (userSnapshot.exists()) {
                                val personalInfo = userSnapshot.getValue(PersonalInfo::class.java)
                                personalInfo?.let { populateUI(it) } // Populate the UI with fetched data
                            } else {
                                Log.e("ProfileFragment", "No data found for userId: $userId")
                            }
                        }
                        .addOnFailureListener {
                            Log.e("ProfileFragment", "Error fetching personal data: ${it.message}")
                        }
                } else {
                    Log.e("ProfileFragment", "No pets found for userId: $userId")
                }
            }
            .addOnFailureListener {
                // Dismiss the loading dialog in case of failure
                loadingDialog.dismiss()
                Log.e("ProfileFragment", "Error fetching pet data: ${it.message}")
            }

    }

    private fun populateUI(personalInfo: PersonalInfo) {
        binding.apply {
            name.text = "${personalInfo.firstname} ${personalInfo.lastname}"
            state.text = personalInfo.category
            // Load profile image if available
            personalInfo.profileImage?.let { imageBase64 ->
                try {
                    val decodedString: ByteArray = Base64.decode(imageBase64, Base64.DEFAULT)
                    val decodedBitmap: Bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                    Glide.with(requireContext())
                        .load(decodedBitmap) // Use Bitmap for local image
                        .into(binding.profileImage)
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "Failed to decode profile image: ${e.message}")
                }
            }
        }
    }

    private fun saveProfileChanges(
        firstName: String,
        lastName: String,
        password: String,
        phoneNumber: String,
        category: String,
        currentProfileImageBase64: String?,
    ) {
        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", "No ID Found").toString()
        val databaseRef = FirebaseDatabase.getInstance().reference.child("users").child(userId)

        // Only update the profile image if a new one is selected
        val updatedProfileImage = selectedProfileImageUri?.let { convertUriToBase64(it) } ?: currentProfileImageBase64

        val updatedInfo = PersonalInfo(
            firstname = firstName,
            lastname = lastName,
            password = password,
            phoneNumber = phoneNumber,
            category = category,
            profileImage = updatedProfileImage
        )

        databaseRef.setValue(updatedInfo)
            .addOnSuccessListener {
                Log.d("ProfileFragment", "Profile updated successfully.")
                findNavController().navigate(R.id.action_profileFragment_self) // Refresh data
            }
            .addOnFailureListener {
                Log.e("ProfileFragment", "Error updating profile: ${it.message}")
            }
    }

    private fun showManageProfileDialog() {
        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", "No ID Found").toString()
        val databaseRef = FirebaseDatabase.getInstance().reference.child("users").child(userId)

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.manage_profile, null)
        val profileImageView = dialogView.findViewById<ShapeableImageView>(R.id.profileImage)

        // Initialize spinner and adapter
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.category)
        val categoryOptions = arrayOf("Select Category", "Adaptor", "Giver")
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            categoryOptions
        )
        categorySpinner.adapter = categoryAdapter

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Save", null) // Set the button without a default behavior
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()

        // Load data into dialog fields from Firebase
        databaseRef.get().addOnSuccessListener { snapshot ->
            val personalInfo = snapshot.getValue(PersonalInfo::class.java)
            personalInfo?.let {
                dialogView.findViewById<EditText>(R.id.firstname).setText(it.firstname)
                dialogView.findViewById<EditText>(R.id.lastname).setText(it.lastname)
                dialogView.findViewById<EditText>(R.id.password).setText(it.password)
                dialogView.findViewById<EditText>(R.id.phoneNumber).setText(it.phoneNumber)

                // Set spinner selection
                val position = categoryAdapter.getPosition(it.category)
                if (position >= 0) categorySpinner.setSelection(position)

                // Load profile image
                it.profileImage?.let { base64Image ->
                    val decodedString: ByteArray = Base64.decode(base64Image, Base64.DEFAULT)
                    val decodedBitmap: Bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                    profileImageView.setImageBitmap(decodedBitmap)
                }
            }
        }

        // Open gallery to select a new profile image
        profileImageView.setOnClickListener {
            selectImageFromGallery("profileImage")
            selectedProfileImageUri?.let { uri ->
                profileImageView.setImageURI(uri) // Update the ImageView if an image is selected
            }
        }

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            saveButton?.setOnClickListener {
                val newFirstName = dialogView.findViewById<EditText>(R.id.firstname).text.toString()
                val newLastName = dialogView.findViewById<EditText>(R.id.lastname).text.toString()
                val newPassword = dialogView.findViewById<EditText>(R.id.password).text.toString()
                val newPhoneNumber = dialogView.findViewById<EditText>(R.id.phoneNumber).text.toString()

                val selectedCategory = categorySpinner.selectedItem.toString()

                // Validate input
                if (selectedCategory == "Select Category") {
                    Toast.makeText(requireContext(), "Select Your Account Category", Toast.LENGTH_LONG).show()
                } else {
                    databaseRef.get().addOnSuccessListener { snapshot ->
                        val personalInfo = snapshot.getValue(PersonalInfo::class.java)
                        val existingProfileImageBase64 = personalInfo?.profileImage
                        val updatedProfileImage = selectedProfileImageUri?.let { uri -> convertUriToBase64(uri) } ?: existingProfileImageBase64

                        saveProfileChanges(
                            newFirstName,
                            newLastName,
                            newPassword,
                            newPhoneNumber,
                            selectedCategory,
                            updatedProfileImage
                        )

                        dialog.dismiss() // Close the dialog only after successful validation and save
                    }
                }
            }
        }

        dialog.show()
    }

    // Convert URI to Base64 String
    private fun convertUriToBase64(uri: Uri): String {
        val bitmap = BitmapFactory.decodeStream(requireContext().contentResolver.openInputStream(uri))
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    // Save the profile image to Firebase
    private fun saveProfileImageToFirebase(base64Image: String) {
        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", "No ID Found").toString()
        val databaseRef = FirebaseDatabase.getInstance().reference.child("users").child(userId)

        // Fetch existing user data and update profile image
        databaseRef.get().addOnSuccessListener { snapshot ->
            val personalInfo = snapshot.getValue(PersonalInfo::class.java)
            personalInfo?.let {
                val updatedInfo = it.copy(profileImage = base64Image)
                // Update the user profile data in Firebase
                databaseRef.setValue(updatedInfo).addOnSuccessListener {
                    Log.d("ProfileFragment", "Profile image updated successfully.")
                }.addOnFailureListener {
                    Log.e("ProfileFragment", "Failed to update profile image: ${it.message}")
                }
            }
        }
    }

    private fun selectImageFromGallery(imageType: String) {
        this.imageType = imageType
        getImageResult.launch("image/*")
    }

    private fun showLoadingDialog(): AlertDialog {
        val builder = AlertDialog.Builder(requireContext(), R.style.TransparentDialog)
        builder.setView(R.layout.loading_dialog)
        builder.setCancelable(false)
        binding.filter.visibility = View.VISIBLE
        return builder.create().apply { show() }
    }

}
