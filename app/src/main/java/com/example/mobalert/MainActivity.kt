package com.example.mobalert

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.mobalert.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)

        goToFragment(HomeFragment())

        auth = FirebaseAuth.getInstance()
        Log.d("LOGIN", "${auth.currentUser}")
        if(auth.currentUser?.email != null){
            Log.d("LOGIN", "LOGGATO")
            setContentView(binding.root)
        }
        else{
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        /*binding.goToLoginButton.setOnClickListener {
            Toast.makeText(this, "Go to login button clicked", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }*/

        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.item_home -> {
                    goToFragment(HomeFragment())
                }

                R.id.item_alert -> {
                    goToFragment(AlertsFragment())
                }

                R.id.item_person -> {
                    goToFragment(ProfileFragment())
                }
            }

            true
        }

    }

    private fun goToFragment(fragment: Fragment) {
        Log.d("LOGIN", "goToFragment")
        supportFragmentManager.beginTransaction()
            .replace(R.id.Fragment, fragment)
            .commit()

    }
}