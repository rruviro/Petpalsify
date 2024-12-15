package com.doctorblue.petpalsify

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.transition.Visibility
import com.bumptech.glide.Glide
import com.doctorblue.petpalsify.Model.PetProfile
import com.doctorblue.petpalsify.databinding.FragmentInformationBinding
import com.google.firebase.database.FirebaseDatabase

class InformationFragment : Fragment() {

    private lateinit var binding: FragmentInformationBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentInformationBinding.inflate(inflater, container, false)

        // Retrieve user ID from SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        val userId = sharedPreferences.getString("userId", "No ID Found")
        val userPetId = requireArguments().getString("userPetId").toString()
        val petId = requireArguments().getString("petId")

        binding.back.setOnClickListener {
            findNavController().navigate(R.id.action_informationFragment_to_homeFragment)
        }

        // Fetch the pet data using the petId
        if (petId != null) {
            fetchPetData(userPetId, petId)
        } else {
            Toast.makeText(requireContext(), "Pet ID is missing", Toast.LENGTH_SHORT).show()
        }

        if (userPetId == userId) {
            binding.contact.visibility = View.GONE  // Hide the contact button if userPetId matches userId
        } else {
            binding.contact.setOnClickListener {
                val bundle = Bundle()
                bundle.putString("petId", petId)
                bundle.putString("userPetId", userPetId)
                findNavController().navigate(R.id.action_informationFragment_to_convoFragment, bundle)
            }
        }

        return binding.root

    }

    private fun fetchPetData(userPetId: String, petId: String) {
        // Show loading dialog before fetching data
        val loadingDialog = showLoadingDialog()

        // Retrieve user ID from SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", "No ID Found")

        // Check if userId is your specific userId and disable button
        if (userId == "yourUserId") {
            binding.contact.isEnabled = false // Disable the button if userId matches
        }

        if (userPetId != "No ID Found") {
            // Firebase Database reference for the specific pet profile
            val database = FirebaseDatabase.getInstance().reference
                .child("pet-profile")
                .child(userPetId)
                .child(petId)

            database.get().addOnSuccessListener { snapshot ->
                // Dismiss the loading dialog once the data is fetched
                binding.filter.visibility = View.GONE
                loadingDialog.dismiss()

                Log.d("PetProfileSnapshot", snapshot.value.toString()) // Log the snapshot data
                if (snapshot.exists()) {
                    val petProfile = snapshot.getValue(PetProfile::class.java)
                    petProfile?.let {
                        // Populate the UI with the fetched pet data
                        binding.petName.text = it.petName
                        binding.petCategory.text = it.petCategory
                        binding.petAge.text = it.petAge
                        binding.petWeight.text = it.petWeight
                        binding.petDescription.text = it.petAbout
                        binding.petBreed.text = it.petBreed
                        binding.healthdescription.text = it.petHealth

                        // Check if petImage is not null and load it
                        petProfile.petImage?.let { imageBase64 ->
                            // Decode the Base64 string into a Bitmap
                            try {
                                val decodedString: ByteArray = Base64.decode(imageBase64, Base64.DEFAULT)
                                val decodedBitmap: Bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

                                // Load the decoded Bitmap into the ImageView using Glide
                                Glide.with(requireContext())
                                    .load(decodedBitmap)  // Use Bitmap for local image
                                    .into(binding.petImage)
                            } catch (e: Exception) {
                                // Handle image decoding error
                            }
                        }
                    }
                } else {
                    // If no pet profile is found
                    Toast.makeText(requireContext(), "Pet profile not found", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                // Dismiss the loading dialog in case of failure
                loadingDialog.dismiss()

                // Handle error fetching data
                Toast.makeText(requireContext(), "Error fetching pet data", Toast.LENGTH_SHORT).show()
            }

        } else {
            // Handle case when user ID is not found
            Toast.makeText(requireContext(), "User ID not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoadingDialog(): AlertDialog {
        val builder = AlertDialog.Builder(requireContext(), R.style.TransparentDialog)
        builder.setView(R.layout.loading_dialog)
        builder.setCancelable(false)
        binding.filter.visibility = View.VISIBLE
        return builder.create().apply { show() }
    }

}