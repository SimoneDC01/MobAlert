package com.example.mobalert

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mobalert.databinding.FragmentEditProfileBinding
import com.example.mobalert.databinding.FragmentProfileBinding
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class EditProfileFragment : Fragment() {
    private lateinit var binding: FragmentEditProfileBinding
    private val auth = Firebase.auth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        // Inflate the layout for this fragment


        binding.updateProfileButton.setOnClickListener {
            val profile = UserProfileChangeRequest.Builder()
                .setDisplayName(binding.nameTil.editText?.text.toString())
                .build()
            auth.currentUser?.updateProfile(profile)
            parentFragmentManager.beginTransaction()
                .replace(R.id.Fragment, ProfileFragment())
                .commit()
        }

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.profileTv.text = auth.currentUser?.displayName
        super.onViewCreated(view, savedInstanceState)
        if(auth.currentUser?.displayName != "") binding.nameTil.editText?.setText(auth.currentUser?.displayName)
        if(auth.currentUser?.phoneNumber != "") binding.phoneTil.editText?.setText(auth.currentUser?.phoneNumber)
    }

}