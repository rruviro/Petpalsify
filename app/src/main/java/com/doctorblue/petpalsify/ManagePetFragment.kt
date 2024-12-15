package com.doctorblue.petpalsify

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.doctorblue.petpalsify.Model.PetProfile
import com.doctorblue.petpalsify.databinding.FragmentManagePetBinding
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream

class ManagePetFragment : Fragment() {

    private lateinit var binding: FragmentManagePetBinding
    companion object {
        private const val REQUEST_PICK_IMAGE = 2
    }
    private var selectedImageUri: Uri? = null
    private var currentPetImage: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentManagePetBinding.inflate(inflater, container, false)

        // Navigation back to the previous screen
        binding.back.setOnClickListener {
            findNavController().navigate(R.id.action_managePetFragment_to_myPostFragment)
        }

        val petId = requireArguments().getString("petId") ?: run {
            showToast("Pet ID is missing")
            return binding.root
        }

        binding.petMainImage.setOnClickListener {
            selectImageFromGallery()
        }

        setupSpinners()
        fetchPetData(petId)

        binding.check.setOnClickListener { handleUpdate(petId) }

        return binding.root
    }

    private fun setupSpinners() {
        val genderOptions = arrayOf("Select Gender", "Male", "Female")
        val categoryOptions = arrayOf("Select Pet", "Dog", "Cat", "Rabbit", "Fish", "Hamster", "Bird")

        binding.sex.adapter = GenderAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, genderOptions)
        binding.petCategory.adapter = CategoryAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categoryOptions)
    }

    private fun fetchPetData(petId: String) {
        val userId = getUserId() ?: return
        val loadingDialog = showLoadingDialog()
        loadingDialog.show()
        binding.filter.visibility = View.VISIBLE

        val databaseRef = FirebaseDatabase.getInstance().reference
            .child("pet-profile")
            .child(userId)
            .child(petId)

        databaseRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    loadingDialog.dismiss()
                    binding.filter.visibility = View.GONE
                    snapshot.getValue(PetProfile::class.java)?.let { petProfile ->
                        populateUI(petProfile)
                        // Store the current pet image
                        currentPetImage = petProfile.petImage
                    }
                } else {
                    showToast("Pet profile not found")
                }
            }
            .addOnFailureListener {
                binding.filter.visibility = View.VISIBLE
                showToast("Error fetching pet data")
            }
    }

    private fun populateUI(petProfile: PetProfile) {
        binding.apply {
            petName.setText(petProfile.petName)
            address.setText(petProfile.petAddress)
            age.setText(petProfile.petAge)
            weight.setText(petProfile.petWeight)
            birth.setText(petProfile.petBirthday)
            contact.setText(petProfile.userContact)
            about.setText(petProfile.petAbout)
            healthtxt.setText(petProfile.petHealth)
            petBreed.setText(petProfile.petBreed)

            // Check if petImage is not null and load it
            petProfile.petImage?.let { imageBase64 ->
                // Decode the Base64 string into a Bitmap
                try {
                    val decodedString: ByteArray = Base64.decode(imageBase64, Base64.DEFAULT)  // Corrected here
                    val decodedBitmap: Bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

                    // Load the decoded Bitmap into the ImageView using Glide
                    Glide.with(requireContext())
                        .load(decodedBitmap)  // Use Bitmap for local image
                        .into(binding.petMainImage)
                } catch (e: Exception) {
                    print("Failed to decode image")
                }
            }

            // Set the spinner selections based on the pet profile values
            setSpinnerSelection(petCategory, petProfile.petCategory)
            setSpinnerSelection(sex, petProfile.petGender)
        }
    }


    private fun handleUpdate(petId: String) {
        val userId = getUserId() ?: return

        val petProfile = PetProfile(
            petName = binding.petName.text.toString().trim(),
            petAddress = binding.address.text.toString().trim(),
            petAge = binding.age.text.toString().trim(),
            petWeight = binding.weight.text.toString().trim(),
            petBirthday = binding.birth.text.toString().trim(),
            userContact = binding.contact.text.toString().trim(),
            petAbout = binding.about.text.toString().trim(),
            petHealth = binding.healthtxt.text.toString().trim(),
            petCategory = binding.petCategory.selectedItem.toString(),
            petGender = binding.sex.selectedItem.toString(),
            petBreed = binding.petBreed.text.toString().trim()
        )

        // Check validation
        if (!validateProfile(petProfile)) return

        // Show loading dialog
        val loadingDialog = showLoadingDialog()

        // Fetch the current pet profile
        val databaseRef = FirebaseDatabase.getInstance().reference
            .child("pet-profile")
            .child(userId)
            .child(petId)

        databaseRef.get().addOnSuccessListener { snapshot ->
            val currentPetProfile = snapshot.getValue(PetProfile::class.java)

            // If the current pet profile exists, check which fields are different
            if (currentPetProfile != null) {
                val updatedProfile = PetProfile(
                    petImage = if (selectedImageUri != null) convertImageToBytes(selectedImageUri) else currentPetImage,
                    petName = if (petProfile.petName.isNotEmpty()) petProfile.petName else currentPetProfile.petName,
                    petAddress = if (petProfile.petAddress.isNotEmpty()) petProfile.petAddress else currentPetProfile.petAddress,
                    petAge = if (petProfile.petAge.isNotEmpty()) petProfile.petAge else currentPetProfile.petAge,
                    petWeight = if (petProfile.petWeight.isNotEmpty()) petProfile.petWeight else currentPetProfile.petWeight,
                    petBirthday = if (petProfile.petBirthday.isNotEmpty()) petProfile.petBirthday else currentPetProfile.petBirthday,
                    userContact = if (petProfile.userContact.isNotEmpty()) petProfile.userContact else currentPetProfile.userContact,
                    petAbout = if (petProfile.petAbout.isNotEmpty()) petProfile.petAbout else currentPetProfile.petAbout,
                    petHealth = if (petProfile.petHealth.isEmpty()) petProfile.petHealth else currentPetProfile.petHealth,
                    petCategory = if (petProfile.petCategory != "Select Pet") petProfile.petCategory else currentPetProfile.petCategory,
                    petGender = if (petProfile.petGender != "Select Gender") petProfile.petGender else currentPetProfile.petGender,
                    petBreed = if (petProfile.petBreed.isNotEmpty()) petProfile.petBreed else currentPetProfile.petBreed,
                    petPostDate = currentPetProfile.petPostDate,  // Keep the post date unchanged
                    petId = currentPetProfile.petId,  // Keep petId unchanged
                    userpetId = currentPetProfile.userpetId,  // Keep userpetId unchanged
                    userId = currentPetProfile.userId  // Keep userId unchanged
                )

                // Update only the changed fields
                databaseRef.setValue(updatedProfile)
                    .addOnCompleteListener { task ->
                        loadingDialog.dismiss() // Dismiss the dialog after task completion
                        if (task.isSuccessful) {
                            showToast("Pet Profile Updated Successfully!")
                            findNavController().navigate(R.id.action_managePetFragment_to_myPostFragment)
                        } else {
                            showToast("Error updating pet profile")
                        }
                    }
                    .addOnFailureListener { error ->
                        loadingDialog.dismiss()
                        showToast("Update failed: ${error.message}")
                    }
            } else {
                loadingDialog.dismiss()
                showToast("Pet profile not found")
            }
        }
    }

    private fun validateProfile(profile: PetProfile): Boolean {
        if (profile.petName.isEmpty() || profile.petAddress.isEmpty() || profile.petCategory == "Select Pet" || profile.petGender == "Select Gender") {
            showToast("Please fill in all required fields")
            return false
        }
        return true
    }

    private fun setSpinnerSelection(spinner: Spinner, value: String?) {
        val index = (spinner.adapter as ArrayAdapter<String>).getPosition(value)
        if (index >= 0) spinner.setSelection(index)
    }

    private fun getUserId(): String? {
        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", null)

        if (userId.isNullOrEmpty()) {
            showToast("User ID not found")
            return null
        }

        return userId
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            binding.petMainImage.setImageURI(selectedImageUri)
        }
    }

    private fun convertImageToBytes(uri: Uri?): String {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri!!)
            val byteArrayOutputStream = ByteArrayOutputStream()
            inputStream?.use { input ->
                val buffer = ByteArray(1024)
                var length: Int
                while (input.read(buffer).also { length = it } != -1) {
                    byteArrayOutputStream.write(buffer, 0, length)
                }
            }
            val byteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showLoadingDialog(): AlertDialog {
        val builder = AlertDialog.Builder(requireContext(), R.style.TransparentDialog)
        builder.setView(R.layout.loading_dialog)
        binding.filter.visibility = View.VISIBLE
        builder.setCancelable(false)
        return builder.create().apply { show() }
    }

}
