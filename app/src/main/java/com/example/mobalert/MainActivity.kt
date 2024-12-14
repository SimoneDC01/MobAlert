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

        if(savedInstanceState == null){
            supportFragmentManager.beginTransaction()
                .replace(R.id.Fragment, HomeFragment())
                .commit()
        }

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
            val currentFragment = supportFragmentManager.findFragmentById(R.id.Fragment)
            when (menuItem.itemId) {
                R.id.item_home -> {
                    if (currentFragment !is HomeFragment) {
                        goToFragment(HomeFragment())
                    }
                }

                R.id.item_alert -> {
                    if (currentFragment !is AlertsFragment) {
                        goToFragment(AlertsFragment())
                    }
                }

                R.id.item_person -> {
                    if (currentFragment !is ProfileFragment) {
                        goToFragment(ProfileFragment())
                    }
                }
            }

            true
        }

        supportFragmentManager.addOnBackStackChangedListener {
            updateBottomNavSelection()
        }

    }
    private fun updateBottomNavSelection() {
        // Ottieni il fragment attualmente visibile
        val currentFragment = supportFragmentManager.findFragmentById(R.id.Fragment)
        Log.d("LOGIN", "updateBottomNavSelection loc: $currentFragment")
        when (currentFragment) {
            is HomeFragment -> binding.bottomNavigationView.selectedItemId = R.id.item_home
            is AlertsFragment -> binding.bottomNavigationView.selectedItemId = R.id.item_alert
            is ProfileFragment -> binding.bottomNavigationView.selectedItemId = R.id.item_person
        }
    }

    private fun goToFragment(fragment: Fragment) {

        Log.d("LOGIN", "goToFragment")
        supportFragmentManager.beginTransaction()
            .replace(R.id.Fragment, fragment)
            .addToBackStack(null)
            .commit()

    }

    companion object {
        const val url = "http://192.168.1.12:5000"
    }
}