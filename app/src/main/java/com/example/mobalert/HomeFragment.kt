package com.example.mobalert

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mobalert.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var referece: DatabaseReference
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        referece = database.reference.child("Users")
        binding.prova.setOnClickListener{
            referece.child(auth.uid.toString()).setValue("prova").addOnSuccessListener {
                Log.d("LOGIN", "prova: OK")
            }
            .addOnFailureListener {e->
                Log.d("LOGIN", "prova: ERROR $e")
            }
            Log.d("LOGIN", "prova: ${referece}")
        }

        return binding.root
    }

}