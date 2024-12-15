package com.doctorblue.petpalsify

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.doctorblue.petpalsify.Adapter.PetProfileAdapter
import com.doctorblue.petpalsify.Model.PetProfile
import com.doctorblue.petpalsify.databinding.FragmentMyPostBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MyPostFragment : Fragment() {

    private lateinit var binding: FragmentMyPostBinding
    private lateinit var database: DatabaseReference
    private lateinit var petProfileList: MutableList<PetProfile>  // List to hold pet profiles
    private lateinit var petProfileAdapter: PetProfileAdapter  // Adapter for displaying data

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMyPostBinding.inflate(inflater, container, false)

        binding.bottomNavigation.homebtn.setOnClickListener {
            findNavController().navigate(R.id.action_myPostFragment_to_homeFragment)
        }

        binding.bottomNavigation.inboxbtn.setOnClickListener {
            findNavController().navigate(R.id.action_myPostFragment_to_chatsFragment)
        }

        binding.bottomNavigation.profilebtn.setOnClickListener {
            findNavController().navigate(R.id.action_myPostFragment_to_profileFragment)
        }

        binding.add.setOnClickListener {
            findNavController().navigate(R.id.action_myPostFragment_to_petProfilesFragment)
        }

        // Initialize Firebase database reference
        database = FirebaseDatabase.getInstance().reference.child("pet-profile")

        // Get the userId from SharedPreferences
        val sharedPreferences =
            requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", "No ID Found")

        // Initialize the list and adapter
        petProfileList = mutableListOf()
        petProfileAdapter = PetProfileAdapter(petProfileList, requireContext(), ::onManageClick, ::onDeleteClick)

        // Set up the RecyclerView
        binding.rectangleRecycleview.layoutManager = LinearLayoutManager(requireContext())
        binding.rectangleRecycleview.adapter = petProfileAdapter

        // Check if userId is valid
        if (userId != null && userId != "No ID Found") {
            // Fetch pet profiles from Firebase based on the userId
            fetchPetProfiles(userId)
        } else {
            Toast.makeText(requireContext(), "User ID not found", Toast.LENGTH_SHORT).show()
        }

        return binding.root
    }

    // Fetch pet profiles for the given userId
    private fun fetchPetProfiles(userId: String) {
        // Ensure that database is initialized properly
        val database = FirebaseDatabase.getInstance().reference  // Initialize Firebase database reference
        val loadingDialog = showLoadingDialog()
        // Fetch pet profiles for the given userId
        database.child("pet-profile").child(userId).get()
            .addOnSuccessListener { snapshot ->
                binding.filter.visibility = View.GONE
                loadingDialog.dismiss()
                if (snapshot.exists()) {
                    petProfileList.clear()  // Clear previous data
                    for (dataSnapshot in snapshot.children) {
                        // Get each pet profile data
                        val petProfile = dataSnapshot.getValue(PetProfile::class.java)
                        petProfile?.let {
                            it.petId = dataSnapshot.key
                            petProfileList.add(it)  // Add the pet profile to the list
                        }
                    }
                    binding.noPetPostText.visibility = View.GONE // Ensure the message is initially hidden
                    petProfileAdapter.notifyDataSetChanged()  // Notify adapter to update UI
                } else {
                    binding.noPetPostText.visibility = View.VISIBLE // Ensure the message is initially hidden
                }
            }
            .addOnFailureListener { exception ->
                loadingDialog.dismiss()
                // Handle failure
                Toast.makeText(
                    requireContext(),
                    "Error fetching data: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // Callback for manage button
    private fun onManageClick(petProfile: PetProfile) {

        // Handle manage action (you can navigate to another fragment or show details)
        val bundle = Bundle()
        bundle.putString("petId", petProfile.petId)
        findNavController().navigate(R.id.action_myPostFragment_to_managePetFragment, bundle)

    }

    private fun onDeleteClick(petProfile: PetProfile) {
        val userId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("userId", "No ID Found")

        if (userId != null && userId != "No ID Found") {
            if (petProfile.petId != null) {
                val dialogBuilder = AlertDialog.Builder(requireContext())
                    .setMessage("Are you sure you want to delete this pet profile?")
                    .setCancelable(false)
                    .setPositiveButton("Yes") { _, _ ->
                        database = FirebaseDatabase.getInstance().getReference("pet-profile")

                        // Remove pet profile from Firebase
                        database.child(userId).child(petProfile.petId!!).removeValue()
                            .addOnSuccessListener {
                                // Remove the item from the local list
                                petProfileList.remove(petProfile)

                                // Notify adapter about the change
                                petProfileAdapter.notifyDataSetChanged()
                                // Show confirmation message
                                Toast.makeText(requireContext(), "Pet profile deleted successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { exception ->
                                // Handle failure
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to delete: ${exception.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss() // Dismiss the dialog if user clicks "No"
                    }

                val alertDialog = dialogBuilder.create()
                alertDialog.show()
            } else {
                Toast.makeText(requireContext(), "Pet ID is missing", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "User ID not found", Toast.LENGTH_SHORT).show()
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
