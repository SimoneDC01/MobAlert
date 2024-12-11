package com.example.mobalert

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mobalert.databinding.FragmentMapBinding
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager

class MapFragment : Fragment(), PermissionsListener {

    lateinit var permissionsManager: PermissionsManager
    lateinit var binding: FragmentMapBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (PermissionsManager.areLocationPermissionsGranted(requireContext())) {
            // Logica sensibile ai permessi, ad esempio attivare la LocationComponent
            // HERE
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(requireActivity())

        }

        binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        // Spiega perché hai bisogno dei permessi (ad esempio mostrando un Toast o un AlertDialog)
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {

        } else {
            // Logica per gestire il rifiuto dei permessi
        }
    }
}
