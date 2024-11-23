package com.doctorblue.petpalsify

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.doctorblue.petpalsify.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.squareRecycleview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
//        binding.squareRecycleview.adapter = adapter

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

        return binding.root
    }

}