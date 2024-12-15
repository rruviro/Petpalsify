package com.doctorblue.petpalsify

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.doctorblue.petpalsify.Model.PetProfile
import com.doctorblue.petpalsify.databinding.FragmentPetProfileBinding
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class PetProfileFragment : Fragment() {

    private lateinit var binding: FragmentPetProfileBinding

    companion object {
        private const val REQUEST_PICK_IMAGE = 2
    }

    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPetProfileBinding.inflate(inflater, container, false)

        setupUI()

        binding.check.setOnClickListener { validateAndSavePetProfile() }

        binding.petMainImage.setOnClickListener { selectImageFromGallery() }

        return binding.root
    }

    private fun setupUI() {
        binding.back.setOnClickListener {
            findNavController().navigate(R.id.action_petProfileFragment_to_myPostFragment)
        }

        val genderOptions = arrayOf("Select Gender", "Male", "Female")
        val genderAdapter = GenderAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, genderOptions)
        binding.sex.adapter = genderAdapter

        val categoryOptions = arrayOf("Select Pet", "Dog", "Cat", "Rabbit", "Fish", "Hamster", "Bird")
        val categoryAdapter = CategoryAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categoryOptions)
        binding.petCategory.adapter = categoryAdapter
    }

    private fun validateAndSavePetProfile() {
        val petName = binding.petName.text.toString().trim()
        val petAddress = binding.address.text.toString().trim()
        val petAge = binding.age.text.toString().trim()
        val petWeight = binding.weight.text.toString().trim()
        val petBirthday = binding.birth.text.toString().trim()
        val userContact = binding.contact.text.toString().trim()
        val petAbout = binding.about.text.toString().trim()
        val petHealth = binding.healthtxt.text.toString().trim()
        val petGender = binding.sex.selectedItem.toString()
        val petCategory = binding.petCategory.selectedItem.toString()
        val petBreed = binding.petBreed.text.toString().trim()

        if (petName.isEmpty() || petAddress.isEmpty() || petAge.isEmpty() ||
            petWeight.isEmpty() || petBirthday.isEmpty() || userContact.isEmpty() ||
            petAbout.isEmpty() || petHealth.isEmpty() || petCategory == "Select Pet" || petBreed.isEmpty() ||
            petGender == "Select Gender" || selectedImageUri == null
        ) {
            Toast.makeText(requireContext(), "Please fill in all fields and select an image", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = getUserId()
        if (userId.isNotEmpty()) {
            val currentDateTime = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("'On' MMM dd h:mma", Locale.getDefault())
            val petPostDate = dateFormat.format(currentDateTime)

            storePetProfileData(
                userId, petName, petAge, petAbout, petHealth, petWeight,
                petAddress, petBirthday, userContact, petCategory,
                petBreed, petGender, petPostDate
            )
        } else {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getUserId(): String {
        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("userId", "") ?: ""
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

    private fun storePetProfileData(
        userId: String,
        petName: String,
        petAge: String,
        petHealth: String,
        petAbout: String,
        petWeight: String,
        petAddress: String,
        petBirthday: String,
        userContact: String,
        petCategory: String,
        petBreed: String,
        petGender: String,
        petPostDate: String
    ) {
        val loadingDialog = showLoadingDialog()

        val database = FirebaseDatabase.getInstance().reference.child("pet-profile")
        val petId = database.push().key

        val petProfile = PetProfile(
            petImage = convertImageToBytes(selectedImageUri),
            petName = petName,
            petAge = petAge,
            petAbout = petAbout,
            petHealth = petHealth,
            petWeight = petWeight,
            petAddress = petAddress,
            petBirthday = petBirthday,
            userContact = userContact,
            petCategory = petCategory,
            petBreed = petBreed,
            petGender = petGender,
            petPostDate = petPostDate,
            petId = petId,
            userpetId = userId
        )

        petId?.let {
            database.child(userId).child(it).setValue(petProfile).addOnCompleteListener { task ->
                loadingDialog.dismiss()
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Pet Profile Saved!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_petProfileFragment_to_myPostFragment)
                } else {
                    Toast.makeText(requireContext(), "Error saving pet profile", Toast.LENGTH_SHORT).show()
                }
            }
        } ?: run {
            loadingDialog.dismiss()
            Toast.makeText(requireContext(), "Error generating pet ID", Toast.LENGTH_SHORT).show()
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

    private fun showLoadingDialog(): AlertDialog {
        val builder = AlertDialog.Builder(requireContext(), R.style.TransparentDialog)
        builder.setView(R.layout.loading_dialog)
        builder.setCancelable(false)
        return builder.create().apply { show() }
    }

}