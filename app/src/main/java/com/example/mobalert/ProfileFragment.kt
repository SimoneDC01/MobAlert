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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import java.lang.Thread.sleep

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private val auth = Firebase.auth
    private var database = FirebaseDatabase.getInstance()
    private var reference = database.reference.child("Users")

    override fun onStart() {
        super.onStart()
        Log.d("LOGIN", "onStart")
        reference.child(auth.uid.toString()).get().addOnSuccessListener {
            binding.fullNameTv.text = it.child("name").value.toString()
            binding.emailTv.text = it.child("email").value.toString()
            val phone = it.child("phoneNumber").value.toString()
            if(phone == "") binding.phoneTv.text = "Not Set"
            else binding.phoneTv.text = phone
            val dob = it.child("dob").value.toString()
            if(dob == "") binding.dobTv.text = "Not Set"
            else binding.dobTv.text = dob
        }
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

        binding.changePasswordCv.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.Fragment, ChangePasswordFragment())
                .commit()
        }

        binding.editCv.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.Fragment, EditProfileFragment())
                .commit()
        }

        binding.deleteCv.setOnClickListener {
            reference.child(auth.uid.toString()).removeValue().addOnSuccessListener {
                Log.d("LOGIN", "User deleted from database")
            }.addOnFailureListener {
                Log.d("LOGIN", "User not deleted from database")
            }
            auth.currentUser!!.delete().addOnSuccessListener {
                Log.d("LOGIN", "User deleted from firebase")
            }.addOnFailureListener {e->
                Log.d("LOGIN", "User not deleted from firebase $e")
            }
            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }


}