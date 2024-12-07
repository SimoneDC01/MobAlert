package com.example.mobalert

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.mobalert.databinding.FragmentAlertsBinding
import com.example.mobalert.databinding.FragmentEditAlertBinding


class EditAlertFragment : Fragment() {

    private lateinit var binding: FragmentEditAlertBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditAlertBinding.inflate(inflater, container, false);


        val alertTitle = arguments?.getString("alertTitle") // Altro campo, se passato
        val alertDescription = arguments?.getString("alertDescription")
        setupCategoryDropdown()
        binding.editTitle.setText(alertTitle)
        binding.editDescription.setText(alertDescription)

        binding.editAlertButton.setOnClickListener {
            Toast.makeText(requireContext(), "Edited", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            parentFragmentManager.beginTransaction()
                .replace(R.id.Fragment, AlertsFragment())
                .commit()
        }


        // Inflate the layout for this fragment
        return binding.root
    }

    private fun setupCategoryDropdown() {
        // Lista di categorie predefinite
        val categories = listOf("Emergency", "Warning", "Info", "Critical")

        // Crea un adattatore per l'AutoCompleteTextView
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        )

        // Imposta l'adattatore nell'AutoCompleteTextView
        binding.editCategory.setAdapter(adapter)

        // Apri sempre il dropdown quando l'utente clicca sul campo
        binding.editCategory.setOnClickListener {
            binding.editCategory.showDropDown()
        }

        // Garantire che si apra anche al focus
        binding.editCategory.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.editCategory.showDropDown()
            }
        }
    }
}