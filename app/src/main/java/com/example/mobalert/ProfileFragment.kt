package com.example.mobalert

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mobalert.databinding.FragmentProfileBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.lang.Thread.sleep

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private val auth = Firebase.auth

    override fun onStart() {
        super.onStart()
        Log.d("LOGIN", "onStart")
        sleep(1000)
        binding.fullNameTv.text = auth.currentUser?.displayName
        binding.emailTv.text = auth.currentUser?.email
        if(auth.currentUser?.phoneNumber == "") binding.phoneTv.text = "Not Set"
        else binding.phoneTv.text = auth.currentUser?.phoneNumber
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentProfileBinding.inflate(inflater, container, false);

        binding.logoutCv.setOnClickListener {
            Log.d("LOGIN", "Logout button clicked")
            auth.signOut()
            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
        }

        binding.editCv.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.Fragment, EditProfileFragment())
                .commit()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }


}