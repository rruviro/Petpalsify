package com.doctorblue.petpalsify

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.doctorblue.petpalsify.databinding.FragmentConvoBinding

class ConvoFragment : Fragment() {

    private lateinit var binding: FragmentConvoBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentConvoBinding.inflate(inflater, container, false)

        binding.back.setOnClickListener {
            findNavController().navigate(R.id.action_convoFragment_to_chatsFragment)
        }

        return binding.root
    }

}