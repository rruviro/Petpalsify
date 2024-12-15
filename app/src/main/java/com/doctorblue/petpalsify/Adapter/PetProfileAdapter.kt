package com.doctorblue.petpalsify.Adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.doctorblue.petpalsify.Model.PetProfile
import com.doctorblue.petpalsify.R
import com.google.android.material.imageview.ShapeableImageView

class PetProfileAdapter(
    private val petProfiles: List<PetProfile>,
    private val context: Context,
    private val onManageClick: (PetProfile) -> Unit,   // Callback for manage button
    private val onDeleteClick: (PetProfile) -> Unit    // Callback for delete button
) : RecyclerView.Adapter<PetProfileAdapter.PetProfileViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetProfileViewHolder {
        // Inflate the item layout for each pet profile
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.rectangle_manage_card, parent, false)
        return PetProfileViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PetProfileViewHolder, position: Int) {
        val petProfile = petProfiles[position]
        holder.bind(petProfile)

        // Handle the Manage button click
        holder.manageButton.setOnClickListener {
            onManageClick(petProfile)
        }

        // Handle the Delete button click
        holder.deleteButton.setOnClickListener {
            onDeleteClick(petProfile)
        }

    }

    override fun getItemCount(): Int = petProfiles.size

    // ViewHolder class to bind the data to the views
    inner class PetProfileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val petName: TextView = itemView.findViewById(R.id.petName)
        private val petAge: TextView = itemView.findViewById(R.id.petCategory) // Assuming this is where age goes
        private val petAddress: TextView = itemView.findViewById(R.id.petLocation) // Assuming this is where address goes
        private val petPost: TextView = itemView.findViewById(R.id.petPost)  // Assuming this is where birthday goes
        private val petBreed: TextView = itemView.findViewById(R.id.petBreed)  // Assuming this is where breed goes
        private val petCategory: TextView = itemView.findViewById(R.id.petCategory)  // Assuming this is where breed goes
        private val petImage: ShapeableImageView = itemView.findViewById(R.id.petImage) // Correct initialization

        // Buttons for Manage and Delete
        val manageButton: TextView = itemView.findViewById(R.id.manageBtn)
        val deleteButton: TextView = itemView.findViewById(R.id.deleteBtn)

        fun bind(petProfile: PetProfile) {
            // Bind data to each TextView based on the PetProfile model
            petName.text = petProfile.petName
            petAge.text = petProfile.petAge
            petAddress.text = petProfile.petAddress
            petPost.text = petProfile.petPostDate
            petCategory.text = petProfile.petCategory
            petBreed.text = petProfile.petBreed

            // Check if petImage is not null and load it
            petProfile.petImage?.let { imageBase64 ->
                // Decode the Base64 string into a Bitmap
                try {
                    val decodedString: ByteArray = Base64.decode(imageBase64, Base64.DEFAULT)  // Corrected here
                    val decodedBitmap: Bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

                    // Load the decoded Bitmap into the ImageView using Glide
                    Glide.with(context)
                        .load(decodedBitmap)  // Use Bitmap for local image
                        .into(petImage)
                } catch (e: Exception) {
//                                showToast("Failed to decode image")
                }
            }

        }
    }
}
