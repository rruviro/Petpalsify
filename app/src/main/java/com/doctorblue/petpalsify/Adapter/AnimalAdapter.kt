package com.doctorblue.petpalsify.Adapter

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.doctorblue.petpalsify.Model.Animal
import com.doctorblue.petpalsify.databinding.CategoryListBinding

class AnimalAdapter(
    private val animalList: List<Animal>,
    private val onAnimalSelected: (String) -> Unit  // Callback function for selected animal
) : RecyclerView.Adapter<AnimalAdapter.AnimalViewHolder>() {

    private var selectedPosition = -1 // To track the selected item

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimalViewHolder {
        val binding = CategoryListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnimalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnimalViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val animal = animalList[position]
        holder.binding.animalName.text = animal.name // Set the name of the animal
        holder.binding.animalIcon.setBackgroundResource(animal.iconResource) // Set the icon

        // Handle item click
        holder.itemView.setOnClickListener {
            if (selectedPosition != position) {
                val previousSelectedPosition = selectedPosition
                selectedPosition = position

                // Notify only if the selected item has changed
                notifyItemChanged(previousSelectedPosition) // Reset the bold for the previous item
                notifyItemChanged(selectedPosition) // Set bold for the newly selected item

                // Call the callback function to pass the selected animal category
                onAnimalSelected(animal.name)
            }
        }
    }

    override fun getItemCount(): Int = animalList.size

    // ViewHolder for binding the layout
    inner class AnimalViewHolder(val binding: CategoryListBinding) : RecyclerView.ViewHolder(binding.root)
}
