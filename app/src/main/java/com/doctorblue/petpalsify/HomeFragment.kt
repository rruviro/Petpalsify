package com.doctorblue.petpalsify

import PetAdapter
import PetSquareAdapter
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
import com.doctorblue.petpalsify.Adapter.AnimalAdapter
import com.doctorblue.petpalsify.Model.Animal
import com.doctorblue.petpalsify.Model.PetProfile
import com.doctorblue.petpalsify.databinding.FragmentHomeBinding
import com.google.firebase.database.FirebaseDatabase

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var petProfileList: MutableList<PetProfile>
    private lateinit var petAdapter: PetAdapter
    private lateinit var petSquareAdapter: PetSquareAdapter

    private var selectedCategory: String = "All"  // Default category is "All"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Handle button clicks and navigation
        binding.chatbtn.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_chatsFragment)
        }

        binding.bottomNavigation.inboxbtn.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_chatsFragment)
        }

        binding.bottomNavigation.petsbtn.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_myPostFragment)
        }

        binding.bottomNavigation.profilebtn.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        }

        binding.search.setOnClickListener {

            binding.search.setOnClickListener {
                val searchText = binding.searchTxt.text.toString().trim()
                if (searchText.isNotEmpty()) {
                    fetchPicks(searchText) // Pass the searchText to the fetchPicks method
                } else {
                    Toast.makeText(requireContext(), "Please enter a search term", Toast.LENGTH_SHORT).show()
                }
            }

        }

        binding.searchLogo.setOnClickListener {

            val searchText = binding.searchTxt.text.toString().trim()
            if (searchText.isNotEmpty()) {
                fetchPicks(searchText) // Pass the searchText to the fetchPicks method
            } else {
                Toast.makeText(requireContext(), "Please enter a search term", Toast.LENGTH_SHORT).show()
            }

        }

        // Initialize the list and adapter
        petProfileList = mutableListOf()
        petAdapter = PetAdapter(petProfileList, requireContext()) { petProfile ->
            val bundle = Bundle()
            bundle.putString("petId", petProfile.petId)
            bundle.putString("userPetId", petProfile.userpetId)
            findNavController().navigate(R.id.action_homeFragment_to_informationFragment, bundle)
        }

        petSquareAdapter = PetSquareAdapter(petProfileList, requireContext()) { petProfile ->
            val bundle = Bundle()
            bundle.putString("petId", petProfile.petId)
            bundle.putString("userPetId", petProfile.userpetId)
            findNavController().navigate(R.id.action_homeFragment_to_informationFragment, bundle)
        }

        // Set up RecyclerView for pet categories
        val animalList = listOf(
            Animal("All", R.drawable.all),
            Animal("Dog", R.drawable.dog),
            Animal("Cat", R.drawable.cat),
            Animal("Rabbit", R.drawable.rabit),
            Animal("Fish", R.drawable.fish),
            Animal("Hamster", R.drawable.hamster),
            Animal("Bird", R.drawable.bird),
            Animal("Others", R.drawable.others)
        )

        val adapter = AnimalAdapter(animalList) { selectedCategory ->
            this.selectedCategory = selectedCategory
            fetchPicks()  // Fetch the pets based on the selected category
        }

        binding.petCategory.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.petCategory.adapter = adapter

        binding.rectangleRecycleview.layoutManager = LinearLayoutManager(requireContext())
        binding.squareRecycleview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rectangleRecycleview.adapter = petAdapter
        binding.squareRecycleview.adapter = petSquareAdapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchPicks()  // Fetch the data when the fragment is created
    }

    private fun fetchPicks(searchText: String? = null) {
        // Show shimmer while data is loading
        binding.shimmerContainer.visibility = View.VISIBLE
        binding.squareLoading.visibility = View.VISIBLE
        binding.squareRecycleview.visibility = View.GONE
        binding.rectangleRecycleview.visibility = View.INVISIBLE
        binding.noPetPostText.visibility = View.GONE
        binding.noPetPostText2.visibility = View.GONE

        val database = FirebaseDatabase.getInstance()
        val petProfileRef = database.getReference("pet-profile")

        petProfileRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val allPetProfiles = mutableListOf<PetProfile>()
                snapshot.children.forEach { userSnapshot ->
                    userSnapshot.children.forEach { petSnapshot ->
                        val petCategory = petSnapshot.child("petCategory").getValue(String::class.java) ?: "Unknown"
                        val petName = petSnapshot.child("petName").getValue(String::class.java) ?: ""
                        val petBreed = petSnapshot.child("petBreed").getValue(String::class.java) ?: ""

                        // Check if searchText matches petName or petBreed
                        val matchesSearch = searchText?.let {
                            petName.contains(it, ignoreCase = true) || petBreed.contains(it, ignoreCase = true)
                        } ?: true

                        if ((selectedCategory == "All" || petCategory == selectedCategory) && matchesSearch) {
                            val petProfile = PetProfile(
                                petSnapshot.child("petImage").getValue(String::class.java) ?: "Unknown",
                                petName,
                                petSnapshot.child("petAge").getValue(String::class.java) ?: "Unknown",
                                petSnapshot.child("petAbout").getValue(String::class.java) ?: "No info",
                                petSnapshot.child("petHealth").getValue(String::class.java) ?: "Unknown",
                                petSnapshot.child("petWeight").getValue(String::class.java) ?: "Unknown",
                                petSnapshot.child("petAddress").getValue(String::class.java) ?: "Unknown",
                                petSnapshot.child("petBirthday").getValue(String::class.java) ?: "Unknown",
                                petCategory,
                                petBreed,
                                petSnapshot.child("petGender").getValue(String::class.java) ?: "Unknown",
                                petSnapshot.child("userContact").getValue(String::class.java) ?: "Unknown",
                                petSnapshot.child("petPostDate").getValue(String::class.java) ?: "Unknown",
                                petSnapshot.child("petId").getValue(String::class.java) ?: "Unknown",
                                petSnapshot.child("userpetId").getValue(String::class.java) ?: "Unknown",
                            )
                            allPetProfiles.add(petProfile)
                        }
                    }
                }

                if (allPetProfiles.isEmpty()) {
                    // No data fetched
                    binding.noPetPostText.visibility = View.VISIBLE
                    binding.noPetPostText2.visibility = View.VISIBLE
                    binding.rectangleRecycleview.visibility = View.INVISIBLE
                    binding.squareRecycleview.visibility = View.GONE
                } else {
                    // Update the adapter
                    petProfileList.clear()
                    petProfileList.addAll(allPetProfiles)
                    petAdapter.notifyDataSetChanged()

                    binding.noPetPostText.visibility = View.GONE
                    binding.noPetPostText2.visibility = View.GONE
                    binding.rectangleRecycleview.visibility = View.VISIBLE
                    binding.squareRecycleview.visibility = View.VISIBLE
                }
            } else {
                binding.noPetPostText.visibility = View.VISIBLE
                binding.noPetPostText2.visibility = View.VISIBLE
                binding.rectangleRecycleview.visibility = View.INVISIBLE
                binding.squareRecycleview.visibility = View.GONE
            }

            binding.shimmerContainer.visibility = View.GONE
            binding.squareLoading.visibility = View.GONE
        }.addOnFailureListener { exception ->
            Log.e("fetchPicks", "Error fetching data: ${exception.message}")
            binding.noPetPostText.visibility = View.VISIBLE
            binding.noPetPostText2.visibility = View.VISIBLE
            binding.rectangleRecycleview.visibility = View.INVISIBLE
            binding.squareRecycleview.visibility = View.GONE
            binding.shimmerContainer.visibility = View.GONE
            binding.squareLoading.visibility = View.GONE
        }
    }

}
