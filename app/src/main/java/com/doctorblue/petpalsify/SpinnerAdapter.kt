package com.doctorblue.petpalsify

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class GenderAdapter(context: Context, resource: Int, private val genderOption: Array<String>) :
    ArrayAdapter<String>(context, resource, genderOption) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        val resources = context.resources
        val largeTextSize = resources.getDimension(R.dimen.text_size_large)
        if (position == 0) {
            textView.text = "Select Gender"
            textView.textSize = largeTextSize
            textView.setTextColor(Color.GRAY)
        } else {
            textView.textSize = 13f
            textView.text = genderOption[position]
        }
        textView.setTextColor(Color.BLACK)
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        val resources = context.resources
        val largeTextSize = resources.getDimension(R.dimen.text_size_large)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.textSize = largeTextSize
        textView.text = genderOption[position]
        textView.setTextColor(Color.BLACK)
        textView.setBackgroundColor(Color.WHITE)
        return view
    }

}

class CategoryAdapter(context: Context, resource: Int, private val categoryOption: Array<String>) :
    ArrayAdapter<String>(context, resource, categoryOption) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        val resources = context.resources
        val largeTextSize = resources.getDimension(R.dimen.text_size_large)
        if (position == 0) {
            textView.text = "Select Pet"
            textView.textSize = largeTextSize
            textView.setTextColor(Color.GRAY)
        } else {
            textView.textSize = 13f
            textView.text = categoryOption[position]
        }
        textView.setTextColor(Color.BLACK)
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        val resources = context.resources
        val largeTextSize = resources.getDimension(R.dimen.text_size_large)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.textSize = largeTextSize
        textView.text = categoryOption[position]
        textView.setTextColor(Color.BLACK)
        textView.setBackgroundColor(Color.WHITE)
        return view
    }

}