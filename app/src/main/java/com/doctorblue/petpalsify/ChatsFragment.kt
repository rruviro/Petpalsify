package com.doctorblue.petpalsify

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.doctorblue.petpalsify.databinding.FragmentChatsBinding

class ChatsFragment : Fragment() {

    private lateinit var binding: FragmentChatsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentChatsBinding.inflate(inflater, container, false)

        binding.bottomNavigation.homebtn.setOnClickListener {
            findNavController().navigate(R.id.action_chatsFragment_to_homeFragment)
        }

        binding.bottomNavigation.petsbtn.setOnClickListener {
            findNavController().navigate(R.id.action_chatsFragment_to_myPostFragment)
        }

        binding.bottomNavigation.profilebtn.setOnClickListener {
            findNavController().navigate(R.id.action_chatsFragment_to_profileFragment)
        }

        return binding.root
    }

}